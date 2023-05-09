package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.soywiz.korge.intellij.actions.KorgeStoreAction
import org.jetbrains.yaml.psi.YAMLKeyValue

class DepsKProjectYmlAnnotator : Annotator {
    companion object {
        fun isDependenciesBlock(element: PsiElement): Boolean {
            if (element !is YAMLKeyValue) return false
            return element.keyText == "dependencies"
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!isDependenciesBlock(element)) return
        KorgeStoreAnnotation.annotate(element, holder)
    }
}
