package com.medfind.maroc.ui.map

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfind.maroc.R
import com.medfind.maroc.config.MapConfig
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.search.GeoPosition
import com.medfind.maroc.domain.search.GeoUtils
import com.medfind.maroc.ui.components.DemoBadge
import com.medfind.maroc.ui.components.PrimaryButton
import com.medfind.maroc.ui.directory.DirectoryUiState
import com.medfind.maroc.ui.directory.DirectoryViewModel
import com.medfind.maroc.ui.directory.LocationStatus
import com.medfind.maroc.ui.directory.MapCamera
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: DirectoryUiState,
    viewModel: DirectoryViewModel,
    onOpenProfile: (String) -> Unit,
    onShowList: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusId by viewModel.mapFocusDoctorId.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var centerOnUserPending by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val online = remember { isOnline(context) }

    val mapView = remember {
        createMapView(context, viewModel.mapCamera, state)
    }

    // Cycle de vie de la carte (osmdroid exige onResume/onPause/onDetach).
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                val center = mapView.mapCenter
                viewModel.mapCamera = MapCamera(center.latitude, center.longitude, mapView.zoomLevelDouble)
                mapView.onPause()
                mapView.onDetach()
            }
        }
    }

    // Médecins affichables (avec coordonnées), limités pour garder une carte fluide.
    val withCoordinates = remember(state.results) { state.results.filter { it.doctor.hasCoordinates } }
    val displayed = remember(withCoordinates, state.catalog, focusId, selectedId) {
        val base = withCoordinates.take(MapConfig.MAX_MARKERS).map { it.doctor }
        val extraIds = listOfNotNull(focusId, selectedId).filter { id -> base.none { it.id == id } }
        base + state.catalog.doctors.filter { it.id in extraIds && it.hasCoordinates }
    }
    val missingCoordinates = state.results.size - withCoordinates.size
    val truncated = withCoordinates.size > MapConfig.MAX_MARKERS

    // Fond de carte légèrement assombri en mode sombre (lisibilité sans inverser les couleurs).
    LaunchedEffect(mapView, isDark) {
        runCatching {
            mapView.overlayManager.tilesOverlay.setColorFilter(if (isDark) darkTilesFilter() else null)
            mapView.invalidate()
        }
    }

    // Marqueurs
    LaunchedEffect(mapView, displayed, selectedId, state.position) {
        runCatching {
            updateMarkers(context, mapView, displayed, selectedId, state.position) { id -> selectedId = id }
        }
    }

    // Centrage demandé depuis un profil
    LaunchedEffect(focusId, state.catalog) {
        val id = focusId ?: return@LaunchedEffect
        val doctor = state.catalog.doctors.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val lat = doctor.latitude
        val lon = doctor.longitude
        if (lat != null && lon != null) {
            mapView.controller.setZoom(MapConfig.DOCTOR_ZOOM)
            mapView.controller.animateTo(GeoPoint(lat, lon))
            selectedId = id
        }
        viewModel.consumeMapFocus()
    }

    // Centrage sur l'utilisateur après obtention de la position
    LaunchedEffect(state.position, centerOnUserPending) {
        val position = state.position
        if (centerOnUserPending && position != null) {
            mapView.controller.setZoom(MapConfig.CITY_ZOOM)
            mapView.controller.animateTo(GeoPoint(position.latitude, position.longitude))
            centerOnUserPending = false
        }
        if (centerOnUserPending && state.locationStatus !is LocationStatus.Loading &&
            state.locationStatus !is LocationStatus.Idle && position == null
        ) {
            centerOnUserPending = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // En-tête : bascule Liste / Carte
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.large, shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(6.dp)) {
                    SegmentedButton(
                        selected = false,
                        onClick = onShowList,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    ) { Text("Liste") }
                    SegmentedButton(
                        selected = true,
                        onClick = {},
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    ) { Text("Carte") }
                }
            }
            val notes = buildList {
                add(if (displayed.size == 1) "1 médecin affiché" else "${displayed.size} médecins affichés")
                if (truncated) add("affinez la recherche pour tout voir")
                if (missingCoordinates > 0) add("$missingCoordinates sans coordonnées")
            }
            MapChip(notes.joinToString(" · "))
            if (!online) {
                MapChip("Hors connexion : le fond de carte peut être incomplet.", icon = true)
            }
        }

        // Bouton « ma position »
        SmallFloatingActionButton(
            onClick = {
                val position = state.position
                if (position != null) {
                    mapView.controller.setZoom(MapConfig.CITY_ZOOM)
                    mapView.controller.animateTo(GeoPoint(position.latitude, position.longitude))
                } else {
                    centerOnUserPending = true
                    onRequestLocation()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedId != null) 190.dp else 36.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            if (state.locationStatus is LocationStatus.Loading) {
                CircularProgressIndicator(Modifier.padding(10.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = "Centrer sur ma position")
            }
        }

        // Attribution OpenStreetMap (obligatoire)
        Text(
            MapConfig.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        // Fiche du médecin sélectionné
        val selected = selectedId?.let { id -> state.catalog.doctors.firstOrNull { it.id == id } }
        if (selected != null) {
            SelectedDoctorCard(
                doctor = selected,
                position = state.position,
                onOpenProfile = { onOpenProfile(selected.id) },
                onClose = { selectedId = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun MapChip(text: String, icon: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectedDoctorCard(
    doctor: Doctor,
    position: GeoPosition?,
    onOpenProfile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    Text(doctor.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(doctor.specialtyLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    val distance = position?.let { p ->
                        val lat = doctor.latitude
                        val lon = doctor.longitude
                        if (lat != null && lon != null) GeoUtils.distanceKm(p.latitude, p.longitude, lat, lon) else null
                    }
                    Text(
                        doctor.locationLabel + (distance?.let { " · " + GeoUtils.formatDistance(it) } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
            }
            if (doctor.isDemo) {
                Spacer(Modifier.height(8.dp))
                DemoBadge()
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                "Voir le profil",
                onClick = onOpenProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------

private fun createMapView(context: Context, camera: MapCamera?, state: DirectoryUiState): MapView =
    MapView(context).apply {
        setTileSource(MapConfig.tileSource)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = MapConfig.MIN_ZOOM
        maxZoomLevel = MapConfig.MAX_ZOOM
        isTilesScaledToDpi = true
        setUseDataConnection(true)

        val position = state.position
        val city = state.filters.cityId?.let { id -> state.catalog.cities.firstOrNull { it.id == id } }
        val cityLat = city?.latitude
        val cityLon = city?.longitude
        when {
            camera != null -> {
                controller.setZoom(camera.zoom)
                controller.setCenter(GeoPoint(camera.latitude, camera.longitude))
            }
            position != null -> {
                controller.setZoom(MapConfig.CITY_ZOOM)
                controller.setCenter(GeoPoint(position.latitude, position.longitude))
            }
            cityLat != null && cityLon != null -> {
                controller.setZoom(MapConfig.CITY_ZOOM)
                controller.setCenter(GeoPoint(cityLat, cityLon))
            }
            else -> {
                controller.setZoom(MapConfig.DEFAULT_ZOOM)
                controller.setCenter(GeoPoint(MapConfig.DEFAULT_LATITUDE, MapConfig.DEFAULT_LONGITUDE))
            }
        }
    }

private fun updateMarkers(
    context: Context,
    mapView: MapView,
    doctors: List<Doctor>,
    selectedId: String?,
    position: GeoPosition?,
    onSelect: (String) -> Unit,
) {
    mapView.overlays.removeAll { it is Marker }
    val normalIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_marker)
    val selectedIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_selected)

    if (position != null) {
        mapView.overlays.add(
            Marker(mapView).apply {
                this.position = GeoPoint(position.latitude, position.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_user_location)
                title = "Ma position"
                setInfoWindow(null as MarkerInfoWindow?)
                setOnMarkerClickListener { _, _ -> true }
            },
        )
    }

    // Le marqueur sélectionné est ajouté en dernier pour être dessiné au-dessus.
    val ordered = doctors.sortedBy { it.id == selectedId }
    for (doctor in ordered) {
        val lat = doctor.latitude ?: continue
        val lon = doctor.longitude ?: continue
        val isSelected = doctor.id == selectedId
        mapView.overlays.add(
            Marker(mapView).apply {
                this.position = GeoPoint(lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = if (isSelected) selectedIcon else normalIcon
                title = doctor.displayName
                snippet = doctor.specialtyLabel
                setInfoWindow(null as MarkerInfoWindow?)
                setOnMarkerClickListener { _, _ ->
                    onSelect(doctor.id)
                    true
                }
            },
        )
    }
    mapView.invalidate()
}

private fun darkTilesFilter(): ColorMatrixColorFilter {
    val m = ColorMatrix()
    m.setScale(0.72f, 0.74f, 0.78f, 1f)
    return ColorMatrixColorFilter(m)
}

private fun isOnline(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return@runCatching true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return@runCatching false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(true)
