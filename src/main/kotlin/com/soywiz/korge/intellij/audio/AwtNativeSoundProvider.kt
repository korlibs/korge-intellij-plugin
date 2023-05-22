package com.soywiz.korge.intellij.audio

import korlibs.datastructure.*
import korlibs.time.*
import korlibs.audio.error.*
import korlibs.audio.format.*
import korlibs.audio.sound.*
import korlibs.io.async.*
import korlibs.io.stream.*
import java.io.*
import java.nio.*
import javax.sound.sampled.*
import javax.sound.sampled.AudioFormat
import kotlin.coroutines.*

// AudioSystem.getMixerInfo()
val mixer by lazy { AudioSystem.getMixer(null) }

object AwtNativeSoundProvider : NativeSoundProvider() {
    init {
        // warming and preparing
        mixer.mixerInfo
        val af = AudioFormat(44100f, 16, 2, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, af)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(af, 4096)
        line.start()
        line.write(ByteArray(4), 0, 4)
        line.drain()
        line.stop()
        line.close()
    }

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        return JvmPlatformAudioOutput(coroutineContext, freq)
    }
}


val AudioSamples.size: Int get() = totalSamples * channels

data class SampleBuffer(val timestamp: Long, val data: AudioSamples)

class JvmPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int) : PlatformAudioOutput(coroutineContext, freq) {
    companion object {
        var lastId = 0
        val mixer by lazy { AudioSystem.getMixer(null) }
    }

    val id = lastId++
    val format by lazy { AudioFormat(freq.toFloat(), 16, 2, true, false) }
    var _msElapsed = 0.0
    val msElapsed get() = _msElapsed
    var totalShorts = 0
    val buffers = Queue<SampleBuffer>()
    var thread: Thread? = null
    var running = true

    val availableBuffers: Int get() = synchronized(buffers) { buffers.size }
    val line by lazy { mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine }

    override val availableSamples get() = synchronized(buffers) { totalShorts }

    fun ensureThread() {
        if (thread == null) {

            thread = Thread {
                line.open()
                line.start()
                //println("OPENED_LINE($id)!")
                try {
                    var timesWithoutBuffers = 0
                    while (running) {
                        while (availableBuffers > 0) {
                            timesWithoutBuffers = 0
                            val buf = synchronized(buffers) { buffers.dequeue() }
                            synchronized(buffers) { totalShorts -= buf.data.size }
                            buf.data.scaleVolume(volume)
                            val bdata = convertFromShortToByte(buf.data.interleaved().data)

                            val msChunk = (((bdata.size / 2) * 1000.0) / frequency.toDouble()).toInt()

                            _msElapsed += msChunk
                            val now = System.currentTimeMillis()
                            val latency = now - buf.timestamp
                            //val drop = latency >= 150
                            val start = System.currentTimeMillis()
                            line.write(bdata, 0, bdata.size)
                            //line.drain()
                            val end = System.currentTimeMillis()
                            //println("LINE($id): ${end - start} :: msChunk=$msChunk :: start=$start, end=$end :: available=${line.available()} :: framePosition=${line.framePosition} :: availableBuffers=$availableBuffers")
                        }
                        //println("SHUT($id)!")
                        //Thread.sleep(500L) // 0.5 seconds of grace before shutting down this thread!
                        Thread.sleep(50L) // 0.5 seconds of grace before shutting down this thread!
                        timesWithoutBuffers++
                        if (timesWithoutBuffers >= 10) break
                    }
                } finally {
                    line.drain()
                    Thread.sleep(250L)

                    //println("CLOSED_LINE($id)!")
                    line.stop()
                    line.close()
                }

                thread = null
            }.apply {
                name = "NativeAudioStream$id"
                isDaemon = false
                start()
            }
        }
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        val buffer = SampleBuffer(System.currentTimeMillis(), samples.copyOfRange(offset, offset + size))
        synchronized(buffers) {
            totalShorts += buffer.data.size
            buffers.enqueue(buffer)
        }

        ensureThread()

        while (availableBuffers >= 5) {
            coroutineContext.delay(4.milliseconds)
        }

        //val ONE_SECOND = 44100 * 2
        ////val BUFFER_TIME_SIZE = ONE_SECOND / 8 // 1/8 second of buffer
        //val BUFFER_TIME_SIZE = ONE_SECOND / 4 // 1/4 second of buffer
        ////val BUFFER_TIME_SIZE = ONE_SECOND / 2 // 1/2 second of buffer
        //while (bufferSize >= 32 && synchronized(buffers) { totalShorts } > BUFFER_TIME_SIZE) {
        //	ensureThread()
        //	getCoroutineContext().eventLoop.sleepNextFrame()
        //}
    }

    fun convertFromShortToByte(sa: ShortArray, offset: Int = 0, size: Int = sa.size - offset): ByteArray {
        val bb = ByteBuffer.allocate(size * 2).order(ByteOrder.nativeOrder())
        val sb = bb.asShortBuffer()
        sb.put(sa, offset, size)
        return bb.array()
    }

    //suspend fun CoroutineContext.sleepImmediate2() = suspendCoroutine<Unit> { c ->
    //	eventLoop.setImmediate { c.resume(Unit) }
    //}
    override fun stop() {
        running = false
    }

    override fun start() {

    }
}
