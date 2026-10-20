package com.surajpole.parkingfinder.data.service

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapsInitializer
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object MapPreloader {
    private const val TAG = "MapPreloader"
    @Volatile
    private var isPreloaded = false

    fun preload(context: Context, parkingRepository: ParkingRepository) {
        if (isPreloaded) return
        isPreloaded = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Asynchronously pre-initialize Google Maps with LATEST vector renderer
                MapsInitializer.initialize(
                    context.applicationContext,
                    MapsInitializer.Renderer.LATEST
                ) { renderer ->
                    Log.d(TAG, "Google Maps Vector Renderer pre-warmed: $renderer")
                }

                // 2. Warm up parking lots repository and offline cache
                parkingRepository.seedInitialDataIfEmpty()
                parkingRepository.observeParkingLots().firstOrNull()

                // 3. Warm up FusedLocationProviderClient to pre-fetch GPS hardware state
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            Log.d(TAG, "GPS location pre-fetched: ${loc.latitude}, ${loc.longitude}")
                        }
                    }
                } catch (_: SecurityException) {
                    // Normal behavior: permission might be granted later on privacy/home screen
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background preloading notice: ${e.message}")
            }
        }
    }
}
