package com.soywiz.korge.intellij.editor.tile

import com.intellij.ui.components.*
import com.intellij.uiDesigner.core.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

class MyTileMapEditorPanel(val tmx: TiledMap) : JPanel(BorderLayout()) {
	val tileMapEditor = TileMapEditor()

	val maxTileGid = tmx.tilesets.map { it.firstgid + it.tileset.textures.size }.max() ?: 0

	data class TileInfo(val bmp32: Bitmap32) {
		val miniSlice = bmp32.slice()
	}

	val tiles = Array<TileInfo?>(maxTileGid) { null }.also { tiles ->
		for (tileset in tmx.tilesets) {
			for (tileIdx in tileset.tileset.textures.indices) {
				val tile = tileset.tileset.textures[tileIdx]
				if (tile != null) {
					val miniSlice = (tile as BitmapSlice<*>).extract().toBMP32()
					tiles[tileset.firstgid + tileIdx] = when {
						miniSlice.allFixed { it.a == 0 } -> null // Transparent
						else -> TileInfo(miniSlice)
					}
				}
			}
		}
	}

	val realPanel = tileMapEditor.contentPanel
	var scale = 2.0
		set(value) {
			field = value
			mapComponent.updateSize()
			mapComponent.revalidate()
			mapComponentScroll.revalidate()
			mapComponentScroll.repaint()
		}

	inner class MapComponent : JComponent() {
		init {
			updateSize()
			addMouseMotionListener(object : MouseMotionAdapter() {
				override fun mouseDragged(e: MouseEvent) {
					println("mouseDragged: $e")
					onPressMouse(e.point)
				}
			})
			addMouseListener(object : MouseAdapter() {
				override fun mousePressed(e: MouseEvent) {
					println("mousePressed: $e")
					if (e.button == MouseEvent.BUTTON1) {
						onPressMouse(e.point)
					} else {
						onRightPressMouse(e.point)
					}
				}
			})
		}

		var currentTileSelected = 1

		fun onPressMouse(point: Point) {
			val tileIndex = getTileIndex(point)
			tmx.patternLayers[0].map[tileIndex.x, tileIndex.y] = RGBA(currentTileSelected)
			repaint()
			//println(tileIndex)
		}

		fun onRightPressMouse(point: Point) {
			val tileIndex = getTileIndex(point)
			currentTileSelected = tmx.patternLayers[0].map[tileIndex.x, tileIndex.y].value
		}

		fun updateSize() {
			this.preferredSize = Dimension((tmx.pixelWidth * scale).toInt(), (tmx.pixelHeight * scale).toInt())
		}

		fun getTileIndex(coords: Point): Point = Point((coords.x / tmx.tilewidth / scale).toInt(), (coords.y / tmx.tileheight / scale).toInt())

		override fun paintComponent(g: Graphics) {
			val g = (g as Graphics2D)

			val TILE_WIDTH = tmx.tilewidth
			val TILE_HEIGHT = tmx.tileheight

			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
			val clipBounds = g.clipBounds
			val displayTilesX = ((clipBounds.width / TILE_WIDTH / scale) + 3).toInt()
			val displayTilesY = ((clipBounds.height / TILE_HEIGHT / scale) + 3).toInt()
			val temp = Bitmap32((displayTilesX * TILE_WIDTH), (displayTilesY * TILE_HEIGHT))

			val offsetX = (clipBounds.x / TILE_WIDTH / scale).toInt()
			val offsetY = (clipBounds.y / TILE_HEIGHT / scale).toInt()

			for (layer in tmx.allLayers) {
				when (layer) {
					is TiledMap.Layer.Patterns -> {
						for (x in 0 until displayTilesX) {
							for (y in 0 until displayTilesY) {
								val rx = x + offsetX
								val ry = y + offsetY

								if (rx < 0 || rx >= layer.map.width) continue
								if (ry < 0 || ry >= layer.map.height) continue

								val tileIdx = layer.map[rx, ry].value
								val tile = tiles.getOrNull(tileIdx)
								if (tile != null) {
									temp._draw(tile.miniSlice, x * TILE_WIDTH, y * TILE_HEIGHT, mix = true)
								}
							}
						}
					}
				}
			}

			//val oldTransform = g.transform
			g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)
			g.scale(scale, scale)
			g.drawImage(temp.toAwt(), 0, 0, null)

			//g.transform = oldTransform

			g.stroke = BasicStroke((1f / scale).toFloat())
			g.color = Color.BLACK
			//g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)
			for (y in 0 until displayTilesY) g.drawLine(0, y * TILE_HEIGHT, displayTilesX * TILE_WIDTH, y * TILE_HEIGHT)
			for (x in 0 until displayTilesX) g.drawLine(x * TILE_WIDTH, 0, x * TILE_WIDTH, displayTilesY * TILE_HEIGHT)
		}
	}

	val mapComponent = MapComponent()
	val mapComponentScroll = JBScrollPane(mapComponent).also { scroll ->
		//scroll.verticalScrollBar.unitIncrement = 16
	}

	fun updatedSize() {
		tileMapEditor.leftSplitPane.dividerLocation = 200
		tileMapEditor.rightSplitPane.dividerLocation = tileMapEditor.rightSplitPane.width - 200
	}

	val layersController = LayersController(tileMapEditor.layersPane)

	init {

		add(realPanel, BorderLayout.CENTER)

		tileMapEditor.mapPanel.add(mapComponentScroll, GridConstraints().also { it.fill = GridConstraints.FILL_BOTH })

		tileMapEditor.zoomInButton.addActionListener { scale *= 1.5 }
		tileMapEditor.zoomOutButton.addActionListener { scale /= 1.5 }


		updatedSize()
		addComponentListener(object : ComponentAdapter() {
			override fun componentResized(e: ComponentEvent) {
				updatedSize()
			}
		})
	}
}

class LayersController(val panel: LayersPane) {
	init {
		val menu = JPopupMenu("Menu")
		menu.add("Tile Layer")
		menu.add("Object Layer")
		menu.add("Image Layer")

		panel.newButton.addActionListener {
			menu.show(panel.newButton, 0, panel.newButton.height)
		}
	}
}

class MyTileMapEditorFrame(val tmx: TiledMap) : JFrame() {
	init {
		contentPane = MyTileMapEditorPanel(tmx)
		pack()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val tmx = runBlocking { localCurrentDirVfs["samples/gfx/sample.tmx"].readTiledMap() }
			val frame = MyTileMapEditorFrame(tmx)
			frame.defaultCloseOperation = EXIT_ON_CLOSE
			frame.setLocationRelativeTo(null)
			frame.isVisible = true
		}
	}
}

inline fun Bitmap32.anyFixed(callback: (RGBA) -> Boolean): Boolean = (0 until area).any { callback(data[it]) }
inline fun Bitmap32.allFixed(callback: (RGBA) -> Boolean): Boolean = (0 until area).all { callback(data[it]) }
