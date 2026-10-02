package com.example.simpleplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simpleplayer.playback.PlaybackService

@Composable
fun PlayerScreen(service: PlaybackService) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tutti i file", "Playlist")

    Column(modifier = Modifier.fillMaxSize()) {

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> FilesTab(service = service)
                1 -> PlaylistTab(service = service)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f)
                .padding(horizontal = 8.dp)
        ) {
            PlayerBar(service = service)
        }
    }
}
