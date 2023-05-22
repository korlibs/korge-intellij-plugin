package com.soywiz.korge.intellij.editor

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.soywiz.korge.intellij.audio.AwtNativeSoundProvider
import com.soywiz.korge.intellij.runBackgroundTaskWithProgress
import korlibs.audio.sound.readMusic
import com.soywiz.korge.intellij.toVfs
import com.soywiz.korge.intellij.util.backgroundTask
import com.soywiz.korge.intellij.util.onClick
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.AudioFormats
import korlibs.audio.format.MP3
import korlibs.audio.format.WAV
import korlibs.audio.format.mp3.MP3Decoder
import korlibs.audio.mod.MOD
import korlibs.audio.mod.S3M
import korlibs.audio.mod.XM
import korlibs.audio.sound.PlaybackParameters
import korlibs.audio.sound.Sound
import korlibs.audio.sound.SoundChannel
import korlibs.time.toTimeString
import kotlinx.coroutines.*
import org.jetbrains.kotlin.idea.gradleTooling.get
import javax.swing.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class AudioFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, virtualFile: VirtualFile): Boolean {
        val name = virtualFile.name
        return when {
            name.endsWith(".wav", ignoreCase = true) -> true
            name.endsWith(".mp3", ignoreCase = true) -> true
            name.endsWith(".mod", ignoreCase = true) -> true
            name.endsWith(".s3m", ignoreCase = true) -> true
            name.endsWith(".xm", ignoreCase = true) -> true
            else -> false
        }
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return object : FileEditorBase() {
            override fun getFile(): VirtualFile = file

            var sound = CompletableDeferred<Sound>()

            init {
                launch2 {
                    project.backgroundTask("Loading sound") {
                        runBlocking {
                            sound.complete(
                                AwtNativeSoundProvider.createSound(
                                    file.toVfs(),
                                    streaming = true,
                                    props = AudioDecodingProps.DEFAULT.copy(
                                        exactTimings = true,
                                        formats = AudioFormats(WAV, MP3Decoder, XM, MOD, S3M)
                                    )
                                )
                            )
                        }
                    }
                }
            }

            var playButton: JButton
            var stopButton: JButton
            var soundChannel: SoundChannel? = null
            var job: Job? = null
            var volume: Double = 0.5
            var panel = JPanel().also { panel ->
                panel.add(JBLabel(file.name))
                panel.add(JButton("Play").also {
                    playButton = it
                    it.onClick {
                        job?.cancel()
                        job = launch2 {
                            soundChannel = sound.await().play(params = PlaybackParameters.DEFAULT.copy(volume = volume))
                        }
                    }
                })
                panel.add(JButton("Stop").also {
                    stopButton = it
                    it.onClick {
                        job?.cancel()
                    }
                })
                panel.add(JBLabel("-:-").also { label ->
                    sound.invokeOnCompletion { error ->
                        println("COMPLETED!")
                        launch2 {
                            if (error == null) {
                                label.text = sound.await().length.toTimeString(2, addMilliseconds = true)
                            } else {
                                label.text = "$error"
                            }
                        }
                    }
                })
                panel.add(JSlider(0, 100).also { slider ->
                    slider.value = (100 * volume).toInt()
                    slider.addChangeListener {
                        volume = slider.value.toDouble() / 100.0
                        //println("Changing volume to... $volume")
                        soundChannel?.volume = volume
                    }
                })
            }

            override fun dispose() {
                job?.cancel()
                super.dispose()
            }

            override fun getComponent(): JComponent = panel
            override fun getName(): String = "KorauEditor"
            override fun getPreferredFocusedComponent(): JComponent? = playButton
        }
    }

    override fun getEditorTypeId(): String = "KORAU_TYPE_ID"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

fun launch2(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit): Job {
    return CoroutineScope(context).launch(start = CoroutineStart.DEFAULT) {
        block()
    }
}
