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
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.lytefast.flexinput.R

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class CloseDMs : Plugin() {
	private val muteItemId = Utils.getResId("text_action_mute", "id")
	private val deleteIconId = Utils.getResId("drawable_chip_delete", "drawable")

	override fun start(ctx: Context) {
		patcher.after<WidgetChannelsListItemChannelActions>(
			"configureUI",
			WidgetChannelsListItemChannelActions.Model::class.java
		) {
			val model = it.args[0] as WidgetChannelsListItemChannelActions.Model
			if (model.guild != null) return@after
			val view = (requireView() as NestedScrollView).getChildAt(0) as LinearLayout

			val closeDm = TextView(view.context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
				text = "Close DM"
				setCompoundDrawablesWithIntrinsicBounds(deleteIconId, 0, 0, 0)
				setOnClickListener {
					dismiss()
					Utils.threadPool.execute {
						val (_, err) = (RestAPI.Companion).api.deleteChannel(model.channel.id).await()
						if (err != null) logger.errorToast("Failed to close DM!", err)
					}
				}
			}

			for (index in 0 until view.childCount) {
				if (view.getChildAt(index).id == muteItemId) {
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
