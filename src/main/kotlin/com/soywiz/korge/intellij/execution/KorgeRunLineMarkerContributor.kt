package com.soywiz.korge.intellij.execution

import com.intellij.execution.lineMarker.*
import com.intellij.icons.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.config.*

// class KotlinRunLineMarkerContributor : RunLineMarkerContributor() {
class KorgeRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        return when {
            KorgeMainDetector.detect(element) -> {
                //Info(AllIcons.RunConfigurations.TestState.Run, { "Run SOMETHING" }, *ExecutorAction.getActions(0))
                Info(AllIcons.RunConfigurations.TestState.Run, null, *ExecutorAction.getActions(0))
            }
            else -> null
        }
    }
}
