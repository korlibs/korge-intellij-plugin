package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.*
import com.intellij.icons.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.actions.*
import com.soywiz.korge.intellij.resolver.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.*
import javax.swing.*

class KorgeBuildGradleAnnotator : Annotator {
    companion object {
        fun isKorgeBlock(element: PsiElement): Boolean {
            if (element !is KtElement) return false
            if (element.containingFile.name != "build.gradle.kts") return false
            if (element !is KtCallExpression) return false
            val ref = element.calleeExpression as? KtReferenceExpression ?: return false
            val text = ref.getDebugText()
            if (text != "korge") return false
            val context by lazy { KorgeTypeResolver(element) }
            val expressionType = context.getFqName(element)
            return !(expressionType != "com.soywiz.korge.gradle.KorgeExtension"
                && expressionType != "korlibs.korge.gradle.KorgeExtension")
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!isKorgeBlock(element)) return
        KorgeStoreAnnotation.annotate(element, holder)
    }

    data class MyGutterIconRenderer(val action: AnAction, val _icon: Icon) : GutterIconRenderer() {
        override fun getIcon() = _icon
        override fun getClickAction() = action
        override fun getTooltipText(): String = "Install bundles from store..."
    }
}
