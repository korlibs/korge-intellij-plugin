package com.soywiz.korge.intellij.ui

import java.awt.*
import java.util.*
import javax.swing.*
import kotlin.collections.ArrayList

fun verticalList(block: JComponent.() -> Unit) {
}

fun Styled<Container>.button(text: String, block: Styled<JButton>.() -> Unit = {}) {
	component.add(JButton(text).also { block(it.styled) })
}

fun Styled<Container>.stack(vertical: Boolean, block: Styled<Container>.() -> Unit = {}): Container {
	val container = Container()
	//container.layout = BoxLayout(container, if (vertical) BoxLayout.Y_AXIS else BoxLayout.X_AXIS)
	container.layout = LinearLayout(if (vertical) LinearLayout.Direction.VERTICAL else LinearLayout.Direction.HORIZONTAL)
	component.add(container)
	block(container.styled)
	return container
}

fun Styled<Container>.verticalStack(block: Styled<Container>.() -> Unit = {}) = stack(vertical = true, block = block)
fun Styled<Container>.horizontalStack(block: Styled<Container>.() -> Unit = {}) = stack(vertical = false, block = block)

object UIBuilderSample {
	@JvmStatic
	fun main(args: Array<String>) {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		val frame = JFrame()
		//frame.minimumSize = Dimension(200, 200)
		frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
		frame.contentPane.layout = FillLayout()
		frame.contentPane.styled.verticalStack {
			button("HELLO") {
				height = 10.percentage
				minHeight = 64.points
			}
			button("WORLD") {
				height = MUnit.Fill
			}
			button("DEMO") {
				height = 10.percentage
			}
		}

		frame.pack()
		frame.setLocationRelativeTo(null)
		frame.isVisible = true
	}
}

val Container.root: Container get() = this.parent?.root ?: this

sealed class MUnit {
	abstract fun compute(total: Int): Int
	fun compute(total: Int, preferred: Int): Int {
		val result = compute(total)
		return if (result == Int.MIN_VALUE) preferred else result
	}

	object Auto : MUnit() {
		override fun compute(total: Int): Int = Int.MIN_VALUE
	}
	object Fill : MUnit() {
		override fun compute(total: Int): Int = total
	}
	class Ratio(val ratio: Double) : MUnit() {
		override fun compute(total: Int): Int = (total * ratio).toInt()
	}
	class Points(val points: Int) : MUnit() {
		override fun compute(total: Int): Int = points
	}
}

val Number.points get() = MUnit.Points(this.toInt())
val Number.ratio get() = MUnit.Ratio(this.toDouble())
val Number.percentage get() = (this.toDouble() / 100.0).ratio

private val styledWeakMap = WeakHashMap<Component, Styled<Component>>()

val <T : Component> T.styled: Styled<T> get() = styledWeakMap.getOrPut(this) { Styled(this) } as Styled<T>

class Styled<T : Component> constructor(val component: T) {
	var width: MUnit = MUnit.Auto
	var height: MUnit = MUnit.Auto
	var minWidth: MUnit = 0.points
	var minHeight: MUnit = 0.points
	var maxWidth: MUnit = Int.MAX_VALUE.points
	var maxHeight: MUnit = Int.MAX_VALUE.points
	var padding: MUnit = 0.points
	var margin: MUnit = 0.points

	internal var temp: Int = 0
}

