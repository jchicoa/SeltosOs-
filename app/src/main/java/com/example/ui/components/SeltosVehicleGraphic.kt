package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AmbientMood
import com.example.model.DoorStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * High-contrast dark-studio render of a Black 2021 Kia Seltos on a glowing
 * perspective grid pad with interactive tap zones for Headlights, Doors/Trunk,
 * and Interior Ambient RGB mood lighting.
 */
@Composable
fun SeltosVehicleGraphic(
    headlightsOn: Boolean,
    ambientMood: AmbientMood,
    doorStatus: DoorStatus,
    onToggleHeadlights: () -> Unit,
    onToggleDoor: (String) -> Unit,
    onCycleAmbient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headlightAlpha by animateFloatAsState(
        targetValue = if (headlightsOn) 1f else 0.08f,
        animationSpec = tween(durationMillis = 320),
        label = "headlight_glow"
    )
    val ambientColor by animateColorAsState(
        targetValue = ambientMood.color,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ambient_rgb"
    )
    val vehicleDesc = stringResource(R.string.cd_vehicle_graphic)

    Column(
        modifier = modifier
            .semantics { contentDescription = vehicleDesc }
            .testTag("seltos_vehicle_graphic"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 120.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f
                val groundY = h * 0.78f

                // 1. Studio ambient under-glow
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = 0.28f),
                            ElectricCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(cx, groundY),
                        radius = w * 0.48f
                    ),
                    topLeft = Offset(cx - w * 0.45f, groundY - h * 0.16f),
                    size = Size(w * 0.9f, h * 0.32f)
                )

                // 2. Perspective floor grid pad
                val gridStroke = Color(0xFF1F2736)
                for (i in -3..3) {
                    val topX = cx + i * (w * 0.08f)
                    val botX = cx + i * (w * 0.145f)
                    drawLine(
                        color = gridStroke,
                        start = Offset(topX, groundY - h * 0.14f),
                        end = Offset(botX, h * 0.94f),
                        strokeWidth = 1.2f
                    )
                }
                for (r in 0..3) {
                    val y = (groundY - h * 0.10f) + r * (h * 0.085f)
                    val halfSpan = w * (0.28f + r * 0.065f)
                    drawLine(
                        color = gridStroke,
                        start = Offset(cx - halfSpan, y),
                        end = Offset(cx + halfSpan, y),
                        strokeWidth = 1.2f
                    )
                }

                // 3. Volumetric LED Headlight Beams (when headlightsOn)
                if (headlightAlpha > 0.1f) {
                    val leftBeam = Path().apply {
                        moveTo(cx - w * 0.24f, h * 0.59f)
                        lineTo(cx - w * 0.44f, h * 0.92f)
                        lineTo(cx - w * 0.12f, h * 0.92f)
                        close()
                    }
                    val rightBeam = Path().apply {
                        moveTo(cx + w * 0.24f, h * 0.59f)
                        lineTo(cx + w * 0.12f, h * 0.92f)
                        lineTo(cx + w * 0.44f, h * 0.92f)
                        close()
                    }
                    val beamBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0FAFF).copy(alpha = 0.34f * headlightAlpha),
                            ElectricCyan.copy(alpha = 0.14f * headlightAlpha),
                            Color.Transparent
                        )
                    )
                    drawPath(leftBeam, beamBrush)
                    drawPath(rightBeam, beamBrush)
                }

                // 4. Black 2021 Kia Seltos Tires & Wheel Arches
                val tireW = w * 0.095f
                val tireH = h * 0.16f
                drawRoundRect(
                    color = Color(0xFF050608),
                    topLeft = Offset(cx - w * 0.33f, h * 0.65f),
                    size = Size(tireW, tireH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFF050608),
                    topLeft = Offset(cx + w * 0.33f - tireW, h * 0.65f),
                    size = Size(tireW, tireH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // 5. Black 2021 Kia Seltos Main Lower Body & Sculpted Bumper
                val bodyPath = Path().apply {
                    moveTo(cx - w * 0.34f, h * 0.68f)
                    lineTo(cx - w * 0.35f, h * 0.53f)
                    lineTo(cx - w * 0.29f, h * 0.44f)
                    lineTo(cx + w * 0.29f, h * 0.44f)
                    lineTo(cx + w * 0.35f, h * 0.53f)
                    lineTo(cx + w * 0.34f, h * 0.68f)
                    lineTo(cx + w * 0.24f, h * 0.72f)
                    lineTo(cx - w * 0.24f, h * 0.72f)
                    close()
                }
                drawPath(
                    path = bodyPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF232834),
                            Color(0xFF101319),
                            Color(0xFF07080B)
                        )
                    )
                )
                drawPath(
                    path = bodyPath,
                    color = Color(0xFF394254),
                    style = Stroke(width = 2.2f, join = StrokeJoin.Round)
                )

                // 6. Roofline, Roof Rails & Tinted Windshield (2021 Seltos)
                val cabinPath = Path().apply {
                    moveTo(cx - w * 0.27f, h * 0.44f)
                    lineTo(cx - w * 0.20f, h * 0.24f)
                    lineTo(cx + w * 0.20f, h * 0.24f)
                    lineTo(cx + w * 0.27f, h * 0.44f)
                    close()
                }
                drawPath(
                    path = cabinPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C2330),
                            Color(0xFF0C1018)
                        )
                    )
                )
                drawPath(
                    path = cabinPath,
                    color = Color(0xFF475569),
                    style = Stroke(width = 1.8f, join = StrokeJoin.Round)
                )

                // Interior Ambient RGB Cabin Strip
                drawLine(
                    color = ambientColor.copy(alpha = 0.85f),
                    start = Offset(cx - w * 0.19f, h * 0.41f),
                    end = Offset(cx + w * 0.19f, h * 0.41f),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )

                // Roof Rails
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(cx - w * 0.18f, h * 0.225f),
                    end = Offset(cx - w * 0.13f, h * 0.225f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(cx + w * 0.13f, h * 0.225f),
                    end = Offset(cx + w * 0.18f, h * 0.225f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Side Mirrors (with indicator accents if doors unlocked)
                val mirrorColor = if (doorStatus.doorsLocked) Color(0xFF1A1E28) else ElectricCyan.copy(alpha = 0.45f)
                drawRoundRect(
                    color = mirrorColor,
                    topLeft = Offset(cx - w * 0.355f, h * 0.41f),
                    size = Size(w * 0.065f, h * 0.045f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = mirrorColor,
                    topLeft = Offset(cx + w * 0.29f, h * 0.41f),
                    size = Size(w * 0.065f, h * 0.045f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // 7. Signature Kia Seltos Tiger-Nose Grille + Extended LED DRL Brow
                val grilleRect = Size(w * 0.28f, h * 0.11f)
                val grilleTopLeft = Offset(cx - grilleRect.width / 2f, h * 0.50f)
                drawRoundRect(
                    color = Color(0xFF0A0C10),
                    topLeft = grilleTopLeft,
                    size = grilleRect,
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = grilleTopLeft,
                    size = grilleRect,
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 1.6f)
                )

                // Kia Seltos Signature Split LED DRL Eyebrow & Lower Headlights
                val drlColor = if (headlightsOn) Color(0xFFE6FCFF) else Color(0xFF334155)
                val drlGlow = ElectricCyan.copy(alpha = headlightAlpha)

                // Left & Right upper LED brow connecting into Tiger-Nose grille
                drawLine(
                    color = drlGlow,
                    start = Offset(cx - w * 0.31f, h * 0.50f),
                    end = Offset(cx - w * 0.07f, h * 0.51f),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = drlColor,
                    start = Offset(cx - w * 0.31f, h * 0.50f),
                    end = Offset(cx - w * 0.07f, h * 0.51f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = drlGlow,
                    start = Offset(cx + w * 0.07f, h * 0.51f),
                    end = Offset(cx + w * 0.31f, h * 0.50f),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = drlColor,
                    start = Offset(cx + w * 0.07f, h * 0.51f),
                    end = Offset(cx + w * 0.31f, h * 0.50f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Lower projector headlights + fog light accents
                drawCircle(
                    color = drlGlow,
                    radius = w * 0.025f,
                    center = Offset(cx - w * 0.25f, h * 0.57f)
                )
                drawCircle(
                    color = drlColor,
                    radius = w * 0.013f,
                    center = Offset(cx - w * 0.25f, h * 0.57f)
                )
                drawCircle(
                    color = drlGlow,
                    radius = w * 0.025f,
                    center = Offset(cx + w * 0.25f, h * 0.57f)
                )
                drawCircle(
                    color = drlColor,
                    radius = w * 0.013f,
                    center = Offset(cx + w * 0.25f, h * 0.57f)
                )

                // Lower Silver Skid Plate Accent
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(cx - w * 0.16f, h * 0.675f),
                    size = Size(w * 0.32f, h * 0.032f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Door / Trunk Open Visual Alerts on Vehicle Flanks
                if (doorStatus.frontLeftOpen || doorStatus.rearLeftOpen) {
                    drawLine(
                        color = AmberWarning,
                        start = Offset(cx - w * 0.35f, h * 0.45f),
                        end = Offset(cx - w * 0.42f, h * 0.58f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
                if (doorStatus.frontRightOpen || doorStatus.rearRightOpen) {
                    drawLine(
                        color = AmberWarning,
                        start = Offset(cx + w * 0.35f, h * 0.45f),
                        end = Offset(cx + w * 0.42f, h * 0.58f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
                if (doorStatus.trunkOpen) {
                    drawRoundRect(
                        color = AmberWarning,
                        topLeft = Offset(cx - w * 0.12f, h * 0.21f),
                        size = Size(w * 0.24f, h * 0.025f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Invisible Interactive Tap Zones over Graphic
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 120.dp, height = 56.dp)
                    .clickable { onCycleAmbient() }
                    .testTag("tap_zone_ambient_cabin")
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 160.dp, height = 64.dp)
                    .clickable { onToggleHeadlights() }
                    .testTag("tap_zone_headlights")
            )
        }

        // Interactive Quick Status Chips below Seltos render (minimum 48dp touch height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VehicleZonePill(
                label = if (headlightsOn) "DRL ON" else "LIGHTS",
                active = headlightsOn,
                activeColor = ElectricCyan,
                onClick = onToggleHeadlights,
                modifier = Modifier
                    .weight(1f)
                    .testTag("vehicle_lights_pill")
            )
            VehicleZonePill(
                label = if (doorStatus.anyOpen) "OPEN" else if (doorStatus.doorsLocked) "LOCKED" else "UNLOCKED",
                active = !doorStatus.doorsLocked || doorStatus.anyOpen,
                activeColor = if (doorStatus.anyOpen) AmberWarning else EmeraldActive,
                onClick = { onToggleDoor("LOCK") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("vehicle_doors_pill")
            )
            VehicleZonePill(
                label = "TRUNK",
                active = doorStatus.trunkOpen,
                activeColor = AmberWarning,
                onClick = { onToggleDoor("TRUNK") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("vehicle_trunk_pill")
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonElevated)
                    .border(1.dp, ambientMood.color.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .clickable { onCycleAmbient() }
                    .testTag("vehicle_ambient_pill"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ambientMood.color)
                )
            }
        }
    }
}

@Composable
private fun VehicleZonePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) activeColor.copy(alpha = 0.14f) else CarbonElevated)
            .border(
                width = 1.dp,
                color = if (active) activeColor.copy(alpha = 0.7f) else CarbonBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) TelemetryWhite else SlateLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}
