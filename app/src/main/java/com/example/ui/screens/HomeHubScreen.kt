package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MapEngineMode
import com.example.model.SeltosDashboardUiState
import com.example.model.StageTab
import com.example.ui.components.InteractiveMapPanel
import com.example.ui.components.ObdPerformanceClusterCard
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PemexGold
import com.example.ui.theme.PemexGreen
import com.example.ui.theme.PemexRed
import com.example.ui.theme.TelemetryWhite

/**
 * Concise Pemex-themed Home Hub with immediate visible Map, Seltos 2021 Hero,
 * and live OBD-II gauges (no excess descriptive copy).
 */
@Composable
fun HomeHubScreen(
    state: SeltosDashboardUiState,
    onSelectTab: (StageTab) -> Unit,
    onOpenObdSheet: () -> Unit,
    onSelectMapMode: (MapEngineMode) -> Unit,
    onLaunchExternalNav: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "hero_pulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row: Seltos Hero + Quick Launchers (left) + Live Interactive Map (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonSurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PemexGreen.copy(alpha = glowAlpha),
                                PemexRed.copy(alpha = glowAlpha),
                                CarbonBorder
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_seltos_hero),
                    contentDescription = "Kia Seltos 2021",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.85f),
                                    ObsidianBg.copy(alpha = 0.35f),
                                    ObsidianBg.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PemexGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SELTOSMATE OS",
                            color = PemexGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HeroQuickPill(
                            label = "Video",
                            icon = Icons.Rounded.PlayCircle,
                            accent = PemexGreen,
                            onClick = { onSelectTab(StageTab.VIDEO_STREAM) },
                            modifier = Modifier.weight(1f).testTag("hero_btn_video")
                        )
                        HeroQuickPill(
                            label = "Dual",
                            icon = Icons.Rounded.Splitscreen,
                            accent = PemexRed,
                            onClick = { onSelectTab(StageTab.DUAL_VIEW) },
                            modifier = Modifier.weight(1f).testTag("hero_btn_dual")
                        )
                        HeroQuickPill(
                            label = "MyQ",
                            icon = Icons.Rounded.Garage,
                            accent = PemexGold,
                            onClick = { onSelectTab(StageTab.MYQ_GARAGE) },
                            modifier = Modifier.weight(1f).testTag("hero_btn_myq")
                        )
                        HeroQuickPill(
                            label = "Web",
                            icon = Icons.Rounded.Apps,
                            accent = TelemetryWhite,
                            onClick = { onSelectTab(StageTab.BROWSER_APPS) },
                            modifier = Modifier.weight(1f).testTag("hero_btn_apps")
                        )
                    }
                }
            }

            // Always-Visible Live Interactive Map right on Home Hub
            Box(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
            ) {
                InteractiveMapPanel(
                    gps = state.gps,
                    mapMode = state.mapEngineMode,
                    compactMode = true,
                    onSelectMapMode = onSelectMapMode,
                    onLaunchExternalNav = onLaunchExternalNav
                )
            }
        }

        // Bottom Row: Animated OBD-II Live Performance Cluster + Full Map Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObdPerformanceClusterCard(
                obd = state.obd,
                onOpenScanner = onOpenObdSheet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroQuickPill(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonElevated.copy(alpha = 0.92f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TelemetryWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
