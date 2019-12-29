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

class KorimBitmapDebugNodeRenderer : com.intellij.debugger.ui.tree.render.NodeRendererImpl(NAME) {
	companion object {
		const val NAME = "KorimBitmapDebugNodeRenderer"
	}
	override fun isApplicable(type: Type?): Boolean = type.instanceOf<Bitmap>()
	override fun isEnabled(): Boolean = true
	override fun getUniqueId(): String = NAME

	override fun calcLabel(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): String {
		val value = descriptor.value
		val thread = evaluationContext.suspendContext.thread?.threadReference
		if (value is ObjectReference) {
			val width = value.invoke("getWidth", listOf(), thread = thread).int()
			val height = value.invoke("getHeight", listOf(), thread = thread).int()
			return "${width}x${height}"
		}
		return value.toString()
	}

	override fun hasOverhead(): Boolean = true

	override fun calcValueIcon(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): Icon? {
		try {
			val value = descriptor.value
			val thread = evaluationContext.suspendContext.thread?.threadReference
			val vm = value.virtualMachine()
			if (value is ObjectReference) {
				val bmp32 = value.readKorimBitmap32(thread)
				return ImageIcon(bmp32.toAwt().getScaledInstance(16, 16, Image.SCALE_SMOOTH))
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		return super.calcValueIcon(descriptor, evaluationContext, listener)
	}
}