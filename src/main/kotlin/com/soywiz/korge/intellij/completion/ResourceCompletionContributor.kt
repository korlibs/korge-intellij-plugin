package com.soywiz.korge.intellij.completion

import com.intellij.codeInsight.completion.*
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext


class ResourceCompletionContributor : CompletionContributor() {
    //init {
    //    extend(
    //        CompletionType.BASIC,
    //        PlatformPatterns.psiElement(),
    //        object : CompletionProvider<CompletionParameters?>() {
    //            override fun addCompletions(
    //                parameters: CompletionParameters,
    //                context: ProcessingContext,
    //                resultSet: CompletionResultSet
    //            ) {
    //                for (lookupElement in resultSet.getLookupElements()) {
    //                    // Modify the icon of each completion item
    //                    lookupElement =
    //                        lookupElement.withIcon(AllIcons.General.Warning) // Replace with your custom icon
    //                    resultSet.addElement(lookupElement)
    //                }
    //            }
    //        }
    //    )
    //}

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    }
}
