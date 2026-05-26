package com.example.data.repository

import android.content.Context
import com.example.data.db.DownloadDao
import com.example.data.model.DownloadItem
import com.example.downloader.CustomDownloader
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val downloader = CustomDownloader(context, downloadDao)

    val allDownloadsFlow: Flow<List<DownloadItem>> = downloadDao.getAllDownloadsFlow()

    suspend fun getDownloadById(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun triggerDownload(url: String, filename: String? = null) {
        downloader.startDownload(url, filename)
    }

    suspend fun deleteDownload(download: DownloadItem) {
        // Delete the actual downloaded file from storage if present
        download.localUri?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        downloadDao.deleteDownloadById(download.id)
    }

    suspend fun clearAllHistoryAndFiles() {
        val allDownloads = downloadDao.getAllDownloads()
        for (item in allDownloads) {
            item.localUri?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        downloadDao.deleteAll()
    }

    suspend fun clearHistoryOnly() {
        downloadDao.clearHistory()
    }
}
