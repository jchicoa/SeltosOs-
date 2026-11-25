package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apps.AppLauncherHelper
import com.example.media.IptvPlaylistParser
import com.example.model.AmbientMood
import com.example.model.BrowserAgentProfile
import com.example.model.DualViewModule
import com.example.model.MapEngineMode
import com.example.model.MediaItemEntry
import com.example.model.MediaSourceType
import com.example.model.MyQConnectionMode
import com.example.model.SeltosDashboardUiState
import com.example.model.SpeedUnit
import com.example.model.StageTab
import com.example.myq.MyQProtocolClient
import com.example.telemetry.LocationTelemetryManager
import com.example.telemetry.Obd2BleService
import com.example.telemetry.SeltosTelemetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.seltosDataStore by preferencesDataStore(name = "seltosmate_prefs")

class SeltosTelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val locationManager = LocationTelemetryManager(appContext)
    val myQClient = MyQProtocolClient()

    private val _uiState = MutableStateFlow(SeltosDashboardUiState())
    val uiState: StateFlow<SeltosDashboardUiState> = _uiState.asStateFlow()

    init {
        loadSavedPreferences()
        observeSharedTelemetry()
        refreshInstalledApps()
        startSystemStatusLoop()
    }

    private fun loadSavedPreferences() {
        viewModelScope.launch {
            // Load initial snapshot reliably and keep state in sync without overwriting user interactions
            val prefs = appContext.seltosDataStore.data.first()
            val unit = prefs[KEY_SPEED_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                ?: SpeedUnit.KMH
            val mood = prefs[KEY_AMBIENT_MOOD]?.let { runCatching { AmbientMood.valueOf(it) }.getOrNull() }
                ?: AmbientMood.PEMEX_GREEN
            val ratio = prefs[KEY_SPLIT_RATIO] ?: 0.32f
            val dockCollapsed = prefs[KEY_DOCK_COLLAPSED] ?: false
            val activeTab = prefs[KEY_ACTIVE_TAB]?.let { runCatching { StageTab.valueOf(it) }.getOrNull() }
                ?: StageTab.HOME_HUB
            val dualPrimary = prefs[KEY_DUAL_PRIMARY]?.let { runCatching { DualViewModule.valueOf(it) }.getOrNull() }
                ?: DualViewModule.VIDEO_PLAYER
            val dualSecondary = prefs[KEY_DUAL_SECONDARY]?.let { runCatching { DualViewModule.valueOf(it) }.getOrNull() }
                ?: DualViewModule.MAP_VIEW
            val dualHoriz = prefs[KEY_DUAL_HORIZONTAL] ?: true
            val dualInner = prefs[KEY_DUAL_INNER_RATIO] ?: 0.50f
            val mapMode = prefs[KEY_MAP_MODE]?.let { runCatching { MapEngineMode.valueOf(it) }.getOrNull() }
                ?: MapEngineMode.VECTOR_DARK
            val browserAgent = prefs[KEY_BROWSER_AGENT]?.let { runCatching { BrowserAgentProfile.valueOf(it) }.getOrNull() }
                ?: BrowserAgentProfile.CHROME_DESKTOP
            val webUrl = prefs[KEY_WEB_URL] ?: "https://www.youtube.com/?app=desktop"
            val dimmer = prefs[KEY_OLED_DIMMER] ?: false
            val bridgeUrl = prefs[KEY_MYQ_BRIDGE_URL] ?: "http://192.168.1.50:8080"
            val myqMode = prefs[KEY_MYQ_MODE]?.let { runCatching { MyQConnectionMode.valueOf(it) }.getOrNull() }
                ?: MyQConnectionMode.INTERACTIVE_PREVIEW

            SeltosTelemetryRepository.setAmbientMood(mood)
            SeltosTelemetryRepository.updateMyQ {
                it.copy(mode = myqMode, localBridgeUrl = bridgeUrl)
            }

            _uiState.update {
                it.copy(
                    speedUnit = unit,
                    ambientMood = mood,
                    splitRatio = ratio.coerceIn(0.24f, 0.44f),
                    isLeftDockCollapsed = dockCollapsed,
                    activeTab = activeTab,
                    dualPrimaryModule = dualPrimary,
                    dualSecondaryModule = dualSecondary,
                    dualHorizontalSplit = dualHoriz,
                    dualInnerRatio = dualInner.coerceIn(0.30f, 0.70f),
                    mapEngineMode = mapMode,
                    browserAgent = browserAgent,
                    webCurrentUrl = webUrl,
                    oledNightDimmer = dimmer
                )
            }
        }
    }

    private fun observeSharedTelemetry() {
        viewModelScope.launch {
            SeltosTelemetryRepository.gps.collect { gps ->
                _uiState.update { it.copy(gps = gps) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.obd.collect { obd ->
                _uiState.update { it.copy(obd = obd) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.scannedDevices.collect { devices ->
                _uiState.update { it.copy(scannedBleDevices = devices) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.isScanningBle.collect { scanning ->
                _uiState.update { it.copy(isScanningBle = scanning) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.myQ.collect { myQ ->
                _uiState.update { it.copy(myQ = myQ) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.headlightsOn.collect { on ->
                _uiState.update { it.copy(headlightsOn = on) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.ambientMood.collect { mood ->
                _uiState.update { it.copy(ambientMood = mood) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.doorStatus.collect { doors ->
                _uiState.update { it.copy(doorStatus = doors) }
            }
        }
        viewModelScope.launch {
            SeltosTelemetryRepository.mediaNowPlaying.collect { (title, playing) ->
                _uiState.update {
                    it.copy(currentMediaTitle = title, isMediaPlaying = playing)
                }
            }
        }
    }

    private fun startSystemStatusLoop() {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            while (isActive) {
                val timeStr = timeFormat.format(Date())
                val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 90) ?: 90
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val pct = ((level * 100f) / scale.coerceAtLeast(1)).toInt().coerceIn(0, 100)
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

                val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                val hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

                val currentVol = audioManager?.let {
                    val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
                } ?: _uiState.value.mediaVolume

                _uiState.update {
                    it.copy(
                        currentTimeText = timeStr,
                        batteryPct = pct,
                        isCharging = charging,
                        wifiConnected = hasWifi,
                        cellularSignalLevel = if (hasCell || hasWifi) 4 else 1,
                        mediaVolume = currentVol
                    )
                }
                delay(4000L)
            }
        }
    }

    fun startLocationTelemetry() {
        locationManager.startUpdates()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = AppLauncherHelper.queryLaunchableApps(appContext)
            val browsers = AppLauncherHelper.queryInstalledBrowsers(appContext)
            _uiState.update { it.copy(installedApps = apps, installedBrowsers = browsers) }
        }
    }

    fun selectTab(tab: StageTab) {
        _uiState.update { it.copy(activeTab = tab) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_ACTIVE_TAB] = tab.name }
        }
    }

    fun updateSplitRatio(deltaFraction: Float) {
        _uiState.update { state ->
            val next = (state.splitRatio + deltaFraction).coerceIn(0.24f, 0.44f)
            state.copy(splitRatio = next, isLeftDockCollapsed = false)
        }
    }

    fun persistSplitRatio() {
        val ratio = _uiState.value.splitRatio
        val collapsed = _uiState.value.isLeftDockCollapsed
        viewModelScope.launch {
            appContext.seltosDataStore.edit {
                it[KEY_SPLIT_RATIO] = ratio
                it[KEY_DOCK_COLLAPSED] = collapsed
            }
        }
    }

    fun toggleLeftDockCollapse() {
        val next = !_uiState.value.isLeftDockCollapsed
        _uiState.update { it.copy(isLeftDockCollapsed = next) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_DOCK_COLLAPSED] = next }
        }
    }

    fun toggleSpeedUnit() {
        val next = if (_uiState.value.speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
        _uiState.update { it.copy(speedUnit = next) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_SPEED_UNIT] = next.name }
        }
    }

    fun toggleHeadlights() {
        SeltosTelemetryRepository.setHeadlights(!SeltosTelemetryRepository.headlightsOn.value)
    }

    fun cycleAmbientMood() {
        val moods = AmbientMood.entries
        val nextIdx = (moods.indexOf(_uiState.value.ambientMood) + 1) % moods.size
        selectAmbientMood(moods[nextIdx])
    }

    fun selectAmbientMood(mood: AmbientMood) {
        SeltosTelemetryRepository.setAmbientMood(mood)
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_AMBIENT_MOOD] = mood.name }
        }
    }

    fun toggleDoor(zone: String) {
        SeltosTelemetryRepository.updateDoors { d ->
            when (zone) {
                "FL" -> d.copy(frontLeftOpen = !d.frontLeftOpen)
                "FR" -> d.copy(frontRightOpen = !d.frontRightOpen)
                "RL" -> d.copy(rearLeftOpen = !d.rearLeftOpen)
                "RR" -> d.copy(rearRightOpen = !d.rearRightOpen)
                "TRUNK" -> d.copy(trunkOpen = !d.trunkOpen)
                "LOCK" -> d.copy(doorsLocked = !d.doorsLocked)
                else -> d
            }
        }
    }

    fun setScreenBrightness(value: Float) {
        _uiState.update { it.copy(screenBrightness = value.coerceIn(0.1f, 1f)) }
    }

    fun setMediaVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(mediaVolume = clamped) }
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            it.setStreamVolume(AudioManager.STREAM_MUSIC, (clamped * max).toInt(), 0)
        }
    }

    fun toggleOledNightDimmer() {
        val next = !_uiState.value.oledNightDimmer
        _uiState.update { it.copy(oledNightDimmer = next) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_OLED_DIMMER] = next }
        }
    }

    fun toggleKeepScreenAwake() {
        _uiState.update { it.copy(keepScreenAwake = !it.keepScreenAwake) }
    }

    fun toggleBackgroundPlayback() {
        _uiState.update { it.copy(backgroundPlaybackEnabled = !it.backgroundPlaybackEnabled) }
    }

    fun setObdSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showObdSheet = visible) }
    }

    fun startObdBleScan() {
        Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_START_SCAN)
    }

    fun stopObdBleScan() {
        Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_STOP_SCAN)
    }

    fun connectObdBleDevice(address: String, name: String) {
        Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_CONNECT, address, name)
    }

    fun disconnectObdBle() {
        Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_DISCONNECT)
        Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_STOP_DEMO)
    }

    fun toggleObdDemoMode() {
        if (_uiState.value.obd.demoMode) {
            Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_STOP_DEMO)
        } else {
            Obd2BleService.sendAction(appContext, Obd2BleService.ACTION_START_DEMO)
        }
    }

    fun setDualPrimaryModule(module: DualViewModule) {
        _uiState.update { it.copy(dualPrimaryModule = module) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_DUAL_PRIMARY] = module.name }
        }
    }

    fun setDualSecondaryModule(module: DualViewModule) {
        _uiState.update { it.copy(dualSecondaryModule = module) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_DUAL_SECONDARY] = module.name }
        }
    }

    fun toggleDualOrientation() {
        val next = !_uiState.value.dualHorizontalSplit
        _uiState.update { it.copy(dualHorizontalSplit = next) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_DUAL_HORIZONTAL] = next }
        }
    }

    fun updateDualInnerRatio(delta: Float) {
        val next = (_uiState.value.dualInnerRatio + delta).coerceIn(0.30f, 0.70f)
        _uiState.update { it.copy(dualInnerRatio = next) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_DUAL_INNER_RATIO] = next }
        }
    }

    fun setMapEngineMode(mode: MapEngineMode) {
        _uiState.update { it.copy(mapEngineMode = mode) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_MAP_MODE] = mode.name }
        }
    }

    fun setBrowserAgent(profile: BrowserAgentProfile) {
        _uiState.update { it.copy(browserAgent = profile) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_BROWSER_AGENT] = profile.name }
        }
    }

    fun selectMediaItem(entry: MediaItemEntry) {
        _uiState.update {
            it.copy(
                currentMediaTitle = entry.title,
                currentMediaUri = entry.uri
            )
        }
    }

    fun addCustomMediaUri(title: String, uri: String) {
        val cleanUri = uri.trim()
        if (cleanUri.isBlank()) return
        val type = when {
            cleanUri.contains(".m3u8", ignoreCase = true) -> MediaSourceType.IPTV_HLS
            cleanUri.startsWith("content://") || cleanUri.startsWith("file://") -> MediaSourceType.LOCAL_USB
            else -> MediaSourceType.DIRECT_URL
        }
        val entry = MediaItemEntry(
            id = "custom_${System.currentTimeMillis()}",
            title = title.ifBlank { cleanUri.substringAfterLast('/').ifBlank { "Stream" } },
            uri = cleanUri,
            type = type,
            group = if (type == MediaSourceType.IPTV_HLS) "IPTV" else "Local"
        )
        _uiState.update {
            it.copy(
                mediaPlaylist = listOf(entry) + it.mediaPlaylist,
                currentMediaTitle = entry.title,
                currentMediaUri = entry.uri
            )
        }
    }

    fun loadM3uPlaylistFromText(rawM3u: String) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.Default) {
                IptvPlaylistParser.parseM3uContent(rawM3u)
            }
            if (parsed.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        mediaPlaylist = parsed + it.mediaPlaylist,
                        currentMediaTitle = parsed.first().title,
                        currentMediaUri = parsed.first().uri,
                        toastMessage = "${parsed.size} canales IPTV"
                    )
                }
            } else {
                showToast("Sin canales M3U válidos")
            }
        }
    }

    fun navigateWebUrl(url: String) {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) {
            url.trim()
        } else {
            "https://${url.trim()}"
        }
        _uiState.update { it.copy(webCurrentUrl = normalized) }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { it[KEY_WEB_URL] = normalized }
        }
    }

    fun toggleAdBlock() {
        _uiState.update { it.copy(adBlockEnabled = !it.adBlockEnabled) }
    }

    fun openUrlInExternalBrowser(url: String, packageName: String? = null) {
        val ok = AppLauncherHelper.openUrlInBrowser(
            context = appContext,
            url = url,
            browserPackageName = packageName,
            splitScreen = true
        )
        if (!ok) {
            showToast("Sin navegador externo disponible")
        }
    }

    fun setMyQConfigVisible(visible: Boolean) {
        _uiState.update { it.copy(showMyQConfigDialog = visible) }
    }

    fun configureMyQ(mode: MyQConnectionMode, bridgeUrl: String, bearerToken: String) {
        SeltosTelemetryRepository.updateMyQ {
            it.copy(
                mode = mode,
                localBridgeUrl = bridgeUrl.trim(),
                bearerToken = bearerToken.trim(),
                statusMessage = mode.label
            )
        }
        viewModelScope.launch {
            appContext.seltosDataStore.edit { prefs ->
                prefs[KEY_MYQ_MODE] = mode.name
                prefs[KEY_MYQ_BRIDGE_URL] = bridgeUrl.trim()
            }
        }
        if (mode == MyQConnectionMode.CLOUD_MYQ && bearerToken.isNotBlank()) {
            myQClient.fetchMyQCloudDevices(bearerToken)
        }
    }

    fun triggerMyQDoor(command: MyQProtocolClient.DoorCommand) {
        myQClient.executeDoorCommand(command)
    }

    fun toggleMyQGarageLight() {
        myQClient.toggleGarageLight()
    }

    fun launchAppInSplitScreen(packageName: String) {
        val launched = AppLauncherHelper.launchInSplitScreen(appContext, packageName)
        if (!launched) {
            showToast("No se pudo abrir $packageName")
        }
    }

    fun launchExternalNavigation() {
        val ok = AppLauncherHelper.launchNavigationSplit(appContext)
        if (!ok) {
            selectTab(StageTab.MAP_NAV)
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        locationManager.stopUpdates()
        super.onCleared()
    }

    companion object {
        private val KEY_SPEED_UNIT = stringPreferencesKey("speed_unit")
        private val KEY_AMBIENT_MOOD = stringPreferencesKey("ambient_mood")
        private val KEY_SPLIT_RATIO = floatPreferencesKey("split_ratio")
        private val KEY_DOCK_COLLAPSED = booleanPreferencesKey("dock_collapsed")
        private val KEY_ACTIVE_TAB = stringPreferencesKey("active_tab")
        private val KEY_DUAL_PRIMARY = stringPreferencesKey("dual_primary")
        private val KEY_DUAL_SECONDARY = stringPreferencesKey("dual_secondary")
        private val KEY_DUAL_HORIZONTAL = booleanPreferencesKey("dual_horizontal")
        private val KEY_DUAL_INNER_RATIO = floatPreferencesKey("dual_inner_ratio")
        private val KEY_MAP_MODE = stringPreferencesKey("map_mode")
        private val KEY_BROWSER_AGENT = stringPreferencesKey("browser_agent")
        private val KEY_WEB_URL = stringPreferencesKey("web_url")
        private val KEY_OLED_DIMMER = booleanPreferencesKey("oled_dimmer")
        private val KEY_MYQ_MODE = stringPreferencesKey("myq_mode")
        private val KEY_MYQ_BRIDGE_URL = stringPreferencesKey("myq_bridge_url")
    }
}
