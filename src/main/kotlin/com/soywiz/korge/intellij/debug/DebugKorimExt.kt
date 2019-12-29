package com.soywiz.korge.intellij.debug

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.sun.jdi.*

fun ObjectReference.readKorimBitmap32(thread: ThreadReference? = null): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<Bitmap>()) error("Not a korim bitmap")
	val width = value.invoke("getWidth", listOf(), thread = thread).int(0)
	val height = value.invoke("getHeight", listOf(), thread = thread).int(0)
	val premultiplied = value.invoke("getPremultiplied", listOf(), thread = thread).bool(false)
	val bmp32Mirror = value.invoke("toBMP32", listOf(), thread = thread) as ObjectReference
	val dataInstance = (bmp32Mirror.invoke("getData", listOf(), thread = thread) as ObjectReference).debugToLocalInstanceViaSerialization(thread = thread) as IntArray
	return Bitmap32(width, height, RgbaArray(dataInstance.copyOf()), premultiplied)
}

