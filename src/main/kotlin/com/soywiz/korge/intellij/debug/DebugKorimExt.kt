package com.soywiz.korge.intellij.debug

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.sun.jdi.*


fun Type.isKorimBitmapOrDrawable() = this.instanceOf<Bitmap>() || this.instanceOf<BmpSlice>() || this.instanceOf<Context2d.Drawable>()

fun ObjectReference.readKorimBitmap32(hintWidth: Int, hintHeight: Int, thread: ThreadReference? = null): Bitmap32 {
	val value = this
	val type = value.type()
	return when {
		type.instanceOf<Bitmap>() -> readKorimBitmap32Internal(thread)
		type.instanceOf<BmpSlice>() -> readKorimBmpSliceInternal(thread)
		type.instanceOf<Context2d.Drawable>() -> {
			val isSizedDrawable = type.instanceOf<Context2d.SizedDrawable>()
			val width = if (isSizedDrawable) value.invoke("getWidth", listOf(), thread = thread).int(hintWidth) else hintWidth
			val height = if (isSizedDrawable) value.invoke("getHeight", listOf(), thread = thread).int(hintHeight) else hintHeight
			readKorimDrawableInternal(width, height, thread)
		}
		else -> error("Can't interpret $this object as Bitmap or Context2d.Drawable")
	}
}

fun ObjectReference.readKorimBmpSliceInternal(thread: ThreadReference? = null): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<BmpSlice>()) error("Not a korim BmpSlice")
	val left = value.invoke("getLeft", thread = thread).int(0)
	val top = value.invoke("getTop", thread = thread).int(0)
	val width = value.invoke("getWidth", thread = thread).int(0)
	val height = value.invoke("getHeight", thread = thread).int(0)
	return (value.invoke("getBmp", listOf(), thread = thread) as ObjectReference).readKorimBitmap32Internal(thread).sliceWithSize(left, top, width, height).extract()
}

fun ObjectReference.readKorimBitmap32Internal(thread: ThreadReference? = null): Bitmap32 {
	val value = this
	if (!value.type().instanceOf<Bitmap>()) error("Not a korim Bitmap")
	val width = value.invoke("getWidth", listOf(), thread = thread).int(0)
	val height = value.invoke("getHeight", listOf(), thread = thread).int(0)
	val premultiplied = value.invoke("getPremultiplied", listOf(), thread = thread).bool(false)
	val bmp32Mirror = value.invoke("toBMP32", listOf(), thread = thread) as ObjectReference
	val dataInstance = (bmp32Mirror.invoke("getData", listOf(), thread = thread) as ObjectReference).debugToLocalInstanceViaSerialization(thread = thread) as IntArray
	return Bitmap32(width, height, RgbaArray(dataInstance.copyOf()), premultiplied)
}

fun ObjectReference.readKorimDrawableInternal(width: Int, height: Int, thread: ThreadReference? = null): Bitmap32 {
	val value = this
	val vm = virtualMachine()
	if (!value.type().instanceOf<Context2d.Drawable>()) error("Not a korim Context2d.Drawable")
	val clazz = virtualMachine().getRemoteClass("com.soywiz.korim.format.NativeImageFormatProviderJvmKt", thread = thread) ?: error("Can't find NativeImageFormatProviderJvmKt")
	val nativeImageFormatProvider = clazz.invoke("getNativeImageFormatProvider", listOf(), thread = thread) as? ObjectReference? ?: error("Error calling getNativeImageFormatProvider")
	val image = nativeImageFormatProvider.invoke("create", listOf(vm.mirrorOf(width), vm.mirrorOf(height)), thread = thread) as? ObjectReference? ?: error("Error calling create")
	val ctx2d = image.invoke("getContext2d", listOf(vm.mirrorOf(false)), thread = thread) as? ObjectReference? ?: error("Error calling getContext2d")
	ctx2d.invoke("draw", listOf(value), thread = thread)
	return image.readKorimBitmap32Internal(thread)
}