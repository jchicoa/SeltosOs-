package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SeltosDashboardUiState
import com.example.model.SpeedUnit
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
 * Persistent Left Telemetry & Vehicle Dock (adaptive scrollable layout for 800x480+).
 * Displays coordinate-only GPS telemetry, OBD-II cluster, and 12 quick controls.
 */
@Composable
fun LeftTelemetryDock(
    state: SeltosDashboardUiState,
    onToggleSpeedUnit: () -> Unit,
    onToggleHeadlights: () -> Unit,
    onToggleDoor: (String) -> Unit,
    onCycleAmbient: () -> Unit,
    onOpenObdSheet: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleSplitCollapse: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleOledDimmer: () -> Unit,
    onLaunchExternalNav: () -> Unit,
    onTriggerGarage: () -> Unit,
    onToggleKeepAwake: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSpeedKmh = if (state.gps.hasGpsLock && state.gps.speedMps > 0.3f) {
        state.gps.speedKmh
    } else {
        state.obd.speedKmh
    }
    val displaySpeed = if (state.speedUnit == SpeedUnit.KMH) {
        effectiveSpeedKmh
    } else {
        (effectiveSpeedKmh * 0.621371f).toInt()
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B120E), ObsidianBg)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Vehicle Studio Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 165.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PemexRed)
                    )
                    Text(
                        text = "SELTOS 2021",
                        color = TelemetryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (state.obd.connected) "OBD ON" else "PEMEX",
                        color = if (state.obd.connected) PemexGreen else PemexGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonElevated)
                            .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                            .clickable { onToggleSplitCollapse() }
                            .testTag("btn_collapse_dock"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = "Ocultar Menú",
                            tint = PemexGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            SeltosVehicleGraphic(
                headlightsOn = state.headlightsOn,
                ambientMood = state.ambientMood,
                doorStatus = state.doorStatus,
                onToggleHeadlights = onToggleHeadlights,
                onToggleDoor = onToggleDoor,
                onCycleAmbient = onCycleAmbient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            )
        }

        // 2. Coordinate-Only GPS & OBD-II Telemetry Cluster
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Digital Speedometer
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSpeedUnit() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("speed_unit_toggle"),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$displaySpeed",
                        color = TelemetryWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        lineHeight = 38.sp
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = state.speedUnit.label,
                        color = PemexGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Strictly Coordinates + Compass/Alt + Battery
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = null,
                            tint = if (state.gps.hasGpsLock) PemexGreen else PemexGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = state.gps.formattedCoordinates,
                            color = TelemetryWhite,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = PemexGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${state.gps.cardinalHeading} ${state.gps.bearingDeg.toInt()}° • ${state.gps.altitudeMeters.toInt()}m",
                            color = SlateLabel,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (state.isCharging) PemexGreen else TelemetryWhite,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "${state.batteryPct}%",
                            color = SlateLabel,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }

            // Live BLE OBD-II Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CarbonElevated)
                    .border(
                        width = 1.dp,
                        color = if (state.obd.connected) PemexGreen.copy(alpha = 0.5f) else CarbonBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onOpenObdSheet() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("obd_mini_cluster"),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ObdMiniStat("RPM", if (state.obd.connected) "${state.obd.rpm}" else "--", PemexGreen)
                ObdMiniStat("TEMP", if (state.obd.connected) "${state.obd.coolantTempC}°C" else "--", PemexGold)
                val econText = if (state.obd.connected && state.obd.fuelEconomyLPer100Km > 0f) {
                    String.format("%.1fL", state.obd.fuelEconomyLPer100Km)
                } else {
                    "--"
                }
                ObdMiniStat("CONS", econText, PemexRed)
                val voltText = if (state.obd.connected && state.obd.batteryVoltage > 0f) {
                    String.format("%.1fV", state.obd.batteryVoltage)
                } else {
                    "--"
                }
                ObdMiniStat("VOLT", voltText, TelemetryWhite)
            }
        }

        // 3. Swipeable Quick Controls (12 Automotive Tiles)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            val pagerState = rememberPagerState(pageCount = { 2 })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTROLES",
                    color = SlateLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = if (pagerState.currentPage == idx) 14.dp else 6.dp, height = 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == idx) PemexGreen else CarbonBorder
                                )
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactSliderTile(
                                icon = Icons.Rounded.Brightness6,
                                label = "Brillo",
                                value = state.screenBrightness,
                                onValueChange = onBrightnessChange,
                                modifier = Modifier.weight(1f)
                            )
                            CompactSliderTile(
                                icon = Icons.Rounded.VolumeUp,
                                label = "Volumen",
                                value = state.mediaVolume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ControlShortcutTile(
                                icon = Icons.Rounded.ViewSidebar,
                                label = "Menú",
                                active = !state.isLeftDockCollapsed,
                                onClick = onToggleSplitCollapse,
                                modifier = Modifier.weight(1f).testTag("tile_split_toggle")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.PictureInPictureAlt,
                                label = "PiP",
                                active = false,
                                onClick = onEnterPip,
                                modifier = Modifier.weight(1f).testTag("tile_pip")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.DarkMode,
                                label = "OLED",
                                active = state.oledNightDimmer,
                                onClick = onToggleOledDimmer,
                                modifier = Modifier.weight(1f).testTag("tile_oled")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.Bluetooth,
                                label = "OBD",
                                active = state.obd.connected,
                                onClick = onOpenObdSheet,
                                modifier = Modifier.weight(1f).testTag("tile_obd_ble")
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ControlShortcutTile(
                                icon = Icons.Rounded.WifiTethering,
                                label = "Red",
                                active = state.wifiConnected,
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("tile_hotspot")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.Navigation,
                                label = "Nav",
                                active = false,
                                onClick = onLaunchExternalNav,
                                modifier = Modifier.weight(1f).testTag("tile_ext_nav")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.Garage,
                                label = "MyQ",
                                active = state.myQ.doorOpenFraction > 0.1f,
                                onClick = onTriggerGarage,
                                modifier = Modifier.weight(1f).testTag("tile_myq")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ControlShortcutTile(
                                icon = Icons.Rounded.LightMode,
                                label = "RGB",
                                active = true,
                                onClick = onCycleAmbient,
                                modifier = Modifier.weight(1f).testTag("tile_ambient")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.ScreenLockPortrait,
                                label = "Activo",
                                active = state.keepScreenAwake,
                                onClick = onToggleKeepAwake,
                                modifier = Modifier.weight(1f).testTag("tile_awake")
                            )
                            ControlShortcutTile(
                                icon = Icons.Rounded.Headphones,
                                label = "Audio",
                                active = state.backgroundPlaybackEnabled,
                                onClick = onToggleBackgroundAudio,
                                modifier = Modifier.weight(1f).testTag("tile_bg_audio")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObdMiniStat(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SlateLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily
        )
    }
}

@Composable
private fun CompactSliderTile(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonElevated)
            .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = PemexGreen, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = PemexGreen,
                activeTrackColor = PemexGreen,
                inactiveTrackColor = CarbonBorder
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlShortcutTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) PemexGreen.copy(alpha = 0.16f) else CarbonElevated)
            .border(
                width = 1.dp,
                color = if (active) PemexGreen.copy(alpha = 0.7f) else CarbonBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) PemexGreen else TelemetryWhite,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = if (active) TelemetryWhite else SlateLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
