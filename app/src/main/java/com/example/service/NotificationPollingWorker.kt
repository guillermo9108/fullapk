package com.example.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.pref.ServerConfig
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request

class NotificationPollingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val serverConfig = ServerConfig(applicationContext)
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    override suspend fun doWork(): Result {
        if (!serverConfig.isConfigured) {
            return Result.success()
        }

        val baseUrl = serverConfig.fullUrl
        if (baseUrl.isBlank()) {
            return Result.success()
        }

        // Try getting cookies and userId from saved settings
        var cookies = serverConfig.lastSavedCookies
        var userId = serverConfig.lastSavedUserId

        // Fallback: check CookieManager if on Main thread or accessible
        if (cookies.isBlank()) {
            try {
                cookies = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.webkit.CookieManager.getInstance().getCookie(baseUrl) ?: ""
                }
            } catch (e: Exception) {
                Log.e("PollingWorker", "CookieManager read failed: ${e.message}")
            }
        }

        if (userId.isBlank() && cookies.isNotEmpty()) {
            val cookieList = cookies.split(";")
            for (cookie in cookieList) {
                val parts = cookie.trim().split("=")
                if (parts.size >= 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (key.equals("userId", ignoreCase = true) || 
                        key.equals("user_id", ignoreCase = true) || 
                        key.equals("uid", ignoreCase = true) || 
                        key.equals("id", ignoreCase = true)) {
                        userId = value
                        break
                    }
                }
            }
        }

        if (userId.isBlank()) {
            Log.d("PollingWorker", "No userId found. Skipping background polling.")
            return Result.success()
        }

        createNotificationChannel()

        // 1. Poll Notifications
        try {
            val url = "$baseUrl/api/index.php?action=get_unread_notifications&userId=$userId"
            val request = Request.Builder()
                .url(url)
                .header("Cookie", cookies)
                .header("User-Agent", "StreamPayAPK/1.0-Background")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val adapter = moshi.adapter(Map::class.java)
                    val responseMap = adapter.fromJson(bodyString) as? Map<String, Any>
                    val success = responseMap?.get("success") as? Boolean ?: false
                    
                    if (success) {
                        val dataList = responseMap["data"] as? List<Map<String, Any>>
                        if (dataList != null) {
                            val maxTimestampPolled = serverConfig.lastPolledNotificationTime
                            var newMaxTimestamp = maxTimestampPolled
                            
                            for (item in dataList) {
                                val text = item["text"] as? String ?: ""
                                val type = item["type"] as? String ?: "SYSTEM"
                                val id = when (val rawId = item["id"]) {
                                    is Double -> rawId.toLong().toString()
                                    is Long -> rawId.toString()
                                    is Int -> rawId.toString()
                                    else -> rawId?.toString() ?: ""
                                }
                                val timestampSec = when (val rawTime = item["timestamp"]) {
                                    is Double -> rawTime.toLong()
                                    is Long -> rawTime
                                    is Int -> rawTime.toLong()
                                    is String -> rawTime.toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                                
                                if (timestampSec > maxTimestampPolled) {
                                    if (timestampSec > newMaxTimestamp) {
                                        newMaxTimestamp = timestampSec
                                    }
                                    showSystemNotification(id.hashCode(), "StreamPay: $type", text)
                                }
                            }
                            if (newMaxTimestamp > maxTimestampPolled) {
                                serverConfig.lastPolledNotificationTime = newMaxTimestamp
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PollingWorker", "Failed to poll notifications in background: ${e.message}")
        }

        // 2. Poll Chat Messages
        val actions = listOf("get_unread_messages", "get_unread_chat")
        for (action in actions) {
            try {
                val url = "$baseUrl/api/index.php?action=$action&userId=$userId"
                val request = Request.Builder()
                    .url(url)
                    .header("Cookie", cookies)
                    .header("User-Agent", "StreamPayAPK/1.0-Background")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val adapter = moshi.adapter(Map::class.java)
                        val responseMap = adapter.fromJson(bodyString) as? Map<String, Any>?
                        val success = responseMap?.get("success") as? Boolean ?: false
                        
                        if (success) {
                            val dataList = responseMap["data"] as? List<Map<String, Any>>
                            if (dataList != null && dataList.isNotEmpty()) {
                                val maxPolledChat = serverConfig.lastPolledChatTime
                                var newMaxChat = maxPolledChat
                                
                                for (item in dataList) {
                                    val text = item["text"] as? String
                                        ?: item["message"] as? String
                                        ?: item["content"] as? String
                                        ?: item["body"] as? String ?: ""
                                    if (text.isBlank()) continue
                                    
                                    val sender = item["sender"] as? String
                                        ?: item["username"] as? String
                                        ?: item["from_user"] as? String ?: "Chat"
                                        
                                    val id = when (val rawId = item["id"]) {
                                        is Double -> rawId.toLong().toString()
                                        is Long -> rawId.toString()
                                        is Int -> rawId.toString()
                                        else -> rawId?.toString() ?: ""
                                    }
                                    val timestampSec = when (val rawTime = item["timestamp"]) {
                                        is Double -> rawTime.toLong()
                                        is Long -> rawTime
                                        is Int -> rawTime.toLong()
                                        is String -> rawTime.toLongOrNull() ?: 0L
                                        else -> 0L
                                    }
                                    
                                    if (timestampSec > maxPolledChat) {
                                        if (timestampSec > newMaxChat) {
                                            newMaxChat = timestampSec
                                        }
                                        showSystemNotification(id.hashCode() + 100000, "StreamPay Chat: $sender", text)
                                    }
                                }
                                
                                if (newMaxChat > maxPolledChat) {
                                    serverConfig.lastPolledChatTime = newMaxChat
                                }
                                return@use
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PollingWorker", "Failed to poll chat action $action in background: ${e.message}")
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "streampay_alerts",
                "Notificaciones StreamPay",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de actividades, transmisiones y recargas"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showSystemNotification(id: Int, title: String, content: String) {
        val builder = NotificationCompat.Builder(applicationContext, "streampay_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            
        try {
            val manager = NotificationManagerCompat.from(applicationContext)
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.e("PollingWorker", "SecurityException posting notification: ${e.message}")
        } catch (e: Exception) {
            Log.e("PollingWorker", "Error posting notification: ${e.message}")
        }
    }
}
