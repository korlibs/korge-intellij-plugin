package com.soywiz.korge.intellij.util

import com.intellij.openapi.editor.Editor
import com.intellij.ui.ColorChooserService
import java.awt.Color

fun Editor.chooseColor(color: Color? = null): Color? {
    val editor = this
    return ColorChooserService.getInstance().showDialog(editor.project,
        editor.component, "Choose Color", color, true, listOf(), true)
}
