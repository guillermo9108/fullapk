package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle possible multimedia resume intents
        viewModel.handleVideoIntent(intent)
        
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                // Track if server is configured and saved
                val isConfigured = viewModel.serverConfig.isConfigured

                // Check and request all missing permissions on first launch / start
                val permissions = remember {
                    val list = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                        list.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                        list.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    list
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { resultMap ->
                    // Permissions requested directly from OS
                }

                LaunchedEffect(Unit) {
                    try {
                        val ungranted = permissions.filter {
                            ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (ungranted.isNotEmpty() && !isFinishing && !isDestroyed) {
                            permissionLauncher.launch(ungranted.toTypedArray())
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error requesting permissions checklist: ${e.message}", e)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Persistent WebView: Kept alive when navigating to other screens
                    // to prevent audio/video streams from resetting or losing credentials cookies!
                    if (isConfigured) {
                        WebViewScreen(viewModel)
                    }

                    // 2. Configuration Overlay (rendered initially if not configured yet)
                    if (currentScreen == Screen.Configuration) {
                        ConfigScreen(viewModel)
                    }

                    // 3. Downloads Fullscreen Overlay
                    if (currentScreen == Screen.Downloads) {
                        DownloadsScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleVideoIntent(intent)
    }
}
