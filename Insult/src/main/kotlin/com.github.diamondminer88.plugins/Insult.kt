package com.github.diamondminer88.plugins

import android.content.Context
import android.widget.Toast
import com.aliucord.Constants
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import java.io.File

@Suppress("unused")
@AliucordPlugin
@Deprecated("This plugin is deprecated")
class Insult : Plugin() {
	override fun start(ctx: Context) {
		Toast.makeText(
			ctx,
			"The Insult plugin no longer functional. It has been automatically removed.",
			Toast.LENGTH_LONG
		).show()

		File(Constants.PLUGINS_PATH, "Insult.zip").delete()
	}

	override fun stop(context: Context) {}
}
