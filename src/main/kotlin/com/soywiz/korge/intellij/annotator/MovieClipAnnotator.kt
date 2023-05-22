package com.soywiz.korge.intellij.annotator

/*
import com.intellij.codeInsight.intention.impl.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.psi.*
import com.intellij.util.ui.*
import com.soywiz.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.config.*
import com.soywiz.korge.intellij.ui.FillLayout
import com.soywiz.korge.intellij.util.*
import korlibs.korge.view.*
import korlibs.korgw.awt.*
import korlibs.image.color.*
import korlibs.image.color.Colors
import korlibs.io.async.*
import korlibs.korge.Korge
import korlibs.math.geom.*
import korlibs.math.geom.Anchor
import korlibs.render.awt.GLCanvas
import korlibs.render.awt.GLCanvasGameWindow
import kotlinx.coroutines.*
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.idea.refactoring.fqName.*
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.*
import org.jetbrains.uast.util.*
import java.awt.*
import javax.swing.*
import kotlin.reflect.*

class MovieClipAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtClass) return
        if (!element.isOrdinaryClass) return
        for (superTypeListEntry in element.superTypeListEntries) {
            if (superTypeListEntry !is KtSuperTypeCallEntry) continue
            if (superTypeListEntry.calleeExpression.typeReference!!.textWithoutDummy == "MovieClip") {
                if (!element.project.hasAccessToEarlyPreviewFeatures()) return

                val renderer = MovieClipEditorRenderer(element)
                holder.newAnnotation(HighlightSeverity.INFORMATION, "MovieClip")
                    .withFix(object : BaseIntentionAction() {
                        override fun getText(): String = "Edit MovieClip..."
                        override fun getFamilyName(): String = text
                        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
                        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = renderer.editMovieClip()
                    })
                    .gutterIconRenderer(renderer)
                    .create()
            }
        }
    }
}

data class MovieClipEditorRenderer(val ktClass: KtClass): GutterIconRenderer() {
    override fun getIcon(): Icon = ColorIcon(12, Colors.PINK.toAwt(), true)

    fun editMovieClip() {
        for (prop in ktClass.body!!.properties) {
            println("PROP: ${prop.name} : ${prop.typeExt()}: ${prop.isViewProp()}, ${prop.isStateProp()}")
            if (prop.name == "rect1") {
                val viewProp = ViewProperty(prop.initializer, prop)
                viewProp.setProp("width", "120")
                viewProp.setProp("x", "120.0")
            }
        }

        class MyDialogWrapper : DialogWrapper(true) {
            var views: Views? = null
            var thread: Thread? = null

            override fun createCenterPanel(): JComponent? {
                val dialogPanel = JPanel(FillLayout())
                dialogPanel.preferredSize = Dimension(512, 512)
                val bgcolor = dialogPanel.background.rgba()

                val canvas = GLCanvas()
                canvas.background = bgcolor.toAwt()
                dialogPanel.add(canvas)
                val gameWindow = GLCanvasGameWindow(canvas).apply {
                    exitProcessOnExit = false
                }

                Thread {
                    runBlocking {
                        Korge(
                            width = 640, height = 480,
                            virtualWidth = 640, virtualHeight = 480,
                            gameWindow = gameWindow!!,
                            scaleMode = ScaleMode.NO_SCALE,
                            //scaleMode = ScaleMode.SHOW_ALL,
                            scaleAnchor = Anchor.TOP_LEFT,
                            clipBorders = false,
                            bgcolor = bgcolor,
                            debug = false
                        ) {
                            this@MyDialogWrapper.views = views
                            solidRect(100, 100, Colors.CADETBLUE)
                        }
                    }
                }
                    .also { it.isDaemon = true }
                    .also { thread = it }
                    .start()

                return dialogPanel
            }

            override fun createActions(): Array<Action> {
                return super.createActions()
            }

            init {
                init()
                this.title = "Editing ${ktClass.name} MovieClip"
                this.isOK
            }

        }


        val myDialogWrapper = MyDialogWrapper()
        myDialogWrapper.showAndGet()
        runBlocking {
            myDialogWrapper.views?.close()
        }
        //myDialogWrapper.thread?.stop()
        //println(ktClass.text)
    }

    override fun getClickAction(): AnAction = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            editMovieClip()
        }
    }

    fun KtProperty.isViewProp(): Boolean {
        return this.typeExt()?.isView() == true
    }

    fun KotlinType.isView(): Boolean {
        return when (this.fqName!!.shortName().asString()) {
            "SolidRect", "View", "Container", "Image" -> true
            else -> false
        }
    }

    fun KtProperty.isStateProp(): Boolean {
        val prop = this
        val initializer = prop.initializer
        if (initializer !is KtCallExpression) return false
        val calleeExpression = initializer!!.calleeExpression
        if (calleeExpression !is KtReferenceExpression) return false
        if ((calleeExpression as PsiElement).text != "state") return false
        return true
    }

    fun KtClass.getStateProperties(): List<KtProperty> {
        return this!!.body!!.properties.filter { it.isStateProp() }
    }
}

class ViewProperty(val expr: KtExpression?, val nameDecl: KtNamedDeclaration?) {
    val props = LinkedHashMap<String, KtExpression?>()

    init {
        if (expr is KtCallExpression) {
            val callee = expr.calleeExpression
            if ((callee as PsiElement).text == "solidRect") {
                props["width"] = expr.valueArguments.getOrNull(0)?.getArgumentExpression()
                props["height"] = expr.valueArguments.getOrNull(1)?.getArgumentExpression()
                for (lambda in expr.lambdaArguments) {
                    val bodyExpression = lambda.getLambdaExpression()?.functionLiteral?.bodyExpression
                    for (child in (bodyExpression?.children?.toList() ?: listOf())) {
                        if (child is KtBinaryExpression) {
                            if (child.left?.text == "x") props["x"] = child.right
                            if (child.left?.text == "y") props["y"] = child.right
                        }
                    }
                }
            }
        }
    }

    fun setProp(prop: String, value: String) {
        (props[prop] as? PsiElement?)?.replace(value)
    }
}
*/
