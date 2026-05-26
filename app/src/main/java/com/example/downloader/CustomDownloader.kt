package com.example.downloader

import android.content.Context
import android.util.Log
import com.example.data.db.DownloadDao
import com.example.data.model.DownloadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CustomDownloader(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    suspend fun startDownload(downloadUrl: String, customFilename: String? = null) {
        val id = UUID.randomUUID().toString()
        val originalFilename = downloadUrl.substringAfterLast("/").substringBefore("?")
        val filename = customFilename ?: if (originalFilename.isNotBlank() && originalFilename.contains(".")) {
            originalFilename
        } else {
            "streampay_download_${System.currentTimeMillis()}.mp4"
        }
        
        // Initial insert
        val initialItem = DownloadItem(
            id = id,
            filename = filename,
            url = downloadUrl,
            timestamp = System.currentTimeMillis(),
            sizeBytes = 0,
            status = "DOWNLOADING",
            progress = 0,
            localUri = null
        )
        downloadDao.insertDownload(initialItem)
        
        withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 12000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "StreamPayAPK/1.0")
                connection.connect()
                
                if (connection.responseCode !in 200..299) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }
                
                val totalSize = connection.contentLengthLong
                val inputStream = connection.inputStream

                // Determine destination directory based on ServerConfig
                val serverConfig = com.example.data.pref.ServerConfig(context)
                val destDir = if (serverConfig.downloadLocation == com.example.data.pref.ServerConfig.VAL_LOCATION_SD_CARD) {
                    context.getExternalFilesDir(null) ?: context.filesDir
                } else {
                    context.filesDir
                }

                // Save to selected directory
                val outputFile = File(destDir, filename)
                val outputStream = FileOutputStream(outputFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0
                var lastProgressUpdate = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    val progress = if (totalSize > 0) {
                        ((totalBytesRead * 100) / totalSize).toInt()
                    } else {
                        // Guessing or indeterminate progress
                        50
                    }
                    
                    val now = System.currentTimeMillis()
                    // Throttle updates to database to limit disk overhead
                    if (now - lastProgressUpdate > 300) {
                        lastProgressUpdate = now
                        val updatedItem = DownloadItem(
                            id = id,
                            filename = filename,
                            url = downloadUrl,
                            timestamp = initialItem.timestamp,
                            sizeBytes = totalBytesRead,
                            status = "DOWNLOADING",
                            progress = progress,
                            localUri = outputFile.absolutePath
                        )
                        downloadDao.insertDownload(updatedItem)
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                // Completed State
                val completedItem = DownloadItem(
                    id = id,
                    filename = filename,
                    url = downloadUrl,
                    timestamp = initialItem.timestamp,
                    sizeBytes = outputFile.length(),
                    status = "COMPLETED",
                    progress = 100,
                    localUri = outputFile.absolutePath
                )
                downloadDao.insertDownload(completedItem)
                
            } catch (e: Exception) {
                Log.e("CustomDownloader", "Download failure: ${e.message}", e)
                val failedItem = DownloadItem(
                    id = id,
                    filename = filename,
                    url = downloadUrl,
                    timestamp = initialItem.timestamp,
                    sizeBytes = 0,
                    status = "FAILED",
                    progress = 0,
                    localUri = null
                )
                downloadDao.insertDownload(failedItem)
                
                // Cleanup partial file if failed
                try {
                    val serverConfig = com.example.data.pref.ServerConfig(context)
                    val destDir = if (serverConfig.downloadLocation == com.example.data.pref.ServerConfig.VAL_LOCATION_SD_CARD) {
                        context.getExternalFilesDir(null) ?: context.filesDir
                    } else {
                        context.filesDir
                    }
                    val partFile = File(destDir, filename)
                    if (partFile.exists()) {
                        partFile.delete()
                    }
                } catch (cleanupEx: Exception) {
                    Log.e("CustomDownloader", "Cleanup failed: ${cleanupEx.message}")
                }
            }
        }
    }
}
