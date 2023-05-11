package com.soywiz.korge.intellij.ai

import com.soywiz.korge.intellij.http.SimpleHttpClient
import com.soywiz.korge.intellij.http.bodyAsJson
import com.soywiz.korge.intellij.util.Json
import com.soywiz.korge.intellij.util.tempCreateTempFile
import java.io.File
import java.net.URL

class OpenAI(val apiKey: String) {
    companion object {
        val DEFAULT_TRANSCODE_MODEL = "whisper-1"
        val DEFAULT_COMPLETION_MODEL = "gpt-3.5-turbo"
    }

    object Role {
        val SYSTEM = "system"
        val USER = "user"
        val ASSISTANT = "assistant"
    }

    data class ChatEntry(val role: String, val content: String) {
        companion object {
            fun system(content: String): ChatEntry = ChatEntry(Role.SYSTEM, content)
            fun user(content: String): ChatEntry = ChatEntry(Role.USER, content)
            fun assistant(content: String): ChatEntry = ChatEntry(Role.ASSISTANT, content)
        }
    }

    class ChatStream(val openai: OpenAI, val temperature: Double, val model: String) {
        val entries: MutableList<ChatEntry> = arrayListOf()

        val lastEntry: ChatEntry? get() = entries.lastOrNull()
        fun filterEntries(filter: (ChatEntry) -> Boolean): ChatStream {
            val filteredEntries = entries.filter { filter(it) }
            entries.clear()
            entries.addAll(filteredEntries)
            return this
        }

        fun message(entry: ChatEntry): ChatStream { entries += entry; return this }
        fun system(content: String): ChatStream = message(ChatEntry.system(content))
        fun user(content: String): ChatStream = message(ChatEntry.user(content))
        fun assistant(content: String): ChatStream = message(ChatEntry.assistant(content))

        fun send(handler: (ChatEntry) -> Unit): ChatStream {
            handler(send())
            return this
        }

        fun send(): ChatEntry {
            return openai.chat(entries, temperature, model).also { entries += it }
        }
    }

    fun chatStream(temperature: Double = 1.0, model: String = OpenAI.DEFAULT_COMPLETION_MODEL): ChatStream = ChatStream(this, temperature, model)

    fun chat(entries: List<ChatEntry>, temperature: Double = 1.0, model: String = OpenAI.DEFAULT_COMPLETION_MODEL): ChatEntry {
        data class Choice(val message: ChatEntry)
        data class Result(val choices: List<Choice>)

        return SimpleHttpClient.postJson(
            URL("https://api.openai.com/v1/chat/completions"),
            Json.stringify(mapOf(
                "model" to model,
                "messages" to entries,
                "temperature" to temperature
            )),
            headers = mapOf("Authorization" to "Bearer $apiKey",)
        ).bodyAsJson<Result>().choices.first().message
    }

    fun transcode(
        audioData: ByteArray,
        language: String? = null,
        model: String = OpenAI.DEFAULT_TRANSCODE_MODEL
    ): String {
        tempCreateTempFile("mp3", audioData) { tempFile ->
            return transcode(tempFile, language, model)
        }
    }

    fun transcode(
        audioFile: File,
        language: String? = null,
        model: String = OpenAI.DEFAULT_TRANSCODE_MODEL
    ): String {
        data class Result(val text: String)
        val parts = mutableListOf(
            "model" to model,
            "file" to audioFile,
        )
        if (language != null) {
            parts += listOf(
                "language" to language,
            )
        }

        return SimpleHttpClient.postMultipart(
            URL("https://api.openai.com/v1/audio/transcriptions"),
            *parts.toTypedArray(),
            headers = mapOf("Authorization" to "Bearer $apiKey",)
        ).bodyAsJson<Result>().text
    }
}
