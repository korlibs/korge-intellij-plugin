package com.soywiz.korge.intellij.util

import com.soywiz.korio.async.Signal
import com.soywiz.korio.lang.Closeable

fun <T> Signal<T>.addCallInit2(initial: T, handler: (T) -> Unit): Closeable {
	handler(initial)
	return add(handler)
}

fun Signal<Unit>.addCallInit2(handler: (Unit) -> Unit): Closeable = addCallInit2(Unit, handler)
