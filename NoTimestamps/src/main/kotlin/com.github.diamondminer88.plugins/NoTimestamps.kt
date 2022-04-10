package com.github.diamondminer88.plugins

import android.content.Context
import android.view.View
import android.widget.TextView
import com.aliucord.PluginManager
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry

@Suppress("unused")
@AliucordPlugin
class NoTimestamps : Plugin() {
	private val timestampId = Utils.getResId("chat_list_adapter_item_text_timestamp", "id")

	override fun start(ctx: Context) {
		if (PluginManager.isPluginEnabled("CustomTimestamps")) {
			Utils.showToast("NoTimestamps: Disabled CustomTimestamps")
			PluginManager.disablePlugin("CustomTimestamps")
		}

		patcher.after<WidgetChatListAdapterItemMessage>(
			"onConfigure",
			Integer.TYPE,
			ChatListEntry::class.java
		) {
			itemView.findViewById<TextView>(timestampId)
				.visibility = View.GONE
		}
	}

	// TODO: enable CustomTimestamps on onUninstall()

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
