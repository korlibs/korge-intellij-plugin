package com.soywiz.korge.intellij.debug

import com.sun.jdi.*
import kotlin.ByteArray
import javassist.util.proxy.*
import org.gradle.internal.id.UniqueId.*
import java.io.*
import java.lang.reflect.Method
import java.util.*

fun ArrayReference.convertToLocalBytes(): kotlin.ByteArray {
	val base64Class = this.virtualMachine().getRemoteClass(Base64::class.java) ?: error("Can't find Base64 class")
	val encoder = base64Class.invoke("getEncoder", listOf()) as ObjectReference
	val str = encoder.invoke("encodeToString", listOf(this)) as StringReference
	return Base64.getDecoder().decode(str.value())
}

fun ObjectReference.debugToLocalInstanceViaSerialization(): Any? {
	return this.debugSerialize().debugDeserialize()
}

fun ObjectReference.debugSerialize(): ByteArray {
	val vm = this.virtualMachine()

	val baosClass = vm.getRemoteClass(ByteArrayOutputStream::class.java) ?: error("Cant't find ByteArrayOutputStream")
	val baosClassConstructor = baosClass.methods().firstOrNull { it.isConstructor && it.arguments().size == 0 } ?: error("Can't find ByteArrayOutputStream constructor")
	val baos = baosClass.newInstance(vm.anyThread(), baosClassConstructor, listOf(), ClassType.INVOKE_SINGLE_THREADED)

	val oosClass = vm.getRemoteClass(ObjectOutputStream::class.java) ?: error("Can't find ObjectOutputStream")
	val oosClassConstructor = oosClass.methods().firstOrNull { it.isConstructor && it.arguments().size == 1 } ?: error("Can't find ObjectOutputStream constructor")
	val oos = oosClass.newInstance(vm.anyThread(), oosClassConstructor, listOf(baos), ClassType.INVOKE_SINGLE_THREADED)

	oos.invoke("writeObject", listOf(this))

	return (baos.invoke("toByteArray", listOf()) as ArrayReference).convertToLocalBytes()
}

fun ByteArray.debugDeserialize(): Any? = ObjectInputStream(this.inputStream()).readObject()

fun VirtualMachine.getRemoteClass(clazz: Class<*>): ClassType? {
	val clazzType = classesByName("java.lang.Class").firstOrNull() ?: error("Can't find java.lang.Class")
	val clazzClassType = (clazzType as? ClassType?) ?: error("Invalid java.lang.Class")
	val realClazz = clazzClassType.invoke("forName", listOf(mirrorOf(clazz.name))) as ClassObjectReference
	return realClazz.reflectedType() as ClassType?
}

fun VirtualMachine.anyThread() = allThreads().first()

fun ClassType.invoke(methodName: String, args: List<Value>, signature: String? = null, thread: ThreadReference? = null): Value {
	val method = if (signature != null) this.methodsByName(methodName, signature).first() else this.methodsByName(methodName).first()
	return this.invokeMethod(thread ?: this.virtualMachine().anyThread(), method, args, ClassType.INVOKE_SINGLE_THREADED)
}

fun ObjectReference.invoke(methodName: String, args: List<Value>, signature: String? = null, thread: ThreadReference? = null): Value {
	val method = if (signature != null) this.referenceType().methodsByName(methodName, signature).first() else this.referenceType().methodsByName(methodName).first()
	return this.invokeMethod(thread ?: this.virtualMachine().anyThread(), method, args, ClassType.INVOKE_SINGLE_THREADED)
}

fun ClassType.getField(fieldName: String): Value = this.getValue(this.fieldByName(fieldName))
fun ObjectReference.getField(fieldName: String): Value = this.getValue(this.referenceType().fieldByName(fieldName))

fun Value.int(): Int? = if (this is PrimitiveValue) this.intValue() else null
fun Value.int(default: Int): Int = if (this is PrimitiveValue) this.intValue() else default

fun Value.bool(): Boolean? = if (this is PrimitiveValue) this.booleanValue() else null
fun Value.bool(default: Boolean): Boolean = if (this is PrimitiveValue) this.booleanValue() else default

inline fun <reified T> Value.asLocalType() = asLocalType(T::class.java)

fun <T> Value.asLocalType(clazz: Class<T>): T {
	return ProxyFactory().also {
		it.superclass = clazz
	}.create(emptyArray(), arrayOf()) { self, thisMethod, proceed, args ->
		println("CALLING $args")
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	} as T
}
