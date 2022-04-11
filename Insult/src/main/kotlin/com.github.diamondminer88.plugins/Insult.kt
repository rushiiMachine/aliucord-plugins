package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Http
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin

@Suppress("unused")
@AliucordPlugin
class Insult : Plugin() {
	override fun start(ctx: Context) {
		commands.registerCommand(
			"insult",
			"Send an insult",
		) {
			CommandResult(Http.simpleGet("https://insult.mattbas.org/api/insult"))
		}
	}

	override fun stop(context: Context) {
		commands.unregisterAll()
	}
}
