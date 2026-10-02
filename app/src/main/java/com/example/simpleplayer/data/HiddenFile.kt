package com.example.simpleplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_files")
data class HiddenFile(
    @PrimaryKey
    val mediaId: Long
)
