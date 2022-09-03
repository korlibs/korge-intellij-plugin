package com.soywiz.korge.intellij.listeners

import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.soywiz.korge.intellij.util.fixLibraries
import java.util.concurrent.atomic.AtomicBoolean

class KorgeModuleRootListener : ModuleRootListener {

    private var updateInProgress = AtomicBoolean(false)

    override fun beforeRootsChange(event: ModuleRootEvent) {
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        println("Running KorgeGradleSyncListener")
        if (updateInProgress.get()) {
            println("Update currently in progress... Quick returning.")
            return
        }
        updateInProgress.set(true)
        fixLibraries(event.project)
        updateInProgress.set(false)
    }
}
