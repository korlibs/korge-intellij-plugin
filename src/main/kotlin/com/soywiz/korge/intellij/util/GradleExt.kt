package com.soywiz.korge.intellij.util

import com.intellij.execution.RunManager
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.io.systemIndependentPath
import org.jetbrains.kotlin.idea.refactoring.project
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction
import org.jetbrains.plugins.gradle.action.GradleRefreshProjectDependenciesAction
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.awt.Component


fun linkAndRefreshGradleProject2(projectFilePath: String, project: Project) {
    linkAndRefreshGradleProject(projectFilePath, project)
}

val Project.gradleExternalSettings get() = ExternalSystemApiUtil.getSettings(this, GradleConstants.SYSTEM_ID)

fun getActionFromId(str: String): AnAction = ActionManager.getInstance().getAction(str)

val Project.project: Project get() = this
val dataManager: DataManager get() = DataManager.getInstance()
fun AnAction.triggerEvent(
    dataContext: DataContext,
    presentation: Presentation? = null,
    place: String = "your.unique.event.id"
) = actionPerformed(
    AnActionEvent.createFromDataContext(place, presentation, dataContext)
)
fun Component.getDataContext() = DataManager.getInstance().getDataContext(this)
fun Component.getDataContext(x: Int, y: Int) = DataManager.getInstance().getDataContext(this, x, y)

fun Project.getDataContext(): DataContext? {
    return DataManager.getInstance().dataContextFromFocusAsync.blockingGet(100)
}

fun refreshGradleProject(dataContext: DataContext) {
    //PsiManager.getInstance(dataCo).context
    //getActionFromId("Gradle.RefreshDependencies")

    val project = dataContext.project
    GradleExecuteTaskAction.runGradle(project, null, project.rootFile!!.toNioPath().toFile().absolutePath, "")
    GradleRefreshProjectDependenciesAction().triggerEvent(dataContext)
    //val dataContext = DataManager.getInstance().getDataContext()
    //val event = AnActionEvent.createFromDataContext("your.unique.event.id", null, dataContext)

    //val projectRootFile = project.rootFile?.toNioPath()?.systemIndependentPath ?: error("Can't find project root: $project")
    //val externalSettings = project.gradleExternalSettings
    //val linkedProjectSettings = externalSettings.getLinkedProjectSettings(projectRootFile) ?: externalSettings.linkedProjectsSettings.firstOrNull()
    //val externalProjectPath = linkedProjectSettings?.externalProjectPath ?: projectRootFile
    //ExternalSystemUtil.refreshProject(
    //    externalProjectPath,
    //    ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
    //        .withArguments("--refresh-dependencies")
    //)


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
