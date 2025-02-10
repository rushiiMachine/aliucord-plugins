package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.Normalizer2
import android.os.Bundle
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.settings.delegate
import com.aliucord.utils.ReflectUtils
import com.aliucord.widgets.BottomSheet
import com.discord.api.guildmember.GuildMember
import com.discord.api.user.User
import com.discord.views.CheckedSetting

@Suppress("unused")
@AliucordPlugin(requiresRestart = true)
class NormalizeNames : Plugin() {
	private val accentRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

	var normalizeDiacritics: Boolean by settings.delegate(false)
	private lateinit var normalizer: Normalizer2

	init {
		settingsTab = SettingsTab(
			NormalizeNamesSettings::class.java,
			SettingsTab.Type.BOTTOM_SHEET
		).withArgs(this)
	}

	private fun normalizeString(str: String): String {
		var normalized = normalizer.normalize(str)
		if (normalizeDiacritics)
			normalized = normalized.replace(accentRegex, "")
		return normalized
	}

	@Suppress("UNCHECKED_CAST")
	override fun start(ctx: Context) {
		normalizer = if (normalizeDiacritics) {
			Normalizer2.getNFDInstance()!!
		} else {
			Normalizer2.getNFKCInstance()!!
		}

		patcher.after<User>("getUsername") {
			if (it.result != null)
				it.result = normalizeString(it.result as String)
		}

		patcher.after<GuildMember>("h") {
			if (it.result != null)
				it.result = normalizeString(it.result as String)
		}

		// Normalize user global names when they're added to a private cache by Aliucore core
		val originalGlobalNames = ReflectUtils.getField(Class.forName("com.aliucord.coreplugins.rn.PatchesKt"), null, "globalNames")
		val globalNamesInterceptor = object : HashMap<Long, String>() {
			override fun put(key: Long, value: String): String? {
				return super.put(key, normalizeString(value))
			}
		}
		ReflectUtils.setFinalField(Class.forName("com.aliucord.coreplugins.rn.PatchesKt"), null, "globalNames", globalNamesInterceptor)
		globalNamesInterceptor.putAll(originalGlobalNames as Map<Long, String>)
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

@SuppressLint("SetTextI18n")
class NormalizeNamesSettings(private val plugin: NormalizeNames) : BottomSheet() {
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val ctx = view.context

		addView(
			Utils.createCheckedSetting(
				ctx, CheckedSetting.ViewType.SWITCH,
				"Diacritics",
				"Remove diacritics/accents on characters."
			).apply {
				isChecked = plugin.normalizeDiacritics
				setOnCheckedListener {
					plugin.normalizeDiacritics = it
					Utils.promptRestart()
				}
			})
	}
}
