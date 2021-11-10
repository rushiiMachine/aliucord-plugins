package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.models.member.GuildMember

@Suppress("unused")
@AliucordPlugin
class NoNitroAvatars : Plugin() {
    private val avatarHashField = GuildMember::class.java.getDeclaredField("avatarHash")
        .apply { isAccessible = true }

    override fun start(ctx: Context) {
        patcher.patch(
            GuildMember::class.java.declaredConstructors[1], Hook {
                avatarHashField.set(it.thisObject, null)
            }
        )

        // not present in source but it exists so ill patch to be sure
        patcher.patch(
            GuildMember::class.java.declaredConstructors[2], Hook {
                avatarHashField.set(it.thisObject, null)
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
