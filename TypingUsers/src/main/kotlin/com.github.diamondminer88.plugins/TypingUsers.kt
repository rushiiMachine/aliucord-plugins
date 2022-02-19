package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.widgets.BottomSheet
import com.discord.databinding.WidgetChatOverlayBinding
import com.discord.models.member.GuildMember
import com.discord.models.user.User
import com.discord.stores.StoreStream
import com.discord.utilities.icon.IconUtils
import com.discord.widgets.chat.overlay.ChatTypingModel
import com.discord.widgets.chat.overlay.WidgetChatOverlay
import com.discord.widgets.settings.WidgetSettings
import com.discord.widgets.user.usersheet.WidgetUserSheet
import com.facebook.drawee.view.SimpleDraweeView
import com.lytefast.flexinput.R

@Suppress("unused")
@AliucordPlugin
class TypingUsers : Plugin() {
    var typingUsers = emptySet<Long>()
    private var widgetSettings: WidgetSettings? = null

    private val fChatOverlayBinding = WidgetChatOverlay.TypingIndicatorViewHolder::class.java
        .getDeclaredField("binding")
        .apply { isAccessible = true }


    override fun start(ctx: Context) {
        StoreStream.getChannelsSelected().observeId().subscribe {
            StoreStream.getUsersTyping().observeTypingUsers(this).subscribe {
                typingUsers = this
            }
        }

        patcher.after<WidgetSettings>("onViewBound", View::class.java) {
            widgetSettings = this
        }

        patcher.after<WidgetChatOverlay.TypingIndicatorViewHolder>(
            "configureTyping",
            ChatTypingModel.Typing::class.java
        ) {
            val model = it.args[0] as ChatTypingModel.Typing
            if (model.typingUsers.isEmpty()) return@after

            val binding = fChatOverlayBinding.get(this) as WidgetChatOverlayBinding
            binding.c.setOnClickListener {
                val manager = widgetSettings?.parentFragmentManager
                    ?: return@setOnClickListener
                TypingUsersSheet(typingUsers).show(manager, "TypingUsers")
            }
        }
    }

    override fun stop(context: Context) {
        widgetSettings = null
        patcher.unpatchAll()
    }
}


@SuppressLint("SetTextI18n")
class TypingUsersSheet(typingUsers: Set<Long>) : BottomSheet() {
    private val guildId = StoreStream.getGuildSelected().selectedGuildId as Long?
    private val members = guildId?.let { StoreStream.getGuilds().members[guildId] }
    private val pairs = typingUsers.map { id ->
        StoreStream.getUsers().users[id] to members?.let { members[id] }
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        pairs.filter { it.first != null }
            .forEach { addView(makeTypingUser(it.first!!, it.second)) }
    }

    companion object {
        private fun openUserProfile(userId: Long) =
            WidgetUserSheet.Companion.`show$default`(
                WidgetUserSheet.Companion,
                userId,
                StoreStream.getChannelsSelected().id,
                Utils.appActivity.supportFragmentManager,
                StoreStream.getGuildSelected().selectedGuildId,
                null,
                null,
                null,
                112,
                null
            )

        private val reactionUserLayoutId = Utils.getResId("widget_manage_reactions_result_user", "layout")
        private fun makeTypingUser(user: User, member: GuildMember?): View {
            val view = Utils.appActivity.layoutInflater.inflate(reactionUserLayoutId, null) as RelativeLayout

            // Set avatar
            IconUtils.`setIcon$default`(
                view.getChildAt(0) as SimpleDraweeView,
                user,
                R.d.avatar_size_standard,
                null,
                null,
                member,
                24,
                null
            )

            val username = view.getChildAt(1) as TextView
            username.text = if (member?.nick == null) user.username else member.nick

            // Remove reaction icon
            view.getChildAt(2).visibility = View.GONE

            view.setOnClickListener { openUserProfile(user.id) }
            return view
        }
    }
}
