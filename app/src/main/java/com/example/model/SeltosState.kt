package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

enum class SpeedUnit(val label: String) {
    KMH("KM/H"),
    MPH("MPH")
}

enum class AmbientMood(val displayName: String, val color: Color) {
    PEMEX_GREEN("Verde Pemex", Color(0xFF00A86B)),
    PEMEX_RED("Rojo Pemex", Color(0xFFCE1126)),
    PEMEX_GOLD("Dorado", Color(0xFFD4AF37)),
    PEMEX_GUINDA("Guinda", Color(0xFF9F2241)),
    PLATINUM("Blanco", Color(0xFFE2E8F0))
}

enum class StageTab(val title: String) {
    HOME_HUB("Inicio"),
    MAP_NAV("Mapa"),
    VIDEO_STREAM("Video"),
    DUAL_VIEW("Dual View"),
    BROWSER_APPS("Web / Apps"),
    MYQ_GARAGE("MyQ")
}

enum class DualViewModule(val title: String) {
    MAP_VIEW("Mapa GPS"),
    VIDEO_PLAYER("ExoPlayer"),
    WEB_STREAM("Navegador Web"),
    OBD_CLUSTER("OBD-II"),
    MYQ_GARAGE("MyQ"),
    INSTALLED_APPS("Apps")
}

enum class MapEngineMode(val label: String) {
    VECTOR_DARK("Mapa Live"),
    GOOGLE_MAPS("Google Maps"),
    WAZE_LIVE("Waze")
}

enum class BrowserAgentProfile(val label: String, val userAgent: String) {
    CHROME_DESKTOP(
        "Chrome PC",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    ),
    CHROME_MOBILE(
        "Chrome Móvil",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    ),
    FIREFOX_DESKTOP(
        "Firefox",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0"
    ),
    SAFARI_IPAD(
        "Safari",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15"
    )
}

data class DoorStatus(
    val frontLeftOpen: Boolean = false,
    val frontRightOpen: Boolean = false,
    val rearLeftOpen: Boolean = false,
    val rearRightOpen: Boolean = false,
    val trunkOpen: Boolean = false,
    val doorsLocked: Boolean = true
) {
    val anyOpen: Boolean
        get() = frontLeftOpen || frontRightOpen || rearLeftOpen || rearRightOpen || trunkOpen
}

data class GpsTelemetry(
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val latitude: Double = 19.432608,
    val longitude: Double = -99.133209,
    val hasGpsLock: Boolean = false,
    val accuracyMeters: Float = 0f
) {
    val speedKmh: Int get() = (speedMps * 3.6f).toInt().coerceAtLeast(0)
    val speedMph: Int get() = (speedMps * 2.23694f).toInt().coerceAtLeast(0)

    val formattedCoordinates: String
        get() = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

    val cardinalHeading: String
        get() {
            val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((bearingDeg % 360f) + 360f) % 360f / 45f + 0.5f).toInt() % 8
            return dirs[index]
        }
}

data class ObdTelemetry(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val demoMode: Boolean = false,
    val deviceName: String = "OBD-II",
    val deviceAddress: String = "",
    val rpm: Int = 0,
    val coolantTempC: Int = 0,
    val speedKmh: Int = 0,
    val fuelEconomyLPer100Km: Float = 0f,
    val throttlePct: Int = 0,
    val engineLoadPct: Int = 0,
    val batteryVoltage: Float = 0f,
    val intakeTempC: Int = 0,
    val statusNote: String = "BLE OBD-II",
    val lastUpdatedMs: Long = 0L
) {
    val fuelEconomyMpg: Float
        get() = if (fuelEconomyLPer100Km > 0.2f) 235.215f / fuelEconomyLPer100Km else 0f
}

data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int
)

enum class MyQDoorState(val label: String) {
    CLOSED("CERRADO"),
    OPEN("ABIERTO"),
    OPENING("ABRIENDO"),
    CLOSING("CERRANDO"),
    STOPPED("DETENIDO"),
    UNKNOWN("SINCRONIZANDO")
}

