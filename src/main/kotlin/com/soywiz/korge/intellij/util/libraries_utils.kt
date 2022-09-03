package com.soywiz.korge.intellij.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

sealed class ParsedLibraryName {
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

fun fixLibraries(
    project: Project?,
): Unit {
    if (project == null) return
    println(project)

    val instance = LibraryTablesRegistrar.getInstance()

    val libraryTable = instance.getLibraryTable(project)

    val parsedLibraries = mutableListOf<ParsedLibrary>()

    println("Iterating through all libraries:")
    val gradleLibraries = libraryTable.libraries.filter { library ->
        val libraryName = library.name ?: return@filter false
        if (!libraryName.startsWith("Gradle: ")) return@filter false
        true
    }

    gradleLibraries.forEach { library ->
        val libraryName = library.name!!
        println("Parsing library: $libraryName")
        val gradleParsed = libraryName.removePrefix("Gradle: ").split(":")
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

            else -> {
                println("Unsupported name: $gradleParsed")
                return@forEach
            }
        }

        val parsedClassRoots = library.getUrls(OrderRootType.CLASSES).map {
            ParsedClassRoot(it!!)
        }
        val parsedSourceRoots = library.getUrls(OrderRootType.SOURCES).map {
            ParsedSourceRoot(it!!)
        }

        parsedLibraries.add(
            ParsedLibrary(
                parsedLibraryName,
                parsedClassRoots,
                parsedSourceRoots,
                library
            )
        )

        //            println(library.rootProvider.getUrls(OrderRootType.CLASSES).toList())
        //            println(library.rootProvider.getUrls(OrderRootType.SOURCES).toList())
        //            println(library.getFiles(OrderRootType.CLASSES).toList())
        //            println()
    }

    //        println("Parsed libraries:")
    //        println(parsedLibaries.joinToString("\n"))
    //        println()

    val korgeLibraries = parsedLibraries.filter { it.name.isKorgeLibrary() }

    val korgeJvmLibraries = korgeLibraries.filter {
        it.name.isKorgeJvmLibrary()
    }

//    println("Korge JVM libraries")
//    println(korgeJvmLibraries.joinToString("\n"))

    val groupIdToJvmLibrary = korgeJvmLibraries.associateBy {
        it.name.groupId
    }

    println("Adding JVM source to applicable Korge libraries.")
    var numUpdated = 0
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

            println("Adding JVM source file for: ${it.name}")
            it.library.modifiableModel.apply {
                addRoot(root.predictedSourceJarFilePath, OrderRootType.SOURCES)
                commit()
            }
            numUpdated++
        }
    }
    println("$numUpdated libraries updated. Done.")
}
