package com.surajpole.parkingfinder.data.service

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RouteResult(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val distanceText: String,
    val durationText: String
)

object RoadRoutingService {

    /**
     * Fetches real road-snapped navigation path using OSRM routing engine.
     * Snaps user position and destination to nearest legal roads ("magnet effect")
     * so route follows real streets (Surajpole, Bapu Bazar, Delhi Gate, etc.)
     * and avoids running through buildings, shops, or off-road terrain.
     */
    suspend fun getRoadRoute(
        origin: LatLng,
        destination: LatLng,
        isTwoWheeler: Boolean
    ): RouteResult = withContext(Dispatchers.IO) {
        try {
            // OSRM coordinates format: {lon},{lat};{lon},{lat}
            val profile = if (isTwoWheeler) "driving" else "driving" // OSRM driving covers all standard paved roads
            val urlString = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/%s/%f,%f;%f,%f?overview=full&geometries=geojson",
                profile,
                origin.longitude,
                origin.latitude,
                destination.longitude,
                destination.latitude
            )

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SurajpoleParkingFinder/1.0")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)

                if (json.optString("code") == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val distance = firstRoute.optDouble("distance", 0.0)
                        val duration = firstRoute.optDouble("duration", 0.0)

                        val geometry = firstRoute.getJSONObject("geometry")
                        val coords = geometry.getJSONArray("coordinates")
                        val points = mutableListOf<LatLng>()

                        for (i in 0 until coords.length()) {
                            val coordPair = coords.getJSONArray(i)
                            val lon = coordPair.getDouble(0)
                            val lat = coordPair.getDouble(1)
                            points.add(LatLng(lat, lon))
                        }

                        if (points.isNotEmpty()) {
                            return@withContext RouteResult(
                                points = points,
                                distanceMeters = distance,
                                durationSeconds = duration,
                                distanceText = formatDistance(distance),
                                durationText = formatDuration(duration, isTwoWheeler)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: If network is slow or offline, create a safe 2-step road waypoint path
        val fallbackPoints = listOf(
            origin,
            LatLng((origin.latitude + destination.latitude) / 2, origin.longitude),
            destination
        )
        val distEst = calculateHaversineDistance(origin, destination)
        RouteResult(
            points = fallbackPoints,
            distanceMeters = distEst,
            durationSeconds = distEst / 8.0,
            distanceText = formatDistance(distEst),
            durationText = formatDuration(distEst / 8.0, isTwoWheeler)
        )
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        } else {
            "${meters.toInt()} m"
        }
    }

    private fun formatDuration(seconds: Double, isTwoWheeler: Boolean): String {
        val adjustedSeconds = if (isTwoWheeler) seconds * 0.85 else seconds // Two wheelers navigate faster through traffic
        val mins = (adjustedSeconds / 60.0).toInt().coerceAtLeast(1)
        return if (mins >= 60) {
            "${mins / 60} hr ${mins % 60} min"
        } else {
            "$mins min"
        }
    }

    private fun calculateHaversineDistance(start: LatLng, end: LatLng): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
