package com.soywiz.korge.intellij.execution

import com.intellij.execution.*
import com.intellij.execution.configuration.*
import com.intellij.openapi.project.*

val Project.runManager get() = RunManager.getInstance(this)
val Project.runConfigurationProducerService get() = RunConfigurationProducerService.getInstance(this)
val Project.executionManager get() = ExecutionManager.getInstance(this)
val Project.executionTargetManager get() = ExecutionTargetManager.getInstance(this)
//val Project.runConfigurationExtensionsManager get() = RunConfigurationExtensionsManager.

private fun test() {
    //RunConfigurationExtensionsManager.
}