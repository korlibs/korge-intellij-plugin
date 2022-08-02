package com.soywiz.korge.intellij.actions

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import com.intellij.*
import com.intellij.icons.*
import com.intellij.ide.*
import com.intellij.ide.browsers.*
import com.intellij.ide.browsers.actions.*
import com.intellij.openapi.*
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.fileTypes.ex.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.jcef.*
import com.intellij.util.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.ui.*
import kotlinx.coroutines.*
import org.cef.browser.*
import org.cef.handler.*
import org.cef.network.*
import org.jetbrains.ide.*
import java.awt.*
import java.beans.*
import java.lang.Runnable
import javax.swing.*
import kotlin.Result
import kotlin.coroutines.*
import kotlin.reflect.*

object KorgeWebPreviewUtils {
    fun open(project: Project, title: String, url: String) {
        val file = LightVirtualFile(title, KorgeWebPreviewFileType.INSTANCE, "")
        file.putUserData(HTMLEditorProvider.AFFINITY_KEY, url)
        val url = Urls.newFromEncoded(url)
        FileEditorManager.getInstance(project).openFile(KorgeWebPreviewVirtualFile(file, url), true)
    }
}

class KorgeWebPreviewFileType private constructor() : FakeFileType() {
    override fun getName(): String = "KorgeWebPreview"
    override fun getDisplayName(): String = IdeBundle.message("filetype.web.preview.display.name")
    override fun getDescription(): String = IdeBundle.message("filetype.web.preview.description")
    override fun isMyFileType(file: VirtualFile): Boolean = file is KorgeWebPreviewVirtualFile
    override fun getIcon(): Icon = AllIcons.Nodes.PpWeb

    companion object {
        val INSTANCE = KorgeWebPreviewFileType()
    }
}

data class KorgeWebPreviewVirtualFile(private val myFile: VirtualFile, val previewUrl: Url) : LightVirtualFile() {
    init {
        fileType = KorgeWebPreviewFileType.INSTANCE
        isWritable = false
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true)
    }
    override fun getOriginalFile(): VirtualFile = myFile
    override fun getName(): String = "Korge Preview of ${myFile.name}"
}

class KorgeWebPreviewEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is KorgeWebPreviewVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = KorgeWebPreviewFileEditor(project, (file as KorgeWebPreviewVirtualFile))

    override fun getEditorTypeId(): String {
        return "korge-web-preview-editor"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}

data class InstallKorgePluginRequest(
    val kind: String = "id",
    val id: String = "id",
    val author: String = "author",
    val title: String = "title",
    val description: String = "description",
    val link: String = "url",
    val url: String = "url",
    val parameters: List<String> = emptyList(),
)

class KorgeWebPreviewFileEditor(project: Project, file: KorgeWebPreviewVirtualFile) : UserDataHolderBase(), FileEditor {
    private val myFile: VirtualFile = file.originalFile
    private val myUrl: String = file.previewUrl.toExternalForm()
    //private val jbApp = JBCefApp.getInstance()
    //private val jbClient = jbApp.createClient().also {
    //    it.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, RegistryManager.getInstance().intValue("ide.browser.jcef.jsQueryPoolSize"))
    //}
    //private val myPanel: JCEFHtmlPanel = JCEFHtmlPanel(jbClient, "")
    private val myPanel: JCEFHtmlPanel = JCEFHtmlPanel("")
    private var disposables = arrayListOf<Disposable>()

