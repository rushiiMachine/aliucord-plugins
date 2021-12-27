package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.RxUtils.await
import com.aliucord.wrappers.ChannelWrapper.Companion.name
import com.aliucord.wrappers.ChannelWrapper.Companion.parentId
import com.aliucord.wrappers.ChannelWrapper.Companion.topic
import com.aliucord.wrappers.ChannelWrapper.Companion.type
import com.discord.api.channel.Channel
import com.discord.api.permission.Permission
import com.discord.restapi.RestAPIParams.ChannelPermissionOverwrites
import com.discord.restapi.RestAPIParams.CreateGuildChannel
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.lytefast.flexinput.R

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class CloneChannels : Plugin() {
    private val settingsItemId = Utils.getResId("action_channel_settings", "id")
    private val cloneIconId = Utils.getResId("ic_content_copy_white_a60_24dp", "drawable")

    override fun start(ctx: Context) {
        patcher.after<WidgetChannelsListItemChannelActions>(
            "configureUI",
            WidgetChannelsListItemChannelActions.Model::class.java
        ) {
            val model = it.args[0] as WidgetChannelsListItemChannelActions.Model
            val guild = model.guild
            val channel = model.channel
            if (guild == null || channel.type != Channel.GUILD_TEXT) return@after

            val me = StoreStream.getGuilds().getMember(guild.id, StoreStream.getUsers().me.id)
            val roles = StoreStream.getGuilds().roles[guild.id]
            val permissions = PermissionUtils.computeNonThreadPermissions(
                me.userId,
                guild.id,
                guild.ownerId,
                me,
                roles,
                null
            )
            if (!PermissionUtils.can(Permission.MANAGE_CHANNELS, permissions)) return@after

            val view = (requireView() as NestedScrollView).getChildAt(0) as LinearLayout

            val closeDm = TextView(view.context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
                text = "Clone Channel"
                setCompoundDrawablesWithIntrinsicBounds(cloneIconId, 0, 0, 0)
                setOnClickListener {
                    dismiss()
                    Utils.threadPool.execute {
                        val (_, err) = RestAPI.api.createGuildChannel(
                            model.guild.id,
                            CreateGuildChannel(
                                channel.type,
                                null,
                                channel.name,
                                channel.parentId,
                                channel.s().map { overwrite ->
                                    (ChannelPermissionOverwrites.Companion).fromPermissionOverwrite(overwrite)
                                },
                                channel.topic,
                            )
                        ).await()
                        if (err != null) logger.errorToast("Failed to clone channel!", err)
                    }
                }
            }

            for (index in 0 until view.childCount) {
                if (view.getChildAt(index).id == settingsItemId) {
                    view.addView(closeDm, index + 1)
                    return@after
                }
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
