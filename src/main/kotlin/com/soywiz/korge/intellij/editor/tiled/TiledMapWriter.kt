package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*

suspend fun VfsFile.writeTiledMap(map: TiledMap) {
	writeString(map.toXML().toString())
}

fun TiledMap.toXML(): Xml {
	val map = this
	val mapData = map.data
	return buildXml(
		"map",
		"version" to 1.2,
		"tiledversion" to "1.3.1",
		"orientation" to mapData.orientation.value,
		"renderorder" to mapData.renderOrder.value,
		"compressionlevel" to mapData.compressionLevel,
		"width" to mapData.width,
		"height" to mapData.height,
		"tilewidth" to mapData.tilewidth,
		"tileheight" to mapData.tileheight,
		"hexsidelength" to mapData.hexSideLength,
		"staggeraxis" to mapData.staggerAxis,
		"staggerindex" to mapData.staggerIndex,
		"backgroundcolor" to mapData.backgroundColor?.toStringARGB(),
		"nextlayerid" to mapData.nextLayerId,
		"nextobjectid" to mapData.nextObjectId,
		"infinite" to mapData.infinite
	) {
		propertiesToXml(mapData.properties)
		for (tileset in map.tilesets) {
			val tilesetData = tileset.data
			if (tilesetData.tilesetSource != null) {
				node("tileset", "firstgid" to tilesetData.firstgid, "source" to tilesetData.tilesetSource)
			} else {
				node(tilesetData.toXML())
			}
		}
		for (layer in map.allLayers) {
			when (layer) {
				is TiledMap.Layer.Tiles -> {
					node(
						"layer",
						"id" to layer.id,
						"name" to layer.name,
						"width" to layer.width,
						"height" to layer.height
					) {
						node("data", "encoding" to "csv") {
							text(buildString(layer.area * 4) {
								append("\n")
								for (y in 0 until layer.height) {
									for (x in 0 until layer.width) {
										append(layer.data[x, y].value)
										if (y != layer.height - 1 || x != layer.width - 1) append(',')
									}
									append("\n")
								}
							})
						}
					}
				}
				//is TiledMap.Layer.Objects -> {}
				//is TiledMap.Layer.Image -> {}
				//is TiledMap.Layer.Group -> {}
				else -> TODO("Unsupported layer $layer")
			}
		}
	}
}

private fun TileSetData.toXML(): Xml {
	return buildXml(
		"tileset",
		"firstgid" to firstgid,
		"name" to name,
		"tilewidth" to tileWidth,
		"tileheight" to tileHeight,
		"spacing" to spacing.takeIf { it > 0 },
		"margin" to margin.takeIf { it > 0 },
		"tilecount" to tileCount,
		"columns" to columns,
		"objectalignment" to objectAlignment.value
	) {
		imageToXml(image)
		if (tileOffsetX != 0 || tileOffsetY != 0) {
			node("tileoffset", "x" to tileOffsetX, "y" to tileOffsetY)
		}
		grid?.let { grid ->
			node(
				"grid",
				"orientation" to grid.orientation.value,
				"width" to grid.cellWidth,
				"height" to grid.cellHeight
			)
		}
		propertiesToXml(properties)
		if (terrains.isNotEmpty()) {
			node("terraintypes") {
				for (terrain in terrains) {
					node("terrain", "name" to terrain.name, "tile" to terrain.tile) {
						propertiesToXml(terrain.properties)
					}
				}
			}
		}
		if (wangsets.isNotEmpty()) {
			node("wangsets") {
				for (wangset in wangsets) node(wangset.toXml())
			}
		}
		if (tiles.isNotEmpty()) {
			for (tile in tiles) {
				node(tile.toXml())
			}
		}
	}
}

private fun WangSet.toXml(): Xml {
	return buildXml("wangset", "name" to name, "tile" to tileId) {
		propertiesToXml(properties)
		if (cornerColors.isNotEmpty()) {
			for (color in cornerColors) {
				node(
					"wangcornercolor",
					"name" to color.name,
					"color" to color.color,
					"tile" to color.tileId,
					"probability" to color.probability
				)
			}
		}
		if (edgeColors.isNotEmpty()) {
			for (color in edgeColors) {
				node(
					"wangedgecolor",
					"name" to color.name,
					"color" to color.color,
					"tile" to color.tileId,
					"probability" to color.probability
				)
			}
		}
		if (wangtiles.isNotEmpty()) {
			for (wangtile in wangtiles) {
				node(
					"wangtile",
					"tileid" to wangtile.tileId,
					"wangid" to wangtile.wangId.toUInt().toString(16).toUpperCase(),
					"hflip" to wangtile.hflip.takeIf { it },
					"vflip" to wangtile.vflip.takeIf { it },
					"dflip" to wangtile.dflip.takeIf { it }
				)
			}
		}
	}
}

private fun TileData.toXml(): Xml {
	return buildXml("tile",
		"id" to id,
		"type" to type.takeIf { it != -1 },
		"terrain" to terrain?.joinToString(",") { it?.toString() ?: "" },
		"probability" to probability.takeIf { it != 0.0 }
	) {
		propertiesToXml(properties)
		imageToXml(image)
		objectLayerToXml(objectGroup)
		if (frames != null && frames.isNotEmpty()) {
			node("animation") {
				for (frame in frames) {
					node("frame", "tileid" to frame.tileId, "duration" to frame.duration)
				}
			}
		}
	}
}

private fun XmlBuilder.objectLayerToXml(objectLayer: TiledMap.Layer.Objects?) {
	if (objectLayer == null) return
	TODO()
}

private fun XmlBuilder.imageToXml(image: TiledMap.Image?) {
	if (image == null) return
	node(
		"image",
		when (image) {
			is TiledMap.Image.Embedded -> "format" to image.format
			is TiledMap.Image.External -> "source" to image.source
		},
		"width" to image.width,
		"height" to image.height,
		"transparent" to image.transparent
	) {
		if (image is TiledMap.Image.Embedded) {
			node(image.data.toXml())
		}
	}
}

private fun TiledMap.Data.toXml(): Xml {
	val data = this
	return buildXml(
		"data",
		"encoding" to data.encoding.value,
		"compression" to data.compression.value
	) {
		text(data.data)
	}
}

private fun XmlBuilder.propertiesToXml(properties: Map<String, TiledMap.Property>) {
	if (properties.isEmpty()) return

	fun property(name: String, type: String, value: Any) =
		node("property", "name" to name, "type" to type, "value" to value)

	node("properties") {
		properties.forEach { (name, prop) ->
			when (prop) {
				is TiledMap.Property.StringT -> property(name, "string", prop.value)
				is TiledMap.Property.IntT -> property(name, "int", prop.value)
				is TiledMap.Property.FloatT -> property(name, "float", prop.value)
				is TiledMap.Property.BoolT -> property(name, "bool", prop.value.toString())
				is TiledMap.Property.ColorT -> property(name, "color", prop.value.toStringARGB())
				is TiledMap.Property.FileT -> property(name, "file", prop.path)
				is TiledMap.Property.ObjectT -> property(name, "object", prop.id)
			}
		}
	}
}

//TODO: move to korim
private fun RGBA.toStringARGB(): String {
	return "#%02x%02x%02x%02x".format(a, r, g, b)
}