    private fun registerInsideKorgeIntellij(browser: CefBrowser = myPanel.cefBrowser) {
        browser.executeJavaScript(
            """
                window.insideKorgeIntellij = true;
                window.insideKorgeIntellijVersion = 1;
            """.trimIndent(), null, 0
        )
        for (disposable in disposables) Disposer.dispose(disposable)
        disposables += browser.registerCallback("installKorgePlugin") { request: InstallKorgePluginRequest ->
            val deferred = CompletableDeferred<Unit>()
            println("installKorgePlugin: $request")
            ApplicationManager.getApplication().invokeLater {
                showDialog("Install ${request.title}", preferredSize = Dimension(400, 200)) {
                    verticalStack {
                        val LEFT = 120.pt
                        horizontalStack {
                            height = 32.pt
                            label("ID:") {
                                width = LEFT
                                height = 32.pt
                            }
                            label(request.id) {
                                width = 500.pt
                            }
                        }
                        horizontalStack {
                            height = 32.pt
                            label("Title:") {
                                width = LEFT
                                height = 32.pt
                            }
                            label(request.title)
                        }
                        horizontalStack {
                            height = 32.pt
                            label("Author:") {
                                width = LEFT
                                height = 32.pt
                            }
                            label(request.author)
                        }
                        horizontalStack {
                            height = 32.pt
                            label("Description:") {
                                width = LEFT
                                height = 32.pt
                            }
                            label(request.description)
                        }
                        for (param in request.parameters) {
                            horizontalStack {
                                height = 32.pt
                                label("${param}:") {
                                    width = LEFT
                                    height = 32.pt
                                }
                                textField("") {
                                    width = 270.pt
                                }
                            }
                        }
                    }
                }
                deferred.complete(Unit)
            }
            deferred.await()
            println("installKorgePlugin DONE: $request")
            mapOf(1 to 2)
        }
        disposables += browser.registerCallback("getKorgePlugins") { it: Any? ->
            //listOf("https://github.com/korlibs/korge-bundles.git::korge-admob::4ac7fcee689e1b541849cedd1e017016128624b9##2ca2bf24ab19e4618077f57092abfc8c5c8fba50b2797a9c6d0e139cd24d8b35")
            listOf<String>()
        }
    }

    init {
        myPanel.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 100)
        //registerInsideKorgeIntellij()
        myPanel.jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                super.onAfterCreated(browser)
                registerInsideKorgeIntellij()
            }
        }, myPanel.cefBrowser)
        myPanel.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                println("KorgeWebPreviewFileEditor.onLoadingStateChange! ${browser}, isLoading=$isLoading, canGoBack=$canGoBack, canGoForward=$canGoForward")
                if (isLoading) {
                    registerInsideKorgeIntellij()
                }
            }

            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                println("KorgeWebPreviewFileEditor.onLoadStart! ${frame.url}, transitionType=$transitionType")
                registerInsideKorgeIntellij()
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                println("KorgeWebPreviewFileEditor.onLoadEnd! $frame, httpStatusCode=$httpStatusCode")
                super.onLoadEnd(browser, frame, httpStatusCode)
            }
        }, myPanel.cefBrowser)

        reloadPage()
        previewsOpened++
        showPreviewTooltip()
    }

    private fun reloadPage() {
        FileDocumentManager.getInstance().saveAllDocuments()
        ApplicationManager.getApplication().saveAll()
        myPanel.loadURL(myUrl)
    }

    private fun showPreviewTooltip() {
        ApplicationManager.getApplication().invokeLater {
            val gotItTooltip = GotItTooltip(
                WEB_PREVIEW_RELOAD_TOOLTIP_ID,
                BuiltInServerBundle.message("reload.on.save.preview.got.it.content"),
                this
            )
            if (!gotItTooltip.canShow()) return@invokeLater
            if (WebBrowserManager.PREVIEW_RELOAD_MODE_DEFAULT != ReloadMode.RELOAD_ON_SAVE) {
                Logger.getInstance(WebPreviewFileEditor::class.java).error(
                    "Default value for " + BuiltInServerBundle.message("reload.on.save.preview.got.it.title") + " has changed, tooltip is outdated."
                )
                return@invokeLater
            }
            if (WebBrowserManager.getInstance().webPreviewReloadMode != ReloadMode.RELOAD_ON_SAVE) {
                // changed before gotIt was shown
                return@invokeLater
            }
            gotItTooltip
                .withHeader(BuiltInServerBundle.message("reload.on.save.preview.got.it.title"))
                .withPosition(Balloon.Position.above)
                .withLink(CommonBundle.message("action.text.configure.ellipsis"),
                    Runnable {
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            null,
                            { it: Configurable? -> it is SearchableConfigurable && it.id == "reference.settings.ide.settings.web.browsers" },
                            null
                        )
                    })
            gotItTooltip.show(myPanel.component) { c: Component?, b: Balloon? -> Point(0, 0) }
        }
    }

    override fun getComponent(): JComponent = myPanel.component
    override fun getPreferredFocusedComponent(): JComponent? = myPanel.component
    override fun getName(): String = IdeBundle.message("web.preview.file.editor.name", myFile.name)
    override fun setState(state: FileEditorState) {}
    override fun getFile(): VirtualFile = myFile
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {
        previewsOpened--
        Disposer.dispose(myPanel)
    }

    companion object {
        const val WEB_PREVIEW_RELOAD_TOOLTIP_ID = "web.preview.reload.on.save"
        private var previewsOpened = 0
        val isPreviewOpened: Boolean get() = previewsOpened > 0
    }
}

