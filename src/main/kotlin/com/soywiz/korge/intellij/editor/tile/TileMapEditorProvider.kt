package com.soywiz.korge.intellij.editor.tile

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korge.intellij.toVfs
import kotlinx.coroutines.runBlocking
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JFrame

class TileMapEditorProvider : FileEditorProvider, DumbAware {
	override fun getEditorTypeId(): String = this::class.java.name
	override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

	override fun accept(
		project: Project,
		virtualFile: VirtualFile
	): Boolean {
		val name = virtualFile.name
		return when {
			name.endsWith(".tmx", ignoreCase = true) -> true
			else -> false
		}
	}

	override fun createEditor(project: Project, file: VirtualFile): FileEditor {
		val tmxFile = file.toVfs()
		val tmx = runBlocking { tmxFile.readTiledMap() }

		return object : FileEditorBase(), DumbAware {
			val panel by lazy { MyTileMapEditorPanel(tmx) }

			override fun isModified(): Boolean {
				return false
			}

			override fun getName(): String = "Editor"

			override fun setState(state: FileEditorState) {
			}

			override fun getComponent(): JComponent = panel

			override fun getPreferredFocusedComponent(): JComponent? = null

			override fun <T : Any?> getUserData(key: Key<T>): T? {
				return null
			}

			override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
			}

			override fun getCurrentLocation(): FileEditorLocation? {
				return null
			}

			override fun isValid(): Boolean = true

			override fun addPropertyChangeListener(listener: PropertyChangeListener) {
			}

			override fun removePropertyChangeListener(listener: PropertyChangeListener) {
			}

			override fun dispose() {
			}
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val frame = JFrame()
			frame.isVisible = true
		}
	}
}