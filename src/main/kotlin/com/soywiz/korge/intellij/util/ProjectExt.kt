package com.soywiz.korge.intellij.util

import com.intellij.openapi.application.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.soywiz.korge.intellij.moduleManager
import com.soywiz.korge.intellij.rootManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*

//val Project.rootFile: VirtualFile? get() = if (projectFile?.canonicalPath?.contains(".idea") == true) projectFile?.parent?.parent else projectFile?.parent
val Project.rootFile: VirtualFile? get() {
    val project = this
    val projectFile = this.projectFile

    val moduleContentRoots = moduleManager.findModuleByName(project.name)?.let { module -> ModuleRootManager.getInstance(module).contentRoots.toList() }
    val moduleFirstContentRoot = moduleContentRoots?.firstOrNull()
    val guessFirstContentRoot = rootManager.contentRoots.firstOrNull()

    //this.rootManager.contentRoots
    //println("Project.rootFile: rootManager.contentRoots=${rootManager.contentRoots.toList()}")
    //println("Project.rootFile: moduleContentRoots=${moduleContentRoots.toList()}")
    //println("Project.rootFile: moduleFirstContentRoot=$moduleFirstContentRoot")
    //println("Project.rootFile: projectFile=$projectFile, path=${projectFile?.path}, name=${projectFile?.name}")
    if (projectFile != null) {
        val rootFile = when {
            // path/to/project/.idea/misc.xml - for directory-based projects
            projectFile.path.contains(".idea") -> projectFile.parent.parent
            // path/to/project/project.ipr - for file-based projects
            else -> projectFile.parent
        }
        //println(" -> $rootFile")
        return rootFile
    }
    return moduleFirstContentRoot ?: guessFirstContentRoot
}

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
val Project.codeStyleManager: CodeStyleManager get() = CodeStyleManager.getInstance(this)
val Project.psiManager: PsiManager get() = PsiManager.getInstance(this)
val Project.psiDocumentManager: PsiDocumentManager get() = PsiDocumentManager.getInstance(this)
val Document.virtualFile: VirtualFile? get() = FileDocumentManager.getInstance().getFile(this)
fun Document.saveDocument() {
    FileDocumentManager.getInstance().saveDocument(this)
}
fun Document.psiFile(project: Project): PsiFile = project.psiManager.findFile(this.virtualFile!!)!!
