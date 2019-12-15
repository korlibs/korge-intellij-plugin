package com.soywiz.korge.intellij.filetype

import com.intellij.openapi.fileTypes.*

open class KorgeAudioFileType : UserBinaryFileType() {
	override fun getName(): String = "KORGE_AUDIO"
}
