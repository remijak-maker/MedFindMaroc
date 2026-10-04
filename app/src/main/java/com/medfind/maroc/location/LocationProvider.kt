package com.medfind.maroc.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.medfind.maroc.domain.search.GeoPosition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(val position: GeoPosition) : LocationResult
    data object PermissionMissing : LocationResult
    data object LocationDisabled : LocationResult
    data object Unavailable : LocationResult
}

/**
 * Localisation basée sur le LocationManager d'Android : aucun service payant,
 * aucune dépendance aux Google Play Services.
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean =
        hasFine() || ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasFine(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean =
        locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false

    @SuppressLint("MissingPermission")
    suspend fun currentPosition(): LocationResult {
        val lm = locationManager ?: return LocationResult.Unavailable
        if (!hasPermission()) return LocationResult.PermissionMissing
        if (!isLocationEnabled()) return LocationResult.LocationDisabled

        return try {
            val providers = buildList {
                if (hasFine() && lm.safeEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
                if (lm.safeEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
                if (lm.safeEnabled(LocationManager.PASSIVE_PROVIDER)) add(LocationManager.PASSIVE_PROVIDER)
            }
            if (providers.isEmpty()) return LocationResult.LocationDisabled

            val lastKnown = providers
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.elapsedRealtimeNanos }

            if (lastKnown != null && lastKnown.ageMillis() < FRESH_LOCATION_MS) {
                return LocationResult.Success(lastKnown.toPosition())
            }

            val provider = providers.first { it != LocationManager.PASSIVE_PROVIDER || providers.size == 1 }
            val fresh = withTimeoutOrNull(TIMEOUT_MS) { requestSingle(lm, provider) }
                ?: if (provider == LocationManager.GPS_PROVIDER && LocationManager.NETWORK_PROVIDER in providers) {
                    withTimeoutOrNull(TIMEOUT_MS / 2) { requestSingle(lm, LocationManager.NETWORK_PROVIDER) }
                } else null

            when {
                fresh != null -> LocationResult.Success(fresh.toPosition())
                lastKnown != null -> LocationResult.Success(lastKnown.toPosition())
                else -> LocationResult.Unavailable
            }
        } catch (e: SecurityException) {
            LocationResult.PermissionMissing
        } catch (e: Exception) {
            LocationResult.Unavailable
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingle(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { runCatching { signal.cancel() } }
            try {
                LocationManagerCompat.getCurrentLocation(
                    lm,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext),
                ) { location: Location? ->
                    if (cont.isActive) cont.resume(location)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun LocationManager.safeEnabled(provider: String): Boolean =
        runCatching { isProviderEnabled(provider) }.getOrDefault(false)

    private fun Location.ageMillis(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private fun Location.toPosition() = GeoPosition(latitude, longitude)

    private companion object {
        const val FRESH_LOCATION_MS = 2 * 60 * 1000L
        const val TIMEOUT_MS = 15_000L
    }
}
