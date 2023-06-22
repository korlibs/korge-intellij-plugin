package com.soywiz.korge.intellij.config

import com.intellij.codeInsight.daemon.*
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.xmlb.*
import korlibs.time.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.util.*
import korlibs.io.async.delay
import korlibs.io.async.launchImmediately
import korlibs.io.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.image.*
import java.net.*
import java.util.*
import javax.imageio.*
import javax.swing.*
import kotlin.coroutines.EmptyCoroutineContext
import com.intellij.ide.projectView.ProjectView
import com.intellij.util.ui.JBImageIcon
import com.soywiz.korge.intellij.internal.dyn
import com.soywiz.korge.intellij.ui.DialogSettings
import com.soywiz.korge.intellij.ui.showNewDialog
import korlibs.io.dynamic.Dyn

@State(
	name = "KorgeGlobalPrivateSettings",
	storages = [Storage("korge-private.xml")]
)
open class KorgeGlobalPrivateSettings : PersistentStateComponent<KorgeGlobalPrivateSettings> {
	var userUuid: String? = null
	var userSponsor: Boolean? = null
	var userLogin: String? = null
	var userAvatar: String? = null
	var userAvatarBytes: String? = null
	private var userAvatarBitmap: BufferedImage? = null
	private var userAvatarIcon: Icon? = null
	var userRank: String? = null
	var userPrice: Int? = null
	var validDate: Long? = null
	var lastChecked: Long = 0L

    fun getSureUserUUID(): String {
        if (userUuid == null) userUuid = UUID.randomUUID().toString()
        return userUuid!!
    }

    fun clearUserUUID() {
        userUuid = null
    }

	private fun getAvatarBytes(): ByteArray {
		if (userAvatarBytes == null) {

			//val img = ImageIO.read(bytes.inputStream()).getScaledInstance(128, 128, Image.SCALE_SMOOTH)
			//userAvatarBytes = Base64.encode(img.toJpegBytes(.8f))
			val bytes = userAvatar?.let { URL(it).readBytes() } ?: KorgeIcons.USER_UNKNOWN_BYTES ?: byteArrayOf()

			userAvatarBytes = korlibs.crypto.encoding.Base64.encode(bytes)
		}
		return korlibs.crypto.encoding.Base64.decode(userAvatarBytes!!)
	}

	fun getAvatarBitmap(): BufferedImage {
		if (userAvatarBitmap == null) {
			userAvatarBitmap = ImageIO.read(getAvatarBytes().inputStream())
		}
		return userAvatarBitmap!!
	}

	fun getAvatarIcon(): Icon {
		if (userAvatarIcon == null) {
			userAvatarIcon = JBImageIcon(getAvatarBitmap().getScaledInstance(16, 16, Image.SCALE_SMOOTH))
		}
		return userAvatarIcon!!
	}

	fun isUserLoggedIn(): Boolean {
		return userLogin != null && validDate != null && validDate!! > System.currentTimeMillis()
	}

	fun getUserDonationPrice(): Int {
		return if (isUserLoggedIn()) userPrice ?: 0 else 0
	}

	fun donatesAtLeast(value: Int): Boolean {
		return getUserDonationPrice() >= value
	}

	fun hasAccessToEarlyPreviewFeatures(): Boolean {
		return donatesAtLeast(5)
		//return donatesAtLeast(0)
	}

    fun boolFixed(value: Any?): Boolean {
        return when (value) {
            is com.soywiz.korge.intellij.internal.Dyn -> boolFixed(value.value)
            is Dyn -> boolFixed(value.value)
            is Boolean -> value
            is String -> value == "1" || value == "true" || value == "on"
            is Number -> value.toInt() != 0
            else -> false
        }
    }

	private val Any?.boolFixed: Boolean get() = boolFixed(this)

	fun nullifyProps() {
		userLogin = null
		userRank = null
		userSponsor = null
		userAvatar = null
		userAvatarBytes = null
		userAvatarBitmap = null
		userPrice = null
		validDate = null
	}

	val ID_KORGE_DOMAIN get() = "https://id.korge.org"
	//val ID_KORGE_DOMAIN get() = "http://localhost:8080"

	fun logout(project: Project?) {
		val oldUuid = userUuid
		val oldUserLogin = userLogin
		launchImmediately(EmptyCoroutineContext) {
			withContext(Dispatchers.IO) {
				URL("$ID_KORGE_DOMAIN/logout?uuid=${oldUuid}")
					.readText()
			}
		}

		clearUserUUID()
		nullifyProps()

        notifyMessage(project, NotificationType.INFORMATION, "KorGE Successful logout", "Goodbye $oldUserLogin")

		reloadAnnotators(project)
	}

    fun notifyMessage(project: Project?, type: NotificationType, title: String, message: String) {
        Notification("korge", title, type).notify(project)
        //Notification("korge", KorgeIcons.KORGE, title, null, message, type, null).notify(project)
    }

