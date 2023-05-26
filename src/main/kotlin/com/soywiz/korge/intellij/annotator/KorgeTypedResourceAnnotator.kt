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
import com.soywiz.korge.intellij.util.get
import com.soywiz.korge.intellij.util.getScaledInstance
import com.soywiz.korge.intellij.util.rootFile
import korlibs.image.awt.toBufferedImage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.notebooks.visualization.use
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon


class KorgeTypedResourceAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtAnnotationEntry) return
        val firstExpression = element.valueArguments.firstOrNull()?.getArgumentExpression()
        if (firstExpression !is KtStringTemplateExpression) return
        val annotationText = element.calleeExpression?.node?.text
        if (annotationText != "ResourceVfsPath") return
        val resourcePath = firstExpression.node.text.trim('"')
        if (resourcePath.endsWith(".png")) {
            holder.addOpenImageAnnotation(element.project, resourcePath)
        }
    }
}

fun AnnotationHolder.addOpenImageAnnotation(project: Project, resourcePath: String) {
    val holder = this
    holder.newAnnotation(HighlightSeverity.INFORMATION, "Image")
        .withFix(object : BaseIntentionAction() {
            override fun getText(): String = resourcePath
            override fun getFamilyName(): String = text
            override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
            override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            }
        })
        .gutterIconRenderer(
            DefaultGutterIconRenderer(OpenVirtualFileAction(project.getResourceVirtualFile(resourcePath)!!),
                ImageIcon(project.getCachedResourceIcon(resourcePath))
            ))
        .create()
}

data class DefaultGutterIconRenderer(val action: AnAction, val _icon: Icon) : GutterIconRenderer() {
    override fun getIcon() = _icon
    override fun getClickAction() = action
    override fun getTooltipText(): String = "Resource"
}

class OpenVirtualFileAction(val file: VirtualFile) : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
    }
}

fun Project.getResourceVirtualFile(path: String): VirtualFile? {
    return rootFile?.get("src/commonMain/resources")?.get(path)
}

class ResourceIconCache(val project: Project) {
    companion object {
        val KEY = Key.create<ResourceIconCache>("korge.resourceicon.cache.ext")
    }
    val cache = LinkedHashMap<String, BufferedImage?>()
}

val Project.resourceIconCache get() = this.getOrPutUserData(ResourceIconCache.KEY) { ResourceIconCache(this) }

fun Project.getCachedResourceIcon(path: String): BufferedImage? {
    val resourceIconCache = this.resourceIconCache
    return synchronized(resourceIconCache) {
        resourceIconCache.cache.getOrPut(path) {
            val project = this
            val bytes = project.getResourceVirtualFile(path)?.readBytes()
            return bytes?.inputStream()?.let { ImageIO.read(it).resized(16, 16) }
        }
    }
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
