package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String, // UUID
    val filename: String,
    val url: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val status: String, // "COMPLETED", "DOWNLOADING", "FAILED"
    val progress: Int, // 0-100
    val localUri: String? // Absolute local file path
)
