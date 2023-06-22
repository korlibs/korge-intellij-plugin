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
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readBytes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.UIUtil
import com.soywiz.korge.intellij.documentation.ResourceType
import com.soywiz.korge.intellij.getOrPutUserData
import com.soywiz.korge.intellij.util.*
import korlibs.datastructure.linkedHashMapOf
import korlibs.image.awt.toBufferedImage
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.plugins.notebooks.visualization.use
import java.awt.*
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import javax.swing.Icon
import kotlin.jvm.optionals.getOrNull


class KorgeResourceAnnotator : Annotator {
    companion object {
        fun extractResourcesVfsPath(element: PsiElement): String? {
            if (element !is KtArrayAccessExpression) return null
            //println("KorgeResourceAnnotator.extractResourcesVfsPath: $element : ${element.text}")
            val refText = element.referenceExpression()?.firstChild?.text ?: return null
            //println(" -> $refText")
            if (refText != "resourcesVfs") return null
            val expression = element.indexExpressions.firstOrNull() ?: return null
            //println(" -> $expression")
            val expressionText = expression.text
            //println(" -> $expressionText")
            return expressionText.trim('"')
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val resourcePath = extractResourcesVfsPath(element) ?: return
        if (com.soywiz.korge.intellij.documentation.isPathForPreviewResource(resourcePath)) {
            holder.addOpenResourceAnnotation(element.project, resourcePath)
        }

        return // @TODO: This shouldn't be necessary anymore, as KorgeTypedResourceExAnnotator is providing already the icons in both cases
        //if (element !is KtAnnotationEntry) return
        //val firstExpression = element.valueArguments.firstOrNull()?.getArgumentExpression()
        //if (firstExpression !is KtStringTemplateExpression) return
        //val annotationText = element.calleeExpression?.node?.text
        //if (annotationText != "ResourceVfsPath") return
        //val resourcePath = firstExpression.node.text.trim('"')
        //if (resourcePath.endsWith(".png")) {
        //    holder.addOpenImageAnnotation(element.project, resourcePath)
        //}
    }
}

fun AnnotationHolder.addOpenResourceAnnotation(project: Project, resourcePath: String) {
    val holder = this
    val resourceType = ResourceType.getResourceTypeFromPath(resourcePath) ?: return
    val virtualFile = project.getResourceVirtualFile(resourcePath) ?: return
    if (!virtualFile.exists()) return
    val image = project.getCachedResourceIcon(resourcePath) ?: return

    holder.newAnnotation(HighlightSeverity.INFORMATION, "Open ${resourceType.name.lowercase()}")
        .withFix(object : BaseIntentionAction() {
            override fun getText(): String = "Open $resourcePath"
            override fun getFamilyName(): String = text
            override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
            override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
                OpenVirtualFileAction(virtualFile).openFile(project)
            }
        })
        .gutterIconRenderer(DefaultGutterIconRenderer(OpenVirtualFileAction(virtualFile), JBImageIcon(image)))
        .create()
}

data class DefaultGutterIconRenderer(val action: AnAction, val _icon: Icon) : GutterIconRenderer() {
    override fun getIcon() = _icon
    override fun getClickAction() = action
    override fun getTooltipText(): String = "Resource"
}

