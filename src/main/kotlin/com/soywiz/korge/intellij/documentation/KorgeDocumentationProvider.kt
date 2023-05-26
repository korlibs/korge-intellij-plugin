package com.soywiz.korge.intellij.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement

class KorgeDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        //println("KorgeDocumentationProvider.generateHoverDoc: $element : ${element.text} , originalElement : ${originalElement?.text}")
        return null
    }
}
