package com.soywiz.korge.intellij.editor.tile

import com.soywiz.korge.intellij.util.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*


suspend fun VfsFile.writeTiledMap(map: TiledMap) {
	writeString(map.toXML().toString())
}

fun TiledMap.toXML(): Xml {
	val map = this
	return buildXml("map",
		"version" to 1.2,
		"tiledversion" to "1.3.1",
		"orientation" to "orthogonal",
		"renderorder" to "right-down",
		"compressionlevel" to -1,
		"width" to map.width,
		"height" to map.height,
		"tilewidth" to map.tilewidth,
		"tileheight" to map.tileheight,
		"infinite" to 0,
		"nextlayerid" to map.allLayers.size + 1,
		"nextobjectid" to map.objectLayers.size + 1
	) {
		for (tileset in map.tilesets) {
			if (tileset.data.tilesetSource != null) {
				node("tileset", "firstgid" to tileset.data.firstgid, "source" to tileset.data.tilesetSource)
			} else {
				node(tileset.data.toXML())
			}
		}
		for (layer in map.allLayers) {
			when (layer) {
				is TiledMap.Layer.Patterns -> {
					node("layer", "id" to layer.id, "name" to layer.name, "width" to layer.width, "height" to layer.height) {
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
				else -> TODO("Unsupported layer $layer")
			}
		}
	}
}

fun TileSetData.toXML(): Xml {
	//<tileset version="1.2" tiledversion="1.3.1" name="Overworld" tilewidth="16" tileheight="16" tilecount="1440" columns="40">
	//  <image source="Overworld.png" width="640" height="576"/>
	//</tileset>
	return buildXml("tileset",
		"version" to 1.2,
		"tiledversion" to "1.3.1",
		"name" to name,
		"tilewidth" to tilewidth,
		"tileheight" to tileheight,
		"tilecount" to tilecount,
		"columns" to columns
	) {
		node("image", "source" to imageSource, "width" to width, "height" to height)
		if (terrains.isNotEmpty()) {
			node("terraintypes") {
				//<terrain name="Ground1" tile="0"/>
				for (terrain in terrains) {
					node("terrain", "name" to terrain.name, "tile" to terrain.tile)
				}
			}
		}
		for (tile in tiles) {
			node("tile",
				"id" to tile.id,
				"terrain" to tile.terrain?.joinToString(",") { it?.toString() ?: "" },
				"probability" to tile.probability.takeIf { it != 1.0 }
			) {
				val frames = tile.frames
				if (frames != null) {
					node("animation") {
						for (frame in frames) {
							node("frame", "tileid" to frame.tileid, "duration" to frame.duration)
						}
					}
				}
			}
		}
	}
}
