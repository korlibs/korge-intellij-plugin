package com.soywiz.korge.intellij.filetype

import com.intellij.ide.highlighter.*
import com.intellij.lang.xml.*
import com.intellij.openapi.fileTypes.*
import com.soywiz.korge.intellij.*
import javax.swing.*

open class KorgeAniFileType : UserBinaryFileType() {
	override fun getName(): String = "KORGE_ANI"
}

open class KorgeAudioFileType : UserBinaryFileType() {
	override fun getName(): String = "KORGE_AUDIO"
}

open class TmxFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.TILED
	override fun getName(): String = "TMX"
	override fun getDefaultExtension(): String = "tmx"
	override fun getDescription(): String = "Tiled Map Files"
}

open class TsxFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.TILED
	override fun getName(): String = "TSX"
	override fun getDefaultExtension(): String = "tsx"
	override fun getDescription(): String = "Tiled Tileset Files"
}

open class PexFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.PARTICLE
	override fun getName(): String = "PEX"
	override fun getDefaultExtension(): String = "pex"
	override fun getDescription(): String = "Particle Definitions"
}

open class ScmlFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.SPRITER
	override fun getName(): String = "SCML"
	override fun getDefaultExtension(): String = "scml"
	override fun getDescription(): String = "Spriter Text File"
}

open class SconFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.SPRITER
	override fun getName(): String = "SCON"
	override fun getDefaultExtension(): String = "scon"
	override fun getDescription(): String = "Spriter Binary File"
}

open class FntFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = KorgeIcons.BITMAP_FONT
	override fun getName(): String = "FNT"
	override fun getDefaultExtension(): String = "fnt"
	override fun getDescription(): String = "Font Definition File"
}
