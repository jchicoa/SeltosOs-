package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.UnrestrictedVideoPlayer
import com.example.media.UnrestrictedWebHub
import com.example.model.MyQDoorState
import com.example.model.SeltosDashboardUiState
import com.example.model.StageTab
import com.example.myq.MyQGarageScreen
import com.example.ui.components.DualViewContainer
import com.example.ui.components.InstalledAppsSplitLauncher
import com.example.ui.components.InteractiveMapPanel
import com.example.ui.components.LeftTelemetryDock
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
import com.example.viewmodel.SeltosTelemetryViewModel
import kotlinx.coroutines.delay

/**
 * Primary Pemex-themed Tesla-style split-screen cockpit.
 * Persists menu collapse & window layout, adapts cleanly to 800x480+,
 * and keeps Map, Video, Dual-View, Multi-Browser, and MyQ accessible.
 */
@Composable
fun TeslaSplitDashboard(
    state: SeltosDashboardUiState,
    viewModel: SeltosTelemetryViewModel,
    onEnterPip: () -> Unit,
    onRequestBleScanPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetLeftWeight by animateFloatAsState(
        targetValue = if (state.isLeftDockCollapsed) 0f else state.splitRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "split_weight"
    )

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(2600L)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. LEFT TELEMETRY & VEHICLE DOCK
                if (!state.isLeftDockCollapsed && targetLeftWeight > 0.05f) {
                    LeftTelemetryDock(
                        state = state,
                        onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                        onToggleHeadlights = viewModel::toggleHeadlights,
                        onToggleDoor = viewModel::toggleDoor,
                        onCycleAmbient = viewModel::cycleAmbientMood,
                        onOpenObdSheet = { viewModel.setObdSheetVisible(true) },
                        onBrightnessChange = viewModel::setScreenBrightness,
                        onVolumeChange = viewModel::setMediaVolume,
                        onToggleSplitCollapse = viewModel::toggleLeftDockCollapse,
                        onEnterPip = onEnterPip,
                        onToggleOledDimmer = viewModel::toggleOledNightDimmer,
                        onLaunchExternalNav = viewModel::launchExternalNavigation,
                        onTriggerGarage = {
                            val cmd = if (state.myQ.doorState == MyQDoorState.OPEN) {
                                com.example.myq.MyQProtocolClient.DoorCommand.CLOSE
                            } else {
                                com.example.myq.MyQProtocolClient.DoorCommand.OPEN
                            }
                            viewModel.triggerMyQDoor(cmd)
                        },
                        onToggleKeepAwake = viewModel::toggleKeepScreenAwake,
                        onToggleBackgroundAudio = viewModel::toggleBackgroundPlayback,
                        modifier = Modifier
                            .weight(targetLeftWeight)
                            .fillMaxHeight()
                    )

                    // Resizable Pemex Green Drag Handle
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = { viewModel.persistSplitRatio() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    viewModel.updateSplitRatio(dragAmount.x / totalWidthPx)
                                }
                            }
                            .testTag("split_drag_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PemexGreen.copy(alpha = 0.75f))
                        )
                    }
                }

                // 2. RIGHT MULTIMEDIA, MAP & APP STAGE
                Column(
                    modifier = Modifier
                        .weight(1f - targetLeftWeight)
                        .fillMaxHeight()
                        .padding(
                            top = 6.dp,
                            end = 8.dp,
                            bottom = 6.dp,
                            start = if (state.isLeftDockCollapsed) 8.dp else 0.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TopMinimalistDockBar(
                        state = state,
                        onToggleLeftDock = viewModel::toggleLeftDockCollapse,
                        onSelectTab = viewModel::selectTab,
                        onOpenObd = { viewModel.setObdSheetVisible(true) }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = state.activeTab,
                            transitionSpec = {
                                fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith
                                    fadeOut(animationSpec = androidx.compose.animation.core.tween(160))
                            },
                            label = "stage_tab_transition"
                        ) { tab ->
                            when (tab) {
                                StageTab.HOME_HUB -> {
                                    HomeHubScreen(
                                        state = state,
                                        onSelectTab = viewModel::selectTab,
                                        onOpenObdSheet = { viewModel.setObdSheetVisible(true) },
                                        onSelectMapMode = viewModel::setMapEngineMode,
                                        onLaunchExternalNav = viewModel::launchExternalNavigation
                                    )
                                }
                                StageTab.MAP_NAV -> {
                                    InteractiveMapPanel(
                                        gps = state.gps,
                                        mapMode = state.mapEngineMode,
                                        compactMode = false,
                                        onSelectMapMode = viewModel::setMapEngineMode,
                                        onLaunchExternalNav = viewModel::launchExternalNavigation
                                    )
                                }
                                StageTab.VIDEO_STREAM -> {
                                    UnrestrictedVideoPlayer(
                                        currentTitle = state.currentMediaTitle,
                                        currentUri = state.currentMediaUri,
                                        playlist = state.mediaPlaylist,
                                        brightness = state.screenBrightness,
                                        volume = state.mediaVolume,
                                        backgroundPlaybackEnabled = state.backgroundPlaybackEnabled,
                                        onSelectMedia = viewModel::selectMediaItem,
                                        onAddCustomMedia = viewModel::addCustomMediaUri,
                                        onLoadM3uText = viewModel::loadM3uPlaylistFromText,
                                        onBrightnessChange = viewModel::setScreenBrightness,
                                        onVolumeChange = viewModel::setMediaVolume,
                                        onEnterPip = onEnterPip
                                    )
                                }
                                StageTab.DUAL_VIEW -> {
                                    DualViewContainer(
                                        state = state,
                                        onSelectPrimaryModule = viewModel::setDualPrimaryModule,
                                        onSelectSecondaryModule = viewModel::setDualSecondaryModule,
                                        onToggleOrientation = viewModel::toggleDualOrientation,
                                        onInnerRatioDelta = viewModel::updateDualInnerRatio,
                                        onSelectMedia = viewModel::selectMediaItem,
                                        onAddCustomMedia = viewModel::addCustomMediaUri,
                                        onLoadM3uText = viewModel::loadM3uPlaylistFromText,
                                        onBrightnessChange = viewModel::setScreenBrightness,
                                        onVolumeChange = viewModel::setMediaVolume,
                                        onEnterPip = onEnterPip,
                                        onWebUrlChange = viewModel::navigateWebUrl,
                                        onToggleAdBlock = viewModel::toggleAdBlock,
                                        onSelectBrowserAgent = viewModel::setBrowserAgent,
                                        onOpenInExternalBrowser = viewModel::openUrlInExternalBrowser,
                                        onSelectMapMode = viewModel::setMapEngineMode,
                                        onLaunchExternalNav = viewModel::launchExternalNavigation,
                                        onOpenObdSheet = { viewModel.setObdSheetVisible(true) },
                                        onMyQCommand = viewModel::triggerMyQDoor,
                                        onToggleMyQLight = viewModel::toggleMyQGarageLight,
                                        onOpenMyQConfig = { viewModel.setMyQConfigVisible(true) },
                                        onDismissMyQConfig = { viewModel.setMyQConfigVisible(false) },
                                        onSaveMyQConfig = viewModel::configureMyQ,
                                        onLaunchAppSplit = viewModel::launchAppInSplitScreen
                                    )
                                }
                                StageTab.BROWSER_APPS -> {
                                    BrowserAndAppsStage(
                                        state = state,
                                        onWebUrlChange = viewModel::navigateWebUrl,
                                        onToggleAdBlock = viewModel::toggleAdBlock,
                                        onSelectBrowserAgent = viewModel::setBrowserAgent,
                                        onOpenInExternalBrowser = viewModel::openUrlInExternalBrowser,
                                        onLaunchAppSplit = viewModel::launchAppInSplitScreen
                                    )
                                }
                                StageTab.MYQ_GARAGE -> {
                                    MyQGarageScreen(
                                        myQState = state.myQ,
                                        showConfigDialog = state.showMyQConfigDialog,
                                        onCommand = viewModel::triggerMyQDoor,
                                        onToggleLight = viewModel::toggleMyQGarageLight,
                                        onOpenConfig = { viewModel.setMyQConfigVisible(true) },
                                        onDismissConfig = { viewModel.setMyQConfigVisible(false) },
                                        onSaveConfig = viewModel::configureMyQ
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.oledNightDimmer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
            )
        }

        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CarbonSurface)
                    .border(1.dp, PemexGreen, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.toastMessage.orEmpty(),
                    color = TelemetryWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (state.showObdSheet) {
            ObdBleScannerDialog(
                state = state,
                onDismiss = { viewModel.setObdSheetVisible(false) },
                onStartScan = {
                    onRequestBleScanPermissions()
                    viewModel.startObdBleScan()
                },
                onStopScan = viewModel::stopObdBleScan,
                onConnectDevice = { addr, name -> viewModel.connectObdBleDevice(addr, name) },
                onDisconnect = viewModel::disconnectObdBle,
                onToggleDemo = viewModel::toggleObdDemoMode
            )
        }
    }
}

@Composable
private fun TopMinimalistDockBar(
    state: SeltosDashboardUiState,
    onToggleLeftDock: () -> Unit,
    onSelectTab: (StageTab) -> Unit,
    onOpenObd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonSurface)
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Always-visible Saved Menu Collapse/Expand Toggle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (state.isLeftDockCollapsed) PemexGreen.copy(alpha = 0.22f) else CarbonElevated)
                .border(
                    1.dp,
                    if (state.isLeftDockCollapsed) PemexGreen else CarbonBorder,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onToggleLeftDock() }
                .testTag("btn_top_menu_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.isLeftDockCollapsed) Icons.Rounded.Menu else Icons.Rounded.MenuOpen,
                contentDescription = "Menú Lateral",
                tint = PemexGreen,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(6.dp))

        // Scrollable Stage Navigation Tabs (prevents clipping on 800x480)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StageTab.entries.forEach { tab ->
                val selected = state.activeTab == tab
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) PemexGreen.copy(alpha = 0.20f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) PemexGreen else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 10.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (selected) PemexGreen else SlateLabel,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        // Compact Status Icons & Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                Icons.Rounded.Garage,
                contentDescription = "MyQ",
                tint = if (state.myQ.doorState == MyQDoorState.OPEN) PemexGold else PemexGreen,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onSelectTab(StageTab.MYQ_GARAGE) }
            )
            Icon(
                Icons.Rounded.Bluetooth,
                contentDescription = "OBD",
                tint = if (state.obd.connected) PemexGreen else SlateLabel,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onOpenObd() }
            )
            Icon(
                Icons.Rounded.GpsFixed,
                contentDescription = "GPS",
                tint = if (state.gps.hasGpsLock) PemexGreen else PemexGold,
                modifier = Modifier.size(15.dp)
            )
            Icon(
                Icons.Rounded.Wifi,
                contentDescription = "Wi-Fi",
                tint = if (state.wifiConnected) PemexGreen else SlateLabel,
                modifier = Modifier.size(15.dp)
            )
            Icon(
                Icons.Rounded.SignalCellularAlt,
                contentDescription = "LTE",
                tint = TelemetryWhite,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = state.currentTimeText,
                color = TelemetryWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
private fun BrowserAndAppsStage(
    state: SeltosDashboardUiState,
    onWebUrlChange: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onSelectBrowserAgent: (com.example.model.BrowserAgentProfile) -> Unit,
    onOpenInExternalBrowser: (String, String?) -> Unit,
    onLaunchAppSplit: (String) -> Unit
) {
    var showAppsGrid by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!showAppsGrid) PemexGreen.copy(alpha = 0.18f) else CarbonSurface)
                    .border(1.dp, if (!showAppsGrid) PemexGreen else CarbonBorder, RoundedCornerShape(10.dp))
                    .clickable { showAppsGrid = false }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Navegador Web", color = TelemetryWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showAppsGrid) PemexGreen.copy(alpha = 0.18f) else CarbonSurface)
                    .border(1.dp, if (showAppsGrid) PemexGreen else CarbonBorder, RoundedCornerShape(10.dp))
                    .clickable { showAppsGrid = true }
                    .padding(horizontal = 12.dp)
                    .testTag("btn_show_split_apps"),
                contentAlignment = Alignment.Center
            ) {
                Text("Apps Split (${state.installedApps.size})", color = TelemetryWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (showAppsGrid) {
                InstalledAppsSplitLauncher(
                    apps = state.installedApps,
                    onLaunchAppSplit = onLaunchAppSplit
                )
            } else {
                UnrestrictedWebHub(
                    currentUrl = state.webCurrentUrl,
                    adBlockEnabled = state.adBlockEnabled,
                    browserAgent = state.browserAgent,
                    installedBrowsers = state.installedBrowsers,
                    showQuickCards = true,
                    onUrlChange = onWebUrlChange,
                    onToggleAdBlock = onToggleAdBlock,
                    onSelectBrowserAgent = onSelectBrowserAgent,
                    onOpenInExternalBrowser = onOpenInExternalBrowser
                )
            }
        }
    }
}

