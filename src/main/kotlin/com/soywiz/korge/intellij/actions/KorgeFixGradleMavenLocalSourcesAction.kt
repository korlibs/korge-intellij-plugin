package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
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
        val project = e.project
        println("Inside FixGradleMavenLocalSourcesAction")
        println(e)
        println(e.project)
        if (project == null) return

        val instance = LibraryTablesRegistrar.getInstance()

        val libraryTable = instance.getLibraryTable(e.project!!)

        val localFileSystem = LocalFileSystem.getInstance()

        println("libraryTable: $libraryTable")

        val parsedLibraries = mutableListOf<ParsedLibrary>()

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
                ParsedClassRoot(it!!)
            }
            val parsedSourceRoots = library.getUrls(OrderRootType.SOURCES).map {
                ParsedSourceRoot(it!!)
            }

            parsedLibraries.add(ParsedLibrary(
                parsedLibraryName,
                parsedClassRoots,
                parsedSourceRoots,
                library
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

//        println("Parsed libraries:")
//        println(parsedLibaries.joinToString("\n"))
//        println()

        println("Korge libraries")
        println(parsedLibraries.filter {
            it.name.isKorgeLibrary()
        }.joinToString("\n"))
        println()

        val korgeLibraries = parsedLibraries.filter { it.name.isKorgeLibrary() }


        val korgeJvmLibraries = korgeLibraries.filter {
            it.name.isKorgeJvmLibrary()
        }

        println("Korge JVM libraries")
        println(korgeJvmLibraries.joinToString("\n"))

        val groupIdToJvmLibrary = korgeJvmLibraries.map {
            it.name.groupId to it
        }.toMap()


        println("Adding JVM source to applicable Korge libraries.")
        runWriteAction {
            korgeLibraries.asSequence().forEach {
                // Already has sources, skip.
                if (it.parsedSourceRoots.isNotEmpty()) return@forEach
                // No JVM library found, skip.
                val jvmLibrary = groupIdToJvmLibrary[it.name.groupId] ?: return@forEach
                // No JVM root found in library, skip.
                val root = jvmLibrary.parsedClassRoots.firstOrNull() ?: return@forEach
                // No JVM source file found for library, skip.
                root.predictedSourceFile ?: return@forEach

                println("Adding JVM source file for:")
                println(it)
                it.library.modifiableModel.apply {
                    addRoot(root.predictedSourceJarFilePath, OrderRootType.SOURCES)
                    commit()
                }
            }
        }



    }
}
