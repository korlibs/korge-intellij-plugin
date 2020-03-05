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

open class XmlBaseType(
	val _icon: Icon,
	val _name: String,
	val _extension: String,
	val _description: String
) : XmlLikeFileType(XMLLanguage.INSTANCE) {
	override fun getIcon(): Icon? = _icon
	override fun getName(): String = _name
	override fun getDefaultExtension(): String = _extension
	override fun getDescription(): String = _description
}

open class TmxFileType : XmlBaseType(KorgeIcons.TILED, "TMX", "tmx", "Tiled Map Files")
open class TsxFileType : XmlBaseType(KorgeIcons.TILED, "TSX", "tsx", "Tiled Tileset Files")
open class PexFileType : XmlBaseType(KorgeIcons.PARTICLE, "PEX", "pex", "Particle Definitions")
open class ScmlFileType : XmlBaseType(KorgeIcons.SPRITER, "SCML", "scml", "Spriter Text File")
open class SconFileType : XmlBaseType(KorgeIcons.SPRITER, "SCON", "scon", "Spriter Binary File")
open class FntFileType : XmlBaseType(KorgeIcons.BITMAP_FONT, "FNT", "fnt", "Font Definition File")
