package com.example.simpleplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController

@Composable
fun PlayerBar(controller: MediaController) {

    // Stato osservato dal player
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var repeatMode by remember { mutableIntStateOf(controller.repeatMode) }
    var title by remember { mutableStateOf("Nessun brano") }
    var artist by remember { mutableStateOf("") }

    // Listener per aggiornare la UI quando cambia lo stato del player
    DisposableEffect(controller) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                title = metadata.title?.toString() ?: "Sconosciuto"
                artist = metadata.artist?.toString() ?: ""
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val md = mediaItem?.mediaMetadata
                title = md?.title?.toString() ?: "Sconosciuto"
                artist = md?.artist?.toString() ?: ""
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- Titolo e artista ---
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // --- Pulsanti ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Indietro
            IconButton(onClick = {
                if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                }
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Indietro")
            }

            // Play / Pausa
            IconButton(onClick = {
                if (controller.isPlaying) controller.pause() else controller.play()
            }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play"
                )
            }

            // Avanti
            IconButton(onClick = {
                if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                }
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Avanti")
            }

            // Repeat: cicla OFF → ALL → ONE → OFF
            IconButton(onClick = {
                val next = when (controller.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                controller.repeatMode = next
            }) {
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                    else Icons.Filled.Repeat,
                    contentDescription = "Ripeti",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
