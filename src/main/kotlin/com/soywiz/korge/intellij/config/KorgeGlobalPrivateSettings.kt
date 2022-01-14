package com.soywiz.korge.intellij.config

import com.intellij.codeInsight.daemon.*
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.xmlb.*
import com.jetbrains.rd.util.string.print
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.ui.showDialog
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.awt.*
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.dynamic.KDynamic
import com.soywiz.korio.serialization.json.Json
import com.soywiz.krypto.encoding.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.image.*
import java.io.*
import java.net.*
import java.util.*
import javax.imageio.*
import javax.imageio.stream.*
import javax.swing.*
import kotlin.coroutines.EmptyCoroutineContext
import com.intellij.ide.projectView.ProjectView




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

	private fun getAvatarBytes(): ByteArray {
		if (userAvatarBytes == null) {

			//val img = ImageIO.read(bytes.inputStream()).getScaledInstance(128, 128, Image.SCALE_SMOOTH)
			//userAvatarBytes = Base64.encode(img.toJpegBytes(.8f))
			val bytes = userAvatar?.let { URL(it).readBytes() } ?: KorgeIcons.USER_UNKNOWN_BYTES ?: byteArrayOf()
			userAvatarBytes = Base64.encode(bytes)
		}
		return Base64.decode(userAvatarBytes!!)
	}

	fun getAvatarBitmap(): BufferedImage {
		if (userAvatarBitmap == null) {
			userAvatarBitmap = ImageIO.read(getAvatarBytes().inputStream())
		}
		return userAvatarBitmap!!
	}

	fun getAvatarIcon(): Icon {
		if (userAvatarIcon == null) {
			userAvatarIcon = ImageIcon(getAvatarBitmap().getScaledInstance(16, 16, Image.SCALE_SMOOTH))
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

	private val Any?.boolFixed: Boolean get() = when (this) {
		is Boolean -> this
		is String -> this == "1" || this == "true" || this == "on"
		is Number -> toInt() != 0
		else -> false
	}

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

		userUuid = null
		nullifyProps()

		Notification(
			"korge",
			KorgeIcons.KORGE,
			"KorGE Successful logout",
			null,
			"Goodbye $oldUserLogin",
			NotificationType.INFORMATION,
			null
		).notify(project)

		reloadAnnotators(project)
	}

	suspend fun updateUserInformation(project: Project?): Boolean {
		if (korgeGlobalPrivateSettings.userUuid == null) return false
		println("KorgeGlobalPrivateSettings.updateUserInformation")
		try {
			val info =
				withContext(Dispatchers.IO) { Json.parse(URL("$ID_KORGE_DOMAIN/info?uuid=${korgeGlobalPrivateSettings.userUuid}").readText()) }
			KDynamic {
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
					korgeGlobalPrivateSettings.nullifyProps()
					return false
				}
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
		korgeGlobalPrivateSettings.userUuid = korgeGlobalPrivateSettings.userUuid ?: UUID.randomUUID().toString()
		Desktop.getDesktop().browse(URI.create("$ID_KORGE_DOMAIN/?uuid=${korgeGlobalPrivateSettings.userUuid}"))
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
			showDialog("Waiting for login", settings = DialogSettings(onlyCancelButton = true), preferredSize = Dimension(200, 60)) {
				label("Use the web browser \nto login into the service \nyou used as sponsor")
				dialog = it
			}
		} finally {
			job.cancel()
		}

		if (korgeGlobalPrivateSettings.userLogin != null) {
			Notification(
				"korge",
				KorgeIcons.KORGE,
				"KorGE Successful login",
				null,
				"Welcome ${korgeGlobalPrivateSettings.userLogin}",
				NotificationType.INFORMATION,
				null
			).notify(project)
			reloadAnnotators(project)
		}
	}

	override fun getState() = this

	override fun loadState(state: KorgeGlobalPrivateSettings) {
		println("KorgeGlobalPrivateSettings.loadState: $this")
		XmlSerializerUtil.copyBean(state, this)

		launchImmediately(EmptyCoroutineContext) {
			delay(0.1.seconds)
			val elapsedTimeSinceLastCheck = DateTime.now() - DateTime.fromUnix(getState().lastChecked)
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
val KorgeProjectExt.globalPrivateSettings get() = korgeGlobalPrivateSettings
fun Project.hasAccessToEarlyPreviewFeatures(): Boolean = korge.globalPrivateSettings.hasAccessToEarlyPreviewFeatures()

