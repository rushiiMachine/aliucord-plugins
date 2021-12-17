package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.discord.utilities.textprocessing.MessageRenderContext
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R
import java.util.*

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class AudioPlayer : Plugin() {
    private val playerBarId = View.generateViewId()
    private val attachmentCardId = Utils.getResId("chat_list_item_attachment_card", "id")
    private val attachmentCardIconId = Utils.getResId("chat_list_item_attachment_icon", "id")
    private val audioFileIconId = Utils.getResId("ic_file_audio", "drawable")
    private val validFileExtensions = listOf("webm", "mp3", "aac", "m4a", "wav", "flac", "wma")

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
        var onPauseListeners = mutableListOf<() -> Unit>()

        // rotated triangle icon
        val playIcon = AppCompatResources.getDrawable(
            context,
            com.google.android.exoplayer2.ui.R.b.exo_controls_pause
        )
        // two vertical bars icon
        val pauseIcon = AppCompatResources.getDrawable(
            context,
            com.google.android.exoplayer2.ui.R.b.exo_controls_play
        )
        val rewindIcon = AppCompatResources.getDrawable(
            context,
            com.yalantis.ucrop.R.c.ucrop_rotate
        )

        patcher.after<AppActivity>("onPause") {
            onPauseListeners.forEach { it.invoke() }
            if (onPauseListeners.size > 10) {
                onPauseListeners = onPauseListeners.takeLast(10).toMutableList()
            }
        }

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

            var loadError: Throwable? = null
            val duration = try {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(messageAttachment.url, hashMapOf())
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    retriever.release()
                    duration?.toLong()
                }
            } catch (e: Throwable) {
                loadError = e
                null
            }

            val player = try {
                MediaPlayer().apply {
                    setDataSource(messageAttachment.url)
                }
            } catch (e: Throwable) {
                loadError = e
                null
            }

            if (loadError != null) {
                logger.errorToast("AudioPlayer: Failed to load metadata", loadError)
            }

            card.addView(LinearLayout(ctx, null, 0, R.i.UiKit_ViewGroup).apply {
                id = playerBarId

                // Invalid file, ignore
                if (duration == null || loadError != null) {
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
                    text = "0:00 / " + msToTime(duration)
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
                var playing = false

                var timer: Timer? = null
                fun scheduleUpdater() {
                    timer = Timer()
                    timer!!.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            if (!playing) return
                            Utils.mainThread.post {
                                progressView.text =
                                    "${msToTime(player!!.currentPosition.toLong())} / ${msToTime(duration)}"
                                sliderView.progress = (500 * player.currentPosition / duration).toInt()
                            }
                        }
                    }, 2000, 250)
                }

                @MainThread
                fun updatePlaying() {
                    if (playing) {
                        player!!.start()
                        scheduleUpdater()
                        buttonView.background = playIcon
                    } else {
                        player!!.pause()
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
                        player!!.seekTo((progress.div(500f) * duration).toInt())
                        progressView.text =
                            "${msToTime(player.currentPosition.toLong())} / ${msToTime(duration)}"
                    }
                })

                buttonView.setOnClickListener {
                    playing = !playing

                    if (!isPrepared) {
                        isPrepared = true
                        // TODO: set btn to loading icon
                        Utils.mainThread.post { buttonView.background = null }
                        player!!.setOnPreparedListener {
                            Utils.mainThread.post { updatePlaying() }
                        }
                        player.prepareAsync()
                    } else updatePlaying()
                }

                player!!.setOnCompletionListener {
                    playing = false
                    player.seekTo(0)
                    Utils.mainThread.post { buttonView.background = rewindIcon }
                }

                onPauseListeners.add {
                    playing = false
                    Utils.mainThread.post { updatePlaying() }
                }

                addView(buttonView)
                addView(progressView)
                addView(sliderView)
            })
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
