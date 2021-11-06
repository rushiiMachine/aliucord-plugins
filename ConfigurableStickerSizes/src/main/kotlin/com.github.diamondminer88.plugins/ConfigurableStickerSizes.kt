package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
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
    private val logger = Logger(this::class.simpleName)
    private val bindingField = WidgetChatListAdapterItemSticker::class.java
        .getDeclaredField("binding")
        .apply { isAccessible = true }

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

                val binding = bindingField.get(it.thisObject)
                        as WidgetChatListAdapterItemStickerBinding

                // set sticker size
                binding.b.layoutParams.apply {
                    height = stickerSize
                    width = stickerSize
                }

                // FIXME: stickers + text can still be sent and will overlap (even though the occurrence is rare)
                // move sticker upward to remove gap between header and sticker
//                (binding.a.layoutParams as RecyclerView.LayoutParams).apply {
//                    setMargins(0, DimenUtils.dpToPx(-16), 0, 0)
//                }
            })

        // hide text
//        patcher.patch(
//            WidgetChatListAdapterItemMessage::class.java.getDeclaredMethod(
//                "onConfigure",
//                Int::class.javaPrimitiveType,
//                ChatListEntry::class.java
//            ), Hook {
//                val thisObj = it.thisObject as WidgetChatListAdapterItemMessage
//                val itemText = ReflectUtils.getField(
//                    WidgetChatListAdapterItemMessage::class.java,
//                    thisObj,
//                    "itemText"
//                ) as SimpleDraweeSpanTextView
//                if (itemText.text[0] == '\u200b')
//                    itemText.visibility = View.GONE
//            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

@SuppressLint("SetTextI18n")
class ConfigurableStickerSizesSettings(private val settings: SettingsAPI) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context

        val stickerSize = settings.getInt("stickerSize", DEFAULT_STICKER_SIZE)

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

                override fun onStopTrackingTouch(seekBar: SeekBar) =
                    settings.setInt("stickerSize", progress + 100)
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
                settings.setInt("stickerSize", 240)
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

        // TODO: reload messages after closing window
        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
            text = "Changes will apply after reloading the current channel"
            textSize = DimenUtils.dpToPx(4).toFloat()
        })
    }
}
