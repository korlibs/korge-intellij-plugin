package com.soywiz.korge.intellij.image

import com.intellij.ide.browsers.actions.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*

class QoiEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file is WebPreviewVirtualFile
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return WebPreviewFileEditor(project, (file as WebPreviewVirtualFile))
    }

    override fun getEditorTypeId(): String {
        return "web-preview-editor"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}

