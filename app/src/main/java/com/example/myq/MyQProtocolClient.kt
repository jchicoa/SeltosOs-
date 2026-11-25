package com.example.myq

import com.example.model.MyQConnectionMode
import com.example.model.MyQDoorState
import com.example.telemetry.SeltosTelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Chamberlain MyQ Protocol Client supporting:
 * 1. MyQ Cloud v6 REST Protocol (Account discovery + GarageDoorOpener state/actions)
 * 2. Local MyQ / ratgdo / HomeBridge HTTP Bridge (LAN/Hotspot zero-cloud control)
 * 3. Interactive Garage Studio mode for offline testing & visual feedback
 */
class MyQProtocolClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var travelJob: Job? = null

    fun executeDoorCommand(command: DoorCommand) {
        val state = SeltosTelemetryRepository.myQ.value
        when (state.mode) {
            MyQConnectionMode.INTERACTIVE_PREVIEW -> simulateDoorTravel(command)
            MyQConnectionMode.LOCAL_BRIDGE -> sendLocalBridgeCommand(state.localBridgeUrl, command)
            MyQConnectionMode.CLOUD_MYQ -> sendMyQCloudCommand(
                bearerToken = state.bearerToken,
                accountId = state.accountId,
                deviceSerial = state.deviceSerial,
                command = command
            )
        }
    }

    fun toggleGarageLight() {
        val current = SeltosTelemetryRepository.myQ.value
        val nextLight = !current.garageLightOn
        SeltosTelemetryRepository.updateMyQ {
            it.copy(
                garageLightOn = nextLight,
                statusMessage = if (nextLight) "Garage Work Light ON" else "Garage Work Light OFF"
            )
        }
        if (current.mode == MyQConnectionMode.LOCAL_BRIDGE && current.localBridgeUrl.isNotBlank()) {
            scope.launch {
                runCatching {
                    val base = current.localBridgeUrl.trimEnd('/')
                    val req = Request.Builder()
                        .url("$base/light/${if (nextLight) "on" else "off"}")
                        .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                    client.newCall(req).execute().close()
                }
            }
        }
    }

    fun fetchMyQCloudDevices(bearerToken: String) {
        if (bearerToken.isBlank()) {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(statusMessage = "Enter a valid MyQ OAuth Bearer token")
            }
            return
        }
        scope.launch {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(isBusy = true, statusMessage = "Querying Chamberlain MyQ v6 accounts...")
            }
            try {
                val acctReq = Request.Builder()
                    .url("https://accounts.myq-cloud.com/api/v6.0/Accounts")
                    .addHeader("Authorization", "Bearer ${bearerToken.trim()}")
                    .addHeader("MyQApplicationId", MYQ_APP_ID)
                    .addHeader("User-Agent", MYQ_USER_AGENT)
                    .get()
                    .build()

                val acctBody = client.newCall(acctReq).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("MyQ Auth HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
                val acctJson = JSONObject(acctBody)
                val items = acctJson.optJSONArray("items") ?: JSONArray()
                if (items.length() == 0) throw IllegalStateException("No MyQ accounts found")
                val accountId = items.getJSONObject(0).getString("id")

                val devReq = Request.Builder()
                    .url("https://devices.myq-cloud.com/api/v6.0/Accounts/$accountId/Devices")
                    .addHeader("Authorization", "Bearer ${bearerToken.trim()}")
                    .addHeader("MyQApplicationId", MYQ_APP_ID)
                    .addHeader("User-Agent", MYQ_USER_AGENT)
                    .get()
                    .build()

                val devBody = client.newCall(devReq).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("MyQ Devices HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
                val devJson = JSONObject(devBody)
                val devItems = devJson.optJSONArray("items") ?: JSONArray()
                var serial = ""
                var doorName = "MyQ Garage Door"
                var doorStateStr = "closed"

                for (i in 0 until devItems.length()) {
                    val dev = devItems.getJSONObject(i)
                    val family = dev.optString("device_family")
                    if (family == "garagedoor") {
                        serial = dev.optString("serial_number")
                        doorName = dev.optString("name", doorName)
                        doorStateStr = dev.optJSONObject("state")?.optString("door_state", "closed") ?: "closed"
                        break
                    }
                }

                val parsedState = parseMyQStateString(doorStateStr)
                val fraction = if (parsedState == MyQDoorState.OPEN) 1f else 0f

                SeltosTelemetryRepository.updateMyQ {
                    it.copy(
                        isBusy = false,
                        bearerToken = bearerToken.trim(),
                        accountId = accountId,
                        deviceSerial = serial.ifBlank { it.deviceSerial },
                        doorName = doorName,
                        doorState = parsedState,
                        doorOpenFraction = fraction,
                        statusMessage = "MyQ Cloud Synced: $doorName ($doorStateStr)"
                    )
                }
            } catch (e: Exception) {
                SeltosTelemetryRepository.updateMyQ {
                    it.copy(isBusy = false, statusMessage = "MyQ Cloud error: ${e.message}")
                }
            }
        }
    }

    private fun sendMyQCloudCommand(
        bearerToken: String,
        accountId: String,
        deviceSerial: String,
        command: DoorCommand
    ) {
        if (bearerToken.isBlank() || accountId.isBlank() || deviceSerial.isBlank()) {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(statusMessage = "Configure MyQ Cloud token & account first")
            }
            return
        }
        scope.launch {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(isBusy = true, statusMessage = "Sending MyQ ${command.apiAction} command...")
            }
            try {
                val payload = JSONObject().put("action_type", command.apiAction).toString()
                val req = Request.Builder()
                    .url("https://devices.myq-cloud.com/api/v6.0/Accounts/$accountId/Devices/$deviceSerial/actions")
                    .addHeader("Authorization", "Bearer $bearerToken")
                    .addHeader("MyQApplicationId", MYQ_APP_ID)
                    .addHeader("User-Agent", MYQ_USER_AGENT)
                    .put(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                }
                SeltosTelemetryRepository.updateMyQ { it.copy(isBusy = false) }
                simulateDoorTravel(command, prefix = "MyQ Cloud: ")
            } catch (e: Exception) {
                SeltosTelemetryRepository.updateMyQ {
                    it.copy(isBusy = false, statusMessage = "MyQ command failed: ${e.message}")
                }
            }
        }
    }

    private fun sendLocalBridgeCommand(baseUrl: String, command: DoorCommand) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        if (cleanUrl.isBlank()) {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(statusMessage = "Enter Local Bridge URL (e.g. http://192.168.1.50:8080)")
            }
            return
        }
        scope.launch {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(isBusy = true, statusMessage = "Sending to Bridge ${command.apiAction}...")
            }
            try {
                val req = Request.Builder()
                    .url("$cleanUrl/door/${command.apiAction}")
                    .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("Bridge HTTP ${resp.code}")
                }
                SeltosTelemetryRepository.updateMyQ { it.copy(isBusy = false) }
                simulateDoorTravel(command, prefix = "Local Bridge: ")
            } catch (e: Exception) {
                SeltosTelemetryRepository.updateMyQ {
                    it.copy(isBusy = false, statusMessage = "Bridge unreachable: ${e.message}")
                }
            }
        }
    }

    private fun simulateDoorTravel(command: DoorCommand, prefix: String = "MyQ Protocol: ") {
        travelJob?.cancel()
        if (command == DoorCommand.STOP) {
            val frac = SeltosTelemetryRepository.myQ.value.doorOpenFraction
            SeltosTelemetryRepository.setMyQDoorState(
                MyQDoorState.STOPPED,
                frac,
                "${prefix}Door stopped at ${(frac * 100).toInt()}%"
            )
            return
        }

        val target = if (command == DoorCommand.OPEN) 1f else 0f
        val transitionState = if (command == DoorCommand.OPEN) MyQDoorState.OPENING else MyQDoorState.CLOSING
        val finalState = if (command == DoorCommand.OPEN) MyQDoorState.OPEN else MyQDoorState.CLOSED

        travelJob = scope.launch {
            SeltosTelemetryRepository.updateMyQ {
                it.copy(
                    doorState = transitionState,
                    garageLightOn = true,
                    statusMessage = "${prefix}${transitionState.label}"
                )
            }
            val start = SeltosTelemetryRepository.myQ.value.doorOpenFraction
            val steps = 36
            for (i in 1..steps) {
                val p = start + (target - start) * (i / steps.toFloat())
                SeltosTelemetryRepository.setMyQDoorState(
                    transitionState,
                    p,
                    "${prefix}${transitionState.label} (${(p * 100).toInt()}%)"
                )
                delay(65)
            }
            SeltosTelemetryRepository.setMyQDoorState(
                finalState,
                target,
                "${prefix}Bay ${finalState.label}"
            )
        }
    }

    private fun parseMyQStateString(raw: String): MyQDoorState = when (raw.lowercase()) {
        "open" -> MyQDoorState.OPEN
        "closed" -> MyQDoorState.CLOSED
        "opening" -> MyQDoorState.OPENING
        "closing" -> MyQDoorState.CLOSING
        "stopped" -> MyQDoorState.STOPPED
        else -> MyQDoorState.UNKNOWN
    }

    enum class DoorCommand(val apiAction: String) {
        OPEN("open"),
        CLOSE("close"),
        STOP("stop")
    }

    companion object {
        private const val MYQ_APP_ID = "JVM/G9Nwih5BwKgNCjLxiFUHQdijMrY5J5g4d"
        private const val MYQ_USER_AGENT = "myQ/240.0 CFNetwork/1406.0.4 Darwin/22.4.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
