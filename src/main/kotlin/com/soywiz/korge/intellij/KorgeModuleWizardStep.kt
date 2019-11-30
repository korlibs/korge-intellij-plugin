package com.soywiz.korge.intellij

import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.module.*
import com.intellij.openapi.options.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.ui.configuration.*
import javax.swing.*

class KorgeModuleWizardStep : ModuleBuilder() {
	@Throws(ConfigurationException::class)
	override fun setupRootModel(rootModel: ModifiableRootModel) {
		//modifiableRootModel.
	}

	override fun getModuleType(): ModuleType<*> {
		//return JavaModuleType.getModuleType()
		return ModuleType.EMPTY
	}

	override fun createWizardSteps(
		wizardContext: WizardContext,
		modulesProvider: ModulesProvider
	): Array<ModuleWizardStep> {
		return arrayOf(object : ModuleWizardStep() {
			override fun getComponent(): JComponent {
				return JLabel("Put your content here")
			}

			override fun updateDataModel() {
			}
		})
	}
}
