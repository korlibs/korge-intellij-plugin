package com.soywiz.korge.intellij.audio

import com.soywiz.korge.intellij.util.tempCreateTempFile
import java.io.File

object AudioConverter {
    fun wav2mp3(data: ByteArray): ByteArray {
        tempCreateTempFile("wav", data) { input ->
            tempCreateTempFile("mp3") { output ->
                wav2mp3(input, output)
                return output.readBytes()
            }
        }
    }

    fun wav2mp3(file: File, out: File = File(file.parentFile, "${file.nameWithoutExtension}.mp3")): File {
        val process = ProcessBuilder()
            //.inheritIO()
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .command("ffmpeg", "-y", "-i", file.absolutePath, out.absolutePath)
            .start()
        process.waitFor()
        return out
    }
}

