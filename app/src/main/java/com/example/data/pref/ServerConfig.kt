package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class ServerConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("streampay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IP_ADDRESS = "ip_address"
        private const val KEY_PORT = "port"
        private const val KEY_FULL_URL = "full_url"
        private const val KEY_IS_CONFIGURED = "is_configured"
        private const val KEY_DOWNLOAD_LOCATION = "download_location"
        private const val KEY_KEEP_CACHE = "keep_cache"
        private const val KEY_CACHE_CLEAN_INTERVAL = "cache_clean_interval"
        private const val KEY_LAST_CACHE_CLEAN = "last_cache_clean_time"
        
        // Defaults to demo or empty
        private const val DEFAULT_IP = "192.168.43.101"
        private const val DEFAULT_PORT = ""

        const val VAL_LOCATION_INTERNAL = "INTERNAL"
        const val VAL_LOCATION_SD_CARD = "SD_CARD"
    }

    var downloadLocation: String
        get() = prefs.getString(KEY_DOWNLOAD_LOCATION, VAL_LOCATION_INTERNAL) ?: VAL_LOCATION_INTERNAL
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_LOCATION, value).apply()

    var lastPolledNotificationTime: Long
        get() = prefs.getLong("last_polled_notification_time", 0L)
        set(value) = prefs.edit().putLong("last_polled_notification_time", value).apply()

    var lastPolledChatTime: Long
        get() = prefs.getLong("last_polled_chat_time", 0L)
        set(value) = prefs.edit().putLong("last_polled_chat_time", value).apply()

    var lastSavedUserId: String
        get() = prefs.getString("last_saved_user_id", "") ?: ""
        set(value) = prefs.edit().putString("last_saved_user_id", value).apply()

    var lastSavedCookies: String
        get() = prefs.getString("last_saved_cookies", "") ?: ""
        set(value) = prefs.edit().putString("last_saved_cookies", value).apply()

    var ipAddress: String
        get() = prefs.getString(KEY_IP_ADDRESS, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = prefs.edit().putString(KEY_IP_ADDRESS, value).apply()

    var port: String
        get() = prefs.getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        set(value) = prefs.edit().putString(KEY_PORT, value).apply()

    var fullUrl: String
        get() {
            val savedFull = prefs.getString(KEY_FULL_URL, "")
            if (!savedFull.isNullOrBlank()) return savedFull
            
            // Otherwise combine IP and Port
            val ip = ipAddress.trim()
            val pt = port.trim()
            val formattedIp = if (!ip.startsWith("http://") && !ip.startsWith("https://")) {
                "http://$ip"
            } else {
                ip
            }
            return if (pt.isNotBlank()) "$formattedIp:$pt" else formattedIp
        }
        set(value) = prefs.edit().putString(KEY_FULL_URL, value).apply()

    var isConfigured: Boolean
        get() = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONFIGURED, value).apply()

    var keepCache: Boolean
        get() = prefs.getBoolean(KEY_KEEP_CACHE, true) // enabled by default to save 100% of user views
        set(value) = prefs.edit().putBoolean(KEY_KEEP_CACHE, value).apply()

    var cacheCleanInterval: String
        get() = prefs.getString(KEY_CACHE_CLEAN_INTERVAL, "24H") ?: "24H" // 24 hours by default
        set(value) = prefs.edit().putString(KEY_CACHE_CLEAN_INTERVAL, value).apply()

    var lastCacheCleanTime: Long
        get() = prefs.getLong(KEY_LAST_CACHE_CLEAN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CACHE_CLEAN, value).apply()
        
    fun resetConfig() {
        prefs.edit().clear().apply()
    }
}
