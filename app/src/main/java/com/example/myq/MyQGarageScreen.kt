package com.example.myq

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MyQConnectionMode
import com.example.model.MyQDoorState
import com.example.model.MyQState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * Chamberlain MyQ Garage Door Control Center with realistic animated architectural
 * garage illustration showing sectional panels, frosted windows, Chamberlain opener rail,
 * safety beam sensors, and the 2021 Black Kia Seltos inside the bay.
 */
@Composable
fun MyQGarageScreen(
    myQState: MyQState,
    showConfigDialog: Boolean,
    compactMode: Boolean = false,
    onCommand: (MyQProtocolClient.DoorCommand) -> Unit,
    onToggleLight: () -> Unit,
    onOpenConfig: () -> Unit,
    onDismissConfig: () -> Unit,
    onSaveConfig: (MyQConnectionMode, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedOpenFraction by animateFloatAsState(
        targetValue = myQState.doorOpenFraction,
        animationSpec = tween(durationMillis = 120),
        label = "garage_door_travel"
    )
    val garageDesc = stringResource(R.string.cd_garage_illustration)

    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left/Main Architectural Garage Illustration
        Column(
            modifier = Modifier
                .weight(if (compactMode) 1f else 0.62f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Garage,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = myQState.doorName,
                            color = TelemetryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${myQState.mode.label} • SN ${myQState.deviceSerial}",
                            color = SlateLabel,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                val badgeColor = when (myQState.doorState) {
                    MyQDoorState.OPEN -> EmeraldActive
                    MyQDoorState.OPENING, MyQDoorState.CLOSING -> AmberWarning
                    MyQDoorState.STOPPED -> CrimsonAlert
                    else -> ElectricCyan
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(1.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = myQState.doorState.label,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Architectural Garage Canvas Illustration with Photorealistic Interior Backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF07080B))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
                    .semantics { contentDescription = garageDesc }
                    .testTag("garage_illustration_canvas")
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_garage_backdrop),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.25f + (animatedOpenFraction * 0.55f),
                    modifier = Modifier.fillMaxSize()
                )
                GarageIllustrationCanvas(
                    openFraction = animatedOpenFraction,
                    lightOn = myQState.garageLightOn,
                    isMoving = myQState.doorState == MyQDoorState.OPENING || myQState.doorState == MyQDoorState.CLOSING,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (compactMode) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GarageActionButton(
                        label = "OPEN",
                        icon = Icons.Rounded.ArrowUpward,
                        accent = EmeraldActive,
                        onClick = { onCommand(MyQProtocolClient.DoorCommand.OPEN) },
                        modifier = Modifier.weight(1f)
                    )
                    GarageActionButton(
                        label = "CLOSE",
                        icon = Icons.Rounded.ArrowDownward,
                        accent = ElectricCyan,
                        onClick = { onCommand(MyQProtocolClient.DoorCommand.CLOSE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Right Chamberlain MyQ Control Panel (full view)
        if (!compactMode) {
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonSurface)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MYQ PROTOCOL",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = myQState.statusMessage,
                        color = TelemetryWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${(animatedOpenFraction * 100).toInt()}%",
                        color = SlateLabel,
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GarageActionButton(
                        label = "ABRIR",
                        icon = Icons.Rounded.ArrowUpward,
                        accent = EmeraldActive,
                        onClick = { onCommand(MyQProtocolClient.DoorCommand.OPEN) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_myq_open")
                    )
                    GarageActionButton(
                        label = "CERRAR",
                        icon = Icons.Rounded.ArrowDownward,
                        accent = ElectricCyan,
                        onClick = { onCommand(MyQProtocolClient.DoorCommand.CLOSE) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_myq_close")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GarageActionButton(
                            label = "ALTO",
                            icon = Icons.Rounded.Pause,
                            accent = AmberWarning,
                            onClick = { onCommand(MyQProtocolClient.DoorCommand.STOP) },
                            modifier = Modifier.weight(1f).testTag("btn_myq_stop")
                        )
                        GarageActionButton(
                            label = "LUZ",
                            icon = Icons.Rounded.Lightbulb,
                            accent = if (myQState.garageLightOn) AmberWarning else SlateLabel,
                            onClick = onToggleLight,
                            modifier = Modifier.weight(1f).testTag("btn_myq_light")
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_myq_config"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Config MyQ", color = TelemetryWhite, fontSize = 12.sp)
                }
            }
        }
    }

    if (showConfigDialog) {
        var selectedMode by remember { mutableStateOf(myQState.mode) }
        var bridgeUrl by remember { mutableStateOf(myQState.localBridgeUrl) }
        var bearerToken by remember { mutableStateOf(myQState.bearerToken) }

        AlertDialog(
            onDismissRequest = onDismissConfig,
            containerColor = CarbonSurface,
            title = { Text("Chamberlain MyQ Protocol Setup", color = TelemetryWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select MyQ protocol transport:",
                        color = SlateLabel,
                        fontSize = 12.sp
                    )
                    MyQConnectionMode.entries.forEach { mode ->
                        val active = selectedMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) ElectricCyan.copy(alpha = 0.16f) else CarbonElevated)
                                .border(1.dp, if (active) ElectricCyan else CarbonBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedMode = mode }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (active) ElectricCyan else SlateLabel)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(mode.label, color = TelemetryWhite, fontSize = 13.sp)
                        }
                    }

                    if (selectedMode == MyQConnectionMode.LOCAL_BRIDGE) {
                        OutlinedTextField(
                            value = bridgeUrl,
                            onValueChange = { bridgeUrl = it },
                            label = { Text("Local Bridge / ratgdo URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (selectedMode == MyQConnectionMode.CLOUD_MYQ) {
                        OutlinedTextField(
                            value = bearerToken,
                            onValueChange = { bearerToken = it },
                            label = { Text("MyQ OAuth2 Bearer Token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveConfig(selectedMode, bridgeUrl, bearerToken)
                        onDismissConfig()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg)
                ) {
                    Text("Apply Protocol")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfig) {
                    Text("Cancel", color = SlateLabel)
                }
            }
        )
    }
}

@Composable
private fun GarageActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = TelemetryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

/**
 * Custom Canvas architectural garage elevation illustrating:
 * - Modern dark stucco/slate garage structure with warm architectural soffit downlights
 * - Interior illuminated bay with Chamberlain MyQ ceiling opener rail & parked 2021 Kia Seltos
 * - Animated 4-section insulated garage door with top frosted glass window lites
 * - Safety beam sensors at floor level
 */
@Composable
fun GarageIllustrationCanvas(
    openFraction: Float,
    lightOn: Boolean,
    isMoving: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Night sky & modern residential backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF090D16), Color(0xFF101521), Color(0xFF0A0C12))
            )
        )

        // Driveway concrete apron
        val floorY = h * 0.84f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1B202C), Color(0xFF0E1118))
            ),
            topLeft = Offset(0f, floorY),
            size = Size(w, h - floorY)
        )

        // Modern Garage Exterior Facade
        val wallLeft = w * 0.10f
        val wallRight = w * 0.90f
        val roofY = h * 0.10f
        drawRoundRect(
            color = Color(0xFF171C26),
            topLeft = Offset(wallLeft, roofY),
            size = Size(wallRight - wallLeft, floorY - roofY),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFF2A3242),
            topLeft = Offset(wallLeft, roofY),
            size = Size(wallRight - wallLeft, floorY - roofY),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f)
        )

        // Architectural Fascia / Roof Overhang + Warm Sconce Lights
        drawRect(
            color = Color(0xFF232A38),
            topLeft = Offset(wallLeft - w * 0.02f, roofY - h * 0.02f),
            size = Size((wallRight - wallLeft) + w * 0.04f, h * 0.06f)
        )
        // Left & right architectural wall sconces
        listOf(wallLeft + w * 0.045f, wallRight - w * 0.045f).forEach { sx ->
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55FBBF24), Color.Transparent),
                    center = Offset(sx, h * 0.42f),
                    radius = w * 0.08f
                ),
                topLeft = Offset(sx - w * 0.06f, h * 0.30f),
                size = Size(w * 0.12f, h * 0.25f)
            )
            drawRoundRect(
                color = Color(0xFFFDE68A),
                topLeft = Offset(sx - 6f, h * 0.36f),
                size = Size(12f, 20f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Garage Door Opening Frame
        val doorLeft = w * 0.20f
        val doorRight = w * 0.80f
        val doorTop = h * 0.22f
        val doorWidth = doorRight - doorLeft
        val doorHeight = floorY - doorTop

        // Interior Garage Bay (visible as door opens)
        clipRect(left = doorLeft, top = doorTop, right = doorRight, bottom = floorY) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (lightOn) Color(0xFF262A35) else Color(0xFF0A0D14),
                        if (lightOn) Color(0xFF1A1E28) else Color(0xFF06080C)
                    )
                ),
                topLeft = Offset(doorLeft, doorTop),
                size = Size(doorWidth, doorHeight)
            )

            // Interior Chamberlain MyQ Overhead Rail & Opener Unit
            val cx = (doorLeft + doorRight) * 0.5f
            drawLine(
                color = Color(0xFF475569),
                start = Offset(cx, doorTop),
                end = Offset(cx, doorTop + doorHeight * 0.22f),
                strokeWidth = 5f
            )
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(cx - 24f, doorTop + doorHeight * 0.15f),
                size = Size(48f, 18f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            if (lightOn) {
                val cone = Path().apply {
                    moveTo(cx - 16f, doorTop + doorHeight * 0.18f)
                    lineTo(doorLeft + doorWidth * 0.12f, floorY)
                    lineTo(doorRight - doorWidth * 0.12f, floorY)
                    lineTo(cx + 16f, doorTop + doorHeight * 0.18f)
                    close()
                }
                drawPath(
                    path = cone,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x55FEF08A), Color(0x11FEF08A), Color.Transparent)
                    )
                )
            }

            // Parked Black 2021 Kia Seltos silhouette inside garage bay
            val carW = doorWidth * 0.54f
            val carH = doorHeight * 0.52f
            val carLeft = cx - carW / 2f
            val carBottom = floorY - 6f
            // Cabin
            val cabin = Path().apply {
                moveTo(cx - carW * 0.36f, carBottom - carH * 0.45f)
                lineTo(cx - carW * 0.25f, carBottom - carH * 0.90f)
                lineTo(cx + carW * 0.25f, carBottom - carH * 0.90f)
                lineTo(cx + carW * 0.36f, carBottom - carH * 0.45f)
                close()
            }
            drawPath(cabin, color = Color(0xFF151922))
            drawPath(cabin, color = Color(0xFF384254), style = Stroke(width = 1.6f))
            // Body
            drawRoundRect(
                color = Color(0xFF0C0E14),
                topLeft = Offset(carLeft, carBottom - carH * 0.52f),
                size = Size(carW, carH * 0.44f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color(0xFF333C4D),
                topLeft = Offset(carLeft, carBottom - carH * 0.52f),
                size = Size(carW, carH * 0.44f),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 1.8f)
            )
            // Seltos DRL signature glowing inside bay
            drawLine(
                color = ElectricCyan,
                start = Offset(carLeft + carW * 0.12f, carBottom - carH * 0.36f),
                end = Offset(carLeft + carW * 0.34f, carBottom - carH * 0.34f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ElectricCyan,
                start = Offset(carLeft + carW * 0.66f, carBottom - carH * 0.34f),
                end = Offset(carLeft + carW * 0.88f, carBottom - carH * 0.36f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )

            // Animated Sectional Garage Door Curtain
            val visibleDoorHeight = doorHeight * (1f - openFraction).coerceIn(0f, 1f)
            val panelCount = 4
            val panelH = doorHeight / panelCount
            val curtainTop = doorTop - (doorHeight - visibleDoorHeight)

            for (p in 0 until panelCount) {
                val py = curtainTop + p * panelH
                if (py + panelH < doorTop) continue

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF282F3D), Color(0xFF1E2430))
                    ),
                    topLeft = Offset(doorLeft, py),
                    size = Size(doorWidth, panelH - 2f)
                )
                drawLine(
                    color = Color(0xFF3E485C),
                    start = Offset(doorLeft, py + panelH - 2f),
                    end = Offset(doorRight, py + panelH - 2f),
                    strokeWidth = 2f
                )

                // Top panel frosted glass window lites
                if (p == 0) {
                    val liteW = (doorWidth - 40f) / 4f
                    for (l in 0 until 4) {
                        val lx = doorLeft + 8f + l * (liteW + 8f)
                        drawRoundRect(
                            color = if (lightOn) Color(0x66FEF3C7) else Color(0xFF3B485E),
                            topLeft = Offset(lx, py + panelH * 0.22f),
                            size = Size(liteW, panelH * 0.54f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }

        // Garage Frame Border & Safety Laser Sensors
        drawRect(
            color = Color(0xFF3B4559),
            topLeft = Offset(doorLeft, doorTop),
            size = Size(doorWidth, doorHeight),
            style = Stroke(width = 4f)
        )
        val sensorColor = if (isMoving) AmberWarning else EmeraldActive
        drawCircle(color = sensorColor, radius = 4.5f, center = Offset(doorLeft + 6f, floorY - 12f))
        drawCircle(color = sensorColor, radius = 4.5f, center = Offset(doorRight - 6f, floorY - 12f))
    }
}
