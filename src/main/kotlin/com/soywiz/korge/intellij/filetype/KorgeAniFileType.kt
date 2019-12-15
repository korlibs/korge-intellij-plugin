package com.soywiz.korge.intellij.filetype

import com.intellij.openapi.fileTypes.*

open class KorgeAniFileType : UserBinaryFileType() {
	override fun getName(): String = "KORGE_ANI"
}