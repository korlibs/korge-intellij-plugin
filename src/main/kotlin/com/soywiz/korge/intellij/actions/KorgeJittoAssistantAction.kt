package com.soywiz.korge.intellij.actions

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.soywiz.korge.intellij.ai.OpenAI
import com.soywiz.korge.intellij.audio.AudioConverter
import com.soywiz.korge.intellij.audio.Microphone
import com.soywiz.korge.intellij.config.korgeGlobalPrivateSettings
import com.soywiz.korge.intellij.passwordSafe
import com.soywiz.korge.intellij.util.fileEditorManager
import com.soywiz.korge.intellij.util.runWriteAction
import javax.swing.JOptionPane

class KorgeJittoAssistantAction : AnAction() {
    val OPEN_AI_KEY_CREDS = CredentialAttributes("KorgeJittoOpenAIKey", "OpenAIKey")

    override fun update(e: AnActionEvent) {
        val isSoywiz = korgeGlobalPrivateSettings.isUserLoggedIn() && korgeGlobalPrivateSettings.userLogin == "soywiz"
        e.presentation.isVisible = isSoywiz
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        //val editor: Editor? = FileEditorManager.getInstance(e.project.fileEdidot).selectedTextEditor
        if (project.passwordSafe.getPassword(OPEN_AI_KEY_CREDS) == null) {
            val userInput = JOptionPane.showInputDialog(null, "Enter your OpenAI key:")
            if (userInput != null) {
                project.passwordSafe.setPassword(OPEN_AI_KEY_CREDS, userInput)
            } else {
                TODO()
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
            .user("""
                Always use this code as base:
                
                ```kotlin
                import korlibs.time.*
                import korlibs.korge.*
                import korlibs.korge.scene.*
                import korlibs.korge.tween.*
                import korlibs.korge.view.*
                import korlibs.image.color.*
                import korlibs.image.format.*
                import korlibs.io.file.std.*
                import korlibs.math.geom.*
                import korlibs.math.interpolation.*
                import korlibs.korge.view.align.*

                suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]).start {
                    sceneContainer().changeTo({ MyScene() })
                }
                
                class MyScene : Scene() {
                    override suspend fun SContainer.sceneMain() {
                        // Your code here
                    }
                }
                ```
            """.trimIndent())

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
                            .replace("com.soywiz.klock.", "korlibs.time.")
                            .replace("com.soywiz.korge.", "korlibs.korge.")
                            .replace("com.soywiz.korim.", "korlibs.image.")
                            .replace("com.soywiz.korio.", "korlibs.io.")
                            .replace("com.soywiz.korma.", "korlibs.math.")
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
}

