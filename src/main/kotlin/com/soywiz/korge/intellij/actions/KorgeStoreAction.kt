package com.soywiz.korge.intellij.actions

import com.intellij.ide.browsers.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.project.*
import com.intellij.testFramework.*
import com.intellij.ui.jcef.*
import com.intellij.util.*
import org.cef.browser.*
import org.cef.handler.*
import org.cef.network.*

class KorgeStoreAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        openStore(e.project!!)
    }

    companion object {
        fun openStore(project: Project) {
            val medium = "store-action"
            val campaign = "no-campaign"
            val utmSuffix = "?utm_source=ide&utm_medium=$medium&utm_campaign=$campaign"
            //KorgeWebPreviewUtils.open(project, "KorGE Store", "https://store.korge.org/$utmSuffix")
            KorgeWebPreviewUtils.open(project, "KorGE Store", "http://127.0.0.1:4000/$utmSuffix")
        }
    }
}
