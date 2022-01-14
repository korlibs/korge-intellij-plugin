package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.project.*

class KorgeStoreAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        openStore(e.project!!)
    }

    companion object {
        fun openStore(project: Project) {
            HTMLEditorProvider.openEditor(project, "KorGE Store", url = "https://awesome.korge.org/")
        }
    }
}