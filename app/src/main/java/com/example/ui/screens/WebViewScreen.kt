package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WebViewCommand
import com.example.ui.viewmodel.VideoControlCommand
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val fullUrl = viewModel.serverConfig.fullUrl
    val activeDownloads by viewModel.activeDownloadsCount.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableStateOf(0) }
    var isWebLoading by remember { mutableStateOf(false) }

    // Full screen video handling
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Gesture and Menu state
    var isMenuOpen by remember { mutableStateOf(false) }

    // Simple tracking of current url for refresh actions
    var currentUrl by remember { mutableStateOf(fullUrl) }

    // Command listener
    var pendingSeekTime by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.webViewCommands.collectLatest { command ->
            when (command) {
                WebViewCommand.RELOAD -> webViewRef?.reload()
                WebViewCommand.CLEAR_CACHE -> {
                    webViewRef?.clearCache(true)
                    webViewRef?.reload()
                }
                is WebViewCommand.NAVIGATE_AND_SEEK -> {
                    pendingSeekTime = command.seekTime
                    webViewRef?.loadUrl(command.url)
                }
            }
        }
    }

    // Direct playback commands listener from service buttons
    LaunchedEffect(key1 = true) {
        viewModel.videoControlCommands.collectLatest { cmd ->
            when (cmd) {
                VideoControlCommand.PLAY -> {
                    webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if(v) v.play();", null)
                }
                VideoControlCommand.PAUSE -> {
                    webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if(v) v.pause();", null)
                }
            }
        }
    }

    // Background pausing & timers suspension (supports background playback)
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPlayingState by viewModel.isTrackedVideoPlaying.collectAsState()
    DisposableEffect(lifecycleOwner, webViewRef, isPlayingState) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Do NOT pause the WebView and its timers if background video is playing!
                    if (!isPlayingState) {
                        webViewRef?.onPause()
                        webViewRef?.pauseTimers()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep screen awake whenever video is playing or custom video fullscreen overlay is shown
    val isVideoPlaying by viewModel.isTrackedVideoPlaying.collectAsState()
    val keepScreenOn = (customVideoView != null) || isVideoPlaying
    val activity = context as? Activity
    DisposableEffect(keepScreenOn) {
        if (activity != null) {
            val window = activity.window
            if (keepScreenOn) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Double backward navigation handler (Handles full screen close first, then back state)
    BackHandler(enabled = customVideoView != null || isMenuOpen || (webViewRef?.canGoBack() == true)) {
        if (customVideoView != null) {
            // Dismiss fullscreen video
            customVideoCallback?.onCustomViewHidden()
            customVideoView = null
            customVideoCallback = null
        } else if (isMenuOpen) {
            isMenuOpen = false
        } else {
            webViewRef?.goBack()
        }
    }

    // Build main content layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        if (customVideoView != null) {
            // Fullscreen video rendering overlay (Overrides general layout)
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                        
                        // Detach customVideoView from any existing parent first to prevent crashes
                        (customVideoView?.parent as? ViewGroup)?.removeView(customVideoView)
                        
                        addView(customVideoView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Web view content container
            Column(modifier = Modifier.fillMaxSize()) {
                // Inline micro progress bar
                if (isWebLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Slate950)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = webProgress / 100f)
                                .background(Indigo500)
                        )
                    }
                }

                // Native WebView wrapping container
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewRef = this

                            addJavascriptInterface(VideoBridge(
                                onStateChanged = { title, pageUrl, videoSrc, currentTime, duration, isPlaying ->
                                    viewModel.updateVideoState(title, pageUrl, videoSrc, currentTime, duration, isPlaying)
                                    
                                    // If there's a seek pending, apply it once video is detected (safely executed on main thread via post)
                                    post {
                                        if (pendingSeekTime != null) {
                                            val seekTo = pendingSeekTime
                                            pendingSeekTime = null
                                            evaluateJavascript("""
                                                (function() {
                                                    var v = document.querySelector('video');
                                                    if (v) {
                                                        v.currentTime = $seekTo;
                                                        v.play();
                                                    }
                                                })();
                                            """.trimIndent(), null)
                                        }
                                    }
                                },
                                onNoVideo = {
                                    viewModel.clearVideoState()
                                },
                                onUserResolved = { userId ->
                                    viewModel.updateUserId(userId)
                                }
                            ), "AndroidVideoBridge")

                            // Client declarations
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isWebLoading = true
                                    webProgress = 10
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isWebLoading = false
                                    webProgress = 100

                                    // Inject high-fidelity HTML5 video tracking bridge & user identity resolver
                                    view?.evaluateJavascript("""
                                        (function() {
                                            if (window.videoBridgeInjected) {
                                                try { window.tryResolveUserId(); } catch(e) {}
                                                return;
                                            }
                                            window.videoBridgeInjected = true;

                                            function tryResolveUserId() {
                                                var userId = "";
                                                try {
                                                    userId = localStorage.getItem('userId') || localStorage.getItem('user_id') || localStorage.getItem('uid') || localStorage.getItem('id');
                                                    if (!userId) {
                                                        userId = sessionStorage.getItem('userId') || sessionStorage.getItem('user_id');
                                                    }
                                                } catch(e) {}
                                                if (!userId) {
                                                    userId = window.userId || window.user_id || (window.currentUser && window.currentUser.id) || (window.user && window.user.id);
                                                }
                                                if (!userId) {
                                                    var meta = document.querySelector('meta[name="user-id"], meta[name="userId"]');
                                                    if (meta) userId = meta.content;
                                                }
                                                if (userId) {
                                                    window.AndroidVideoBridge.onUserResolved(userId.toString());
                                                }
                                            }
                                            window.tryResolveUserId = tryResolveUserId;

                                            tryResolveUserId();
                                            setInterval(tryResolveUserId, 4000);

                                            function getTrackedTitle() {
                                                var title = "";
                                                var h1 = document.querySelector('h1, h2, .video-title, [class*="title"], [id*="title"]');
                                                if (h1 && h1.innerText) title = h1.innerText.trim();
                                                if (!title) title = document.title || "Video";
                                                return title;
                                            }

                                            function notifyState(v) {
                                                if (!v) {
                                                    window.AndroidVideoBridge.onNoVideo();
                                                    return;
                                                }
                                                var isPlaying = !v.paused && !v.ended;
                                                window.AndroidVideoBridge.onVideoStateChanged(
                                                    getTrackedTitle(),
                                                    window.location.href,
                                                    v.src || "",
                                                    v.currentTime,
                                                    v.duration || 0,
                                                    isPlaying
                                                );
                                            }

                                            function setupVideoListeners(v) {
                                                if (v.hasVideoBridgeListeners) return;
                                                v.hasVideoBridgeListeners = true;

                                                var events = ['play', 'pause', 'timeupdate', 'ended', 'loadedmetadata'];
                                                events.forEach(function(evt) {
                                                    v.addEventListener(evt, function() {
                                                        notifyState(v);
                                                     });
                                                });
                                            }

                                            setInterval(function() {
                                                var v = document.querySelector('video');
                                                if (v) {
                                                     setupVideoListeners(v);
                                                     notifyState(v);
                                                } else {
                                                     window.AndroidVideoBridge.onNoVideo();
                                                }
                                            }, 1500);
                                        })();
                                    """.trimIndent(), null)
                                }

                                override fun onLoadResource(view: WebView?, url: String?) {
                                    super.onLoadResource(view, url)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    webProgress = newProgress
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    super.onShowCustomView(view, callback)
                                    if (customVideoView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    customVideoView = view
                                    customVideoCallback = callback

                                    // Auto change to vertical or horizontal sensor rotation automatically according to video size
                                    if (context is Activity) {
                                        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                    }
                                }

                                override fun onHideCustomView() {
                                    super.onHideCustomView()
                                    if (customVideoView == null) return
                                    customVideoView = null
                                    customVideoCallback?.onCustomViewHidden()
                                    customVideoCallback = null

                                    // Return orientation back to portrait
                                    if (context is Activity) {
                                        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            }

                            // Optimize rendering hardware layer
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            // Essential specifications
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                                
                                // Support offline cache persistence
                                val hasNetwork = isNetworkAvailable(ctx)
                                cacheMode = if (hasNetwork) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ONLY
                                
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                
                                val defaultUserAgent = userAgentString
                                userAgentString = if (defaultUserAgent.isNullOrBlank()) "StreamPayAPK/1.0" else "$defaultUserAgent StreamPayAPK/1.0"
                            }

                            // Cookie persistence
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Direct download redirection implementation
                            setDownloadListener { url, userAgentLocal, contentDisposition, mimetype, contentLength ->
                                val guessedFilename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                viewModel.triggerFileDownload(url, guessedFilename)
                            }

                            loadUrl(fullUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Left Edge Capsule Handle (always visible for direct layout navigation on click)
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Subtle tactile capsule showing [>] indicator in center-left edge, clicking it directly opens the menu
                    Box(
                        modifier = Modifier
                            .size(18.dp, 60.dp)
                            .clip(RoundedCornerShape(0.dp, 12.dp, 12.dp, 0.dp))
                            .background(Indigo500.copy(alpha = 0.7f))
                            .clickable { isMenuOpen = true }
                            .testTag("fab_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Abrir Menú",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Downloads Count Badge
                    if (activeDownloads > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Red500)
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-8).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeDownloads.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Modal dialog overlay implementing StreamPay modal options
            if (isMenuOpen) {
                StreamPayMenuOverlay(
                    activeDownloadsCount = activeDownloads,
                    onClose = { isMenuOpen = false },
                    onNavigateDownloads = {
                        isMenuOpen = false
                        viewModel.navigateTo(Screen.Downloads)
                    },
                    onRefresh = {
                        isMenuOpen = false
                        viewModel.reloadWebPage()
                    },
                    onClearCache = {
                        isMenuOpen = false
                        viewModel.clearWebCache()
                    },
                    onNavigateConfig = {
                        isMenuOpen = false
                        viewModel.navigateTo(Screen.Configuration)
                    }
                )
            }
        }
    }
}

