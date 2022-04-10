package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.widget.TextView
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after

//private const val TEXT_SIZE_KEY = "textSizeMultiplier"
//private const val DEFAULT_MULTIPLIER = 1f

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
@Suppress("unused")
@AliucordPlugin(requiresRestart = true)
class NoUppercase : Plugin() {
	//    init {
	//        settingsTab = SettingsTab(
	//            NoUppercaseSettings::class.java,
	//            SettingsTab.Type.BOTTOM_SHEET
	//        ).withArgs(settings)
	//    }

	override fun start(ctx: Context) {
		//        val headerIds = listOf(
		//            R.i.AppAlertDialogHeader,
		//            R.i.Feedback_SectionHeaderTextAppearance,
		//            R.i.FriendsList_ItemHeader_Title,
		//            R.i.Markdown_Header1,
		//            R.i.Stage_Section_HeaderTextAppearance,
		//            R.i.TooltipDefaultText,
		//            R.i.UiKit_Calls_ButtonCircle,
		//            R.i.UiKit_Chat_Embed_Header,
		//            R.i.UiKit_Chip_New,
		//            R.i.UiKit_Dialog_Title,
		//            R.i.UiKit_Search_Header,
		//            R.i.UiKit_Settings_Item_Header,
		//            R.i.UiKit_Tabs_TextAppearance,
		//            R.i.UiKit_TextAppearance_ListItem_Label,
		//            R.i.UiKit_TextView_H6,
		//            R.i.UiKit_TextView_NewBadge,
		//            R.i.UiKit_TextView_Tagged,
		//            R.i.UserProfile_Section_HeaderTextAppearance,
		//        )

		val cTextAppearanceAttributes = Class.forName("android.widget.TextView\$TextAppearanceAttributes")
		val fmAllCaps = cTextAppearanceAttributes.getDeclaredField("mAllCaps")
			.apply { isAccessible = true }

		patcher.after<TextView>(
			"readTextAppearance",
			Context::class.java,
			TypedArray::class.java,
			cTextAppearanceAttributes,
			java.lang.Boolean.TYPE
		) {
			fmAllCaps.set(it.args[2], false)
		}

		//        val multiplier = settings.getFloat(TEXT_SIZE_KEY, DEFAULT_MULTIPLIER)
		//        patcher.after<TextView>(Context::class.java, AttributeSet::class.java, Integer.TYPE, Integer.TYPE) {
		//            if (headerIds.contains(it.args[3]))
		//                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * multiplier)
		//        }
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

//@SuppressLint("SetTextI18n")
//class NoUppercaseSettings(private val settings: SettingsAPI) : BottomSheet() {
//    override fun onViewCreated(view: View, bundle: Bundle?) {
//        super.onViewCreated(view, bundle)
//        val ctx = view.context
//
//        val multiplier = settings.getFloat(TEXT_SIZE_KEY, DEFAULT_MULTIPLIER)
//
//        val currentSize = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
//            text = "${multiplier}x"
//            width = 43.dp
//        }
//
//        val slider = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
//            layoutParams = LinearLayout.LayoutParams(
//                MATCH_PARENT,
//                WRAP_CONTENT
//            )
//            max = 200
//            progress = (multiplier * 100).toInt()
//            setPadding(12.dp, 0, 12.dp, 0)
//            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onStartTrackingTouch(seekBar: SeekBar) {}
//
//                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                    currentSize.text = "${progress.div(100f)}x"
//                }
//
//                override fun onStopTrackingTouch(seekBar: SeekBar) {
//                    settings.setFloat(TEXT_SIZE_KEY, progress.div(100f))
//                    Utils.promptRestart()
//                }
//            })
//        }
//
//        val resetButton = Button(ctx).apply {
//            text = "Reset"
//            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
//                setMargins(12.dp, 0, 12.dp, 0)
//            }
//            setOnClickListener {
//                currentSize.text = "1.0x"
//                slider.progress = 100
//                settings.setFloat(TEXT_SIZE_KEY, DEFAULT_MULTIPLIER)
//                Utils.promptRestart()
//            }
//        }
//
//        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
//            text = "Header size (multiplier)"
//        })
//
//        addView(LinearLayout(ctx, null, 0, R.i.UiKit_Settings_Item).apply {
//            addView(currentSize)
//            addView(slider)
//        })
//
//        addView(resetButton)
//
//        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply {
//            text = "Changes will fully apply after reloading Discord"
//            textSize = 4.dp.toFloat()
//        })
//    }
//}
