package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.settings.Plugins
import com.aliucord.settings.delegate
import com.aliucord.views.TextInput
import com.aliucord.widgets.BottomSheet
import com.lytefast.flexinput.R

@Suppress("unused", "DiscouragedPrivateApi")
@AliucordPlugin(requiresRestart = true)
class ReplaceAllText : Plugin() {
    private var firstLaunch: Boolean by settings.delegate(true)
    var selectedText: String by settings.delegate("uwu")

    init {
        settingsTab = SettingsTab(
            ReplaceAllTextSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun load(context: Context) {
        patcher.after<Plugins.Adapter>(
            "onBindViewHolder",
            Plugins.Adapter.ViewHolder::class.java,
            Int::class.javaPrimitiveType!!
        ) {
            val holder = it.args[0] as Plugins.Adapter.ViewHolder
            logger.info("'${holder.card.titleView.text}'")

            if (holder.card.titleView.text.startsWith("ReplaceAllText"))
                holder.card.settingsButton.isEnabled = true
        }

        val fPluginsData = Plugins.Adapter::class.java
            .getDeclaredField("data")
            .apply { isAccessible = true }

        patcher.after<Plugins.Adapter>(
            "onToggleClick",
            Plugins.Adapter.ViewHolder::class.java,
            Boolean::class.javaPrimitiveType!!,
            Integer.TYPE
        ) {
            val data = fPluginsData.get(this) as List<Plugin>
            val plugin = data[it.args[2] as Int]

            logger.info(plugin.manifest.name)
            if (plugin.manifest.name != "ReplaceAllText")
                return@after

            val holder = (it.args[0] as Plugins.Adapter.ViewHolder)
            holder.card.settingsButton.isEnabled = true
        }
    }

    var textViewPatch: Runnable? = null
    override fun start(ctx: Context) {
        if (firstLaunch) {
            firstLaunch = false
            return
        }

        textViewPatch = patcher.before<TextView>(
            "setText",
            CharSequence::class.java,
            TextView.BufferType::class.java,
            Boolean::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!
        ) {
            if (this is EditText)
                return@before
            it.args[0] = selectedText
        }
    }

    override fun stop(context: Context) {
        textViewPatch?.run()
    }
}

@SuppressLint("SetTextI18n")
class ReplaceAllTextSettings(private val plugin: ReplaceAllText) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context

        val textInput = TextInput(ctx).apply {
            editText.setText(plugin.selectedText)
            editText.maxLines = 1
            editText.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {
                    plugin.selectedText = s.toString()
                    Utils.promptRestart()
                }
            })
        }

        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
            text = "Replacement text"
        })

        addView(textInput)
    }
}
