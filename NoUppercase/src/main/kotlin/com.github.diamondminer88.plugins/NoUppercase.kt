package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.ContentLoadingProgressBar
import androidx.core.widget.NestedScrollView
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.databinding.*
import com.discord.utilities.view.text.LinkifiedTextView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.roles.RoleIconView
import com.discord.widgets.roles.RolesListView
import com.discord.widgets.settings.WidgetSettings
import com.discord.widgets.stage.usersheet.UserProfileStageActionsView
import com.discord.widgets.user.profile.UserProfileAdminView
import com.discord.widgets.user.profile.UserProfileConnectionsView
import com.discord.widgets.user.profile.UserProfileHeaderView
import com.discord.widgets.user.usersheet.UserProfileVoiceSettingsView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

@Suppress("unused")
@SuppressLint("PrivateApi")
@AliucordPlugin
class NoUppercase : Plugin() {
    private val logger = Logger(this::class.simpleName)

    override fun start(ctx: Context) {
        // !!!!!!!!!! Before you yell at me for the following patches, these already work = no reason to change them !!!!!!!!!!
        // TODO: Patch TextView directly
        // *although my futile attempts did not work*

        // guild member list role headers
        patcher.patch(
            WidgetChannelMembersListItemHeaderBinding::class.java.getDeclaredConstructor(
                LinearLayout::class.java,
                RoleIconView::class.java,
                TextView::class.java
            ), Hook {
                val thisObj = it.thisObject as WidgetChannelMembersListItemHeaderBinding
                thisObj.c.isAllCaps = false
            })

        // channel list categories
        patcher.patch(
            WidgetChannelsListItemCategoryBinding::class.java.getDeclaredConstructor(
                LinearLayout::class.java,
                ImageView::class.java,
                ImageView::class.java,
                TextView::class.java
            ), Hook {
                val thisObj = it.thisObject as WidgetChannelsListItemCategoryBinding
                thisObj.d.isAllCaps = false
            })

        // user profile
        patcher.patch(
            WidgetUserSheetBinding::class.java.getDeclaredConstructor(
                NestedScrollView::class.java,
                CardView::class.java,
                TextView::class.java,
                LinkifiedTextView::class.java,
                ContentLoadingProgressBar::class.java,
                FrameLayout::class.java,
                Button::class.java,
                CardView::class.java,
                UserProfileAdminView::class.java,
                Button::class.java,
                TextView::class.java,
                UserProfileConnectionsView::class.java,
                LinearLayout::class.java,
                TextView::class.java,
                TextView::class.java,
                MaterialButton::class.java,
                MaterialButton::class.java,
                LinearLayout::class.java,
                LinearLayout::class.java,
                TextView::class.java,
                TextView::class.java,
                FrameLayout::class.java,
                Button::class.java,
                ImageView::class.java,
                TextView::class.java,
                TextInputEditText::class.java,
                TextInputLayout::class.java,
                Button::class.java,
                LinearLayout::class.java,
                View::class.java,
                MaterialButton::class.java,
                FlexboxLayout::class.java,
                UserProfileHeaderView::class.java,
                MaterialButton::class.java,
                CardView::class.java,
                UserProfileStageActionsView::class.java,
                TextView::class.java,
                UserProfileVoiceSettingsView::class.java,
                RolesListView::class.java,
                Button::class.java,
                CardView::class.java
            ), Hook {
                val thisObj = it.thisObject as WidgetUserSheetBinding
                thisObj.c.isAllCaps = false // about me
                thisObj.j.isAllCaps = false // connections
                thisObj.w.isAllCaps = false // note
                thisObj.m.isAllCaps = false // developer mode
            }
        )

        // friends list online/offline headers
        patcher.patch(
            WidgetFriendsListAdapterItemHeaderBinding::class.java.getConstructor(
                FrameLayout::class.java,
                TextView::class.java
            ), Hook {
                val thisObj = it.thisObject as WidgetFriendsListAdapterItemHeaderBinding
                thisObj.b.isAllCaps = false
            })
        // friends list pending headers
        patcher.patch(
            WidgetFriendsListExpandableHeaderBinding::class.java.getConstructor(
                FrameLayout::class.java,
                TextView::class.java,
                TextView::class.java
            ), Hook {
                val thisObj = it.thisObject as WidgetFriendsListExpandableHeaderBinding
                thisObj.c.isAllCaps = false
            })

        // user settings
        // this targets all CoordinatorLayout (root) -> NestedScrollView -> ((LinearLayout -> TextView(1)) / TextView)
        patcher.patch(
            WidgetSettings::class.java.getDeclaredMethod("onViewBound", View::class.java),
            Hook {
                val rootView = it.args[0] as CoordinatorLayout
                val view =
                    (rootView.getChildAt(1) as NestedScrollView).getChildAt(0) as LinearLayoutCompat
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    var textView: TextView? = null

                    if (child is TextView)
                        textView = child
                    else if (child is LinearLayout) {
                        val el = child.getChildAt(1) // 0th is a divider followed by the title
                        if (el is TextView) textView = el
                    } else continue
                    textView?.isAllCaps = false
                }
            })

        // doesn't work
//        patcher.patch(TextView::class.java.getDeclaredMethod("setAllCaps", Boolean::class.javaPrimitiveType), PreHook {
//            logger.info("here")
//            it.args[0] = false
//        })

        // doesn't work
//        patcher.patch(
//            TextView::class.java.getDeclaredMethod(
//                "setTransformationMethod",
//                TransformationMethod::class.java
//            ), Hook {
//                logger.info((it.thisObject as TextView).text.toString())
//                if (it.args[0] is AllCapsTransformationMethod) it.args[0] = null
//            })

//        val textAppearanceAttributesClass =
//            Class.forName("android.widget.TextView\$TextAppearanceAttributes")

        // doesn't work
//        patcher.patch(
//            TextView::class.java.getDeclaredMethod(
//                "readTextAppearance",
//                Context::class.java,
//                TypedArray::class.java,
//                textAppearanceAttributesClass,
//                Boolean::class.javaPrimitiveType
//            ), Hook {
////                val thisObj = it.thisObject as TextView
//                ReflectUtils.setField(TextView::class.java, it.thisObject, "mAllCaps", false)
//            }
//        )

        // doesn't work
//        patcher.patch(textAppearanceAttributesClass.constructors[0], Hook {
//            ReflectUtils.setField(textAppearanceAttributesClass, it.thisObject, "mAllCaps", false)
//        })

        // can't find the private method
//        patcher.patch(
//            TextView::class.java.declaredMethods.first { it.name == "readTextAppearance" },
//            Hook {
////            ReflectUtils.setField(textAppearanceAttributesClass, it.thisObject, "mAllCaps", false)
//                logger.info("here")
//
//            })

        // readTextAppearance isn't showing
//        TextView::class.java.declaredMethods.forEach {
//            if (it.toString().contains("readTextAppearance"))
//                logger.info(it.toString())
//        }

        // fails as well don't remember why
//        patcher.patch(
//            TextView::class.java.getDeclaredMethod(
//                "applyTextAppearance",
//                textAppearanceAttributesClass,
//            ), PreHook {
//                ReflectUtils.setField(textAppearanceAttributesClass, it.args[0], "mAllCaps", false)
//            }
//        )

//        patcher.patch(
//            TextView::class.java.getDeclaredConstructor(
//                Context::class.java,
//                AttributeSet::class.java,
//                Int::class.javaPrimitiveType
//            ), Hook {
//                val attr = it.args[1] as AttributeSet
//                logger.info(attr.styleAttribute.toString())
//            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
