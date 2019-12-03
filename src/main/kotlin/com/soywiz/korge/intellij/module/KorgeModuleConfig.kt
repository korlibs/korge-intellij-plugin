package com.soywiz.korge.intellij.module

import com.soywiz.korge.intellij.*
import com.soywiz.korio.util.*

class KorgeModuleConfig {
	var artifactGroup = "com.example"
	var artifactId = "example"
	var artifactVersion = "0.0.1"
	var projectType = ProjectType.Gradle
	var featuresToInstall = listOf(Features.core)
	var korgeVersion = Versions.LAST

	fun generate(): Map<String, ByteArray> = LinkedHashMap<String, ByteArray>().apply {
		fun putTextFile(name: String, file: Indenter.() -> Unit) {
			put(name, Indenter().also { file(it) }.toString().toByteArray(Charsets.UTF_8))
		}

		val features = featuresToInstall.toSet()
		putTextFile("build.gradle") {
			line("buildscript") {
				line("repositories") {
					+"mavenLocal()"
					+"""maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }"""
					+"""maven { url = uri("https://plugins.gradle.org/m2/") }"""
					+"""mavenCentral()"""
				}
				line("dependencies") {
					+"""classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:$korgeVersion")"""
				}
			}

			+"""apply plugin: "korge""""

			line("korge") {
				+"""id = "$artifactGroup.$artifactId""""
				if (features.contains(Features.f3d)) {
					+"supportExperimental3d()"
				}
				if (features.contains(Features.box2d)) {
					+"supportExperimental3d()"
				}
				if (features.contains(Features.dragonbones)) {
					+"supportDragonbones()"
				}
				if (features.contains(Features.swf)) {
					+"supportSwf()"
				}
			}

		}

		putTextFile("settings.gradle") {
			+"""enableFeaturePreview("GRADLE_METADATA")"""
		}

		putTextFile("gradle.properties") {
			+"org.gradle.jvmargs=-Xmx1536m"
		}

		fun getFileFromGenerator(path: String) = KorgeResources.getBytes("/com/soywiz/korge/intellij/generator/$path")

		put("gradlew", getFileFromGenerator("gradlew"))
		put("gradlew_linux", getFileFromGenerator("gradlew_linux"))
		put("gradlew_win", getFileFromGenerator("gradlew_win"))
		put("gradlew_wine", getFileFromGenerator("gradlew_wine"))
		put("gradlew.bat", getFileFromGenerator("gradlew.bat"))
		put("gradle/wrapper/gradle-wrapper.jar", getFileFromGenerator("gradle/wrapper/gradle-wrapper.jar"))
		put("gradle/wrapper/gradle-wrapper.properties", getFileFromGenerator("gradle/wrapper/gradle-wrapper.properties"))

		putTextFile("src/commonMain/kotlin/main.kt") {
			+"import com.soywiz.klock.seconds"
			+"import com.soywiz.korge.*"
			+"import com.soywiz.korge.tween.*"
			+"import com.soywiz.korge.view.*"
			+"import com.soywiz.korim.color.Colors"
			+"import com.soywiz.korim.format.*"
			+"import com.soywiz.korio.async.launchImmediately"
			+"import com.soywiz.korio.file.std.*"
			+"import com.soywiz.korma.geom.degrees"
			+"import com.soywiz.korma.interpolation.Easing"

			SEPARATOR {
				line("""suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"])""") {
					SEPARATOR {
						+"val minDegrees = (-16).degrees"
						+"val maxDegrees = (+16).degrees"
					}
					SEPARATOR {
						line("""val image = image(resourcesVfs["korge.png"].readBitmap())""") {
							+"rotation = maxDegrees"
							+"anchor(.5, .5)"
							+"scale(.8)"
							+"position(256, 256)"
						}
					}
					SEPARATOR {
						line("launchImmediately") {
							line("while (true)") {
								+"image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)"
								+"image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)"
							}
						}
					}
				}
			}
		}


		put("src/commonMain/resources/korge.png", KorgeResources.KORGE_IMAGE)

		putTextFile("src/commonTest/kotlin/test.kt") {
			SEPARATOR {
				+"import com.soywiz.klock.*"
				+"import com.soywiz.korge.input.*"
				+"import com.soywiz.korge.tests.*"
				+"import com.soywiz.korge.tween.*"
				+"import com.soywiz.korge.view.*"
				+"import com.soywiz.korim.color.*"
				+"import com.soywiz.korma.geom.*"
				+"import kotlin.test.*"
			}
			SEPARATOR {
				line("class MyTest : ViewsForTesting()") {
					+"@Test"
					line("fun test() = viewsTest") {
						+"val log = arrayListOf<String>()"
						+"val rect = solidRect(100, 100, Colors.RED)"
						line("rect.onClick") {
							+"""log += "clicked""""
						}
						+"assertEquals(1, views.stage.children.size)"
						+"rect.simulateClick()"
						+"assertEquals(true, rect.isVisibleToUser())"
						+"tween(rect::x[-102], time = 10.seconds)"
						+"assertEquals(Rectangle(x=-102, y=0, width=100, height=100), rect.globalBounds)"
						+"assertEquals(false, rect.isVisibleToUser())"
						+"""assertEquals(listOf("clicked"), log)"""
					}
				}
			}
		}
	}
}

enum class ProjectType(val id: String) {
	Gradle("gradle"),
	GradleKotlinDsl("gradle-kotlin-dsl");

	companion object {
		val BY_ID = values().associateBy { it.id }
		val BY_NAME = values().associateBy { it.name }
		val BY = BY_ID + BY_NAME

		operator fun invoke(name: String): ProjectType = BY[name] ?: error("Unknown project type $name")
	}
}

object Versions {
	val V142 = KorgeVersion(version = "1.4.2", kotlinVersion = "1.3.61")

	val ALL = arrayOf(V142)
	val LAST = V142
	val LAST_EAP = V142

	private val VMAP = ALL.associate { it.version to it }
	fun fromString(version: String): KorgeVersion? = VMAP[version]
}

data class KorgeVersion(
	val version: String,
	val kotlinVersion: String,
	val extraRepos: List<String> = listOf()
) : Comparable<KorgeVersion> {
	val semVersion = SemVer(version)
	val semKotlinVersion = SemVer(kotlinVersion)

	override fun compareTo(other: KorgeVersion): Int = this.semVersion.compareTo(other.semVersion)
	override fun toString(): String = semVersion.toString()
}

class SemVer(val version: String) : Comparable<SemVer> {
	private val parts1 = version.split('-', limit = 2)
	private val parts2 = parts1.first().split('.')

	val major = parts2.getOrNull(0)?.toIntOrNull() ?: 0
	val minor = parts2.getOrNull(1)?.toIntOrNull() ?: 0
	val patch = parts2.getOrNull(2)?.toIntOrNull() ?: 0
	val info = parts1.getOrNull(1) ?: ""

	override fun compareTo(other: SemVer): Int {
		return this.major.compareTo(other.major).takeIf { it != 0 }
			?: this.minor.compareTo(other.minor).takeIf { it != 0 }
			?: this.patch.compareTo(other.patch).takeIf { it != 0 }
			?: this.info.compareTo(other.info).takeIf { it != 0 }
			?: 0
	}

	override fun toString(): String = version
}

