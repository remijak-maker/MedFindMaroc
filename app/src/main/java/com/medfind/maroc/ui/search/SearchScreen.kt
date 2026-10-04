package com.medfind.maroc.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.medfind.maroc.ads.AdContainer
import com.medfind.maroc.ads.AdsConfig
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.domain.search.DoctorSearch
import com.medfind.maroc.ui.components.DataErrorView
import com.medfind.maroc.ui.components.DoctorCard
import com.medfind.maroc.ui.components.EmptyState
import com.medfind.maroc.ui.components.LoadingView
import com.medfind.maroc.ui.components.MedFilterChip
import com.medfind.maroc.ui.components.PrimaryButton
import com.medfind.maroc.ui.components.SearchBar
import com.medfind.maroc.ui.components.SecondaryButton
import com.medfind.maroc.ui.components.TabTopBar
import com.medfind.maroc.ui.directory.DataStatus
import com.medfind.maroc.ui.directory.DirectoryUiState
import com.medfind.maroc.ui.directory.LocationStatus
import com.medfind.maroc.util.ExternalActions

@Composable
fun SearchScreen(
    state: DirectoryUiState,
    onQueryChange: (String) -> Unit,
    onTypeChange: (DoctorType?) -> Unit,
    onSpecialtyChange: (String?) -> Unit,
    onCityChange: (String?) -> Unit,
    onDistanceChange: (Double?) -> Unit,
    onClearFilters: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowMap: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var showSpecialtySheet by rememberSaveable { mutableStateOf(false) }
    var showCitySheet by rememberSaveable { mutableStateOf(false) }

    // Retour des paramètres système : relancer la localisation si elle est devenue possible.
    val currentStatus by rememberUpdatedState(state.locationStatus)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val status = currentStatus
        if (status is LocationStatus.PermissionDenied || status is LocationStatus.Disabled) {
            onRequestLocationIfAllowed(context, onRequestLocation)
        }
    }

    // Revenir en haut de la liste quand les critères changent.
    LaunchedEffect(state.filters) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        TabTopBar("Recherche")
        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchBar(
                query = state.filters.query,
                onQueryChange = onQueryChange,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        FilterRow(
            state = state,
            onTypeChange = onTypeChange,
            onOpenSpecialties = { showSpecialtySheet = true },
            onOpenCities = { showCitySheet = true },
            onDistanceChange = onDistanceChange,
            onRequestLocation = onRequestLocation,
            onClearFilters = onClearFilters,
        )
        LocationBanner(
            status = state.locationStatus,
            onRetry = onRequestLocation,
            onOpenAppSettings = { ExternalActions.openAppSettings(context) },
            onOpenLocationSettings = { ExternalActions.openLocationSettings(context) },
        )

        when {
            state.dataStatus == DataStatus.LOADING && state.catalog.doctors.isEmpty() -> LoadingView()
            state.dataStatus == DataStatus.FAILED && state.catalog.doctors.isEmpty() -> DataErrorView(onRetry)
            state.catalog.doctors.isEmpty() -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Aucun médecin n'est encore référencé.",
            )
            state.results.isEmpty() -> NoResults(
                hasFilters = state.filters.hasActiveFilters,
                onEditSearch = { focusRequester.requestFocusSafely() },
                onClearFilters = onClearFilters,
            )
            else -> {
                ResultsHeader(count = state.results.size, onShowMap = onShowMap)
                val showListAd = AdsConfig.enabled && state.results.size >= AdsConfig.LIST_AD_MIN_RESULTS
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.results, key = { _, r -> r.doctor.id }) { index, result ->
                        val doctor = result.doctor
                        DoctorCard(
                            doctor = doctor,
                            distanceKm = result.distanceKm,
                            isFavorite = doctor.id in state.favoriteIds,
                            onOpenProfile = { onOpenProfile(doctor.id) },
                            onDirections = { ExternalActions.openDirections(context, doctor) },
                            onToggleFavorite = { onToggleFavorite(doctor.id) },
                        )
                        // Emplacement publicitaire n° 2 : une seule bannière, après le 3ᵉ résultat.
                        if (showListAd && index == AdsConfig.LIST_AD_AFTER_INDEX) {
                            AdContainer(Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSpecialtySheet) {
        SpecialtyPickerSheet(
            specialties = state.catalog.specialties,
            selectedId = state.filters.specialtyId,
            onSelect = {
                onSpecialtyChange(it)
                showSpecialtySheet = false
            },
            onDismiss = { showSpecialtySheet = false },
        )
    }
    if (showCitySheet) {
        CityPickerSheet(
            cities = state.catalog.cities,
            selectedId = state.filters.cityId,
            onSelect = {
                onCityChange(it)
                showCitySheet = false
            },
            onDismiss = { showCitySheet = false },
        )
    }
}

private fun onRequestLocationIfAllowed(context: android.content.Context, request: () -> Unit) {
    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (granted) request()
}

private fun FocusRequester.requestFocusSafely() {
    runCatching { requestFocus() }
}

@Composable
private fun FilterRow(
    state: DirectoryUiState,
    onTypeChange: (DoctorType?) -> Unit,
    onOpenSpecialties: () -> Unit,
    onOpenCities: () -> Unit,
    onDistanceChange: (Double?) -> Unit,
    onRequestLocation: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val filters = state.filters
    var typeMenu by remember { mutableStateOf(false) }
    var distanceMenu by remember { mutableStateOf(false) }
    val specialtyName = state.catalog.specialties.firstOrNull { it.id == filters.specialtyId }?.name
    val cityName = state.catalog.cities.firstOrNull { it.id == filters.cityId }?.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            MedFilterChip(
                label = filters.type?.label ?: "Type : Tous",
                selected = filters.type != null,
                onClick = { typeMenu = true },
            )
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                DropdownMenuItem(text = { Text("Tous") }, onClick = { onTypeChange(null); typeMenu = false })
                DoctorType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.label) }, onClick = { onTypeChange(type); typeMenu = false })
                }
            }
        }
        MedFilterChip(
            label = specialtyName ?: "Spécialité",
            selected = specialtyName != null,
            onClick = onOpenSpecialties,
        )
        MedFilterChip(
            label = cityName ?: "Ville",
            selected = cityName != null,
            onClick = onOpenCities,
        )
        Box {
            MedFilterChip(
                label = filters.maxDistanceKm?.let { "< ${formatKm(it)} km" } ?: "Distance",
                selected = filters.maxDistanceKm != null,
                onClick = {
                    if (state.position != null) distanceMenu = true else onRequestLocation()
                },
            )
            DropdownMenu(expanded = distanceMenu, onDismissRequest = { distanceMenu = false }) {
                DropdownMenuItem(text = { Text("Toutes distances") }, onClick = { onDistanceChange(null); distanceMenu = false })
                DoctorSearch.distanceOptionsKm.forEach { km ->
                    DropdownMenuItem(
                        text = { Text("Moins de ${formatKm(km)} km") },
                        onClick = { onDistanceChange(km); distanceMenu = false },
                    )
                }
            }
        }
        if (filters.hasActiveFilters) {
            TextButton(onClick = onClearFilters) { Text("Effacer") }
        }
    }
}

