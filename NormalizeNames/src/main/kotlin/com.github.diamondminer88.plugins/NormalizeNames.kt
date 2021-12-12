package com.github.diamondminer88.plugins

import android.content.Context
import android.icu.text.Normalizer2
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.api.guildmember.GuildMember
import com.discord.api.user.User

@Suppress("unused")
@AliucordPlugin
class NormalizeNames : Plugin() {
    override fun start(ctx: Context) {
        val normalizer = Normalizer2.getNFKCInstance()
            ?: error("Failed to get normalizer!")

        patcher.after<User>("r") {
            if (it.result != null) {
                it.result = normalizer.normalize(it.result as String)
            }
        }

        patcher.after<GuildMember>("g") {
            if (it.result != null) {
                it.result = normalizer.normalize(it.result as String)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
