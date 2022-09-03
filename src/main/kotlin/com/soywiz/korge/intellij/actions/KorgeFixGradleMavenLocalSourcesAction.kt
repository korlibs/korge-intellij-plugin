package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korge.intellij.util.fixLibraries
import com.soywiz.korge.intellij.util.runWriteAction

enum class ArtifactType {
    UNKNOWN,
    JVM
}

sealed class ParsedLibraryName(

) {
    abstract val groupId: String
    abstract val artifactId: String

    data class Double(
        override val groupId: String,
        override val artifactId: String,
    ) : ParsedLibraryName()

    data class Triple(
        override val groupId: String,
        override val artifactId: String,
        val version: String
    ) : ParsedLibraryName()

    data class Quad(
        override val groupId: String,
        override val artifactId: String,
        val name: String,
        val version: String
    ) : ParsedLibraryName()

    data class Penta(
        override val groupId: String,
        override val artifactId: String,
        val name1: String,
        val name2: String,
        val version: String
    ) : ParsedLibraryName()

    val artifactType get() = if (artifactId.endsWith("-jvm")) ArtifactType.JVM else ArtifactType.UNKNOWN
}

fun ParsedLibraryName.isKorgeLibrary(): Boolean {
    return groupId.startsWith("com.soywiz.korlibs.")
}

fun ParsedLibraryName.isKorgeJvmLibrary(): Boolean {
    return artifactId.endsWith("-jvm")
}

data class ParsedClassRoot(
    // E.g: jar://C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999.jar!/
    val jarUrl: String,
    // E.g: C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999.jar
    val jarFilePath: String = jarUrl.removePrefix("jar://").removeSuffix("!/"),
    // E.g: C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999-sources.jar
    val predictedSourceFilePath: String = jarFilePath.removeSuffix(".jar") + "-sources.jar",
    val predictedSourceFile: VirtualFile? = LocalFileSystem.getInstance().findFileByPath(predictedSourceFilePath),
    // E.g: jar://C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999-sources.jar!/
    val predictedSourceJarFilePath: String = "jar://$predictedSourceFilePath!/"
)

data class ParsedSourceRoot(
    val jarUrl: String,
)

data class ParsedLibrary(
    val name: ParsedLibraryName,
    val parsedClassRoots: List<ParsedClassRoot>,
    val parsedSourceRoots: List<ParsedSourceRoot>,
    val library: Library
)

class KorgeFixGradleMavenLocalSourcesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("Running FixGradleMavenLocalSourcesAction")
        fixLibraries(e.project)
    }
}