@Composable
private fun ObdBleScannerDialog(
    state: SeltosDashboardUiState,
    onDismiss: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String, String) -> Unit,
    onDisconnect: () -> Unit,
    onToggleDemo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OBD-II BLE", color = TelemetryWhite, fontWeight = FontWeight.Bold)
                if (state.isScanningBle) {
                    CircularProgressIndicator(
                        color = PemexGreen,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.obd.statusNote, color = PemexGreen, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = if (state.isScanningBle) onStopScan else onStartScan,
                        colors = ButtonDefaults.buttonColors(containerColor = PemexGreen, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isScanningBle) "Detener" else "Escanear BLE")
                    }
                    OutlinedButton(
                        onClick = onToggleDemo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (state.obd.demoMode) "Fin Demo" else "Demo",
                            color = if (state.obd.demoMode) PemexGold else TelemetryWhite
                        )
                    }
                }

                if (state.obd.connected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Desconectar", color = PemexRed)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.scannedBleDevices, key = { it.address }) { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CarbonElevated)
                                .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
                                .clickable { onConnectDevice(dev.address, dev.name) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.name, color = TelemetryWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(dev.address, color = SlateLabel, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                            }
                            Text("${dev.rssi} dBm", color = PemexGreen, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = PemexGreen)
            }
        }
    )
}
