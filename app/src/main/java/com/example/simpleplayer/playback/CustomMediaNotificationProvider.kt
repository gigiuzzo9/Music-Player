package com.example.simpleplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.core.app.NotificationCompat
import com.google.common.collect.ImmutableList

class CustomMediaNotificationProvider(private val context: Context) :
    MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {

        val player = mediaSession.player
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString() ?: "Music Player"
        val artist = metadata.artist?.toString() ?: ""

        val playIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession, Player.COMMAND_PLAY_PAUSE
        )
        val nextIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession, Player.COMMAND_SEEK_TO_NEXT
        )
        val prevIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession, Player.COMMAND_SEEK_TO_PREVIOUS
        )

        // L'intento che scatta quando l'utente SWIPEA VIA la notifica
        val deleteIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession, Player.COMMAND_STOP
        )

        val isPlaying = player.isPlaying
        val playPauseIcon = if (isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)  // in pausa diventa swipeabile
            .setDeleteIntent(deleteIntent)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevIntent)
            .addAction(playPauseIcon, "Play/Pause", playIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)

        // MediaStyle di Media3 con il token del MediaSession
        builder.setStyle(
            MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0, 1, 2)
        )

        return MediaNotification(NOTIFICATION_ID, builder.build())
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean = false

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return MediaNotification.Provider.NotificationChannelInfo(
            CHANNEL_ID,
            "Music Player"
        )
    }

    companion object {
        private const val CHANNEL_ID = "music_player_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
