package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.utils.DimenUtils
import com.discord.databinding.WidgetHomeBinding
import com.discord.widgets.home.*

@Suppress("unused")
@AliucordPlugin
class NoPingWidthLimit : Plugin() {
    override fun start(ctx: Context) {
        patcher.patch(
            WidgetHomeHeaderManager::class.java.getDeclaredMethod(
                "configure",
                WidgetHome::class.java,
                WidgetHomeModel::class.java,
                WidgetHomeBinding::class.java
            ), Hook {
                val homeModel = (it.args[1] as WidgetHomeModel)
                if (homeModel.unreadCount < 100) return@Hook // comment out when testing

                val widgetHome = (it.args[0] as WidgetHome)
                val unreadCount = widgetHome.unreadCountView
                val unreadLength = homeModel.unreadCount.toString().length // comment out when testing

                // test plugin
//                unreadCount.text = "69420"
//                val unreadLength = unreadCount.text.length

                unreadCount.layoutParams.apply {
                    width = DimenUtils.dpToPx(unreadLength * 7)
                }
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
