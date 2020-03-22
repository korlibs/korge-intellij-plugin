package com.soywiz.korge.intellij.editor.tile

import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.soywiz.korge.intellij.ui.KorgePropertyTable
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.*
import javax.swing.*

class MyTileMapEditorPanel(val tmx: TiledMap) : JPanel(BorderLayout()) {
	val tileMapEditor = TileMapEditor()


	val realPanel = tileMapEditor.contentPanel

	val mapComponent = MapComponent(tmx)

	var scale: Double
		get() = mapComponent.scale
		set(value) = run { mapComponent.scale = value }

	val mapComponentScroll = JBScrollPane(mapComponent).also { scroll ->
		//scroll.verticalScrollBar.unitIncrement = 16
	}

	fun updatedSize() {
		tileMapEditor.leftSplitPane.dividerLocation = 200
		tileMapEditor.rightSplitPane.dividerLocation = tileMapEditor.rightSplitPane.width - 200
	}

	val layersController = LayersController(tileMapEditor.layersPane)
	val propertiesController = PropertiesController(tileMapEditor.propertiesPane)

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

class PropertiesController(val panel: PropertiesPane) {
	var width = 100
	var height = 100
	val propertyTable = KorgePropertyTable(KorgePropertyTable.Properties().register(::width,::height)).also {
		panel.tablePane.add(JScrollPane(it), BorderLayout.CENTER)
	}
}

class LayersController(val panel: LayersPane) {
	init {
		val menu = JPopupMenu("Menu").apply {
			add("Tile Layer")
			add("Object Layer")
			add("Image Layer")
		}

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
