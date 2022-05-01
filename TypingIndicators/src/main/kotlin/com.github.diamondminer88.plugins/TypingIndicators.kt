package com.github.diamondminer88.plugins

import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.RxUtils.subscribe
import com.discord.api.channel.Channel
import com.discord.stores.*
import com.discord.views.typing.TypingDots
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel
import com.discord.widgets.chat.overlay.ChatTypingModel
import com.discord.widgets.chat.overlay.`ChatTypingModel$Companion$get$1`
import rx.Observable
import rx.Subscription
import java.lang.reflect.Method

@Suppress("unused")
@AliucordPlugin
class TypingIndicators : Plugin() {
	private val typingDotsId = View.generateViewId()
	private val allTypingDots = mutableMapOf<TypingDots, Subscription>()
	private lateinit var mNewStopTypingDots: Method

	override fun start(context: Context) {
		if (Constants.DISCORD_VERSION >= 124012) {
			mNewStopTypingDots = TypingDots::class.java.getDeclaredMethod("c")
		}

		val lp = RelativeLayout.LayoutParams(DimenUtils.dpToPx(24), RelativeLayout.LayoutParams.MATCH_PARENT)
			.apply {
				marginEnd = DimenUtils.dpToPx(16)
				addRule(RelativeLayout.ALIGN_PARENT_END)
				addRule(RelativeLayout.CENTER_VERTICAL)
			}

		patcher.after<WidgetChannelsListAdapter.ItemChannelText>(
			"onConfigure",
			Integer.TYPE,
			ChannelListItem::class.java
		) {
			val textChannel = it.args[1] as ChannelListItemTextChannel
			val itemChannelText = it.thisObject as WidgetChannelsListAdapter.ItemChannelText
			val view = this.itemView as RelativeLayout

			val existingTypingDots = itemChannelText.itemView.findViewById<TypingDots>(typingDotsId)
			if (existingTypingDots != null) {
				allTypingDots[existingTypingDots]?.unsubscribe()
				subscribeTypingDots(existingTypingDots, textChannel.channel)
				return@after
			}

			val typingDots = TypingDots(Utils.appActivity, null).apply {
				id = typingDotsId
				visibility = View.GONE
				alpha = 0.4f
				scaleY = 0.8f
				scaleX = 0.8f
			}
			view.addView(typingDots, lp)

			subscribeTypingDots(typingDots, textChannel.channel)
		}

		patcher.after<StoreGuildSelected>(
			"handleGuildSelected",
			java.lang.Long.TYPE
		) {
			val currentGuild = StoreStream.getGuildSelected().selectedGuildId
			val targetGuild = it.args[0]

			if (currentGuild == targetGuild) return@after
			allTypingDots.values.forEach(Subscription::unsubscribe)
			allTypingDots.clear()
		}
	}

	private fun subscribeTypingDots(typingDots: TypingDots, channel: Channel) {
		val subscription =
			`ChatTypingModel$Companion$get$1`<StoreChannelsSelected.ResolvedSelectedChannel, Observable<ChatTypingModel.Typing>>()
				.call(
					StoreChannelsSelected.ResolvedSelectedChannel.Channel(
						channel,
						null, null
					)
				).subscribe {
					this as ChatTypingModel.Typing
					Utils.mainThread.post {
						if (typingUsers.isEmpty()) {
							if (Constants.DISCORD_VERSION >= 124012) {
								mNewStopTypingDots.invoke(typingDots)
							}
							else typingDots.b()
							typingDots.visibility = View.GONE
						} else {
							typingDots.a(false)
							typingDots.visibility = View.VISIBLE
						}
					}
				}
		allTypingDots[typingDots] = subscription
	}

	override fun stop(context: Context) {
		allTypingDots.keys.forEach {
			it.b()
			it.visibility = View.GONE
		}
		allTypingDots.values.forEach(Subscription::unsubscribe)
		allTypingDots.clear()

		patcher.unpatchAll()
	}
}
