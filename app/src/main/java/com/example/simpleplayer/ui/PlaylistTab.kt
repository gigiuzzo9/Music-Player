package com.example.simpleplayer.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import com.example.simpleplayer.data.PlaylistSong
import com.example.simpleplayer.model.AudioFile
import kotlinx.coroutines.launch

@Composable
fun PlaylistTab() {

    val context = LocalContext.current
    val app = context.applicationContext as SimplePlayerApp
    val dao = app.database.playlistDao()
    val scope = rememberCoroutineScope()

    // Playlist osservate dal database
    val playlists by dao.observePlaylists().collectAsState(initial = emptyList())

    // Popup 1: nome
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Popup 2: selezione file
    var showFileDialog by remember { mutableStateOf(false) }
    var allFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    // Permesso audio (per il popup di selezione)
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

    // Carica i file quando si apre il popup
    LaunchedEffect(showFileDialog) {
        if (showFileDialog && hasPermission) {
            allFiles = queryAudioFiles(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists, key = { it.id }) { playlist ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(playlist.name)
                    }
                    HorizontalDivider()
                }
            }

            Button(
                onClick = {
                    newName = ""
                    showNameDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Aggiungi playlist")
            }
        }
    }

    // ---------- Popup 1: nome ----------
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Nuova playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    if (newName.isNotBlank()) {
                        selectedIds = emptySet()
                        if (hasPermission) {
                            allFiles = queryAudioFiles(context)
                            showFileDialog = true
                        } else {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    Manifest.permission.READ_MEDIA_AUDIO
                                else
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Annulla") }
            }
        )
    }

    // ---------- Popup 2: selezione file ----------
    if (showFileDialog) {
        AlertDialog(
            onDismissRequest = { showFileDialog = false },
            title = { Text("Scegli i brani") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (allFiles.isEmpty()) {
                        Text("Nessun file disponibile")
                    } else {
                        allFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.title, maxLines = 1)
                                    Text(
                                        file.artist,
                                        style = androidx.compose.material3.MaterialTheme
                                            .typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                                Checkbox(
                                    checked = file.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds =
                                            if (checked) selectedIds + file.id
                                            else selectedIds - file.id
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = allFiles
                        .filter { it.id in selectedIds }
                        .map { f ->
                            PlaylistSong(
                                playlistId = 0,
                                mediaId = f.id,
                                title = f.title,
                                artist = f.artist,
                                uri = f.uri.toString(),
                                position = 0
                            )
                        }
                    scope.launch {
                        dao.createPlaylistWithSongs(newName, chosen)
                    }
                    showFileDialog = false
                }) { Text("Fatto") }
            },
            dismissButton = {
                TextButton(onClick = { showFileDialog = false }) { Text("Annulla") }
            }
        )
    }
}

private fun queryAudioFiles(context: Context): List<AudioFile> {
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
