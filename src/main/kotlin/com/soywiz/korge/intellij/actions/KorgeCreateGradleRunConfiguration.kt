package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.soywiz.korge.intellij.util.createGradleRunConfiguration

open class KorgeCreateGradleRunConfiguration : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        createGradleRunConfiguration(e.project!!, "runJvmAutoreload")
    }
}
