package com.soywiz.korge.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.soywiz.korge.intellij.config.KorgeGlobalSettings
import com.soywiz.korge.intellij.config.korgeGlobalSettings
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.LayoutManager
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

// Used as reference:
//   com.intellij.ui.ExperimentalUIConfigurable
class KorgeSettingsPageConfigurableProvider : ConfigurableProvider() {

    override fun createConfigurable(): Configurable? {
        return object : BoundSearchableConfigurable(
            "KorGE",
            "com.soywiz.korge.settings.KorgeSettingsPageConfigurableProvider"
        ) {
            //val tempGlobalSettings = KorgeGlobalSettings().also {
            //    it.useLocalStore = korgeGlobalSettings.useLocalStore
            //}

            override fun createPanel(): DialogPanel {
                return panel {
                    row {
                        checkBox("Use Local KorGE Store")
                            .bindSelected({ korgeGlobalSettings.useLocalStore }, {
                                korgeGlobalSettings.useLocalStore = it
                                println("korgeGlobalSettings.useLocalStore = $it : ${korgeGlobalSettings.useLocalStore}")
                            })
                            .comment("Uses http://127.0.0.1:4000/ instead of https://store.korge.org/ for development purposes")
                    }
                }
            }

            override fun isModified(): Boolean = true

            override fun reset() {
            }

            override fun apply() {
                super.apply()
                //if (isModified) {
                    //korgeGlobalSettings.useLocalStore = korgeGlobalSettings.useLocalStore
                //}
            }

            @Nls(capitalization = Nls.Capitalization.Title)
            override fun getDisplayName(): String = "KorGE"
            override fun getId(): String = "org.korge.KorgeSettingsPageConfigurable"

            //override fun enableSearch(option: String?): Runnable? {
            //    println("KorgeSettingsPageConfigurableProvider.enableSearch: $option")
            //    return null
            //}
        }
    }
}
