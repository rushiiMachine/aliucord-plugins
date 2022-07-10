package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.PatcherAPI
import com.aliucord.coreplugins.CorePlugins
import com.aliucord.entities.Plugin
import com.aliucord.patcher.InsteadHook

@Suppress("unused")
@AliucordPlugin
class NoBadges : Plugin() {
	override fun start(ctx: Context) {
		val firstHookCls = Class.forName("com.aliucord.coreplugins.Badges\$\$ExternalSyntheticLambda5")
		patcher.patch(firstHookCls.getDeclaredMethod("call", Object::class.java), InsteadHook {})

		val corePlugins = CorePlugins::class.java.getDeclaredField("corePlugins")
			.apply { isAccessible = true }
			.get(null) as Array<*>
		val badgesPatcher = Plugin::class.java.getDeclaredField("patcher")
			.apply { isAccessible = true }
			.get(corePlugins[0]) as PatcherAPI
		badgesPatcher.unpatchAll()
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
