package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.intellij.editor.tiled.TiledMap.*
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

	val tiledTilesets = arrayListOf<TiledTileset>()

	data.tilesets.fastForEach { tileset ->
		tiledTilesets += tileset.toTiledSet(folder, hasTransparentColor, transparentColor, createBorder)
	}

	return TiledMap(data, tiledTilesets)
}

suspend fun VfsFile.readTileSetData(firstgid: Int = 1): TileSetData {
	return parseTileSetData(this.readXml(), firstgid, this.baseName)
}

suspend fun TileSetData.toTiledSet(
	folder: VfsFile,
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledTileset {
	val tileset = this
	var bmp = try {
		when (tileset.image) {
			is Image.Embedded -> TODO()
			is Image.External -> folder[tileset.image.source].readBitmapOptimized()
			null -> Bitmap32(0, 0)
		}
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
				tileset.tileWidth,
				tileset.tileHeight,
				tileset.columns,
				tileset.tileCount,
				tileset.spacing,
				tileset.margin
			)
			TileSet(slices, tileset.tileWidth, tileset.tileHeight, bmp)
		} else {
			// No separation between tiles: create a new Bitmap adding that separation
			val bitmaps = TileSet.extractBitmaps(
				bmp,
				tileset.tileWidth,
				tileset.tileHeight,
				tileset.columns,
				tileset.tileCount,
				tileset.spacing,
				tileset.margin
			)
			TileSet.fromBitmaps(tileset.tileWidth, tileset.tileHeight, bitmaps, border = createBorder, mipmaps = false)
		}
	} else {
		TileSet(bmp.slice(), tileset.tileWidth, tileset.tileHeight, tileset.columns, tileset.tileCount)
	}

	val tiledTileset = TiledTileset(
		tileset = ptileset,
		data = tileset,
		firstgid = tileset.firstgid
	)

	return tiledTileset
}

