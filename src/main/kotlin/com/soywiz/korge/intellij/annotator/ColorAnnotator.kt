package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.ui.*
import com.intellij.util.ui.*
import korlibs.datastructure.*
import com.soywiz.korge.intellij.resolver.*
import com.soywiz.korge.intellij.util.*
import korlibs.image.color.*
import korlibs.image.color.Colors
import org.jetbrains.kotlin.psi.*
import java.awt.*
import javax.swing.*

class ColorAnnotator: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtElement) return

        val leftExpression = when (element) {
            is KtDotQualifiedExpression -> element.receiverExpression
            is KtArrayAccessExpression -> element.arrayExpression
            else -> return
        }
        val leftExpressionText = leftExpression?.text

        if (leftExpressionText != Colors::class.simpleName && leftExpressionText != Colors::class.qualifiedName) {
            return
        }

        fun gutter(rgba: RGBA) {
            val color = rgba.toAwt()
            val renderer = GutterColorRenderer(element, color)
            holder.newAnnotation(HighlightSeverity.INFORMATION, "Color")
                .withFix(object : BaseIntentionAction() {
                    override fun getText(): String = "Choose color..."
                    override fun getFamilyName(): String = text
                    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = renderer.chooseColor()
                })
                .gutterIconRenderer(renderer)
                .create()
        }

        val context by lazy { KorgeTypeResolver(element) }

        //val annotationSessions = holder.currentAnnotationSession

        when (element) {
            is KtDotQualifiedExpression -> {
                val receiverExpression = element.receiverExpression
                val selectorExpression = element.selectorExpression
                val typeSelector by lazy { context.getFqName(selectorExpression) }
                val typeReceiver by lazy { context.getFqName(receiverExpression) }

                if (typeReceiver == Colors::class.java.name && typeSelector == RGBA::class.java.name) {
                    if (selectorExpression is KtNameReferenceExpression) {
                        try {
                            gutter(Colors[selectorExpression.getReferencedName()])
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            is KtArrayAccessExpression -> {
                //println("KtArrayAccessExpression[0]")
                if (element.indexExpressions.size == 1) {
                    //println("KtArrayAccessExpression[1]")
                    val indexExpression = element.indexExpressions.firstOrNull()
                    if (indexExpression is KtStringTemplateExpression && !indexExpression.hasInterpolation()) {
                        //println("KtArrayAccessExpression[2]")
                        val indexEntries = indexExpression.entries
                        if (indexEntries.size == 1 && indexEntries[0] is KtLiteralStringTemplateEntry) {
                            //println("KtArrayAccessExpression[3]")
                            val indexEntry = indexEntries[0] as KtLiteralStringTemplateEntry
                            val indexText = indexEntry.text
                            val arrayExpression = element.arrayExpression
                            val typeArray = context.getFqName(arrayExpression)
                            //println("KtArrayAccessExpression[4]: $typeArray")
                            if (typeArray == Colors::class.java.name) {
                                //println("KtArrayAccessExpression[5]")
                                try {
                                    gutter(Colors[indexText])
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val COLORS_TO_NAMES = Colors.colorsByName.flip()

data class GutterColorRenderer(val element: PsiElement, val color: Color): GutterIconRenderer() {
    //override fun getIcon(): Icon = ColorIcon(if (UIUtil.isRetina()) 24 else 12, color, true)
    override fun getIcon(): Icon = ColorIcon(12, color, true)

    fun chooseColor() {
        val editor = PsiEditorUtilBase.findEditorByPsiElement(element) ?: return
        val newColor = ColorChooser.chooseColor(editor.component, "Choose Color", color, true, true)
        if (newColor != null) {
            val rgba = newColor.toRgba()
            val colorName = COLORS_TO_NAMES[rgba]
            val replacement = when {
                colorName != null -> "Colors.${colorName.toUpperCase()}"
                rgba.a == 0xFF -> "Colors[\"${rgba.hexStringNoAlpha}\"]"
                else -> "Colors[\"${rgba.hexString}\"]"
            }
            element.replace(replacement)
        }
    }

    override fun getClickAction(): AnAction? {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                chooseColor()
            }
        }
    }
}
