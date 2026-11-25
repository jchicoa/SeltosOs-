package com.example.car

import android.app.Presentation
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.ViewGroup
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.validation.HostValidator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.model.MyQDoorState
import com.example.myq.MyQProtocolClient
import com.example.telemetry.SeltosTelemetryRepository
import com.example.ui.components.SeltosVehicleGraphic
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SeltosMateTheme
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * Android Auto CarAppService rendering SeltosMate OS in High Definition via
 * SurfaceCallback + VirtualDisplay + ComposeView, without any motion-lock restrictions.
 */
class SeltosCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return SeltosCarSession()
    }
}

class SeltosCarSession : Session(), SavedStateRegistryOwner {

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private var surfaceCallback: SeltosHdSurfaceCallback? = null

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                surfaceCallback?.release()
                surfaceCallback = null
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        val callback = SeltosHdSurfaceCallback(this)
        surfaceCallback = callback
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(callback)
        return SeltosHdCarScreen(carContext)
    }
}

class SeltosHdCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    private val myQClient = MyQProtocolClient()

    override fun onGetTemplate(): Template {
        val myQState = SeltosTelemetryRepository.myQ.value
        val garageLabel = if (myQState.doorState == MyQDoorState.OPEN) "Close Garage" else "Open Garage"

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(garageLabel)
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.mipmap.ic_launcher)
                        ).build()
                    )
                    .setOnClickListener {
                        val cmd = if (SeltosTelemetryRepository.myQ.value.doorState == MyQDoorState.OPEN) {
                            MyQProtocolClient.DoorCommand.CLOSE
                        } else {
                            MyQProtocolClient.DoorCommand.OPEN
                        }
                        myQClient.executeDoorCommand(cmd)
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Lights")
                    .setOnClickListener {
                        SeltosTelemetryRepository.setHeadlights(!SeltosTelemetryRepository.headlightsOn.value)
                    }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setBackgroundColor(CarColor.createCustom(AndroidColor.parseColor("#0A0B0E"), AndroidColor.parseColor("#0A0B0E")))
            .build()
    }
}

class SeltosHdSurfaceCallback(
    private val session: SeltosCarSession
) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        release()
        val surface = surfaceContainer.surface ?: return
        val displayManager = session.carContext.getSystemService(DisplayManager::class.java) ?: return

        virtualDisplay = displayManager.createVirtualDisplay(
            "SeltosMate_AA_HD",
            surfaceContainer.width.coerceAtLeast(800),
            surfaceContainer.height.coerceAtLeast(480),
            surfaceContainer.dpi.coerceAtLeast(160),
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
        )

        val display = virtualDisplay?.display ?: return
        presentation = Presentation(session.carContext, display).apply {
            val composeView = ComposeView(session.carContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setViewTreeLifecycleOwner(session)
                setViewTreeSavedStateRegistryOwner(session)
                setContent {
                    SeltosMateTheme {
                        AndroidAutoHdCockpit()
                    }
                }
            }
            setContentView(composeView)
            show()
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        // Full HD surface adapts dynamically via Compose layout constraints
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        release()
    }

    fun release() {
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }
}

@Composable
private fun AndroidAutoHdCockpit() {
    val gps by SeltosTelemetryRepository.gps.collectAsStateWithLifecycle()
    val obd by SeltosTelemetryRepository.obd.collectAsStateWithLifecycle()
    val myQ by SeltosTelemetryRepository.myQ.collectAsStateWithLifecycle()
    val headlights by SeltosTelemetryRepository.headlightsOn.collectAsStateWithLifecycle()
    val ambientMood by SeltosTelemetryRepository.ambientMood.collectAsStateWithLifecycle()
    val doors by SeltosTelemetryRepository.doorStatus.collectAsStateWithLifecycle()
    val media by SeltosTelemetryRepository.mediaNowPlaying.collectAsStateWithLifecycle()

    val effectiveSpeed = if (gps.hasGpsLock && gps.speedKmh > 0) gps.speedKmh else obd.speedKmh

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF121824), ObsidianBg)
                )
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left HD Vehicle & Speed Card
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(CarbonSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELTOSMATE OS • HD",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "${gps.cardinalHeading} • ${gps.altitudeMeters.toInt()}m",
                    color = SlateLabel,
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            SeltosVehicleGraphic(
                headlightsOn = headlights,
                ambientMood = ambientMood,
                doorStatus = doors,
                onToggleHeadlights = {},
                onToggleDoor = {},
                onCycleAmbient = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$effectiveSpeed",
                    color = TelemetryWhite,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "KM/H",
                    color = ElectricCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        // Right HD OBD-II + MyQ + Media Column
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live OBD-II Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HdMetricCard("ENGINE RPM", "${obd.rpm}", "RPM", ElectricCyan, Modifier.weight(1f))
                HdMetricCard("COOLANT", "${obd.coolantTempC}°C", "OBD-II", EmeraldActive, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val econ = if (obd.fuelEconomyLPer100Km > 0f) {
                    String.format("%.1f", obd.fuelEconomyLPer100Km)
                } else {
                    "--"
                }
                HdMetricCard("FUEL ECON", econ, "L/100KM", ambientMood.color, Modifier.weight(1f))
                HdMetricCard("MYQ GARAGE", myQ.doorState.label, myQ.doorName, ElectricCyan, Modifier.weight(1f))
            }

            // Now Playing HD Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonSurface)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE MEDIA", color = SlateLabel, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(media.first, color = TelemetryWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = if (media.second) "PLAYING" else "READY",
                        color = if (media.second) EmeraldActive else ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HdMetricCard(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonSurface)
            .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SlateLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Text(value, color = TelemetryWhite, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFontFamily)
        Text(unit, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
