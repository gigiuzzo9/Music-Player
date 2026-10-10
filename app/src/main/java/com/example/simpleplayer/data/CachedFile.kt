package com.example.simpleplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_files")
data class CachedFile(
    @PrimaryKey
    val mediaId: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: String
)
