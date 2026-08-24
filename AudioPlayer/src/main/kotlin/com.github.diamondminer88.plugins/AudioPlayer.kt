package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
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
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class AudioPlayer : Plugin() {
    private val playerBarId = View.generateViewId()
    private val loadingBarId = View.generateViewId()
    private val attachmentCardId = Utils.getResId("chat_list_item_attachment_card", "id")
    private val validFileExtensions = arrayOf(
        "webm", "mp3", "aac", "m4a", "wav", "flac", "wma", "opus", "ogg"
    )

    private var globalCurrentPlayer: MediaPlayer? = null
    private var globalCleanup: (() -> Unit)? = null
    private var globalPlayingUrl: String? = null
    private var globalIsPlaying: Boolean = false

    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    private var globalIsCompleted: Boolean = false

    private var currentActiveBarReset: (() -> Unit)? = null
    private var previousActiveBarReset: (() -> Unit)? = null

    private val durationCache = ConcurrentHashMap<String, Long>()
    private val oggFileCache = ConcurrentHashMap<String, File>()

    private fun isAudioFile(filename: String?): Boolean {
        if (filename == null) return false
        val ext = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return validFileExtensions.contains(ext)
    }

    private fun msToTime(ms: Long): String {
        val hrs = ms / 3_600_000
        val mins = ms / 60000
        val secs = ms / 1000 % 60
        return if (hrs == 0L)
            String.format("%d:%02d", mins, secs)
        else
            String.format("%d:%d:%02d", hrs, mins, secs)
    }

    private fun stopCurrentPlayer() {
        currentActiveBarReset?.invoke()
        previousActiveBarReset?.invoke()
        currentActiveBarReset = null
        previousActiveBarReset = null

        globalCleanup?.invoke()
        globalCurrentPlayer?.setOnCompletionListener(null)
        globalCurrentPlayer?.setOnPreparedListener(null)
        globalCurrentPlayer?.stop()
        globalCurrentPlayer?.release()

        globalCurrentPlayer = null
        globalCleanup = null
        globalIsPlaying = false
        globalPlayingUrl = null
        globalIsCompleted = false 
    }

    fun requestAudioFocus(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            audioManager = audioManager ?: ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager!!.requestAudioFocus(focusRequest)
        } else {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            var keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            ctx.sendOrderedBroadcast(intent, null)

            keyEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            ctx.sendOrderedBroadcast(intent, null)
        }
    }

    private fun getOggCacheDir(cacheDir: File): File {
        val oggCacheDir = File(cacheDir, "audio")
        if (!oggCacheDir.exists()) oggCacheDir.mkdirs()
        return oggCacheDir
    }

    fun deleteOggCacheFiles(cacheDir: File) {
        val oggCacheDir = getOggCacheDir(cacheDir)
        oggCacheDir.deleteRecursively()
    }

    override fun start(context: Context) {
        deleteOggCacheFiles(context.cacheDir)

        patcher.after<WidgetChatListAdapterItemAttachment>(
            "configureFileData",
            MessageAttachment::class.java,
            MessageRenderContext::class.java
        ) {
            val messageAttachment = it.args[0] as MessageAttachment
            val root = WidgetChatListAdapterItemAttachment.`access$getBinding$p`(this).root as ConstraintLayout
            val card = root.findViewById<MaterialCardView>(attachmentCardId)
            val ctx = root.context

            card.findViewById<MaterialCardView>(playerBarId)?.visibility = View.GONE
            card.findViewById<ProgressBar>(loadingBarId)?.visibility = View.GONE

            if (!isAudioFile(messageAttachment.filename)) return@after

            val existingLoadingBar = card.findViewById<ProgressBar>(loadingBarId)
            if (existingLoadingBar != null) {
                existingLoadingBar.visibility = View.VISIBLE
            } else {
                val loadingBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                    id = loadingBarId
                    isIndeterminate = true
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, 6.dp).apply {
                        gravity = Gravity.BOTTOM
                    }
                }
                card.addView(loadingBar)
            }

            Utils.threadPool.execute {
                val url = messageAttachment.url
                val isOggOrOpus = messageAttachment.filename.lowercase(Locale.ROOT).endsWith(".ogg") ||
                    messageAttachment.filename.lowercase(Locale.ROOT).endsWith(".opus")
                var duration: Long = durationCache[url] ?: run {
                    val dur = if (isOggOrOpus) {
                        OggMetadataFetcher.fetch(url)?.duration?.times(1000)?.toLong() ?: 0L
                    } else {
                        try {
                            MediaMetadataRetriever().use { retriever ->
                                retriever.setDataSource(url)
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                            }
                        } catch (e: Throwable) {
                            0L
                        }
                    }
                    durationCache[url] = dur
                    dur
                }

                Utils.mainThread.post {
                    card.findViewById<ProgressBar>(loadingBarId)?.visibility = View.GONE

                    val existingPlayerCard = card.findViewById<MaterialCardView>(playerBarId)
                    val playerCard = if (existingPlayerCard != null) {
                        existingPlayerCard.visibility = View.VISIBLE
                        existingPlayerCard
                    } else {
                        MaterialCardView(ctx).apply {
                            id = playerBarId
                            cardElevation = 4.dp.toFloat()
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                topMargin = 60.dp
                                gravity = Gravity.BOTTOM
                            }
                            isClickable = true
                            isFocusable = false
                            foreground = null
                            stateListAnimator = null
                        }.also { card.addView(it) }
                    }

                    val playerBar = LinearLayout(ctx, null, 0, R.i.UiKit_ViewGroup).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(24, 24, 24, 24)
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                        var localTimer: Timer? = null

                        var buttonView: ImageButton? = null
                        var progressView: TextView? = null
                        var sliderView: SeekBar? = null
                        lateinit var playIcon: android.graphics.drawable.Drawable
                        lateinit var pauseIcon: android.graphics.drawable.Drawable
                        lateinit var rewindIcon: android.graphics.drawable.Drawable

                        fun cancelTimer() {
                            localTimer?.cancel()
                            localTimer = null
                        }

                        fun setIdleState() {
                            cancelTimer()
                            buttonView?.background = playIcon
                            buttonView?.isEnabled = true
                            sliderView?.progress = 0
                            progressView?.text = "0:00 / ${msToTime(duration)}"
                        }

                        fun updateUiFromPlayer() {
                            if (globalPlayingUrl != url || globalCurrentPlayer == null) {
                                setIdleState()
                                return
                            }
                            val pos = try {
                                globalCurrentPlayer?.currentPosition ?: 0
                            } catch (e: IllegalStateException) {
                                setIdleState()
                                cancelTimer()
                                return
                            }
                            sliderView?.progress = if (duration > 0) (500 * pos / duration).toInt() else 0
                            progressView?.text = "${msToTime(pos.toLong())} / ${msToTime(duration)}"
                            buttonView?.background = when {
                                globalIsCompleted -> rewindIcon
                                globalIsPlaying -> pauseIcon
                                else -> playIcon
                            }
                            buttonView?.isEnabled = true
                        }

                        fun startTimer() {
                            cancelTimer()
                            if (globalPlayingUrl != url || globalCurrentPlayer == null) return
                            localTimer = Timer()
                            localTimer!!.scheduleAtFixedRate(object : TimerTask() {
                                override fun run() {
                                    if (globalPlayingUrl != url || globalCurrentPlayer == null) {
                                        cancelTimer()
                                        Utils.mainThread.post { setIdleState() }
                                        return
                                    }
                                    Utils.mainThread.post { updateUiFromPlayer() }
                                }
                            }, 250, 250)
                        }

                        fun restoreUiToGlobalState() {
                            if (globalPlayingUrl == url && globalCurrentPlayer != null) {
                                updateUiFromPlayer()
                                startTimer()
                            } else {
                                setIdleState()
                            }
                        }

                        playIcon = ContextCompat.getDrawable(ctx, com.google.android.exoplayer2.ui.R.b.exo_controls_play)!!
                        pauseIcon = ContextCompat.getDrawable(ctx, com.google.android.exoplayer2.ui.R.b.exo_controls_pause)!!
                        rewindIcon = ContextCompat.getDrawable(ctx, com.yalantis.ucrop.R.c.ucrop_rotate)!!

                        buttonView = ImageButton(ctx).apply {
                            background = playIcon
                            setPadding(16, 16, 16, 16)
                            isEnabled = true
                        }

                        progressView = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
                            text = "0:00 / ${msToTime(duration)}"
                            setPadding(16, 16, 16, 16)
                        }

                        sliderView = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
                            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { weight = 0.5f }
                            val p = 2.dp
                            setPadding(p, p, p, 0)
                            gravity = Gravity.CENTER
                            progress = 0
                            thumb = null
                            max = 500
                        }

                        sliderView?.setOnSeekBarChangeListener(
                            object : SeekBar.OnSeekBarChangeListener {
                                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                                override fun onStopTrackingTouch(seekBar: SeekBar) {}
                                var prevProgress = 0
                                override fun onProgressChanged(
                                    seekBar: SeekBar,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    if (!fromUser) return
                                    if (globalPlayingUrl == url && globalCurrentPlayer != null && globalCurrentPlayer!!.isPlaying) {
                                        prevProgress = progress
                                        val seekTo = (progress / 500f * duration).toInt()
                                        globalCurrentPlayer!!.seekTo(seekTo)
                                        progressView?.text =
                                            "${msToTime(globalCurrentPlayer!!.currentPosition.toLong())} / ${msToTime(duration)}"
                                    } else {
                                        seekBar.progress = prevProgress
                                    }
                                }
                            }
                        )

                        val resetThisBar = {
                            cancelTimer()
                            setIdleState()
                        }

                        if (globalPlayingUrl == url && globalCurrentPlayer != null) {
                            updateUiFromPlayer()
                            startTimer()
                            currentActiveBarReset = resetThisBar
                        } else {
                            setIdleState()
                        }

                        buttonView?.setOnClickListener {
                            requestAudioFocus(ctx)

                            val isOggOrOpusFile = messageAttachment.filename.lowercase(Locale.ROOT).endsWith(".ogg") ||
                                messageAttachment.filename.lowercase(Locale.ROOT).endsWith(".opus")
                            val url = messageAttachment.url

                            globalIsCompleted = false

                            if (globalPlayingUrl == url && globalCurrentPlayer != null) {
                                if (globalCurrentPlayer!!.isPlaying) {
                                    globalCurrentPlayer!!.pause()
                                    globalIsPlaying = false
                                    buttonView?.background = playIcon
                                } else {
                                    globalCurrentPlayer!!.start()
                                    globalIsPlaying = true
                                    buttonView?.background = pauseIcon
                                }
                                restoreUiToGlobalState()
                                return@setOnClickListener
                            }

                            previousActiveBarReset?.invoke()
                            previousActiveBarReset = currentActiveBarReset
                            currentActiveBarReset = resetThisBar

                            stopCurrentPlayer() 

                            buttonView?.isEnabled = false
                            Utils.threadPool.execute {
                                var playUrl = url
                                if (isOggOrOpusFile) {
                                    val oggCacheDir = getOggCacheDir(ctx.cacheDir)
                                    var file = oggFileCache[url]
                                    if (file == null || !file.exists()) {
                                        file = File(oggCacheDir, "audio_${url.hashCode()}.ogg")
                                        try {
                                            Http.simpleDownload(url, file)
                                            oggFileCache[url] = file
                                        } catch (e: Exception) {
                                            Utils.mainThread.post {
                                                buttonView?.isEnabled = true
                                                Toast.makeText(ctx, "Failed to download audio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                            return@execute
                                        }
                                    }
                                    playUrl = file.absolutePath
                                }

                                Utils.threadPool.execute {
                                    val newPlayer = MediaPlayer()
                                    try {
                                        newPlayer.setDataSource(playUrl)
                                        newPlayer.setOnPreparedListener {
                                            globalCurrentPlayer = newPlayer
                                            globalPlayingUrl = url
                                            globalIsPlaying = true
                                            globalIsCompleted = false 
                                            newPlayer.start()
                                            restoreUiToGlobalState()
                                            startTimer()
                                        }
                                        newPlayer.setOnCompletionListener {
                                            globalIsPlaying = false
                                            globalIsCompleted = true
                                            restoreUiToGlobalState()
                                        }
                                        newPlayer.prepareAsync()
                                        globalCleanup = {
                                            try { newPlayer.stop() } catch (_: Exception) {}
                                            try { newPlayer.release() } catch (_: Exception) {}
                                            buttonView?.background = playIcon
                                            sliderView?.progress = 0
                                            progressView?.text = "0:00 / ${msToTime(duration)}"
                                            globalCurrentPlayer = null
                                            globalPlayingUrl = null
                                            globalIsPlaying = false
                                            globalIsCompleted = false 
                                            cancelTimer()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Failed to play audio: ${e.message}", Toast.LENGTH_SHORT).show()
                                        newPlayer.release()
                                        restoreUiToGlobalState()
                                        cancelTimer()
                                    } finally {
                                        buttonView?.isEnabled = true
                                    }
                                }
                            }
                        }

                        addView(buttonView)
                        addView(progressView)
                        addView(sliderView)
                    }

                    playerCard.removeAllViews()
                    playerCard.addView(playerBar)
                }
            }
        }

        patcher.after<StoreMessages>("handleChannelSelected", Long::class.javaPrimitiveType!!) {
            stopCurrentPlayer()
        }
        patcher.after<AppActivity>("onCreate", Bundle::class.java) {
            stopCurrentPlayer()
        }
        patcher.after<AppActivity>("onPause") {
            stopCurrentPlayer()
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        stopCurrentPlayer()
        deleteOggCacheFiles(context.cacheDir)
    }
}