package com.soywiz.korge.intellij.editor

import korlibs.image.format.*
import korlibs.io.stream.*
import kotlinx.coroutines.*

object AllImageFormats {
    val FORMATS: ImageFormats = ImageFormats(PSD, TGA, KRA, DDS, DXT1, DXT2, DXT3, DXT4, DXT5, SVG, ICO, QOI, ASE, GIF)

    fun detectFormat(s: SyncStream, props: ImageDecodingProps): ImageFormat? {
        for (format in FORMATS.formats) {
            try {
                format.decodeHeader(s.sliceStart(), props)
                return format
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                continue
            }
        }
        return null
    }
}
