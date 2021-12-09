package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.settings.delegate
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.widgets.BottomSheet
import com.discord.api.presence.ClientStatus
import com.discord.models.domain.ModelUserRelationship
import com.discord.models.presence.Presence
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting
import com.discord.widgets.guilds.list.*
import com.lytefast.flexinput.R

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin(requiresRestart = true)
class Counters : Plugin() {
    private val dividerId = Utils.getResId("widget_guilds_list_item_divider", "layout")

    private val serverCount: Boolean by settings.delegate(true)
    private val onlineFriendCount: Boolean by settings.delegate(true)

    init {
        settingsTab = SettingsTab(
            CountersSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(settings)
    }

    override fun start(ignored: Context) {
        patcher.patch(
            WidgetGuildListAdapter::class.java.getDeclaredMethod(
                "onCreateViewHolder",
                ViewGroup::class.java,
                Int::class.javaPrimitiveType
            ), PreHook {
                if (it.args[1] as Int != GuildListItem.TYPE_DIVIDER) return@PreHook

                val inflater = `WidgetGuildListAdapter$onCreateViewHolder$1`(it.args[0] as ViewGroup)
                val divider = inflater.invoke(dividerId) as View
                val ctx = divider.context

                val container = LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                }

                if (serverCount) {
                    val serverCounter = TextView(ctx, null, 0, R.i.UiKit_TextView_H6)
                        .apply {
                            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorChannelDefault))
                            textSize = DimenUtils.dpToPx(3.7f).toFloat()
                            isAllCaps = false
                            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_HORIZONTAL
                            }
                        }
                    StoreStream.getGuilds().observeGuilds().subscribe {
                        Utils.mainThread.post {
                            serverCounter.text = "Servers - $size"
                        }
                    }

                    container.addView(serverCounter)
                }

                if (onlineFriendCount) {
                    val onlineCounter = TextView(ctx, null, 0, R.i.UiKit_TextView_H6)
                        .apply {
                            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorChannelDefault))
                            textSize = DimenUtils.dpToPx(3.7f).toFloat()
                            isAllCaps = false
                            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_HORIZONTAL
                            }
                        }
                    StoreStream.getPresences().observeAllPresences().subscribe {
                        val presences = StoreStream.getPresences().presences
                        val online = StoreStream.getUserRelationships().relationships
                            .filter { r -> r.value == ModelUserRelationship.TYPE_FRIEND }.keys
                            .mapNotNull { id -> presences[id] as Presence? }
                            .filter { p -> p.status != ClientStatus.OFFLINE && p.status != ClientStatus.OFFLINE }
                        Utils.mainThread.post {
                            onlineCounter.text = "Online - ${online.size}"
                        }
                    }

                    container.addView(onlineCounter)
                }

                container.addView(divider)
                it.result = GuildListViewHolder.SimpleViewHolder(container)
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

class CountersSettings(settings: SettingsAPI) : BottomSheet() {
    private var serverCount: Boolean by settings.delegate(true)
    private var onlineFriendCount: Boolean by settings.delegate(true)

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context

        addView(
            Utils.createCheckedSetting(
                ctx, CheckedSetting.ViewType.SWITCH,
                "Server count",
                "Show the amount of servers you are in near the top of the guild list."
            ).apply {
                isChecked = serverCount
                setOnCheckedListener {
                    serverCount = it
                    Utils.promptRestart()
                }
            })

        addView(
            Utils.createCheckedSetting(
                ctx, CheckedSetting.ViewType.SWITCH,
                "Online friend count",
                "Show the amount of friends currently online near the top of the guild list."
            ).apply {
                isChecked = onlineFriendCount
                setOnCheckedListener {
                    onlineFriendCount = it
                    Utils.promptRestart()
                }
            })
    }
}
