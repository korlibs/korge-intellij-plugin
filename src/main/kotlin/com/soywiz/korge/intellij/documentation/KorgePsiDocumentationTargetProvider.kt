package com.soywiz.korge.intellij.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.soywiz.korge.intellij.annotator.KorgeTypedResourceExAnnotator
import com.soywiz.korge.intellij.annotator.getResourceVirtualFile
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata


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
                val imageSize = getImageSize(file)
                val realSize = ScaleMode.SHOW_ALL.invoke(imageSize, Size(140, 140))

                DocumentationResult.documentation(
                    html = "<div><div>${resourcePath}</div><img src='${file.toURI()}' width=\"${realSize.width.toInt()}\" height=\"${realSize.height.toInt()}\" /></div>"
                )
            }

        override fun dereference(): HTMLDocumentationTarget? = this
    }
}

suspend fun getImageSize(imageFile: File): Size {
    return withContext(Dispatchers.IO) {
        try {
            ImageIO.createImageInputStream(imageFile).use { inputStream ->
                val reader: ImageReader = ImageIO.getImageReaders(inputStream).next()
                reader.input = inputStream
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                Size(width, height)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Size(0, 0)
        }
    }
}
