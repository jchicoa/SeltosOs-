package com.example

import com.example.media.IptvPlaylistParser
import com.example.model.ObdTelemetry
import com.example.telemetry.Obd2PidParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeltosParsersUnitTest {

    @Test
    fun `parse OBD2 RPM coolant speed and voltage`() {
        var telemetry = ObdTelemetry()
        // 010C: 41 0C 1A F8 -> ((0x1A * 256) + 0xF8) / 4 = 6904 / 4 = 1726 RPM
        telemetry = Obd2PidParser.parseResponse("41 0C 1A F8>", telemetry)
        assertEquals(1726, telemetry.rpm)

        // 0105: 41 05 82 -> 130 - 40 = 90 C
        telemetry = Obd2PidParser.parseResponse("41 05 82>", telemetry)
        assertEquals(90, telemetry.coolantTempC)

        // 010D: 41 0D 50 -> 80 km/h
        telemetry = Obd2PidParser.parseResponse("41 0D 50>", telemetry)
        assertEquals(80, telemetry.speedKmh)

        // 0110: MAF = 41 10 07 D0 -> 20.00 g/s -> ~8.2 L/100km at 80 km/h
        telemetry = Obd2PidParser.parseResponse("41 10 07 D0>", telemetry)
        assertTrue(telemetry.fuelEconomyLPer100Km in 7.0f..9.5f)

        // ATRV: 14.2V
        telemetry = Obd2PidParser.parseResponse("14.2V>", telemetry)
        assertEquals(14.2f, telemetry.batteryVoltage, 0.01f)
    }

    @Test
    fun `parse M3U IPTV playlist entries`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Seltos Live Sports HD
            https://stream.example.com/sports/index.m3u8
            #EXTINF:-1 group-title="News",World News 4K
            https://stream.example.com/news/master.m3u8
        """.trimIndent()

        val items = IptvPlaylistParser.parseM3uContent(m3u)
        assertEquals(2, items.size)
        assertEquals("Seltos Live Sports HD", items[0].title)
        assertEquals("Sports", items[0].group)
        assertEquals("https://stream.example.com/news/master.m3u8", items[1].uri)
    }
}
