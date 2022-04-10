package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.settings.delegate
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.RxUtils.await
import com.aliucord.widgets.BottomSheet
import com.discord.api.premium.PremiumTier
import com.discord.models.domain.NonceGenerator
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.time.ClockFactory
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.notice.WidgetNoticeDialog
import com.lytefast.flexinput.R
import d0.g0.i

private const val DEFAULT_SPLITS = 3
private const val MAX_SPLITS = 7

@Suppress("unused")
@AliucordPlugin
class SplitMessages : Plugin() {
	private val exceptionRegex = Regex("Exception.*(?:\\R+^\\s*at .*)+", i.k) // multiline
	private val textContentField =
		MessageContent::class.java.getDeclaredField("textContent").apply {
			isAccessible = true
		}

	init {
		settingsTab = SettingsTab(
			SplitMessagesSettings::class.java,
			SettingsTab.Type.BOTTOM_SHEET
		).withArgs(settings)
	}

	var maxSplits: Int by settings.delegate(DEFAULT_SPLITS)

	override fun start(ctx: Context) {
		patcher.before<ChatInputViewModel>(
			"sendMessage",
			Context::class.java,
			MessageManager::class.java,
			MessageContent::class.java,
			List::class.java,
			Boolean::class.javaPrimitiveType!!,
			Function1::class.java
		) {
			val isNitro = StoreStream.getUsers().me.premiumTier == PremiumTier.TIER_2
			val maxMessageSize = if (isNitro) 4000 else 2000
			val messageContent = it.args[2] as MessageContent

			var content = textContentField.get(messageContent) as String

			if (content.length > maxMessageSize) {
				if (exceptionRegex.find(content, 0) == null)
					textContentField.set(
						messageContent,
						content.take(maxMessageSize)
					)
				else {
					WidgetNoticeDialog.Builder(it.args[0] as Context)
						.setTitle("SplitMessages")
						.setMessage("Please be courteous and don't send stacktraces in chat. Use a paste service like hastebin instead.")
						.setPositiveButton("Okay", fun(_) {})
						.show(Utils.appActivity.supportFragmentManager)

					it.result = null  // block from "msg too long" dialog
					return@before
				}
			} else return@before

			content = content.drop(maxMessageSize)

			Utils.threadPool.execute {
				var splits = 1

				while (content.isNotEmpty()) {
					if (splits > maxSplits) {
						Utils.showToast("Limiting splits at $maxSplits for safety")
						break
					}
					splits++

					Thread.sleep(1000)

					val message = RestAPIParams.Message(
						content.take(maxMessageSize),
						NonceGenerator.computeNonce(ClockFactory.get()).toString(),
						null,
						null,
						emptyList(),
						null,
						RestAPIParams.Message.AllowedMentions(
							emptyList(),
							emptyList(),
							emptyList(),
							false
						),
						null,
						null
					)
					val (_, err) = RestAPI.api.sendMessage(
						StoreStream.getChannelsSelected().id,
						message
					).await()
					if (err != null) logger.error(err)

					content = content.drop(maxMessageSize)
				}
			}

		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

@SuppressLint("SetTextI18n")
class SplitMessagesSettings(settings: SettingsAPI) : BottomSheet() {
	var maxSplits: Int by settings.delegate(DEFAULT_SPLITS)

	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val ctx = view.context

		val currentSplits = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
			text = "$maxSplits"
			width = DimenUtils.dpToPx(18)
			setPadding(DimenUtils.dpToPx(10), 0, 0, 0)
		}

		val seekBar = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
			layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
			max = MAX_SPLITS
			progress = maxSplits
			setPadding(DimenUtils.dpToPx(24), 0, DimenUtils.dpToPx(12), 0)
			setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
					currentSplits.text = progress.toString()
				}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					maxSplits = progress
				}
			})
		}

		addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
			text = "Max message splits per message"
		})

		addView(LinearLayout(ctx, null, 0, R.i.UiKit_Settings_Item).apply {
			addView(currentSplits)
			addView(seekBar)
		})
	}
}
