package com.example.simpleplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.simpleplayer.SimplePlayerApp
import com.example.simpleplayer.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HiddenTab() {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePlayerApp
    val dao = app.database.playlistDao()
    val scope = rememberCoroutineScope()

    var allFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    val hiddenIds by dao.observeHiddenIds().collectAsState(initial = emptyList())

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

    // Scansione in background
    LaunchedEffect(Unit) {
        allFiles = withContext(Dispatchers.IO) {
            scanAudioFiles()
        }
    }

    if (!hasPermission) {
        Text(
            "Serve il permesso per leggere i file audio",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val hiddenFiles = allFiles.filter { it.id in hiddenIds }

    if (hiddenFiles.isEmpty()) {
        Text(
            "Nessun file nascosto",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(hiddenFiles, key = { it.id }) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.title)
                    Text(
                        "${file.artist} · ${formatTime(file.durationMs)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = {
                    scope.launch { dao.unhideFile(file.id) }
                }) {
                    Text("Ripristina")
                }
            }
            HorizontalDivider()
        }
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
 */
private fun scanAudioFiles(): List<AudioFile> {
    val list = mutableListOf<AudioFile>()
    val extensions = listOf("mp3", "m4a", "aac", "flac", "ogg", "wav", "opus", "wma")

    val root = Environment.getExternalStorageDirectory()

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
                // Ignora
            }
        }

    return list.sortedBy { it.title.lowercase() }
}
