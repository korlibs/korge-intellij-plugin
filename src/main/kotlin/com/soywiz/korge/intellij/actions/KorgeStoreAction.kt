package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.soywiz.korge.intellij.util.isDevelopmentMode

class KorgeStoreAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        openStore(e.project!!)
    }

    companion object {
        fun openStore(project: Project) {
            val medium = "store-action"
            val campaign = "no-campaign"
            val utmSuffix = "?utm_source=ide&utm_medium=$medium&utm_campaign=$campaign"

            KorgeWebPreviewUtils.open(project, "KorGE Store", when {
                isDevelopmentMode -> "http://127.0.0.1:4000/$utmSuffix"
                else -> "https://store.korge.org/$utmSuffix"
            })
        }
    }
}
