package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.soywiz.korge.intellij.config.korgeGlobalPrivateSettings
import com.soywiz.korge.intellij.korge
import kotlinx.coroutines.*
import java.awt.*
import javax.swing.*

class KorgeLoginAction : AnAction(), DumbAware {
    override fun update(e: AnActionEvent) {
        //e.presentation.text = "_something"
        //println("KorgeLoginAction.update")
        super.update(e)
        e.presentation.isVisible = !korgeGlobalPrivateSettings.isUserLoggedIn() && e.project?.korge?.containsKorge == true
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
        e.presentation.isVisible = korgeGlobalPrivateSettings.isUserLoggedIn() && e.project?.korge?.containsKorge == true
        e.presentation.icon = korgeGlobalPrivateSettings.getAvatarIcon()
    }

    override fun actionPerformed(e: AnActionEvent) {
        korgeGlobalPrivateSettings.logout(e.project)
    }
}
