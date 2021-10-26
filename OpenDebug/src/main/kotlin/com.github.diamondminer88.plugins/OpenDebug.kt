package com.github.diamondminer88.plugins

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.widgets.debugging.WidgetDebugging
import com.lytefast.flexinput.fragment.FlexInputFragment
import com.lytefast.flexinput.fragment.`FlexInputFragment$d`

@Suppress("unused")
@AliucordPlugin
class OpenDebug : Plugin() {
    private val logger = Logger(this::class.simpleName)
    private val pkgName = this.javaClass.`package`?.name

    init {
        needsResources = true
    }

    private fun openDebug(ctx: Context) =
        Utils.openPage(
            ctx,
            WidgetDebugging::class.java,
            Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    override fun start(ctx: Context) {
        var debugIcon = (ResourcesCompat.getDrawable(
            resources,
            resources.getIdentifier("debug_icon", "drawable", pkgName),
            null
        ) ?: throw Error("Failed to load debug icon")) as BitmapDrawable
        debugIcon = BitmapDrawable(
            resources,
            Bitmap.createScaledBitmap(debugIcon.bitmap, 24, 24, true)
        )

        val disableIcon = ResourcesCompat.getDrawable(
            resources,
            resources.getIdentifier("disable_icon", "drawable", pkgName),
            null
        ) ?: throw Error("Failed to load disable icon")

        patcher.patch(
            `FlexInputFragment$d`::class.java,
            "invoke",
            arrayOf<Class<*>>(Any::class.java),
            Hook {
                val frag = (it.thisObject as `FlexInputFragment$d`).receiver as FlexInputFragment
                val binding = frag.j() ?: return@Hook
                binding.h.visibility = View.GONE // blank arrow
                binding.m.visibility = View.VISIBLE // gift button
                binding.l.visibility = View.VISIBLE // gallery

                // we replace the gift button icon with our own & click handler
                binding.m.setImageDrawable(debugIcon)
                binding.m.foreground = disableIcon
                binding.m.foreground.alpha = if (settings.getBool("disabled", false)) 120 else 0

                binding.m.setOnClickListener { v ->
                    openDebug(v.context)
                }
                binding.m.setOnLongClickListener {
                    val disabled = !settings.getBool("disabled", false)
                    settings.setBool("disabled", disabled)
                    binding.m.foreground.alpha = if (disabled) 120 else 0
                    Utils.showToast("${if (disabled) "Disabled" else "Enabled"} auto debug log")
                    return@setOnLongClickListener true
                }
            })

        if (!settings.getBool("disabled", false))
            openDebug(ctx)
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}