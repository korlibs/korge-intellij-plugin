package com.soywiz.korge.intellij.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.soywiz.korge.intellij.documentation.isPathForPreviewResource
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.util.isAnnotated


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
        val resourcePath = getElementResourcesVfsPath(element) ?: return
        if (isPathForPreviewResource(resourcePath)) {
            holder.addOpenResourceAnnotation(element.project, resourcePath)
        }
    }
}
