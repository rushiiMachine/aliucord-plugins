package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.*
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.filename
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.type
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.url
import com.discord.api.message.attachment.MessageAttachment
import com.discord.api.message.attachment.MessageAttachmentType
import com.discord.app.AppActivity
import com.discord.stores.StoreStream
import com.discord.utilities.textprocessing.MessageRenderContext
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class AudioPlayer : Plugin() {
	private val validFileExtensions = arrayOf("webm", "mp3", "aac", "m4a", "wav", "flac", "wma", "opus", "ogg")
	private val playerBarId = View.generateViewId()
	private val progressSliderId = View.generateViewId()
	private val playButtonId = View.generateViewId()
	private val progressTextId = View.generateViewId()

	private val attachmentCardId = Utils.getResId("chat_list_item_attachment_card", "id")
	private val attachmentCardIconId = Utils.getResId("chat_list_item_attachment_icon", "id")
	private val audioFileIconId = Utils.getResId("ic_file_audio", "drawable")

	private var playIcon: Drawable? = null // rotated triangle icon
	private var pauseIcon: Drawable? = null // two vertical bars icon
	private var rewindIcon: Drawable? = null

	private val durations = hashMapOf<String, Int>() // attachment URL -> duration
	private val playerProgress = hashMapOf<String, Int>() // attachment URL -> current progress
	private val players = hashMapOf<String, MediaPlayer>() // attachment URL -> media player
	private var pauseCurrentPlayer: (() -> Unit)? = null
	private var currentPlayingUrl: String? = null
	private var preparing = AtomicBoolean(false)

	private fun bindPlayerElements(url: String, playButton: ImageButton, progressText: TextView, progressSlider: SeekBar) {
		val duration = durations[url] ?: 0

		playButton.setOnClickListener {
			val player = players.computeIfAbsent(url) {
				MediaPlayer().apply {
					setDataSource(url)
				}
			}

			if (url == currentPlayingUrl) {
				Utils.mainThread.post { playButton.background = pauseIcon }
				currentPlayingUrl = null
				player
			}

			if (!preparing.compareAndSet(false, true)) {
				// FIXME: will always pause even if its rebinding the same player
				pauseCurrentPlayer?.invoke()
				pauseCurrentPlayer = null

				Utils.mainThread.post { playButton.visibility = View.INVISIBLE }

				player.apply {
					setOnPreparedListener {
						seekTo(playerProgress.getOrPut(url) { 0 })
						seekTo((sliderView.progress.div(500f) * duration).toInt())
						Utils.mainThread.post { updatePlaying() }
					}
					setOnCompletionListener {
						// TODO: stop timer
						pauseCurrentPlayer = null
						player.seekTo(0)
						Utils.mainThread.post { playButton.background = rewindIcon }
					}
					pauseCurrentPlayer = {
						// TODO: stop timer
						pause()
						Utils.mainThread.post { playButton.background = pauseIcon }
						// playerProgress[url] = 0
						pauseCurrentPlayer = null
					}
					prepare()
					preparing.set(true)
				}
			}
			// TODO: start timer here
		}

		progressSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onStartTrackingTouch(seekBar: SeekBar) {}
			override fun onStopTrackingTouch(seekBar: SeekBar) {}

			// var prevProgress = 0
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (!fromUser) return
				// if (preparing.get()) {
				// 	seekBar.progress = prevProgress
				// 	return
				// }
				// prevProgress = progress

				val newProgress = (progress.div(500f) * duration).toInt()
				players[url]?.seekTo(newProgress)
				playerProgress[url] = newProgress

				progressText.text = "${msToTime(newProgress)} / ${msToTime(duration)}"
			}
		})
	}

	override fun start(context: Context) {
		rewindIcon = ContextCompat.getDrawable(
			context,
			com.yalantis.ucrop.R.c.ucrop_rotate
		)
		rewindIcon = ContextCompat.getDrawable(
			context,
			com.google.android.exoplayer2.ui.R.b.exo_controls_play
		)
		playIcon = ContextCompat.getDrawable(
			context,
			com.google.android.exoplayer2.ui.R.b.exo_controls_pause
		)

		patcher.after<WidgetChatListAdapterItemAttachment>(
			"configureFileData",
			MessageAttachment::class.java,
			MessageRenderContext::class.java
		) {
			val attachment = it.args[0] as MessageAttachment

			if (attachment.type == MessageAttachmentType.IMAGE)
				return@after
			if (!validFileExtensions.any { ext -> attachment.filename.endsWith(ext) })
				return@after

			val root = WidgetChatListAdapterItemAttachment.`access$getBinding$p`(this)
				.root as ConstraintLayout
			val card = root.findViewById<MaterialCardView>(attachmentCardId)
			val ctx = root.context

			val duration = try {
				durations[attachment.url] ?: MediaMetadataRetriever().use { retriever ->
					// TODO: make async
					retriever.setDataSource(attachment.url, hashMapOf())
					retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
						?.toInt()
						?: 0
				}
			} catch (e: Throwable) {
				0
			}
			durations[attachment.url] = duration

			if (duration == 0) {
				val icon = card.findViewById<ImageView>(attachmentCardIconId)
				icon.setImageResource(audioFileIconId)
			}

			// Bind existing player var
			card.findViewById<LinearLayout>(playerBarId)?.apply {
				visibility = View.VISIBLE
				bindPlayerElements(
					attachment.url,
					findViewById(playButtonId),
					findViewById(progressTextId),
					findViewById(progressSliderId),
				)
				return@after
			}

			card.addView(LinearLayout(ctx, null, 0, R.i.UiKit_ViewGroup).apply {
				id = playerBarId
				if (duration == 0) {
					visibility = View.GONE
					return@apply
				}

				val p2 = DimenUtils.defaultPadding / 2
				setPadding(p2, p2, p2, p2)
				setOnClickListener {} // don't download attachment
				layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
					topMargin = 60.dp
					gravity = Gravity.BOTTOM
				}

				val buttonView = ImageButton(ctx).apply {
					id = playButtonId
					background = pauseIcon
					setPadding(p2, p2, p2, p2)
				}

				val progressView = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
					id = progressTextId
					text = "0:00 / " + msToTime(duration)
					setPadding(p2, p2, p2, p2)
				}

				val sliderView = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
					id = progressSliderId
					gravity = Gravity.CENTER
					progress = 0
					thumb = null
					max = 500
					layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
						.apply { weight = 0.5f }
					val p = 2.dp
					setPadding(p, p, p, 0)
				}

				bindPlayerElements(attachment.url, buttonView, progressView, sliderView)

				// 	var isPrepared = false
				// 	var preparing = false
				// 	var playing = false
				//
				// 	var timer: Timer? = null
				// 	fun scheduleUpdater() {
				// 		timer?.cancel()
				// 		timer = Timer()
				// 		timer!!.scheduleAtFixedRate(object : TimerTask() {
				// 			override fun run() {
				// 				if (!playing) return
				// 				Utils.mainThread.post {
				// 					progressView.text =
				// 						"${msToTime(currentPlayer!!.currentPosition.toLong())} / ${msToTime(duration)}"
				// 					sliderView.progress = (500 * currentPlayer!!.currentPosition / duration).toInt()
				// 				}
				// 			}
				// 		}, 2000, 250)
				// 	}
				//
				// 	fun updatePlaying() {
				// 		if (currentPlayer == null)
				// 			return
				//
				// 		if (playing) {
				// 			currentPlayer!!.start()
				// 			scheduleUpdater()
				// 			buttonView.background = playIcon
				// 		} else {
				// 			currentPlayer!!.pause()
				// 			timer?.cancel()
				// 			timer?.purge()
				// 			timer = null
				// 			buttonView.background = pauseIcon
				// 		}
				// 	}
				//

				addView(buttonView)
				addView(progressView)
				addView(sliderView)
			})
		}

		patcher.after<StoreStream>("handleChannelSelected", Long::class.javaPrimitiveType!!) {
			pauseCurrentPlayer?.invoke()
			players.forEach { it.value.release() }
			players.clear()
		}

		patcher.after<AppActivity>("onPause") {
			pauseCurrentPlayer?.invoke()
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
		pauseCurrentPlayer?.invoke()
		players.forEach { it.value.release() }
		players.clear()
		playerProgress.clear()
	}

	private fun msToTime(ms: Int): String {
		val hrs = ms / 3_600_000
		val mins = ms / 60000
		val secs = ms / 1000 % 60

		return if (hrs == 0)
			String.format("%d:%02d", mins, secs)
		else
			String.format("%d:%d:%02d", hrs, mins, secs)
	}
}
