package com.soywiz.korge.intellij.actions

import com.intellij.icons.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.project.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.config.*
import com.soywiz.korge.intellij.image.*
import com.soywiz.korge.intellij.util.*
import java.awt.*
import java.awt.AWTEvent
import java.awt.event.*
import javax.swing.*

class KorgeAccountAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        //val menu = JBPopupMenu()
        //menu.add("HELLO")
        //menu.show(e.inputEvent.component, 0, 24)
        val c = e.inputEvent.component
        val buttonRect = Rectangle(c.locationOnScreen, c.size )
        val dialogSize = Dimension(220, 234)

        val privateSettings = project!!.korge.globalPrivateSettings

        //val frame = JFrame()
        val frame = JDialog(c.parentFrame())
        //frame.isAlwaysOnTop = true
        frame.isUndecorated = true
        //frame.isOpaque = true
        frame.bounds = Rectangle(Point(buttonRect.right - dialogSize.width - 4, buttonRect.bottom), dialogSize)
        frame.isVisible = true

        frame.contentPane.layout = BorderLayout()
        frame.contentPane.minimumSize = dialogSize
        frame.contentPane.maximumSize = dialogSize
        frame.contentPane.preferredSize = dialogSize
        frame.rootPane.border = BorderFactory.createMatteBorder(2, 2, 2, 2, Color(0x5C1A95));

        frame.contentPane.styled {
            margin = 8.pt
            padding = 8.pt
            verticalStack {
                height = MUnit.Auto
                width = 100.percentage
                icon(
                    ImageIcon(
                        privateSettings.getAvatarBitmap().getScaledInstance(128, 128)
                    )
                ) {
                    preferred = MUnit2(128.pt, 128.pt)
                    max = MUnit2(128.pt, 128.pt)
                    min = MUnit2(128.pt, 128.pt)
                }

                if (privateSettings.isUserLoggedIn()) {
                    label("${privateSettings.userLogin} (${privateSettings.userRank})") {
                        component.icon = KorgeIcons.KORGE
                        component.horizontalAlignment = SwingConstants.CENTER
                    }
                    horizontalStack {
                        height = 5.pt
                    }
                    if (privateSettings.hasAccessToEarlyPreviewFeatures()) {
                        label("KorGE Sponsor. Early-access features") {
                            component.icon = KorgeIcons.SPONSOR
                            component.horizontalAlignment = SwingConstants.CENTER
                        }
                    } else {
                        label("No KorGE Sponsor") {
                            component.horizontalAlignment = SwingConstants.CENTER
                        }
                    }
                } else {
                    label("KorGE: Please, login") {
                        component.icon = KorgeIcons.KORGE
                        component.horizontalAlignment = SwingConstants.CENTER
                    }
                }
                horizontalStack {
                    height = 12.pt
                }
                horizontalStack {
                    height = 24.pt
                    if (privateSettings.isUserLoggedIn()) {
                        button("Account") {
                            component.icon = AllIcons.General.User
                            width = 50.percentage
                            click { privateSettings.openAccount(project) }
                        }
                        button("Logout") {
                            component.icon = AllIcons.Actions.Exit
                            width = 50.percentage
                            click { privateSettings.logout(project) }
                        }
                    } else {
                        button("Login") {
                            component.icon = AllIcons.Nodes.Plugin
                            width = 100.percentage
                            click { privateSettings.login(project) }
                        }
                    }
                }
                horizontalStack {
                    height = 5.pt
                }
                //width = 100.percentage - 128.pt
                horizontalStack {
                    width = 100.percentage
                    height = 24.pt
                    button("Docs") {
                        width = 50.percentage
                        component.icon = AllIcons.General.ContextHelp
                        click {
                            //launchBrowserWithKorgeDocumention()
                            //HTMLEditorProvider.openEditor(project, "KorGE Docs", url = "https://docs.korge.org/")
                            KorgeWebPreviewUtils.open(project, "KorGE Docs", "https://docs.korge.org/")

                        }
                    }
                    button("Discord") {
                        width = 50.percentage
                        component.icon = AllIcons.General.Web
                        click { launchBrowserWithUrl("https://discord.korge.org/") }
                    }
                }

            }
        }
        //frame.contentPane.add(JButton("HELLO!"))
        frame.pack()

        SwingUtilities.invokeLater {
            var eventListener: AWTEventListener? = null

            eventListener = AWTEventListener {
                val mouseEvent = it as MouseEvent
                if (mouseEvent.id == MouseEvent.MOUSE_RELEASED) {
                    //if (!mouseEvent.component.hasAncestor(frame)) {
                    if (true) {
                        frame.isVisible = false
                        SwingUtilities.invokeLater {
                            Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)
                        }
                    }
                }
            }

            Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK)
        }
    }

    override fun update(e: AnActionEvent) {
        KorimImageReaderRegister.initialize
        if (korgeGlobalPrivateSettings.isUserLoggedIn()) {
            e.presentation.text = "${korgeGlobalPrivateSettings.userLogin} (${korgeGlobalPrivateSettings.userRank})"
        } else {
            e.presentation.text = "KorGE Login"
        }
        super.update(e)
        //e.presentation.isVisible = korgeGlobalPrivateSettings.isUserLoggedIn()
        e.presentation.icon = when {
            korgeGlobalPrivateSettings.isUserLoggedIn() -> korgeGlobalPrivateSettings.getAvatarIcon()
            else -> AllIcons.General.User
        }
    }
}
