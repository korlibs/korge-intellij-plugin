buildscript {
    val kotlinVersion: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$kotlinVersion")
    }
}

plugins {
    java
    idea
    id("org.jetbrains.intellij") version "1.13.0"
//    id("org.jetbrains.intellij") version "1.8.1"
//    id("org.jetbrains.intellij") version "1.7.0"
    //id("org.jetbrains.intellij") version "1.5.3" //
    //id("org.jetbrains.intellij") version "1.5.2" //
    //id("org.jetbrains.intellij") version "1.4.0"
}

apply(plugin = "kotlin")

//val jvmVersion = JavaLanguageVersion.of(8)
//
//val compiler = javaToolchains.compilerFor {
//    languageVersion.set(jvmVersion)
//}

//project.tasks
//    .withType<org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain>()
//    .configureEach {
//        kotlinJavaToolchain.jdk.use(
//            compiler.get().metadata.installationPath.asFile.absolutePath,
//            jvmVersion
//        )
//    }


tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        this.jvmTarget = "17"
    }
    //sourceCompatibility = JavaVersion.VERSION_17.toString()
    //targetCompatibility = JavaVersion.VERSION_17.toString()
//    kotlinOptions {
//        freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
//        jvmTarget = "1.8"
//    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    this.maybeCreate("main").apply {
        java {
            //srcDirs("korge-intellij-plugin/src/main/kotlin")
            //srcDirs("src/main/kotlin")
        }
        resources {
            //srcDirs("korge-intellij-plugin/src/main/resources")
            //srcDirs("src/main/resources")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

val kbox2dVersion: String by project
val korgeVersion: String by project
val kotlinVersion: String by project

dependencies {
    //implementation("com.soywiz.korlibs.korge.plugins:korge-build:$korgeVersion")

    // @TODO: Dependency substitution: https://docs.gradle.org/current/userguide/composite_builds.html

    //implementation("com.soywiz.kproject:kproject-common:0.2.6")
    implementation("com.soywiz.korlibs.korge2:korge-jvm:$korgeVersion")
    implementation("com.soywiz.korlibs.korge2:korge-dragonbones-jvm:$korgeVersion")
    implementation("com.soywiz.korlibs.korge2:korge-spine-jvm:$korgeVersion")
    implementation("com.soywiz.korlibs.korge2:korge-swf-jvm:$korgeVersion")
    implementation("com.soywiz.korlibs.kbox2d:kbox2d-jvm:$kbox2dVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    //implementation("com.cjcrafter:openai:1.3.0")
    //implementation("pw.mihou:Dotenv:1.0.1")
    //implementation("com.squareup.okhttp3:okhttp:4.9.2")

    //implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.8")
    //implementation("javax.xml.bind:jaxb-api:2.3.1")
    //implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    //implementation("net.sourceforge.mydoggy:mydoggy:1.4.2")
    //implementation("net.sourceforge.mydoggy:mydoggy-plaf:1.4.2")
    //implementation("net.sourceforge.mydoggy:mydoggy-api:1.4.2")
    //implementation("net.sourceforge.mydoggy:mydoggy-res:1.4.2")
    //implementation(project(":korge-build"))
}

intellij {
    // IntelliJ IDEA dependency
    //version.set("IC-2021.3.1")
    //version.set("IC-2022.1")
    //version.set("IC-2022.3.2")
    version.set("IC-2023.1")
    // Bundled plugin dependencies
    plugins.addAll(
        "gradle", "java", "platform-images", "Kotlin", "gradle-java"
    )

    pluginName.set("KorgePlugin")
    updateSinceUntilBuild.set(false)

    downloadSources.set(true)

    //sandboxDir.set(layout.projectDirectory.dir(".sandbox").toString())
}

tasks {
    // @TODO: Dependency substitution: https://docs.gradle.org/current/userguide/composite_builds.html

    //val compileKotlin by existing(Task::class) {
    //    dependsOn(gradle.includedBuild("korge-next").task(":publishJvmPublicationToMavenLocal"))
    //}

    val runIde by existing(org.jetbrains.intellij.tasks.RunIdeTask::class) {
        maxHeapSize = "4g"
        //dependsOn(":korge-next:publishJvmPublicationToMavenLocal")
    }
//    val runDebugTilemap by creating(JavaExec::class) {
//        //classpath = sourceSets.main.runtimeClasspath
//        classpath = sourceSets["main"].runtimeClasspath + configurations["idea"]
//
//        main = "com.soywiz.korge.intellij.editor.tile.MyTileMapEditorFrame"
//    }
//    val runUISample by creating(JavaExec::class) {
//        //classpath = sourceSets.main.runtimeClasspath
//        classpath = sourceSets["main"].runtimeClasspath + configurations["idea"]
//
//        main = "com.soywiz.korge.intellij.ui.UIBuilderSample"
//    }

    // ./gradlew runIde --dry-run

    //afterEvaluate { println((runIde.get() as Task).dependsOn.toList()) }
}

//println(gradle.includedBuilds)
