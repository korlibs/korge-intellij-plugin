package com.soywiz.korge.intellij.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.atomic.AtomicBoolean

enum class ArtifactType {
    UNKNOWN,
    JVM,
    JS,
    MINGWX64,
    COMMON
}

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

    val artifactType: ArtifactType
        get() = when {
            artifactId.endsWith("-jvm") -> ArtifactType.JVM
            artifactId.endsWith("-js") -> ArtifactType.JS
            artifactId.endsWith("-mingwx64") -> ArtifactType.MINGWX64
            this is Quad && name == "commonMain" -> ArtifactType.COMMON
            else -> ArtifactType.UNKNOWN
        }
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
    val splitJarFilePath: List<String> = jarFilePath.split("/"),
    // E.g: C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999-sources.jar
    val predictedJvmSourceFilePath: String = jarFilePath.removeSuffix(".jar") + "-sources.jar",
    val predictedJsSourceFilePath: String = jarFilePath.removeSuffix(".klib") + "-sources.jar",
    val predictedCommonMainSourceFilePath: String = splitJarFilePath.toMutableList().run {
        // klock-jvm-2.0.0.999.jar -> klock-2.0.0.999-sources.jar
        this[lastIndex] =
            this[lastIndex].replace("-jvm-", "-").removeSuffix(".jar") + "-sources.jar"
        // klock-jvm -> klock
        this[lastIndex - 2] = this[lastIndex - 2].removeSuffix("-jvm")

        // Should result in the common source path:
        // C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock/2.0.0.999/klock-2.0.0.999-sources.jar
        this.joinToString("/")
    },
    val predictedMingwx64MainSourceFilePath: String = splitJarFilePath.toMutableList().run {
        // klock-jvm-2.0.0.999.jar -> klock-mingwx64-2.0.0.999-sources.jar
        this[lastIndex] =
            this[lastIndex].replace("-jvm-", "-mingwx64-").removeSuffix(".jar") + "-sources.jar"
        // klock-jvm -> klock-mingwx64
        this[lastIndex - 2] = this[lastIndex - 2].removeSuffix("-jvm") + "-mingwx64"

        // Should result in the common source path:
        // C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-mingwx64/2.0.0.999/klock-mingwx64-2.0.0.999-sources.jar
        this.joinToString("/")
    }
) {
    val predictedJvmSourceFile: VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(predictedJvmSourceFilePath)

    // E.g: jar://C:/Users/kietm/.m2/repository/com/soywiz/korlibs/klock/klock-jvm/2.0.0.999/klock-jvm-2.0.0.999-sources.jar!/
    val predictedJvmSourceJarFilePath: String = "jar://$predictedJvmSourceFilePath!/"

    val predictedJsSourceFile: VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(predictedJsSourceFilePath)
    val predictedJsSourceJarFilePath: String = "jar://$predictedJsSourceFilePath!/"

    val predictedCommonSourceFile: VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(predictedCommonMainSourceFilePath)
    val predictedCommonSourceJarFilePath: String = "jar://$predictedCommonMainSourceFilePath!/"

    val predictedMingwx64SourceFile: VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(predictedMingwx64MainSourceFilePath)
    val predictedMingwx64SourceJarFilePath: String = "jar://$predictedMingwx64MainSourceFilePath!/"
}

data class ParsedSourceRoot(
    val jarUrl: String,
)

data class ParsedLibrary(
    val name: ParsedLibraryName,
    val parsedClassRoots: List<ParsedClassRoot>,
    val parsedSourceRoots: List<ParsedSourceRoot>,
    val library: Library
)

object LibraryFixer {
    private var updateInProgress = AtomicBoolean(false)
    fun fixLibraries(project: Project?, isManualTrigger: Boolean) {
        if (updateInProgress.get()) {
            println("Update currently in progress... Quick returning.")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            updateInProgress.set(true)
            try {
                fixLibrariesInternal(project, isManualTrigger)
            } finally {
                updateInProgress.set(false)
            }
        }
    }

