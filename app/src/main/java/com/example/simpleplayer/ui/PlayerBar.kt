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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

@Composable
fun PlayerBar(controller: MediaController) {

    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var repeatMode by remember { mutableIntStateOf(controller.repeatMode) }
    var title by remember { mutableStateOf("Nessun brano") }
    var artist by remember { mutableStateOf("") }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

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

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {
                val md = mediaItem?.mediaMetadata
                title = md?.title?.toString() ?: "Sconosciuto"
                artist = md?.artist?.toString() ?: ""
                position = 0L
                duration = controller.duration.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // Aggiorna posizione e durata ogni 500 ms
    LaunchedEffect(controller) {
        while (true) {
            if (!isUserDragging) {
                position = controller.currentPosition.coerceAtLeast(0L)
            }
            duration = controller.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    val repeatOffColor = Color(0xFF888888)
    val repeatOnColor = Color(0xFF1DB954)
    val repeatTint = if (repeatMode == Player.REPEAT_MODE_OFF) repeatOffColor else repeatOnColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- Titolo / artista ---
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

        // --- Seekbar + tempi ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTime(
                    if (isUserDragging) dragPosition.toLong() else position
                ),
                style = MaterialTheme.typography.labelSmall
            )

            Slider(
                value = if (isUserDragging) dragPosition
                        else position.toFloat(),
                onValueChange = {
                    isUserDragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    controller.seekTo(dragPosition.toLong())
                    position = dragPosition.toLong()
                    isUserDragging = false
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                formatTime(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }

        // --- Pulsanti ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = {
                if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                }
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Indietro")
            }

            IconButton(onClick = {
                if (controller.isPlaying) controller.pause() else controller.play()
            }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play"
                )
            }

            IconButton(onClick = {
                if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                }
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Avanti")
            }

            IconButton(onClick = {
                val next = when (controller.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                controller.repeatMode = next
            }) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                        Icons.Filled.RepeatOne
                    else
                        Icons.Filled.Repeat,
                    contentDescription = "Ripeti",
                    tint = repeatTint
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
