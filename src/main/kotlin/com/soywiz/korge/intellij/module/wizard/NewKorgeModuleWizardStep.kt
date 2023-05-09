package com.soywiz.korge.intellij.module.wizard

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.actions.jacksonObjectMapper
import com.soywiz.korge.intellij.module.*
import com.soywiz.korge.intellij.module.ProjectType
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.color.toRgba
import java.awt.*
import javax.swing.*

data class KorgeTemplateRow(val title: String, val template: KorgeTemplate?)

data class KorgeTemplate(
    val title: String,
    val authors: List<String>?,
    val category: String,
    val description: String,
    val screenshot: String?,
    val enabled: Boolean,
    val zip: String,
) {
    val screenshotSure: String get() = screenshot ?: "https://korge.org/assets/images/logo/logo.svg"
    override fun toString(): String = title
}

class NewKorgeModuleWizardStep(
    val korgeProjectTemplateProvider: KorgeProjectTemplate.Provider,
    val config: KorgeModuleConfig
) : ModuleWizardStep() {
    companion object {
        val TEMPLATES_URL get() = when {
            isDevelopmentMode -> "https://store.korge.org/templates.json"
            else -> "http://127.0.0.1:4000/templates.json"
        }
        val TEMPLATES_DEFAULT_CONTENT = """
            [
                {
                    "title": "Hello World",
                    "authors": ["korlibs"],
                    "category": "Starter Kits",
                    "repo": "https://github.com/korlibs/korge-hello-world",
                    "zip": "https://github.com/korlibs/korge-hello-world/archive/refs/heads/main.zip",
                    "branch": "main",
                    "enabled": true,
                    "screenshot": null,
                    "description": "<p>A simple Hello World</p>\n"
                }
            ]
        """.trimIndent()
    }
    override fun updateDataModel() {
        config.projectType = ProjectType.Gradle
        config.template = featureList.selectedValue?.template
        //config.featuresToInstall = featuresToCheckbox.keys.filter { it.selected }
        config.korgeVersion = KorgeProjectTemplate.Versions.Version.LAST_KNOWN
        println("KorgeModuleWizardStep.updateDataModel: projectType:${config.projectType}, korgeVersion:${config.korgeVersion}, featuresToInstall:${config.featuresToInstall.size}")
    }

    lateinit var featureList: JBList<KorgeTemplateRow>

    init {
        println("Created KorgeModuleWizardStep")
    }

    fun parseTemplates(json: String): List<KorgeTemplate> {
        return jacksonObjectMapper.readValue<List<KorgeTemplate>>(json)
    }

    fun createTemplateRows(list: List<KorgeTemplate>): List<KorgeTemplateRow> {
        val out = arrayListOf<KorgeTemplateRow>()
        for ((category, templates) in list.groupBy { it.category }) {
            out.add(KorgeTemplateRow(category, null))
            for (template in templates) {
                if (template.enabled) {
                    out.add(KorgeTemplateRow(template.title, template))
                }
            }
        }
        return out
    }

    fun <T> ListModel<T>.toList(): List<T> = (0 until size).map { this.getElementAt(it) }

    val panel by lazy {
        println("Created KorgeModuleWizardStep.panel")
        JPanel().apply {
            border = JBUI.Borders.empty(28, 20)

            val descriptionHtml = JCEFHtmlPanel("")
            val description = descriptionHtml.component.also {
                preferredHeight = 200
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = IdeBorderFactory.createBorder()
            }

            fun showFeatureDocumentation(row: KorgeTemplateRow?) {
                //println("showFeatureDocumentation: $row")

                //description.removeAll()
                if (row?.template != null) {
                    descriptionHtml.setHtml("""
                        <html>
                            <head>
                            <style>
                            p { padding: 0; margin: 0; }
                            </style>
                            </head>
                            <body style="margin:0;padding:0;font: 14px Arial; color: ${UIUtil.getLabelForeground().toRgba()};">
                            <div style="display: flex;">
                                <div style="flex: 0 0 100vh;"><img src="${row.template.screenshotSure}" style="max-width:256px;max-height:100vh;margin-right: 8px;margin-left:8px;"></div>
                                <div style="flex: 1;">
                                    <p>
                                    <strong>Authors:</strong>
                                    ${(row.template.authors ?: emptyList()).joinToString(", ")}
                                    </p>
                                    ${row.template.description}
                                </div>
                            </div>
                            </body>
                        </html>
                    """.trimIndent())
                    //description.add(JLabel(row.template.description, SwingConstants.LEFT))
                    //for (artifact in feature.artifacts) description.add(JLabel(artifact))
                    //val doc = row.template.documentation
                    //description.add(Link(doc, URL(doc)))
                }
                description.doLayout()
                description.repaint()
            }

            val featureListModel = DefaultListModel<KorgeTemplateRow>()
            featureList = JBList(featureListModel).also {
                //it.isEnabled = false
            }
            //featureListModel.rootPane.border = BorderFactory.createMatteBorder(2, 2, 2, 2, Color(0x5C1A95));
            featureList.cellRenderer =
                ListCellRenderer<KorgeTemplateRow> { list, value, index, isSelected, cellHasFocus ->
                    JLabel(value.title).also {
                        it.isEnabled = value.template != null
                        val horizontalAlignment = if (value.template != null) 16 else 8
                        val verticalAlignment = if (value.template != null) 3 else 8
                        it.horizontalAlignment = JLabel.LEFT
                        it.verticalAlignment = JLabel.TOP
                        it.border = BorderFactory.createEmptyBorder(verticalAlignment, horizontalAlignment, verticalAlignment, 1)
                    }
                    //TODO("Not yet implemented")
                }
            featureList.selectionModel = object : DefaultListSelectionModel() {
                override fun setSelectionInterval(index0: Int, index1: Int) {
                    var index0 = index0
                    var index1 = index1
                    val currentSelectedIndex = this.selectedIndices.firstOrNull() ?: 0
                    val direction = when {
                        currentSelectedIndex - index0 > 0 -> -1
                        else -> +1
                    }
                    //println("currentSelectedIndex=$currentSelectedIndex, index0=$index0, index1=$index1")
                    try {
                        for (n in 0 until 10) {
                            if (featureListModel.getElementAt(index0).template == null) {
                                index0 += direction
                                index1 += direction
                            } else {
                                super.setSelectionInterval(index0, index0)
                            }
                        }
                    } catch (e: ArrayIndexOutOfBoundsException) {
                    }
                }
            }

            featureList.addListSelectionListener {
                try {
                    //println("it.firstIndex=${featureList.selectedIndex}")
                    showFeatureDocumentation(
                        when (featureList.selectedIndex) {
                            in 0 until featureListModel.size -> featureListModel.getElementAt(featureList.selectedIndex)
                            else -> null
                        }
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            this.layout = BorderLayout(0, 0)

            add(Splitter(true, 0.7f, 0.2f, 0.8f).apply {
                this.firstComponent = JPanel().also {
                    it.layout = BorderLayout(0, 0)
                    it.add(JLabel("Templates:").also { it.border = JBUI.Borders.emptyBottom(8) }, BorderLayout.NORTH)
                    //it.add(JLabel("Templates:"), BorderLayout.NORTH)
                    it.add(featureList.scrollVertical(), BorderLayout.CENTER)
                }
                this.secondComponent = description
            }, BorderLayout.CENTER)

            fun updateTemplateRows(templates: List<KorgeTemplate>) {
                featureListModel.also { it.clear() }.addAll(createTemplateRows(templates))
                val firstTemplateIndex = featureListModel.toList().indexOfFirst { it.template != null }
                //println("firstTemplateIndex=$firstTemplateIndex, featureList.model.size=${featureList.model.size}")
                featureList.selectedIndex = firstTemplateIndex
            }

            updateTemplateRows(parseTemplates((getUrlCached(TEMPLATES_URL) ?: TEMPLATES_DEFAULT_CONTENT.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)))

            runBackgroundableTask("Downloading templates") {
                updateTemplateRows(parseTemplates(downloadUrlCached(TEMPLATES_URL).toString(Charsets.UTF_8)).toMutableList())
            }
        }
    }

    // @TODO: Can we get the project from somewhere here? By being a module wizard, we don't have the project?
    val project: Project? get() = null

    /*
    fun refresh(invalidate: Boolean = false) {
        fun toggleEnabled(enabled: Boolean) {
            featureList.isEnabled = enabled
            //for (checkbox in featureList.featuresToCheckbox.values) {
            //    checkbox.isEnabled = enabled
            //}
        }

        toggleEnabled(false)

        runBackgroundTaskGlobal {
            // Required since this is blocking
            if (invalidate) {
                korgeProjectTemplateProvider.invalidate(project)
            }
            val korgeProjectTemplate = korgeProjectTemplateProvider.template(project)

            runInUiThread {
                //versionCB.model = DefaultComboBoxModel(korgeProjectTemplate.versions.versions.toTypedArray())
                //featureList.features = korgeProjectTemplateProvider.template(project).features.features.toList()
                toggleEnabled(true)
            }
        }
    }

     */

    //val featuresToCheckbox get() = featureList.featuresToCheckbox
//
    //var Feature.selected: Boolean
    //    get() = featuresToCheckbox[this]?.isChecked ?: false
    //    set(value) = run { featuresToCheckbox[this]?.isChecked = value }
//
    //var Feature.indeterminate: Boolean
    //    get() = featuresToCheckbox[this]?.indeterminate ?: false
    //    set(value) = run { featuresToCheckbox[this]?.indeterminate = value }

    //var Feature.indeterminate : Boolean
    //    get() = featuresToCheckbox[this]?. ?: false
    //    set(value) {
    //        featuresToCheckbox[this]?.isSelected = value
    //    }

    //fun updateTransitive() {
    //    val features = korgeProjectTemplateProvider.template(project).features
    //    val allFeatures = features.features.toList()
    //    val featureSet = FeatureSet(allFeatures.filter { it.selected }, features.allFeatures)
//
    //    for (feature in allFeatures) {
    //        feature.indeterminate = (feature in featureSet.transitive)
    //    }
//
    //    featureList.repaint()
    //}

    override fun getComponent() = JPanel().also {
        it.layout = BorderLayout(0, 0)
        it.border = JBUI.Borders.empty(28, 20)
        it.add(this@NewKorgeModuleWizardStep.panel, BorderLayout.CENTER)
    }
}
