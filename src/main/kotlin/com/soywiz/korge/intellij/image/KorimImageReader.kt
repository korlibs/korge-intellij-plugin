package com.soywiz.korge.intellij.image

import com.soywiz.korge.intellij.editor.*
import korlibs.image.atlas.*
import korlibs.image.awt.*
import korlibs.image.format.*
import korlibs.io.stream.*
import java.awt.image.*
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.*
import javax.imageio.metadata.*
import javax.imageio.spi.*
import javax.imageio.stream.*

open class KorimImageReader @JvmOverloads constructor(val spi: KorimImageReaderSpi = KorimImageReaderSpi()) : ImageReader(spi) {
    //lateinit var mutableAtlas: MutableAtlasUnit
    lateinit var imageData: ImageData

    override fun setInput(input: Any?, seekForwardOnly: Boolean, ignoreMetadata: Boolean) {
        println("KorimImageReader.setInput")
        try {
            super.setInput(input, seekForwardOnly, ignoreMetadata)
            val iis = input as ImageInputStream // Always works
            imageData = AllImageFormats.FORMATS.readImage(iis.readAllBytes().openSync())
            //mutableAtlas = MutableAtlasUnit()
            //imageData.packInMutableAtlas(mutableAtlas)
            println("KorimImageReader.setInput/2")
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    override fun getNumImages(allowSearch: Boolean): Int {
        println("KorimImageReader.getNumImages")
        return imageData.frames.size
    }

    override fun getWidth(imageIndex: Int): Int {
        println("KorimImageReader.getWidth")
        return imageData.frames[imageIndex].width
    }

    override fun getHeight(imageIndex: Int): Int {
        println("KorimImageReader.getHeight")
        return imageData.frames[imageIndex].height
    }

    override fun getImageTypes(imageIndex: Int): Iterator<ImageTypeSpecifier>? {
        println("KorimImageReader.getImageTypes")
        return null
    }

    override fun getStreamMetadata(): IIOMetadata? {
        println("KorimImageReader.getStreamMetadata")
        return null
    }

    override fun getImageMetadata(imageIndex: Int): IIOMetadata? {
        println("KorimImageReader.getImageMetadata")
        return null
    }

    override fun read(imageIndex: Int, param: ImageReadParam?): BufferedImage {
        println("KorimImageReader.read")
        //return imageData.mainBitmap.toAwt()
        //return mutableAtlas.bitmap.toAwt()
        return imageData.frames[imageIndex].bitmap.toAwt()
    }
}

open class KorimImageReaderSpi : ImageReaderSpi(
    "Korlibs",
    "1.0",
    arrayOf("korim", "KORIM"),
    arrayOf("korim"),
    arrayOf("image/korim"),
    KorimImageReader::class.java.name,
    arrayOf(ImageInputStream::class.java),
    arrayOf(),
    false, null, null, null, null, true, "javax_imageio_korim_1.0", null, null, null
) {
    init {
        println("KorimImageReaderSpi!!")
    }
    override fun getDescription(locale: Locale?): String {
        println("KorimImageReaderSpi.getDescription")
        return "Korim"
    }

    override fun canDecodeInput(source: Any?): Boolean {
        println("KorimImageReaderSpi.canDecodeInput: $source")
        if (source !is ImageInputStream) {
            return false
        }
        val ba = source.readAllBytes()
        //val temp = ByteArray(1024)
        //val read = source.read(temp)
        return AllImageFormats.FORMATS.decodeHeader(ba.openSync()) != null
    }

    override fun createReaderInstance(extension: Any?): ImageReader {
        println("KorimImageReaderSpi.createReaderInstance: $extension")
        return KorimImageReader(this)
    }
}

/*
public class KorimMetadataFormat : IIOMetadataFormatImpl("") {
    override fun canNodeAppear(elementName: String?, imageType: ImageTypeSpecifier?): Boolean {
        TODO("Not yet implemented")
    }
}

 */

fun ImageInputStream.readAllBytes(): ByteArray {
    val baos = ByteArrayOutputStream(kotlin.math.max(length(), 32L * 1024L).toInt())
    val temp = ByteArray(32 * 1024)
    while (true) {
        val read = read(temp)
        if (read <= 0) break
        baos.write(temp, 0, read)
    }
    return baos.toByteArray()
}
