package com.github.diamondminer88.plugins

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.ButtonsAPI.ButtonData
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin
import com.discord.api.botuikit.ButtonStyle
import com.discord.models.domain.ModelUserRelationship
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import java.io.File
import java.sql.Timestamp

@Suppress("unused")
@AliucordPlugin
class FriendsExporter : Plugin() {
	private val downloadDir = File(Constants.BASE_PATH, "/friends/")
	private var friendsList: String? = null

	override fun start(ctx: Context) {
		commands.registerCommand("friendsexport", "Export your friends list") {
			val friends = StoreStream.getUserRelationships().relationships
				.filterValues { it == ModelUserRelationship.TYPE_FRIEND }.keys
				.let { StoreStream.getUsers().getUsers(it, true) }

			val content = friends
				.map { (id, user) -> "${user.username}#${user.discriminator} ($id)" }
				.joinToString("\n")
				.also { friendsList = it }
				.replace("""(^> |\*|_|~)""".toRegex()) { "\\${it.value}" }

			CommandResult(
				content,
				null,
				false,
				null,
				null,
				listOf(
					ButtonData(
						"Download",
						ButtonStyle.SUCCESS,
						::onDownload
					)
				)
			)
		}
	}

	private fun onDownload(msg: Message, activity: FragmentActivity) {
		if (friendsList == null) return

		val timestamp = Timestamp(System.currentTimeMillis()).toString()
			.replace(":", "_")
		val downloadFile = File(downloadDir, "friends $timestamp.txt")

		downloadDir.apply { exists() || mkdir() }
		downloadFile.apply {
			createNewFile()
			writeText(friendsList!!)
		}

		friendsList = null
		Utils.showToast("Saved to /Aliucord/friends/")
	}

	override fun stop(context: Context) {
		commands.unregisterAll()
	}
}
