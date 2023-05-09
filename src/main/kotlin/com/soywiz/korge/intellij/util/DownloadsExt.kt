package com.soywiz.korge.intellij.util

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.soywiz.korio.file.std.get
import com.soywiz.krypto.encoding.hexLower
import java.io.File
import java.net.URL
import java.security.MessageDigest

fun File.ensureParents(): File = this.also { it.parentFile.mkdirs() }

fun getUrlCachedFile(url: String): File {
    val urlHash = MessageDigest.getInstance("SHA256").digest(url.toByteArray(Charsets.UTF_8)).hexLower
    return File(PathManager.getPluginTempPath())["downloadCache"]["$urlHash.bin"]
}

fun getUrlCached(url: String): ByteArray? {
    val cacheFile = getUrlCachedFile(url)
    return cacheFile.takeIf { it.exists() }?.readBytes()
}

fun downloadUrlCached(url: String, cacheForMillis: Long = 24 * 3600 * 1000): ByteArray {
    val cacheFile = getUrlCachedFile(url)
    if (!cacheFile.exists() || (System.currentTimeMillis() - cacheFile.lastModified()) >= cacheForMillis) {
        //runBackgroundableTask("Downloading $url") {
        cacheFile.ensureParents().writeBytes(URL(url).readBytes())
        //}
    }
    return cacheFile.readBytes()
}
