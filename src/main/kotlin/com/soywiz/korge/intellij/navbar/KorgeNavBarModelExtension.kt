package com.soywiz.korge.intellij.navbar

import com.intellij.icons.*
import com.intellij.ide.navigationToolbar.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import javax.swing.*

class KorgeNavBarModelExtension : NavBarModelExtension {
    override fun getIcon(`object`: Any?): Icon? {
        return AllIcons.Ide.Gift
    }

    override fun getPresentableText(`object`: Any?): String? {
        return "test"
    }

    override fun getParent(psiElement: PsiElement?): PsiElement? {
        return null
    }

    override fun adjustElement(psiElement: PsiElement): PsiElement? {
        //return psiElement
        return null
    }

    override fun additionalRoots(project: Project?): MutableCollection<VirtualFile> {
        //TODO("Not yet implemented")
        return mutableListOf()
    }
}