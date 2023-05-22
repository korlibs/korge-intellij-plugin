package com.soywiz.korge.intellij.util

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter

fun renderTemplate(template: String, info: Map<String, Any?>): String {
	val writer = StringWriter()
	val velocityEngine = VelocityEngine()
	//velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute::class.java.name)
	//velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute::class.java.name)
	velocityEngine.init()
	velocityEngine.evaluate(VelocityContext(info), writer, "", template)
	return writer.toString()
}
