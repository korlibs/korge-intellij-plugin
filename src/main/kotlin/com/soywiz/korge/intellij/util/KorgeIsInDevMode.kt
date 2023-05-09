package com.soywiz.korge.intellij.util

val isDevelopmentMode: Boolean get() = "true" == System.getProperty("idea.is.internal")
