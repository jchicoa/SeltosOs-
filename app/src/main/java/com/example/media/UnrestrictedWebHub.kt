package com.example.media

import android.annotation.SuppressLint
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrowserAgentProfile
import com.example.model.InstalledAppInfo
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PemexGold
import com.example.ui.theme.PemexGreen
import com.example.ui.theme.PemexRed
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite
import java.io.ByteArrayInputStream

private val AD_BLOCK_HOSTS = setOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "adservice.google.com",
    "ads.youtube.com",
    "pagead2.googlesyndication.com",
    "adnxs.com",
    "taboola.com",
    "outbrain.com"
)

private data class StreamingQuickCard(
    val title: String,
    val url: String,
    val accent: Color
)

private val QuickLaunchStreams = listOf(
    StreamingQuickCard("YouTube", "https://www.youtube.com/?app=desktop", PemexRed),
    StreamingQuickCard("Netflix", "https://www.netflix.com", PemexRed),
    StreamingQuickCard("Prime", "https://www.primevideo.com", PemexGreen),
    StreamingQuickCard("Disney+", "https://www.disneyplus.com", PemexGold),
    StreamingQuickCard("Waze", "https://www.waze.com/live-map", PemexGreen),
    StreamingQuickCard("Maps", "https://www.google.com/maps", PemexGreen)
)

/**
 * Multi-browser compatible WebView Hub:
 * - Selectable User-Agent profiles (Chrome PC, Chrome Mobile, Firefox, Safari)
 * - Direct launch into ANY installed external browser (Chrome, Brave, Edge, Firefox, Opera, Samsung)
 * - Geolocation, Widevine DRM, popup/multi-window redirects, AdBlock, HTML5 Fullscreen
 * - Never pauses playback on lifecycle switches
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UnrestrictedWebHub(
    currentUrl: String,
    adBlockEnabled: Boolean,
    browserAgent: BrowserAgentProfile = BrowserAgentProfile.CHROME_DESKTOP,
    installedBrowsers: List<InstalledAppInfo> = emptyList(),
    showQuickCards: Boolean = true,
    onUrlChange: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onSelectBrowserAgent: (BrowserAgentProfile) -> Unit = {},
    onOpenInExternalBrowser: (String, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var addressText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var showAgentMenu by remember { mutableStateOf(false) }
    var showExternalBrowserMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (wv.url != currentUrl) {
            wv.loadUrl(currentUrl)
        }
    }

    LaunchedEffect(browserAgent) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (wv.settings.userAgentString != browserAgent.userAgent) {
            wv.settings.userAgentString = browserAgent.userAgent
            wv.reload()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showQuickCards) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickLaunchStreams.forEach { card ->
                        val active = currentUrl.contains(
                            card.url.substringAfter("www.").substringBefore("/"),
                            ignoreCase = true
                        )
                        Row(
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) card.accent.copy(alpha = 0.2f) else CarbonElevated)
                                .border(
                                    1.dp,
                                    if (active) card.accent else CarbonBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    addressText = card.url
                                    onUrlChange(card.url)
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    card.title == "Maps" || card.title == "Waze" -> Icons.Rounded.Map
                                    card.title == "YouTube" -> Icons.Rounded.PlayCircle
                                    card.title == "Netflix" -> Icons.Rounded.Movie
                                    else -> Icons.Rounded.Tv
                                },
                                contentDescription = card.title,
                                tint = card.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = card.title,
                                color = TelemetryWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Address, Multi-Browser Agent Switcher & External Browser Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurface)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { webViewRef?.goBack() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás", tint = TelemetryWhite)
                }
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Recargar", tint = PemexGreen)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonElevated)
                        .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TelemetryWhite, fontSize = 12.sp),
                        cursorBrush = SolidColor(PemexGreen),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onUrlChange(addressText) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_url_input")
                    )
                }

                // Browser User-Agent Profile Selector
                Box {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarbonElevated)
                            .border(1.dp, PemexGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { showAgentMenu = true }
                            .padding(horizontal = 8.dp)
                            .testTag("btn_browser_agent"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = "Perfil Navegador", tint = PemexGreen, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(browserAgent.label, color = TelemetryWhite, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = showAgentMenu,
                        onDismissRequest = { showAgentMenu = false },
                        modifier = Modifier.background(CarbonSurface)
                    ) {
                        BrowserAgentProfile.entries.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.label, color = TelemetryWhite) },
                                onClick = {
                                    onSelectBrowserAgent(profile)
                                    showAgentMenu = false
                                }
                            )
                        }
                    }
                }

                // Open in External Browser (Chrome / Firefox / Edge / Brave / System Chooser)
                Box {
                    IconButton(
                        onClick = { showExternalBrowserMenu = true },
                        modifier = Modifier.size(44.dp).testTag("btn_open_external_browser")
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Abrir en Navegador", tint = PemexGold)
                    }
                    DropdownMenu(
                        expanded = showExternalBrowserMenu,
                        onDismissRequest = { showExternalBrowserMenu = false },
                        modifier = Modifier.background(CarbonSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Selector de Navegadores", color = PemexGreen, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onOpenInExternalBrowser(addressText, null)
                                showExternalBrowserMenu = false
                            }
                        )
                        installedBrowsers.forEach { browser ->
                            DropdownMenuItem(
                                text = { Text(browser.label, color = TelemetryWhite) },
                                onClick = {
                                    onOpenInExternalBrowser(addressText, browser.packageName)
                                    showExternalBrowserMenu = false
                                }
                            )
                        }
                    }
                }

                // AdBlock Toggle
                Row(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (adBlockEnabled) PemexGreen.copy(alpha = 0.16f) else CarbonElevated)
                        .border(
                            1.dp,
                            if (adBlockEnabled) PemexGreen else CarbonBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleAdBlock() }
                        .padding(horizontal = 8.dp)
                        .testTag("btn_adblock_toggle"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = "AdBlock",
                        tint = if (adBlockEnabled) PemexGreen else SlateLabel,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Unrestricted Multi-Browser Hardware WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setGeolocationEnabled(true)
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString = browserAgent.userAgent
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString() ?: return false
                                return if (target.startsWith("http://") || target.startsWith("https://")) {
                                    addressText = target
                                    false
                                } else {
                                    true
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (adBlockEnabled) {
                                    val host = request?.url?.host?.lowercase().orEmpty()
                                    if (AD_BLOCK_HOSTS.any { host == it || host.endsWith(".$it") }) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val temp = WebView(ctx)
                                temp.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        v: WebView?,
                                        req: WebResourceRequest?
                                    ): Boolean {
                                        req?.url?.toString()?.let { url ->
                                            addressText = url
                                            view?.loadUrl(url)
                                        }
                                        return true
                                    }
                                }
                                transport.webView = temp
                                resultMsg.sendToTarget()
                                return true
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customFullscreenView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customFullscreenView = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                            }
                        }
                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Fullscreen HTML5 Video Overlay
        customFullscreenView?.let { fsView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { fsView },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = {
                        customFullscreenView = null
                        customViewCallback?.onCustomViewHidden()
                        customViewCallback = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xAA070B09))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = TelemetryWhite)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}
