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
	private val cmdMsgNotFound =
		CommandResult("This user has not sent a message!", null, false)
	private val cmdGuildOnly =
		CommandResult("This combination cannot be used in dms!", null, false)

	override fun start(startCtx: Context) {
		val options = listOf(
			Utils.createCommandOption(
				ApplicationCommandType.USER,
				"user",
				"Target user to get their first message in this server/dm",
			),
			Utils.createCommandOption(
				ApplicationCommandType.CHANNEL,
				"channel",
				"Target channel to get first message of",
				channelTypes = listOf(Channel.GUILD_TEXT)
			),
			Utils.createCommandOption(
				ApplicationCommandType.BOOLEAN,
				"send",
				"Whether to send the resulting url"
			)
		)

		commands.registerCommand(
			"firstmessage",
			"Get the link to the first message in a channel/from a user.",
			options
		) { ctx ->
			val send = ctx.getBoolOrDefault("send", false)

			if (ctx.containsArg("channel") && ctx.containsArg("user")) {
				if (ctx.currentChannel.isDM()) return@registerCommand cmdGuildOnly

				val user = ctx.getRequiredUser("user")
				val channel = ctx.getRequired("channel") as String

				val (_, message) =
					getFirstMessageInGuildByUser(ctx.currentChannel.guildId, user.id, channel)
						?: return@registerCommand cmdMsgNotFound
				return@registerCommand CommandResult(
					"https://discord.com/channels/${ctx.currentChannel.guildId}/$channel/$message",
					null,
					send
				)
			}

			if (ctx.containsArg("channel")) {
				if (ctx.currentChannel.isDM()) return@registerCommand cmdGuildOnly
				// messageid = 0 does not work on mobile and as such we will have to fetch a real id instead
				val (channel, message) = getFirstMessageInGuildByUser(
					ctx.currentChannel.guildId,
					channelId = ctx.getRequired("channel") as String,
					minId = 0
				)
					?: return@registerCommand cmdMsgNotFound
				return@registerCommand CommandResult(
					"https://discord.com/channels/${ctx.currentChannel.guildId}/$channel/$message",
					null,
					send
				)
			}

			if (ctx.containsArg("user")) {
				val user = ctx.getRequiredUser("user")
				if (ctx.currentChannel.isDM()) {
					val firstMessageId = getFirstDMMessage(ctx.channelId, user.id)
						?: return@registerCommand cmdMsgNotFound
					return@registerCommand CommandResult(
						"https://discord.com/channels/@me/${ctx.channelId}/$firstMessageId",
						null,
						send
					)
				} else {
					val (channel, message) = getFirstMessageInGuildByUser(
						ctx.currentChannel.guildId,
						user.id
					)
						?: return@registerCommand cmdMsgNotFound
					return@registerCommand CommandResult(
						"https://discord.com/channels/${ctx.currentChannel.guildId}/$channel/$message",
						null,
						send
					)
				}
			}

			if (ctx.currentChannel.isDM()) {
				val firstMessageId = getFirstDMMessage(ctx.channelId, minId = 0)
					?: return@registerCommand cmdMsgNotFound
				return@registerCommand CommandResult(
					"https://discord.com/channels/@me/${ctx.channelId}/$firstMessageId",
					null,
					send
				)
			} else {
				val (channel, message) = getFirstMessageInGuildByUser(
					ctx.currentChannel.guildId,
					minId = 0
				)
					?: return@registerCommand cmdMsgNotFound
				return@registerCommand CommandResult(
					"https://discord.com/channels/${ctx.currentChannel.guildId}/$channel/$message",
					null,
					send
				)
			}
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
	private fun getFirstMessageInGuildByUser(
		guildId: Long,
		userId: Long? = null,
		channelId: String? = null,
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
