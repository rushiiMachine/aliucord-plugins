package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.api.guildmember.GuildMember
import com.discord.api.user.User
import java.text.Normalizer

@Suppress("unused")
@AliucordPlugin
class NormalizeNames : Plugin() {
    override fun start(ctx: Context) {
        patcher.after<User>("r") {
            if (it.result != null) {
                it.result = Normalizer.normalize(it.result as String, Normalizer.Form.NFKC)
            }
        }

        patcher.after<GuildMember>("g") {
            if (it.result != null) {
                it.result = Normalizer.normalize(it.result as String, Normalizer.Form.NFKC)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
