package com.example.simpleplayer.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.example.simpleplayer.SimplePlayerApp
import com.example.simpleplayer.data.HiddenFile
import com.example.simpleplayer.model.AudioFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesTab(controller: MediaController) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePlayerApp
    val dao = app.database.playlistDao()
    val scope = rememberCoroutineScope()

    var allFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    val hiddenIds by dao.observeHiddenIds().collectAsState(initial = null)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_AUDIO
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var contextFile by remember { mutableStateOf<AudioFile?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_AUDIO
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            allFiles = queryAudioFiles(context)
        }
    }

    if (!hasPermission) {
        Text(
            "Serve il permesso per leggere i file audio",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val hidden = hiddenIds ?: return

    val visibleFiles = allFiles.filter { it.id !in hidden }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(visibleFiles, key = { it.id }) { file ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val items = visibleFiles.map { f ->
                                MediaItem.Builder()
                                    .setUri(f.uri)
                                    .setMediaId(f.id.toString())
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(f.title)
                                            .setArtist(f.artist)
                                            .build()
                                    )
                                    .build()
                            }
                            val index = visibleFiles.indexOf(file).coerceAtLeast(0)
                            controller.setMediaItems(items, index, 0L)
                            controller.prepare()
                            controller.play()
                        },
                        onLongClick = {
                            contextFile = file
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(file.title)
                Text(
                    "${file.artist} · ${formatTime(file.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            HorizontalDivider()
        }
    }

    contextFile?.let { file ->
        AlertDialog(
            onDismissRequest = { contextFile = null },
            title = { Text(file.title) },
            text = { Text("Vuoi nascondere questo file?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.hideFile(HiddenFile(file.id))
                    }
                    contextFile = null
                }) { Text("Nascondi") }
            },
            dismissButton = {
                TextButton(onClick = { contextFile = null }) { Text("Annulla") }
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun queryAudioFiles(context: android.content.Context): List<AudioFile> {
    val list = mutableListOf<AudioFile>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
            "AND ${MediaStore.Audio.Media.DURATION} >= 5000"
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        "${MediaStore.Audio.Media.TITLE} ASC"
    )

    cursor?.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            list.add(
                AudioFile(
                    id = id,
                    title = it.getString(titleCol) ?: "Sconosciuto",
                    artist = it.getString(artistCol) ?: "Sconosciuto",
                    durationMs = it.getLong(durCol),
                    uri = uri,
                    path = null
                )
            )
        }
    }
    return list
}