@Composable
fun StreamPayMenuOverlay(
    activeDownloadsCount: Int,
    onClose: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onNavigateConfig: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Overlay container spanning full coordinates
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(enabled = false) {}, // Intercept clicks inside card
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menú StreamPay",
                            color = Slate200,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Slate700)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Slate200,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Menu Item 1: Downloads
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate700.copy(alpha = 0.3f))
                            .clickable { onNavigateDownloads() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Indigo500,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Descargas",
                            color = Slate200,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (activeDownloadsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Red500)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$activeDownloadsCount activas",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu Item 2: Recargar Pagina
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onRefresh() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Slate200,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Recargar Transmisión",
                            color = Slate200,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Menu Item 3: Limpiar Caché
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onClearCache() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Slate200,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Limpiar Caché Web",
                            color = Slate200,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Menu Item 4: Configuración
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateConfig() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Slate200,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Cambiar Dirección IP",
                            color = Slate200,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

class VideoBridge(
    private val onStateChanged: (title: String, pageUrl: String, videoSrc: String, currentTime: Double, duration: Double, isPlaying: Boolean) -> Unit,
    private val onNoVideo: () -> Unit,
    private val onUserResolved: (userId: String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onVideoStateChanged(title: String, pageUrl: String, videoSrc: String, currentTime: Double, duration: Double, isPlaying: Boolean) {
        onStateChanged(title, pageUrl, videoSrc, currentTime, duration, isPlaying)
    }

    @android.webkit.JavascriptInterface
    fun onNoVideo() {
        onNoVideo()
    }

    @android.webkit.JavascriptInterface
    fun onUserResolved(userId: String) {
        onUserResolved(userId)
    }
}

