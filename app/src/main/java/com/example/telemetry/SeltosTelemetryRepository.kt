package com.example.telemetry

import com.example.model.AmbientMood
import com.example.model.BleDeviceInfo
import com.example.model.DoorStatus
import com.example.model.GpsTelemetry
import com.example.model.MyQDoorState
import com.example.model.MyQState
import com.example.model.ObdTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared real-time telemetry bus synchronizing live GPS, BLE OBD-II engine data,
 * Chamberlain MyQ garage state, and vehicle controls across MainActivity,
 * Obd2BleService, and Android Auto's SeltosCarAppService.
 */
object SeltosTelemetryRepository {

    private val _gps = MutableStateFlow(GpsTelemetry())
    val gps: StateFlow<GpsTelemetry> = _gps.asStateFlow()

    private val _obd = MutableStateFlow(ObdTelemetry())
    val obd: StateFlow<ObdTelemetry> = _obd.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _isScanningBle = MutableStateFlow(false)
    val isScanningBle: StateFlow<Boolean> = _isScanningBle.asStateFlow()

    private val _myQ = MutableStateFlow(MyQState())
    val myQ: StateFlow<MyQState> = _myQ.asStateFlow()

    private val _headlightsOn = MutableStateFlow(true)
    val headlightsOn: StateFlow<Boolean> = _headlightsOn.asStateFlow()

    private val _ambientMood = MutableStateFlow(AmbientMood.PEMEX_GREEN)
    val ambientMood: StateFlow<AmbientMood> = _ambientMood.asStateFlow()

    private val _doorStatus = MutableStateFlow(DoorStatus())
    val doorStatus: StateFlow<DoorStatus> = _doorStatus.asStateFlow()

    private val _mediaNowPlaying = MutableStateFlow("Stream HLS" to false)
    val mediaNowPlaying: StateFlow<Pair<String, Boolean>> = _mediaNowPlaying.asStateFlow()

    fun updateGps(gpsTelemetry: GpsTelemetry) {
        _gps.value = gpsTelemetry
    }

    fun updateObd(transform: (ObdTelemetry) -> ObdTelemetry) {
        _obd.update(transform)
    }

    fun setScannedDevices(devices: List<BleDeviceInfo>) {
        _scannedDevices.value = devices
    }

    fun setScanningBle(scanning: Boolean) {
        _isScanningBle.value = scanning
    }

    fun updateMyQ(transform: (MyQState) -> MyQState) {
        _myQ.update(transform)
    }

    fun setHeadlights(on: Boolean) {
        _headlightsOn.value = on
    }

    fun setAmbientMood(mood: AmbientMood) {
        _ambientMood.value = mood
    }

    fun updateDoors(transform: (DoorStatus) -> DoorStatus) {
        _doorStatus.update(transform)
    }

    fun setMediaNowPlaying(title: String, playing: Boolean) {
        _mediaNowPlaying.value = title to playing
    }

    fun setMyQDoorState(state: MyQDoorState, fraction: Float, message: String) {
        _myQ.update {
            it.copy(
                doorState = state,
                doorOpenFraction = fraction.coerceIn(0f, 1f),
                statusMessage = message
            )
        }
    }
}
