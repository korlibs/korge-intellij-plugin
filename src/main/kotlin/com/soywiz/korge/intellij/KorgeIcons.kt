package com.soywiz.korge.intellij

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.*

// https://jetbrains.design/intellij/resources/icons_list/
object KorgeIcons {
    val KTREE by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
	val PARTICLE by lazy { getIcon("/com/soywiz/korge/intellij/icon/particle.png") }
	val BITMAP_FONT by lazy { getIcon("/com/soywiz/korge/intellij/icon/bitmap_font.png") }
	val KRITA by lazy { getIcon("/com/soywiz/korge/intellij/icon/krita.png") }
	val SWF by lazy { getIcon("/com/soywiz/korge/intellij/icon/swf.png") }
	val TILED by lazy { getIcon("/com/soywiz/korge/intellij/icon/tiled.png") }
	val KORGE by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
	val SPONSOR by lazy { getIcon("/com/soywiz/korge/intellij/icon/sponsor.png") }
	val VOICE by lazy { getIcon("/com/soywiz/korge/intellij/icon/lips.png") }
	val SPRITER by lazy { getIcon("/com/soywiz/korge/intellij/icon/spriter.png") }
	val SOUND by lazy { getIcon("/com/soywiz/korge/intellij/icon/sound.png") }
	val ATLAS by lazy { getIcon("/com/soywiz/korge/intellij/icon/atlas.png") }
    val SPINE by lazy { getIcon("/com/soywiz/korge/intellij/icon/spine.png") }
    val DRAGONBONES by lazy { getIcon("/com/soywiz/korge/intellij/icon/dragonbones.png") }

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
