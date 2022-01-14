package com.soywiz.korge.intellij.util

import java.awt.*

inline fun Component.getAncestor(cond: (Component) -> Boolean): Component? {
    var current: Component? = this
    while (current != null) {
        if (cond(current)) return current
        current = current.parent
    }
    return null
}

fun Component.parentFrame(): Frame? {
    return getAncestor { it is Frame } as? Frame?
}

fun Component.hasAncestor(other: Component): Boolean {
    return getAncestor { it == other } != null
}
