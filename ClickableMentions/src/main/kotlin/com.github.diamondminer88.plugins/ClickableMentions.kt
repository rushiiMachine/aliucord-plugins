package com.github.diamondminer88.plugins

import android.content.Context
import android.text.SpannableStringBuilder
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.utilities.channel.ChannelSelector
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.spans.ClickableSpan
import com.discord.utilities.textprocessing.node.*
import com.discord.widgets.chat.list.WidgetChatList
import com.discord.widgets.chat.list.adapter.*
import com.lytefast.flexinput.R

@Suppress("unused")
@AliucordPlugin
class ClickableMentions : Plugin() {
	private fun makeChannelClickHandler(ctx: Context): (Long) -> Unit {
		return {
			ChannelSelector.getInstance().findAndSet(ctx, it)
		}
	}

	private val userClickHandler: (Long) -> Unit = {
		logger.info("here")
		val adapter = WidgetChatList.`access$getAdapter$p`(Utils.widgetChatList)
		if (adapter.fragmentManager == null) {
			logger.errorToast("Failed to get fragmentManager")
		} else {
			adapter.eventHandler.onUserMentionClicked(
				it,
				adapter.data.channelId,
				adapter.data.guildId
			)
		}
	}

	override fun start(ignored: Context) {
		patcher.after<UserMentionNode<UserMentionNode.RenderContext>>(
			"renderUserMention",
			SpannableStringBuilder::class.java,
			UserMentionNode.RenderContext::class.java
		) {
			val renderContext = it.args[1] as UserMentionNode.RenderContext
			if (renderContext.userMentionOnClick != null) return@after

			val mentionLength = run {
				val userNames = renderContext.userNames
				if (userNames == null || !userNames.containsKey(userId)) 13
				else userNames[this.userId]!!.length + 1
			}

			val builder = it.args[0] as SpannableStringBuilder
			builder.setSpan(
				ClickableSpan(
					ColorCompat.getThemedColor(renderContext.context, R.b.theme_chat_mention_foreground),
					false,
					null,
					`UserMentionNode$renderUserMention$1`(this, userClickHandler),
					4,
					null
				),
				builder.length - mentionLength,
				builder.length,
				33
			)
		}

		patcher.after<ChannelMentionNode<ChannelMentionNode.RenderContext>>(
			"render",
			SpannableStringBuilder::class.java, ChannelMentionNode.RenderContext::class.java
		) {
			val renderContext = it.args[1] as ChannelMentionNode.RenderContext
			if (renderContext.channelMentionOnClick != null) return@after

			val mentionLength = run {
				val userNames = renderContext.channelNames
				if (userNames == null || !userNames.containsKey(channelId)) 16
				else userNames[this.channelId]!!.length + 1
			}

			val builder = it.args[0] as SpannableStringBuilder
			builder.setSpan(
				ClickableSpan(
					ColorCompat.getThemedColor(renderContext.context, R.b.theme_chat_mention_foreground),
					false,
					null,
					`ChannelMentionNode$render$1`(this, makeChannelClickHandler(renderContext.context)),
					4,
					null
				),
				builder.length - mentionLength,
				builder.length,
				33
			)

		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

// -------------------------------------- future reference patches --------------------------------------
//        patcher.instead<WidgetChatListAdapterItemEmbed.Model>(
//            "createRenderContext",
//            Context::class.java,
//            WidgetChatListAdapter.EventHandler::class.java
//        ) {
//            val ctx = it.args[0] as Context
//            val urlHandler = it.args[1] as WidgetChatListAdapter.EventHandler
//            MessageRenderContext(
//                ctx,
//                myId,
//                embedEntry.allowAnimatedEmojis,
//                userNames,
//                channelNames,
//                roles,
//                0,
//                null,
//                `WidgetChatListAdapterItemEmbed$Model$createRenderContext$1`(urlHandler),
//                0,
//                0,
//                null,
//                userClickHandler,
//                makeChannelClickHandler(ctx)
//            )
//        }
