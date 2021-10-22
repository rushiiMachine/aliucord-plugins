package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.ReflectUtils
import com.aliucord.widgets.BottomSheet
import com.discord.databinding.WidgetChatListAdapterItemStickerBinding
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemSticker
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.lytefast.flexinput.R

const val DEFAULT_STICKER_SIZE = 240

@Suppress("unused")
@AliucordPlugin
class ConfigurableStickerSizes : Plugin() {
    private val logger = Logger(this::class.simpleName)

    init {
        settingsTab = SettingsTab(
            ConfigurableStickerSizesSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(settings)
    }

    override fun start(ctx: Context) {
        patcher.patch(
            WidgetChatListAdapterItemSticker::class.java.getDeclaredMethod(
                "onConfigure",
                Int::class.javaPrimitiveType,
                ChatListEntry::class.java
            ), Hook {
                val stickerSize = settings.getInt("stickerSize", DEFAULT_STICKER_SIZE)

                val binding = ReflectUtils.getField(
                    WidgetChatListAdapterItemSticker::class.java,
                    it.thisObject,
                    "binding"
                ) as WidgetChatListAdapterItemStickerBinding

                val stickerView = binding.b
                stickerView.layoutParams.apply {
                    height = stickerSize
                    width = stickerSize
                }
            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

@SuppressLint("SetTextI18n")
@RequiresApi(Build.VERSION_CODES.O) // "min" im too lazy
class ConfigurableStickerSizesSettings(private val settings: SettingsAPI) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context

        val stickerSize = settings.getInt("stickerSize", DEFAULT_STICKER_SIZE)

        val currentSplits = TextView(ctx, null, 0, R.h.UiKit_TextView).apply {
            text = "$stickerSize px"
            width = DimenUtils.dpToPx(60)
        }

        val seekBar = SeekBar(ctx, null, 0, R.h.UiKit_SeekBar).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            min = 100
            max = 800
            progress = stickerSize
            setPadding(DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    currentSplits.text = "$progress px"
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) =
                    settings.setInt("stickerSize", progress)
            })
        }

        addView(TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Label).apply {
            text = "Sticker size (pixels)"
        })

        addView(LinearLayout(ctx, null, 0, R.h.UiKit_Settings_Item).apply {
            addView(currentSplits)
            addView(seekBar)
        })

        // TODO: reload messages after closing window
        addView(TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Label).apply {
            text = "Changes will apply after reloading the current channel"
            textSize = DimenUtils.dpToPx(4).toFloat()
        })
    }
}
