package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.models.commands.ApplicationCommand
import com.discord.stores.BuiltInCommands

@Suppress("unused", "UNCHECKED_CAST")
@AliucordPlugin(requiresRestart = true)
class NoBuiltInCommands : Plugin() {
    private val blockedCommands = listOf("shrug", "tableflip", "unflip", "me", "spoiler")

    override fun start(ctx: Context) {
        patcher.after<BuiltInCommands>("getBuiltInCommands") {
            val result = (it.result as List<ApplicationCommand>)
            it.result = result.filter { cmd -> !blockedCommands.contains(cmd.name) }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
