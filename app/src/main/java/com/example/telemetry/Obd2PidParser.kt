package com.example.telemetry

import com.example.model.ObdTelemetry

/**
 * Pure Kotlin ELM327 / SAE J1979 OBD-II response parser.
 * Supports Mode 01 PIDs (RPM, Coolant Temp, Speed, MAF, Throttle, Load, Intake Temp) and ATRV voltage.
 */
object Obd2PidParser {

    fun parseResponse(raw: String, current: ObdTelemetry): ObdTelemetry {
        val cleaned = raw
            .replace("\r", "\n")
            .replace(">", "")
            .trim()

        if (cleaned.isEmpty()) return current

        var updated = current
        val lines = cleaned.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        for (line in lines) {
            // Check ATRV voltage format e.g. "14.1V" or "12.6V"
            val voltageMatch = Regex("""(\d{1,2}\.\d{1,2})\s*V""", RegexOption.IGNORE_CASE).find(line)
            if (voltageMatch != null) {
                val v = voltageMatch.groupValues[1].toFloatOrNull()
                if (v != null && v in 5f..20f) {
                    updated = updated.copy(
                        batteryVoltage = v,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                    continue
                }
            }

            // Normalize hex tokens
            val hexTokens = line
                .uppercase()
                .replace(" ", "")
                .chunked(2)
                .filter { it.length == 2 && it.all { ch -> ch in '0'..'9' || ch in 'A'..'F' } }

            if (hexTokens.size < 3 || hexTokens[0] != "41") continue

            val pid = hexTokens[1]
            val a = hexTokens.getOrNull(2)?.toIntOrNull(16) ?: continue
            val b = hexTokens.getOrNull(3)?.toIntOrNull(16) ?: 0

            updated = when (pid) {
                // 010C: Engine RPM = ((A * 256) + B) / 4
                "0C" -> {
                    val rpm = ((a * 256) + b) / 4
                    updated.copy(rpm = rpm, lastUpdatedMs = System.currentTimeMillis())
                }
                // 0105: Coolant Temp = A - 40 (°C)
                "05" -> {
                    val coolant = a - 40
                    updated.copy(coolantTempC = coolant, lastUpdatedMs = System.currentTimeMillis())
                }
                // 010D: Vehicle Speed = A (km/h)
                "0D" -> {
                    updated.copy(speedKmh = a, lastUpdatedMs = System.currentTimeMillis())
                }
                // 0110: MAF Air Flow Rate (g/s) = ((A * 256) + B) / 100.0
                // Convert to L/100km using gasoline AFR 14.7 & density 745 g/L
                "10" -> {
                    val mafGramsPerSec = ((a * 256) + b) / 100f
                    val lPer100Km = calculateFuelEconomyLPer100Km(
                        mafGramsPerSec = mafGramsPerSec,
                        speedKmh = updated.speedKmh
                    )
                    updated.copy(
                        fuelEconomyLPer100Km = lPer100Km,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                }
                // 0111: Throttle Position = (A * 100) / 255
                "11" -> {
                    val throttle = ((a * 100) / 255f).toInt().coerceIn(0, 100)
                    updated.copy(throttlePct = throttle, lastUpdatedMs = System.currentTimeMillis())
                }
                // 0104: Calculated Engine Load = (A * 100) / 255
                "04" -> {
                    val load = ((a * 100) / 255f).toInt().coerceIn(0, 100)
                    updated.copy(engineLoadPct = load, lastUpdatedMs = System.currentTimeMillis())
                }
                // 010F: Intake Air Temperature = A - 40 (°C)
                "0F" -> {
                    val intake = a - 40
                    updated.copy(intakeTempC = intake, lastUpdatedMs = System.currentTimeMillis())
                }
                else -> updated
            }
        }
        return updated
    }

    /**
     * Stoichiometric gasoline fuel consumption calculation:
     * Fuel flow (L/h) = (MAF g/s * 3600) / (14.7 * 745)
     * L/100km = (Fuel flow L/h / speedKmh) * 100
     */
    fun calculateFuelEconomyLPer100Km(mafGramsPerSec: Float, speedKmh: Int): Float {
        if (mafGramsPerSec <= 0.1f) return 0f
        val litersPerHour = (mafGramsPerSec * 3600f) / (14.7f * 745f)
        val effectiveSpeed = speedKmh.coerceAtLeast(10)
        return ((litersPerHour / effectiveSpeed) * 100f).coerceIn(1.5f, 35.0f)
    }
}
