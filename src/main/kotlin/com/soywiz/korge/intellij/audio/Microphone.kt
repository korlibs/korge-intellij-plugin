package com.soywiz.korge.intellij.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.*
import kotlin.math.absoluteValue

object Microphone {
    fun recordWavUntilSilence(secondsOfSilence: Double = 1.0): ByteArray {
        val audioFormat = AudioFormat(44100f, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
        val targetDataLine = AudioSystem.getLine(info) as TargetDataLine

        //println("Iniciando grabación...")
        targetDataLine.open(audioFormat)
        targetDataLine.start()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(4410)
        val threshold = secondsOfSilence * audioFormat.frameSize * audioFormat.frameRate.toInt()
        var silentSamples = 0
        var recordedSomething = false

        while (true) {
            val bytesRead = targetDataLine.read(buffer, 0, buffer.size)
            //println("bytesRead=${bytesRead}")
            if (bytesRead > 0) {
                val level = buffer.map { it.toInt().absoluteValue }.average()
                //println("level=$level")
                val chunkIsSilenced = level < 32
                if (chunkIsSilenced) {
                    silentSamples += bytesRead
                    //println("silentSamples:$silentSamples >= threshold:$threshold :: level=$level")
                } else {
                    recordedSomething = true
                    //println(" --> level=$level")
                    silentSamples = 0
                }

                if (recordedSomething) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                if (chunkIsSilenced && recordedSomething && silentSamples >= threshold) break
            }
        }

        //println("Terminando grabación...")
        targetDataLine.stop()
        targetDataLine.close()

        //println(outputStream.toByteArray().size)

        val byteArray = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(byteArray)
        val audioInputStream = AudioSystem.getAudioInputStream(audioFormat, AudioInputStream(inputStream, audioFormat, (byteArray.size / 2).toLong()))

        return ByteArrayOutputStream().also { bao ->
            AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, bao)
        }.toByteArray()
    }
}
