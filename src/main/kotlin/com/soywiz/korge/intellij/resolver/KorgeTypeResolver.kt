package com.soywiz.korge.intellij.resolver

import org.jetbrains.kotlin.idea.base.utils.fqname.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.*

class KorgeTypeResolver(val element: KtElement) {
    val context by lazy { element.analyze(BodyResolveMode.PARTIAL) }

    fun getFqName(expression: KtExpression?): String? =
        expression?.let { context.getType(expression)?.fqName?.asString() }
}
