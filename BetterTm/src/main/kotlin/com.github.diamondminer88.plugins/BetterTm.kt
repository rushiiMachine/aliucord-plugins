package com.github.diamondminer88.plugins

import android.content.Context
import android.os.Bundle
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.aliucord.widgets.BottomSheet
import com.discord.utilities.images.MGImages
import com.discord.views.CheckedSetting

const val BASE_URL = "https://raw.githubusercontent.com/DiamondMiner88/aliucord-plugins/master/BetterTm/"

const val TM_EMOJI_URI = "res:///2131823862"
const val TM_URL = BASE_URL + "tm.png"
const val TM_ORIGINAL_URL = BASE_URL + "tm_original.png"

const val R_EMOJI_URI = "res:///2131824085"
const val R_URL = BASE_URL + "r.png"
const val R_ORIGINAL_URL = BASE_URL + "r_original.png"

const val C_EMOJI_URI = "res:///2131824084"
const val C_URL = BASE_URL + "c.png"
const val C_ORIGINAL_URL = BASE_URL + "c_original.png"

const val USE_ORIGINAL_KEY = "useOriginal"

@Suppress("unused")
@AliucordPlugin(requiresRestart = true)
class BetterTm : Plugin() {
	private infix fun String.orOriginal(originalUrl: String) =
		if (settings.getBool(USE_ORIGINAL_KEY, false)) originalUrl else this

	init {
		settingsTab = SettingsTab(
			BetterTmSettings::class.java,
			SettingsTab.Type.BOTTOM_SHEET
		).withArgs(settings)
	}

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
				TM_EMOJI_URI -> TM_URL orOriginal TM_ORIGINAL_URL
				R_EMOJI_URI -> R_URL orOriginal R_ORIGINAL_URL
				C_EMOJI_URI -> C_URL orOriginal C_ORIGINAL_URL
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

class BetterTmSettings(private val settings: SettingsAPI) : BottomSheet() {
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val ctx = view.context

		addView(
			Utils.createCheckedSetting(
				ctx, CheckedSetting.ViewType.SWITCH,
				"Originals",
				"Use original emojis made white instead of replacements."
			).apply {
				isChecked = settings.getBool(USE_ORIGINAL_KEY, false)
				setOnCheckedListener {
					settings.setBool(USE_ORIGINAL_KEY, it)
					Utils.promptRestart()
				}
			})
	}
}
