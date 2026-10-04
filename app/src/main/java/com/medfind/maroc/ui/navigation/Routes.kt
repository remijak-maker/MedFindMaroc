package com.medfind.maroc.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val MAP = "map"
    const val FAVORITES = "favorites"
    const val SPECIALTIES = "specialties"
    const val CITIES = "cities"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val PROFILE_ARG = "doctorId"
    const val PROFILE = "profile/{$PROFILE_ARG}"

    fun profile(doctorId: String) = "profile/" + Uri.encode(doctorId)
}

enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Accueil", Icons.Filled.Home),
    SEARCH(Routes.SEARCH, "Recherche", Icons.Filled.Search),
    MAP(Routes.MAP, "Carte", Icons.Filled.Map),
    FAVORITES(Routes.FAVORITES, "Favoris", Icons.Filled.Favorite),
}