	suspend fun updateUserInformation(project: Project?): Boolean {
		if (korgeGlobalPrivateSettings.userUuid == null) return false
		//println("KorgeGlobalPrivateSettings.updateUserInformation IID=${korgeGlobalPrivateSettings.userUuid}")
		try {
			val info =
				withContext(Dispatchers.IO) { Json.parse(URL("$ID_KORGE_DOMAIN/info?uuid=${korgeGlobalPrivateSettings.userUuid}").readText()) }
                    .dyn

            //println(" -> ${info.value.toString()}")

            val logged = info["logged"].boolFixed
            if (logged) {
                val sponsor = info["sponsor"].boolFixed
                val login = info["login"].str
                val avatar = info["avatar"].str
                val rank = info["rank"].str
                val price = info["price"].int
                val validDate = info["validDate"].long
                korgeGlobalPrivateSettings.userLogin = login
                korgeGlobalPrivateSettings.userAvatar = avatar
                korgeGlobalPrivateSettings.userSponsor = sponsor
                korgeGlobalPrivateSettings.userRank = rank
                korgeGlobalPrivateSettings.validDate = validDate
                korgeGlobalPrivateSettings.userPrice = price
                korgeGlobalPrivateSettings.getAvatarIcon() // Forces downloading the avatar
                //println("SUCCESSFUL login : $info")
                println("SUCCESSFUL login")
                return true
            } else {
                println("UN-SUCCESSFUL login")
                korgeGlobalPrivateSettings.nullifyProps()
                return false
            }

		} catch (e: Throwable) {
			e.printStackTrace()
			return false
		}
	}

	fun reloadAnnotators(project: Project?) {
		if (project != null) {
			DaemonCodeAnalyzer.getInstance(project).restart()
			ProjectView.getInstance(project).refresh()
		}
	}

	fun login(project: Project?) {
		//val project = e.project

		// @TODO: This causes a 500 error in the webpage
        val uuid = korgeGlobalPrivateSettings.getSureUserUUID()
		Desktop.getDesktop().browse(URI.create("$ID_KORGE_DOMAIN/?uuid=${uuid}"))
		//println("korgeGlobalPrivateSettings.userUuid: ${korgeGlobalPrivateSettings.userUuid}")
		var dialog: DialogWrapper? = null
		val job = launchImmediately(EmptyCoroutineContext) {
			while (true) {
				try {
					if (updateUserInformation(project)) {
						runInUiThread {
							dialog?.close(0, true)
						}
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
				delay(5.seconds)
			}
		}
		try {
            //showNewDialog("Waiting for login", settings = DialogSettings(onlyCancelButton = true), preferredSize = Dimension(200, 60)) {
            showNewDialog("Waiting for login", settings = DialogSettings(onlyCancelButton = true)) {
                row { label("Use the web browser") }
                row { label("to login into the service") }
                row { label("you used as sponsor") }
                //row { label("UUID=$uuid") }
				dialog = it
			}
		} finally {
			job.cancel()
		}

		if (korgeGlobalPrivateSettings.userLogin != null) {
            notifyMessage(
                project,
                NotificationType.INFORMATION,
                "KorGE Successful login",
                "Welcome ${korgeGlobalPrivateSettings.userLogin}"
            )
			reloadAnnotators(project)
		}
	}

	override fun getState() = this

	override fun loadState(state: KorgeGlobalPrivateSettings) {
		println("KorgeGlobalPrivateSettings.loadState: $this")
		XmlSerializerUtil.copyBean(state, this)

		launchImmediately(EmptyCoroutineContext) {
			delay(0.1.seconds)
			val elapsedTimeSinceLastCheck = DateTime.now() - DateTime.fromUnixMillis(getState().lastChecked)
			println("KorgeGlobalPrivateSettings: elapsedTimeSinceLastCheck=$elapsedTimeSinceLastCheck, lastChecked=$lastChecked")
			if (elapsedTimeSinceLastCheck >= 1.days) {
				lastChecked = DateTime.now().unixMillisLong
				getState()
				updateUserInformation(null)
			} else {
				println("KorgeGlobalPrivateSettings: already checked")
			}
		}
	}

	fun openAccount(project: Project?) {
		launchBrowserWithUrl("https://id.korge.org/")
	}
}

val korgeGlobalPrivateSettings: KorgeGlobalPrivateSettings by lazy { getService<KorgeGlobalPrivateSettings>() }
//val KorgeProjectExt.globalPrivateSettings get() = korgeGlobalPrivateSettings
//fun Project.hasAccessToEarlyPreviewFeatures(): Boolean = korge.globalPrivateSettings.hasAccessToEarlyPreviewFeatures()
fun Project.hasAccessToEarlyPreviewFeatures(): Boolean = korgeGlobalPrivateSettings.hasAccessToEarlyPreviewFeatures()

