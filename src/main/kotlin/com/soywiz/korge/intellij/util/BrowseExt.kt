package com.soywiz.korge.intellij.util

import java.awt.*
import java.net.*

fun launchBrowserWithUrl(url: String) {
    Desktop.getDesktop().browse(URI.create(url))
}

