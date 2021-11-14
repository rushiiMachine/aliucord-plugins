package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.discord.api.utcdatetime.UtcDateTime
import com.discord.models.member.GuildMember

@Suppress("unused")
@AliucordPlugin
class NoNitroAvatars : Plugin() {
    override fun start(ctx: Context) {
        patcher.patch(
            GuildMember::class.java.getDeclaredConstructor(
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                List::class.java,
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                UtcDateTime::class.java,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                String::class.java,
            ),
            PreHook {
                it.args[9] = null
//                if (!it.args.contains("a server av hash")) return@PreHook
//                it.args.forEach { Logger().info(":" + it?.toString() + ":") }
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
