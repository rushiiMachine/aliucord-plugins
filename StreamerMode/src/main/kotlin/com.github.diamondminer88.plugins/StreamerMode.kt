package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.Utils.createCheckedSetting
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.ConfirmDialog
import com.aliucord.patcher.Hook
import com.aliucord.patcher.InsteadHook
import com.aliucord.widgets.BottomSheet
import com.discord.models.user.User
import com.discord.utilities.user.UserUtils
import com.discord.views.CheckedSetting.ViewType.SWITCH
import com.discord.widgets.settings.account.*

private const val SETTING_DISCRIM_KEY = "hideDiscriminators"
private const val SETTING_PERSONAL_DETAILS_KEY = "hidePersonalDetails"
private const val SETTING_SHOW_WARNING = "showWarning"

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class StreamerMode : Plugin() {
    private val settingsNameText = Utils.getResId("settings_account_name_text", "id")
    private val settingsEmailText = Utils.getResId("settings_account_email_text", "id")
    private val settingsPhoneText = Utils.getResId("settings_account_phone_text", "id")
    private val settingsSMSText = Utils.getResId("settings_account_sms_phone", "id")
    private val settingsUsernameContainer = Utils.getResId("settings_account_tag_container", "id")
    private val settingsNameContainer = Utils.getResId("settings_account_name_container", "id")
    private val settingsEmailContainer = Utils.getResId("settings_account_email_container", "id")
    private val settingsPhoneContainer = Utils.getResId("settings_account_phone_container", "id")
    private val numRegex = Regex("\\d")

    init {
        settingsTab = SettingsTab(
            StreamerModeSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(settings)
    }

    private fun configureContainer(layout: LinearLayout, onClick: View.OnClickListener) {
        layout.setOnClickListener { v ->
            layout.setOnClickListener(onClick)

            val dialog = ConfirmDialog()
                .setTitle("Warning")
                .setIsDangerous(true)
                .setDescription("This page may contain identifiable content! Do you wish to proceed?")
            dialog.setOnOkListener {
                dialog.dismiss()
                onClick.onClick(v)
            }
            dialog.show(Utils.appActivity.supportFragmentManager, "Warning")
        }
    }

    override fun start(ctx: Context) {
        patcher.patch(
            UserUtils::class.java.getDeclaredMethod(
                "getDiscriminatorWithPadding",
                User::class.java
            ), InsteadHook {
                if (settings.getBool(SETTING_DISCRIM_KEY, true))
                    ""
                else
                    UserUtils.INSTANCE.padDiscriminator((it.args[0] as User).discriminator)
            })

        patcher.patch(
            WidgetSettingsAccount::class.java.getDeclaredMethod(
                "configureUI",
                WidgetSettingsAccount.Model::class.java
            ), Hook {
                val view = (it.thisObject as WidgetSettingsAccount).requireView()

                if (settings.getBool(SETTING_PERSONAL_DETAILS_KEY, true)) {
                    val name = view.findViewById<TextView>(settingsNameText)
                    name.text = "x".repeat(name.length())

                    val email = view.findViewById<TextView>(settingsEmailText)
                    val split = email.text.split("@")
                    email.text = "x".repeat(split[0].length) + "@" + split[1]

                    val phone = view.findViewById<TextView>(settingsPhoneText)
                    phone.text = phone.text.replace(numRegex, "x")

                    val sms = view.findViewById<TextView>(settingsSMSText)
                    sms.visibility = View.GONE
                }

                if (settings.getBool(SETTING_SHOW_WARNING, true)) {
                    val usernameContainer = view.findViewById<LinearLayout>(settingsUsernameContainer)
                    configureContainer(usernameContainer, (`WidgetSettingsAccount$configureUI$2`()))

                    val nameContainer = view.findViewById<LinearLayout>(settingsNameContainer)
                    configureContainer(nameContainer, (`WidgetSettingsAccount$configureUI$3`()))

                    val emailContainer = view.findViewById<LinearLayout>(settingsEmailContainer)
                    configureContainer(emailContainer, (`WidgetSettingsAccount$configureUI$4`()))

                    val phoneContainer = view.findViewById<LinearLayout>(settingsPhoneContainer)
                    configureContainer(phoneContainer, (`WidgetSettingsAccount$configureUI$5`()))
                }
            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

class StreamerModeSettings(private val settings: SettingsAPI) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context

        addView(
            createCheckedSetting(
                ctx, SWITCH,
                "Discriminators",
                "Hide the #0000 part of user tags for everyone"
            ).apply {
                isChecked = settings.getBool(SETTING_DISCRIM_KEY, true)
                setOnCheckedListener {
                    settings.setBool(SETTING_DISCRIM_KEY, it)
                }
            })

        addView(createCheckedSetting(
            ctx, SWITCH,
            "Personal details",
            "Censor personal details on the 'My Account' settings page"
        ).apply {
            isChecked = settings.getBool(SETTING_PERSONAL_DETAILS_KEY, true)
            setOnCheckedListener {
                settings.setBool(SETTING_PERSONAL_DETAILS_KEY, it)
            }
        })

        addView(createCheckedSetting(
            ctx, SWITCH,
            "Warnings",
            "Show warnings when clicking on pages with identifiable content"
        ).apply {
            isChecked = settings.getBool(SETTING_SHOW_WARNING, true)
            setOnCheckedListener {
                settings.setBool(SETTING_SHOW_WARNING, it)
            }
        })
    }
}

// ---------- future reference patches ----------

//private val profileHeaderPrimaryUsername = Utils.getResId("username_text", "id")
//private val profileHeaderSecondaryUsername =
//    Utils.getResId("user_profile_header_secondary_name", "id")
//private val userActionsDialogUserName = Utils.getResId("user_actions_dialog_user_name", "id")

//        patcher.patch(
//            c.a.a.b.a.b::class.java.getDeclaredMethod("onViewBound", View::class.java),
//            Hook {
//                val username = (it.args[0] as View).findViewById<TextView>(userActionsDialogUserName)
//                username.text = username.text.dropLast(5)
//            })

//        patcher.patch(
//            UserProfileHeaderView::class.java.getDeclaredMethod(
//                "updateViewState",
//                UserProfileHeaderViewModel.ViewState.Loaded::class.java
//            ), Hook {
//                val obj = it.thisObject as UserProfileHeaderView
//                val secondary =
//                    obj.findViewById<com.discord.utilities.view.text.SimpleDraweeSpanTextView>(
//                        profileHeaderSecondaryUsername
//                    )
//
//                if (secondary.length() == 0) {
//                    // nickname not present, adjust primary
//                    val primary =
//                        obj.findViewById<com.facebook.drawee.span.SimpleDraweeSpanTextView>(
//                            profileHeaderPrimaryUsername
//                        )
//                    val len = primary.i.length
//                    primary.i.delete(len - 5, len)
//                    primary.setDraweeSpanStringBuilder(primary.i)
//                } else {
//                    // nickname present, adjust secondary
//                    secondary.text = secondary.text.dropLast(5)
//                }
//            })
