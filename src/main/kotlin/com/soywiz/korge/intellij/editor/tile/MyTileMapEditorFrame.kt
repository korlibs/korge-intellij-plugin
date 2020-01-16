package com.soywiz.korge.intellij.editor.tile

import com.intellij.uiDesigner.core.*
import com.soywiz.korge.tiled.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.image.*
import javax.swing.*

class MyTileMapEditorPanel(val tmx: TiledMap) : JPanel(BorderLayout()) {
	val tileMapEditor = TileMapEditor()

	val maxTileGid = tmx.tilesets.map { it.firstgid + it.tileset.textures.size }.max() ?: 0

	data class TileInfo(val image: Bitmap32, val awt: BufferedImage, val area: RectangleInt) {
		val miniImage = awt.getSubimage(area.x, area.y, area.width, area.height)
		val miniBmp32 = image.copySliceWithSize(area.x, area.y, area.width, area.height)
		val miniSlice = miniBmp32.sliceWithSize(0, 0, area.width, area.height)
	}

	private val emptyImage = Bitmaps.transparent.bmp
	private val emptyImageAwt = emptyImage.toAwt()
	val dummyTile = TileInfo(emptyImage, emptyImageAwt, Bitmaps.transparent.bounds)
	val tiles: Array<TileInfo> = Array(maxTileGid) { dummyTile }.also { tiles ->
		for (tileset in tmx.tilesets) {
			val tex = tileset.tileset.base.toBMP32()
			val tex2 = tex.toAwt()
			for (tileIdx in tileset.tileset.textures.indices) {
				val tile = tileset.tileset.textures[tileIdx]
				if (tile != null) {
					tiles[tileset.firstgid + tileIdx] = TileInfo(tex, tex2, (tile as BitmapSlice<*>).bounds)
				}
			}
		}
	}

	val realPanel = tileMapEditor.contentPanel

	init {
		add(realPanel, BorderLayout.CENTER)

		tileMapEditor.mapPanel.add(JScrollPane(object : JComponent() {
			init {
				this.preferredSize = Dimension(tmx.pixelWidth, tmx.pixelHeight)
			}

			override fun paintComponent(g: Graphics) {
				val g = (g as Graphics2D)

				val TILE_WIDTH = tmx.tilewidth
				val TILE_HEIGHT = tmx.tileheight

				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				val clipBounds = g.clipBounds
				val displayTilesX = (clipBounds.width / TILE_WIDTH) + 2
				val displayTilesY = (clipBounds.height / TILE_HEIGHT) + 2
				val temp = Bitmap32(displayTilesX * TILE_WIDTH, displayTilesY * TILE_HEIGHT)

				val offsetX = clipBounds.x / TILE_WIDTH
				val offsetY = clipBounds.y / TILE_HEIGHT

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
									val tile = tiles.getOrElse(tileIdx) { dummyTile }
									val px0 = x * TILE_WIDTH
									val py0 = y * TILE_HEIGHT
									val px = rx * TILE_WIDTH
									val py = ry * TILE_HEIGHT

									temp._draw(tile.miniSlice, px0, py0, mix = true)
									//temp.draw(tile.miniBmp32, px0, py0)
									//g.drawImage(tile.miniImage, px, py, null)
									/*
									g.drawImage(
										tile.image,
										px, py, px + TILE_WIDTH, py + TILE_HEIGHT,
										tile.area.left, tile.area.top, tile.area.right, tile.area.bottom,
										null
									)
									 */
									//g.color = Color.RED
									//g.drawRect(px, py, TILE_WIDTH, TILE_HEIGHT)
								}
							}
						}
					}
				}

				g.drawImage(temp.toAwt(), offsetX * TILE_WIDTH, offsetY * TILE_HEIGHT, null)

				for (x in 0 until displayTilesX) {
					for (y in 0 until displayTilesY) {
						val rx = x + offsetX
						val ry = y + offsetY
						val px = rx * TILE_WIDTH
						val py = ry * TILE_HEIGHT
						g.color = Color.BLACK
						g.drawRect(px, py, TILE_WIDTH, TILE_HEIGHT)
					}
				}
			}
		}), GridConstraints().also { it.fill = GridConstraints.FILL_BOTH })

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
