package com.example.simpleplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.simpleplayer.MainActivity

enum class RepeatMode { OFF, ALL, ONE }

class PlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()

    private var queue: List<Pair<String, String>> = emptyList()
    private var currentIndex = 0

    private var repeatMode = RepeatMode.OFF

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer?.let {
                    if (it.isPlaying) it.pause()
                    updateNotification()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.let {
                    it.start()
                    updateNotification()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun playQueue(items: List<Pair<String, String>>, startIndex: Int) {
        queue = items
        currentIndex = startIndex
        if (requestAudioFocus()) playCurrent()
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
        } else {
            if (requestAudioFocus()) mp.start()
        }
        updateNotification()
    }

    fun next() {
        if (currentIndex < queue.size - 1) {
            currentIndex++
            playCurrent()
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0
            playCurrent()
        }
    }

    fun previous() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrent()
        }
    }

    fun isPlayingNow(): Boolean = mediaPlayer?.isPlaying == true

    fun currentTitle(): String =
        queue.getOrNull(currentIndex)?.second ?: "Nessun brano"

    fun getPosition(): Long = try {
        mediaPlayer?.currentPosition?.toLong() ?: 0L
    } catch (e: Exception) { 0L }

    fun getDuration(): Long = try {
        mediaPlayer?.duration?.toLong() ?: 0L
    } catch (e: Exception) { 0L }

    fun seekTo(ms: Long) {
        mediaPlayer?.seekTo(ms.toInt())
    }

    fun getRepeatMode(): RepeatMode = repeatMode

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaPlayer?.isLooping = (repeatMode == RepeatMode.ONE)
    }

    private fun playCurrent() {
        val item = queue.getOrNull(currentIndex) ?: return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = (repeatMode == RepeatMode.ONE)
            setDataSource(this@PlaybackService, android.net.Uri.parse(item.first))
            setOnPreparedListener {
                it.start()
                updateNotification()
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            setOnCompletionListener {
                next()
            }
            prepareAsync()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
            }
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    private fun stopAndClose() {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_CLOSE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = mediaPlayer?.isPlaying == true
        val playPauseIcon = if (isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        val playPauseLabel = if (isPlaying) "Pausa" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle())
            .setContentText("Music Player")
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Chiudi", closeIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_CLOSE -> {
                stopAndClose()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // NON fare nulla: il servizio continua a vivere in background.
        // La musica suona anche se l'app viene chiusa dalle recenti.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.example.simpleplayer.TOGGLE"
        const val ACTION_NEXT = "com.example.simpleplayer.NEXT"
        const val ACTION_PREV = "com.example.simpleplayer.PREV"
        const val ACTION_CLOSE = "com.example.simpleplayer.CLOSE"
    }
}
