package com.soywiz.korge.intellij.util

enum class KorgeNotificationTypes(
    val groupId: String,
    val defaultTitle: String
) {
    MAVEN_LOCAL_SOURCE_FIXER(
        "Korge Plugin: Maven Local Source Fixer",
        "Korge Plugin: Maven Local Source Fixer"
    )
}
