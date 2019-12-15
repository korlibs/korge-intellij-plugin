package com.soywiz.korge.intellij.config

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.*
import com.soywiz.korge.intellij.*

@State(
	name = "KorgeGlobalSettings",
	storages = [Storage("korge.xml")]
)
open class KorgeGlobalSettings : PersistentStateComponent<KorgeGlobalSettings> {
	var hello: String = "world"

	init {
		println("KorgeGlobalSettings.init")
	}

	override fun getState() = this

	override fun loadState(state: KorgeGlobalSettings) {
		XmlSerializerUtil.copyBean(state, this)
	}
}

val korgeGlobalSettings: KorgeGlobalSettings get() = getService()
