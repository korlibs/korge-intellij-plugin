package com.soywiz.korge.intellij.module.builder

import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.*
import com.intellij.openapi.module.*
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.util.io.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.intellij.module.*
import com.soywiz.korge.intellij.module.wizard.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korio.file.std.ZipVfs
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.stream.openAsync
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.jetbrains.plugins.gradle.service.project.*
import org.jetbrains.plugins.gradle.service.project.open.*
import java.io.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class KorgeWizardModuleBuilder() : ModuleBuilder() {
	val SILENT_GRADLE_IMPORT = false

    override fun getModuleType(): ModuleType<*> = KorgeModuleType.INSTANCE
	override fun getPresentableName() = KorgeModuleType.NAME
	override fun getNodeIcon() = KorgeModuleType.ICON
	override fun getGroupName() = KorgeModuleType.NAME
	//override fun getWeight() = BUILD_SYSTEM_WEIGHT - 1
	override fun getBuilderId() = "korge.module.builder"
	override fun isSuitableSdkType(sdk: SdkTypeId?) = sdk === JavaSdk.getInstance()

	val korgeProjectTemplateProvider = KorgeProjectTemplate.provider()
	var config = KorgeModuleConfig()

	override fun setupRootModel(rootModel: ModifiableRootModel) {
		super.setupRootModel(rootModel)

		val root = createAndGetRoot() ?: return
		val project = rootModel.project
		val module = rootModel.module
		rootModel.addContentEntry(root)
		if (moduleJdk != null) rootModel.sdk = moduleJdk

		project.backgroundTask("Setting Up Project") { indicator ->
			val info = config

			runBlocking {
                val template = config.template ?: error("Template was not selected!")
                indicator.isIndeterminate = true
                indicator.text2 = "Downloading ${template.zip}..."
                val templateZipByteArray = downloadUrlCached(template.zip)
                indicator.text2 = "Unzipping..."
                val zip = ZipVfs(templateZipByteArray.openAsync())
                val firstFolder = zip.list().first { it.isDirectory() }
                val outputFile = root.toNioPath().toFile().toVfs()
                println("Unzipping $firstFolder into $outputFile")
                firstFolder.copyRecursively(outputFile)
                println("Unzipping completed")
                indicator.text2 = "Loading gradle"

                println("Refreshing root")
                root.refresh(false, true)

                /*
				for ((fileName, fileContent) in config.generate(korgeProjectTemplateProvider.template(project))) {
					root.createFile(fileName, fileContent, FileMode("0777"))
				}
                 */
			}

			when (info.projectType) {
				ProjectType.Gradle -> {
					val buildGradle = root["build.gradle.kts"] ?: root["build.gradle"]
					if (buildGradle != null) {
						GradleAutoImportAware()
						invokeLater {
							try {
								println("GradleProjectOpenProcessor().doOpenProject")
								//GradleProjectOpenProcessor().doOpenProject(root["build.gradle"]!!, project, false)
								println("canOpenGradleProject: " + canOpenGradleProject(buildGradle))
								//val projectFilePath = root.canonicalPath + "/build.gradle"
								val projectFilePath = root.canonicalPath + "/build.gradle.kts"
								println("projectFilePath: $projectFilePath")
                                linkAndRefreshGradleProject2(projectFilePath, project)
								//setupGradleSettings(org.jetbrains.plugins.gradle.settings.GradleProjectSettings(), ".", project)
								println("/GradleProjectOpenProcessor().doOpenProject")
							} catch (e: Throwable) {
								e.printStackTrace()
							}
                            try {
                                createGradleRunConfiguration(project, "runJvmAutoreload")
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
						}
					}
				}
			}
		}
	}

	private fun createAndGetRoot(): VirtualFile? {
		val path = contentEntryPath?.let { FileUtil.toSystemIndependentName(it) } ?: return null
		return LocalFileSystem.getInstance().refreshAndFindFileByPath(File(path).apply { mkdirs() }.absolutePath)
	}

	override fun getParentGroup() = KorgeModuleType.NAME

    //override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? = KorgeModuleWizardStep(korgeProjectTemplateProvider, config)

    // 1st screen
	override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable?) =
		//KorgeModuleWizardStep(korgeProjectTemplateProvider, config)
        NewKorgeModuleWizardStep(korgeProjectTemplateProvider, config)

	// 2nd+ screen(s)
	//override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider) = arrayOf(
	//	KorgeArtifactWizardStep(korgeProjectTemplateProvider, config)
	//)
}
