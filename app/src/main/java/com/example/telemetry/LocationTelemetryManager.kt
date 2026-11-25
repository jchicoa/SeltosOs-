package com.example.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.model.GpsTelemetry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Real-time GPS telemetry manager using FusedLocationProviderClient.
 * Strictly reports coordinates, speed, bearing, and altitude without reverse-geocoded street text.
 */
class LocationTelemetryManager(context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var isStarted = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            SeltosTelemetryRepository.updateGps(
                GpsTelemetry(
                    speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                    bearingDeg = if (loc.hasBearing()) loc.bearing else 0f,
                    altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    hasGpsLock = true,
                    accuracyMeters = loc.accuracy
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (isStarted) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isStarted = true
        } catch (_: SecurityException) {
            SeltosTelemetryRepository.updateGps(GpsTelemetry(hasGpsLock = false))
        }
    }

    fun stopUpdates() {
        if (!isStarted) return
        fusedClient.removeLocationUpdates(locationCallback)
        isStarted = false
    }
}
