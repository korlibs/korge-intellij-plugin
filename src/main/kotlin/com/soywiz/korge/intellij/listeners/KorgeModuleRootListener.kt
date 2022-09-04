package com.soywiz.korge.intellij.listeners

import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.soywiz.korge.intellij.util.LibraryFixer
import com.soywiz.korge.intellij.util.invokeLater
import java.util.concurrent.atomic.AtomicBoolean

class KorgeModuleRootListener : ModuleRootListener {

    override fun beforeRootsChange(event: ModuleRootEvent) {
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        println("Running KorgeGradleSyncListener")
        LibraryFixer.fixLibraries(event.project, false)
    }
}
