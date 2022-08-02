package com.soywiz.korge.intellij.actions

import com.intellij.ide.browsers.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.project.*
import com.intellij.testFramework.*
import com.intellij.ui.jcef.*
import com.intellij.util.*
import org.cef.browser.*
import org.cef.handler.*
import org.cef.network.*

class KorgeStoreAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        openStore(e.project!!)
    }

    companion object {
        fun openStore(project: Project) {
            KorgeWebPreviewUtils.open(project, "KorGE Store", "http://127.0.0.1:4000/")

            /*
            val app = JBCefApp.getInstance()
            val client = app.createClient()

            client.addLoadHandler(object : CefLoadHandler {
                override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                    browser.executeJavaScript()
                }

                override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                }

                override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                    browser.executeJavaScript()
                }

                override fun onLoadError(browser: CefBrowser, frame: CefFrame, errorCode: CefLoadHandler.ErrorCode, errorText: String, failedUrl: String) {
                }
            })
            val myPanel = JCEFHtmlPanel(file.getPreviewUrl().toExternalForm(), client)
            myPanel.cefBrowser
            myPanel.executeJavaScriptAsync(sExpression())

            val myCefBrowser = myPanel.cefBrowser
            val myJSQueryOpenInBrowser = JBCefJSQuery.create(myCefBrowser as JBCefBrowserBase)
            myJSQueryOpenInBrowser.addHandler { link: String? ->
                null // can respond back to JS with JBCefJSQuery.Response
                JBCefJSQuery.Response("ok")
            }

// Inject the query callback into JS

// Inject the query callback into JS
            myCefBrowser.executeJavaScript(
                "window.JavaPanelBridge = {" +
                    "openInExternalBrowser : function(link) {" +
                    myJSQueryOpenInBrowser.inject("link") +
                    "}" +
                    "};",
                myCefBrowser.getURL(), 0
            )

// Dispose the query when necessary

// Dispose the query when necessary
            Disposer.dispose(myJSQueryOpenInBrowser)
             */

        }
    }
}
