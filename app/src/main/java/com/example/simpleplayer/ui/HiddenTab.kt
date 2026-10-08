package com.example.simpleplayer.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
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
import kotlinx.coroutines.launch

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

private fun queryAudioFiles(context: Context): List<AudioFile> {
    val list = mutableListOf<AudioFile>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
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
        val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)

        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            val path = if (dataCol >= 0) it.getString(dataCol) else null
            list.add(
                AudioFile(
                    id = id,
                    title = it.getString(titleCol) ?: "Sconosciuto",
                    artist = it.getString(artistCol) ?: "Sconosciuto",
                    durationMs = it.getLong(durCol),
                    uri = uri,
                    path = path
                )
            )
        }
    }
    return list
}
