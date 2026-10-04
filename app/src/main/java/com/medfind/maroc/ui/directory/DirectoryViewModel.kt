package com.medfind.maroc.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.medfind.maroc.MedFindApp
import com.medfind.maroc.data.repository.DirectoryRepository
import com.medfind.maroc.data.repository.FavoritesRepository
import com.medfind.maroc.domain.model.Catalog
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.domain.search.DoctorResult
import com.medfind.maroc.domain.search.DoctorSearch
import com.medfind.maroc.domain.search.GeoPosition
import com.medfind.maroc.domain.search.SearchFilters
import com.medfind.maroc.location.LocationProvider
import com.medfind.maroc.location.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LocationStatus {
    data object Idle : LocationStatus
    data object Loading : LocationStatus
    data class Available(val position: GeoPosition) : LocationStatus
    data object PermissionDenied : LocationStatus
    data object Disabled : LocationStatus
    data object Unavailable : LocationStatus
}

enum class DataStatus { LOADING, READY, FAILED }

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

data class DirectoryUiState(
    val dataStatus: DataStatus = DataStatus.LOADING,
    val catalog: Catalog = Catalog.EMPTY,
    val filters: SearchFilters = SearchFilters(),
    val results: List<DoctorResult> = emptyList(),
    val locationStatus: LocationStatus = LocationStatus.Idle,
    val favoriteIds: Set<String> = emptySet(),
) {
    val position: GeoPosition? get() = (locationStatus as? LocationStatus.Available)?.position
}

/**
 * ViewModel partagé par tous les écrans (portée Activity) : il conserve le
 * catalogue, les filtres, la position et les favoris, y compris lors d'une
 * rotation ou d'un retour depuis l'arrière-plan.
 */
class DirectoryViewModel(
    private val repository: DirectoryRepository,
    private val favorites: FavoritesRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val dataStatus = MutableStateFlow(DataStatus.LOADING)
    private val filters = MutableStateFlow(SearchFilters())
    private val locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Idle)
    private var locationJob: Job? = null

    /** Médecin à centrer sur la carte (depuis le profil). */
    private val _mapFocusDoctorId = MutableStateFlow<String?>(null)
    val mapFocusDoctorId: StateFlow<String?> = _mapFocusDoctorId.asStateFlow()

    /** Dernière position de la caméra de la carte (conservée entre les onglets). */
    var mapCamera: MapCamera? = null

    private val favoriteIds = favorites.favoriteIds.map { it.toSet() }

    private val resultsInput = combine(
        repository.catalog,
        filters,
        locationStatus,
    ) { catalog, f, location ->
        val position = (location as? LocationStatus.Available)?.position
        Triple(catalog, f, DoctorSearch.filter(catalog.doctors, f, position))
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DirectoryUiState> = combine(
        resultsInput,
        locationStatus,
        favoriteIds,
        dataStatus,
    ) { (catalog, f, results), location, favIds, status ->
        DirectoryUiState(
            dataStatus = status,
            catalog = catalog,
            filters = f,
            results = results,
            locationStatus = location,
            favoriteIds = favIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryUiState())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            dataStatus.value = DataStatus.LOADING
            dataStatus.value = when (repository.initialize()) {
                DirectoryRepository.InitResult.Ready,
                DirectoryRepository.InitResult.ReadyWithStaleData -> DataStatus.READY
                DirectoryRepository.InitResult.Failed -> DataStatus.FAILED
            }
        }
    }

    // ---- Filtres -----------------------------------------------------------

    fun setQuery(query: String) = filters.update { it.copy(query = query.take(100)) }

    fun setType(type: DoctorType?) = filters.update { it.copy(type = type) }

    fun setSpecialty(specialtyId: String?) = filters.update { it.copy(specialtyId = specialtyId) }

    fun setCity(cityId: String?) = filters.update { it.copy(cityId = cityId) }

    fun setMaxDistance(km: Double?) = filters.update { it.copy(maxDistanceKm = km) }

    fun clearFilters() = filters.update { SearchFilters() }

    /** Démarre une nouvelle recherche ciblée (depuis l'accueil, une spécialité, une ville…). */
    fun startSearch(
        query: String = "",
        specialtyId: String? = null,
        cityId: String? = null,
        type: DoctorType? = null,
    ) {
        filters.value = SearchFilters(query = query, specialtyId = specialtyId, cityId = cityId, type = type)
    }

    // ---- Localisation ------------------------------------------------------

    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    fun onLocationPermissionDenied() {
        locationStatus.value = LocationStatus.PermissionDenied
        filters.update { it.copy(maxDistanceKm = null) }
    }

    /** « Médecins près de moi » : recherche réinitialisée (les résultats seront triés par distance). */
    fun prepareNearbySearch() {
        filters.value = SearchFilters()
    }

    fun refreshLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val previous = locationStatus.value
            if (previous !is LocationStatus.Available) locationStatus.value = LocationStatus.Loading
            locationStatus.value = when (val result = locationProvider.currentPosition()) {
                is LocationResult.Success -> LocationStatus.Available(result.position)
                LocationResult.PermissionMissing -> LocationStatus.PermissionDenied
                LocationResult.LocationDisabled -> LocationStatus.Disabled
                LocationResult.Unavailable ->
                    if (previous is LocationStatus.Available) previous else LocationStatus.Unavailable
            }
            if (locationStatus.value !is LocationStatus.Available) {
                filters.update { it.copy(maxDistanceKm = null) }
            }
        }
    }

    // ---- Favoris -----------------------------------------------------------

    fun toggleFavorite(doctorId: String) {
        viewModelScope.launch {
            if (doctorId in uiState.value.favoriteIds) favorites.remove(doctorId)
            else favorites.add(doctorId)
        }
    }

    // ---- Carte -------------------------------------------------------------

    fun focusOnMap(doctorId: String) {
        _mapFocusDoctorId.value = doctorId
    }

    fun consumeMapFocus() {
        _mapFocusDoctorId.value = null
    }

    fun findDoctor(id: String): Doctor? = uiState.value.catalog.doctors.firstOrNull { it.id == id }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MedFindApp
                DirectoryViewModel(
                    repository = app.container.directoryRepository,
                    favorites = app.container.favoritesRepository,
                    locationProvider = app.container.locationProvider,
                )
            }
        }
    }
}
