package com.soywiz.korge.intellij.util

import com.soywiz.korge.awt.*
import java.awt.*
import javax.swing.*

fun Styled<out Container>.icon(icon: Icon, tooltip: String? = null, block: @UIDslMarker Styled<JLabel>.() -> Unit = {}) {
    //println("ICON: ${icon.iconWidth}x${icon.iconHeight}")
    component.add(
        JLabel(icon)
            .also {
                //it.iconTextGap = 0
                //it.margin = Insets(0, 0, 0, 0)
                //it.toolTipText = tooltip
                //it.border = BorderFactory.createEmptyBorder()
            }
            .also {
                it.styled
                    .also {
                        it.preferred = MUnit2(32.pt)
                        it.min = MUnit2(32.pt)
                        it.max = MUnit2(32.pt)
                    }
                    .block()
            }
    )
}

fun Styled<out Container>.buttonWithIcon(text: String, icon: Icon, block: @UIDslMarker Styled<JButton>.() -> Unit = {}) {
    component.add(JButton(text).also {
        it.icon = icon
        block(it.styled)
    })
}

