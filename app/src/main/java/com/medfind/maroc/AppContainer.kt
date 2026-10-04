package com.medfind.maroc

import android.content.Context
import com.medfind.maroc.data.local.MedFindDatabase
import com.medfind.maroc.data.repository.DirectoryRepository
import com.medfind.maroc.data.repository.FavoritesRepository
import com.medfind.maroc.data.repository.LocalDirectoryRepository
import com.medfind.maroc.data.repository.LocalFavoritesRepository
import com.medfind.maroc.data.repository.SettingsRepository
import com.medfind.maroc.data.seed.AssetDataSource
import com.medfind.maroc.location.LocationProvider

/** Injection de dépendances manuelle : simple, explicite, sans bibliothèque. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { MedFindDatabase.create(appContext) }
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val directoryRepository: DirectoryRepository by lazy {
        LocalDirectoryRepository(
            dao = database.directoryDao(),
            favoriteDao = database.favoriteDao(),
            assets = AssetDataSource(appContext),
            prefs = prefs,
        )
    }

    val favoritesRepository: FavoritesRepository by lazy {
        LocalFavoritesRepository(database.favoriteDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(prefs) }

    val locationProvider: LocationProvider by lazy { LocationProvider(appContext) }

    companion object {
        const val PREFS_NAME = "medfind_settings"
    }
}
