package com.soywiz.korge.intellij.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.soywiz.korge.intellij.KorgeIcons


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
        //println("ResourceCompletionContributor.fillCompletionVariants")
        //val position = parameters.position
        //val lookupElement = LookupElementBuilder.create("MyElement").withIcon(KorgeIcons.JITTO)
        //result.addElement(lookupElement)
    }

}
