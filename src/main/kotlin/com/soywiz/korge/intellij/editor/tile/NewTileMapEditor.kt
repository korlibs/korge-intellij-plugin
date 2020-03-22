package com.soywiz.korge.intellij.editor.tile

import com.soywiz.kmem.clamp
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.ObservableProperty
import java.awt.Container

fun Styled<out Container>.createTileMapEditor() {
	val zoom = ObservableProperty(1.0) { it.clamp(0.25, 20.0) }
	fun zoomIn() = run { zoom.value *= 1.25 }
	fun zoomOut() = run { zoom.value /= 1.25 }

	verticalStack {
		//horizontalStack {
		//	height = 32.points
		toolbar {
			button("Stamp")
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
				minWidth = 132.pt
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
								tab("Untitled") {
									list(listOf("tileset")) {
										height = MUnit.Fill
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
			height = 32.points
			label("Status Status 2") {
				zoom {
					component.text = "Zoom: ${"%.0f".format(zoom.value * 100)}%"
				}
			}
			slider(min = 25, max = 2000) {
				component.addChangeListener {
					zoom.value = component.value.toDouble() / 100.0
				}

				zoom {
					val zoomPer = (zoom.value * 100).toInt()
					component.value = zoomPer
				}
			}
		}
	}

	zoom.value = 1.0
}
