package com.soywiz.korge.intellij.util

import com.intellij.execution.RunManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.io.systemIndependentPath
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.util.GradleConstants


fun linkAndRefreshGradleProject2(projectFilePath: String, project: Project) {
    linkAndRefreshGradleProject(projectFilePath, project)
}

val Project.gradleExternalSettings get() = ExternalSystemApiUtil.getSettings(this, GradleConstants.SYSTEM_ID)

fun refreshGradleProject(project: Project) {
    val projectRootFile = project.rootFile?.toNioPath()?.systemIndependentPath ?: error("Can't find project root: $project")
    val externalSettings = project.gradleExternalSettings
    val linkedProjectSettings = externalSettings.getLinkedProjectSettings(projectRootFile) ?: externalSettings.linkedProjectsSettings.firstOrNull()
    val externalProjectPath = linkedProjectSettings?.externalProjectPath ?: projectRootFile
    ExternalSystemUtil.refreshProject(
        externalProjectPath,
        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .withArguments("--refresh-dependencies")
    )


    /*
    GradleRefreshProjectDependenciesAction()
        .perform(project, GradleConstants.SYSTEM_ID, KotlinGradleProjectData.KEY)

    GradleRefreshProjectDependenciesAction()
        .actionPerformed(AnActionEvent.createFromDataContext(
            "Gradle.RefreshProject",
            null,
            DataManager.getInstance().getDataContext(WindowManager.getInstance().getFrame(project)!!.rootPane)
            //DataContext.EMPTY_CONTEXT
        ))

     */
    /*
    try {
        // Reload all Gradle projects
        ExternalSystemUtil.refreshProject(
            project, GradleConstants.SYSTEM_ID, project.basePath, true,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC
        )
    } finally {
        // Disconnect the project connection
        //connection.close()
    }
     */
}

fun createGradleRunConfiguration(project: Project, taskName: String) {
    /*
    val runManager = RunManager.getInstance(project)
    val configuration: RunnerAndConfigurationSettings = runManager.createConfiguration("runJvmAutoreload", GradleExternalTaskConfigurationType::class.java)
    configuration.factory
    runManager.addConfiguration(configuration)

     */

    val runManager = RunManager.getInstance(project)
    val factory = GradleExternalTaskConfigurationType.getInstance().configurationFactories[0]
    val runConfiguration = factory.createConfiguration(taskName, GradleRunConfiguration(project, factory, taskName).also {
        it.settings.taskNames = listOf(taskName)
    })
    val runnerAndConfigurationSettings = runManager.createConfiguration(runConfiguration, factory)

    runManager.addConfiguration(runnerAndConfigurationSettings)
    runManager.selectedConfiguration = runnerAndConfigurationSettings
}
