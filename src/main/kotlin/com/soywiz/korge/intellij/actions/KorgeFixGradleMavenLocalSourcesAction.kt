package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korge.intellij.util.fixLibraries
import com.soywiz.korge.intellij.util.runWriteAction



class KorgeFixGradleMavenLocalSourcesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("Running FixGradleMavenLocalSourcesAction")
        fixLibraries(e.project)
    }
}
