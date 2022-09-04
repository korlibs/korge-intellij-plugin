package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.soywiz.korge.intellij.util.LibraryFixer

class KorgeFixGradleMavenLocalSourcesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("Running FixGradleMavenLocalSourcesAction")
        LibraryFixer.fixLibraries(e.project, true)
    }
}
