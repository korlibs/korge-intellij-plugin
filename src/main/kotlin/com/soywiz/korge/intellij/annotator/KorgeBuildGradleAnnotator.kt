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
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtElement) return
        if (element.containingFile.name != "build.gradle.kts") return
        if (element !is KtCallExpression) return
        val ref = element.calleeExpression as? KtReferenceExpression ?: return
        val text = ref.getDebugText()
        if (text != "korge") return
        val context by lazy { KorgeTypeResolver(element) }
        val expressionType = context.getFqName(element)
        if (expressionType != "com.soywiz.korge.gradle.KorgeExtension") return

        holder.newAnnotation(HighlightSeverity.INFORMATION, "Korge store")
            .withFix(object : BaseIntentionAction() {
                override fun getText(): String = "Install bundles from store..."
                override fun getFamilyName(): String = text
                override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = KorgeStoreAction.openStore(project)
            })
            .gutterIconRenderer(MyGutterIconRenderer(action = KorgeStoreAction(), _icon = AllIcons.Actions.Install))
            .create()
    }

    data class MyGutterIconRenderer(val action: AnAction, val _icon: Icon) : GutterIconRenderer() {
        override fun getIcon() = _icon
        override fun getClickAction() = action
        override fun getTooltipText(): String = "Install bundles from store..."
    }
}
