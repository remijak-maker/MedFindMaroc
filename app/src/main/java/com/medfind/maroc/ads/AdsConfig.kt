package com.medfind.maroc.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.medfind.maroc.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Configuration publicitaire centralisée.
 *
 * - Activer/désactiver : propriété Gradle `medfind.ads.enabled` (gradle.properties).
 * - Identifiants réels : `medfind.admob.appId` / `medfind.admob.bannerId` dans
 *   ~/.gradle/gradle.properties (jamais dans le dépôt). En Debug, les
 *   identifiants de TEST Google sont toujours utilisés.
 */
object AdsConfig {

    val enabled: Boolean get() = BuildConfig.ADS_ENABLED

    val bannerUnitId: String get() = BuildConfig.ADMOB_BANNER_ID

    /** Position de la publicité dans la liste de résultats (après le 3ᵉ médecin). */
    const val LIST_AD_AFTER_INDEX = 2

    /** Nombre minimal de résultats pour afficher une publicité dans la liste. */
    const val LIST_AD_MIN_RESULTS = 4

    fun initialize(context: Context, scope: CoroutineScope) {
        if (!enabled) return
        scope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(context.applicationContext) {}
            } catch (e: Exception) {
                Log.w("AdsConfig", "Initialisation AdMob impossible", e)
            }
        }
    }
}
