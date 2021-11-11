package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.stores.StoreStream
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload
import com.discord.widgets.chat.managereactions.ManageReactionsResultsAdapter.ReactionUserItem
import com.discord.widgets.chat.managereactions.ManageReactionsResultsAdapter.ReactionUserViewHolder
import com.discord.widgets.user.usersheet.WidgetUserSheet

@Suppress("unused")
@AliucordPlugin
class OpenProfileInReactions : Plugin() {
    override fun start(ctx: Context) {
        patcher.patch(
            ReactionUserViewHolder::class.java.getDeclaredMethod(
                "onConfigure",
                Int::class.javaPrimitiveType,
                MGRecyclerDataPayload::class.java
            ), Hook {
                val thisObj = it.thisObject as ReactionUserViewHolder

                if (it.args[1] !is ReactionUserItem) return@Hook
                val reactionItem = it.args[1] as ReactionUserItem

                thisObj.itemView.setOnClickListener {
                    WidgetUserSheet.Companion.`show$default`(
                        WidgetUserSheet.Companion,
                        reactionItem.user.id,
                        reactionItem.channelId,
                        Utils.appActivity.supportFragmentManager,
                        StoreStream.getGuildSelected().selectedGuildId,
                        null,
                        null,
                        null,
                        112,
                        null
                    )
                }
            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
