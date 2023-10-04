package com.soywiz.korge.intellij.actions

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import com.intellij.*
import com.intellij.icons.*
import com.intellij.ide.*
import com.intellij.ide.browsers.*
import com.intellij.ide.browsers.actions.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.externalSystem.importing.*
import com.intellij.openapi.externalSystem.service.execution.*
import com.intellij.openapi.externalSystem.util.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.fileTypes.ex.*
import com.intellij.openapi.options.*
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.*
import com.intellij.ui.*
import com.intellij.ui.jcef.*
import com.intellij.util.*
import com.intellij.util.io.*
import com.intellij.util.ui.UIUtil
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.annotator.*
import com.soywiz.korge.intellij.deps.DepsKProjectYml
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.*
import korlibs.image.color.toRgba
import korlibs.io.file.baseName
import korlibs.io.file.normalize
import korlibs.io.file.pathInfo
import korlibs.io.file.std.ZipVfs
import korlibs.io.lang.*
import korlibs.io.net.URL
import korlibs.io.stream.openAsync
import korlibs.io.util.htmlspecialchars
import kotlinx.coroutines.*
import org.cef.browser.*
import org.cef.handler.*
import org.cef.network.*
import org.jetbrains.ide.*
import org.jetbrains.kotlin.idea.core.util.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.plugins.gradle.util.*
import java.awt.*
import java.beans.*
import java.io.IOException
import java.lang.Runnable
import java.net.URI
import java.net.URISyntaxException
import javax.swing.*
import kotlin.Result
import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.text.Charsets


