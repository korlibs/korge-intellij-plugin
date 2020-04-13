package com.soywiz.korge.intellij.editor.tile.editor

import com.intellij.ui.components.*
import com.soywiz.kmem.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.editor.tile.*
import com.soywiz.korge.intellij.editor.tile.dialog.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import javax.swing.*
import kotlin.math.*

fun Styled<out JTabbedPane>.tilesetTab(
	ctx: MapContext
) = ctx.apply {
	tab("Tileset") {
		verticalStack {
			tabs {
				fill()
                component.addChangeListener {
                    selectedTilesetIndex.value = (it.source as JBTabbedPane).selectedIndex
                }
				uiSequence({ tilemap.tilesets }, tilesetsUpdated) { tileset ->
					tab(tileset.data.name) {
						val tilemap = tileset.pickerTilemap()
						val mapComponent = MapComponent(tilemap)
						val tileLayer = tilemap.tileLayers.first()
						mapComponent.selectedRange(0, 0)
						var downStart: PointInt? = null
						val zoomLevel =
							ObservableProperty(zoomLevels.indexOf(100)) { it.clamp(0, zoomLevels.size - 1) }

						fun zoomRatio(): Double = zoomLevels[zoomLevel.value].toDouble() / 100.0
						zoomLevel {
							mapComponent.scale = zoomRatio()
						}
						zoomLevel.trigger()
						mapComponent.onZoom {
							zoomLevel.value += it
						}
						mapComponent.upTileSignal {
							downStart = null
						}
						mapComponent.outTileSignal {
							//downStart = null
						}
						mapComponent.downTileSignal {
							if (downStart == null) {
								downStart = it
							}
							val start = downStart!!
							val xmin = min(start.x, it.x)
							val xmax = max(start.x, it.x)
							val ymin = min(start.y, it.y)
							val ymax = max(start.y, it.y)
							val width = xmax - xmin + 1
							val height = ymax - ymin + 1
							val bmp =
								Bitmap32(width, height) { x, y -> RGBA(tileLayer.map[xmin + x, ymin + y].value) }
							picked.value = PickedSelection(bmp)
							mapComponent.selectedRange(xmin, ymin, bmp.width, bmp.height)
						}
						this.component.add(JBScrollPane(mapComponent))
					}
				}
			}
			toolbar {
				iconButton(toolbarIcon("add.png"), "Add tileset file or image") {
					click {
						val vfsFile = projectContext.chooseFile()?.toVfs()
						if (vfsFile != null) {
							runBlocking {
                                val firstgid = tilemap.nextGid
								val tileset = if (vfsFile.extensionLC != "tsx") {
									val bmp = vfsFile.readBitmap()
									tiledsetFromBitmap(vfsFile.baseName, 32, 32, bmp, firstgid)
								} else {
									vfsFile.readTileSetData(firstgid).toTiledSet(vfsFile.parent)
								}

								history.addAndDo("ADD TILESET") { redo ->
									if (redo) {
										tilemap.data.tilesets += tileset.data
										tilemap.tilesets.add(tileset)
										tilesetsUpdated(Unit)
									} else {
										tilemap.data.tilesets -= tileset.data
                                        tilemap.tilesets.remove(tileset)
                                        tilesetsUpdated(Unit)
									}
								}
							}
						}
					}
				}
				iconButton(toolbarIcon("openDisk.png")) {
					click {
					}
				}
				iconButton(toolbarIcon("edit.png")) {
					click {
					}
				}
				iconButton(toolbarIcon("delete.png"), "Remove tileset") {
                    tilesetsUpdated {
                        component.isEnabled = tilemap.tilesets.size > 1
                    }
					click {
                        val index = selectedTilesetIndex.value
                        val tileset = tilemap.tilesets[index]
                        history.addAndDo("REMOVE TILESET") { redo ->
                            if (redo) {
                                tilemap.data.tilesets.add(tileset.data)
                                tilemap.tilesets.add(index, tileset)
                                tilesetsUpdated(Unit)
                            } else {
                                tilemap.data.tilesets.remove(tileset.data)
                                tilemap.tilesets.removeAt(index)
                                tilesetsUpdated(Unit)
                            }
                        }
					}
				}
			}
		}
	}
}

data class PickedSelection(val data: Bitmap32)

private fun TiledMap.TiledTileset.pickerTilemap(): TiledMap {
	val tileset = this.tileset
	val mapWidth = this.data.columns.takeIf { it >= 0 } ?: (this.tileset.width / this.data.tilewidth)
	val mapHeight = ceil(this.data.tilecount.toDouble() / this.data.columns.toDouble()).toInt()

	return TiledMap(TiledMapData(
		width = mapWidth, height = mapHeight,
		tilewidth = tileset.width, tileheight = tileset.height,
		allLayers = arrayListOf(TiledMap.Layer.Tiles(
            Bitmap32(mapWidth.coerceAtLeast(1), mapHeight.coerceAtLeast(1)) { x, y -> RGBA(y * mapWidth + x + firstgid) }
        ))
	), mutableListOf(this))
}

private fun tiledsetFromBitmap(name: String, tileWidth: Int, tileHeight: Int, bmp: Bitmap, firstgid: Int): TiledMap.TiledTileset {
    val tileset = TileSet(bmp.slice().split(tileWidth, tileHeight), tileWidth, tileHeight)
    return TiledMap.TiledTileset(tileset,
        TileSetData(
            name = name.substringBeforeLast("."),
            firstgid = firstgid,
            tilewidth = tileset.width,
            tileheight = tileset.height,
            tilecount = tileset.textures.size,
            columns = tileset.base.width / tileset.width,
            image = null,
            imageSource = name,
            width = tileset.base.width,
            height = tileset.base.height,
            tilesetSource = null,
            terrains = listOf(),
            tiles = tileset.textures.mapIndexed { index, bmpSlice -> TileData(index) }
        )
    )
}
