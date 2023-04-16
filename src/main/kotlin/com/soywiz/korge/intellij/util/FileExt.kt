package com.soywiz.korge.intellij.util

import java.io.File

inline fun <T> tempCreateTempFile(ext: String, data: ByteArray? = null, block: (file: File) -> T): T {
    val file = File.createTempFile("temp", ".$ext")
    try {
        if (data != null) file.writeBytes(data)
        return block(file)
    } finally {
        file.delete()
    }
}
