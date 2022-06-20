package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.instead
import com.aliucord.utils.RxUtils.await
import com.discord.utilities.rest.RestAPI
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import rx.Observable
import java.lang.reflect.Member
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
@AliucordPlugin
class ReactionQueue : Plugin() {
	private val events = LinkedList<Triple<Member, Any, Array<Any>>>()
	private var isHandlingEvents = AtomicBoolean(false)

	private fun recordEvent(param: XC_MethodHook.MethodHookParam) {
		events.addLast(Triple(
			param.method,
			param.thisObject,
			param.args,
		))
		startHandling()
	}

	override fun start(ctx: Context) {
		patcher.instead<RestAPI>(
			"addReaction",
			Long::class.javaPrimitiveType!!,
			Long::class.javaPrimitiveType!!,
			String::class.java,
		) {
			recordEvent(it)
			Observable<Void> {}
		}

		patcher.instead<RestAPI>(
			"removeReaction",
			Long::class.javaPrimitiveType!!,
			Long::class.javaPrimitiveType!!,
			String::class.java,
			Long::class.javaPrimitiveType!!,
		) {
			recordEvent(it)
			Observable<Void> {}
		}

		patcher.instead<RestAPI>(
			"removeSelfReaction",
			Long::class.javaPrimitiveType!!,
			Long::class.javaPrimitiveType!!,
			String::class.java,
		) {
			recordEvent(it)
			Observable<Void> {}
		}
	}

	private fun startHandling() {
		if (!isHandlingEvents.compareAndSet(false, true))
			return

		Utils.threadPool.execute {
			while (events.isNotEmpty()) {
				val (method, thisObject, args) = events[0]
				(XposedBridge.invokeOriginalMethod(method, thisObject, args) as Observable<*>).await()

				events.removeFirst()
				Thread.sleep(500)
			}

			isHandlingEvents.set(false)
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
