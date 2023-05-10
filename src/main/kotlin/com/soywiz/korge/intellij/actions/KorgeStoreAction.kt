package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.soywiz.korge.intellij.korge
import com.soywiz.korge.intellij.util.isDevelopmentMode

class KorgeStoreAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        openStore(e.project ?: return)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isVisible = project.korge.containsKorge
    }

    companion object {
        fun openStore(project: Project) {
            val medium = "store-action"
            val campaign = "no-campaign"
            val utmSuffix = "?utm_source=ide&utm_medium=$medium&utm_campaign=$campaign"

            KorgeWebPreviewUtils.open(project, "KorGE Store", "$STORE_PREFIX/$utmSuffix")
        }
    }
}

val STORE_PREFIX get() = when {
    isDevelopmentMode -> "http://127.0.0.1:4000"
    else -> "https://store.korge.org"
}
