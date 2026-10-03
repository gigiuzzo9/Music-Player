@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.simpleplayer.R
import com.example.simpleplayer.playback.PlaybackService
import com.example.simpleplayer.playback.RepeatMode
import kotlinx.coroutines.delay

@Composable
fun PlayerBar(service: PlaybackService) {

    var isPlaying by remember { mutableStateOf(service.isPlayingNow()) }
    var title by remember { mutableStateOf(service.currentTitle()) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var repeatMode by remember { mutableStateOf(service.getRepeatMode()) }

    LaunchedEffect(service) {
        while (true) {
            isPlaying = service.isPlayingNow()
            title = service.currentTitle()
            if (!isUserDragging) {
                position = service.getPosition()
            }
            duration = service.getDuration()
            repeatMode = service.getRepeatMode()
            delay(500)
        }
    }

    val repeatOffColor = Color(0xFF888888)
    val repeatOnColor = Color(0xFF1DB954)
    val repeatTint = when (repeatMode) {
        RepeatMode.OFF -> repeatOffColor
        else -> repeatOnColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
         
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // --- Seekbar + tempi + pulsanti -10s / +10s ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { service.skipBy(-10_000L) },
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
                    service.seekTo(dragPosition.toLong())
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
                onClick = { service.skipBy(10_000L) },
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_forward_10),
                    contentDescription = "+10 secondi",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // --- Pulsanti ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = { service.previous() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Indietro",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = { service.togglePlayPause() },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play",
                    modifier = Modifier.size(52.dp)
                )
            }

            IconButton(
                onClick = { service.next() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Avanti",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = { service.cycleRepeatMode() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE)
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
