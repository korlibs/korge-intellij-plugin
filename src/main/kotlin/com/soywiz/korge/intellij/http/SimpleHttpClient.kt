package com.soywiz.korge.intellij.http

import com.soywiz.korge.intellij.util.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object SimpleHttpClient {
    private fun ByteArrayOutputStream.write(str: String) {
        write(str.toByteArray())
    }

    fun postJson(
        url: URL,
        body: String,
        headers: Map<String, String> = mapOf(),
    ): HttpResponse<ByteArray> {
        return HttpClient.newBuilder().build().send(HttpRequest.newBuilder()
            .uri(url.toURI())
            .also { for ((k, v) in headers) it.header(k, v) }
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    fun postMultipart(
        url: URL,
        vararg parts: Pair<String, Any>,
        headers: Map<String, String> = mapOf(),
    ): HttpResponse<ByteArray> {
        val boundary = "----Boundary" + System.currentTimeMillis()
        return HttpClient.newBuilder().build().send(HttpRequest.newBuilder()
            .uri(url.toURI())
            .also { for ((k, v) in headers) it.header(k, v) }
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(createFormData(boundary, *parts)))
            .build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    fun createFormData(boundary: String, vararg parts: Pair<String, Any>): ByteArray {
        val formData = ByteArrayOutputStream()
        //val formData = StringBuilder()

        for ((key, value) in parts) {
            formData.write("--")
            formData.write(boundary)
            formData.write("\r\n")
            when (value) {
                is File -> {
                    formData.write("Content-Disposition: form-data; name=\"${key}\"; filename=\"${value.name}\"\r\n")
                    //val contentType: String = Files.probeContentType(value.toPath()) ?: "application/octet-stream"
                    val contentType: String = "application/octet-stream"
                    formData.write("Content-Type: $contentType\r\n")
                    formData.write("\r\n")
                    //formData.append(Base64.getEncoder().encodeToString(value.readBytes()))
                    formData.write(value.readBytes())
                }

                is String -> {
                    formData.write("Content-Disposition: form-data; name=\"$key\"\r\n")
                    formData.write("\r\n")
                    formData.write(value)
                }

                else -> TODO("Unsupported ${value::class}")
            }
            formData.write("\r\n")
        }

        formData.write("--$boundary--\r\n")

        //println(formData.toByteArray().toString(Charsets.UTF_8))
        //TODO()
        return formData.toByteArray()
    }
}

inline fun <reified T> HttpResponse<ByteArray>.bodyAsJson(): T {
    return Json.parse<T>(this.body().toString(Charsets.UTF_8))
}
