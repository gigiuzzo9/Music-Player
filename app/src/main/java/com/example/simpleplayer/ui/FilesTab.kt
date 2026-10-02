package com.example.simpleplayer.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.simpleplayer.model.AudioFile

@Composable
fun FilesTab() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
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
            files = queryAudioFiles(context)
        }
    }

    if (!hasPermission) {
        Text(
            "Serve il permesso per leggere i file audio",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files, key = { it.id }) { file ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Qui in futuro: fai partire la riproduzione
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(file.title)
                Text(
                    file.artist,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
            HorizontalDivider()
        }
    }
}

private fun queryAudioFiles(context: android.content.Context): List<AudioFile> {
    val list = mutableListOf<AudioFile>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
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
                    uri = uri
                )
            )
        }
    }
    return list
}