    private fun fixLibrariesInternal(project: Project?, isManualTrigger: Boolean) {
        if (project == null) return
        println("Fixing libraries for project: $project")

        val instance = LibraryTablesRegistrar.getInstance()

        val libraryTable = instance.getLibraryTable(project)

        val parsedLibraries = mutableListOf<ParsedLibrary>()

        val gradleLibraries = libraryTable.libraries.filter { library ->
            val libraryName = library.name ?: return@filter false
            if (!libraryName.startsWith("Gradle: ")) return@filter false
            true
        }

        gradleLibraries.forEach { library ->
            val libraryName = library.name!!
//        println("Parsing library: $libraryName")
//        println(library)
//        println()
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

//        println("Korge libraries")
//        println(korgeLibraries.joinToString("\n"))

        val korgeJvmLibraries = korgeLibraries.filter {
            it.name.isKorgeJvmLibrary()
        }

//    println("Korge JVM libraries")
//    println(korgeJvmLibraries.joinToString("\n"))

        val groupIdToJvmLibrary = korgeJvmLibraries.associateBy {
            it.name.groupId
        }

        println("Adding JVM source to applicable Korge libraries.")
        val updatesPerArtifactType = mutableMapOf<ArtifactType, Int>()
        runWriteAction {
            korgeLibraries.asSequence().forEach {
                // Already has sources, skip.
                if (it.parsedSourceRoots.isNotEmpty()) return@forEach

                var wasUpdated = false
                when (it.name.artifactType) {
                    ArtifactType.UNKNOWN -> Unit
                    ArtifactType.JVM -> {
                        // No JVM library found, skip.
                        val jvmLibrary = groupIdToJvmLibrary[it.name.groupId] ?: return@forEach
                        // No JVM root found in library, skip.
                        val root = jvmLibrary.parsedClassRoots.firstOrNull() ?: return@forEach
                        // No JVM source file found for library, skip.
                        root.predictedJvmSourceFile ?: return@forEach

                        println("Adding JVM source file for: ${it.name}")
                        it.library.modifiableModel.apply {
                            addRoot(root.predictedJvmSourceJarFilePath, OrderRootType.SOURCES)
                            commit()
                        }
                        wasUpdated = true
                        updatesPerArtifactType[ArtifactType.JVM]
                    }

                    ArtifactType.JS -> {
                        val jsRoot = it.parsedClassRoots.firstOrNull() ?: return@forEach
                        jsRoot.predictedJsSourceFile ?: return@forEach

                        println("Adding JS source file for: ${it.name}")
                        it.library.modifiableModel.apply {
                            addRoot(jsRoot.predictedJsSourceJarFilePath, OrderRootType.SOURCES)
                            commit()
                        }
                        wasUpdated = true
                    }
                    // IMPORTANT. IMPORTANT. IMPORTANT:
                    // For MINGWX64 and COMMON libraries, for some reason, their roots are not based on
                    // the .m2 folder.
                    //
                    // For example:
                    // COMMON = C:/Users/kietm/GitHub/KorgeboardDDR/.gradle/kotlin/sourceSetMetadata/Common.KorgeboardDDR/commonMain/implementation/com.soywiz.korlibs.korgw-korgw/com.soywiz.korlibs.korgw-korgw-commonMain.klib
                    // MINGWX64 = C:/Users/kietm/.gradle/caches/modules-2/files-2.1/com.soywiz.korlibs.korgw/korgw-mingwx64/2.0.0.999/f6bb43d8591d7d038bb89a0ecd3bb49b270c1728/korgw.klib
                    //
                    // Because of this, we lookup the corresponding JVM library, and resolve the .m2
                    // source jar paths from that.
                    ArtifactType.MINGWX64 -> {
                        // No JVM library found, skip.
                        val jvmLibrary = groupIdToJvmLibrary[it.name.groupId] ?: return@forEach
                        // No JVM root found in library, skip.
                        val root = jvmLibrary.parsedClassRoots.firstOrNull() ?: return@forEach
                        // No MINGWX64 source file found for library, skip.
                        root.predictedMingwx64SourceFile ?: return@forEach

                        println("Adding MINGWX64 source file for: ${it.name}")
                        it.library.modifiableModel.apply {
                            addRoot(root.predictedMingwx64SourceJarFilePath, OrderRootType.SOURCES)
                            commit()
                        }
                        wasUpdated = true
                    }

                    ArtifactType.COMMON -> {
                        // No JVM library found, skip.
                        val jvmLibrary = groupIdToJvmLibrary[it.name.groupId] ?: return@forEach
                        // No JVM root found in library, skip.
                        val root = jvmLibrary.parsedClassRoots.firstOrNull() ?: return@forEach
                        // No COMMON source file found for library, skip.
                        root.predictedCommonSourceFile ?: return@forEach

                        println("Adding COMMON source file for: ${it.name}")
                        it.library.modifiableModel.apply {
                            addRoot(root.predictedCommonSourceJarFilePath, OrderRootType.SOURCES)
                            commit()
                        }
                        wasUpdated = true
                    }
                }

                if (wasUpdated) {
                    updatesPerArtifactType[it.name.artifactType] = updatesPerArtifactType.getOrDefault(it.name.artifactType, 0) + 1
                }

            }
        }
        val totalUpdated = updatesPerArtifactType.values.sum()
        if (totalUpdated == 0 && isManualTrigger) {
            // Only run this for manual trigger, else, it's very noisy.
            Notifications.Bus.notify(
                Notification(
                    KorgeNotificationTypes.MAVEN_LOCAL_SOURCE_FIXER.groupId,
                    KorgeNotificationTypes.MAVEN_LOCAL_SOURCE_FIXER.defaultTitle,
                    "No libraries updated.",
                    NotificationType.INFORMATION
                )
            )
        } else if (totalUpdated > 0) {
            Notifications.Bus.notify(
                Notification(
                    KorgeNotificationTypes.MAVEN_LOCAL_SOURCE_FIXER.groupId,
                    KorgeNotificationTypes.MAVEN_LOCAL_SOURCE_FIXER.defaultTitle,
                    """
                        Updated Korge libraries with maven local sources.
                        jvm: ${updatesPerArtifactType.getOrDefault(ArtifactType.JVM, 0)},
                        js: ${updatesPerArtifactType.getOrDefault(ArtifactType.JS, 0)},
                        mingwx64: ${updatesPerArtifactType.getOrDefault(ArtifactType.MINGWX64, 0)},
                        common: ${updatesPerArtifactType.getOrDefault(ArtifactType.COMMON, 0)}
                    """.trimIndent(),
                    NotificationType.INFORMATION
                )
            )
        }

    }
}


