package com.medfind.maroc.domain.search

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Distance orthodromique (formule de haversine), en kilomètres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** « 850 m », « 1,4 km », « 23 km » (format français). */
    fun formatDistance(km: Double): String = when {
        km < 1.0 -> "${(km * 1000).toInt().coerceAtLeast(10) / 10 * 10} m"
        km < 10.0 -> String.format(Locale.FRANCE, "%.1f km", km)
        else -> "${km.toInt()} km"
    }

    fun isValidCoordinate(lat: Double?, lon: Double?): Boolean =
        lat != null && lon != null && !lat.isNaN() && !lon.isNaN() &&
            lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)
}
