package dev.reelbox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Message
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.reelbox.data.AdBlocker
import dev.reelbox.data.StreamingServers
import dev.reelbox.ui.theme.Accent
import dev.reelbox.ui.theme.InkElevated
import dev.reelbox.ui.theme.TextPrimary
import dev.reelbox.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private enum class GestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
}

private data class GestureHudState(
    val type: GestureType,
    val percent: Float, // 0.0f .. 1.0f
    val label: String,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    type: String,
    id: Int,
    title: String,
    season: Int = 1,
    episode: Int = 1,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val servers = StreamingServers.ALL
    var serverIndex by remember { mutableIntStateOf(0) }
    val selectedServer = servers.getOrElse(serverIndex) { StreamingServers.DEFAULT }

    var currentUrl by remember(selectedServer, type, id, season, episode) {
        mutableStateOf(selectedServer.buildUrl(type, id, season, episode))
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var allServersFailed by remember { mutableStateOf(false) }

    // Screen dimensions for gesture partitioning
    var screenWidth by remember { mutableIntStateOf(0) }
    var screenHeight by remember { mutableIntStateOf(0) }

    // Audio & Volume controls
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var volumeAccumulator by remember {
        mutableFloatStateOf(currentVolume.toFloat())
    }

    // Brightness controls
    var brightnessLevel by remember {
        val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val initial = if (windowBrightness >= 0f) {
            windowBrightness
        } else {
            runCatching {
                val sys = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                sys / 255f
            }.getOrDefault(0.5f)
        }
        mutableFloatStateOf(initial.coerceIn(0.02f, 1.0f))
    }

    // Gesture indicator HUD (Brightness / Volume / Seek pill)
    var gestureHud by remember { mutableStateOf<GestureHudState?>(null) }
    var lastHudUpdate by remember { mutableLongStateOf(0L) }

    // Double-tap tracking
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapOffset by remember { mutableStateOf(Offset.Zero) }

    fun applyBrightness(deltaFraction: Float) {
        val newBrightness = (brightnessLevel + deltaFraction).coerceIn(0.02f, 1.0f)
        brightnessLevel = newBrightness
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = newBrightness
            win.attributes = lp
        }
        gestureHud = GestureHudState(
            type = GestureType.BRIGHTNESS,
            percent = newBrightness,
            label = "Brightness ${(newBrightness * 100).toInt()}%",
        )
        lastHudUpdate = System.currentTimeMillis()
    }

    fun applyVolume(deltaFraction: Float) {
        val newAcc = (volumeAccumulator + deltaFraction * maxVolume).coerceIn(0f, maxVolume.toFloat())
        volumeAccumulator = newAcc
        val newVol = newAcc.roundToInt().coerceIn(0, maxVolume)
        if (newVol != currentVolume) {
            currentVolume = newVol
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        }
        val pct = newAcc / maxVolume.toFloat()
        gestureHud = GestureHudState(
            type = GestureType.VOLUME,
            percent = pct,
            label = "Volume ${(pct * 100).toInt()}%",
        )
        lastHudUpdate = System.currentTimeMillis()
    }

    fun seekRelative(seconds: Int) {
        webViewRef?.evaluateJavascript(
            """
            (function() {
                var v = document.querySelector('video');
                if (v) {
                    v.currentTime = Math.max(0, v.currentTime + ($seconds));
                }
            })();
            """.trimIndent(),
            null,
        )
        val sign = if (seconds > 0) "+$seconds" else "$seconds"
        statusNotice = "${sign}s"
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        val js = if (isPlaying) {
            "var v = document.querySelector('video'); if (v) v.play();"
        } else {
            "var v = document.querySelector('video'); if (v) v.pause();"
        }
        webViewRef?.evaluateJavascript(js, null)
    }

    // Automatic server failover helper
    fun tryNextServer(reason: String) {
        if (serverIndex < servers.size - 1) {
            val failedName = selectedServer.name
            serverIndex += 1
            val next = servers[serverIndex]
            statusNotice = "$failedName unavailable ($reason). Auto-switching to ${next.name}..."
            currentUrl = next.buildUrl(type, id, season, episode)
            allServersFailed = false
        } else {
            allServersFailed = true
            statusNotice = null
        }
    }

    // Auto-dismiss status notice after 3.5 seconds
    LaunchedEffect(statusNotice) {
        if (statusNotice != null) {
            delay(3500)
            statusNotice = null
        }
    }

    // Auto-dismiss gesture indicator HUD 1.2 seconds after gesture ends
    LaunchedEffect(lastHudUpdate) {
        if (lastHudUpdate > 0) {
            delay(1200)
            gestureHud = null
        }
    }

    // Auto-hide top controls HUD after 4.5 seconds of user inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4500)
            showControls = false
        }
    }

    fun exitPlayer() {
        // Reset window brightness override to system default
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            win.attributes = lp
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            ?.show(WindowInsetsCompat.Type.systemBars())
        onBack()
    }

    // Exit fullscreen custom view on back press if active, or exit player
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            exitPlayer()
        }
    }

    // Automatically rotate to Landscape mode and enter immersive fullscreen on entry
    DisposableEffect(activity) {
        val insetsController = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                destroy()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                screenWidth = size.width
                screenHeight = size.height
            },
    ) {
        // Video WebView with AdBlocker and Server Failover
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = false
                        allowContentAccess = false

                        // Block popup windows initiated by scripts
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)

                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress / 100f
                            isLoading = newProgress < 100
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customView = null
                            customViewCallback = null
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?,
                        ): Boolean {
                            // Block popup/popunder creation
                            return false
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            allServersFailed = false
                            // Pre-inject AdBlocker & autoplay rules early
                            view?.evaluateJavascript(AdBlocker.INJECTION_SCRIPT, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            // Inject AdBlocker and auto-trigger video playback
                            view?.evaluateJavascript(AdBlocker.INJECTION_SCRIPT, null)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): WebResourceResponse? {
                            val requestUrl = request?.url?.toString() ?: return null
                            // Filter out ad networks, popunders, and trackers
                            if (AdBlocker.isAd(requestUrl)) {
                                return AdBlocker.emptyResource()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val targetUri = request?.url ?: return false
                            val scheme = targetUri.scheme?.lowercase() ?: ""

                            // Block external intent schemes (market://, intent://, app links, etc.)
                            if (scheme != "http" && scheme != "https") {
                                return true
                            }

                            val targetUrl = targetUri.toString()
                            if (AdBlocker.isAd(targetUrl)) {
                                return true
                            }

                            val targetHost = targetUri.host.orEmpty().lowercase()
                            val allowedHosts = listOf(
                                "vidcore",
                                "pages.dev",
                                "vidlove",
                                "cinemaos",
                                "ythd",
                                "xpass",
                                "autoembed",
                                "webtor",
                            )

                            val isAllowed = allowedHosts.any { targetHost.contains(it) }
                            return !isAllowed
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                tryNextServer("Connection Error")
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                val code = errorResponse?.statusCode ?: 0
                                if (code >= 400) {
                                    isLoading = false
                                    tryNextServer("HTTP $code")
                                }
                            }
                        }
                    }

                    loadUrl(currentUrl)
                    webViewRef = this
                }
            },
            update = { view ->
                if (view.url != currentUrl) {
                    view.loadUrl(currentUrl)
                }
            },
        )

        // Custom Fullscreen View from HTML5 Video
        if (customView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    FrameLayout(it).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        customView?.let { cv ->
                            (cv.parent as? ViewGroup)?.removeView(cv)
                            addView(
                                cv,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }
                    }
                },
            )
        }

        // Gesture Overlay Layer: Handles Brightness, Volume, Single Tap & Double Tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidth, screenHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startOffset = down.position
                        var isDragging = false
                        val gestureType = if (screenWidth > 0 && startOffset.x < screenWidth * 0.5f) {
                            GestureType.BRIGHTNESS
                        } else {
                            GestureType.VOLUME
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                if (!isDragging) {
                                    // Tap or Double Tap detection
                                    val now = System.currentTimeMillis()
                                    val timeDiff = now - lastTapTime
                                    val dist = (startOffset - lastTapOffset).getDistance()

                                    if (timeDiff < 320 && dist < 120f) {
                                        // Double tap: seek
                                        if (screenWidth > 0 && startOffset.x < screenWidth * 0.5f) {
                                            seekRelative(-10)
                                        } else {
                                            seekRelative(10)
                                        }
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = now
                                        lastTapOffset = startOffset
                                        showControls = !showControls
                                    }
                                }
                                break
                            }

                            val totalDragY = change.position.y - startOffset.y
                            if (!isDragging && abs(totalDragY) > viewConfiguration.touchSlop) {
                                isDragging = true
                            }

                            if (isDragging) {
                                change.consume()
                                val deltaY = -(change.position.y - change.previousPosition.y)
                                val sweepFraction = deltaY / (screenHeight * 0.55f).coerceAtLeast(100f)

                                if (gestureType == GestureType.BRIGHTNESS) {
                                    applyBrightness(sweepFraction)
                                } else if (gestureType == GestureType.VOLUME) {
                                    applyVolume(sweepFraction)
                                }
                            }
                        }
                    }
                },
        )

        // Top loading progress
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Accent,
                trackColor = Color.Transparent,
            )
        }

        // Gesture HUD Indicator (Brightness / Volume Capsule)
        AnimatedVisibility(
            visible = gestureHud != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            gestureHud?.let { hud ->
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(170.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        val icon = when (hud.type) {
                            GestureType.BRIGHTNESS -> {
                                if (hud.percent > 0.66f) Icons.Filled.BrightnessHigh
                                else if (hud.percent > 0.33f) Icons.Filled.BrightnessMedium
                                else Icons.Filled.BrightnessLow
                            }
                            GestureType.VOLUME -> {
                                if (hud.percent > 0.5f) Icons.AutoMirrored.Filled.VolumeUp
                                else if (hud.percent > 0.01f) Icons.AutoMirrored.Filled.VolumeDown
                                else Icons.AutoMirrored.Filled.VolumeMute
                            }
                            else -> Icons.Filled.BrightnessMedium
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(32.dp),
                        )

                        Spacer(Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { hud.percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Accent,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = hud.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Auto-switch / Seek Status Pill Notification
        AnimatedVisibility(
            visible = statusNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.6f)),
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = statusNotice.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // All Servers Failed Overlay
        if (allServersFailed && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Unable to Stream",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "All streaming sources are currently unresponsive for this title.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                serverIndex = 0
                                allServersFailed = false
                                currentUrl = servers[0].buildUrl(type, id, season, episode)
                                webViewRef?.loadUrl(currentUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Retry from Server 1")
                        }

                        Button(
                            onClick = { exitPlayer() },
                            colors = ButtonDefaults.buttonColors(containerColor = InkElevated),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        // Center Controls: Rewind 10s, Play/Pause, Forward 10s
        AnimatedVisibility(
            visible = showControls && customView == null && !allServersFailed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { seekRelative(-10) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        Icons.Filled.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }

                IconButton(
                    onClick = { togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Accent),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }

                IconButton(
                    onClick = { seekRelative(10) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        Icons.Filled.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }

        // Top overlay header controls
        AnimatedVisibility(
            visible = showControls && customView == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.85f),
                            0.7f to Color.Black.copy(alpha = 0.6f),
                            1f to Color.Transparent,
                        ),
                    )
                    .statusBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { exitPlayer() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = if (type == "tv") "$title (S${season}E${episode})" else title.ifBlank { "Playing" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reload player",
                            tint = TextSecondary,
                        )
                    }

                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, currentUrl.toUri()),
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.OpenInBrowser,
                            contentDescription = "Open in browser",
                            tint = TextSecondary,
                        )
                    }
                }

                // Server Switcher Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Text(
                            text = "SERVER:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }

                    itemsIndexed(servers, key = { _, server -> server.id }) { index, server ->
                        val isSelected = index == serverIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    serverIndex = index
                                    allServersFailed = false
                                    statusNotice = "Switched to ${server.name}"
                                    currentUrl = server.buildUrl(type, id, season, episode)
                                }
                            },
                            label = {
                                Text(
                                    text = server.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = InkElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = Accent,
                                selectedLabelColor = Color.White,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Accent else Color.Transparent,
                                selectedBorderColor = Accent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }
        }
    }
}
