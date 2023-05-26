package com.soywiz.korge.intellij.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.soywiz.korge.intellij.getService
import com.soywiz.korge.intellij.image.KorimImageReaderRegister
import com.soywiz.korge.intellij.util.getResourceText
import korlibs.time.days
import korlibs.time.milliseconds
import java.net.URL

@State(
	name = "KorgeGlobalSettings",
	storages = [Storage("korge.xml")]
)
open class KorgeGlobalSettings : PersistentStateComponent<KorgeGlobalSettings>, DumbAware {
	var cachedTemplateLastRefreshTime: Long = 0L
	var cachedTemplateString: String? = null
    var useLocalStore: Boolean = false

	init {
		//println("KorgeGlobalSettings.init")
        KorimImageReaderRegister.initialize
	}

	companion object {
		fun compareKorgeTemplateVersion(xml1: String, xml2: String): Int {
			val versionRegex = Regex("<korge-templates version=\"(\\d+)\">")
			val result1 = versionRegex.find(xml1)
			val result2 = versionRegex.find(xml2)
			val v1 = result1?.groupValues?.get(1)?.toIntOrNull() ?: 0
			val v2 = result2?.groupValues?.get(1)?.toIntOrNull() ?: 0
			return v2.compareTo(v1)
		}
	}
	
	fun invalidateCache() {
		cachedTemplateString = null
		cachedTemplateLastRefreshTime = 0L
	}

	fun getCachedTemplate(): String {
		//cachedTemplateLastRefreshTime = 0L // Force cache invalidation
		val now = System.currentTimeMillis()
		val elapsedTimeSinceLastCheck = (now - cachedTemplateLastRefreshTime).milliseconds

		val resource = getResourceText("/com/soywiz/korge/intellij/korge-templates.xml")

		if (cachedTemplateString == null || elapsedTimeSinceLastCheck >= 1.days) {
			cachedTemplateLastRefreshTime = now

			println("Refreshing cached korge-templates.xml :: elapsedTimeSinceLastCheck=${elapsedTimeSinceLastCheck.hours}h")

			val online = runCatching { URL("https://raw.githubusercontent.com/korlibs/korge-intellij-plugin/master/src/main/resources/com/soywiz/korge/intellij/korge-templates.xml").readText() }.getOrNull()

			cachedTemplateString = when {
				online != null && resource != null -> if (compareKorgeTemplateVersion(online, resource) <= 0) {
					println(" - korge-templates.xml: Using online version")
					online
				} else {
					println(" - korge-templates.xml: Using resources version")
					""
				}
				else -> online ?: resource ?: error("Can't get a valid 'korge-templates.xml' file from any source")
			}
		} else {
			println("Using cached korge-templates.xml :: elapsedTimeSinceLastCheck=${elapsedTimeSinceLastCheck.hours}h :: cachedTemplateString.isNullOrEmpty()=${cachedTemplateString.isNullOrEmpty()}")
		}

		return if (cachedTemplateString.isNullOrEmpty()) resource ?: "" else cachedTemplateString!!
	}

	override fun getState() = this

	override fun loadState(state: KorgeGlobalSettings) {
        //println("KorgeGlobalSettings.loadState: $state")
		XmlSerializerUtil.copyBean(state, this)
	}

    override fun noStateLoaded() {
        //println("KorgeGlobalSettings.noStateLoaded")
    }

    override fun initializeComponent() {
        //println("KorgeGlobalSettings.initializeComponent")
    }
}

val korgeGlobalSettings: KorgeGlobalSettings by lazy { getService() }
fun korgeGlobalSettings(project: Project? = null): KorgeGlobalSettings {
    return project?.getService() ?: getService()
}
