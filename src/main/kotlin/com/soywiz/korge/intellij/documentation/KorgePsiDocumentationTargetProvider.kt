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
import korlibs.math.geom.SizeInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader


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
                val imageSizeResult = getImageSize(file)
                val originalSize = imageSizeResult.size
                val scaledSize = ScaleMode.SHOW_ALL.invoke(originalSize, SizeInt(140, 140))

                DocumentationResult.documentation(
                    // language=html
                    html = """
                        <div>
                            <div>${resourcePath}</div>
                            <img src='${file.toURI()}' width="${scaledSize.width}" height="${scaledSize.height}" />
                            <div>${imageSizeResult.formatName} : ${originalSize.width}x${originalSize.height}</div>
                        </div>                        
                    """.trimIndent()
                )
            }

        override fun dereference(): HTMLDocumentationTarget? = this
    }
}

data class ImageSizeResult(val size: SizeInt, val formatName: String)

suspend fun getImageSize(imageFile: File): ImageSizeResult {
    return withContext(Dispatchers.IO) {
        try {
            ImageIO.createImageInputStream(imageFile).use { inputStream ->
                val reader: ImageReader = ImageIO.getImageReaders(inputStream).next()
                reader.input = inputStream
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                ImageSizeResult(SizeInt(width, height), reader.formatName)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ImageSizeResult(SizeInt(0, 0), "unknown")
        }
    }
}
