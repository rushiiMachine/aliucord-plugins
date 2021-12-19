package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.lytefast.flexinput.fragment.FlexInputFragment
import com.lytefast.flexinput.fragment.`FlexInputFragment$a`

@Suppress("unused")
@SuppressLint("ClickableViewAccessibility")
@AliucordPlugin
class Gestures : Plugin() {
    override fun start(ignored: Context) {
        val clipboard = Utils.appActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        patcher.after<FlexInputFragment>("onViewCreated", View::class.java, Bundle::class.java) {
            val editText = this.l()
            val prevListener = `FlexInputFragment$a`(0, this)
            editText.setOnTouchListener(object : OnSwipeListener(editText.context) {
                override fun onSwipeRight() {
                    editText.setText("")
                    prevListener.onClick(editText.rootView)
                }

                override fun onSwipeLeft() {
                    if (!clipboard.hasText()) return
                    editText.append(clipboard.text)
                }

                override fun onClick() {
                    prevListener.onClick(editText.rootView)
                }
            })
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
