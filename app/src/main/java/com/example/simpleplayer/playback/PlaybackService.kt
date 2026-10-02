package com.example.simpleplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.simpleplayer.MainActivity

class PlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()

    // Coda di brani: (uri, titolo)
    private var queue: List<Pair<String, String>> = emptyList()
    private var currentIndex = 0

    private var isPlaying = false

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // --- Controlli pubblici (chiamati dalla UI) ---

    fun playQueue(items: List<Pair<String, String>>, startIndex: Int) {
        queue = items
        currentIndex = startIndex
        playCurrent()
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            mp.start()
            isPlaying = true
        }
        updateNotification()
    }

    fun next() {
        if (currentIndex < queue.size - 1) {
            currentIndex++
            playCurrent()
        }
    }

    fun previous() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrent()
        }
    }

    fun isPlayingNow(): Boolean = isPlaying

    fun currentTitle(): String =
        queue.getOrNull(currentIndex)?.second ?: "Nessun brano"

    // --- Interni ---

    private fun playCurrent() {
        val item = queue.getOrNull(currentIndex) ?: return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlaybackService, android.net.Uri.parse(item.first))
            setOnPreparedListener {
                it.start()
                isPlaying = true
                updateNotification()
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            setOnCompletionListener {
                next()
            }
            prepareAsync()
        }
    }

    private fun stopAndClose() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Notifica ---

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

        val icon = if (isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle())
            .setContentText("Music Player")
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(icon, "Play/Pausa", playPauseIntent)
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

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.example.simpleplayer.TOGGLE"
        const val ACTION_CLOSE = "com.example.simpleplayer.CLOSE"
    }
}
