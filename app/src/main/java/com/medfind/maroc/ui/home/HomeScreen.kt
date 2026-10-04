package com.medfind.maroc.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medfind.maroc.ads.AdContainer
import com.medfind.maroc.ui.components.DataErrorView
import com.medfind.maroc.ui.components.DemoDataBanner
import com.medfind.maroc.ui.components.LabeledDivider
import com.medfind.maroc.ui.components.PrimaryButton
import com.medfind.maroc.ui.components.SearchLauncher
import com.medfind.maroc.ui.components.SectionHeader
import com.medfind.maroc.ui.components.ShortcutCard
import com.medfind.maroc.ui.components.SpecialtyCard
import com.medfind.maroc.ui.directory.DataStatus
import com.medfind.maroc.ui.directory.DirectoryUiState

@Composable
fun HomeScreen(
    state: DirectoryUiState,
    onOpenSearch: () -> Unit,
    onNearby: () -> Unit,
    onBrowseSpecialties: () -> Unit,
    onBrowseCities: () -> Unit,
    onSpecialtySelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.dataStatus == DataStatus.FAILED && state.catalog.doctors.isEmpty()) {
        DataErrorView(onRetry = onRetry, modifier = Modifier.statusBarsPadding())
        return
    }

    val popular = state.catalog.specialties.filter { it.popular }.take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "MedFind Maroc",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Trouvez le médecin\nqu'il vous faut.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(24.dp))
        SearchLauncher(text = "Rechercher un médecin", onClick = onOpenSearch)

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "Médecins près de moi",
            icon = Icons.Filled.MyLocation,
            onClick = onNearby,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.catalog.containsDemoData) {
            Spacer(Modifier.height(16.dp))
            DemoDataBanner()
        }

        Spacer(Modifier.height(28.dp))
        LabeledDivider("Rechercher par")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutCard(
                title = "Spécialité",
                icon = Icons.Filled.MedicalServices,
                onClick = onBrowseSpecialties,
                modifier = Modifier.weight(1f),
            )
            ShortcutCard(
                title = "Ville",
                icon = Icons.Filled.LocationCity,
                onClick = onBrowseCities,
                modifier = Modifier.weight(1f),
            )
        }

        if (popular.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Spécialités populaires")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                popular.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { specialty ->
                            SpecialtyCard(
                                specialty = specialty,
                                onClick = { onSpecialtySelected(specialty.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Emplacement publicitaire n° 1 : en bas de l'accueil, loin des actions principales.
        Spacer(Modifier.height(24.dp))
        AdContainer()
        Spacer(Modifier.height(16.dp))
    }
}
