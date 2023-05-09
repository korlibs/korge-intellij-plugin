package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.soywiz.korge.intellij.KorgeIcons
import com.soywiz.korge.intellij.actions.KorgeStoreAction

object KorgeStoreAnnotation {
    fun annotate(element: PsiElement, holder: AnnotationHolder) {
        holder.newAnnotation(HighlightSeverity.INFORMATION, "Korge store")
            .withFix(object : BaseIntentionAction() {
                override fun getText(): String = "Install bundles from store..."
                override fun getFamilyName(): String = text
                override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = KorgeStoreAction.openStore(project)
            })
            .gutterIconRenderer(
                KorgeBuildGradleAnnotator.MyGutterIconRenderer(
                    action = KorgeStoreAction(),
                    //_icon = AllIcons.Actions.Install
                    _icon = KorgeIcons.JITTO
                )
            )
            .create()
    }
}
