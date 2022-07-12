package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.settings.delegate
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.widgets.BottomSheet
import com.discord.databinding.WidgetChatListAdapterItemStickerBinding
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemSticker
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.lytefast.flexinput.R

const val DEFAULT_STICKER_SIZE = 240

@Suppress("unused")
@AliucordPlugin
class ConfigurableStickerSizes : Plugin() {
	private val bindingField = WidgetChatListAdapterItemSticker::class.java
		.getDeclaredField("binding")
		.apply { isAccessible = true }

	init {
		settingsTab = SettingsTab(
			ConfigurableStickerSizesSettings::class.java,
			SettingsTab.Type.BOTTOM_SHEET
		).withArgs(settings)
	}

	private var stickerSize: Int by settings.delegate(DEFAULT_STICKER_SIZE)

	override fun start(ctx: Context) {
		patcher.after<WidgetChatListAdapterItemSticker>(
			"onConfigure",
			Integer.TYPE,
			ChatListEntry::class.java
		) {
			val binding = bindingField.get(this)
				as WidgetChatListAdapterItemStickerBinding

			// set sticker size
			binding.b.layoutParams.apply {
				height = stickerSize
				width = stickerSize
			}
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

@SuppressLint("SetTextI18n")
class ConfigurableStickerSizesSettings(settings: SettingsAPI) : BottomSheet() {
	var stickerSize: Int by settings.delegate(DEFAULT_STICKER_SIZE)

	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val ctx = view.context

		val currentSize = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
			text = "$stickerSize px"
			width = DimenUtils.dpToPx(45)
		}

		val slider = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
			layoutParams = LinearLayout.LayoutParams(
				MATCH_PARENT,
				WRAP_CONTENT
			)
			max = 700
			progress = stickerSize - 100
			setPadding(DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12), 0)
			setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
					currentSize.text = "${progress + 100} px"
				}

				override fun onStopTrackingTouch(seekBar: SeekBar) {
					stickerSize = progress + 100
				}
			})
		}

		val resetButton = Button(ctx).apply {
			text = "Reset"
			layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
				setMargins(DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12), 0)
			}
			setOnClickListener {
				currentSize.text = "240 px"
				slider.progress = 140
				stickerSize = 240
			}
		}

		addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
			text = "Sticker size (pixels)"
		})

		addView(LinearLayout(ctx, null, 0, R.i.UiKit_Settings_Item).apply {
			addView(currentSize)
			addView(slider)
		})

		addView(resetButton)

		addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
			text = "Changes will apply after reloading the current channel"
			textSize = DimenUtils.dpToPx(4).toFloat()
		})
	}
}
