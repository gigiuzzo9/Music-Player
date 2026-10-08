package com.example.simpleplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
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

    private lateinit var prefs: SharedPreferences

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) it.pause()
                    } catch (e: Exception) { }
                    updateNotification()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.let {
                    try {
                        it.start()
                    } catch (e: Exception) { }
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
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Crea il player UNA VOLTA SOLA e riutilizzalo sempre
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }

        restoreLastTrack()
    }

    private fun restoreLastTrack() {
        val savedUri = prefs.getString(KEY_LAST_URI, null) ?: return
        val savedRepeat = prefs.getString(KEY_REPEAT, "OFF") ?: "OFF"

        val savedTitle = getTitleFromMediaStore(savedUri)
            ?: prefs.getString(KEY_LAST_TITLE, "Brano")
            ?: "Brano"

        repeatMode = when (savedRepeat) {
            "ALL" -> RepeatMode.ALL
            "ONE" -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        queue = listOf(savedUri to savedTitle)
        currentIndex = 0

        // Non parte. Resta pronto in pausa.
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mediaPlayer?.isLooping = (repeatMode == RepeatMode.ONE)
            mediaPlayer?.setDataSource(savedUri)
            mediaPlayer?.setOnPreparedListener {
                // NON parte da solo
            }
            mediaPlayer?.setOnCompletionListener {
                next()
            }
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            // Se fallisce, ignora: il player si ripreparerà al play
        }

        prefs.edit().putString(KEY_LAST_TITLE, savedTitle).apply()
    }

    private fun getTitleFromMediaStore(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val cursor = contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.TITLE),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val title = it.getString(0)
                    if (!title.isNullOrBlank()) return title
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun saveLastTrack() {
        val item = queue.getOrNull(currentIndex) ?: return
        prefs.edit()
            .putString(KEY_LAST_URI, item.first)
            .putString(KEY_LAST_TITLE, item.second)
            .apply()
    }

    private fun saveRepeatMode() {
        prefs.edit()
            .putString(KEY_REPEAT, repeatMode.name)
            .apply()
    }

    fun playQueue(items: List<Pair<String, String>>, startIndex: Int) {
        queue = items
        currentIndex = startIndex
        requestAudioFocus()
        playCurrent()
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
            } else {
                if (requestAudioFocus()) mp.start()
            }
        } catch (e: Exception) { }
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

    fun isPlayingNow(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (e: Exception) { false }

    fun currentTitle(): String =
        queue.getOrNull(currentIndex)?.second ?: "Nessun brano"

    fun getPosition(): Long = try {
        mediaPlayer?.currentPosition?.toLong() ?: 0L
    } catch (e: Exception) { 0L }

    fun getDuration(): Long = try {
        mediaPlayer?.duration?.toLong() ?: 0L
    } catch (e: Exception) { 0L }

    fun seekTo(ms: Long) {
        try {
            mediaPlayer?.seekTo(ms.toInt())
        } catch (e: Exception) { }
    }

    fun skipBy(ms: Long) {
        val mp = mediaPlayer ?: return
        try {
            val newPos = (mp.currentPosition + ms).coerceIn(0L, mp.duration.toLong())
            mp.seekTo(newPos.toInt())
        } catch (e: Exception) { }
    }

    fun getRepeatMode(): RepeatMode = repeatMode

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        try {
            mediaPlayer?.isLooping = (repeatMode == RepeatMode.ONE)
        } catch (e: Exception) { }
        saveRepeatMode()
    }

    private fun playCurrent() {
        val item = queue.getOrNull(currentIndex) ?: return

        val mp = mediaPlayer ?: return

        try {
            // Reset e riconfigura il player ESISTENTE
            mp.reset()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.isLooping = (repeatMode == RepeatMode.ONE)

            // MODIFICA 1: passa la stringa URI direttamente (non Context + Uri)
            mp.setDataSource(item.first)

            // MODIFICA 2: listener di errore, per sapere se qualcosa va storto
            mp.setOnErrorListener { _, what, extra ->
                android.util.Log.e("PLAYER", "Errore MediaPlayer: what=$what extra=$extra")
                true
            }

            mp.setOnPreparedListener {
                try {
                    it.start()
                    updateNotification()
                    startForeground(NOTIFICATION_ID, buildNotification())
                } catch (e: Exception) {
                    android.util.Log.e("PLAYER", "Errore su start(): ${e.message}")
                }
            }

            mp.setOnCompletionListener {
                next()
            }

            // MODIFICA 4: prepare() sincrono invece di prepareAsync()
            mp.prepare()

        } catch (e: Exception) {
            android.util.Log.e("PLAYER", "Errore in playCurrent: ${e.message}")

            // Se qualcosa è andato storto, distruggi e ricrea il player
            try {
                mp.release()
            } catch (ex: Exception) { }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                try {
                    setDataSource(item.first)
                    setOnPreparedListener {
                        it.start()
                        updateNotification()
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                    setOnCompletionListener { next() }
                    prepare()
                } catch (ex: Exception) {
                    android.util.Log.e("PLAYER", "Errore anche con il nuovo player: ${ex.message}")
                }
            }
        }

        saveLastTrack()
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
        try {
            mediaPlayer?.release()
        } catch (e: Exception) { }
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

        val isPlaying = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) { false }

        val playPauseIcon = if (isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        val playPauseLabel = if (isPlaying) "Pausa" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle())
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
        try {
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) { }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_CLOSE -> {
                stopAndClose()
                return START_NOT_STICKY
            }
            else -> {
                // Niente startForeground qui
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) { }
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

        private const val PREFS_NAME = "music_player_prefs"
        private const val KEY_LAST_URI = "last_uri"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_REPEAT = "repeat_mode"
    }
}
