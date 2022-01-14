package com.soywiz.korge.intellij.util

import com.intellij.ui.jcef.*
import java.net.*
import javax.swing.*

private object ResourceExt

fun getResourceFileOrNull(path: String): URL? =
    runCatching { ResourceExt::class.java.classLoader.getResource(path.trimStart('/')) }.getOrNull()
        ?: runCatching { ResourceExt::class.java.classLoader.getResource("/" + path.trimStart('/')) }.getOrNull()
fun getResourceFile(path: String): URL = getResourceFileOrNull(path)
    ?: error("Can't find '$path' from resources")

fun getResourceText(path: String): String = getResourceFile(path).readText()
fun getResourceBytes(path: String): ByteArray = getResourceFile(path).readBytes()
