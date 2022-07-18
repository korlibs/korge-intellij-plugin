package com.soywiz.korge.intellij.editor

/*
import com.intellij.openapi.fileEditor.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.editor.formats.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*

open class KorgeFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider, com.intellij.openapi.project.DumbAware {
	companion object {
		val pluginClassLoader: ClassLoader = KorgeFileEditorProvider::class.java.classLoader
		//val pluginResurcesVfs by lazy { resourcesVfs(pluginClassLoader) }
		val pluginResurcesVfs: VfsFile get() = resourcesVfs
	}

    var myPolicy: FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    enum class AcceptKind(val hasTree: Boolean, val hasProperties: Boolean) {
        TREE_AND_PROPERTIES(true, true),
        PROPERTIES(false, true),
        NO_RIGHT_PANEL(false, false)
    }

    fun accept(virtualFile: com.intellij.openapi.vfs.VirtualFile): AcceptKind? {
        val name = virtualFile.name
        return when {
            ////////
            name.endsWith(".svg", ignoreCase = true) -> AcceptKind.NO_RIGHT_PANEL
            name.endsWith(".pex", ignoreCase = true) -> AcceptKind.PROPERTIES
            name.endsWith(".ktree", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".scml", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith("_ske.json", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            ////////
            name.endsWith(".swf", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".ani", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".voice.wav", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".voice.mp3", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".voice.ogg", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".voice.lipsync", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".wav", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".mp3", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".ogg", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".dbbin", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            name.endsWith(".skel", ignoreCase = true) -> AcceptKind.TREE_AND_PROPERTIES
            else -> null
        }
    }

    override fun accept(
        project: com.intellij.openapi.project.Project,
        virtualFile: com.intellij.openapi.vfs.VirtualFile
    ): Boolean = accept(virtualFile) != null

    override fun getPolicy(): FileEditorPolicy = myPolicy

	override fun createEditor(
		project: com.intellij.openapi.project.Project,
		virtualFile: com.intellij.openapi.vfs.VirtualFile
	): com.intellij.openapi.fileEditor.FileEditor {
        val fileToEdit = KorgeFileToEdit(virtualFile, project)
        val acceptKind = accept(virtualFile)
        virtualFile.name.endsWith(".pex", ignoreCase = true)
		return KorgeBaseKorgeFileEditor(project, fileToEdit, createModule(fileToEdit), "Preview", acceptKind?.hasTree == true)
	}

    open fun createModule(fileToEdit: KorgeFileToEdit): Module {
        val file = fileToEdit.file

        val computedExtension = when {
            file.baseName.endsWith("_ske.json") -> "dbbin"
            else -> file.extensionLC
        }

        initializeIdeaComponentFactory()

        return runBlocking {
            try {
                when (computedExtension) {
                    "dbbin" -> dragonBonesEditor(file)
                    "skel" -> spineEditor(file)
                    "tmx" -> createModule(null) { tiledMapEditor(file) }
                    "svg" -> createModule(null) { sceneView += VectorImage(file.readSVG()).also {
                        //it.useNativeRendering = false
                    } }
                    "pex" -> particleEmiterEditor(file)
                    "ktree" -> ktreeEditor(fileToEdit)
                    "wav", "mp3", "ogg", "lipsync" -> createModule(null) { audioFileEditor(file) }
                    "swf", "ani" -> swfAnimationEditor(file)
                    else -> createModule(null) { }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                createModule(null) {
                    sceneView.text("Error: ${e.message}").centerOnStage()
                }
            }
        }
    }

	override fun getEditorTypeId(): String = this::class.java.name
}

data class BlockToExecute(val block: suspend EditorScene.() -> Unit)
*/
