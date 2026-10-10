package com.example.simpleplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    // Carica i file in background (non blocca la UI)
    LaunchedEffect(Unit) {
        allFiles = withContext(Dispatchers.IO) {
            scanAudioFiles()
        }
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
                                    .setUri(Uri.fromFile(File(f.path!!)))
                                    .setMediaId(f.path)
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

/**
 * Scansiona direttamente il file system (come il file manager).
 * Nessuna query a MediaStore: è istantaneo.
 */
private fun scanAudioFiles(): List<AudioFile> {
    val list = mutableListOf<AudioFile>()
    val extensions = listOf("mp3", "m4a", "aac", "flac", "ogg", "wav", "opus", "wma")

    val root = Environment.getExternalStorageDirectory()

    // Scansiona tutte le cartelle ricorsivamente
    root.walkTopDown()
        .filter { it.isFile }
        .filter { file ->
            extensions.any { ext -> file.name.lowercase().endsWith(".$ext") }
        }
        .forEach { file ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: file.nameWithoutExtension
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Sconosciuto"
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L

                retriever.release()

                // Filtro 5 secondi
                if (duration >= 5000) {
                    list.add(
                        AudioFile(
                            id = file.absolutePath.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            uri = Uri.fromFile(file),
                            path = file.absolutePath
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignora file non leggibili
            }
        }

    // Ordina per titolo
    return list.sortedBy { it.title.lowercase() }
}
