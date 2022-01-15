package com.soywiz.korge.intellij.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.*
import com.intellij.util.xmlb.*
import com.soywiz.korge.intellij.*

@State(
	name = "KorgeProjectSettings",
	storages = [Storage("korge.xml")]
)
open class KorgeProjectSettings : PersistentStateComponent<KorgeProjectSettings> {
	var hello: String = "world"

	init {
		println("KorgeProjectSettings.init")
		//ActionManager.getInstance().group
	}

	override fun getState() = this

	override fun loadState(state: KorgeProjectSettings) {
		XmlSerializerUtil.copyBean(state, this)
	}
}

val Project.korgeProjectSettings: KorgeProjectSettings get() = getService()
