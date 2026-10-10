@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.example.simpleplayer.SimplePlayerApp
import com.example.simpleplayer.data.Playlist
import com.example.simpleplayer.data.PlaylistSong
import com.example.simpleplayer.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@Composable
fun PlaylistTab(controller: MediaController) {

    val context = LocalContext.current
    val app = context.applicationContext as SimplePlayerApp
    val dao = app.database.playlistDao()
    val scope = rememberCoroutineScope()

    val playlists by dao.observePlaylists().collectAsState(initial = emptyList())
    val hiddenIds by dao.observeHiddenIds().collectAsState(initial = emptyList())

    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var renamingPlaylist by remember { mutableStateOf<Playlist?>(null) }

    var showFileDialog by remember { mutableStateOf(false) }
    var allFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var addingToPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var mode by remember { mutableStateOf("") }

    var openedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var openedSongs by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }

    var actionsPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deleteConfirmPlaylist by remember { mutableStateOf<Playlist?>(null) }

    var deletingMode by remember { mutableStateOf(false) }
    var orderingMode by remember { mutableStateOf(false) }

    var localSongs by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }

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
    ) { granted ->
        hasPermission = granted
        if (granted && mode.isNotEmpty()) {
            scope.launch {
                allFiles = withContext(Dispatchers.IO) { scanAudioFiles() }
                showFileDialog = true
            }
        }
    }

    LaunchedEffect(showFileDialog) {
        if (showFileDialog && hasPermission && allFiles.isEmpty()) {
            allFiles = withContext(Dispatchers.IO) { scanAudioFiles() }
        }
    }

    LaunchedEffect(openedPlaylist) {
        val pl = openedPlaylist
        if (pl != null) {
            dao.observeSongs(pl.id).collect { openedSongs = it }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists, key = { it.id }) { playlist ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { actionsPlaylist = playlist }
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Text(playlist.name)
                    }
                    HorizontalDivider()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        newName = ""
                        renamingPlaylist = null
                        addingToPlaylist = null
                        mode = ""
                        showNameDialog = true
                    }
                ) {
                    Text("Aggiungi playlist")
                }
            }
        }
    }

    // ---------- Popup azioni playlist ----------
    actionsPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { actionsPlaylist = null },
            title = { Text(playlist.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        actionsPlaylist = null
                        scope.launch {
                            val songs = dao.getSongs(playlist.id)
                            if (songs.isNotEmpty()) {
                                val items = songs.map { s ->
                                    MediaItem.Builder()
                                        .setUri(Uri.fromFile(File(s.uri)))
                                        .setMediaId(s.mediaId.toString())
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(s.title)
                                                .setArtist(s.artist)
                                                .build()
                                        )
                                        .build()
                                }
                                controller.setMediaItems(items, 0, 0L)
                                controller.prepare()
                                controller.play()
                            }
                        }
                    }) { Text("Riproduci") }

                    TextButton(onClick = {
                        actionsPlaylist = null
                        selectedIds = emptySet()
                        addingToPlaylist = playlist
                        mode = "add"
                        if (hasPermission) {
                            scope.launch {
                                if (allFiles.isEmpty()) {
                                    allFiles = withContext(Dispatchers.IO) { scanAudioFiles() }
                                }
                                showFileDialog = true
                            }
                        } else {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    Manifest.permission.READ_MEDIA_AUDIO
                                else
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }
                    }) { Text("Aggiungi brani") }

                    TextButton(onClick = {
                        actionsPlaylist = null
                        scope.launch {
                            localSongs = dao.getSongs(playlist.id)
                            openedPlaylist = playlist
                            orderingMode = true
                        }
                    }) { Text("Ordina brani") }

                    TextButton(onClick = {
                        actionsPlaylist = null
                        openedPlaylist = playlist
                        deletingMode = true
                    }) { Text("Elimina brani") }

                    TextButton(onClick = {
                        actionsPlaylist = null
                        newName = playlist.name
                        renamingPlaylist = playlist
                        showNameDialog = true
                    }) { Text("Rinomina") }

                    TextButton(onClick = {
                        actionsPlaylist = null
                        deleteConfirmPlaylist = playlist
                    }) { Text("Elimina") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionsPlaylist = null }) { Text("Chiudi") }
            }
        )
    }

    // ---------- Popup conferma eliminazione playlist ----------
    deleteConfirmPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPlaylist = null },
            title = { Text("Eliminare la playlist?") },
            text = { Text("\"${playlist.name}\" verrà eliminata definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deletePlaylist(playlist.id) }
                    deleteConfirmPlaylist = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPlaylist = null }) { Text("Annulla") }
            }
        )
    }

    // ---------- Popup brani della playlist ----------
    openedPlaylist?.let { playlist ->
        if (orderingMode) {
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                localSongs = localSongs.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }

            AlertDialog(
                onDismissRequest = {
                    scope.launch {
                        dao.reorderSongs(playlist.id, localSongs.map { it.id })
                    }
                    openedPlaylist = null
                    orderingMode = false
                },
                title = { Text("Ordina: ${playlist.name}") },
                text = {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(localSongs, key = { it.id }) { song ->
                            ReorderableItem(reorderableState, key = song.id) { _ ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.DragHandle,
                                        contentDescription = "Trascina",
                                        modifier = Modifier
                                            .draggableHandle(onDragStopped = {})
                                            .padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, maxLines = 1)
                                        Text(
                                            song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            dao.reorderSongs(playlist.id, localSongs.map { it.id })
                        }
                        openedPlaylist = null
                        orderingMode = false
                    }) { Text("Salva") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openedPlaylist = null
                        orderingMode = false
                    }) { Text("Annulla") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = {
                    openedPlaylist = null
                    deletingMode = false
                },
                title = { Text(playlist.name) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (openedSongs.isEmpty()) {
                            Text("Nessun brano")
                        } else {
                            openedSongs.forEach { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!deletingMode) {
                                                    val index = openedSongs.indexOf(song)
                                                    val items = openedSongs.map { s ->
                                                        MediaItem.Builder()
                                                            .setUri(Uri.fromFile(File(s.uri)))
                                                            .setMediaId(s.mediaId.toString())
                                                            .setMediaMetadata(
                                                                MediaMetadata.Builder()
                                                                    .setTitle(s.title)
                                                                    .setArtist(s.artist)
                                                                    .build()
                                                            )
                                                            .build()
                                                    }
                                                    controller.setMediaItems(items, index, 0L)
                                                    controller.prepare()
                                                    controller.play()
                                                }
                                            }
                                    ) {
                                        Text(song.title, maxLines = 1)
                                        Text(
                                            song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                    if (deletingMode) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                dao.removeSong(playlist.id, song.mediaId)
                                            }
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Rimuovi")
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        openedPlaylist = null
                        deletingMode = false
                    }) { Text("Chiudi") }
                }
            )
        }
    }

    // ---------- Popup nome ----------
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(if (renamingPlaylist == null) "Nuova playlist" else "Rinomina") },
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
                        val renaming = renamingPlaylist
                        if (renaming != null) {
                            scope.launch { dao.renamePlaylist(renaming.id, newName) }
                        } else {
                            selectedIds = emptySet()
                            addingToPlaylist = null
                            mode = "create"
                            if (hasPermission) {
                                scope.launch {
                                    if (allFiles.isEmpty()) {
                                        allFiles = withContext(Dispatchers.IO) { scanAudioFiles() }
                                    }
                                    showFileDialog = true
                                }
                            } else {
                                permissionLauncher.launch(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    else
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                            }
                        }
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Annulla") }
            }
        )
    }

    // ---------- Popup selezione file ----------
    if (showFileDialog) {
        val visibleFiles = allFiles.filter { it.id !in hiddenIds }

        AlertDialog(
            onDismissRequest = {
                showFileDialog = false
                addingToPlaylist = null
                mode = ""
            },
            title = {
                Text(
                    if (mode == "add") "Aggiungi a ${addingToPlaylist?.name}"
                    else "Scegli i brani"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (visibleFiles.isEmpty()) {
                        Text("Nessun file disponibile")
                    } else {
                        visibleFiles.forEach { file ->
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
                                        style = MaterialTheme.typography.bodySmall,
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
                                uri = f.path ?: f.uri.toString(),
                                path = f.path,
                                position = 0
                            )
                        }

                    val currentMode = mode
                    val adding = addingToPlaylist

                    scope.launch {
                        if (currentMode == "add" && adding != null) {
                            dao.addSongsToPlaylist(adding.id, chosen)
                        } else {
                            dao.createPlaylistWithSongs(newName, chosen)
                        }
                    }

                    showFileDialog = false
                    addingToPlaylist = null
                    mode = ""
                }) { Text("Fatto") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFileDialog = false
                    addingToPlaylist = null
                    mode = ""
                }) { Text("Annulla") }
            }
        )
    }
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
