package com.soywiz.korge.intellij.annotator

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readBytes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil
import com.soywiz.korge.intellij.getOrPutUserData
import com.soywiz.korge.intellij.resolver.KorgeTypeResolver
import com.soywiz.korge.intellij.util.get
import com.soywiz.korge.intellij.util.rootFile
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.plugins.notebooks.visualization.use
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon


class KorgeTypedResourceExAnnotator : Annotator {
    companion object {
        fun getElementResourcesVfsPath(element: PsiElement): String? {
            var element: PsiElement = element
            if (element is KtNameReferenceExpression) {
                element = element.mainReference.resolve() ?: return null
            }
            if (element !is KtProperty || !element.isAnnotated) return null
            val annotation = element.findAnnotation(FqName("ResourceVfsPath")) ?: return null
            val valueArgument = annotation.valueArguments.firstOrNull() as? KtValueArgument? ?: return null
            return valueArgument.text.trim('"')
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val resourcePath = KorgeTypedResourceExAnnotator.getElementResourcesVfsPath(element) ?: return
        if (resourcePath.endsWith(".png")) {
            holder.addOpenImageAnnotation(element.project, resourcePath)
        }
    }
}
