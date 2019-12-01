package com.soywiz.korge.intellij.module.wizard

import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.util.ui.*
import com.soywiz.korge.intellij.module.*
import com.soywiz.korge.intellij.util.*
import java.awt.*
import java.net.*
import javax.swing.*
import javax.swing.tree.*

class KorgeModuleWizardStep(val config: KorgeModuleConfig) : ModuleWizardStep() {
	override fun updateDataModel() {
		config.projectType = projectTypeCB.selectedItem as ProjectType
		config.featuresToInstall = featuresToCheckbox.keys.filter { it.selected }
		config.korgeVersion = versionCB.selected
	}

	lateinit var projectTypeCB: JComboBox<ProjectType>

	lateinit var versionCB: JComboBox<KorgeVersion>

	lateinit var wrapperCheckBox: JCheckBox

	lateinit var featureList: FeatureCheckboxList

	val featuresToCheckbox = LinkedHashMap<Feature, ThreeStateCheckedTreeNode>()

	val panel by lazy {
		JPanel().apply {
			val description = JPanel().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				border = IdeBorderFactory.createBorder()
			}

			fun showFeatureDocumentation(feature: Feature?) {
				description.removeAll()
				if (feature != null) {
					description.add(JLabel(feature.description, SwingConstants.LEFT))
					for (artifact in feature.artifacts) {
						description.add(JLabel(artifact))
					}
					val doc = feature.documentation
					if (doc != null) {
						description.add(Link(doc, URL(doc)))
					}
				}
				description.doLayout()
				description.repaint()
			}

			featureList = object : FeatureCheckboxList(Features.ALL) {
				override fun onSelected(feature: Feature?, node: ThreeStateCheckedTreeNode) = showFeatureDocumentation(feature)
				override fun onChanged(feature: Feature, node: ThreeStateCheckedTreeNode) = updateTransitive()
			}

			featuresToCheckbox += featureList.featuresToCheckbox

			this.layout = BorderLayout(0, 0)

			add(table {
				tr(
					policy = TdSize.FIXED,
					fill = TdFill.NONE,
					align = TdAlign.CENTER_LEFT
				) {
					td(JLabel("Project:"))
					td(JComboBox(ProjectType.values()).apply { projectTypeCB = this })
					td(JCheckBox("Wrapper", true).apply { wrapperCheckBox = this })
					td(JLabel("Korge Version:"))
					td(JComboBox(Versions.ALL).apply {
						versionCB = this
						selectedItem = Versions.LAST
					})
				}
			}, BorderLayout.NORTH)

			add(Splitter(true, 0.8f, 0.2f, 0.8f).apply {
				this.firstComponent = table {
					tr(
						policy = TdSize.FIXED,
						minHeight = 24,
						maxHeight = 24,
						fill = TdFill.NONE,
						align = TdAlign.CENTER_LEFT
					) {
						td(JLabel("Features:"))
					}
					tr {
						td(featureList.scrollVertical())
					}
				}
				this.secondComponent = description
			}, BorderLayout.CENTER)
		}
	}

	var Feature.selected: Boolean
		get() = featuresToCheckbox[this]?.isChecked ?: false
		set(value) = run { featuresToCheckbox[this]?.isChecked = value }

	var Feature.indeterminate: Boolean
		get() = featuresToCheckbox[this]?.indeterminate ?: false
		set(value) = run { featuresToCheckbox[this]?.indeterminate = value }

	//var Feature.indeterminate : Boolean
	//    get() = featuresToCheckbox[this]?. ?: false
	//    set(value) {
	//        featuresToCheckbox[this]?.isSelected = value
	//    }

	fun updateTransitive() {
		val featureSet = FeatureSet(Features.ALL.filter { it.selected })

		for (feature in Features.ALL) {
			feature.indeterminate = (feature in featureSet.transitive)
		}

		featureList.repaint()
	}

	override fun getComponent() = panel
}

open class ThreeStateCheckedTreeNode : CheckedTreeNode {
	constructor() : super()
	constructor(userObject: Any?) : super(userObject)

	var indeterminate = false
}

abstract class FeatureCheckboxList(val features: List<Feature>) : CheckboxTree(
	object : CheckboxTree.CheckboxTreeCellRenderer() {
		override fun customizeRenderer(
			tree: JTree?,
			value: Any?,
			selected: Boolean,
			expanded: Boolean,
			leaf: Boolean,
			row: Int,
			hasFocus: Boolean
		) {
			if (value is ThreeStateCheckedTreeNode) {
				val feature = value.userObject
				val tscheckbox = checkbox as ThreeStateCheckBox
				if (feature is Feature) {
					val style: SimpleTextAttributes = when {
						value.indeterminate -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
						else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
					}
					textRenderer.append(feature.title, style)
					textRenderer.isEnabled = true
					tscheckbox.isVisible = true
					tscheckbox.state = when {
						value.indeterminate -> ThreeStateCheckBox.State.DONT_CARE
						value.isChecked -> ThreeStateCheckBox.State.SELECTED
						else -> ThreeStateCheckBox.State.NOT_SELECTED
					}
					textRenderer.foreground = UIUtil.getTreeForeground()
				} else if (feature is String) {
					textRenderer.append(feature)
					textRenderer.isEnabled = false
					isEnabled = false
					tscheckbox.isVisible = false
				}
			}
		}
	},
	ThreeStateCheckedTreeNode()
) {
	val CheckedTreeNode?.feature: Feature? get() = this?.userObject as? Feature?

	val featuresToCheckbox = LinkedHashMap<Feature, ThreeStateCheckedTreeNode>()
	val root = (this.model as DefaultTreeModel).root as ThreeStateCheckedTreeNode
	init {
		this.model = object : DefaultTreeModel(root) {
			override fun valueForPathChanged(path: TreePath, newValue: Any) {
				super.valueForPathChanged(path, newValue)
				val node = path.lastPathComponent as ThreeStateCheckedTreeNode
				val feature = node.feature
				if (feature != null) {
					onChanged(feature, node)
				}
			}
		}
	}

	init {
		for ((group, gfeatures) in features.groupBy { it.group }) {
			root.add(ThreeStateCheckedTreeNode(group).apply { isChecked = false })
			for (feature in gfeatures) {
				root.add(ThreeStateCheckedTreeNode(feature).apply { isChecked = false; featuresToCheckbox[feature] = this })
			}
		}
		(this.model as DefaultTreeModel).reload(root)

		addTreeSelectionListener { e ->
			val node = (e.newLeadSelectionPath.lastPathComponent as? ThreeStateCheckedTreeNode)
			val feature = node?.userObject as? Feature?

			if (node != null) {
				onSelected(feature, node)
			}
		}
	}

	abstract fun onSelected(feature: Feature?, node: ThreeStateCheckedTreeNode)
	open fun onChanged(feature: Feature, node: ThreeStateCheckedTreeNode) {
	}
}
