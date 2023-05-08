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

    fun post(
        url: URL,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = mapOf(),
    ): HttpResponse<ByteArray> {
        val response = HttpClient.newBuilder().build().send(HttpRequest.newBuilder()
            .uri(url.toURI())
            .also { for ((k, v) in headers) it.header(k, v) }
            .POST(body)
            .build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() >= 400) {
            error("Error calling $url : ${response.statusCode()} : ${response.body().toString(Charsets.UTF_8)}")
        }
        return response
    }

    fun postJson(
        url: URL,
        body: String,
        headers: Map<String, String> = mapOf(),
    ): HttpResponse<ByteArray> =
        post(url, HttpRequest.BodyPublishers.ofString(body), headers + mapOf("Content-Type" to "application/json"))

    fun postMultipart(
        url: URL,
        vararg parts: Pair<String, Any>,
        headers: Map<String, String> = mapOf(),
    ): HttpResponse<ByteArray> {
        val boundary = "----Boundary" + System.currentTimeMillis()
        return post(
            url,
            HttpRequest.BodyPublishers.ofByteArray(createFormData(boundary, *parts)),
            headers + mapOf("Content-Type" to "multipart/form-data; boundary=$boundary")
        )
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
