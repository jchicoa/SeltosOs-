package com.example.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.media.UnrestrictedVideoPlayer
import com.example.media.UnrestrictedWebHub
import com.example.model.BrowserAgentProfile
import com.example.model.DualViewModule
import com.example.model.InstalledAppInfo
import com.example.model.MapEngineMode
import com.example.model.MediaItemEntry
import com.example.model.ObdTelemetry
import com.example.model.SeltosDashboardUiState
import com.example.myq.MyQGarageScreen
import com.example.myq.MyQProtocolClient
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PemexGold
import com.example.ui.theme.PemexGreen
import com.example.ui.theme.PemexRed
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * Dual-View Split Container with Pemex Institutional theme, always-visible Map module,
 * multi-browser WebView, OBD-II animated gauges, and Any-App Multi-Window launcher.
 */
@Composable
fun DualViewContainer(
    state: SeltosDashboardUiState,
    onSelectPrimaryModule: (DualViewModule) -> Unit,
    onSelectSecondaryModule: (DualViewModule) -> Unit,
    onToggleOrientation: () -> Unit,
    onInnerRatioDelta: (Float) -> Unit,
    onSelectMedia: (MediaItemEntry) -> Unit,
    onAddCustomMedia: (String, String) -> Unit,
    onLoadM3uText: (String) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onEnterPip: () -> Unit,
    onWebUrlChange: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onSelectBrowserAgent: (BrowserAgentProfile) -> Unit,
    onOpenInExternalBrowser: (String, String?) -> Unit,
    onSelectMapMode: (MapEngineMode) -> Unit,
    onLaunchExternalNav: () -> Unit,
    onOpenObdSheet: () -> Unit,
    onMyQCommand: (MyQProtocolClient.DoorCommand) -> Unit,
    onToggleMyQLight: () -> Unit,
    onOpenMyQConfig: () -> Unit,
    onDismissMyQConfig: () -> Unit,
    onSaveMyQConfig: (com.example.model.MyQConnectionMode, String, String) -> Unit,
    onLaunchAppSplit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModuleDropdownChip("A", state.dualPrimaryModule, onSelectPrimaryModule)
                Text("+", color = PemexGreen, fontWeight = FontWeight.Bold)
                ModuleDropdownChip("B", state.dualSecondaryModule, onSelectSecondaryModule)
            }

            IconButton(
                onClick = onToggleOrientation,
                modifier = Modifier.size(44.dp).testTag("btn_dual_orientation")
            ) {
                Icon(
                    imageVector = if (state.dualHorizontalSplit) Icons.Rounded.ViewColumn else Icons.Rounded.Splitscreen,
                    contentDescription = "Orientación",
                    tint = PemexGreen
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val totalHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            if (state.dualHorizontalSplit) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(state.dualInnerRatio).fillMaxHeight()) {
                        DualModulePane(
                            module = state.dualPrimaryModule,
                            state = state,
                            onSelectMedia = onSelectMedia,
                            onAddCustomMedia = onAddCustomMedia,
                            onLoadM3uText = onLoadM3uText,
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange,
                            onEnterPip = onEnterPip,
                            onWebUrlChange = onWebUrlChange,
                            onToggleAdBlock = onToggleAdBlock,
                            onSelectBrowserAgent = onSelectBrowserAgent,
                            onOpenInExternalBrowser = onOpenInExternalBrowser,
                            onSelectMapMode = onSelectMapMode,
                            onLaunchExternalNav = onLaunchExternalNav,
                            onOpenObdSheet = onOpenObdSheet,
                            onMyQCommand = onMyQCommand,
                            onToggleMyQLight = onToggleMyQLight,
                            onOpenMyQConfig = onOpenMyQConfig,
                            onDismissMyQConfig = onDismissMyQConfig,
                            onSaveMyQConfig = onSaveMyQConfig,
                            onLaunchAppSplit = onLaunchAppSplit
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onInnerRatioDelta(dragAmount.x / totalWidth)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PemexGreen.copy(alpha = 0.75f))
                        )
                    }

                    Box(modifier = Modifier.weight(1f - state.dualInnerRatio).fillMaxHeight()) {
                        DualModulePane(
                            module = state.dualSecondaryModule,
                            state = state,
                            onSelectMedia = onSelectMedia,
                            onAddCustomMedia = onAddCustomMedia,
                            onLoadM3uText = onLoadM3uText,
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange,
                            onEnterPip = onEnterPip,
                            onWebUrlChange = onWebUrlChange,
                            onToggleAdBlock = onToggleAdBlock,
                            onSelectBrowserAgent = onSelectBrowserAgent,
                            onOpenInExternalBrowser = onOpenInExternalBrowser,
                            onSelectMapMode = onSelectMapMode,
                            onLaunchExternalNav = onLaunchExternalNav,
                            onOpenObdSheet = onOpenObdSheet,
                            onMyQCommand = onMyQCommand,
                            onToggleMyQLight = onToggleMyQLight,
                            onOpenMyQConfig = onOpenMyQConfig,
                            onDismissMyQConfig = onDismissMyQConfig,
                            onSaveMyQConfig = onSaveMyQConfig,
                            onLaunchAppSplit = onLaunchAppSplit
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(state.dualInnerRatio).fillMaxWidth()) {
                        DualModulePane(
                            module = state.dualPrimaryModule,
                            state = state,
                            onSelectMedia = onSelectMedia,
                            onAddCustomMedia = onAddCustomMedia,
                            onLoadM3uText = onLoadM3uText,
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange,
                            onEnterPip = onEnterPip,
                            onWebUrlChange = onWebUrlChange,
                            onToggleAdBlock = onToggleAdBlock,
                            onSelectBrowserAgent = onSelectBrowserAgent,
                            onOpenInExternalBrowser = onOpenInExternalBrowser,
                            onSelectMapMode = onSelectMapMode,
                            onLaunchExternalNav = onLaunchExternalNav,
                            onOpenObdSheet = onOpenObdSheet,
                            onMyQCommand = onMyQCommand,
                            onToggleMyQLight = onToggleMyQLight,
                            onOpenMyQConfig = onOpenMyQConfig,
                            onDismissMyQConfig = onDismissMyQConfig,
                            onSaveMyQConfig = onSaveMyQConfig,
                            onLaunchAppSplit = onLaunchAppSplit
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onInnerRatioDelta(dragAmount.y / totalHeight)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(56.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PemexGreen.copy(alpha = 0.75f))
                        )
                    }

                    Box(modifier = Modifier.weight(1f - state.dualInnerRatio).fillMaxWidth()) {
                        DualModulePane(
                            module = state.dualSecondaryModule,
                            state = state,
                            onSelectMedia = onSelectMedia,
                            onAddCustomMedia = onAddCustomMedia,
                            onLoadM3uText = onLoadM3uText,
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange,
                            onEnterPip = onEnterPip,
                            onWebUrlChange = onWebUrlChange,
                            onToggleAdBlock = onToggleAdBlock,
                            onSelectBrowserAgent = onSelectBrowserAgent,
                            onOpenInExternalBrowser = onOpenInExternalBrowser,
                            onSelectMapMode = onSelectMapMode,
                            onLaunchExternalNav = onLaunchExternalNav,
                            onOpenObdSheet = onOpenObdSheet,
                            onMyQCommand = onMyQCommand,
                            onToggleMyQLight = onToggleMyQLight,
                            onOpenMyQConfig = onOpenMyQConfig,
                            onDismissMyQConfig = onDismissMyQConfig,
                            onSaveMyQConfig = onSaveMyQConfig,
                            onLaunchAppSplit = onLaunchAppSplit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleDropdownChip(
    prefix: String,
    selected: DualViewModule,
    onSelect: (DualViewModule) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CarbonElevated)
                .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(prefix, color = PemexGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(selected.title, color = TelemetryWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CarbonSurface)
        ) {
            DualViewModule.entries.forEach { mod ->
                DropdownMenuItem(
                    text = { Text(mod.title, color = TelemetryWhite) },
                    onClick = {
                        onSelect(mod)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DualModulePane(
    module: DualViewModule,
    state: SeltosDashboardUiState,
    onSelectMedia: (MediaItemEntry) -> Unit,
    onAddCustomMedia: (String, String) -> Unit,
    onLoadM3uText: (String) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onEnterPip: () -> Unit,
    onWebUrlChange: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onSelectBrowserAgent: (BrowserAgentProfile) -> Unit,
    onOpenInExternalBrowser: (String, String?) -> Unit,
    onSelectMapMode: (MapEngineMode) -> Unit,
    onLaunchExternalNav: () -> Unit,
    onOpenObdSheet: () -> Unit,
    onMyQCommand: (MyQProtocolClient.DoorCommand) -> Unit,
    onToggleMyQLight: () -> Unit,
    onOpenMyQConfig: () -> Unit,
    onDismissMyQConfig: () -> Unit,
    onSaveMyQConfig: (com.example.model.MyQConnectionMode, String, String) -> Unit,
    onLaunchAppSplit: (String) -> Unit
) {
    when (module) {
        DualViewModule.MAP_VIEW -> {
            InteractiveMapPanel(
                gps = state.gps,
                mapMode = state.mapEngineMode,
                compactMode = true,
                onSelectMapMode = onSelectMapMode,
                onLaunchExternalNav = onLaunchExternalNav
            )
        }
        DualViewModule.VIDEO_PLAYER -> {
            UnrestrictedVideoPlayer(
                currentTitle = state.currentMediaTitle,
                currentUri = state.currentMediaUri,
                playlist = state.mediaPlaylist,
                brightness = state.screenBrightness,
                volume = state.mediaVolume,
                backgroundPlaybackEnabled = state.backgroundPlaybackEnabled,
                compactMode = true,
                onSelectMedia = onSelectMedia,
                onAddCustomMedia = onAddCustomMedia,
                onLoadM3uText = onLoadM3uText,
                onBrightnessChange = onBrightnessChange,
                onVolumeChange = onVolumeChange,
                onEnterPip = onEnterPip
            )
        }
        DualViewModule.WEB_STREAM -> {
            UnrestrictedWebHub(
                currentUrl = state.webCurrentUrl,
                adBlockEnabled = state.adBlockEnabled,
                browserAgent = state.browserAgent,
                installedBrowsers = state.installedBrowsers,
                showQuickCards = false,
                onUrlChange = onWebUrlChange,
                onToggleAdBlock = onToggleAdBlock,
                onSelectBrowserAgent = onSelectBrowserAgent,
                onOpenInExternalBrowser = onOpenInExternalBrowser
            )
        }
        DualViewModule.OBD_CLUSTER -> {
            ObdPerformanceClusterCard(
                obd = state.obd,
                onOpenScanner = onOpenObdSheet
            )
        }
        DualViewModule.MYQ_GARAGE -> {
            MyQGarageScreen(
                myQState = state.myQ,
                showConfigDialog = false,
                compactMode = true,
                onCommand = onMyQCommand,
                onToggleLight = onToggleMyQLight,
                onOpenConfig = onOpenMyQConfig,
                onDismissConfig = onDismissMyQConfig,
                onSaveConfig = onSaveMyQConfig
            )
        }
        DualViewModule.INSTALLED_APPS -> {
            InstalledAppsSplitLauncher(
                apps = state.installedApps,
                onLaunchAppSplit = onLaunchAppSplit
            )
        }
    }
}

@Composable
fun ObdPerformanceClusterCard(
    obd: ObdTelemetry,
    onOpenScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animRpm by animateFloatAsState(
        targetValue = (obd.rpm / 6500f).coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "obd_rpm_arc"
    )
    val animLoad by animateFloatAsState(
        targetValue = (obd.engineLoadPct / 100f).coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "obd_load_arc"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonSurface)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OBD-II", color = PemexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(8.dp))
                Text(obd.deviceName, color = SlateLabel, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CarbonElevated)
                    .border(1.dp, PemexGreen.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .clickable { onOpenScanner() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (obd.connected) "BLE" else "CONECTAR", color = PemexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedGaugeRing("${obd.rpm}", "RPM", animRpm, PemexGreen)
            AnimatedGaugeRing(
                "${obd.coolantTempC}°C",
                "TEMP",
                ((obd.coolantTempC - 20) / 100f).coerceIn(0f, 1f),
                if (obd.coolantTempC > 104) PemexRed else PemexGold
            )
            val econStr = if (obd.fuelEconomyLPer100Km > 0f) String.format("%.1f", obd.fuelEconomyLPer100Km) else "--"
            AnimatedGaugeRing(econStr, "L/100", (obd.fuelEconomyLPer100Km / 20f).coerceIn(0f, 1f), PemexRed)
            AnimatedGaugeRing("${obd.throttlePct}%", "TPS", animLoad, TelemetryWhite)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ECU ${String.format("%.1f", obd.batteryVoltage)}V",
                color = TelemetryWhite,
                fontSize = 11.sp,
                fontFamily = JetBrainsMonoFontFamily
            )
            Text(
                text = "CARGA ${obd.engineLoadPct}%",
                color = SlateLabel,
                fontSize = 11.sp,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
private fun AnimatedGaugeRing(
    valueText: String,
    label: String,
    progress: Float,
    accent: Color
) {
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.5f
            val inset = stroke / 2f
            drawArc(
                color = Color(0xFF1E2D26),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                color = TelemetryWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
            Text(label, color = SlateLabel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InstalledAppsSplitLauncher(
    apps: List<InstalledAppInfo>,
    onLaunchAppSplit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonSurface)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = PemexGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SPLIT APPS", color = TelemetryWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("MULTI-VENTANA", color = PemexGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin apps", color = SlateLabel, fontSize = 12.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 118.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val iconBitmap = remember(app.packageName) {
                        runCatching {
                            val d = context.packageManager.getApplicationIcon(app.packageName)
                            (d as? BitmapDrawable)?.bitmap ?: d.toBitmap(96, 96)
                        }.getOrNull()?.asImageBitmap()
                    }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarbonElevated)
                            .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                            .clickable { onLaunchAppSplit(app.packageName) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("split_app_${app.packageName}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = app.label, tint = PemexGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = app.label,
                            color = TelemetryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
