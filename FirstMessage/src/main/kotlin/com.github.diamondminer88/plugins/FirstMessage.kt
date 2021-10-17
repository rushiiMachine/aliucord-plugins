package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.utils.ReflectUtils
import com.discord.api.channel.Channel
import com.discord.api.commands.ApplicationCommandType
import com.discord.stores.StoreStream
import com.discord.utilities.analytics.AnalyticSuperProperties
import com.discord.utilities.rest.RestAPI
import com.google.gson.Gson
import java.util.*

@Suppress("unused")
@AliucordPlugin
class FirstMessage : Plugin() {
    private val LOGGER = Logger("FirstMessage")
    private val GSON = Gson()
    private val CMD_COULD_NOT_FIND_MSG = PrivateCommandResult("This user has not sent a message!")

    private fun PrivateCommandResult(content: String) =
        CommandsAPI.CommandResult(content, null, false)

    override fun start(ctx: Context) {
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
            )
        )

        commands.registerCommand(
            "firstmessage",
            "Get the link to the first message in a channel/from a user.",
            options
        ) {
            if (it.containsArg("channel") && it.containsArg("user")) {
                if (it.channel.isDM()) return@registerCommand PrivateCommandResult("This combination cannot be used in dms!")

                val user = it.getRequiredUser("user")
                val channel = it.getRequired("channel") as String

                val firstMessageId =
                    getFirstMessageInGuildByUser(it.channel.guildId, user.id, channel)
                        ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                return@registerCommand PrivateCommandResult("https://discord.com/channels/${it.channel.guildId}/$channel/$firstMessageId")
            }

            if (it.containsArg("channel")) {
                if (it.channel.isDM()) return@registerCommand PrivateCommandResult("This option cannot be used in dms!")
                val channelId = it.getRequired("channel") as String

                // messageid = 0 does not work on mobile and as such we will have to fetch a real id instead
                val firstMessageId = getFirstMessageInGuildByUser(
                    it.channel.guildId,
                    channelId = channelId,
                    minId = 0
                )
                    ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                return@registerCommand PrivateCommandResult("https://discord.com/channels/${it.channel.guildId}/$channelId/$firstMessageId")
            }

            if (it.containsArg("user")) {
                val user = it.getRequiredUser("user")
                if (it.channel.isDM()) {
                    val firstMessageId = getFirstDMMessage(it.channelId, user.id)
                        ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                    return@registerCommand PrivateCommandResult("https://discord.com/channels/@me/${it.channelId}/$firstMessageId")
                } else {
                    val firstMessageId = getFirstMessageInGuildByUser(it.channel.guildId, user.id)
                        ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                    return@registerCommand PrivateCommandResult("https://discord.com/channels/@me/${it.channelId}/$firstMessageId")
                }
            }

            if (it.channel.isDM()) {
                val firstMessageId = getFirstDMMessage(it.channelId, minId = 0)
                    ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                return@registerCommand PrivateCommandResult("https://discord.com/channels/@me/${it.channelId}/$firstMessageId")
            } else {
                val firstMessageId = getFirstMessageInGuildByUser(it.channel.guildId, minId = 0)
                    ?: return@registerCommand CMD_COULD_NOT_FIND_MSG
                return@registerCommand PrivateCommandResult("https://discord.com/channels/${it.channel.guildId}/${it.channelId}/$firstMessageId")
            }
        }
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }

    /**
     * Gets the first message by a user in a dm
     * The RestAPI#searchChannelMessages/searchGuildMessages does not support sort order, so I will instead do it manually and easier
     * @return Message Id or null if failed
     */
    @Suppress("UNCHECKED_CAST")
    private fun getFirstMessageInGuildByUser(
        guildId: Long,
        userId: Long? = null,
        channelId: String? = null,
        minId: Long? = null
    ): String? {
        try {
            val channelParam = if (channelId != null) "&channel_id=$channelId" else ""
            val userParam = if (userId != null) "author_id=$userId" else ""
            val minIdParam = if (minId != null) "min_id=$minId" else ""

            val data =
                sendAuthenticatedGETRequest<Map<String, *>>("https://discord.com/api/v9/guilds/$guildId/messages/search?$userParam$minIdParam&include_nsfw=true&sort_by=timestamp&sort_order=asc&offset=0$channelParam")
            return (data["messages"] as List<List<Map<String, *>>>?)?.get(0)?.get(0)
                ?.get("id") as String?
        } catch (e: Error) {
            return null
        }
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
        minId: Long? = null
    ): String? {
        val userParam = if (userId != null) "author_id=$userId" else ""
        val minIdParam = if (minId != null) "min_id=$minId" else ""

        try {
            val data =
                sendAuthenticatedGETRequest<Map<String, *>>("https://discord.com/api/v9/channels/${dmId}/messages/search?$userParam$minIdParam&include_nsfw=true&sort_by=timestamp&sort_order=asc&offset=0")
            return (data["messages"] as List<List<Map<String, *>>>?)?.get(0)?.get(0)
                ?.get("id") as String?
        } catch (e: Error) {
            LOGGER.error(e)
            return null
        }
    }

    // this is used for the search functionality
    @Suppress("UNCHECKED_CAST")
    private fun <T : Map<String, *>> sendAuthenticatedGETRequest(url: String): T {
        val token = ReflectUtils.getField(StoreStream.getAuthentication(), "authToken") as String?
        val req = Http.Request(url, "GET")
            .setHeader("Authorization", token)
            .setHeader("User-Agent", RestAPI.AppHeadersProvider.INSTANCE.userAgent)
            .setHeader(
                "X-Super-Properties",
                AnalyticSuperProperties.INSTANCE.superPropertiesStringBase64
            )
            .setHeader("Accept", "*/*")

        return GSON.f(req.execute().text(), Map::class.java) as T
    }
}