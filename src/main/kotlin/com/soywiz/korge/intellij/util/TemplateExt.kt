package com.soywiz.korge.intellij.util

import org.apache.velocity.*
import org.apache.velocity.app.*
import java.io.*

fun renderTemplate(template: String, info: Map<String, Any?>): String {
	val writer = StringWriter()
	val velocityEngine = VelocityEngine()
	velocityEngine.init()
	velocityEngine.evaluate(VelocityContext(info), writer, "", template)
	return writer.toString()
}
