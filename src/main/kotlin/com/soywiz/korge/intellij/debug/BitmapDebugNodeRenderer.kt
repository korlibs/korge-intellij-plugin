package com.soywiz.korge.intellij.debug

import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.ui.tree.*
import com.intellij.debugger.ui.tree.render.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.sun.jdi.*
import com.sun.tools.jdi.*
import javassist.util.proxy.*
import java.awt.*
import javax.swing.*
import kotlin.jvm.internal.*

class BitmapDebugNodeRenderer : com.intellij.debugger.ui.tree.render.NodeRendererImpl("BitmapDebugNodeRenderer") {
	override fun isApplicable(type: Type?): Boolean {
		if (type == null) return false
		if (type is ClassType) {
			//println("TYPE: ${type.signature()} ${type.name()} :: ${Bitmap::class.java.name}")
			if (type.name() == Bitmap::class.java.name) return true
			return isApplicable(type.superclass())
		}
		return false
	}

	override fun isEnabled(): Boolean {
		return true
	}

	override fun getUniqueId(): String {
		return "BitmapDebugNodeRenderer"
	}

	override fun calcLabel(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): String {
		val value = descriptor.value
		if (value is ObjectReference) {
			val width = value.invoke("getWidth", listOf()).int()
			val height = value.invoke("getHeight", listOf()).int()
			return "${width}x${height}"
		}
		return value.toString()
	}

	override fun hasOverhead(): Boolean = true

	override fun calcValueIcon(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): Icon? {
		try {
			val value = descriptor.value
			val vm = value.virtualMachine()
			if (value is ObjectReference) {
				val width = value.invoke("getWidth", listOf()).int(0)
				val height = value.invoke("getHeight", listOf()).int(0)
				val premultiplied = value.invoke("getPremultiplied", listOf()).bool(false)
				val bmp32Mirror = value.invoke("toBMP32", listOf()) as ObjectReference
				val dataInstance = (bmp32Mirror.invoke("getData", listOf()) as ObjectReference).debugToLocalInstanceViaSerialization() as IntArray
				val bmp32 = Bitmap32(width, height, RgbaArray(dataInstance), premultiplied)
				return ImageIcon(bmp32.toAwt().getScaledInstance(16, 16, Image.SCALE_SMOOTH))
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		return super.calcValueIcon(descriptor, evaluationContext, listener)
	}
}