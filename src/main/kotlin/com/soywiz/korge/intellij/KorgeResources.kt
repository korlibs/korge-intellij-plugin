package com.soywiz.korge.intellij

import org.intellij.lang.annotations.*

object KorgeResources {
	val classLoader = KorgeResources::class.java.classLoader

	fun getBytes(@Language("file-reference") path: String) = classLoader.getResource(path)?.readBytes() ?: error("Can't find resource '$path'")

	val KORGE_IMAGE get() = getBytes("/com/soywiz/korge/intellij/generator/korge.png")
}