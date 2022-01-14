package com.soywiz.korge.intellij.util

import java.awt.*

val Rectangle.left get() = this.x
val Rectangle.top get() = this.y
val Rectangle.right get() = this.x + this.width
val Rectangle.bottom get() = this.y + this.height
