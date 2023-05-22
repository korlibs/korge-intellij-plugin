package com.soywiz.korge.intellij.actions

import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.utils.editor.getVirtualFile
import com.intellij.testFramework.utils.editor.saveToDisk
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.KorgeIcons
import com.soywiz.korge.intellij.ai.OpenAI
import com.soywiz.korge.intellij.config.globalPrivateSettings
import com.soywiz.korge.intellij.config.korgeGlobalPrivateSettings
import com.soywiz.korge.intellij.korge
import com.soywiz.korge.intellij.passwordSafe
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.*
import org.jetbrains.kotlin.idea.base.util.onTextChange
import java.awt.*
import java.awt.event.AWTEventListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*

class KorgeJittoAssistantAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val isSoywiz = korgeGlobalPrivateSettings.isUserLoggedIn() && korgeGlobalPrivateSettings.userLogin == "soywiz"
        e.presentation.isVisible = isSoywiz && e.project?.korge?.containsKorge == true
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val project = actionEvent.project
        //val menu = JBPopupMenu()
        //menu.add("HELLO")
        //menu.show(e.inputEvent.component, 0, 24)
        val c = actionEvent.inputEvent.component
        val buttonRect = Rectangle(c.locationOnScreen, c.size )
        val dialogSize = Dimension(220, 120)

        val privateSettings = project!!.korge.globalPrivateSettings

        //val frame = JFrame()
        val frame = JDialog(c.parentFrame())
        //frame.isAlwaysOnTop = true
        frame.isUndecorated = true
        //frame.isOpaque = true
        frame.bounds = Rectangle(Point(buttonRect.right - dialogSize.width - 4, buttonRect.bottom), dialogSize)
        frame.isVisible = true

        frame.contentPane.layout = BorderLayout()
        frame.contentPane.minimumSize = dialogSize
        frame.contentPane.maximumSize = dialogSize
        frame.contentPane.preferredSize = dialogSize
        frame.rootPane.border = BorderFactory.createMatteBorder(2, 2, 2, 2, Color(0x5C1A95));

        val chat = KorgeAI(project)

        val initialCode = project!!.fileEditorManager.selectedTextEditor!!.document.text
        // SContainer.sceneMain
        if (initialCode.contains("SContainer.sceneMain")) {
            chat.initialCode(initialCode)
        } else {
            chat.initialCode()
        }

        frame.contentPane.styled {
            margin = 8.pt
            padding = 8.pt
            verticalStack {
                height = MUnit.Auto
                width = 100.percentage
                icon(KorgeIcons.JITTO)
                lateinit var label: JLabel
                label("Type your prompt...") {
                    label = this.component
                }
                textField("") {
                    //component.onTextChange { println(component.text) }
                    component.addKeyListener(object : KeyAdapter() {
                        override fun keyPressed(e: KeyEvent) {
                            if (e.keyCode == KeyEvent.VK_ESCAPE) {
                                frame.isVisible = false
                            }
                            if (e.keyCode == KeyEvent.VK_ENTER && component.text.isNotBlank()) {
                                val promptText = component.text
                                label.text = "Processing... '$promptText'"
                                component.text = ""
                                component.isEnabled = false

                                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Running AI") {
                                    override fun run(indicator: ProgressIndicator) {
                                        try {
                                            val result = chat.prompt(promptText)

                                            project.runWriteCommandAction {
                                                val document: Document = project!!.fileEditorManager.selectedTextEditor!!.document
                                                document.setText(result)


                                                project.codeStyleManager.reformatText(document.psiFile(project), 0, result.length)
                                                //project.psiDocumentManager.commitDocument(document)

                                                //ReformatCodeAction().actionPerformed(actionEvent)
                                                //CodeStyleManager.getInstance(project).collapseImports()
                                                document.saveDocument()
                                            }
                                        } catch (e: Throwable) {
                                            e.printStackTrace()
                                            label.text = "There was an error: ${e.message}. Try it again"
                                        } finally {
                                            component.isEnabled = true
                                            component.grabFocus()
                                        }
                                    }
                                })
                                //println("TYPED:" + component.text)
                            }
                        }
                    })
                }
                button("Stop") {
                    this.click {
                        frame.isVisible = false
                    }
                }
            }
        }
        frame.opacity = 0.9f
        //frame.contentPane.add(JButton("HELLO!"))
        frame.pack()

        //SwingUtilities.invokeLater {
        //    var eventListener: AWTEventListener? = null
        //    eventListener = AWTEventListener {
        //        val mouseEvent = it as MouseEvent
        //        if (mouseEvent.id == MouseEvent.MOUSE_RELEASED) {
        //            //if (!mouseEvent.component.hasAncestor(frame)) {
        //            if (true) {
        //                frame.isVisible = false
        //                SwingUtilities.invokeLater {
        //                    Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)
        //                }
        //            }
        //        }
        //    }
        //    Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK)
        //}
 }

    class KorgeAI(val project: Project) {
        companion object {
            val OPEN_AI_KEY_CREDS = CredentialAttributes("KorgeJittoOpenAIKey", "OpenAIKey")
        }

        init {
            //val editor: Editor? = FileEditorManager.getInstance(e.project.fileEdidot).selectedTextEditor
            if (project.passwordSafe.getPassword(OPEN_AI_KEY_CREDS) == null) {
                val userInput = JOptionPane.showInputDialog(null, "Enter your OpenAI key:")
                if (userInput != null) {
                    project.passwordSafe.setPassword(OPEN_AI_KEY_CREDS, userInput)
                } else {
                    TODO()
                }
            }
        }

        val openAIKey = project.passwordSafe.getPassword(OPEN_AI_KEY_CREDS) ?: error("Couldn't find OpenAI key")
        val openai = OpenAI(openAIKey)

        //val chat = openai.chatStream(temperature = 0.5)
        val chat = openai.chatStream(temperature = 0.0)
            .system("""
                Your are a Kotlin programmer, using the KorGE game engine. 
                Only write code without explaining it. 
                Try to be as concise as possible.
            """.trimIndent())

        val IMPORTS = """
            import korlibs.time.*
            import korlibs.korge.*
            import korlibs.korge.scene.*
            import korlibs.korge.tween.*
            import korlibs.korge.view.*
            import korlibs.image.color.*
            import korlibs.image.format.*
            import korlibs.image.text.*
            import korlibs.io.file.std.*
            import korlibs.math.geom.*
            import korlibs.math.interpolation.*
            import korlibs.korge.view.align.*
        """.trimIndent().trim()

        fun initialCode(code: String = """
            $IMPORTS

            suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]).start {
                sceneContainer().changeTo({ MyScene() })
            }
            
            class MyScene : Scene() {
                override suspend fun SContainer.sceneMain() {
                    // Your code here
                }
            }
        """.trimIndent().trim()) {
            chat.user("""
                Always use this code as base:
                
                ```kotlin
                $code
                ```
                
                KorGE changed and now instead of Double you should use Float. Do not use align methods.
                
                ```kotlin
                // To tween to a position, you can:
                view.tween(view::pos[Point(10f, 20f)], time = 1.seconds)
                // For position a view, you can use:
                view.xy(Point(10f, 20f))
                // To set a view rotation:
                view.rotation = 90.degrees
                // To set the anchor point:
                view.anchor(0.5f, 0.5f)
                // To create a new text
                container.text("hello world", textSize = 64f, color = Colors.WHITE)
                ```
                
                When rotating views, typically set the anchor point to the center.
                
                For text do not use anchor, but use `text.alignment = TextAlignment.CENTER`.
                Never use `text.anchor(...)`
            """.trimIndent())
        }

        fun prompt(prompt: String): String {
            //chat.user("$prompt. Show me the whole code")
            chat.user("$prompt. Show me the whole code inside `SContainer.sceneMain`")

            //chat.user("$prompt. Show me the changes in diff format as compact as possible")

            val response = chat.send().content
            println("Agent said '$response'")
            if (!response.contains("```kotlin")) {
                println("Didn't generate kotlin code. Stopping")
                error("Didn't generate kotlin code. Stopping")
            }
            return fixResponse(response)
        }

        fun fixResponse(text: String): String {
            var result = (Regex("```kotlin(.*)```", RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)?.trim() ?: error("Culdn't find kotlin code"))
                .replace("korlibs.time.", "korlibs.time.")
                .replace("com.soywiz.korge.", "korlibs.korge.")
                .replace("korlibs.image.", "korlibs.image.")
                .replace("korlibs.io.", "korlibs.io.")
                .replace("korlibs.math.", "korlibs.math.")
                .replace("override suspend fun Container.sceneMain()", "override suspend fun SContainer.sceneMain()")
                .replace("override suspend fun Container.sceneInit()", "override suspend fun SContainer.sceneMain()")
                .replace("sceneContainer().changeTo<MyScene>()", "sceneContainer().changeTo({ MyScene() })")
                .replace("sceneContainer().changeTo { MyScene() }", "sceneContainer().changeTo({ MyScene() })")
                .replace("::scale[", "::scaleXY[")
                .replace(".0", "f")
                .replace("!!!!", "!!")
                .replace(Regex("\\bstage\\b"), "stage!!")
                .replace(Regex("\\bseconds\\((.*?)\\)\\b")) { "${it.groupValues[1]}.seconds()" }

            if (result.contains("fun main")) {
                return result
            } else {
                run {
                    val prefix = "class MyScene : Scene() {"
                    result = (if (result.startsWith(prefix)) result.removePrefix(prefix).removeSuffix("}") else result).trim()
                }
                run {
                    val prefix = "override suspend fun SContainer.sceneMain() {"
                    val suffix = "}"
                    result = (if (result.startsWith(prefix)) result.removePrefix(prefix)
                        .removeSuffix(suffix) else result).trim()
                }
                return """
                    $IMPORTS
        
                    suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]).start {
                        sceneContainer().changeTo({ MyScene() })
                    }
                    
                    class MyScene : Scene() {
                        override suspend fun SContainer.sceneMain() {
                    $result
                        }
                    }
                """.trimIndent().trim()
            }
        }
    }

    /*
    fun actionPerformed2(e: AnActionEvent) {
        val project = e.project!!

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Running AI") {
            override fun run(indicator: ProgressIndicator) {
                //indicator.fraction = 0.5
                indicator.isIndeterminate = true

                while (!indicator.isCanceled) {
                    indicator.text = "Listening..."
                    val wavData = Microphone.recordWavUntilSilence()
                    if (indicator.isCanceled) break
                    indicator.text = "Converting to mp3..."
                    val mp3Data = AudioConverter.wav2mp3(wavData)
                    if (indicator.isCanceled) break
                    indicator.text = "Transcoding..."
                    val transcode = openai.transcode(mp3Data, language = "en").trim()
                    if (indicator.isCanceled) break
                    if (transcode.isNullOrBlank()) continue

                    indicator.text = "Generating code..."

                    chat.user(transcode)
                    if (indicator.isCanceled) break

                    println("User said '$transcode'")

                    val response = chat.send().content
                    println("Agent said '$response'")
                    if (!response.contains("```kotlin")) {
                        println("Didn't generate kotlin code. Stopping")
                        error("Didn't generate kotlin code. Stopping")
                    }
                    //val text = response.trim().removePrefix("```kotlin").removeSuffix("```").trim()
                    val text =
                        (Regex("```kotlin(.*)```", RegexOption.DOT_MATCHES_ALL).find(response)?.groupValues?.get(1)?.trim() ?: error("Culdn't find kotlin code"))
                            .replace("korlibs.time.", "korlibs.time.")
                            .replace("com.soywiz.korge.", "korlibs.korge.")
                            .replace("korlibs.image.", "korlibs.image.")
                            .replace("korlibs.io.", "korlibs.io.")
                            .replace("korlibs.math.", "korlibs.math.")
                            .replace("override suspend fun Container.sceneMain()", "override suspend fun SContainer.sceneMain()")
                            .replace("override suspend fun Container.sceneInit()", "override suspend fun SContainer.sceneMain()")
                            .replace("sceneContainer().changeTo<MyScene>()", "sceneContainer().changeTo({ MyScene() })")
                            .replace("sceneContainer().changeTo { MyScene() }", "sceneContainer().changeTo({ MyScene() })")
                            .replace("::scale[", "::scaleXY[")
                            .replace(Regex("\\bstage\\b"), "stage!!")
                            .replace(Regex("\\bseconds\\((.*?)\\)\\b")) { "${it.groupValues[1]}.seconds()" }

                    chat.filterEntries { it.role != OpenAI.Role.ASSISTANT || it == chat.lastEntry }

                    runWriteAction {
                        val document = e.project!!.fileEditorManager.selectedTextEditor!!.document
                        document.setText(text)
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                }
            }

        })

        //e.project!!.currentSession.edi
    }

     */
}

