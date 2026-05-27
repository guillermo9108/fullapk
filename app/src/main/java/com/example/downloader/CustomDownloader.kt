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
        
        var finalFilename = filename
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

                // Resolve filename and extension from headers
                val disposition = connection.getHeaderField("Content-Disposition")
                val contentType = connection.getHeaderField("Content-Type")
                
                var ext: String? = null
                if (contentType != null) {
                    ext = when {
                        contentType.startsWith("video/mp4", ignoreCase = true) -> "mp4"
                        contentType.startsWith("video/webm", ignoreCase = true) -> "webm"
                        contentType.startsWith("video/ogg", ignoreCase = true) -> "ogv"
                        contentType.startsWith("video/3gpp", ignoreCase = true) -> "3gp"
                        contentType.startsWith("video/quicktime", ignoreCase = true) -> "mov"
                        contentType.startsWith("video/x-matroska", ignoreCase = true) -> "mkv"
                        contentType.startsWith("video/", ignoreCase = true) -> {
                            val subtype = contentType.substringAfter("video/").substringBefore(";").trim()
                            if (subtype.isNotBlank() && subtype.length < 5) subtype else "mp4"
                        }
                        contentType.startsWith("audio/mpeg", ignoreCase = true) -> "mp3"
                        contentType.startsWith("audio/ogg", ignoreCase = true) -> "ogg"
                        contentType.startsWith("audio/wav", ignoreCase = true) -> "wav"
                        contentType.startsWith("audio/webm", ignoreCase = true) -> "weba"
                        contentType.startsWith("audio/aac", ignoreCase = true) -> "aac"
                        contentType.startsWith("audio/flac", ignoreCase = true) -> "flac"
                        contentType.startsWith("audio/", ignoreCase = true) -> {
                            val subtype = contentType.substringAfter("audio/").substringBefore(";").trim()
                            if (subtype.isNotBlank() && subtype.length < 5) subtype else "mp3"
                        }
                        contentType.startsWith("image/jpeg", ignoreCase = true) -> "jpg"
                        contentType.startsWith("image/png", ignoreCase = true) -> "png"
                        contentType.startsWith("image/gif", ignoreCase = true) -> "gif"
                        contentType.startsWith("image/webp", ignoreCase = true) -> "webp"
                        contentType.startsWith("image/", ignoreCase = true) -> {
                            val subtype = contentType.substringAfter("image/").substringBefore(";").trim()
                            if (subtype.isNotBlank() && subtype.length < 5) subtype else "jpg"
                        }
                        else -> null
                    }
                }

                if (disposition != null) {
                    val filenameRegex = """filename\*=\s*utf-8''([^;\n]+)|filename=\s*["']?([^;"'\n]+)["']?""".toRegex(RegexOption.IGNORE_CASE)
                    val match = filenameRegex.find(disposition)
                    if (match != null) {
                        val parsed = match.groupValues[1].takeIf { it.isNotBlank() }
                            ?: match.groupValues[2].takeIf { it.isNotBlank() }
                        if (parsed != null) {
                            try {
                                finalFilename = java.net.URLDecoder.decode(parsed, "UTF-8")
                            } catch (e: Exception) {
                                finalFilename = parsed
                            }
                        }
                    }
                }

                val lowerName = finalFilename.lowercase()
                if (lowerName == "index.php" || lowerName == "index.html" || lowerName == "index.htm" || finalFilename.isBlank()) {
                    val nameBase = "streampay_download_${System.currentTimeMillis()}"
                    finalFilename = if (ext != null) "$nameBase.$ext" else "$nameBase.mp4"
                } else if (ext != null && (lowerName.endsWith(".php") || !lowerName.contains(".") || lowerName.endsWith(".html") || lowerName.endsWith(".htm"))) {
                    val baseName = if (finalFilename.contains(".")) finalFilename.substringBeforeLast(".") else finalFilename
                    finalFilename = "$baseName.$ext"
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
                val outputFile = File(destDir, finalFilename)
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
                            filename = finalFilename,
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
                    filename = finalFilename,
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
                    filename = finalFilename,
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
                    val partFile = File(destDir, finalFilename)
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
