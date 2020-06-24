package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import kotlin.collections.set

suspend fun VfsFile.readTiledMap(
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledMap {
	val folder = this.parent.jail()
	val data = readTiledMapData()

	//val combinedTileset = kotlin.arrayOfNulls<Texture>(data.maxGid + 1)

	data.imageLayers.fastForEach { layer ->
		layer.image = try {
			folder[layer.source].readBitmapOptimized()
		} catch (e: Throwable) {
			e.printStackTrace()
			Bitmap32(layer.width, layer.height)
		}
	}

	val tiledTilesets = arrayListOf<TiledMap.TiledTileset>()

	data.tilesets.fastForEach { tileset ->
		tiledTilesets += tileset.toTiledSet(folder, hasTransparentColor, transparentColor, createBorder)
	}

	return TiledMap(data, tiledTilesets)
}

private fun Xml.parseProperties(): Map<String, Any> {
	val out = LinkedHashMap<String, Any>()
	for (property in this.children("property")) {
		val pname = property.str("name")
		val rawValue = if (property.hasAttribute("value")) property.str("value") else property.text
		val type = property.str("type", "text")
		val pvalue: Any = when (type) {
			"bool" -> rawValue == "true"
			"color" -> Colors[rawValue]
			"text" -> rawValue
			"int" -> rawValue.toIntOrNull() ?: 0
			"float" -> rawValue.toDoubleOrNull() ?: 0.0
			"file" -> TiledFile(pname)
			else -> rawValue
		}
		out[pname] = pvalue
		//println("$pname: $pvalue")
	}
	return out
}

fun parseTileSetData(element: Xml, firstgid: Int, tilesetSource: String? = null): TileSetData {
	//<?xml version="1.0" encoding="UTF-8"?>
	//<tileset version="1.2" tiledversion="1.3.1" name="Overworld" tilewidth="16" tileheight="16" tilecount="1440" columns="40">
	//	<image source="Overworld.png" width="640" height="576"/>
	//</tileset>
	val image = element.child("image")

	return TileSetData(
		name = element.str("name"),
		firstgid = firstgid,
		tilewidth = element.int("tilewidth"),
		tileheight = element.int("tileheight"),
		tilecount = element.int("tilecount", -1),
		spacing = element.int("spacing"),
		margin = element.int("margin"),
		columns = element.int("columns", -1),
		image = image,
		tilesetSource = tilesetSource,
		imageSource = image?.str("source") ?: "",
		width = image?.int("width", 0) ?: 0,
		height = image?.int("height", 0) ?: 0,
		terrains = element.children("terraintypes").children("terrain")
			.map { TerrainData(name = it.str("name"), tile = it.int("tile")) },
		tiles = element.children("tile").map {
			TileData(
				id = it.int("id"),
				terrain = it.str("terrain").takeIf { it.isNotEmpty() }?.split(',')?.map { it.toIntOrNull() },
				probability = it.double("probability", 1.0),
				frames = it.child("animation")?.children("frame")?.map {
					AnimationFrameData(it.int("tileid"), it.int("duration"))
				}
			)
		}
	)
}

suspend fun VfsFile.readTileSetData(firstgid: Int = 1): TileSetData {
	return parseTileSetData(this.readXml(), firstgid, this.baseName)
}

suspend fun TileSetData.toTiledSet(
	folder: VfsFile,
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledMap.TiledTileset {
	val tileset = this
	var bmp = try {
		folder[tileset.imageSource].readBitmapOptimized()
	} catch (e: Throwable) {
		e.printStackTrace()
		Bitmap32(tileset.width, tileset.height)
	}

	// @TODO: Preprocess this, so in JS we don't have to do anything!
	if (hasTransparentColor) {
		bmp = bmp.toBMP32()
		for (n in 0 until bmp.area) {
			if (bmp.data[n] == transparentColor) bmp.data[n] = Colors.TRANSPARENT_BLACK
		}
	}

	val ptileset = if (createBorder > 0) {
		bmp = bmp.toBMP32()

		if (tileset.spacing >= createBorder) {
			// There is already separation between tiles, use it as it is
			val slices = TileSet.extractBmpSlices(
				bmp,
				tileset.tilewidth,
				tileset.tileheight,
				tileset.columns,
				tileset.tilecount,
				tileset.spacing,
				tileset.margin
			)
			TileSet(slices, tileset.tilewidth, tileset.tileheight, bmp)
		} else {
			// No separation between tiles: create a new Bitmap adding that separation
			val bitmaps = TileSet.extractBitmaps(
				bmp,
				tileset.tilewidth,
				tileset.tileheight,
				tileset.columns,
				tileset.tilecount,
				tileset.spacing,
				tileset.margin
			)
			TileSet.fromBitmaps(tileset.tilewidth, tileset.tileheight, bitmaps, border = createBorder, mipmaps = false)
		}
	} else {
		TileSet(bmp.slice(), tileset.tilewidth, tileset.tileheight, tileset.columns, tileset.tilecount)
	}

	val tiledTileset = TiledMap.TiledTileset(
		tileset = ptileset,
		data = tileset,
		firstgid = tileset.firstgid
	)

	return tiledTileset
}

suspend fun VfsFile.readTiledMapData(): TiledMapData {
	val log = tilemapLog
	val file = this
	val folder = this.parent.jail()
	val tiledMap = TiledMapData()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32

	tilemapLog.trace { "tilemap: width=${tiledMap.width}, height=${tiledMap.height}, tilewidth=${tiledMap.tilewidth}, tileheight=${tiledMap.tileheight}" }
	tilemapLog.trace { "tilemap: $tiledMap" }

	val elements = mapXml.allChildrenNoComments

	tilemapLog.trace { "tilemap: elements=${elements.size}" }
	tilemapLog.trace { "tilemap: elements=$elements" }

	var maxGid = 1
	//var lastBaseTexture = views.transparentTexture.base

	elements.fastForEach { element ->
		val elementName = element.nameLC
		@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
		when {
			elementName == "tileset" -> {
				tilemapLog.trace { "tileset" }
				val firstgid = element.int("firstgid", +1)
				// TSX file / embedded element
				val sourcePath = element.getString("source")
				val element = if (sourcePath != null) folder[sourcePath].readXml() else element
				tiledMap.tilesets += parseTileSetData(element, firstgid, sourcePath)
				//lastBaseTexture = tex.base
			}
			elementName == "layer" || elementName == "objectgroup" || elementName == "imagelayer" -> {
				tilemapLog.trace { "layer:$elementName" }
				val layer = when (element.nameLC) {
					"layer" -> TiledMap.Layer.Tiles()
					"objectgroup" -> TiledMap.Layer.Objects()
					"imagelayer" -> TiledMap.Layer.Image()
					else -> invalidOp
				}
				tiledMap.allLayers += layer
				layer.name = element.str("name")
				layer.visible = element.int("visible", 1) != 0
				layer.draworder = element.str("draworder", "")
				layer.color = Colors[element.str("color", "#ffffff")]
				layer.opacity = element.double("opacity", 1.0)
				layer.offsetx = element.double("offsetx", 0.0)
				layer.offsety = element.double("offsety", 0.0)

				val properties = element.child("properties")?.parseProperties()
				if (properties != null) {
					layer.properties.putAll(properties)
				}

				when (layer) {
					is TiledMap.Layer.Tiles -> {
						val width = element.int("width")
						val height = element.int("height")
						val count = width * height
						val data = element.child("data")
						val encoding = data?.str("encoding", "") ?: ""
						val compression = data?.str("compression", "") ?: ""

						@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
						val tilesArray: IntArray = when {
							encoding == "" || encoding == "xml" -> {
								val items = data?.children("tile")?.map { it.uint("gid") } ?: listOf()
								items.toIntArray()
							}
							encoding == "csv" -> {
								val content = data?.text ?: ""
								val items = content.replace(spaces, "").split(',').map { it.toUInt().toInt() }
								items.toIntArray()
							}
							encoding == "base64" -> {
								val base64Content = (data?.text ?: "").trim()
								val rawContent = base64Content.fromBase64()

								val content = when (compression) {
									"" -> rawContent
									"gzip" -> rawContent.uncompress(GZIP)
									"zlib" -> rawContent.uncompress(ZLib)
									else -> invalidOp("Unknown compression '$compression'")
								}
								content.readIntArrayLE(0, count)
							}
							else -> invalidOp("Unhandled encoding '$encoding'")
						}
						if (tilesArray.size != count) invalidOp("tilesArray.size != count (${tilesArray.size} != ${count})")
						layer.map = Bitmap32(width, height, RgbaArray(tilesArray))
						layer.encoding = encoding
						layer.compression = compression
					}
					is TiledMap.Layer.Image -> {
						for (image in element.children("image")) {
							layer.source = image.str("source")
							layer.width = image.int("width")
							layer.height = image.int("height")
						}
					}
					is TiledMap.Layer.Objects -> {
						for (obj in element.children("object")) {
							val id = obj.int("id")
							val rotation = obj.double("rotation")
							val gid = obj.intNull("gid")
							val name = obj.str("name")
							val type = obj.str("type")
							val bounds =
								obj.run { Rectangle(double("x"), double("y"), double("width"), double("height")) }
							var rkind = RKind.RECT
							var points = listOf<Point>()
							var objprops: Map<String, Any> = LinkedHashMap()

							for (kind in obj.allNodeChildren) {
								val kindType = kind.nameLC
								@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
								when {
									kindType == "ellipse" -> {
										rkind = RKind.ELLIPSE
									}
									kindType == "polyline" || kindType == "polygon" -> {
										val pointsStr = kind.str("points")
										points = pointsStr.split(spaces).map {
											val parts = it.split(',').map { it.trim().toDoubleOrNull() ?: 0.0 }
											Point(parts[0], parts[1])
										}

										rkind = (if (kindType == "polyline") RKind.POLYLINE else RKind.POLYGON)
									}
									kindType == "properties" -> {
										objprops = kind.parseProperties()
									}
									kindType == "point" -> {
										rkind = RKind.POINT
									}
									else -> invalidOp("Invalid object kind '$kindType'")
								}
							}

							val info = TiledMap.Layer.ObjectInfo(id, gid, name, rotation, type, bounds, objprops)
							layer.objects += when (rkind) {
								RKind.POINT -> TiledMap.Layer.Objects.PPoint(info)
								RKind.RECT -> TiledMap.Layer.Objects.Rect(info)
								RKind.ELLIPSE -> TiledMap.Layer.Objects.Ellipse(info)
								RKind.POLYLINE -> TiledMap.Layer.Objects.Polyline(info, points)
								RKind.POLYGON -> TiledMap.Layer.Objects.Polygon(info, points)
							}
						}
					}
				}
			}
		}
	}

	return tiledMap
}

//TODO: move to korio
private fun Xml.uint(name: String, defaultValue: Int = 0): Int =
	this.attributesLC[name]?.toUIntOrNull()?.toInt() ?: defaultValue

private enum class RKind {
	POINT, RECT, ELLIPSE, POLYLINE, POLYGON
}

private val spaces = Regex("\\s+")
