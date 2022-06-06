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
import com.aliucord.patcher.after
import com.aliucord.patcher.instead
import com.aliucord.settings.delegate
import com.aliucord.widgets.BottomSheet
import com.discord.models.user.User
import com.discord.utilities.user.UserUtils
import com.discord.views.CheckedSetting.ViewType.SWITCH
import com.discord.widgets.channels.*
import com.discord.widgets.settings.account.*
import com.discord.widgets.user.profile.UserProfileConnectionsView
import com.discord.widgets.user.profile.UserProfileConnectionsView.ConnectedAccountItem
import com.discord.widgets.user.profile.UserProfileHeaderViewModel

@Suppress("unused")
@SuppressLint("SetTextI18n", "DiscouragedPrivateApi", "PrivateApi")
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
	private val connectionDialogHeader = Utils.getResId("connected_account_actions_dialog_header", "id")
	private val dmChannelAKAs = Utils.getResId("channel_aka", "id")
	private val numRegex = Regex("\\d")
	private val fShowAkas =
		UserProfileHeaderViewModel.ViewState.Loaded::class.java.getDeclaredField("showAkas")
			.apply { isAccessible = true }

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

	private val mGetListenerInfo = View::class.java.getDeclaredMethod("getListenerInfo")
		.apply { isAccessible = true }
	private val fOnClickListener = Class.forName("android.view.View\$ListenerInfo")
		.getDeclaredField("mOnClickListener")

	private fun getOnClickListener(view: View): View.OnClickListener =
		fOnClickListener.get(mGetListenerInfo.invoke(view)) as View.OnClickListener

	private val hideAKAs: Boolean by settings.delegate(true)
	private val hideConnections: Boolean by settings.delegate(true)
	private val hideDiscriminators: Boolean by settings.delegate(true)
	private val hidePersonalDetails: Boolean by settings.delegate(true)
	private val showWarning: Boolean by settings.delegate(true)

	override fun start(ctx: Context) {
		// dm sidebar AKAs
		patcher.after<WidgetChannelTopic>("configureUI", WidgetChannelTopicViewModel.ViewState::class.java) {
			if (!hideAKAs) return@after
			requireView().findViewById<UserAkaView>(dmChannelAKAs).visibility = View.GONE
		}

		// user profile AKAs
		patcher.instead<UserProfileHeaderViewModel.ViewState.Loaded>("getShowAkas") {
			if (hideAKAs) false
			else fShowAkas.get(this)
		}

		// profile connections
		patcher.after<UserProfileConnectionsView.ViewHolder>(
			"onConfigure",
			Integer.TYPE,
			ConnectedAccountItem::class.java
		) {
			if (!hideConnections) return@after
			val textView = this.itemView as TextView
			textView.text = "x".repeat(textView.length())
		}

		// connection popup
		patcher.after<b.a.a.i>("onViewBound", View::class.java) {
			if (!hideConnections) return@after
			val header = requireView().findViewById<TextView>(connectionDialogHeader)
			header.text = "x".repeat(header.length())
		}

		// user discriminators
		patcher.instead<UserUtils>(
			"getDiscriminatorWithPadding",
			User::class.java
		) {
			if (hideDiscriminators) ""
			else UserUtils.INSTANCE.padDiscriminator((it.args[0] as User).discriminator)
		}

		// user account settings
		patcher.after<WidgetSettingsAccount>(
			"configureUI",
			WidgetSettingsAccount.Model::class.java
		) {
			val view = requireView()

			if (hidePersonalDetails) {
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

			if (showWarning) {
				val usernameContainer = view.findViewById<LinearLayout>(settingsUsernameContainer)
				configureContainer(usernameContainer, `WidgetSettingsAccount$configureUI$2`())

				val nameContainer = view.findViewById<LinearLayout>(settingsNameContainer)
				configureContainer(nameContainer, `WidgetSettingsAccount$configureUI$3`())

				val emailContainer = view.findViewById<LinearLayout>(settingsEmailContainer)
				configureContainer(emailContainer, getOnClickListener(emailContainer))

				val phoneContainer = view.findViewById<LinearLayout>(settingsPhoneContainer)
				configureContainer(phoneContainer, `WidgetSettingsAccount$configureUI$5`())
			}
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}

class StreamerModeSettings(settings: SettingsAPI) : BottomSheet() {
	private var hideAKAs: Boolean by settings.delegate(true)
	private var hideConnections: Boolean by settings.delegate(true)
	private var hideDiscriminators: Boolean by settings.delegate(true)
	private var hidePersonalDetails: Boolean by settings.delegate(true)
	private var showWarning: Boolean by settings.delegate(true)

	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val ctx = view.context

		addView(
			createCheckedSetting(
				ctx, SWITCH,
				"Discriminators",
				"Hide the #0000 part of user tags for everyone"
			).apply {
				isChecked = hideDiscriminators
				setOnCheckedListener { hideDiscriminators = it }
			})

		addView(createCheckedSetting(
			ctx, SWITCH,
			"Personal details",
			"Censor personal details on the 'My Account' settings page"
		).apply {
			isChecked = hidePersonalDetails
			setOnCheckedListener { hidePersonalDetails = it }
		})

		addView(createCheckedSetting(
			ctx, SWITCH,
			"Warnings",
			"Show warnings when clicking on pages with identifiable content"
		).apply {
			isChecked = showWarning
			setOnCheckedListener { showWarning = it }
		})

		addView(createCheckedSetting(
			ctx, SWITCH,
			"AKAs",
			"Hide user AKAs from profile/dms"
		).apply {
			isChecked = hideAKAs
			setOnCheckedListener { hideAKAs = it }
		})

		addView(createCheckedSetting(
			ctx, SWITCH,
			"Connections",
			"Hide user account connections"
		).apply {
			isChecked = hideConnections
			setOnCheckedListener { hideConnections = it }
		})
	}
}

// ---------- patches for future reference ----------

//private val profileHeaderPrimaryUsername = Utils.getResId("username_text", "id")
//private val profileHeaderSecondaryUsername =
//	Utils.getResId("user_profile_header_secondary_name", "id")
//private val userActionsDialogUserName = Utils.getResId("user_actions_dialog_user_name", "id")

//		patcher.patch(
//			c.a.a.b.a.b::class.java.getDeclaredMethod("onViewBound", View::class.java),
//			Hook {
//				val username = (it.args[0] as View).findViewById<TextView>(userActionsDialogUserName)
//				username.text = username.text.dropLast(5)
//			})

//		patcher.patch(
//			UserProfileHeaderView::class.java.getDeclaredMethod(
//				"updateViewState",
//				UserProfileHeaderViewModel.ViewState.Loaded::class.java
//			), Hook {
//				val obj = it.thisObject as UserProfileHeaderView
//				val secondary =
//					obj.findViewById<com.discord.utilities.view.text.SimpleDraweeSpanTextView>(
//						profileHeaderSecondaryUsername
//					)
//
//				if (secondary.length() == 0) {
//					// nickname not present, adjust primary
//					val primary =
//						obj.findViewById<com.facebook.drawee.span.SimpleDraweeSpanTextView>(
//							profileHeaderPrimaryUsername
//						)
//					val len = primary.i.length
//					primary.i.delete(len - 5, len)
//					primary.setDraweeSpanStringBuilder(primary.i)
//				} else {
//					// nickname present, adjust secondary
//					secondary.text = secondary.text.dropLast(5)
//				}
//			})
