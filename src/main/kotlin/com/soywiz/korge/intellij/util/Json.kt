package com.soywiz.korge.intellij.util

import com.google.gson.Gson
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

object Json {
    val gson = Gson()
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T> parse(str: String): T {
        return gson.fromJson(str, typeOf<T>().javaType)
    }

    fun stringify(data: Any?): String {
        return gson.toJson(data)
    }
}
