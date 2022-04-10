package com.github.diamondminer88.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.widgets.chat.list.actions.WidgetChatListActions

private const val REPORT_FORM_URL = "https://support.discord.com/hc/en-us/requests/new?ticket_form_id=360000029731"

@Suppress("unused")
@AliucordPlugin
class AlternateReport : Plugin() {
	private val reportBtnId = Utils.getResId("dialog_chat_actions_report", "id")

	override fun start(ctx: Context) {
		patcher.after<WidgetChatListActions>(
			"configureUI",
			WidgetChatListActions.Model::class.java
		) {
			val model = it.args[0] as WidgetChatListActions.Model

			requireView()
				.findViewById<TextView>(reportBtnId)
				.setOnClickListener {
					val msgUrl = "https://discord.com/channels/${model.message.channelId}/${model.message.id}"

					Utils.setClipboard("Message Link", msgUrl)
					Utils.showToast("Copied url to Clipboard!")

					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPORT_FORM_URL))
					startActivity(intent)
					logger.info("started")
				}
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
