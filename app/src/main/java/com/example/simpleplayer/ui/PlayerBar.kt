package com.example.simpleplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpleplayer.playback.PlaybackService
import kotlinx.coroutines.delay

@Composable
fun PlayerBar(service: PlaybackService) {

    var isPlaying by remember { mutableStateOf(service.isPlayingNow()) }
    var title by remember { mutableStateOf(service.currentTitle()) }

    LaunchedEffect(service) {
        while (true) {
            isPlaying = service.isPlayingNow()
            title = service.currentTitle()
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = { service.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Indietro")
            }

            IconButton(onClick = { service.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play"
                )
            }

            IconButton(onClick = { service.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Avanti")
            }
        }
    }
}
