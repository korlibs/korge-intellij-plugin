package com.soywiz.korge.intellij.util

import korlibs.image.awt.*
import java.awt.*
import java.io.*
import javax.imageio.*
import javax.imageio.stream.*

fun Image.toJpegBytes(quality: Float = .8f): ByteArray {
    val img = this
    val jpg = ImageIO.getImageWritersByFormatName("jpg").next()
    val param = jpg.defaultWriteParam
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = .8f
    val outBytes = ByteArrayOutputStream()
    jpg.output = MemoryCacheImageOutputStream(outBytes)
    jpg.write(null, IIOImage(img.toBufferedImage(), null, null), param)
    return outBytes.toByteArray()
}

fun Image.getScaledInstance(width: Int, height: Int, smooth: Boolean = true): Image {
    return getScaledInstance(width, height, if (smooth) Image.SCALE_SMOOTH else 0)
}
