package com.soywiz.korge.intellij.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.soywiz.korge.intellij.annotator.KorgeTypedResourceExAnnotator
import com.soywiz.korge.intellij.annotator.getResourceVirtualFile
import java.io.File

class KorgePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        //println("KorgePsiDocumentationTargetProvider.documentationTarget: $element, ${element.text}")
        val resourcePath = KorgeTypedResourceExAnnotator.getElementResourcesVfsPath(element) ?: return null
        //println(" -> $resourcePath")
        if (!resourcePath.endsWith(".png")) return null
        val virtualFile = element.project.getResourceVirtualFile(resourcePath) ?: return null
        //println(" -> $virtualFile")
        val file = virtualFile.toNioPath().toFile()
        //println(" -> $file")
        return HTMLDocumentationTarget(file, resourcePath)
    }

    class HTMLDocumentationTarget(
        val file: File,
        val resourcePath: String
    ) : DocumentationTarget, Pointer<HTMLDocumentationTarget> {
        override fun computePresentation(): TargetPresentation =
            TargetPresentation.builder("${file.name}").presentation()

        override fun createPointer(): Pointer<out DocumentationTarget> = this

        override fun computeDocumentation(): DocumentationResult? =
            DocumentationResult.asyncDocumentation {
                DocumentationResult.documentation(
                    html = "<div><div>${resourcePath}</div><img src='${file.toURI()}' width=\"140\" /></div>"
                )

            }

        override fun dereference(): HTMLDocumentationTarget? = this
    }
}


