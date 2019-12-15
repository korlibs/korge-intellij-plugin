package com.soywiz.korge.intellij.config

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.*
import com.soywiz.klock.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.module.*
import java.net.*

@State(
	name = "KorgeGlobalSettings",
	storages = [Storage("korge.xml")]
)
open class KorgeGlobalSettings : PersistentStateComponent<KorgeGlobalSettings> {
	var cachedTemplateLastRefreshTime: Long = 0L
	var cachedTemplateString: String? = null

	init {
		println("KorgeGlobalSettings.init")
	}

	fun getCachedTemplate(): String {
		val now = System.currentTimeMillis()
		if (cachedTemplateString == null || (now - cachedTemplateLastRefreshTime).milliseconds >= 1.days) {
			cachedTemplateLastRefreshTime = now
			cachedTemplateString =
				runCatching { URL("https://raw.githubusercontent.com/korlibs/korge-intellij-plugin/master/src/main/resources/com/soywiz/korge/intellij/korge-templates.xml").readText() }.getOrNull()
					?: runCatching { KorgeProjectTemplate::class.java.getResource("/com/soywiz/korge/intellij/korge-templates.xml")?.readText() }.getOrNull()
						?: error("Can't get a valid 'korge-templates.xml' file from any source")
		}
		return cachedTemplateString!!
	}

	override fun getState() = this

	override fun loadState(state: KorgeGlobalSettings) {
		XmlSerializerUtil.copyBean(state, this)
	}
}

val korgeGlobalSettings: KorgeGlobalSettings get() = getService()
