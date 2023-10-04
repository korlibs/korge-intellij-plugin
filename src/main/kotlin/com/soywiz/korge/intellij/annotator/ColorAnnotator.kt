package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.util.ui.*
import korlibs.datastructure.*
import com.soywiz.korge.intellij.resolver.*
import com.soywiz.korge.intellij.util.*
import korlibs.image.color.*
import korlibs.image.color.Colors
import org.jetbrains.kotlin.idea.core.resolveType
import org.jetbrains.kotlin.psi.*
import java.awt.*
import javax.swing.*
import kotlin.reflect.full.memberProperties

class ColorAnnotator: Annotator {
    companion object {
        private val materialColors: Map<String, RGBA?> = MaterialColors::class.memberProperties.toList().associate {
            it.name to (it.getValue(MaterialColors, it) as? RGBA?)
        }

        fun colorNameToRGBA(name: String?): RGBA? = try {
            //println("colorNameToRGBA:name=$name, materialColors=$materialColors")
            materialColors[name] ?: (if (name != null) Colors[name] else null)
        } catch (e: Throwable) {
            null
        }

        fun tryGetColorFromElement(element: PsiElement): RGBA? {
            if (element is KtProperty) {
                val rtype = element.typeReference?.text
                //val rtype = if (element.typeReference?.text == null) {
                //    val context by lazy { KorgeTypeResolver(element) }
                //    context.getFqName(element)
                //} else {
                //    element.typeReference?.text
                //}


                //println("${element.text} == type=${rtype}")


                if (rtype == "korlibs.image.color.RGBA") {
                    try {
                        return colorNameToRGBA(element.name)
                    } catch (e: Throwable) {

                    }
                }

            }

            if (element !is KtElement) return null

            val leftExpression = when (element) {
                is KtDotQualifiedExpression -> element.receiverExpression
                is KtArrayAccessExpression -> element.arrayExpression
                else -> return null
            }
            val leftExpressionText = leftExpression?.text

            if (
                leftExpressionText != Colors::class.simpleName
                && leftExpressionText != Colors::class.qualifiedName
                && leftExpressionText != MaterialColors::class.simpleName
                && leftExpressionText != MaterialColors::class.qualifiedName
                ) {
                return null
            }

            val context by lazy { KorgeTypeResolver(element) }

            //val annotationSessions = holder.currentAnnotationSession

            when (element) {
                is KtDotQualifiedExpression -> {
                    val receiverExpression = element.receiverExpression
                    val selectorExpression = element.selectorExpression
                    val typeSelector by lazy { context.getFqName(selectorExpression) }
                    val typeReceiver by lazy { context.getFqName(receiverExpression) }

                    if (
                        typeSelector == RGBA::class.java.name &&
                        (typeReceiver == Colors::class.java.name || typeReceiver == MaterialColors::class.java.name)
                    ) {
                        if (selectorExpression is KtNameReferenceExpression) {
                            try {
                                return colorNameToRGBA(selectorExpression.getReferencedName())
                                //gutter()
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
                                        return colorNameToRGBA(indexText)
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val rgba = tryGetColorFromElement(element) ?: return

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
}

private val COLORS_TO_NAMES = Colors.colorsByName.flip()

data class GutterColorRenderer(val element: PsiElement, val color: Color): GutterIconRenderer() {
    //override fun getIcon(): Icon = ColorIcon(if (UIUtil.isRetina()) 24 else 12, color, true)
    override fun getIcon(): Icon = ColorIcon(12, color, true)

    fun chooseColor() {
        val editor = PsiEditorUtil.getInstance().findEditorByPsiElement(element) ?: return
        val newColor = editor.chooseColor(color)
        if (newColor != null) {
            val rgba = newColor.toRgba()
            val colorName = COLORS_TO_NAMES[rgba]
            val replacement = when {
                colorName != null -> "Colors.${colorName.uppercase()}"
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
