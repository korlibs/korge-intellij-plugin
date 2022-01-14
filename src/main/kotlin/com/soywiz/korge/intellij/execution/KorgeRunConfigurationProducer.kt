package com.soywiz.korge.intellij.execution

import com.intellij.execution.actions.*
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import org.jetbrains.plugins.gradle.execution.*
import org.jetbrains.plugins.gradle.service.execution.*
import org.jetbrains.plugins.gradle.service.project.*

// KotlinJUnitRunConfigurationProducer
//class KorgeRunConfigurationProducer : GradleTestRunConfigurationProducer<GradleRunConfiguration>() {
//class KorgeRunConfigurationProducer : RunConfigurationProducer<GradleRunConfiguration>() {
//class KorgeRunConfigurationProducer : KotlinJUnitRunConfigurationProducer<GradleRunConfiguration>() {
// class org.jetbrains.kotlin.idea.run.KotlinRunConfiguration cannot be cast to class org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration (org.jetbrains.kotlin.idea.run.KotlinRunConfiguration is in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @4bb6037e; org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration is in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @247eebb1)

// GradleTestRunConfigurationProducer
open class KorgeRunConfigurationProducer : LazyRunConfigurationProducer<GradleRunConfiguration>() {

    val taskName: String get() = "runJvm"

    override fun getConfigurationFactory(): ConfigurationFactory =
        object : ConfigurationFactory(GradleExternalTaskConfigurationType.getInstance()) {
            override fun getId(): String = "KorgeRunConfigurationProducerFactory"

            override fun createTemplateConfiguration(project: Project): RunConfiguration =
                GradleRunConfiguration(project, this, "MyKorgeRunConfiguration")
        }

    override fun isConfigurationFromContext(configuration: GradleRunConfiguration, context: ConfigurationContext): Boolean {
        val projectPath = context.getProjectPath()
        val realTaskName = context.getGradleFullTaskName()

        return (configuration.name == realTaskName) &&
                (configuration.settings.externalProjectPath == projectPath) &&
                (configuration.settings.taskNames == listOf(realTaskName))
    }

    fun ConfigurationContext.getGradleFullTaskName(): String {
        val gradlePath = GradleProjectResolverUtil.getGradlePath(module)
        return "${gradlePath?.trimEnd(':')}:$taskName"
    }
    fun ConfigurationContext.getProjectPath(): String? {
        return if (module != null) GradleRunnerUtil.resolveProjectPath(module) else null
    }

    override fun setupConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val project = context.project
        val module = context.module
        //println("KorgeRunConfigurationProducer.setupConfigurationFromContext")
        if (KorgeMainDetector.detect(sourceElement.get())) {
            val projectPath = context.getProjectPath()
            val realTaskName = context.getGradleFullTaskName()

            //TODO("Not yet implemented")
            configuration.name = realTaskName
            configuration.settings.also { settings ->
                //context.module.isGradleModule()
                //settings.executionName = "korge.$taskName"
                settings.externalProjectPath = projectPath
                settings.taskNames = listOf(realTaskName)

            }
            return true
        }
        //println("!!!sourceElement.get(): ${sourceElement.get()}")
        return false
    }
}
