package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin

@Suppress("unused")
@AliucordPlugin
class Template : Plugin() {
    val logger = Logger("Template")

    override fun start(ctx: Context) {

    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}