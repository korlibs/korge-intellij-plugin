package com.soywiz.korge.intellij.editor

import com.soywiz.korio.async.Signal

class HistoryManager {
	class Entry(val name: String, val apply: (redo: Boolean) -> Unit) {
		fun redo() = apply(true).also { println("REDO: $this") }
		fun undo() = apply(false).also { println("UNDO: $this") }
		override fun toString(): String = "HistoryManager.Entry('$name')"
	}

	var cursor = 0
	val entries = arrayListOf<Entry>()
	val onChange = Signal<Unit>()

	fun add(entry: Entry): Entry {
		while (cursor < entries.size) entries.removeAt(entries.size - 1)
		this.entries.add(entry)
		println("ADD: $entry")
		cursor = this.entries.size
		onChange(Unit)
		return entry
	}

	fun add(name: String, apply: (redo: Boolean) -> Unit) = add(Entry(name, apply))
	fun addAndDo(name: String, apply: (redo: Boolean) -> Unit) = add(Entry(name, apply)).redo()

	fun undo(): Boolean {
		if (cursor > 0) {
			val entry = entries.getOrNull(--cursor)
			if (entry != null) {
				entry.undo()
				onChange(Unit)
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