suspend fun VfsFile.readTiledMapData(): TiledMapData {
	val file = this
	val folder = this.parent.jail()
	val tiledMap = TiledMapData()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	//TODO: Support different orientations
	val orientation = mapXml.getString("orientation")
	@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
	tiledMap.orientation = when {
		orientation == "orthogonal" -> TiledMap.Orientation.ORTHOGONAL
		else -> unsupported("Orientation \"$orientation\" is not supported")
	}
	val renderOrder = mapXml.getString("renderorder")
	@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
	tiledMap.renderOrder = when {
		renderOrder == "right-down" -> RenderOrder.RIGHT_DOWN
		renderOrder == "right-up" -> RenderOrder.RIGHT_UP
		renderOrder == "left-down" -> RenderOrder.LEFT_DOWN
		renderOrder == "left-up" -> RenderOrder.LEFT_UP
		else -> RenderOrder.RIGHT_DOWN
	}
	tiledMap.compressionLevel = mapXml.getInt("compressionlevel") ?: -1
	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32
	tiledMap.hexSideLength = mapXml.getInt("hexsidelength")
	val staggerAxis = mapXml.getString("staggeraxis")
	@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
	tiledMap.staggerAxis = when {
		staggerAxis == "x" -> StaggerAxis.X
		staggerAxis == "y" -> StaggerAxis.Y
		else -> null
	}
	val staggerIndex = mapXml.getString("staggerindex")
	@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
	tiledMap.staggerIndex = when {
		staggerIndex == "even" -> StaggerIndex.EVEN
		staggerIndex == "odd" -> StaggerIndex.ODD
		else -> null
	}
	tiledMap.backgroundColor = mapXml.getString("backgroundcolor")?.let { colorFromARGB(it, Colors.TRANSPARENT_BLACK) }
	val nextLayerId = mapXml.getInt("nextlayerid")
	val nextObjectId = mapXml.getInt("nextobjectid")
	tiledMap.infinite = mapXml.getInt("infinite") == 1

	mapXml.child("properties")?.parseProperties()?.let {
		tiledMap.properties.putAll(it)
	}

	tilemapLog.trace { "tilemap: width=${tiledMap.width}, height=${tiledMap.height}, tilewidth=${tiledMap.tilewidth}, tileheight=${tiledMap.tileheight}" }
	tilemapLog.trace { "tilemap: $tiledMap" }

	val elements = mapXml.allChildrenNoComments

	tilemapLog.trace { "tilemap: elements=${elements.size}" }
	tilemapLog.trace { "tilemap: elements=$elements" }

	elements.fastForEach { element ->
		val elementName = element.nameLC
		@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
		when {
			elementName == "tileset" -> {
				tilemapLog.trace { "tileset" }
				val firstgid = element.int("firstgid", 1)
				val sourcePath = element.getString("source")
				val tileset = if (sourcePath != null) folder[sourcePath].readXml() else element
				tiledMap.tilesets += parseTileSetData(tileset, firstgid, sourcePath)
			}
			//TODO: Support group //elementName == "group"
			elementName == "layer" || elementName == "objectgroup" || elementName == "imagelayer" -> {
				tilemapLog.trace { "layer:$elementName" }
				val layer = when (element.nameLC) {
					"layer" -> TiledMap.Layer.Tiles()
					"objectgroup" -> TiledMap.Layer.Objects()
					"imagelayer" -> TiledMap.Layer.Image()
					else -> invalidOp
				}
				tiledMap.allLayers += layer
				//TODO: support layer id
				//layer.id = element.int("id")
				layer.name = element.str("name")
				//TODO: move to objects only
				layer.draworder = element.str("draworder", "topdown")
				//TODO: move to objects only
				layer.color = Colors[element.str("color", "#a0a0a4")]
				layer.opacity = element.double("opacity", 1.0)
				layer.visible = element.int("visible", 1) != 0
				//TODO: support locked
				//layer.locked = element.int("locked", 1) != 0
				//TODO: support tintcolor
				//layer.tintcolor = element.str("tintcolor") //optional
				layer.offsetx = element.double("offsetx", 0.0)
				layer.offsety = element.double("offsety", 0.0)

				element.child("properties")?.parseProperties()?.let {
					layer.properties.putAll(it)
				}

				when (layer) {
					//TODO: Support group
					//is TiledMap.Layer.Group -> {  }
					is TiledMap.Layer.Tiles -> {
						val width = element.int("width")
						val height = element.int("height")
						val count = width * height
						val data = element.child("data")
						val encoding = data?.str("encoding", "") ?: ""
						val compression = data?.str("compression", "") ?: ""

						//TODO: support chunks as <data> elements
						@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
						val tilesArray: IntArray = when {
							encoding == "" || encoding == "xml" -> {
								val items = data?.children("tile")?.map { it.uint("gid").toInt() } ?: listOf()
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
									//TODO: support "zstd" compression
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
							val name = obj.str("name")
							val type = obj.str("type")
							val bounds =
								obj.run { Rectangle(double("x"), double("y"), double("width"), double("height")) }
							val rotation = obj.double("rotation")
							val gid = obj.intNull("gid")
							//TODO: support visible property
							//val visible = obj.int("visible", 1) != 0
							//TODO: support template
							var rkind = RKind.RECT
							var points = listOf<Point>()
							var objprops: Map<String, Property> = LinkedHashMap()

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
									//TODO: support <text>
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
			elementName == "editorsettings" -> { /* ignore */ }
		}
	}

	tiledMap.nextLayerId = nextLayerId ?: run {
		var maxLayerId = 0
		for (layer in tiledMap.allLayers) {
			if (layer.id > maxLayerId) maxLayerId = layer.id
		}
		maxLayerId + 1
	}
	tiledMap.nextObjectId = nextObjectId ?: run {
		var maxObjectId = 0
		for (objects in tiledMap.objectLayers) {
			for (obj in objects.objects) {
				if (obj.id > maxObjectId) maxObjectId = obj.id
			}
		}
		maxObjectId + 1
	}

	return tiledMap
}

fun parseTileSetData(tileset: Xml, firstgid: Int, tilesetSource: String? = null): TileSetData {
	val alignment = tileset.str("objectalignment", "unspecified")
	val objectAlignment = ObjectAlignment.values().find { it.value == alignment } ?: ObjectAlignment.UNSPECIFIED
	val image = tileset.child("image")?.parseImage()
	val tileOffset = tileset.child("tileoffset")

	return TileSetData(
		name = tileset.str("name"),
		firstgid = firstgid,
		tileWidth = tileset.int("tilewidth"),
		tileHeight = tileset.int("tileheight"),
		tileCount = tileset.int("tilecount", 0),
		spacing = tileset.int("spacing", 0),
		margin = tileset.int("margin", 0),
		columns = tileset.int("columns", 0),
		image = image,
		tileOffsetX = tileOffset?.int("x") ?: 0,
		tileOffsetY = tileOffset?.int("y") ?: 0,
		grid = tileset.child("grid")?.parseGrid(),
		tilesetSource = tilesetSource,
		objectAlignment = objectAlignment,
		terrains = tileset.children("terraintypes").children("terrain").map { it.parseTerrain() },
		wangsets = tileset.children("wangsets").children("wangset").map { it.parseWangSet() },
		properties = tileset.child("properties")?.parseProperties() ?: mapOf(),
		tiles = tileset.children("tile").map { it.parseTile() }
	)
}

private fun Xml.parseTile(): TileData {
	val tile = this
	fun Xml.parseFrame(): AnimationFrameData {
		return AnimationFrameData(this.int("tileid"), this.int("duration"))
	}
	return TileData(
		id = tile.int("id"),
		type = tile.int("type", -1),
		terrain = tile.str("terrain").takeIf { it.isNotEmpty() }?.split(',')?.map { it.toIntOrNull() },
		probability = tile.double("probability"),
		image = tile.child("image")?.parseImage(),
		properties = tile.child("properties")?.parseProperties() ?: mapOf(),
		objectGroup = tile.child("objectgroup")?.parseObjectLayer(),
		frames = tile.child("animation")?.children("frame")?.map { it.parseFrame() }
	)
}

private fun Xml.parseObjectLayer(): Layer.Objects {
	TODO()
}

private fun Xml.parseTerrain(): TerrainData {
	return TerrainData(
		name = str("name"),
		tile = int("tile"),
		properties = parseProperties()
	)
}

private fun Xml.parseWangSet(): WangSet {
	fun Xml.parseWangColor(): WangSet.WangColor {
		val wangcolor = this
		return WangSet.WangColor(
			name = wangcolor.str("name"),
			color = Colors[wangcolor.str("color")],
			tileId = wangcolor.int("tile"),
			probability = wangcolor.double("probability")
		)
	}

	fun Xml.parseWangTile(): WangSet.WangTile {
		val wangtile = this
		val hflip = wangtile.str("hflip")
		val vflip = wangtile.str("vflip")
		val dflip = wangtile.str("dflip")
		return WangSet.WangTile(
			tileId = wangtile.int("tileid"),
			wangId = wangtile.int("wangid"),
			hflip = hflip == "1" || hflip == "true",
			vflip = vflip == "1" || vflip == "true",
			dflip = dflip == "1" || dflip == "true"
		)
	}
	val wangset = this
	return WangSet(
		name = wangset.str("name"),
		tileId = wangset.int("tile"),
		properties = wangset.parseProperties(),
		cornerColors = wangset.children("wangcornercolor").map { it.parseWangColor() },
		edgeColors = wangset.children("wangedgecolor").map { it.parseWangColor() },
		wangtiles = wangset.children("wangtile").map { it.parseWangTile() }
	)
}

private fun Xml.parseGrid(): Grid {
	val grid = this
	val orientation = grid.str("orientation")
	return Grid(
		cellWidth = grid.int("width"),
		cellHeight = grid.int("height"),
		orientation = Grid.Orientation.values().find { it.value == orientation } ?: Grid.Orientation.ORTHOGONAL
	)
}

private fun Xml.parseImage(): Image? {
	val image = this
	val width = image.int("width")
	val height = image.int("height")
	val trans = image.str("trans")
	val transparent = when {
		trans.isEmpty() -> null
		trans.startsWith("#") -> Colors[trans]
		else -> Colors["#$trans"]
	}
	val source = image.str("source")
	return if (source.isNotEmpty()) {
		Image.External(
			source = source,
			width = width,
			height = height,
			transparent = transparent
		)
	} else {
		val data = image.child("data") ?: return null
		Image.Embedded(
			format = image.str("format"),
			data = data.parseData(),
			width = width,
			height = height,
			transparent = transparent
		)
	}
}

private fun Xml.parseData(): Data {
	val encoding = str("encoding")
	val compression = str("compression")
	return Data(
		encoding = Data.Encoding.values().find { it.value == encoding } ?: Data.Encoding.XML,
		compression = Data.Compression.values().find { it.value == compression } ?: Data.Compression.NO,
		data = content
	)
}

private fun Xml.parseProperties(): Map<String, Property> {
	val out = LinkedHashMap<String, Property>()
	for (property in this.children("property")) {
		val pname = property.str("name")
		val rawValue = if (property.hasAttribute("value")) property.str("value") else ""
		val type = property.str("type", "string")
		val pvalue = when (type) {
			"string" -> Property.StringT(rawValue)
			"int" -> Property.IntT(rawValue.toIntOrNull() ?: 0)
			"float" -> Property.FloatT(rawValue.toDoubleOrNull() ?: 0.0)
			"bool" -> Property.BoolT(rawValue == "true")
			"color" -> Property.ColorT(colorFromARGB(rawValue, Colors.TRANSPARENT_BLACK))
			"file" -> Property.FileT(if (rawValue.isEmpty()) "." else rawValue)
			"object" -> Property.ObjectT(rawValue.toIntOrNull() ?: 0)
			else -> Property.StringT(rawValue)
		}
		out[pname] = pvalue
	}
	return out
}

//TODO: move to korio
private fun Xml.uint(name: String, defaultValue: UInt = 0u): UInt =
	this.attributesLC[name]?.toUIntOrNull() ?: defaultValue

//TODO: move to korim
fun colorFromARGB(color: String, default: RGBA): RGBA {
	if (!color.startsWith('#') || color.length != 9) return default
	val hex = color.substring(1)
	val a = (hex.substr(0, 2).toInt(0x10) * 1.0).toInt()
	val r = (hex.substr(2, 2).toInt(0x10) * 1.0).toInt()
	val g = (hex.substr(4, 2).toInt(0x10) * 1.0).toInt()
	val b = (hex.substr(6, 2).toInt(0x10) * 1.0).toInt()
	return RGBA(r, g, b, a)
}

private enum class RKind {
	POINT, RECT, ELLIPSE, POLYLINE, POLYGON
}

private val spaces = Regex("\\s+")
