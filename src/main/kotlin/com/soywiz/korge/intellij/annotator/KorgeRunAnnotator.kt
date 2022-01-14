package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.*
import com.intellij.execution.actions.*
import com.intellij.execution.configurations.*
import com.intellij.icons.*
import com.intellij.ide.ui.laf.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.execution.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.plugins.gradle.service.execution.*
import javax.swing.*

// https://jetbrains.design/intellij/resources/icons_list/
/*
class KorgeRunAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtElement) return

        if (element is KtNamedFunction) {
            if (element.name == "main") {
                val renderer = KorgeRunAnnotationGutterIconRenderer(element)
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Run")
                    .withFix(object : BaseIntentionAction() {
                        override fun getText(): String = "Run JVM"
                        override fun getFamilyName(): String = text
                        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {

                            GradleRunConfiguration(project, JvmKorgeRunConfigurationProducer().configurationFactory, "RunJVM")
                        }
                    })
                        /*
                    .withFix(object : BaseIntentionAction() {
                        override fun getText(): String = "Run JS"
                        override fun getFamilyName(): String = text
                        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
                        }
                    })

                         */
                    .gutterIconRenderer(renderer)
                    .create()
            }
        }
    }
}

object Demo {
    @JvmStatic
    fun main(args: Array<String>) {

    }
}

data class KorgeRunAnnotationGutterIconRenderer(val element: KtNamedFunction) : GutterIconRenderer() {
    //override fun getIcon(): Icon = AllIcons.Actions.Execute
    override fun getIcon(): Icon = AllIcons.Nodes.RunnableMark
}
 */
