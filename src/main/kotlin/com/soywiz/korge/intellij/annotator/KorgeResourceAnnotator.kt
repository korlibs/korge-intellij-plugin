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
import com.jetbrains.rd.util.string.print
import com.soywiz.korge.intellij.getOrPutUserData
import com.soywiz.korge.intellij.util.fileEditorManager
import com.soywiz.korge.intellij.util.get
import com.soywiz.korge.intellij.util.getScaledInstance
import com.soywiz.korge.intellij.util.rootFile
import io.ktor.utils.io.*
import korlibs.image.awt.toBufferedImage
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.Optional
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
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
        val resourcePath = KorgeResourceAnnotator.extractResourcesVfsPath(element) ?: return
        if (com.soywiz.korge.intellij.documentation.isPathForImage(resourcePath)) {
            holder.addOpenImageAnnotation(element.project, resourcePath)
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

fun AnnotationHolder.addOpenImageAnnotation(project: Project, resourcePath: String) {
    val holder = this
    val virtualFile = project.getResourceVirtualFile(resourcePath) ?: return
    if (!virtualFile.exists()) return
    val image = project.getCachedResourceIcon(resourcePath) ?: return

    holder.newAnnotation(HighlightSeverity.INFORMATION, "Open image")
        .withFix(object : BaseIntentionAction() {
            override fun getText(): String = "Open $resourcePath"
            override fun getFamilyName(): String = text
            override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
            override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
                OpenVirtualFileAction(virtualFile).openFile(project)
            }
        })
        .gutterIconRenderer(DefaultGutterIconRenderer(OpenVirtualFileAction(virtualFile), ImageIcon(image)))
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
    val cache = LinkedHashMap<String, Optional<BufferedImage>>()
}

val Project.resourceIconCache: ResourceIconCache get() = this.getOrPutUserData(ResourceIconCache.KEY) { ResourceIconCache(this) }

fun Project.getCachedResourceIcon(path: String): BufferedImage? {
    try {
        val resourceIconCache = this.resourceIconCache
        this.getResourceVirtualFile(path) ?: return null
        return synchronized(resourceIconCache) {
            resourceIconCache.cache.getOrPut(path) {
                try {
                    val project = this
                    val file = project.getResourceVirtualFile(path) ?: return@getOrPut Optional.empty()
                    if (!file.isFile) return@getOrPut Optional.empty()
                    val bytes = file.readBytes()
                    return@getOrPut Optional.of(ImageIO.read(bytes.inputStream()).resized(16, 16))
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
