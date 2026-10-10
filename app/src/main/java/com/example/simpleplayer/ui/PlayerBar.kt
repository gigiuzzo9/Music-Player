package com.example.simpleplayer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.simpleplayer.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(controller: MediaController?) {

    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }

    // Ascolta il controller (quando arriva)
    DisposableEffect(controller) {
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onRepeatModeChanged(mode: Int) {
                    repeatMode = mode
                }

                override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                    title = metadata.title?.toString() ?: ""
                    artist = metadata.artist?.toString() ?: ""
                }

                override fun onMediaItemTransition(
                    mediaItem: androidx.media3.common.MediaItem?,
                    reason: Int
                ) {
                    val md = mediaItem?.mediaMetadata
                    title = md?.title?.toString() ?: ""
                    artist = md?.artist?.toString() ?: ""
                    position = 0L
                    duration = controller.duration.coerceAtLeast(0L)
                }
            }
            controller.addListener(listener)

            val md = controller.currentMediaItem?.mediaMetadata
            title = md?.title?.toString() ?: ""
            artist = md?.artist?.toString() ?: ""
            duration = controller.duration.coerceAtLeast(0L)
            repeatMode = controller.repeatMode

            onDispose { controller.removeListener(listener) }
        }
    }

    // Aggiorna posizione e durata ogni 500 ms (solo se controller non è null)
    LaunchedEffect(controller) {
        if (controller == null) return@LaunchedEffect
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            title.ifBlank { "Nessun brano" },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    val newPos = (c.currentPosition - 10_000L)
                        .coerceIn(0L, c.duration.coerceAtLeast(0L))
                    c.seekTo(newPos)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_replay_10),
                    contentDescription = "-10 secondi",
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                formatTime(if (isUserDragging) dragPosition.toLong() else position),
                fontSize = 14.sp
            )

            Slider(
                value = if (isUserDragging) dragPosition else position.toFloat(),
                onValueChange = {
                    isUserDragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    controller?.seekTo(dragPosition.toLong())
                    position = dragPosition.toLong()
                    isUserDragging = false
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .padding(horizontal = 8.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(2.dp),
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 2.dp
                    )
                }
            )

            Text(
                formatTime(duration),
                fontSize = 14.sp
            )

            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    val newPos = (c.currentPosition + 10_000L)
                        .coerceIn(0L, c.duration.coerceAtLeast(0L))
                    c.seekTo(newPos)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_forward_10),
                    contentDescription = "+10 secondi",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Indietro",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    if (c.isPlaying) c.pause() else c.play()
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play",
                    modifier = Modifier.size(52.dp)
                )
            }

            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    if (c.hasNextMediaItem()) c.seekToNextMediaItem()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Avanti",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = {
                    val c = controller ?: return@IconButton
                    val next = when (c.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    c.repeatMode = next
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                        Icons.Filled.RepeatOne
                    else
                        Icons.Filled.Repeat,
                    contentDescription = "Ripeti",
                    tint = repeatTint,
                    modifier = Modifier.size(40.dp)
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
