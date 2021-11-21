package com.github.diamondminer88.plugins

import android.content.Context
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.DimenUtils
import com.discord.databinding.WidgetHomeBinding
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel
import com.discord.widgets.home.*

@Suppress("unused")
@AliucordPlugin
class NoPingWidthLimit : Plugin() {
    private val channelMentionsId = Utils.getResId("channels_item_channel_mentions", "id")

    override fun start(ctx: Context) {
        patcher.after<WidgetHomeHeaderManager>(
            "configure",
            WidgetHome::class.java,
            WidgetHomeModel::class.java,
            WidgetHomeBinding::class.java
        ) {
            val homeModel = (it.args[1] as WidgetHomeModel)
            if (homeModel.unreadCount < 100) return@after // comment out when testing

            val widgetHome = (it.args[0] as WidgetHome)
            val unreadCount = widgetHome.unreadCountView
            val unreadLength = homeModel.unreadCount.toString().length // comment out when testing

            // testing
//            unreadCount.text = "69420"
//            val unreadLength = unreadCount.text.length

            unreadCount.layoutParams.apply {
                width = DimenUtils.dpToPx(unreadLength * 7)
            }
        }

        patcher.after<WidgetChannelsListAdapter.ItemChannelText>(
            "onConfigure",
            Integer.TYPE,
            ChannelListItem::class.java
        ) {
            val item = it.args[1] as ChannelListItemTextChannel
            if (item.mentionCount < 99) return@after // comment out when testing
            val mentionsView = this.itemView.findViewById<TextView>(channelMentionsId)

            mentionsView.text = "69420" // testing

            val length = mentionsView.text.length
            mentionsView.layoutParams.width = DimenUtils.dpToPx(length * 9)
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
