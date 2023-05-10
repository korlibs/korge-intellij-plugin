package com.soywiz.korge.intellij.deps

import kotlin.test.Test
import kotlin.test.assertEquals

class DepsKProjectYmlTest {
    @Test
    fun testEmptyFile() {
        assertEquals(emptyList(), DepsKProjectYml.extractDeps(""))
    }

    @Test
    fun testEmptyDependencies() {
        assertEquals(emptyList(), DepsKProjectYml.extractDeps("dependencies:"))
    }

    @Test
    fun testInvalidFormat() {
        assertEquals(emptyList(), DepsKProjectYml.extractDeps("dependencies: true"))
        assertEquals(emptyList(), DepsKProjectYml.extractDeps("dependencies: 1"))
        assertEquals(emptyList(), DepsKProjectYml.extractDeps("dependencies: {}"))
    }

    @Test
    fun testSimple() {
        assertEquals(listOf("a", "b"), DepsKProjectYml.extractDeps("""
            dependencies:
            - a
            - b
        """.trimIndent()))
    }
}
