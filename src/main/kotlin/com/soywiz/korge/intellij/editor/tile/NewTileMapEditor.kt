package com.soywiz.korge.intellij.editor.tile

import com.intellij.ui.components.JBScrollPane
import com.soywiz.kmem.clamp
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.ObservableProperty
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.runBlocking
import java.awt.Container

fun Styled<out Container>.createTileMapEditor() {
	val zoom = ObservableProperty(2.0) { it.clamp(0.25, 20.0) }
	data class PickedSelection(val gid: Int)
	val picked = ObservableProperty(PickedSelection(0))
	fun zoomIn() = run { zoom.value *= 1.25 }
	fun zoomOut() = run { zoom.value /= 1.25 }
	val tilemap = runBlocking { localCurrentDirVfs["samples/gfx/sample.tmx"].readTiledMap() }

	verticalStack {
		//horizontalStack {
		//	height = 32.points
		toolbar {
			iconButton(toolbarIcon("edit.png"))
			button("Dropper")
			button("Eraser")
			button("Fill")
			button("Rect")
			button("Poly")
			button("Point")
			button("Oval")
			//}
			//toolbar {
			iconButton(toolbarIcon("settings.png"))
			iconButton(toolbarIcon("zoomIn.png")) {
				click { zoomIn() }
			}
			iconButton(toolbarIcon("zoomOut.png")) {
				click { zoomOut() }
			}
			//}
			//toolbar {
			button("FlipX")
			button("FlipY")
			button("RotateL")
			button("RotateR")
		}
		//}

		horizontalStack {
			fill()
			verticalStack {
				//minWidth = 132.pt
				minWidth = 228.pt
				width = minWidth
				//width = 20.percentage
				tabs {
					height = 50.percentage
					tab("Properties") {
						verticalStack {
							list(listOf("prop1", "prop2", "prop3")) {
								height = MUnit.Fill
							}
							toolbar {
								iconButton(toolbarIcon("add.png"))
								iconButton(toolbarIcon("edit.png"))
								iconButton(toolbarIcon("delete.png"))
							}
						}
					}
				}
				tabs {
					height = 50.percentage
					tab("Tileset") {
						verticalStack {
							tabs {
								fill()
								for (tileset in tilemap.tilesets) {
									tab("Untitled") {
										val tilemap = tileset.pickerTilemap()
										val mapComponent = MapComponent(tilemap)
										val patternLayer = tilemap.patternLayers.first()
										mapComponent.selectedRange(0, 0)
										mapComponent.downTileSignal {
											picked.value = PickedSelection(patternLayer.map[it.x, it.y].value)
											mapComponent.selectedRange(it.x, it.y)
										}
										this.component.add(JBScrollPane(mapComponent))
									}
								}
							}
							toolbar {
								iconButton(toolbarIcon("add.png"))
								iconButton(toolbarIcon("openDisk.png"))
								iconButton(toolbarIcon("edit.png"))
								iconButton(toolbarIcon("delete.png"))
							}
						}
					}
				}
			}
			verticalStack {
				minWidth = 32.pt
				fill()
				tabs {
					fill()
					tab("Map") {
						val mapComponent = MapComponent(tilemap)
						mapComponent.overTileSignal { println("OVER: $it") }
						mapComponent.downTileSignal {
							println("DOWN: $it")
							mapComponent.tmx.patternLayers[0].map[it.x, it.y] = RGBA(picked.value.gid)
							mapComponent.repaint()
						}
						mapComponent.downRightTileSignal { println("DOWN_RIGHT: $it") }
						this.component.add(JBScrollPane(mapComponent))
						zoom { mapComponent.scale = it }
					}
				}
			}
			verticalStack {
				minWidth = 228.pt
				width = minWidth
				//width = 20.percentage
				tabs {
					height = 50.percentage
					tab("Layers") {
						verticalStack {
							fill()
							list(listOf("layer1", "layer2", "layer2")) {
								height = MUnit.Fill
							}
							toolbar {
								iconButton(toolbarIcon("add.png")) {
									click {
										showPopupMenu(listOf("Tile Layer", "Object Layer", "Image Layer")) {
											println("CLICKED ON: $it")
										}
									}
								}
								iconButton(toolbarIcon("up.png"))
								iconButton(toolbarIcon("down.png"))
								iconButton(toolbarIcon("delete.png"))
								iconButton(toolbarIcon("copy.png"))
								iconButton(toolbarIcon("show.png"))
								iconButton(toolbarIcon("locked_dark.png"))
							}
						}
					}
				}
				tabs {
					height = 50.percentage
					tab("Preview") {
					}
				}
			}
		}
		horizontalStack {
			height = 32.pt
			label("Status Status 2") {
				zoom { component.text = "Zoom: ${"%.0f".format(zoom.value * 100)}%" }
			}
			slider(min = 25, max = 2000) {
				component.addChangeListener { zoom.value = component.value.toDouble() / 100.0 }
				zoom { component.value = (zoom.value * 100).toInt() }
			}
		}
	}

	zoom.value = 2.0
}

private fun TiledMap.TiledTileset.pickerTilemap(): TiledMap {
	val tileset = this.tileset

	val mapWidth = this.data.columns
	val mapHeight = Math.ceil(this.data.tilecount.toDouble() / this.data.columns.toDouble()).toInt()


	return TiledMap(TiledMapData(
		width = mapWidth, height = mapHeight,
		tilewidth = tileset.width, tileheight = tileset.height,
		allLayers = arrayListOf(TiledMap.Layer.Patterns(Bitmap32(mapWidth, mapHeight) { x, y -> RGBA(y * mapWidth + x) }))
	), listOf(this), tileset)
}
