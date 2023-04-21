package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.filename
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.url
import com.discord.api.message.attachment.MessageAttachment
import com.discord.app.AppActivity
import com.discord.stores.StoreMessages
import com.discord.utilities.textprocessing.MessageRenderContext
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R
import java.io.File
import java.util.*

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class AudioPlayer : Plugin() {
	private val playerBarId = View.generateViewId()
	private val attachmentCardId = Utils.getResId("chat_list_item_attachment_card", "id")
	private val attachmentCardIconId = Utils.getResId("chat_list_item_attachment_icon", "id")
	private val audioFileIconId = Utils.getResId("ic_file_audio", "drawable")
	private val validFileExtensions = arrayOf("webm", "mp3", "aac", "m4a", "wav", "flac", "wma", "opus", "ogg")

	private fun msToTime(ms: Long): String {
		val hrs = ms / 3_600_000
		val mins = ms / 60000
		val secs = ms / 1000 % 60

		return if (hrs == 0L)
			String.format("%d:%02d", mins, secs)
		else
			String.format("%d:%d:%02d", hrs, mins, secs)
	}

	override fun start(context: Context) {
		val p2 = DimenUtils.defaultPadding / 2
		var onPauseListener: (() -> Unit)? = null
		var currentPlayerUnsubscribe: (() -> Unit)? = null
		var currentPlayer: MediaPlayer? = null

		// rotated triangle icon
		val playIcon = ContextCompat.getDrawable(
			context,
			com.google.android.exoplayer2.ui.R.b.exo_controls_pause
		)
		// two vertical bars icon
		val pauseIcon = ContextCompat.getDrawable(
			context,
			com.google.android.exoplayer2.ui.R.b.exo_controls_play
		)
		val rewindIcon = ContextCompat.getDrawable(
			context,
			com.yalantis.ucrop.R.c.ucrop_rotate
		)

		patcher.after<WidgetChatListAdapterItemAttachment>(
			"configureFileData",
			MessageAttachment::class.java,
			MessageRenderContext::class.java
		) {
			val messageAttachment = it.args[0] as MessageAttachment
			if (!validFileExtensions.contains(messageAttachment.filename.split(".").last())) return@after

			val root = WidgetChatListAdapterItemAttachment.`access$getBinding$p`(this)
				.root as ConstraintLayout
			val card = root.findViewById<MaterialCardView>(attachmentCardId)
			val ctx = root.context

			if (card.findViewById<LinearLayout>(playerBarId) != null) return@after

			var duration: Long = try {
				MediaMetadataRetriever().use { retriever ->
					retriever.setDataSource(messageAttachment.url, hashMapOf())
					val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
					retriever.release()
					duration?.toLong() ?: 0L
				}
			} catch (e: Throwable) {
				-1L
			}

			card.addView(LinearLayout(ctx, null, 0, R.i.UiKit_ViewGroup).apply {
				id = playerBarId

				// Invalid file, ignore
				if (duration == -1L) {
					visibility = View.GONE
					return@after
				}

				val icon = card.findViewById<ImageView>(attachmentCardIconId)
				icon.setImageResource(audioFileIconId)

				setPadding(p2, p2, p2, p2)
				setOnClickListener {} // don't download attachment
				layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
					.apply {
						topMargin = 60.dp
						gravity = Gravity.BOTTOM
					}

				val buttonView = ImageButton(ctx).apply {
					background = pauseIcon
					setPadding(p2, p2, p2, p2)
				}

				val progressView = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
					text = "0:00 / " + if (duration != 0L) msToTime(duration) else "??"
					setPadding(p2, p2, p2, p2)
				}

				val sliderView = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
					layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
						.apply { weight = 0.5f }
					val p = 2.dp
					setPadding(p, p, p, 0)
					gravity = Gravity.CENTER
					progress = 0
					thumb = null
					max = 500
				}

				var isPrepared = false
				var preparing = false
				var playing = false

				var timer: Timer? = null
				fun scheduleUpdater() {
					timer?.cancel()
					timer = Timer()
					timer!!.scheduleAtFixedRate(object : TimerTask() {
						override fun run() {
							if (!playing || duration == 0L) return
							Utils.mainThread.post {
								progressView.text =
									"${msToTime(currentPlayer!!.currentPosition.toLong())} / ${msToTime(duration)}"
								sliderView.progress = (500 * currentPlayer!!.currentPosition / duration).toInt()
							}
						}
					}, 2000, 250)
				}

				fun updatePlaying() {
					if (currentPlayer == null)
						return

					if (playing) {
						currentPlayer!!.start()
						scheduleUpdater()
						buttonView.background = playIcon
					} else {
						currentPlayer!!.pause()
						timer?.cancel()
						timer?.purge()
						timer = null
						buttonView.background = pauseIcon
					}
				}

				sliderView.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
					override fun onStartTrackingTouch(seekBar: SeekBar) {}
					override fun onStopTrackingTouch(seekBar: SeekBar) {}
					var prevProgress = 0
					override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
						if (!fromUser) return
						if (!isPrepared) {
							seekBar.progress = prevProgress
							return
						}
						prevProgress = progress
						currentPlayer!!.seekTo((progress.div(500f) * duration).toInt())
						progressView.text =
							"${msToTime(currentPlayer!!.currentPosition.toLong())} / ${msToTime(duration)}"
					}
				})

				buttonView.setOnClickListener {
					playing = !playing

					if (!isPrepared && !preparing) {
						preparing = true
						Utils.mainThread.post { buttonView.background = null }

						currentPlayer?.release()
						currentPlayerUnsubscribe?.invoke()
						onPauseListener = null
						var url = messageAttachment.url
						currentPlayer = MediaPlayer()

						Utils.threadPool.execute {

							if (messageAttachment.filename.endsWith(".ogg")) {
								var file = File(ctx.cacheDir, "audio.ogg")
								file.deleteOnExit()
								Http.simpleDownload(url, file)
								url = file.absolutePath
							}

							currentPlayer?.apply {
								setDataSource(url)
								setOnPreparedListener {
									seekTo((sliderView.progress.div(500f) * duration).toInt())
									Utils.mainThread.post { updatePlaying() }
									duration = it.duration.toLong()
								}
								setOnCompletionListener { player ->
									playing = false
									player.seekTo(0)
									Utils.mainThread.post { buttonView.background = rewindIcon }
								}
								currentPlayerUnsubscribe = {
									playing = false
									Utils.mainThread.post { updatePlaying() }
								}
								onPauseListener = {
									playing = false
									Utils.mainThread.post { updatePlaying() }
								}
								prepare()
								isPrepared = true
								preparing = false
							}
						}
					} else {
						updatePlaying()
					}
				}

				addView(buttonView)
				addView(progressView)
				addView(sliderView)
			})
		}

		patcher.after<StoreMessages>("handleChannelSelected", Long::class.javaPrimitiveType!!) {
			currentPlayerUnsubscribe?.invoke()
			currentPlayerUnsubscribe = null
			onPauseListener = null
		}

		patcher.after<AppActivity>("onCreate", Bundle::class.java) {
			currentPlayerUnsubscribe?.invoke()
			currentPlayerUnsubscribe = null
			onPauseListener = null
		}

		patcher.after<AppActivity>("onPause") {
			onPauseListener?.invoke()
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
