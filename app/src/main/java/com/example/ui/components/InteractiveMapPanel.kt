package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.GpsTelemetry
import com.example.model.MapEngineMode
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PemexGold
import com.example.ui.theme.PemexGreen
import com.example.ui.theme.PemexRed
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * Always-visible Interactive Automotive Map Panel with Pemex dark styling.
 * Supports built-in Live Dark Vector Map (CartoDB/Leaflet), Google Maps Web, and Waze Live,
 * backed by a dark GPS radar coordinate grid when offline.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapPanel(
    gps: GpsTelemetry,
    mapMode: MapEngineMode,
    compactMode: Boolean = false,
    onSelectMapMode: (MapEngineMode) -> Unit,
    onLaunchExternalNav: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapWebView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(mapMode) {
        val wv = mapWebView ?: return@LaunchedEffect
        when (mapMode) {
            MapEngineMode.VECTOR_DARK -> {
                wv.loadDataWithBaseURL(
                    "https://seltosmate.local/",
                    buildDarkLeafletHtml(gps.latitude, gps.longitude),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
            MapEngineMode.GOOGLE_MAPS -> {
                wv.loadUrl("https://www.google.com/maps/@${gps.latitude},${gps.longitude},15z")
            }
            MapEngineMode.WAZE_LIVE -> {
                wv.loadUrl("https://www.waze.com/live-map?ll=${gps.latitude}%2C${gps.longitude}&zoom=15")
            }
        }
    }

    LaunchedEffect(gps.latitude, gps.longitude, gps.bearingDeg, mapMode) {
        if (mapMode == MapEngineMode.VECTOR_DARK) {
            mapWebView?.evaluateJavascript(
                "if (typeof updateCarPos === 'function') { updateCarPos(${gps.latitude}, ${gps.longitude}, ${gps.bearingDeg}); }",
                null
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .testTag("interactive_map_panel")
    ) {
        // Compact Map Mode & Coordinates Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapEngineMode.entries.forEach { mode ->
                    val selected = mode == mapMode
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PemexGreen.copy(alpha = 0.20f) else CarbonElevated)
                            .border(
                                1.dp,
                                if (selected) PemexGreen else CarbonBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectMapMode(mode) }
                            .padding(horizontal = 10.dp)
                            .testTag("map_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            color = if (selected) TelemetryWhite else SlateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!compactMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonElevated)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.GpsFixed,
                            contentDescription = null,
                            tint = if (gps.hasGpsLock) PemexGreen else PemexGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = gps.formattedCoordinates,
                            color = TelemetryWhite,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PemexRed.copy(alpha = 0.18f))
                        .border(1.dp, PemexRed.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .clickable { onLaunchExternalNav() }
                        .padding(horizontal = 10.dp)
                        .testTag("btn_map_external_split"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Navigation, contentDescription = "Split Nav", tint = PemexRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Split Nav", color = TelemetryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Map Viewport (Radar Fallback Canvas underneath + Live WebView Map on top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            OfflinePemexRadarGrid(
                gps = gps,
                modifier = Modifier.fillMaxSize()
            )

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setGeolocationEnabled(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        }
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }
                        }
                        when (mapMode) {
                            MapEngineMode.VECTOR_DARK -> loadDataWithBaseURL(
                                "https://seltosmate.local/",
                                buildDarkLeafletHtml(gps.latitude, gps.longitude),
                                "text/html",
                                "UTF-8",
                                null
                            )
                            MapEngineMode.GOOGLE_MAPS -> loadUrl("https://www.google.com/maps/@${gps.latitude},${gps.longitude},15z")
                            MapEngineMode.WAZE_LIVE -> loadUrl("https://www.waze.com/live-map?ll=${gps.latitude}%2C${gps.longitude}&zoom=15")
                        }
                        mapWebView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Coordinates Overlay Pill (bottom-left)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC070B09))
                    .border(1.dp, PemexGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${gps.formattedCoordinates} • ${gps.cardinalHeading} ${gps.bearingDeg.toInt()}°",
                    color = TelemetryWhite,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }
    }
}

@Composable
private fun OfflinePemexRadarGrid(
    gps: GpsTelemetry,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color(0xFF080D0A))) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val gridColor = Color(0xFF15241C)

        val step = 48f
        var x = 0f
        while (x < w) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += step
        }

        drawCircle(PemexGreen.copy(alpha = 0.16f), radius = minOf(w, h) * 0.22f, center = Offset(cx, cy), style = Stroke(2f))
        drawCircle(PemexGreen.copy(alpha = 0.10f), radius = minOf(w, h) * 0.40f, center = Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(PemexGreen, radius = 9f, center = Offset(cx, cy))
        drawCircle(Color.White, radius = 4f, center = Offset(cx, cy))
    }
}

private fun buildDarkLeafletHtml(lat: Double, lon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { margin:0; padding:0; width:100%; height:100%; background:#080D0A; }
            .leaflet-control-attribution { display:none !important; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: false }).setView([$lat, $lon], 15);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
              maxZoom: 19,
              subdomains: 'abcd'
            }).addTo(map);
            var marker = L.circleMarker([$lat, $lon], {
              radius: 9,
              fillColor: '#00A86B',
              color: '#F8FAFC',
              weight: 3,
              opacity: 1,
              fillOpacity: 1
            }).addTo(map);
            function updateCarPos(newLat, newLon, bearing) {
              marker.setLatLng([newLat, newLon]);
              map.panTo([newLat, newLon], { animate: true });
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