class LinearLayout(
	val direction: Direction,
	val growToFill: Boolean = false,
	val shrinkToFill: Boolean = false
) : LayoutAdapter() {
	enum class Direction { VERTICAL, HORIZONTAL }

	fun Styled<Component>.dimension(direction: Direction) = if (direction == Direction.HORIZONTAL) width else height
	fun Styled<Component>.minDimension(direction: Direction) = if (direction == Direction.HORIZONTAL) minWidth else minHeight
	fun Styled<Component>.maxDimension(direction: Direction) = if (direction == Direction.HORIZONTAL) maxWidth else maxHeight

	fun Styled<Component>.preferred(direction: Direction) = if (direction == Direction.HORIZONTAL) preferredDimensions.width else preferredDimensions.height
	fun Styled<Component>.min(direction: Direction) = if (direction == Direction.HORIZONTAL) minDimensions.width else minDimensions.height
	fun Styled<Component>.max(direction: Direction) = if (direction == Direction.HORIZONTAL) maxDimensions.width else maxDimensions.height
	fun Component.size(direction: Direction) = if (direction == Direction.HORIZONTAL) width else height

	override fun layoutChildren(parent: Container, children: List<Styled<Component>>) {
		//println("${parent.size} : ${parent.root.size}")

		val containerSize = parent.size(direction)
		var childrenSize = 0
		var fillCount = 0
		for (doFill in listOf(false, true)) {
			val remaining = containerSize - childrenSize
			val fillSize = (if (fillCount > 0) remaining / fillCount else 0).coerceAtLeast(0)
			for (child in children) {
				val preferred = child.preferred(direction)
				val rdim = child.dimension(direction)
				val min = child.minDimension(direction).compute(containerSize, preferred)
				val dim = rdim.compute(containerSize, preferred)
				val max = child.maxDimension(direction).compute(containerSize, preferred)
				when (rdim) {
					is MUnit.Fill -> {
						if (doFill) {
							child.temp = fillSize.coerceIn(min, max)
							childrenSize += child.temp
						} else {
							fillCount++
						}
					}
					else -> {
						if (!doFill) {
							child.temp = dim.coerceIn(min, max)
							childrenSize += child.temp
						}
					}
				}
			}
		}

		if ((shrinkToFill && childrenSize > containerSize) || (growToFill && childrenSize < containerSize)) {
			val scale = containerSize.toDouble() / childrenSize.toDouble()
			for (child in children) {
				child.temp = (child.temp * scale).toInt()
			}
		}
		//println("childrenSize=$childrenSize, containerSize=$containerSize")

		//println("$containerSize, $remaining, $fillSize")

		var v = 0
		for (child in children) {
			when (direction) {
				Direction.VERTICAL -> child.component.setBounds(0, v, parent.width, child.temp)
				Direction.HORIZONTAL -> child.component.setBounds(v, 0, child.temp, parent.height)
			}
			v += child.temp
		}
		//println("layoutContainer: ${children.toList()}")
	}
}

class FillLayout : LayoutAdapter() {
	override fun layoutChildren(parent: Container, children: List<Styled<Component>>) {
		for (child in children) {
			child.component.setBounds(0, 0, parent.width, parent.height)
		}
	}
}

abstract class LayoutAdapter : LayoutManager2 {
	val minDimensions = Dimension(1, 1)
	val preferredDimensions = Dimension(128, 128)
	val maxDimensions = Dimension(2048, 2048)

	val children = ArrayList<Styled<Component>>()

	abstract fun layoutChildren(parent: Container, children: List<Styled<Component>>)

	override fun invalidateLayout(target: Container) {
	}

	override fun layoutContainer(parent: Container) {
		layoutChildren(parent, children)
	}

	override fun getLayoutAlignmentY(target: Container?): Float {
		return 0f
	}

	override fun getLayoutAlignmentX(target: Container?): Float {
		return 0f
	}

	override fun maximumLayoutSize(target: Container): Dimension = maxDimensions
	override fun preferredLayoutSize(parent: Container): Dimension = preferredDimensions
	override fun minimumLayoutSize(parent: Container): Dimension = minDimensions

	override fun addLayoutComponent(comp: Component, constraints: Any?) {
		//println("addLayoutComponent: $comp, $constraints")
		children.add(comp.styled)
	}

	override fun addLayoutComponent(name: String, comp: Component) {
		//println("addLayoutComponent: $name, $comp")
		children.add(comp.styled)
	}

	override fun removeLayoutComponent(comp: Component) {
		//println("removeLayoutComponent: $comp")
		children.remove(comp.styled)
	}
}
