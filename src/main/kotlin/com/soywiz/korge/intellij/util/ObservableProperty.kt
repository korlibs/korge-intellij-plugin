package com.soywiz.korge.intellij.util

import com.soywiz.korio.async.Signal
import kotlin.reflect.KProperty

class ObservableProperty<T>(val initial: T, val adjust: (T) -> T = { it }) {
	val changed = Signal<T>()

	var value: T = initial
		set(value) = run { field = adjust(value) }.also { changed(field) }

	operator fun invoke(handler: (T) -> Unit) {
		changed(handler)
	}

	operator fun getValue(obj: Any?, property: KProperty<*>): T {
		return value
	}

	operator fun setValue(obj: Any?, property: KProperty<*>, value: T) {
		this.value = value
	}
}