object KorgeWebPreviewUtils {
    fun open(project: Project, title: String, url: String) {
        val file = LightVirtualFile(title, KorgeWebPreviewFileType.INSTANCE, "")
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
    override fun getName(): String = myFile.name
    override fun getPath(): String = previewUrl.toString().substringBefore('?')
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

class KorgeWebPreviewFileEditor(val project: Project, file: KorgeWebPreviewVirtualFile) : UserDataHolderBase(), FileEditor {
    private val myFile: VirtualFile = file.originalFile
    private val myUrl: String = file.previewUrl.toExternalForm()
    //private val jbApp = JBCefApp.getInstance()
    //private val jbClient = jbApp.createClient().also {
    //    it.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, RegistryManager.getInstance().intValue("ide.browser.jcef.jsQueryPoolSize"))
    //}
    //private val myPanel: JCEFHtmlPanel = JCEFHtmlPanel(jbClient, "")
    private val myPanel: JCEFHtmlPanel = JCEFHtmlPanel("").also {
        it.setPageBackgroundColor(UIUtil.getPanelBackground().toRgba().toString())
    }
    private var disposables = arrayListOf<Disposable>()

    val dataContext: DataContext = project.getDataContext() ?: myPanel.component.getDataContext()

    private fun registerInsideKorgeIntellij(browser: CefBrowser = myPanel.cefBrowser) {
        //println("project.projectFilePath: ${project.projectFilePath}")
        //println("project.projectFile: ${project.projectFile}")
        println("project.rootFile: ${project.rootFile}")
        //println("project.roots: ${project.rootManager.contentRoots.toList()}")
        val buildGradleKfsFile: VirtualFile? = project.rootFile["build.gradle.kts"]

        println("buildGradleKfsFile: $buildGradleKfsFile")

        val css = """
            :root {
              --bgcolor: ${UIUtil.getPanelBackground().toRgba()};
              --labelcolor: ${UIUtil.getLabelForeground().toRgba()};
            }
        """.trimIndent()
        browser.executeJavaScript("var style = document.createElement('style');style.innerHTML = `$css`;document.head.appendChild(style);", browser.url, 0);

        browser.executeJavaScript(
            """
                window.insideKorgeIntellij = true;
                window.insideKorgeIntellijStore = true;
                window.insideKorgeIntellijVersion = 2;
            """.trimIndent(), null, 0
        )
        for (disposable in disposables) Disposer.dispose(disposable)
        disposables += browser.registerCallback("installKorgePlugin") { request: InstallKorgePluginRequest ->
            println("installKorgePlugin: $request")
            val params = LinkedHashMap<String, Styled<JTextField>>()
            val deferred = CompletableDeferred<Boolean>()
            ApplicationManager.getApplication().invokeLater {
                val result = showDialog("Install ${request.title}", preferredSize = Dimension(400, 200)) {
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
                                    params[param] = this
                                    width = 270.pt
                                }
                            }
                        }
                    }
                }
                if (result) {
                    FileEditorManager.getInstance(project).openFile(buildGradleKfsFile!!, true)
                    val psi = buildGradleKfsFile.toPsiFile(project)
                    val korgeBlock = psi?.findDescendantOfType<KtCallExpression> { KorgeBuildGradleAnnotator.isKorgeBlock(it) }
                    if (korgeBlock != null) {
                        println("korge block: $korgeBlock : ${korgeBlock.text}")
                        val addText = buildList {
                            add("bundle(${request.url.quoted})")
                            for ((paramName, paramTextField) in params) {
                                add("config(${paramName.quoted}, ${paramTextField.component.text.quoted})")
                            }
                        }

                        val newText = korgeBlock.text.trim().removeSuffix("}") + "${addText.joinToString("\n")}\n" + "}"
                        korgeBlock.replace(newText, reformat = true)

                        //val gradleBuildRootsManager = GradleBuildRootsManager.getInstance(project)
                        //gradleBuildRootsManager!!.reflective()
                        //.update(KotlinDslGradleBuildSync(ExternalSystemTaskId.getProjectId(project)))

                        project.runReadActionInSmartModeExt {
                            refreshGradleProject(
                                project.getDataContext()
                                    ?: this@KorgeWebPreviewFileEditor.myPanel.component.getDataContext()
                            )
                        }
                    }
                }
                deferred.complete(result)
            }
            val installed = deferred.await()
            println("installKorgePlugin DONE: $request, installed=$installed")
            mapOf("installed" to installed)
        }

        disposables += browser.registerCallback("openNativeBrowser") { url: String ->
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(URI(url))
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            } else {
                System.err.println("Desktop browsing is not supported on this platform")
            }
        }

        disposables += browser.registerCallback("getKorgePlugins") { it: Any? ->
            invokeLaterSuspend {
                val bundleUrls = arrayListOf<String>()
                //FileEditorManager.getInstance(project).openFile(buildGradleKfsFile!!, true)
                val buildText = buildGradleKfsFile?.inputStream?.readBytes()?.toString(Charsets.UTF_8)
                if (buildText != null) {
                    val regex = Regex("bundle\\(\"(.*)\"\\)")
                    for (result in regex.findAll(buildText)) {
                        bundleUrls += result.groupValues[1]
                    }
                }
                /*
                    val psi = buildGradleKfsFile!!.toPsiFile(project)?.text
                    val text = psi?.text
                    val korgeBlock = psi?.findDescendantOfType<KtCallExpression> { KorgeBuildGradleAnnotator.isKorgeBlock(it) }
                    if (korgeBlock != null) {
                        val bundles = korgeBlock.findDescendantsOfType<KtCallExpression> { it.originalElement.text == "bundle" }
                        for (bundle in bundles) {
                            val element = korgeBlock.findDescendantOfType<PsiElement> { it.elementType == KtTokens.REGULAR_STRING_PART }
                            element?.text?.let { bundleUrls += it }
                        }
                    }
                    //listOf("https://github.com/korlibs/korge-bundles.git::korge-admob::4ac7fcee689e1b541849cedd1e017016128624b9##2ca2bf24ab19e4618077f57092abfc8c5c8fba50b2797a9c6d0e139cd24d8b35")
                 */
                bundleUrls
            }
        }


        // NEW KPROJECT

        fun getDepsKProjectYaml(): VirtualFile? {
            return buildGradleKfsFile?.parent?.create("deps.kproject.yml")?.also {
                if (it.contentsToByteArray().isEmpty()) {
                    it.setText(DepsKProjectYml.createEmpty())
                }
            }
        }

        disposables += browser.registerCallback("getKProjectDependencies") { it: Any? ->
            val depsKProjectYaml = getDepsKProjectYaml()
            println("getKProjectDependencies called! project.rootFile=${project.rootFile}, buildGradleKfsFile=$buildGradleKfsFile, depsKProjectYaml=$depsKProjectYaml")
            if (depsKProjectYaml == null) return@registerCallback null
            println("getKProjectDependencies depsKProjectYaml: $depsKProjectYaml")
            val yamlText = depsKProjectYaml.getText()
            println("getKProjectDependencies yamlText: $yamlText")
            val result = DepsKProjectYml.extractDeps(yamlText)
            println("getKProjectDependencies result: $result")
            result
        }

        disposables += browser.registerCallback("installKProjectDependency") { it: InstallKProjectDependencyRequest ->
            val depsKProjectYaml = getDepsKProjectYaml()
            println("installKProjectDependency called! project.rootFile=${project.rootFile}, buildGradleKfsFile=$buildGradleKfsFile, depsKProjectYaml=$depsKProjectYaml")
            if (depsKProjectYaml == null) return@registerCallback null
            val toInstall = it.askForPermissions(project, dataContext)
            if (toInstall) {
                depsKProjectYaml.setText(DepsKProjectYml.addDep(depsKProjectYaml.getText(), it.url, it.removeUrl))
            }
            mapOf("installed" to toInstall)
        }

        disposables += browser.registerCallback("downloadAsset") { dep: InstallKProjectDependencyRequest ->
            val depsKProjectYaml = getDepsKProjectYaml()
            println("downloadAsset called! project.rootFile=${project.rootFile}, buildGradleKfsFile=$buildGradleKfsFile, depsKProjectYaml=$depsKProjectYaml")
            if (depsKProjectYaml == null) return@registerCallback null
            val toInstall = dep.askForPermissions(project, dataContext)
            if (toInstall) {
                val baseName = dep.url.pathInfo.baseName
                val outputName = (dep.output ?: baseName).pathInfo.normalize()
                runBackgroundableTask("Downloading asset: ${dep.url} to $outputName") {
                    if (dep.unzip == true) {
                        runBlocking {
                            val zip = ZipVfs(downloadUrlCached(dep.url).openAsync())
                            val rootFiles = zip.listSimple()
                            val root = if (rootFiles.size == 1 && rootFiles.first().isDirectory()) rootFiles.first() else zip
                            root.copyToRecursively(
                                project.rootFile!!.toVfs()["src/commonMain/resources/${outputName.removeSuffix(".zip")}"]
                            )
                            project.rootFile?.get("src/commonMain/resources")?.refresh(true, true)
                        }
                    } else {
                        project.rootFile!!.createFile("src/commonMain/resources/$outputName", downloadUrlCached(dep.url))
                    }
                }
                //depsKProjectYaml.setText(DepsKProjectYml.addDep(depsKProjectYaml.getText(), it.url, it.removeUrl))
            }
            mapOf("installed" to toInstall)
        }
    }

    data class InstallKProjectDependencyRequest(
        val url: String = "id",
        val title: String = "id",
        val author: String = "author",
        val html: String? = null,
        val icon: String? = null,
        val removeUrl: String? = null,
        val extra: Any? = null,
        val output: String? = null,
        val unzip: Boolean? = null,
    ) {
        suspend fun askForPermissions(project: Project, dataContext: DataContext): Boolean {
            val deferred = CompletableDeferred<Boolean>()

            ApplicationManager.getApplication().invokeLater {
                val result = showDialog("Install ${title}", preferredSize = Dimension(400, 200)) {
                    val bgColor = UIUtil.getPanelBackground()
                    val textColor = UIUtil.getLabelForeground()
                    webBrowser(html ?: """
                        <html>
                            <head>
                                <style>
                                body {
                                    background: ${bgColor.toRgba()};
                                    font: 14px Arial;
                                    color: ${textColor.toRgba()};
                                }
                                p {
                                    margin: 0; padding: 0;
                                }
                                </style>
                            </head>
                            <body>
                                <p><strong>Title:</strong> ${title.htmlspecialchars()}</p>
                                <p><strong>Author:</strong> ${author.htmlspecialchars()}</p>
                                <img src="${icon?.htmlspecialchars()}" style="max-height:calc(100vh - 4em);">
                            </body>
                        </html>
                    """.trimIndent()) {
                        this.component.background = bgColor
                        //this.min = MUnit2(320.pt, 240.pt)
                        //this.preferred = MUnit2(320.pt, 240.pt)
                    }
                }
                deferred.complete(result)
                if (result) {
                    try {
                        refreshGradleProject(dataContext)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
            return deferred.await()
        }
    }


    init {
        myPanel.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 100)

        myPanel.jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                super.onAfterCreated(browser)
                //registerInsideKorgeIntellij()
            }
        }, myPanel.cefBrowser)
        myPanel.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {

            override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                println("KorgeWebPreviewFileEditor.onLoadingStateChange! ${browser}, isLoading=$isLoading, canGoBack=$canGoBack, canGoForward=$canGoForward")
                if (isLoading) {
                    //registerInsideKorgeIntellij()
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
        ApplicationManager.getApplication().saveSettings()
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
    val jsQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)
    val jcefLastId = "window.jcefLastId"
    val jcefDeferreds = "window.jcefDeferreds"
    val jcefFunctions = "window.jcefFunctions"
    val callbackId = "callbackId"
    jsQuery.addHandler { param: String ->
        try {
            val (callbackId, json) = param.split("@@@", limit = 2)
            println("CefBrowser.registerCallback['$name'].callbackId=$callbackId, input.json=$json")
            val input = jacksonObjectMapper.readValue(json, clazz.java)
            //println("!!!! STARTING!")
            block.startCoroutine(input, object : Continuation<Any?> {
                override val context: CoroutineContext = coroutineContext

                override fun resumeWith(result: Result<Any?>) {
                    try {
                        //println("!!!! COMPLETED!")

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
    val jscode = """
        $jcefLastId = $jcefLastId || 0;
        $jcefDeferreds = $jcefDeferreds || {};
        $jcefFunctions = $jcefFunctions || {};
        $jcefFunctions['$name'] = async function(str) {
            const $callbackId = $jcefLastId++
            const promise = new Promise((resolve, reject) => { $jcefDeferreds[$callbackId] = { resolve, reject }; })
            //alert(`calling: ` + $callbackId)
            const json = '' + $callbackId + '@@@' + JSON.stringify(str)
            ${jsQuery.inject("json")}
            try {
                return await promise;
            } finally {
                //alert(`completed: ` + $callbackId)
                delete $jcefDeferreds[$callbackId];
            }
        }
    """.trimIndent()
    @Suppress("JSUnusedLocalSymbols")
    //println("jscode=$jscode")
    cefBrowser.executeJavaScript(jscode, null, 0)
    return jsQuery
}