private fun formatKm(km: Double): String = if (km % 1.0 == 0.0) km.toInt().toString() else km.toString()

@Composable
private fun ResultsHeader(count: Int, onShowMap: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 médecin trouvé" else "$count médecins trouvés",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AssistChip(
            onClick = onShowMap,
            label = { Text("Carte") },
            leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun NoResults(hasFilters: Boolean, onEditSearch: () -> Unit, onClearFilters: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.SearchOff,
        title = "Aucun médecin correspondant à votre recherche.",
        message = "Essayez un autre nom, une autre ville ou une autre spécialité.",
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Modifier votre recherche", onClick = onEditSearch)
                if (hasFilters) SecondaryButton("Effacer tous les filtres", onClick = onClearFilters)
            }
        },
    )
}

@Composable
private fun LocationBanner(
    status: LocationStatus,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    when (status) {
        LocationStatus.Loading -> BannerSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Recherche de votre position…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        LocationStatus.PermissionDenied -> BannerSurface {
            BannerContent(
                message = "La localisation est nécessaire pour rechercher les médecins à proximité. " +
                    "Vous pouvez toujours rechercher par ville ou par spécialité.",
                actions = listOf("Autoriser" to onRetry, "Paramètres" to onOpenAppSettings),
            )
        }
        LocationStatus.Disabled -> BannerSurface {
            BannerContent(
                message = "La localisation de votre appareil est désactivée.",
                actions = listOf("Activer" to onOpenLocationSettings, "Réessayer" to onRetry),
            )
        }
        LocationStatus.Unavailable -> BannerSurface {
            BannerContent(
                message = "Votre position n'a pas pu être déterminée. Vérifiez le GPS puis réessayez.",
                actions = listOf("Réessayer" to onRetry),
            )
        }
        is LocationStatus.Available, LocationStatus.Idle -> Unit
    }
}

@Composable
private fun BannerSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
    }
}

@Composable
private fun BannerContent(message: String, actions: List<Pair<String, () -> Unit>>) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.LocationOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.align(Alignment.End)) {
            actions.forEach { (label, action) -> TextButton(onClick = action) { Text(label) } }
        }
    }
}
