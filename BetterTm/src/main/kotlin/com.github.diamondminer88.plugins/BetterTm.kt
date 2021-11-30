package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.discord.utilities.images.MGImages

const val TM_EMOJI_URI = "res:///2131823862"

@Suppress("unused")
@AliucordPlugin(requiresRestart = true)
class BetterTm : Plugin() {
    override fun start(ctx: Context) {
        patcher.patch(MGImages::class.java.getMethod(
            "getImageRequest",
            String::class.java,
            Integer.TYPE,
            Integer.TYPE,
            java.lang.Boolean.TYPE
        ), PreHook {
            if (it.args[0] != TM_EMOJI_URI) return@PreHook

            it.args[0] = "https://github.com/DiamondMiner88/aliucord-plugins/blob/master/BetterTm/tm.png"
            it.args[1] = 100
            it.args[2] = 100
        })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}


