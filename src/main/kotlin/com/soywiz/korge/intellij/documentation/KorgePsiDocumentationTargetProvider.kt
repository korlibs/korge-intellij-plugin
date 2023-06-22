package com.soywiz.korge.intellij.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationContent
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.soywiz.korge.intellij.annotator.ColorAnnotator
import com.soywiz.korge.intellij.annotator.KorgeTypedResourceExAnnotator
import com.soywiz.korge.intellij.annotator.getFontPreview
import com.soywiz.korge.intellij.annotator.getResourceVirtualFile
import com.soywiz.korge.intellij.util.getScaledInstance
import korlibs.datastructure.iterators.fastForEach
import korlibs.image.color.RGBA
import korlibs.image.color.toAwt
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.SizeInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader

enum class ResourceType(val extensions: Set<String>) {
    IMAGE("png", "jpg", "qoi", "ase"),
    FONT("ttf", "otf");

    constructor(vararg extensions: String) : this(extensions.toSet())

    fun pathHashExtension(path: String): Boolean = path.substringAfterLast('.') in extensions

    companion object {
        val TYPES = ResourceType.values()

        fun getResourceTypeFromPath(path: String): ResourceType? {
            TYPES.fastForEach {
                if (it.pathHashExtension(path)) return it
            }
            return null
        }
    }
}

//fun isPathForImage(path: String): Boolean = ResourceType.IMAGE.pathHashExtension(path)
//fun isPathForFont(path: String): Boolean = ResourceType.FONT.pathHashExtension(path)

fun isPathForPreviewResource(path: String): Boolean = ResourceType.getResourceTypeFromPath(path) != null

class KorgePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        //println("KorgePsiDocumentationTargetProvider.documentationTarget: $element, ${element.text}")
        KorgeTypedResourceExAnnotator.getElementResourcesVfsPath(element)?.let { resourcePath ->
            //println(" -> $resourcePath")
            if (!isPathForPreviewResource(resourcePath)) return null
            val virtualFile = element.project.getResourceVirtualFile(resourcePath) ?: return null
            //println(" -> $virtualFile")
            val file = virtualFile.toNioPath().toFile()
            //println(" -> $file")
            return HTMLDocumentationTarget(file, resourcePath)
        }

        //println("$element : ${element::class.java}")
        ColorAnnotator.tryGetColorFromElement(element)?.let { rgba ->
            return ColorDocumentationTarget(rgba)
        }

        return null
    }

    abstract class BaseDocumentationTarget : DocumentationTarget, Pointer<BaseDocumentationTarget> {
        override fun createPointer(): Pointer<out DocumentationTarget> = this
        override fun dereference(): BaseDocumentationTarget? = this
    }

    class ColorDocumentationTarget(
        val color: RGBA
    ) : BaseDocumentationTarget() {
        override fun computePresentation(): TargetPresentation =
            TargetPresentation.builder(color.hexString).presentation()

        override fun computeDocumentation(): DocumentationResult? =
            DocumentationResult.documentation(DocumentationContent.content(
                """
                    <div>
                        <img src="https://127.0.0.1/image.png" width=140 height=140 />
                        <p>${color.hexString}</p>
                    </div>
                """.trimIndent(),
                mutableMapOf(
                    "https://127.0.0.1/image.png" to BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR).also {
                        it.setRGB(0, 0, color.toAwt().rgb)
                    }
                )
            ))
    }

    class HTMLDocumentationTarget(
        val file: File,
        val resourcePath: String
    ) : BaseDocumentationTarget() {
        override fun computePresentation(): TargetPresentation =
            TargetPresentation.builder("${file.name}").presentation()

        override fun computeDocumentation(): DocumentationResult? =
            DocumentationResult.asyncDocumentation {
                val resourceType = ResourceType.getResourceTypeFromPath(file.absolutePath) ?: return@asyncDocumentation null
                when (resourceType) {
                    ResourceType.IMAGE -> {
                        val imageSizeResult = getImageSize(file)
                        val originalSize = imageSizeResult.size
                        val scaledSize = ScaleMode.SHOW_ALL.invoke(originalSize, SizeInt(240, 140))

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
                    ResourceType.FONT -> {
                        val (font, preview) = getFontPreview(null, 64, file.readBytes())
                        val originalSize = SizeInt(preview.width, preview.height)
                        val scaledSize = ScaleMode.SHOW_ALL.invoke(originalSize, SizeInt(240, 140))
                        val previewScaled = preview.getScaledInstance(scaledSize.width, scaledSize.height, smooth = true)

                        DocumentationResult.documentation(DocumentationContent.content(
                            """
                                <div>
                                    <div>${resourcePath}</div>
                                    <img src='https://127.0.0.1/image.png' width="${scaledSize.width}" height="${scaledSize.height}" />
                                    <div>${font.fontName} : ${font.psName}</div>
                                </div>
                            """.trimIndent(),
                            mutableMapOf(
                                "https://127.0.0.1/image.png" to previewScaled
                            )
                        ))
                    }
                }
            }
    }
}

data class ImageSizeResult(val size: SizeInt, val formatName: String)

suspend fun getImageSize(bytes: ByteArray): ImageSizeResult = _getImageSize(bytes.inputStream())
suspend fun getImageSize(inputStream: InputStream): ImageSizeResult = _getImageSize(inputStream)
suspend fun getImageSize(imageFile: File): ImageSizeResult = _getImageSize(imageFile)

private suspend fun _getImageSize(imageFile: Any?): ImageSizeResult {
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