class OpenVirtualFileAction(val file: VirtualFile) : AnAction(), DumbAware {
    fun openFile(project: Project?) {
        file.refresh(false, true)
        project?.fileEditorManager?.openFile(file, true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        openFile(e.project)
    }
}

fun Project.getResourceVirtualFile(path: String): VirtualFile? {
    return rootFile?.get("src/commonMain/resources")?.get(path)
}

class ResourceIconCache(val project: Project) {
    companion object {
        val KEY = Key.create<ResourceIconCache>("korge.resourceicon.cache.ext")
    }
    // @TODO: TTL
    val cache = LinkedHashMap<String, LinkedHashMap<String, Optional<BufferedImage>>>()
}

val Project.resourceIconCache: ResourceIconCache get() = this.getOrPutUserData(ResourceIconCache.KEY) { ResourceIconCache(this) }

fun Project.getCachedResourceIcon(path: String): BufferedImage? {
    try {
        val resourceIconCache = this.resourceIconCache
        val resourceType = ResourceType.getResourceTypeFromPath(path) ?: return null
        this.getResourceVirtualFile(path) ?: return null

        return synchronized(resourceIconCache) {
            resourceIconCache.cache.getOrPut(project.colorsScheme.name) { linkedHashMapOf() }.getOrPut(path) {
                try {
                    val project = this
                    val file = project.getResourceVirtualFile(path) ?: return@getOrPut Optional.empty()
                    if (!file.isFile) return@getOrPut Optional.empty()
                    val bytes = file.readBytes()
                    val image = when (resourceType) {
                        ResourceType.IMAGE -> ImageIO.read(bytes.inputStream())
                        ResourceType.FONT -> getGlyphImage(project.ideFrame, Dimension(16, 16), bytes, project.colorsScheme.defaultForeground)
                    }.resizedHidpiAware(16, 16)
                    return@getOrPut Optional.of(image)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    return@getOrPut Optional.empty()
                }
            }.getOrNull()
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        return null
    }
}

//fun getGlyphImage(component: Component?, size: Dimension, ttfBytes: ByteArray, str: String? = null): BufferedImage {
fun getGlyphImage(component: Component?, size: Dimension, ttfBytes: ByteArray, color: Color, str: String? = "a"): BufferedImage {
    val size = Dimension(size.width * 2, size.height * 2)
    val font = Font.createFont(Font.TRUETYPE_FONT, ttfBytes.inputStream()).deriveFont(size.height.toFloat())
    val str = str ?: "${font.name.first()}"
    @Suppress("UndesirableClassUsage")
    val img = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB_PRE)
    img.createGraphics().use { g2d ->
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            //g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g2d.font = font
            g2d.color = color
            val fontMetrics = g2d.fontMetrics

            val x = (size.width - fontMetrics.stringWidth(str)) / 2
            val y = fontMetrics.ascent
            //println("DRAW TEXT AT: $x, $y ascent=${fontMetrics.ascent}, height=${fontMetrics.height}")
            g2d.drawString(str, x, y)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    //val img2 = UIUtil.createImage(component, size.width, size.height, BufferedImage.TYPE_INT_ARGB_PRE)
    //img2.createGraphics().use {
    //    it.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    //    it.drawImage(img, 0, 0, size.width, size.height, null, null)
    //}
    return img
}

fun Font.getFontMetrics(component: Component?): FontMetrics =
    UIUtil.createImage(component, 1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().use {
        it.font = this
        it.fontMetrics
    }

fun getFontPreview(component: Component?, height: Int, ttfBytes: ByteArray, color: Color, str: String? = null): Pair<Font, BufferedImage> {
    val font = Font.createFont(Font.TRUETYPE_FONT, ttfBytes.inputStream()).deriveFont(height.toFloat())
    val fontMetrics = font.getFontMetrics(component)
    val text = str ?: font.name
    val width = fontMetrics.stringWidth(text)
    val img = UIUtil.createImage(component, width, fontMetrics.height, BufferedImage.TYPE_INT_ARGB)
    img.createGraphics().use { g2d ->
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        //g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        try {
            g2d.color = color
            g2d.font = font
            g2d.drawString(text, 0, fontMetrics.ascent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return font to img
}

fun BufferedImage.resized(newWidth: Int, newHeight: Int): BufferedImage {
    return this.getScaledInstance(newWidth, newHeight, smooth = true).toBufferedImage()
    //val originalImage = this
    //val resizedImage = UIUtil.createImage(null, newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
    //resizedImage.createGraphics().use { g ->
    //    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    //    g.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
    //}
    //return resizedImage
}
// @TODO: Implement HDPI-Aware functionality
fun BufferedImage.resizedHidpiAware(newWidth: Int, newHeight: Int): BufferedImage {
    return resized(newWidth, newHeight)
}
