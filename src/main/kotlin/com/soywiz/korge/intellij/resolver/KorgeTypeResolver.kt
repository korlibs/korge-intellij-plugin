package com.soywiz.korge.intellij.resolver

import org.jetbrains.kotlin.idea.base.utils.fqname.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.*

class KorgeTypeResolver(val element: KtElement) {
    val context by lazy { element.analyze(BodyResolveMode.PARTIAL) }
    //val contextFull by lazy { element.analyze(BodyResolveMode.FULL) }

    fun getFqName(expression: KtExpression?): String? =
        expression?.let { context.getType(expression)?.fqName?.asString() }
            //?: expression?.let { contextFull.getType(expression)?.fqName?.asString() }
}
