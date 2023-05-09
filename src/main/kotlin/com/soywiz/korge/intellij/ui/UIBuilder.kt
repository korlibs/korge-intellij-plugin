package com.soywiz.korge.intellij.ui

import com.intellij.openapi.ui.*
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.UIUtil
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.color.toRgba
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
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
            dialogPanel.size = preferredSize
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
    val bgcolor = UIUtil.getPanelBackground()
    val panel = JCEFHtmlPanel("")

    fun registerInsideKorgeIntellij(browser: CefBrowser) {
        val css = """
            :root {
              --bgcolor: ${UIUtil.getPanelBackground().toRgba()};
              --labelcolor: ${UIUtil.getLabelForeground().toRgba()};
            }
        """.trimIndent()
        browser.executeJavaScript("var style = document.createElement('style');style.innerHTML = `$css`;document.head.appendChild(style);", browser.url, 0);
    }

    panel.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 100)
    //panel.jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
    //    override fun onAfterCreated(browser: CefBrowser) {
    //        super.onAfterCreated(browser)
    //        registerInsideKorgeIntellij(browser)
    //    }
    //}, panel.cefBrowser)
    panel.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
        //override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        //    if (isLoading) {
        //        registerInsideKorgeIntellij(browser)
        //    }
        //}

        override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
            registerInsideKorgeIntellij(browser)
        }

        override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
            super.onLoadEnd(browser, frame, httpStatusCode)
        }
    }, panel.cefBrowser)

    panel.component.foreground = bgcolor
    panel.component.background = bgcolor
    panel.setPageBackgroundColor(bgcolor.toRgba().hexString)
    panel.setHtml(html)
    component.add(panel.component.also { block(it.styled, panel) })
}
