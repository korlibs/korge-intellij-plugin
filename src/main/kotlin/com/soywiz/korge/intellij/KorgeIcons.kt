package com.soywiz.korge.intellij

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.*

// https://jetbrains.design/intellij/resources/icons_list/
object KorgeIcons {
    @JvmStatic val KTREE: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
    @JvmField val JITTO: Icon = getIcon("/com/soywiz/korge/intellij/icon/jitto.png")
	@JvmStatic val PARTICLE: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/particle.png") }
	@JvmStatic val BITMAP_FONT: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/bitmap_font.png") }
	@JvmStatic val KRITA: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/krita.png") }
    @JvmStatic val QOI: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/qoi.png") }
	@JvmStatic val SWF: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/swf.png") }
	@JvmStatic val TILED: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/tiled.png") }
	@JvmStatic val KORGE: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
	@JvmStatic val SPONSOR: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/sponsor.png") }
	@JvmStatic val VOICE: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/lips.png") }
	@JvmStatic val SPRITER: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/spriter.png") }
	@JvmStatic val SOUND: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/sound.png") }
	@JvmStatic val ATLAS: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/atlas.png") }
    @JvmStatic val SPINE: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/spine.png") }
    @JvmStatic val DRAGONBONES: Icon by lazy { getIcon("/com/soywiz/korge/intellij/icon/dragonbones.png") }

	val USER_UNKNOWN_BYTES by lazy { getResourceBytes("/com/soywiz/korge/intellij/image/user_unknown.png") }

    fun getResourceBytes(path: String): ByteArray? =
        try {
            KorgeIcons::class.java.classLoader.getResourceAsStream(normalizePath(path))?.readBytes()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

    fun getIcon(path: String): Icon = try {IconLoader.getIcon(normalizePath(path), KorgeIcons::class.java)
    } catch (e: Throwable) {
        e.printStackTrace()
        AllIcons.Ide.ErrorPoint
    }

    private fun normalizePath(path: String): String = path.trimStart('/')
}
