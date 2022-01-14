package com.soywiz.korge.intellij.execution

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.config.*
import org.jetbrains.kotlin.psi.*

object KorgeMainDetector {
    fun detect(element: PsiElement): Boolean {
        //return (element is LeafPsiElement && element.parent is KtNamedFunction && element.text == "fun" && element.nextSibling.text == "main")
        val parent = element.parent
        if (parent !is KtNamedFunction) return false
        if (element !is LeafPsiElement) return false
        if (element.text != "main") return false
        if (parent.name != "main") return false
        if (!element.project.korge.containsKorge) return false
        if (!element.project.hasAccessToEarlyPreviewFeatures()) return false
        return true
    }
}