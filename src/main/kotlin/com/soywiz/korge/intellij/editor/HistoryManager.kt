package com.soywiz.korge.intellij.editor

import com.soywiz.korio.async.Signal

class HistoryManager {
	class Entry(val cursor: Int, val name: String, val apply: (redo: Boolean) -> Unit) {
		fun redo() = apply(true).also { println("REDO: $this") }
		fun undo() = apply(false).also { println("UNDO: $this") }
		override fun toString(): String = "HistoryManager.Entry('$name')"
	}

	var cursor = 0
	val entries = arrayListOf<Entry>()
	val onChange = Signal<Unit>()
	val isModified: Boolean get() = cursor != 0
	val onAdd = Signal<Entry>()
	val onUndo = Signal<Entry>()
	val onRedo = Signal<Entry>()

	fun add(name: String, apply: (redo: Boolean) -> Unit): Entry {
		while (cursor < entries.size) entries.removeAt(entries.size - 1)
		val entry = Entry(this.entries.size + 1, name, apply)
		this.entries.add(entry)
		println("ADD: $entry")
		cursor = entry.cursor
		onChange(Unit)
		onAdd(entry)
		return entry
	}
	fun addAndDo(name: String, apply: (redo: Boolean) -> Unit) = add(name, apply).redo()

	fun undo(): Boolean {
		if (cursor > 0) {
			val entry = entries.getOrNull(--cursor)
			if (entry != null) {
				entry.undo()
				onChange(Unit)
				onUndo(entry)
				return true
			}
		}
		return false
	}

	fun redo(): Boolean {
		if (cursor <= entries.size) {
			val entry = entries.getOrNull(cursor++)
			if (entry != null) {
				entry.redo()
				onChange(Unit)
				onRedo(entry)
				return true
			}
		}
		return false
	}

	fun moveTo(index: Int) {
		while (cursor < index) if (!redo()) break
		while (cursor > index) if (!undo()) break
	}
}