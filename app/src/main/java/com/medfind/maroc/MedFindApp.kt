package com.medfind.maroc

import android.app.Application
import android.content.Context
import com.medfind.maroc.ads.AdsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.osmdroid.config.Configuration
import java.io.File

class MedFindApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        configureOsmdroid()
        AdsConfig.initialize(this, appScope)
    }

    private fun configureOsmdroid() {
        runCatching {
            val config = Configuration.getInstance()
            config.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // Politique OSM : un User-Agent identifiant l'application est obligatoire.
            config.userAgentValue = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            // Cache privé de l'application : aucune permission de stockage nécessaire.
            config.osmdroidBasePath = File(filesDir, "osmdroid")
            config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
