package com.soywiz.korge.intellij.module

@Suppress("unused")
object Features {
	val core = Feature(
		title = "Korge",
		description = "Korge support",
		artifacts = listOf(),
		documentation = "https://korlibs.soywiz.com/korge/",
		dependencies = setOf(),
		group = "Features"
	)
	val f3d = Feature(
		title = "3D Support",
		description = "Adds experimental 3d support",
		artifacts = listOf(),
		documentation = "https://korlibs.soywiz.com/korge/3d/",
		dependencies = setOf(core),
		group = "Features"
	)
	val box2d = Feature(
		title = "Box-2D Support",
		description = "Adds support for Box-2D",
		artifacts = listOf(),
		documentation = "https://korlibs.soywiz.com/korge/physics/box2d/",
		dependencies = setOf(core),
		group = "Features"
	)
	val dragonbones = Feature(
		title = "DragonBones Support",
		description = "Adds support for DragonBones",
		artifacts = listOf(),
		documentation = "https://korlibs.soywiz.com/korge/skeleton/dragonbones/",
		dependencies = setOf(core),
		group = "Features"
	)
	val swf = Feature(
		title = "SWF Support",
		description = "Adds support for Adobe Flash/Animate SWF files",
		artifacts = listOf(),
		documentation = "https://korlibs.soywiz.com/korge/animation/swf/",
		dependencies = setOf(core),
		group = "Features"
	)

	val ALL by lazy { listOf(core, f3d, box2d, dragonbones, swf) }
}

class Feature(
	val title: String,
	val description: String,
	val artifacts: List<String>,
	val documentation: String,
	val dependencies: Set<Feature>,
	val group: String = "Features"
) {
}

class FeatureSet(features: Iterable<Feature>) {
	val direct = features.toSet()
	val all = direct.flatMap { it.dependencies }.toSet()
	val transitive = all - direct
}
