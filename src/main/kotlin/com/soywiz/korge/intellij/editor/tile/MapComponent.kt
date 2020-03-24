package com.soywiz.korge.intellij.editor.tile

import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import java.awt.image.*
import javax.swing.*


class MapComponent(val tmx: TiledMap) : JComponent() {
	val downRightTileSignal = Signal<PointInt>()
	val onZoom = Signal<Int>()
	val upTileSignal = Signal<PointInt>()
	val downTileSignal = Signal<PointInt>()
	val middleTileSignal = Signal<PointInt>()
	val overTileSignal = Signal<PointInt>()
	val outTileSignal = Signal<PointInt>()

	fun mapRepaint(invalidateBitmapCache: Boolean) {
		if (invalidateBitmapCache) {
			cachedBitmapKey = null
		}
		updateSize()
		revalidate()
		parent?.revalidate()
		parent?.repaint()
	}

	var scale: Double = 2.0
		set(value) {
			field = value
			mapRepaint(false)
		}
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

	init {
		updateSize()
		addMouseMotionListener(object : MouseMotionAdapter() {
			override fun mouseDragged(e: MouseEvent) {
				//println("mouseDragged: $e")
				when {
					SwingUtilities.isLeftMouseButton(e) -> onPressMouse(e.point)
					SwingUtilities.isMiddleMouseButton(e) -> Unit
					else -> Unit
				}
			}

			override fun mouseMoved(e: MouseEvent) {
				//println("mouseMoved: $e")
				overTileSignal(getTileIndex(e.point))
			}

		})
		addMouseWheelListener { e ->
			if (e.isControlDown) {
				//val dir = e.wheelRotation
				//println("mouseWheelMoved: $e")
				onZoom(-e.wheelRotation)
			} else {
				parent.dispatchEvent(e)
			}
		}
		addMouseListener(object : MouseAdapter() {
			override fun mouseExited(e: MouseEvent) {
				outTileSignal(getTileIndex(e.point))
			}

			override fun mouseReleased(e: MouseEvent) {
				if (e.button == MouseEvent.BUTTON1) {
					upTileSignal(getTileIndex(e.point))
				}
			}

			override fun mousePressed(e: MouseEvent) {
				//println("mousePressed: $e")
				when {
					SwingUtilities.isLeftMouseButton(e) -> onPressMouse(e.point)
					SwingUtilities.isMiddleMouseButton(e) -> onMiddleMouse(e.point)
					else -> onRightPressMouse(e.point)
				}
			}
		})
	}

	var currentTileSelected = 1

	fun onMiddleMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		middleTileSignal(tileIndex)
	}

	fun onPressMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		downTileSignal(tileIndex)
		/*
		tmx.patternLayers[0].map[tileIndex.x, tileIndex.y] =
            RGBA(currentTileSelected)
		repaint()
		*/
		//println(tileIndex)
	}

	fun onRightPressMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		downRightTileSignal(tileIndex)
		//currentTileSelected = tmx.patternLayers[0].map[tileIndex.x, tileIndex.y].value
	}

	fun updateSize() {
		this.preferredSize =
            Dimension((tmx.pixelWidth * scale).toInt(), (tmx.pixelHeight * scale).toInt())
	}

	fun getTileIndex(coords: Point): PointInt =
		PointInt(
            (coords.x / tmx.tilewidth / scale).toInt(),
            (coords.y / tmx.tileheight / scale).toInt()
        )

	data class CacheKey(
		val offsetX: Int, val offsetY: Int,
		val displayTilesX: Int, val displayTilesY: Int,
		val TILE_WIDTH: Int, val TILE_HEIGHT: Int
	)
	private var cachedBitmapKey: CacheKey? = null
	private var cachedBitmap: BufferedImage? = null

	override fun paintComponent(g: Graphics) {
		val g = (g as Graphics2D)

		val TILE_WIDTH = tmx.tilewidth
		val TILE_HEIGHT = tmx.tileheight

		g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
		val clipBounds = g.clipBounds
		val displayTilesX = ((clipBounds.width / TILE_WIDTH / scale) + 3).toInt()
		val displayTilesY = ((clipBounds.height / TILE_HEIGHT / scale) + 3).toInt()
		val offsetX = (clipBounds.x / TILE_WIDTH / scale).toInt()
		val offsetY = (clipBounds.y / TILE_HEIGHT / scale).toInt()

		val cacheKey = CacheKey(
			offsetX = offsetX, offsetY = offsetY,
			displayTilesX = displayTilesX,
			displayTilesY = displayTilesY,
			TILE_WIDTH = TILE_WIDTH, TILE_HEIGHT = TILE_WIDTH
		)

		if (cachedBitmapKey != cacheKey) {
			//println("REPAINT: $cachedBitmapKey != $cacheKey")
			cachedBitmapKey = cacheKey
			val temp = Bitmap32(
				(displayTilesX * TILE_WIDTH),
				(displayTilesY * TILE_HEIGHT)
			)
			for (layer in tmx.allLayers) {
				if (!layer.visible) continue
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
			cachedBitmap = temp.toAwt()
		}

		//val oldTransform = g.transform
		val oldTransform = g.transform
		g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)
		g.scale(scale, scale)
		g.drawImage(cachedBitmap!!, 0, 0, null)

		//g.transform = oldTransform

		//g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)

		/*
		g.preserveStroke {
			g.color = Color.DARK_GRAY
			g.stroke = BasicStroke((1f / scale).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(2f), 0f)
			for (y in 0 until displayTilesY) g.drawLine(0, y * TILE_HEIGHT, displayTilesX * TILE_WIDTH, y * TILE_HEIGHT)
			for (x in 0 until displayTilesX) g.drawLine(x * TILE_WIDTH, 0, x * TILE_WIDTH, displayTilesY * TILE_HEIGHT)
		}
		 */

		g.transform = oldTransform
		g.scale(scale, scale)

		selected?.let { selected ->
			g.preserveStroke {
				g.stroke = BasicStroke((2f / scale).toFloat())
				g.color = Color.RED
				g.drawRect(
						selected.x * TILE_WIDTH,
						selected.y * TILE_HEIGHT,
						selected.width * TILE_WIDTH,
						selected.height * TILE_HEIGHT
				)
			}
		}
	}

	var selected: Rectangle? = null
	fun selectedRange(x: Int, y: Int, width: Int = 1, height: Int = 1) {
		selected = Rectangle(x, y, width, height)
		mapRepaint(false)
	}
	fun unselect() {
		selected = null
		mapRepaint(false)
	}
}