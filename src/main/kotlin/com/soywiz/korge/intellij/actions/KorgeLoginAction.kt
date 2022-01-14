package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.soywiz.korge.intellij.config.korgeGlobalPrivateSettings
import kotlinx.coroutines.*
import java.awt.*
import javax.swing.*

class KorgeLoginAction : AnAction(), DumbAware {
    override fun update(e: AnActionEvent) {
        //e.presentation.text = "_something"
        //println("KorgeLoginAction.update")
        super.update(e)
        e.presentation.isVisible = !korgeGlobalPrivateSettings.isUserLoggedIn()
    }

    override fun actionPerformed(e: AnActionEvent) {
        korgeGlobalPrivateSettings.login(e.project)
    }
}

class KorgeLogoutAction : AnAction(), DumbAware {
    override fun update(e: AnActionEvent) {
        e.presentation.text = "_Logout ${korgeGlobalPrivateSettings.userLogin} (${korgeGlobalPrivateSettings.userRank})"
        //println("KorgeLoginAction.update")
        super.update(e)
        e.presentation.isVisible = korgeGlobalPrivateSettings.isUserLoggedIn()
        e.presentation.icon = korgeGlobalPrivateSettings.getAvatarIcon()
    }

    override fun actionPerformed(e: AnActionEvent) {
        korgeGlobalPrivateSettings.logout(e.project)
    }
}
