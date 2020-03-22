package com.soywiz.korge.intellij.editor.tile

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korge.intellij.editor.HistoryManager
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
		val history = HistoryManager()
		val undoManager = UndoManager.getInstance(project)
		val fileEditor = object : FileEditorBase(), DumbAware {
			val panel by lazy { MyTileMapEditorPanel(tmx, history, registerHistoryShortcuts = false) }
			override fun isModified(): Boolean = panel.history.isModified
			override fun getName(): String = "Editor"
			override fun setState(state: FileEditorState) = Unit
			override fun getComponent(): JComponent = panel
			override fun getPreferredFocusedComponent(): JComponent? = null
			override fun <T : Any?> getUserData(key: Key<T>): T? = null
			override fun <T : Any?> putUserData(key: Key<T>, value: T?) = Unit
			override fun getCurrentLocation(): FileEditorLocation? = null
			override fun isValid(): Boolean = true
			override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
			override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
			override fun dispose() = Unit
		}

		val refs = arrayOf(DocumentReferenceManager.getInstance().create(file))
		history.onAdd { entry ->
			CommandProcessor.getInstance().executeCommand(project, {
				undoManager.undoableActionPerformed(object : UndoableAction {
					override fun redo() {
						history.moveTo(entry.cursor)
					}
					override fun undo() {
						history.moveTo(entry.cursor - 1)
					}
					//override fun isGlobal(): Boolean = false
					//override fun getAffectedDocuments() = refs
					override fun isGlobal(): Boolean = true
					override fun getAffectedDocuments() = refs
				})
			}, entry.name, "tilemap", UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION, refs[0].document)

		}

		return fileEditor
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val frame = JFrame()
			frame.isVisible = true
		}
	}
}