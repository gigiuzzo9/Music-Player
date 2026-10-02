package com.example.simpleplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simpleplayer.model.AudioFile

@Composable
fun PlaylistTab() {

    // Stato finto per ora — poi lo collegheremo a Room
    var playlists by remember { mutableStateOf(listOf<String>()) }

    // Popup 1: nome playlist
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Popup 2: selezione file
    var showFileDialog by remember { mutableStateOf(false) }
    var allFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    val selectedIds = remember { mutableStateOf(setOf<Long>()) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Lista playlist + pulsante "aggiungi"
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists) { name ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(name)
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

    // ---------- Popup 1: nome playlist ----------
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
                TextButton(
                    onClick = {
                        showNameDialog = false
                        if (newName.isNotBlank()) {
                            // Popup 2: scegli i file
                            // (i file veri li caricheremo con la query di FilesTab)
                            showFileDialog = true
                        }
                    }
                ) { Text("OK") }
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
                                Text(
                                    file.title,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = file.id in selectedIds.value,
                                    onCheckedChange = { checked ->
                                        selectedIds.value =
                                            if (checked) selectedIds.value + file.id
                                            else selectedIds.value - file.id
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Salva la playlist (per ora in memoria)
                        playlists = playlists + newName
                        showFileDialog = false
                        selectedIds.value = emptySet()
                    }
                ) { Text("Fatto") }
            },
            dismissButton = {
                TextButton(onClick = { showFileDialog = false }) { Text("Annulla") }
            }
        )
    }
}
