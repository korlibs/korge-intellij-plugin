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
            KorgeWebPreviewUtils.open(project, "KorGE Store", "https://awesome.korge.org/")
        }
    }
}
