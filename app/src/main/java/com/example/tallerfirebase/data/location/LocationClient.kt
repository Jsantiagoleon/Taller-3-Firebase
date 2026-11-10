package com.example.tallerfirebase.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Rastreo de posición con locationUpdates del FusedLocationProvider
class LocationClient(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    // Se debe verificar el permiso antes de recolectar este Flow
    @SuppressLint("MissingPermission")
    fun locationUpdates(
        intervalMs: Long = UPDATE_INTERVAL_MS,
        minDistanceMeters: Float = MIN_DISTANCE_METERS
    ): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    companion object {
        const val UPDATE_INTERVAL_MS = 3000L
        const val MIN_DISTANCE_METERS = 2f
    }
}
