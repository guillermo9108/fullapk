package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.pref.ServerConfig
import com.example.data.repository.DownloadRepository
import com.example.service.MediaPlaybackService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import android.net.Uri
import android.provider.OpenableColumns
import com.squareup.moshi.Moshi

enum class Screen {
    Configuration,
    WebView,
    Downloads
}

sealed class WebViewCommand {
    object RELOAD : WebViewCommand()
    object CLEAR_CACHE : WebViewCommand()
    data class NAVIGATE_AND_SEEK(val url: String, val seekTime: Double) : WebViewCommand()
}

enum class VideoControlCommand {
    PLAY,
    PAUSE
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private var activeInstance: AppViewModel? = null
        fun getActiveInstance(): AppViewModel? = activeInstance
    }

    // Active video playing state for keeping screen on
    private val _isTrackedVideoPlaying = MutableStateFlow(false)
    val isTrackedVideoPlaying: StateFlow<Boolean> = _isTrackedVideoPlaying.asStateFlow()

    // Control commands flow to send inputs from notification buttons back to WebView
    private val _videoControlCommands = MutableSharedFlow<VideoControlCommand>()
    val videoControlCommands: SharedFlow<VideoControlCommand> = _videoControlCommands.asSharedFlow()

    fun sendVideoControl(command: VideoControlCommand) {
        viewModelScope.launch {
            _videoControlCommands.emit(command)
        }
    }

    private var lastMediaTitle = ""
    private var lastMediaUrl = ""
    private var lastMediaTime = 0.0

    var currentUserId = ""
    fun updateUserId(userId: String) {
        if (userId.isNotBlank() && currentUserId != userId) {
            currentUserId = userId
            Log.d("AppViewModel", "Resolved userId from WebView: $userId")
        }
    }

    fun updateVideoState(title: String, pageUrl: String, videoSrc: String, currentTime: Double, duration: Double, isPlaying: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isTrackedVideoPlaying.value = isPlaying
            lastMediaTitle = title
            lastMediaUrl = pageUrl
            lastMediaTime = currentTime

            try {
                val context = getApplication<Application>()
                val activeService = MediaPlaybackService.getActiveInstance()
                if (activeService != null) {
                    // Update the running foreground service directly.
                    // This avoids repetitive startForegroundService calls from background which crashes on Android 12+.
                    activeService.updateNotificationState(title, pageUrl, currentTime, isPlaying)
                } else if (isPlaying) {
                    // Only start a new foreground service when video begins playing (guaranteed foreground origin context)
                    val intent = Intent(context, MediaPlaybackService::class.java).apply {
                        action = MediaPlaybackService.ACTION_UPDATE
                        putExtra("title", title)
                        putExtra("pageUrl", pageUrl)
                        putExtra("currentTime", currentTime)
                        putExtra("isPlaying", isPlaying)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to start/update MediaPlaybackService: ${e.message}")
            }
        }
    }

    fun clearVideoState() {
        viewModelScope.launch(Dispatchers.Main) {
            if (_isTrackedVideoPlaying.value) {
                _isTrackedVideoPlaying.value = false
                try {
                    val context = getApplication<Application>()
                    val intent = Intent(context, MediaPlaybackService::class.java)
                    context.stopService(intent)
                } catch (e: Exception) {
                    Log.e("AppViewModel", "Failed to stop MediaPlaybackService: ${e.message}")
                }
            }
        }
    }

    fun handleVideoIntent(intent: Intent?) {
        if (intent == null) return
        val pageUrl = intent.getStringExtra("page_url")
        val seekTime = intent.getDoubleExtra("seek_time", -1.0)
        
        if (!pageUrl.isNullOrEmpty()) {
            navigateTo(Screen.WebView)
            viewModelScope.launch {
                _webViewCommands.emit(WebViewCommand.NAVIGATE_AND_SEEK(pageUrl, seekTime))
            }
        }
    }

    private val database = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(application, database.downloadDao())
    val serverConfig = ServerConfig(application)

    // Screen navigation
    private val _currentScreen = MutableStateFlow(
        if (serverConfig.isConfigured) Screen.WebView else Screen.Configuration
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Config fields (temporary visual states during editing)
    val ipAddressState = MutableStateFlow(serverConfig.ipAddress)
    val portState = MutableStateFlow(serverConfig.port)
    val downloadLocationState = MutableStateFlow(serverConfig.downloadLocation)
    val keepCacheState = MutableStateFlow(serverConfig.keepCache)
    val cacheCleanIntervalState = MutableStateFlow(serverConfig.cacheCleanInterval)

    // Downloads
    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloadsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active downloading count
    val activeDownloadsCount: StateFlow<Int> = allDownloads
        .map { list -> list.count { it.status == "DOWNLOADING" || it.status == "QUEUED" } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Event flow for WebView actions
    private val _webViewCommands = MutableSharedFlow<WebViewCommand>()
    val webViewCommands: SharedFlow<WebViewCommand> = _webViewCommands.asSharedFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Shared media file URIs to propose for upload
    private val _sharedFileUris = MutableStateFlow<List<Uri>>(emptyList())
    val sharedFileUris: StateFlow<List<Uri>> = _sharedFileUris.asStateFlow()

    val sharedFileUri: StateFlow<Uri?> = _sharedFileUris
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    fun handleSharedFiles(uris: List<Uri>) {
        _sharedFileUris.value = uris
        _uploadStatus.value = null // clear previous status
    }

    fun handleSharedFile(uri: Uri) {
        handleSharedFiles(listOf(uri))
    }

    fun clearSharedFiles() {
        _sharedFileUris.value = emptyList()
        _uploadStatus.value = null
    }

    fun clearSharedFile() {
        clearSharedFiles()
    }

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "archivo_compartido"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        name = cursor.getString(nameIdx)
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed to resolve shared name/size: ${e.message}", e)
        }
        return Pair(name, size)
    }

    fun uploadSharedFiles() {
        val uris = _sharedFileUris.value
        if (uris.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                
                // Resolve user identification cookies to authenticate with the server
                val cookies = try {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.webkit.CookieManager.getInstance().getCookie(serverConfig.fullUrl) ?: ""
                    }
                } catch (e: Exception) {
                    ""
                }
                
                var userId = currentUserId
                if (userId.isBlank()) {
                    userId = serverConfig.lastSavedUserId
                }

                val totalCount = uris.size
                for ((index, uri) in uris.withIndex()) {
                    val fileNum = index + 1
                    if (totalCount > 1) {
                        _uploadStatus.value = "UPLOADING_PROGRESS:$fileNum:$totalCount"
                    } else {
                        _uploadStatus.value = "UPLOADING"
                    }

                    // Get filename and size
                    val resolved = getFileNameAndSize(context, uri)
                    var filename = resolved.first
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    
                    // If resolving didn't provide a valid extension, guess one
                    if (!filename.contains(".")) {
                        val ext = when {
                            mimeType.startsWith("video/mp4") -> "mp4"
                            mimeType.startsWith("video/") -> "mp4"
                            mimeType.startsWith("image/png") -> "png"
                            mimeType.startsWith("image/jpeg") -> "jpg"
                            mimeType.startsWith("image/") -> "jpg"
                            mimeType.startsWith("audio/mpeg") -> "mp3"
                            mimeType.startsWith("audio/") -> "mp3"
                            else -> "bin"
                        }
                        filename = "$filename.$ext"
                    }

                    // Read file content into a RequestBody
                    val inputStream = contentResolver.openInputStream(uri) 
                        ?: throw Exception("No se pudo leer el archivo: $filename")
                    
                    val bytes = inputStream.use { it.readBytes() }
                    
                    // Prepare multipart request body
                    val mediaType = mimeType.toMediaTypeOrNull()
                    val filePartBody = RequestBody.create(mediaType, bytes)

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("uploaded_file", filename, filePartBody)
                        .addFormDataPart("userId", userId)
                        .build()

                    // Execute the POST request to the upload API
                    val uploadUrl = "${serverConfig.fullUrl}/api/index.php?action=upload&userId=$userId"
                    val request = Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .header("Cookie", cookies)
                        .header("User-Agent", "StreamPayAPK/1.0")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Error en $filename (servidor: ${response.code})")
                        }
                    }
                }

                _uploadStatus.value = "SUCCESS"
                delay(2000)
                clearSharedFiles()
                
                // Reload standard page to show new media
                _webViewCommands.emit(WebViewCommand.RELOAD)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to upload shared media: ${e.message}", e)
                _uploadStatus.value = "ERROR: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun uploadSharedFile() {
        uploadSharedFiles()
    }

    fun saveConfig(ip: String, port: String) {
        serverConfig.ipAddress = ip
        serverConfig.port = port
        serverConfig.isConfigured = true
        // Update temp states
        ipAddressState.value = ip
        portState.value = port
        // Slide right into the live stream!
        _currentScreen.value = Screen.WebView
    }

    fun resetConfig() {
        serverConfig.resetConfig()
        ipAddressState.value = ""
        portState.value = ""
        downloadLocationState.value = ServerConfig.VAL_LOCATION_INTERNAL
        keepCacheState.value = true
        cacheCleanIntervalState.value = "24H"
        _currentScreen.value = Screen.Configuration
    }

    fun saveDownloadLocation(location: String) {
        serverConfig.downloadLocation = location
        downloadLocationState.value = location
    }

    fun saveKeepCache(keep: Boolean) {
        serverConfig.keepCache = keep
        keepCacheState.value = keep
    }

    fun saveCacheCleanInterval(interval: String) {
        serverConfig.cacheCleanInterval = interval
        cacheCleanIntervalState.value = interval
        // recheck immediately when interval changes
        checkAndPerformAutoCacheCleaning()
    }

    fun checkAndPerformAutoCacheCleaning() {
        val interval = serverConfig.cacheCleanInterval
        if (interval == "NUNCA") return

        val intervalMs = when (interval) {
            "1H" -> 3600000L
            "24H" -> 86400000L
            "7D" -> 604800000L
            else -> 86400000L
        }

        val lastClean = serverConfig.lastCacheCleanTime
        val now = System.currentTimeMillis()
        if (lastClean == 0L) {
            serverConfig.lastCacheCleanTime = now
        } else if (now - lastClean > intervalMs) {
            Log.d("AppViewModel", "Auto cache cleaning interval reached ($interval). Executing clear cache...")
            viewModelScope.launch(Dispatchers.Main) {
                _webViewCommands.emit(WebViewCommand.CLEAR_CACHE)
                serverConfig.lastCacheCleanTime = now
            }
        }
    }

    fun triggerFileDownload(url: String, filename: String? = null) {
        viewModelScope.launch {
            repository.triggerDownload(url, filename)
        }
    }

    fun triggerTestDownload() {
        // High fidelity test file download so user can play around immediately!
        // We will download a direct public test video.
        val testUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        val timestamp = System.currentTimeMillis()
        val testFilename = "streampay_demo_$timestamp.mp4"
        triggerFileDownload(testUrl, testFilename)
    }

    fun deleteItem(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item)
        }
    }

    fun clearAllHistoryAndFiles() {
        viewModelScope.launch {
            repository.clearAllHistoryAndFiles()
        }
    }

    fun clearHistoryOnly() {
        viewModelScope.launch {
            repository.clearHistoryOnly()
        }
    }

    // Commands to trigger WebView logic
    fun reloadWebPage() {
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.RELOAD)
        }
    }

    fun clearWebCache() {
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.CLEAR_CACHE)
        }
    }

    init {
        activeInstance = this
        createNotificationChannel()
        startNotificationPolling()
        checkAndPerformAutoCacheCleaning()
    }

    override fun onCleared() {
        super.onCleared()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "streampay_alerts",
                "Notificaciones StreamPay",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de actividades, transmisiones y recargas"
            }
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startNotificationPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(12000) // Poll every 12 seconds
                
                if (!serverConfig.isConfigured) continue
                
                try {
                    // 1. Resolve userId from CookieManager if available safely on the Main thread
                    val cookies = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.webkit.CookieManager.getInstance().getCookie(serverConfig.fullUrl) ?: ""
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationPolling", "CookieManager read failed: ${e.message}")
                        ""
                    }
                    
                    var userId = ""
                    if (cookies.isNotEmpty()) {
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
                        userId = currentUserId
                    }
                    
                    if (cookies.isNotEmpty()) {
                        serverConfig.lastSavedCookies = cookies
                    }
                    if (userId.isNotEmpty()) {
                        serverConfig.lastSavedUserId = userId
                    }
                    
                    // 2. Fetch unread notifications
                    val url = "${serverConfig.fullUrl}/api/index.php?action=get_unread_notifications&userId=$userId"
                    val request = Request.Builder()
                        .url(url)
                        .header("Cookie", cookies)
                        .header("User-Agent", "StreamPayAPK/1.0")
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
                                    
                                    // Process found notifications
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
                                        
                                        // If this notification is newer than our last polled checkpoint
                                        if (timestampSec > maxTimestampPolled) {
                                            if (timestampSec > newMaxTimestamp) {
                                                newMaxTimestamp = timestampSec
                                            }
                                            
                                            // Show System Notification!
                                            showSystemNotification(id.hashCode(), "StreamPay: $type", text)
                                        }
                                    }
                                    
                                    // Save the new max timestamp so we never duplicate
                                    if (newMaxTimestamp > maxTimestampPolled) {
                                        serverConfig.lastPolledNotificationTime = newMaxTimestamp
                                    }
                                }
                            }
                        }
                    }

                    // 3. Poll chat messages if userId is available
                    if (userId.isNotBlank()) {
                        pollChatMessages(userId, cookies)
                    }
                } catch (e: Exception) {
                    Log.e("NotificationPolling", "Polling failed: ${e.message}")
                }
            }
        }
    }

    private fun pollChatMessages(userId: String, cookies: String) {
        val actions = listOf("get_unread_messages", "get_unread_chat")
        for (action in actions) {
            try {
                val url = "${serverConfig.fullUrl}/api/index.php?action=$action&userId=$userId"
                val request = Request.Builder()
                    .url(url)
                    .header("Cookie", cookies)
                    .header("User-Agent", "StreamPayAPK/1.0")
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
                                break // If successful, skip other redundant endpoints
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatPolling", "Chat polling action $action failed: ${e.message}")
            }
        }
    }

    private fun showSystemNotification(id: Int, title: String, content: String) {
        val context = getApplication<Application>()
        val builder = NotificationCompat.Builder(context, "streampay_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
