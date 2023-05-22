package com.soywiz.korge.intellij.editor

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korau.sound.readMusic
import com.soywiz.korge.intellij.toVfs
import com.soywiz.korge.intellij.util.onClick
import kotlinx.coroutines.runBlocking
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AudioFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, virtualFile: VirtualFile): Boolean {
        val name = virtualFile.name
        return when {
            name.endsWith(".wav", ignoreCase = true) -> true
            name.endsWith(".mp3", ignoreCase = true) -> true
            name.endsWith(".mp3", ignoreCase = true) -> true
            else -> false
        }
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return object : FileEditorBase() {
            override fun getFile(): VirtualFile = file

            var playButton: JButton
            var panel = JPanel().also { panel ->
                panel.add(JButton("Play").also {
                    playButton = it
                    it.onClick {
                        runBlocking {
                            file.toVfs().readMusic().play()
                        }
                    }
                })
            }

            override fun getComponent(): JComponent = panel
            override fun getName(): String = "KorauEditor"
            override fun getPreferredFocusedComponent(): JComponent? = playButton
        }
    }

    override fun getEditorTypeId(): String = "KORAU_TYPE_ID"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
