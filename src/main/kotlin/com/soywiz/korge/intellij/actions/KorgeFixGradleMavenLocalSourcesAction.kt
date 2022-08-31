package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem

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

data class ParsedRoot(
    val jarUrl: String
)

data class ParsedLibrary(
    val name: ParsedLibraryName,
    val parsedClassRoots: List<ParsedRoot>,
    val parsedSourceRoots: List<ParsedRoot>,

)

class KorgeFixGradleMavenLocalSourcesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        println("Inside FixGradleMavenLocalSourcesAction")
        println(e)
        println(e.project)
        if (project == null) return

        val instance = LibraryTablesRegistrar.getInstance()

        val libraryTable = instance.getLibraryTable(e.project!!)

        val localFileSystem = LocalFileSystem.getInstance()

        println("libraryTable: $libraryTable")

        val parsedLibaries = mutableListOf<ParsedLibrary>()

        println("Iterating through all libraries:")
        libraryTable.libraries.forEach { library ->
            val libraryName = library.name ?: return@forEach
            if (!libraryName.startsWith("Gradle: ")) return@forEach

            println(library)
            println("it.name: ${libraryName}")
            println("it.presentableName: ${library.presentableName}")

            val gradleParsed = libraryName!!.removePrefix("Gradle: ").split(":")
            val parsedLibraryName = when (gradleParsed.size) {
                2 -> {
                        ParsedLibraryName.Double(
                            gradleParsed[0],
                            gradleParsed[1],
                        )
                }

                3 -> {
                        ParsedLibraryName.Triple(
                            gradleParsed[0],
                            gradleParsed[1],
                            gradleParsed[2],
                        )
                }

                4 -> {
                        ParsedLibraryName.Quad(
                            gradleParsed[0],
                            gradleParsed[1],
                            gradleParsed[2],
                            gradleParsed[3],
                        )
                }

                5 -> {
                        ParsedLibraryName.Penta(
                            gradleParsed[0],
                            gradleParsed[1],
                            gradleParsed[2],
                            gradleParsed[3],
                            gradleParsed[4],
                        )
                }
                else -> TODO()
            }

            val parsedClassRoots = library.getUrls(OrderRootType.CLASSES).map {
                ParsedRoot(it!!)
            }
            val parsedSourceRoots = library.getUrls(OrderRootType.SOURCES).map {
                ParsedRoot(it!!)
            }

            parsedLibaries.add(ParsedLibrary(
                parsedLibraryName,
                parsedClassRoots,
                parsedSourceRoots
            ))

            println(library.rootProvider.getUrls(OrderRootType.CLASSES).toList())
            println(library.rootProvider.getUrls(OrderRootType.SOURCES).toList())
            println(library.getFiles(OrderRootType.CLASSES).toList())
            val parsedClassPath =
                library.rootProvider.getUrls(OrderRootType.CLASSES)[0].removePrefix("jar://")
                    .removeSuffix("!/")
            println("parsedClassPath: $parsedClassPath")
            println(localFileSystem.findFileByPath(parsedClassPath))
            println()
        }

        println("Parsed libraries:")
        println(parsedLibaries.joinToString("\n"))


    }
}
