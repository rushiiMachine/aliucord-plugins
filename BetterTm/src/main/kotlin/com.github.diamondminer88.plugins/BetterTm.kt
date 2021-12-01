package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.discord.utilities.images.MGImages

const val BASE_URL = "https://raw.githubusercontent.com/DiamondMiner88/aliucord-plugins/master/BetterTm/"

const val TM_EMOJI_URI = "res:///2131823862"
const val TM_URL = BASE_URL + "tm.png"

const val R_EMOJI_URI = "res:///2131824085"
const val R_URL = BASE_URL + "r.png"

const val C_EMOJI_URI = "res:///2131824084"
const val C_URL = BASE_URL + "c.png"

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
//            logger.info(it.args[0] as String)

            it.args[0] = when (it.args[0]) {
                TM_EMOJI_URI -> TM_URL
                R_EMOJI_URI -> R_URL
                C_EMOJI_URI -> C_URL
                else -> return@PreHook
            }

            it.args[1] = 100
            it.args[2] = 100
        })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}


