package com.example.telemetry

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.model.BleDeviceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

/**
 * Foreground Android Service managing Bluetooth Low Energy (BLE) GATT communication
 * with ELM327 / OBD-II adapters, streaming live RPM, coolant temperature, speed,
 * throttle, battery voltage, and fuel economy into SeltosTelemetryRepository.
 */
class Obd2BleService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): Obd2BleService = this@Obd2BleService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private var pollingJob: Job? = null
    private var demoJob: Job? = null
    private var scanJob: Job? = null

    private val discoveredMap = LinkedHashMap<String, BleDeviceInfo>()
    private val responseBuffer = StringBuilder()
    private var pendingCommandDeferred: CompletableDeferred<String>? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> startBleScan()
            ACTION_STOP_SCAN -> stopBleScan()
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                val name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "OBD-II BLE"
                if (!address.isNullOrBlank()) {
                    connectToDevice(address, name)
                }
            }
            ACTION_DISCONNECT -> disconnectGatt()
            ACTION_START_DEMO -> startDemoMode()
            ACTION_STOP_DEMO -> stopDemoMode()
        }
        return START_STICKY
    }

    private fun startForegroundNotification(statusText: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SeltosMate OBD-II BLE")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OBD-II BLE Telemetry",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            SeltosTelemetryRepository.updateObd {
                it.copy(statusNote = "Enable Bluetooth to scan ELM327 BLE")
            }
            return
        }
        bleScanner = adapter.bluetoothLeScanner
        val scanner = bleScanner ?: return

        discoveredMap.clear()
        SeltosTelemetryRepository.setScannedDevices(emptyList())
        SeltosTelemetryRepository.setScanningBle(true)
        SeltosTelemetryRepository.updateObd {
            it.copy(statusNote = "Scanning for OBD-II BLE adapters...")
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (se: SecurityException) {
            Log.e(TAG, "Missing BLE scan permission", se)
            SeltosTelemetryRepository.setScanningBle(false)
            SeltosTelemetryRepository.updateObd {
                it.copy(statusNote = "Bluetooth Scan permission required")
            }
            return
        }

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            delay(12_000L)
            stopBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        scanJob?.cancel()
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        SeltosTelemetryRepository.setScanningBle(false)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = try {
                device.name ?: result.scanRecord?.deviceName ?: "BLE Device"
            } catch (_: SecurityException) {
                "BLE Device"
            }
            val address = device.address ?: return
            val info = BleDeviceInfo(name = name, address = address, rssi = result.rssi)
            discoveredMap[address] = info

            // Prioritize common OBD-II adapter names at the top
            val sorted = discoveredMap.values.sortedWith(
                compareByDescending<BleDeviceInfo> {
                    val lower = it.name.lowercase()
                    lower.contains("obd") || lower.contains("elm") ||
                        lower.contains("vgate") || lower.contains("veepeak") ||
                        lower.contains("carista") || lower.contains("ios-vlink")
                }.thenByDescending { it.rssi }
            )
            SeltosTelemetryRepository.setScannedDevices(sorted)
        }

        override fun onScanFailed(errorCode: Int) {
            SeltosTelemetryRepository.setScanningBle(false)
            SeltosTelemetryRepository.updateObd {
                it.copy(statusNote = "BLE scan failed (code $errorCode)")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, displayName: String) {
        stopBleScan()
        stopDemoMode()
        disconnectGatt()

        val adapter = bluetoothAdapter ?: return
        startForegroundNotification("Connecting to $displayName...")
        SeltosTelemetryRepository.updateObd {
            it.copy(
                connecting = true,
                connected = false,
                demoMode = false,
                deviceName = displayName,
                deviceAddress = address,
                statusNote = "Connecting GATT to $displayName..."
            )
        }

        try {
            val device = adapter.getRemoteDevice(address)
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(this, false, gattCallback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(this, false, gattCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "GATT connect error", e)
            SeltosTelemetryRepository.updateObd {
                it.copy(connecting = false, connected = false, statusNote = "GATT error: ${e.message}")
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                SeltosTelemetryRepository.updateObd {
                    it.copy(statusNote = "Discovering OBD-II GATT services...")
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                pollingJob?.cancel()
                SeltosTelemetryRepository.updateObd {
                    it.copy(
                        connected = false,
                        connecting = false,
                        statusNote = "OBD-II disconnected"
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                SeltosTelemetryRepository.updateObd {
                    it.copy(connecting = false, statusNote = "Service discovery failed ($status)")
                }
                return
            }

            // Locate ELM327 / BLE UART write + notify characteristics
            var foundWrite: BluetoothGattCharacteristic? = null
            var foundNotify: BluetoothGattCharacteristic? = null

            val preferredServices = gatt.services.sortedByDescending { svc ->
                val uuid = svc.uuid.toString().lowercase()
                uuid.startsWith("0000fff0") || uuid.startsWith("0000ffe0") || uuid.startsWith("6e400001")
            }

            for (service in preferredServices) {
                for (char in service.characteristics) {
                    val props = char.properties
                    val canWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                    val canNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                        (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

                    if (canWrite && foundWrite == null) foundWrite = char
                    if (canNotify && foundNotify == null) foundNotify = char
                }
                if (foundWrite != null && foundNotify != null) break
            }

            if (foundWrite == null || foundNotify == null) {
                SeltosTelemetryRepository.updateObd {
                    it.copy(connecting = false, statusNote = "No compatible ELM327 GATT UART found")
                }
                return
            }

            writeCharacteristic = foundWrite
            notifyCharacteristic = foundNotify

            gatt.setCharacteristicNotification(foundNotify, true)
            val cccd = foundNotify.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(cccd)
                }
            }

            SeltosTelemetryRepository.updateObd {
                it.copy(
                    connected = true,
                    connecting = false,
                    statusNote = "ELM327 Connected — Streaming live PIDs"
                )
            }
            startForegroundNotification("Streaming OBD-II from ${SeltosTelemetryRepository.obd.value.deviceName}")
            startPidPollingLoop()
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val bytes = characteristic.value ?: return
            handleIncomingBytes(bytes)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val chunk = bytes.decodeToString()
        synchronized(responseBuffer) {
            responseBuffer.append(chunk)
            if (responseBuffer.contains(">")) {
                val full = responseBuffer.toString()
                responseBuffer.clear()
                pendingCommandDeferred?.complete(full)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendObdCommand(command: String, timeoutMs: Long = 1500L): String {
        val gatt = bluetoothGatt ?: return ""
        val writeChar = writeCharacteristic ?: return ""
        val deferred = CompletableDeferred<String>()
        pendingCommandDeferred = deferred
        synchronized(responseBuffer) { responseBuffer.clear() }

        val payload = "$command\r".toByteArray(Charsets.US_ASCII)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val writeType = if ((writeChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                gatt.writeCharacteristic(writeChar, payload, writeType)
            } else {
                @Suppress("DEPRECATION")
                writeChar.value = payload
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(writeChar)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Write error for $command", e)
            return ""
        }

        return withTimeoutOrNull(timeoutMs) { deferred.await() } ?: ""
    }

    private fun startPidPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            // Standard ELM327 initialization sequence
            sendObdCommand("ATZ", 2000L)
            delay(250)
            sendObdCommand("ATE0")
            sendObdCommand("ATL0")
            sendObdCommand("ATS0")
            sendObdCommand("ATSP0")

            val pids = listOf("010C", "0105", "010D", "0110", "0111", "0104", "ATRV")
            while (isActive && SeltosTelemetryRepository.obd.value.connected) {
                for (pid in pids) {
                    if (!isActive) break
                    val raw = sendObdCommand(pid, 1200L)
                    if (raw.isNotBlank()) {
                        SeltosTelemetryRepository.updateObd { current ->
                            Obd2PidParser.parseResponse(raw, current)
                        }
                    }
                    delay(60)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        pollingJob?.cancel()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {
        }
        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null
        SeltosTelemetryRepository.updateObd {
            it.copy(
                connected = false,
                connecting = false,
                statusNote = "Disconnected — Tap to scan BLE OBD-II"
            )
        }
    }

    /**
     * Bench / Preview simulation stream clearly flagged with demoMode = true
     * when testing UI telemetry without a physical Kia Seltos BLE dongle attached.
     */
    fun startDemoMode() {
        disconnectGatt()
        demoJob?.cancel()
        startForegroundNotification("OBD-II Bench Demo Mode active")
        SeltosTelemetryRepository.updateObd {
            it.copy(
                connected = true,
                connecting = false,
                demoMode = true,
                deviceName = "Kia Seltos 2.0L (Simulated)",
                deviceAddress = "DEMO:OBD2:BLE",
                statusNote = "Simulated OBD-II Bench Telemetry"
            )
        }
        demoJob = serviceScope.launch {
            var t = 0.0
            while (isActive) {
                t += 0.18
                val rpm = (1650 + 950 * sin(t * 0.7) + 320 * sin(t * 1.9)).toInt().coerceIn(780, 4800)
                val speed = (68 + 22 * sin(t * 0.35)).toInt().coerceIn(0, 130)
                val coolant = (88 + (3 * sin(t * 0.1))).toInt()
                val throttle = (24 + 18 * abs(sin(t * 0.65))).toInt().coerceIn(8, 75)
                val fuel = (7.4f + 2.3f * abs(sin(t * 0.5).toFloat()))
                val load = (32 + 24 * abs(sin(t * 0.55))).toInt().coerceIn(14, 88)
                val volts = 14.1f + 0.1f * sin(t * 0.3).toFloat()

                SeltosTelemetryRepository.updateObd {
                    it.copy(
                        connected = true,
                        demoMode = true,
                        rpm = rpm,
                        coolantTempC = coolant,
                        speedKmh = speed,
                        fuelEconomyLPer100Km = fuel,
                        throttlePct = throttle,
                        engineLoadPct = load,
                        batteryVoltage = volts,
                        intakeTempC = 29,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                }
                delay(250)
            }
        }
    }

    fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
        if (SeltosTelemetryRepository.obd.value.demoMode) {
            SeltosTelemetryRepository.updateObd {
                it.copy(
                    connected = false,
                    demoMode = false,
                    rpm = 0,
                    coolantTempC = 0,
                    fuelEconomyLPer100Km = 0f,
                    throttlePct = 0,
                    batteryVoltage = 0f,
                    deviceName = "OBD-II Disconnected",
                    statusNote = "Tap to scan BLE ELM327"
                )
            }
        }
    }

    override fun onDestroy() {
        stopBleScan()
        disconnectGatt()
        stopDemoMode()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Obd2BleService"
        private const val CHANNEL_ID = "seltosmate_obd_channel"
        private const val NOTIFICATION_ID = 2021
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val ACTION_START_SCAN = "com.example.obd.START_SCAN"
        const val ACTION_STOP_SCAN = "com.example.obd.STOP_SCAN"
        const val ACTION_CONNECT = "com.example.obd.CONNECT"
        const val ACTION_DISCONNECT = "com.example.obd.DISCONNECT"
        const val ACTION_START_DEMO = "com.example.obd.START_DEMO"
        const val ACTION_STOP_DEMO = "com.example.obd.STOP_DEMO"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
        const val EXTRA_DEVICE_NAME = "extra_device_name"

        fun sendAction(context: Context, action: String, address: String? = null, name: String? = null) {
            val intent = Intent(context, Obd2BleService::class.java).apply {
                this.action = action
                address?.let { putExtra(EXTRA_DEVICE_ADDRESS, it) }
                name?.let { putExtra(EXTRA_DEVICE_NAME, it) }
            }
            context.startService(intent)
        }
    }
}
