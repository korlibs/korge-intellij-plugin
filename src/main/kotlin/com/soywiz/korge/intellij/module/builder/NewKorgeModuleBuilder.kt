package com.soywiz.korge.intellij.module.builder

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.soywiz.korge.intellij.module.*

class NewKorgeModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = KorgeModuleType.INSTANCE

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
    }
}
