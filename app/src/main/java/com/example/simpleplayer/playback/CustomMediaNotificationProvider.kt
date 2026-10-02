package com.example.simpleplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

class CustomMediaNotificationProvider(private val context: Context) :
    MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(context)

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        val player = mediaSession.player
        val isPaused = !player.playWhenReady || player.playbackState == Player.STATE_IDLE

        if (isPaused) {
            val stopIntent = actionFactory.createMediaActionPendingIntent(
                mediaSession,
                Player.COMMAND_STOP
            )
            mediaNotification.notification.deleteIntent = stopIntent
        }

        return mediaNotification
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean = false

    // Aggiunto: l'interfaccia lo richiede esplicitamente
    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return MediaNotification.Provider.NotificationChannelInfo(
            "simple_player_channel",
            "Music Player"
        )
    }
}
