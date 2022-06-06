package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.api.utcdatetime.UtcDateTime
import com.discord.models.member.GuildMember

@Suppress("unused")
@AliucordPlugin(requiresRestart = true)
class NoNitroAvatars : Plugin() {
	override fun start(ctx: Context) {
		patcher.before<GuildMember>(
			Int::class.javaPrimitiveType!!,
			Long::class.javaPrimitiveType!!,
			List::class.java,
			String::class.java,
			String::class.java,
			Boolean::class.javaPrimitiveType!!,
			UtcDateTime::class.java,
			Long::class.javaPrimitiveType!!,
			Long::class.javaPrimitiveType!!,
			String::class.java,
			String::class.java,
			String::class.java,
			UtcDateTime::class.java
		) {
			it.args[9] = null

			// debugging
			//			if (!it.args.contains("a server av hash")) return@before
			//			it.args.forEach { Logger().info(":" + it?.toString() + ":") }
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
