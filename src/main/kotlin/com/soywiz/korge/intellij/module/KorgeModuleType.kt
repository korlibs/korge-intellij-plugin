package com.soywiz.korge.intellij.module

import com.intellij.openapi.module.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.module.builder.*
import javax.swing.*

//open class KorgeModuleType : JpsJavaModuleType("korge") {
//open class KorgeModuleType : JavaModuleType("korge") {
open class KorgeModuleType : ModuleType<KorgeWizardModuleBuilder>("korge") {
	//open class KorgeModuleType : EmptyModuleType("korge") {
	companion object {
		val INSTANCE = KorgeModuleType()
		val NAME = "KorGE Game"
		val DESCRIPTION = "KorGE Game Engine"
		val BIG_ICON = KorgeIcons.JITTO
		val ICON = KorgeIcons.JITTO
	}

	override fun createModuleBuilder() = KorgeWizardModuleBuilder()
	override fun getName(): String = NAME
	override fun getDescription(): String = DESCRIPTION
	override fun getIcon(): Icon = BIG_ICON
	override fun getNodeIcon(isOpened: Boolean): Icon = ICON
}
