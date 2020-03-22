package com.soywiz.korge.intellij.util

import java.awt.Graphics2D

inline fun Graphics2D.preserveStroke(block: () -> Unit) {
	val oldStroke = stroke
	try {
		block()
	} finally {
		stroke = oldStroke
	}
}