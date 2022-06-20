package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.instead
import com.aliucord.utils.RxUtils.await
import com.discord.utilities.rest.RestAPI
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import rx.Observable
import java.util.concurrent.Executors

@Suppress("unused")
@AliucordPlugin
class ReactionQueue : Plugin() {
	private val queueWorker = Executors.newSingleThreadExecutor()

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
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
