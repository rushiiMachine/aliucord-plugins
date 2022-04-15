package com.github.diamondminer88.plugins

import android.content.Context
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.stores.StoreStream
import com.discord.widgets.chat.list.actions.WidgetChatListActions

@Suppress("unused", "ClassName")
@AliucordPlugin
class `1ClickDelete` : Plugin() {
	private val reportBtnId = Utils.getResId("dialog_chat_actions_delete", "id")

	override fun start(ctx: Context) {
		patcher.after<WidgetChatListActions>(
			"configureUI",
			WidgetChatListActions.Model::class.java
		) {
			val model = it.args[0] as WidgetChatListActions.Model

			requireView()
				.findViewById<TextView>(reportBtnId)
				.setOnClickListener {
					dismiss()
					StoreStream.getMessages().deleteMessage(model.message)
				}
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
