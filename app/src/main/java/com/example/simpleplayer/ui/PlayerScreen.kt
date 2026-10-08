package com.example.simpleplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(controller: MediaController) {

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val headerBackground = if (isSystemInDarkTheme()) {
        Color(0xFF2F2F2F)
    } else {
        Color(0xFFE0E0E0)
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(headerBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TabItem(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                modifier = Modifier.weight(1f)
            ) {
                Text("Tutti i file")
            }

            TabItem(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                modifier = Modifier.weight(1f)
            ) {
                Text("Playlist")
            }

            TabItem(
                selected = pagerState.currentPage == 2,
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                modifier = Modifier.weight(0.25f)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Nascosti"
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> FilesTab(controller = controller)
                1 -> PlaylistTab(controller = controller)
                2 -> HiddenTab()
            }
        }

        // Contenitore player: sfondo grigio che riempie tutto,
        // ma il contenuto è alzato SOPRA la barra nera di Android
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .background(headerBackground)
        ) {
            // padding inferiore = altezza barra Android
            // così il contenuto non finisce sotto i pulsanti
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = navBarPadding)
            ) {
                PlayerBar(controller = controller)
            }
        }
    }
}

@Composable
private fun TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                content()
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(2.dp)
                        .fillMaxWidth(0.6f)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                )
            }
        }
    }
}
