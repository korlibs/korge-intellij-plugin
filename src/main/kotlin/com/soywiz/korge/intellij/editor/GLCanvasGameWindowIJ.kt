package com.soywiz.korge.intellij.editor

import com.intellij.openapi.ui.*
import com.soywiz.klogger.*
import com.soywiz.korgw.awt.*
import javax.swing.*

class GLCanvasGameWindowIJ(canvas: GLCanvas) : GLCanvasGameWindow(canvas) {
    init {
        exitProcessOnExit = false
    }

    override fun showContextMenu(items: List<MenuItem>) {
        val popupMenu = JBPopupMenu()
        for (item in items) {
            if (item?.text == null) {
                //popupMenu.add(JSeparator())
                //popupMenu.add(JBMenuItem(null as? String?))
                popupMenu.add(JBMenuItem("-"))
            } else {
                popupMenu.add(JBMenuItem(item.text).also {
                    it.isEnabled = item.enabled
                    it.addActionListener {
                        item.action()
                    }
                })
            }
        }
        popupMenu.show(contentComponent, mouseX, mouseY)
    }
}
