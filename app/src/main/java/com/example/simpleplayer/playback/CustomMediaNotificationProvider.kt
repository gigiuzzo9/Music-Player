package com.example.simpleplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/**
 * Provider personalizzato che intercetta lo swipe della notifica in pausa.
 * Quando l'utente swipea via la notifica mentre il player è in pausa,
 * invia il comando STOP alla sessione, chiudendo il servizio.
 */
class CustomMediaNotificationProvider(private val context: Context) :
    MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(context)

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        // Crea la notifica con il provider di default
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        // Intercetta lo swipe solo quando il player è in pausa
        val player = mediaSession.player
        val isPaused = !player.playWhenReady || player.playbackState == Player.STATE_IDLE

        if (isPaused) {
            // Imposta un deleteIntent che invia STOP alla sessione
            // Questo fa sì che il player si fermi quando la notifica viene swipeata
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
    ): Boolean {
        // Nessun comando custom da gestire
        return false
    }
}
