package com.soywiz.korge.intellij.util

import com.intellij.openapi.application.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korge.intellij.rootManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*

//val Project.rootFile: VirtualFile? get() = if (projectFile?.canonicalPath?.contains(".idea") == true) projectFile?.parent?.parent else projectFile?.parent
val Project.rootFile: VirtualFile? get() = rootManager.contentRoots.firstOrNull()

fun <T> Project.runReadActionInSmartModeExt(action: () -> T): T {
    if (ApplicationManager.getApplication().isReadAccessAllowed) return action()
    return DumbService.getInstance(this).runReadActionInSmartMode(action)
}

fun <T> invokeAndWaitIfNeededExt(modalityState: ModalityState? = null, runnable: () -> T): T {
    val app = ApplicationManager.getApplication()
    if (app.isDispatchThread) {
        return runnable()
    }
    else {
        return computeDelegatedExt {
            app.invokeAndWait({ it (runnable()) }, modalityState ?: ModalityState.defaultModalityState())
        }
    }
}

internal inline fun <T> computeDelegatedExt(executor: (setter: (T) -> Unit) -> Unit): T {
    var resultRef: T? = null
    executor { resultRef = it }
    @Suppress("UNCHECKED_CAST")
    return resultRef as T
}

fun KtDeclaration.typeExt() =
    (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType

fun KtReferenceExpression.resolveExt() =
    mainReference.resolve()

val Project.fileEditorManager: FileEditorManager get() = FileEditorManager.getInstance(this)