enum class MyQConnectionMode(val label: String) {
    INTERACTIVE_PREVIEW("Interactivo"),
    LOCAL_BRIDGE("Bridge Local"),
    CLOUD_MYQ("MyQ Cloud")
}

data class MyQState(
    val mode: MyQConnectionMode = MyQConnectionMode.INTERACTIVE_PREVIEW,
    val email: String = "",
    val bearerToken: String = "",
    val localBridgeUrl: String = "http://192.168.1.50:8080",
    val accountId: String = "",
    val deviceSerial: String = "CG0840291SELTOS",
    val doorName: String = "Garaje Principal",
    val doorState: MyQDoorState = MyQDoorState.CLOSED,
    val doorOpenFraction: Float = 0f,
    val garageLightOn: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String = "MyQ Listo"
)

enum class MediaSourceType { LOCAL_USB, IPTV_HLS, DIRECT_URL }

data class MediaItemEntry(
    val id: String,
    val title: String,
    val uri: String,
    val type: MediaSourceType,
    val group: String = "Stream"
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val className: String
)

data class SeltosDashboardUiState(
    val activeTab: StageTab = StageTab.HOME_HUB,
    val splitRatio: Float = 0.32f,
    val isLeftDockCollapsed: Boolean = false,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val headlightsOn: Boolean = true,
    val ambientMood: AmbientMood = AmbientMood.PEMEX_GREEN,
    val doorStatus: DoorStatus = DoorStatus(),
    val gps: GpsTelemetry = GpsTelemetry(),
    val obd: ObdTelemetry = ObdTelemetry(),
    val scannedBleDevices: List<BleDeviceInfo> = emptyList(),
    val isScanningBle: Boolean = false,
    val showObdSheet: Boolean = false,
    val myQ: MyQState = MyQState(),
    val showMyQConfigDialog: Boolean = false,
    val dualPrimaryModule: DualViewModule = DualViewModule.VIDEO_PLAYER,
    val dualSecondaryModule: DualViewModule = DualViewModule.MAP_VIEW,
    val dualHorizontalSplit: Boolean = true,
    val dualInnerRatio: Float = 0.50f,
    val mapEngineMode: MapEngineMode = MapEngineMode.VECTOR_DARK,
    val browserAgent: BrowserAgentProfile = BrowserAgentProfile.CHROME_DESKTOP,
    val screenBrightness: Float = 0.85f,
    val mediaVolume: Float = 0.75f,
    val oledNightDimmer: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val backgroundPlaybackEnabled: Boolean = true,
    val wifiConnected: Boolean = true,
    val wifiLevel: Int = 4,
    val cellularSignalLevel: Int = 4,
    val batteryPct: Int = 92,
    val isCharging: Boolean = true,
    val currentTimeText: String = "07:31",
    val currentMediaTitle: String = "Stream HLS",
    val currentMediaUri: String = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    val isMediaPlaying: Boolean = false,
    val mediaPlaylist: List<MediaItemEntry> = DefaultMediaCatalog,
    val webCurrentUrl: String = "https://www.youtube.com/?app=desktop",
    val adBlockEnabled: Boolean = true,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val installedBrowsers: List<InstalledAppInfo> = emptyList(),
    val toastMessage: String? = null
)

val DefaultMediaCatalog = listOf(
    MediaItemEntry(
        id = "hls_mux",
        title = "Stream HLS",
        uri = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        type = MediaSourceType.IPTV_HLS,
        group = "HLS"
    ),
    MediaItemEntry(
        id = "hls_bipbop",
        title = "Stream AAC",
        uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        type = MediaSourceType.IPTV_HLS,
        group = "HLS"
    ),
    MediaItemEntry(
        id = "mp4_bbb",
        title = "Video MP4",
        uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        type = MediaSourceType.DIRECT_URL,
        group = "MP4"
    )
)
