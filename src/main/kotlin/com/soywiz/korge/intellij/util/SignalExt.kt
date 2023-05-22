package com.soywiz.korge.intellij.util

import korlibs.io.async.Signal
import korlibs.io.lang.Closeable

fun <T> Signal<T>.addCallInit2(initial: T, handler: (T) -> Unit): Closeable {
	handler(initial)
	return add(handler)
}

fun Signal<Unit>.addCallInit2(handler: (Unit) -> Unit): Closeable = addCallInit2(Unit, handler)
