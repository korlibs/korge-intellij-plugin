package com.soywiz.korge.intellij.ui

import com.intellij.openapi.ui.*
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.UIUtil
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.color.toRgba
import java.awt.*
import javax.imageio.*
import javax.swing.*

fun icon(path: String) = ImageIcon(ImageIO.read(getResourceFile(path)))
fun toolbarIcon(path: String) = icon("/com/soywiz/korge/intellij/toolbar/$path")

fun showDialog(title: String = "Dialog", settings: DialogSettings = DialogSettings(), preferredSize: Dimension = Dimension(200, 200), block: Styled<JPanel>.(wrapper: DialogWrapper) -> Unit): Boolean {
	class MyDialogWrapper : DialogWrapper(true) {
		override fun createCenterPanel(): JComponent? {
			val dialogPanel = JPanel(FillLayout())
			dialogPanel.preferredSize = preferredSize
			block(dialogPanel.styled, this)
			return dialogPanel
		}

        override fun createActions(): Array<Action> {
            if (settings.onlyCancelButton) {
                return arrayOf(cancelAction)
            } else {
                return super.createActions()
            }
        }

        init {
			init()
            this.title = title
            this.isOK
		}
	}

	return MyDialogWrapper().showAndGet()
}

fun Styled<out Container>.webBrowser(html: String = "<html></html>", block: @UIDslMarker Styled<JComponent>.(JCEFHtmlPanel) -> Unit = {}) {
    //val panel = JCEFHtmlPanel.createBuilder().build()
    val color = UIUtil.getPanelBackground()
    val hexColor = color.toRgba().toString()
    val panel = JCEFHtmlPanel("")
    panel.component.foreground = color
    panel.component.background = color
    panel.setPageBackgroundColor(hexColor)
    panel.setHtml(html)
    component.add(panel.component.also { block(it.styled, panel) })
}
