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
				uiSequence({ tilemap.tilesets }, tilesetsUpdated) { tileset ->
					println("TAB.TILESET: $tileset")
					tab("Untitled") {
						val tilemap = tileset.pickerTilemap()
						val mapComponent = MapComponent(tilemap)
						val patternLayer = tilemap.patternLayers.first()
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
								Bitmap32(width, height) { x, y -> RGBA(patternLayer.map[xmin + x, ymin + y].value) }
							picked.value = PickedSelection(bmp)
							mapComponent.selectedRange(xmin, ymin, bmp.width, bmp.height)
						}
						this.component.add(JBScrollPane(mapComponent))
					}
				}
			}
			toolbar {
				iconButton(toolbarIcon("add.png")) {
					click {
						val file = projectContext.chooseFile()
						if (file != null) {
							val vfsFile = file.toVfs()
							runBlocking {
								val tileset = try {
									val bmp = vfsFile.readBitmap()
									TiledMap.TiledTileset(TileSet(bmp.slice().split(32, 32), 32, 32, bmp))
								} catch (e: Throwable) {
									vfsFile.readTileSetData().toTiledSet(vfsFile.parent)
								}

								val oldMap = tilemap
								history.addAndDo("ADD TILESET") { redo ->
									if (redo) {
										tilemap.data.tilesets += tileset.data
										tilemap.tilesets += tileset
										tilemap.tileset = tileset.tileset
										tilesetsUpdated(Unit)
									} else {
										TODO("Unsupported now")
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
				iconButton(toolbarIcon("delete.png")) {
					click {
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
		allLayers = arrayListOf(TiledMap.Layer.Tiles(Bitmap32(mapWidth.coerceAtLeast(1), mapHeight.coerceAtLeast(1)) { x, y -> RGBA(y * mapWidth + x) }))
	), listOf(this), tileset)
}
