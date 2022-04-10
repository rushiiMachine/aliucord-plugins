package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.aliucord.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.models.domain.ModelMessageDelete
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.lytefast.flexinput.R

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class HideMessages : Plugin() {
	private val contextItemId = View.generateViewId()
	private val deleteIconId = Utils.getResId("drawable_chip_delete", "drawable")
	private val deleteContextItemId = Utils.getResId("dialog_chat_actions_delete", "id")
	private val msgIdField = WidgetChatListActions::class.java.getDeclaredField("messageId")
		.apply { isAccessible = true }
	private val channelIdField = WidgetChatListActions::class.java.getDeclaredField("channelId")
		.apply { isAccessible = true }

	override fun start(ctx: Context) {
		patcher.after<WidgetChatListActions>(
			"configureUI",
			WidgetChatListActions.Model::class.java
		) {
			val layout = (requireView() as NestedScrollView).getChildAt(0) as LinearLayout

			if (layout.findViewById<TextView>(contextItemId) != null)
				return@after

			val textView = TextView(layout.context, null, 0, R.i.UiKit_Settings_Item_Icon)
			textView.id = contextItemId
			textView.text = "Hide Message"
			textView.setCompoundDrawablesWithIntrinsicBounds(deleteIconId, 0, 0, 0)
			textView.compoundDrawables[0].setTint(
				ColorCompat.getThemedColor(
					layout.context,
					R.b.colorInteractiveNormal
				)
			)
			textView.setOnClickListener {
				val channelId = channelIdField.getLong(this)
				val msgId = msgIdField.getLong(this)

				dismiss()

				if (PluginManager.isPluginEnabled("MessageLogger")) {
					logger.info("Due to how this plugin works, MessageLogger needs to be disabled")
					PluginManager.disablePlugin("MessageLogger")
					StoreStream.getMessages().handleMessageDelete(ModelMessageDelete(channelId, msgId))
					PluginManager.enablePlugin("MessageLogger")
				} else {
					StoreStream.getMessages().handleMessageDelete(ModelMessageDelete(channelId, msgId))
				}
			}

			for (index in 0 until layout.childCount) {
				if (layout.getChildAt(index).id == deleteContextItemId) {
					layout.addView(textView, index + 1)
					return@after
				}
			}

			layout.addView(textView) // backup in case delete btn is gone completely
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
