package com.medfind.maroc.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.search.GeoUtils
import com.medfind.maroc.ui.components.DoctorCard
import com.medfind.maroc.ui.components.EmptyState
import com.medfind.maroc.ui.components.LoadingView
import com.medfind.maroc.ui.components.PrimaryButton
import com.medfind.maroc.ui.components.TabTopBar
import com.medfind.maroc.ui.directory.DataStatus
import com.medfind.maroc.ui.directory.DirectoryUiState
import com.medfind.maroc.util.ExternalActions

@Composable
fun FavoritesScreen(
    state: DirectoryUiState,
    onOpenProfile: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val context = LocalContext.current
    // Les favoris dont le médecin n'existe plus sont simplement ignorés (et nettoyés à l'import).
    val favorites = remember(state.catalog, state.favoriteIds) {
        state.catalog.doctors.filter { it.id in state.favoriteIds }.sortedBy { it.lastName }
    }
    val position = state.position

    Column(Modifier.fillMaxSize()) {
        TabTopBar("Mes favoris")
        when {
            state.dataStatus == DataStatus.LOADING && state.catalog.doctors.isEmpty() -> LoadingView()
            favorites.isEmpty() -> EmptyState(
                icon = Icons.Filled.FavoriteBorder,
                title = "Aucun favori pour le moment.",
                message = "Touchez le cœur sur la fiche d'un médecin pour le retrouver ici, même hors connexion.",
                action = { PrimaryButton("Rechercher un médecin", onClick = onOpenSearch) },
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(favorites, key = { it.id }) { doctor ->
                    val distance = if (position != null && doctor.latitude != null && doctor.longitude != null) {
                        GeoUtils.distanceKm(position.latitude, position.longitude, doctor.latitude, doctor.longitude)
                    } else null
                    DoctorCard(
                        doctor = doctor,
                        distanceKm = distance,
                        isFavorite = true,
                        onOpenProfile = { onOpenProfile(doctor.id) },
                        onDirections = { ExternalActions.openDirections(context, doctor) },
                        onToggleFavorite = { onToggleFavorite(doctor.id) },
                    )
                }
            }
        }
    }
}
