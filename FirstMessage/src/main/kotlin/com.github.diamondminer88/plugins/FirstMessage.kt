package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin
import com.discord.api.channel.Channel
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.MessageTypes
import java.util.*

@Suppress("unused")
@AliucordPlugin
class FirstMessage : Plugin() {
	private val cmdMsgNotFound
		get() = CommandResult("This user has not sent a message!", null, false)
	private val cmdGuildOnly
		get() = CommandResult("This combination of arguments cannot be used in dms!", null, false)

	override fun start(startCtx: Context) {
		val options = listOf(
			Utils.createCommandOption(
				ApplicationCommandType.USER,
				"user",
				"Get the first message sent by a specific user",
			),
			Utils.createCommandOption(
				ApplicationCommandType.CHANNEL,
				"channel",
				"Get the first message sent in another channel",
				channelTypes = listOf(Channel.GUILD_TEXT)
			),
			Utils.createCommandOption(
				ApplicationCommandType.BOOLEAN,
				"send",
				"Whether to send the resulting message url in chat"
			)
		)

		commands.registerCommand(
			"firstmessage",
			"Get the link to the first message in a channel/from a user.",
			options
		) { ctx ->
			val send = ctx.getBoolOrDefault("send", false)

			// Handle DMs
			if (ctx.currentChannel.isDM()) {
				if (ctx.containsArg("channel"))
					return@registerCommand cmdGuildOnly

				val user = ctx.getUser("user")
				val firstMessageId = getFirstDMMessage(dmId = ctx.channelId, userId = user?.id, minId = if (user == null) 0 else null)
					?: return@registerCommand cmdMsgNotFound

				return@registerCommand CommandResult(
					"https://discord.com/channels/@me/${ctx.channelId}/$firstMessageId",
					null,
					send
				)
			}

			// Get the specified channel
			val channel = if (ctx.containsArg("channel")) {
				ctx.getRequiredChannel("channel")
			} else {
				// Fail if user specified in a thread
				if (ctx.containsArg("user") && ctx.currentChannel.threadMetadata != null)
					return@registerCommand CommandResult("Cannot get the first message sent by a user in a thread!", null, false)

				ctx.currentChannel
			}

			// Get the first message sent in a thread
			if (channel.threadMetadata != null) {
				return@registerCommand CommandResult(
					"https://discord.com/channels/${channel.guildId}/${channel.id}/${channel.id}",
					null,
					send
				)
			}

			// Get the first message sent in a channel by a user
			if (ctx.containsArg("channel") && ctx.containsArg("user")) {
				val user = ctx.getRequiredUser("user")

				val (_, messageId) = getFirstGuildMessage(guildId = ctx.currentChannel.guildId, userId = user.id, channelId = channel.id)
					?: return@registerCommand cmdMsgNotFound

				return@registerCommand CommandResult(
					"https://discord.com/channels/${ctx.currentChannel.guildId}/${channel.id}/$messageId",
					null,
					send
				)
			}

			// Get the first message sent in a channel
			if (ctx.containsArg("channel")) {
				// messageid = 0 does not work on mobile and as such we will have to fetch a real id instead
				val (_, messageId) = getFirstGuildMessage(guildId = ctx.currentChannel.guildId, channelId = channel.id, minId = 0)
					?: return@registerCommand cmdMsgNotFound

				return@registerCommand CommandResult(
					"https://discord.com/channels/${channel.guildId}/${channel.id}/$messageId",
					null,
					send
				)
			}

			// Get the first message sent in a guild by a user
			if (ctx.containsArg("user")) {
				val (foundChannelId, messageId) = getFirstGuildMessage(
					guildId = ctx.currentChannel.guildId,
					userId = ctx.getRequiredUser("user").id,
				) ?: return@registerCommand cmdMsgNotFound

				return@registerCommand CommandResult(
					"https://discord.com/channels/${ctx.currentChannel.guildId}/$foundChannelId/$messageId",
					null,
					send
				)
			}

			// Get the first message sent in a guild
			val (foundChannelId, messageId) = getFirstGuildMessage(
				guildId = ctx.currentChannel.guildId,
				minId = 0,
			) ?: return@registerCommand cmdMsgNotFound

			return@registerCommand CommandResult(
				"https://discord.com/channels/${ctx.currentChannel.guildId}/$foundChannelId/$messageId",
				null,
				send
			)
		}
	}

	override fun stop(context: Context) =
		commands.unregisterAll()

	/**
	 * Gets the first message by a user in a dm
	 * The RestAPI#searchChannelMessages/searchGuildMessages does not support sort order, so I will instead do it manually and easier
	 * @return Channel ID -> Message ID or null if message/channel not found
	 */
	@Suppress("UNCHECKED_CAST")
	private fun getFirstGuildMessage(
		guildId: Long,
		userId: Long? = null,
		channelId: Long? = null,
		minId: Long? = null,
	): Pair<String, String>? {
		val channelParam = if (channelId != null) "&channel_id=$channelId" else ""
		val userParam = if (userId != null) "author_id=$userId" else ""
		val minIdParam = if (minId != null) "min_id=$minId" else ""

		val data = try {
			Http.Request
				.newDiscordRequest("https://discord.com/api/v9/guilds/$guildId/messages/search?$userParam$minIdParam&include_nsfw=true&sort_by=timestamp&sort_order=asc&offset=0$channelParam")
				.execute()
				.json(Map::class.java)
		} catch (e: Error) {
			logger.error(e)
			return null
		}

		val messages = (data["messages"] as List<List<Map<String, *>>>?)
			?.flatten() ?: emptyList()

		for (message in messages) {
			if (message["type"] == MessageTypes.USER_JOIN.toDouble()) continue
			return (message["channel_id"] as String) to (message["id"] as String)
		}
		return null
	}

	/**
	 * Gets the first message by a user in a dm
	 * The RestAPI#searchChannelMessages/searchGuildMessages does not support sort order, so I will instead do it manually and easier
	 * @return Message Id or null if failed
	 */
	@Suppress("UNCHECKED_CAST")
	private fun getFirstDMMessage(
		dmId: Long,
		userId: Long? = null,
		minId: Long? = null,
	): String? {
		val userParam = if (userId != null) "author_id=$userId" else ""
		val minIdParam = if (minId != null) "min_id=$minId" else ""

		val data = try {
			Http.Request
				.newDiscordRequest("https://discord.com/api/v9/channels/${dmId}/messages/search?$userParam$minIdParam&include_nsfw=true&sort_by=timestamp&sort_order=asc&offset=0")
				.execute()
				.json(Map::class.java)
		} catch (e: Error) {
			logger.error(e)
			return null
		}

		return (data["messages"] as List<List<Map<String, *>>>?)
			?.get(0)
			?.get(0)
			?.get("id") as String?
	}
}
