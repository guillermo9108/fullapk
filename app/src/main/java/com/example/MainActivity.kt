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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.service.NotificationPollingWorker
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.screens.Slate950
import com.example.ui.screens.Indigo500
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle possible multimedia resume intents
        viewModel.handleVideoIntent(intent)
        handleSendIntent(intent)

        // Schedule periodic background notification and chat polling worker
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val pollingRequest = PeriodicWorkRequestBuilder<NotificationPollingWorker>(
                15, TimeUnit.MINUTES
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "StreamPayNotificationPolling",
                ExistingPeriodicWorkPolicy.KEEP,
                pollingRequest
            )
            Log.d("MainActivity", "Successfully enqueued unique periodic polling worker!")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to schedule background WorkManager: ${e.message}", e)
        }
        
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                // Track if server is configured and saved
                val isConfigured = viewModel.serverConfig.isConfigured

                // Check and request all missing permissions on first launch / start
                val permissions = remember {
                    val list = mutableListOf<String>()
                    list.add(android.Manifest.permission.CAMERA)
                    list.add(android.Manifest.permission.RECORD_AUDIO)
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

                    // 4. Shared Media Upload Confirm Popup Dialog
                    val sharedUris by viewModel.sharedFileUris.collectAsState()
                    val uploadStatus by viewModel.uploadStatus.collectAsState()
                    
                    if (sharedUris.isNotEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val singleSharedUri = sharedUris.first()
                        val fileNameAndSize = remember(sharedUris) {
                            if (sharedUris.size == 1) {
                                viewModel.getFileNameAndSize(context, singleSharedUri)
                            } else {
                                Pair("${sharedUris.size} archivos seleccionados", 0L)
                            }
                        }
                        
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { viewModel.clearSharedFiles() }
                        ) {
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = Slate950
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (sharedUris.size > 1) "Subir Archivos Compartidos" else "Subir Archivo Compartido",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Indigo500,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = fileNameAndSize.first,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    
                                    if (sharedUris.size == 1 && fileNameAndSize.second > 0) {
                                        val mb = fileNameAndSize.second / (1024.0 * 1024.0)
                                        val sizeFormatted = String.format("%.2f MB", mb)
                                        Text(
                                            text = sizeFormatted,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    when {
                                        uploadStatus == "UPLOADING" -> {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                color = Indigo500
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Subiendo archivo al servidor...",
                                                color = Color.White
                                            )
                                        }
                                        uploadStatus?.startsWith("UPLOADING_PROGRESS:") == true -> {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                color = Indigo500
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val parts = uploadStatus!!.split(":")
                                            val currentIdx = parts.getOrNull(1) ?: "1"
                                            val totalIdx = parts.getOrNull(2) ?: "1"
                                            Text(
                                                text = "Subiendo archivo $currentIdx de $totalIdx...",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        uploadStatus == "SUCCESS" -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.Green,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "¡Subida exitosa!",
                                                color = Color.Green,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        uploadStatus?.startsWith("ERROR") == true -> {
                                            Text(
                                                text = "Error al subir",
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = uploadStatus!!.substringAfter("ERROR: "),
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            androidx.compose.material3.Button(
                                                onClick = { viewModel.clearSharedFiles() },
                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                    containerColor = Color.Gray
                                                )
                                            ) {
                                                Text("Cerrar")
                                            }
                                        }
                                        else -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { viewModel.clearSharedFiles() },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Cancelar")
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                androidx.compose.material3.Button(
                                                    onClick = { viewModel.uploadSharedFiles() },
                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                        containerColor = Indigo500
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Subir", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleVideoIntent(intent)
        handleSendIntent(intent)
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri
            }
            if (uri != null) {
                viewModel.handleSharedFiles(listOf(uri))
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            }
            if (uris != null) {
                viewModel.handleSharedFiles(uris.filterNotNull())
            }
        }
    }
}
