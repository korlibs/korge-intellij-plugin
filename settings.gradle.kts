pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

// @TODO: Dependency substitution: https://docs.gradle.org/current/userguide/composite_builds.html

plugins {
    //id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT"
    id("com.soywiz.kproject.settings") version "0.3.0"
}

kproject("./deps")

rootProject.name = "korge-intellij-plugin"

