package com.soywiz.korge.intellij.util

import com.soywiz.korio.serialization.xml.*

// @TODO: Move this to Korio
class BuildXml() {
	private val nodes = arrayListOf<Xml>()
	fun node(node: Xml) = node.also { nodes += node }
	fun node(tag: String, vararg props: Pair<String, Any?>, block: BuildXml.() -> Unit = {}): Xml =
			Xml.Tag(tag, props.filter { it.second != null }.toMap(), BuildXml().apply(block).nodes).also { nodes += it }
	fun comment(comment: String): Xml = Xml.Comment(comment).also { nodes += it }
	fun text(text: String): Xml = Xml.Text(text).also { nodes += it }
	fun cdata(text: String): Xml = Xml.Text(text).also { nodes += it }
}

fun buildXml(rootTag: String, vararg props: Pair<String, Any?>, block: BuildXml.() -> Unit): Xml =
		BuildXml().node(rootTag, *props, block = block)
