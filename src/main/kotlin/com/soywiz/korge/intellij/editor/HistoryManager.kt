package com.soywiz.korge.intellij.editor

class HistoryManager {
	class Entry(val name: String, val apply: (redo: Boolean) -> Unit) {
		fun redo() = apply(true).also { println("REDO: $this") }
		fun undo() = apply(false).also { println("UNDO: $this") }
		override fun toString(): String = "HistoryManager.Entry('$name')"
	}

	var cursor = 0
	val entries = arrayListOf<Entry>()

	fun add(entry: Entry): Entry {
		while (cursor < entries.size) entries.removeAt(entries.size - 1)
		this.entries.add(entry)
		println("ADD: $entry")
		cursor = this.entries.size
		return entry
	}

	fun add(name: String, apply: (redo: Boolean) -> Unit) = add(Entry(name, apply))
	fun addAndDo(name: String, apply: (redo: Boolean) -> Unit) = add(Entry(name, apply)).redo()

	fun undo() {
		if (cursor > 0) {
			entries.getOrNull(--cursor)?.undo()
		}
	}

	fun redo() {
		if (cursor <= entries.size) {
			entries.getOrNull(cursor++)?.redo()
		}
	}
}