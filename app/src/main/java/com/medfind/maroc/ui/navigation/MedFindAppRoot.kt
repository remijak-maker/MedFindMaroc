package com.medfind.maroc.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medfind.maroc.ui.cities.CitiesScreen
import com.medfind.maroc.ui.directory.DirectoryViewModel
import com.medfind.maroc.ui.directory.rememberLocationRequester
import com.medfind.maroc.ui.favorites.FavoritesScreen
import com.medfind.maroc.ui.home.HomeScreen
import com.medfind.maroc.ui.map.MapScreen
import com.medfind.maroc.ui.profile.ProfileScreen
import com.medfind.maroc.ui.search.SearchScreen
import com.medfind.maroc.ui.settings.AboutScreen
import com.medfind.maroc.ui.settings.PrivacyPolicyScreen
import com.medfind.maroc.ui.settings.SettingsScreen
import com.medfind.maroc.ui.specialties.SpecialtiesScreen

@Composable
fun MedFindAppRoot() {
    val navController = rememberNavController()
    // ViewModel partagé, lié à l'Activity : survit aux changements d'écran et à la rotation.
    val viewModel: DirectoryViewModel = viewModel(factory = DirectoryViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestLocation = rememberLocationRequester(viewModel)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onOpenSearch = {
                        viewModel.startSearch()
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                    onNearby = {
                        viewModel.prepareNearbySearch()
                        requestLocation()
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                    onBrowseSpecialties = { navController.navigate(Routes.SPECIALTIES) },
                    onBrowseCities = { navController.navigate(Routes.CITIES) },
                    onSpecialtySelected = { id ->
                        viewModel.startSearch(specialtyId = id)
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onRetry = viewModel::loadData,
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    state = state,
                    onQueryChange = viewModel::setQuery,
                    onTypeChange = viewModel::setType,
                    onSpecialtyChange = viewModel::setSpecialty,
                    onCityChange = viewModel::setCity,
                    onDistanceChange = viewModel::setMaxDistance,
                    onClearFilters = viewModel::clearFilters,
                    onRequestLocation = requestLocation,
                    onOpenProfile = { navController.navigate(Routes.profile(it)) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onShowMap = { navController.navigateTopLevel(Routes.MAP) },
                    onRetry = viewModel::loadData,
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenProfile = { navController.navigate(Routes.profile(it)) },
                    onShowList = { navController.navigateTopLevel(Routes.SEARCH) },
                    onRequestLocation = requestLocation,
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    state = state,
                    onOpenProfile = { navController.navigate(Routes.profile(it)) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenSearch = {
                        viewModel.startSearch()
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                )
            }
            composable(Routes.SPECIALTIES) {
                SpecialtiesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSpecialtySelected = { id ->
                        viewModel.startSearch(specialtyId = id)
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                )
            }
            composable(Routes.CITIES) {
                CitiesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onCitySelected = { id ->
                        viewModel.startSearch(cityId = id)
                        navController.navigateTopLevel(Routes.SEARCH, restore = false)
                    },
                )
            }
            composable(
                route = Routes.PROFILE,
                arguments = listOf(navArgument(Routes.PROFILE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val doctorId = entry.arguments?.getString(Routes.PROFILE_ARG).orEmpty()
                ProfileScreen(
                    state = state,
                    doctorId = doctorId,
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onShowOnMap = { id ->
                        viewModel.focusOnMap(id)
                        navController.navigateTopLevel(Routes.MAP, restore = false)
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onRequestLocation = requestLocation,
                    locationStatus = state.locationStatus,
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRIVACY) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Navigation entre onglets : une seule copie de chaque onglet, état conservé. */
fun NavHostController.navigateTopLevel(route: String, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = restore }
        launchSingleTop = true
        restoreState = restore
    }
}
