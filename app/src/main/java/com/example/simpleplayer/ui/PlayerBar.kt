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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Modalità di ripetizione
enum class RepeatMode { OFF, ALL, ONE }

@Composable
fun PlayerBar() {

    // Stato finto — poi lo collegheremo al MediaController
    var isPlaying by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(RepeatMode.OFF) }
    var currentTitle by remember { mutableStateOf("Nessun brano") }
    var currentArtist by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- Titolo e artista ---
        Text(
            currentTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            currentArtist,
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
                // TODO: collegare a controller.seekToPrevious()
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Indietro")
            }

            // Play / Pausa
            IconButton(onClick = {
                isPlaying = !isPlaying
                // TODO: collegare a controller.play() / controller.pause()
            }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play"
                )
            }

            // Avanti
            IconButton(onClick = {
                // TODO: collegare a controller.seekToNext()
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Avanti")
            }

            // Repeat: cicla OFF → ALL → ONE → OFF
            IconButton(onClick = {
                repeatMode = when (repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                // TODO: collegare a controller.setRepeatMode(...)
            }) {
                Icon(
                    if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne
                    else Icons.Filled.Repeat,
                    contentDescription = "Ripeti",
                    tint = if (repeatMode == RepeatMode.OFF)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
