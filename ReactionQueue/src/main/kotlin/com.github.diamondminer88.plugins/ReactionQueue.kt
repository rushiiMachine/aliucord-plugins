package com.github.diamondminer88.plugins

import android.content.Context
import android.os.Bundle
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.patcher.instead
import com.aliucord.settings.delegate
import com.aliucord.utils.RxUtils.await
import com.aliucord.widgets.BottomSheet
import com.discord.models.domain.emoji.Emoji
import com.discord.utilities.rest.RestAPI
import com.discord.views.CheckedSetting
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import rx.Observable
import java.util.concurrent.Executors

@Suppress("unused")
@AliucordPlugin
class ReactionQueue : Plugin() {
	private val queueWorker = Executors.newSingleThreadExecutor()
	var closeMessageMenu by settings.delegate(true)

	init {
		settingsTab = SettingsTab(
			ReactionQueueSettings::class.java,
			SettingsTab.Type.BOTTOM_SHEET
		).withArgs(this)
	}

	private fun handleHook(param: XC_MethodHook.MethodHookParam) {
		queueWorker.execute {
			val result = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
			(result as Observable<*>).await()
			Thread.sleep(500)
		}
	}

	override fun start(ctx: Context) {
		val longPrimitiveType = Long::class.javaPrimitiveType!!
		val stringType = String::class.java

		patcher.instead<RestAPI>(
			"addReaction",
			longPrimitiveType,
			longPrimitiveType,
			stringType,
		) {
			handleHook(it)
			Observable<Void> {}
		}

		patcher.instead<RestAPI>(
			"removeReaction",
			longPrimitiveType,
			longPrimitiveType,
			stringType,
			longPrimitiveType,
		) {
			handleHook(it)
			Observable<Void> {}
		}

		patcher.instead<RestAPI>(
			"removeSelfReaction",
			longPrimitiveType,
			longPrimitiveType,
			stringType,
		) {
			handleHook(it)
			Observable<Void> {}
		}

		patcher.before<WidgetChatListActions>(
			"addReaction",
			Emoji::class.java,
		) {
			if (closeMessageMenu)
				dismiss()
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

class ReactionQueueSettings(private val plugin: ReactionQueue) : BottomSheet() {
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		addView(
			Utils.createCheckedSetting(
				view.context, CheckedSetting.ViewType.SWITCH,
				"Close message menu",
				"Close the message context menu after adding a reaction."
			).apply {
				isChecked = plugin.closeMessageMenu
				setOnCheckedListener {
					plugin.closeMessageMenu = it
				}
			})
	}
}
