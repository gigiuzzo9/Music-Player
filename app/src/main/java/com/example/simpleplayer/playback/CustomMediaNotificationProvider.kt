package com.example.simpleplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList

class CustomMediaNotificationProvider(context: Context) :
    androidx.media3.session.DefaultMediaNotificationProvider(context) {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val notification = super.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        // Sovrascriviamo il deleteIntent per gestire lo swipe
        val player = mediaSession.player
        val isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED

        if (!isPlaying) {
            // Solo quando è in pausa, impostiamo un deleteIntent custom
            // che ferma tutto quando la notifica viene swipeata via
            val dismissIntent = actionFactory.createMediaActionPendingIntent(
                mediaSession,
                Player.COMMAND_STOP
            )
            notification.notification.deleteIntent = dismissIntent
        }

        return notification
    }
}