val jacksonObjectMapper = jacksonObjectMapper().also {
    it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    it.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
}

// https://plugins.jetbrains.com/docs/intellij/jcef.html#jbcefjsquery
inline fun <reified T : Any> CefBrowser.registerCallback(name: String, coroutineContext: CoroutineContext = EmptyCoroutineContext, noinline block: suspend (T) -> Any?): Disposable =
    registerCallback(name, T::class, coroutineContext, block)
fun <T : Any> CefBrowser.registerCallback(name: String, clazz: KClass<T>, coroutineContext: CoroutineContext = EmptyCoroutineContext, block: suspend (T) -> Any?): Disposable {
    val cefBrowser = this
    val jbCefBrowser = JBCefBrowser.getJBCefBrowser(cefBrowser) ?: error("Can't get JBCefBrowser")
    val jsQuery = JBCefJSQuery.create(jbCefBrowser)
    val jcefLastId = "window.jcefLastId"
    val jcefDeferreds = "window.jcefDeferreds"
    val jcefFunctions = "window.jcefFunctions"
    val callbackId = "callbackId"
    jsQuery.addHandler { param: String ->
        try {
            val (callbackId, json) = param.split("@@@", limit = 2)
            println("CefBrowser.registerCallback['$name'].callbackId=$callbackId, input.json=$json")
            val input = jacksonObjectMapper.readValue(json, clazz.java)
            block.startCoroutine(input, object : Continuation<Any?> {
                override val context: CoroutineContext = coroutineContext

                override fun resumeWith(result: Result<Any?>) {
                    try {
                        val func = if (result.isSuccess) "resolve" else "reject"
                        val json = jacksonObjectMapper.writeValueAsString(result.getOrNull() ?: result.exceptionOrNull())

                        println("CefBrowser.registerCallback['$name'].callbackId=$callbackId, output.json=$json")

                        if (result.isFailure) {
                            result.exceptionOrNull()?.printStackTrace()
                        }

                        jbCefBrowser.executeJavaScriptAsync(
                            """
                        $jcefDeferreds[$callbackId].$func($json);
                    """.trimIndent()
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            })
            null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
    @Suppress("JSUnusedLocalSymbols")
    cefBrowser.executeJavaScript("""
        $jcefLastId = $jcefLastId || 0;
        $jcefDeferreds = $jcefDeferreds || {};
        $jcefFunctions = $jcefFunctions || {};
        $jcefFunctions['$name'] = async function(str) {
            const $callbackId = $jcefLastId++
            const promise = new Promise((resolve, reject) => { $jcefDeferreds[$callbackId] = { resolve, reject }; })
            const json = '' + $callbackId + '@@@' + JSON.stringify(str)
            ${jsQuery.inject("json")}
            try {
                return await promise;
            } finally {
                delete $jcefDeferreds[$callbackId];
            }
        }
    """.trimIndent(), null, 0)
    return jsQuery
}
