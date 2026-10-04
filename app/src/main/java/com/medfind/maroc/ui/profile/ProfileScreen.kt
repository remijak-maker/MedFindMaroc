package com.medfind.maroc.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.search.GeoUtils
import com.medfind.maroc.ui.components.BackTopBar
import com.medfind.maroc.ui.components.DemoBadge
import com.medfind.maroc.ui.components.EmptyState
import com.medfind.maroc.ui.components.IconBadge
import com.medfind.maroc.ui.components.LoadingView
import com.medfind.maroc.ui.components.PrimaryButton
import com.medfind.maroc.ui.components.SecondaryButton
import com.medfind.maroc.ui.components.SectionCard
import com.medfind.maroc.ui.components.VerifiedBadge
import com.medfind.maroc.ui.components.specialtyIcon
import com.medfind.maroc.ui.directory.DataStatus
import com.medfind.maroc.ui.directory.DirectoryUiState
import com.medfind.maroc.util.ExternalActions

@Composable
fun ProfileScreen(
    state: DirectoryUiState,
    doctorId: String,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowOnMap: (String) -> Unit,
) {
    val doctor = state.catalog.doctors.firstOrNull { it.id == doctorId }
    val isFavorite = doctorId in state.favoriteIds

    Column(Modifier.fillMaxSize()) {
        BackTopBar(
            title = "Profil du médecin",
            onBack = onBack,
            actions = {
                if (doctor != null) {
                    IconButton(onClick = { onToggleFavorite(doctor.id) }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        when {
            doctor != null -> ProfileContent(
                doctor = doctor,
                isFavorite = isFavorite,
                distanceKm = state.position?.let { p ->
                    val lat = doctor.latitude
                    val lon = doctor.longitude
                    if (lat != null && lon != null) GeoUtils.distanceKm(p.latitude, p.longitude, lat, lon) else null
                },
                onToggleFavorite = { onToggleFavorite(doctor.id) },
                onShowOnMap = { onShowOnMap(doctor.id) },
            )
            state.dataStatus == DataStatus.LOADING -> LoadingView()
            else -> EmptyState(
                icon = Icons.Filled.Person,
                title = "Ce médecin n'est plus disponible.",
                message = "Sa fiche a peut-être été retirée de l'annuaire.",
                action = { PrimaryButton("Retour", onClick = onBack) },
            )
        }
    }
}

@Composable
private fun ProfileContent(
    doctor: Doctor,
    isFavorite: Boolean,
    distanceKm: Double?,
    onToggleFavorite: () -> Unit,
    onShowOnMap: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // En-tête
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(specialtyIcon(doctor.specialty?.icon), size = 64)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(doctor.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    doctor.specialtyLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    doctor.type.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            doctor.isDemo -> DemoBadge()
            doctor.verified -> VerifiedBadge()
        }

        // Actions principales (aucune publicité sur cet écran)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = if (doctor.phone != null) "Appeler" else "Téléphone non disponible",
                icon = Icons.Filled.Call,
                onClick = { ExternalActions.dial(context, doctor.phone) },
                enabled = ExternalActions.sanitizePhone(doctor.phone) != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (ExternalActions.toInternationalDigits(doctor.whatsapp) != null) {
                SecondaryButton(
                    text = "WhatsApp",
                    icon = Icons.Filled.Forum,
                    onClick = { ExternalActions.openWhatsApp(context, doctor.whatsapp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Itinéraire",
                    icon = Icons.Filled.Navigation,
                    onClick = { ExternalActions.openDirections(context, doctor) },
                    enabled = doctor.hasCoordinates || doctor.address != null || doctor.city != null,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Sur la carte",
                    icon = Icons.Filled.Map,
                    onClick = onShowOnMap,
                    enabled = doctor.hasCoordinates,
                    modifier = Modifier.weight(1f),
                )
            }
            SecondaryButton(
                text = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                onClick = onToggleFavorite,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Informations
        Spacer(Modifier.height(20.dp))
        SectionCard {
            Column(Modifier.padding(vertical = 4.dp)) {
                val addressLines = listOfNotNull(
                    doctor.address,
                    listOfNotNull(doctor.quarter, doctor.city?.name).joinToString(", ").ifBlank { null },
                    doctor.city?.regionName?.let { "Région $it" },
                )
                InfoBlock(
                    icon = Icons.Filled.LocationOn,
                    title = "Adresse",
                    value = addressLines.joinToString("\n").ifBlank { "Adresse non renseignée" },
                )
                if (distanceKm != null) {
                    InfoDivider()
                    InfoBlock(Icons.Filled.Straighten, "Distance", "À ${GeoUtils.formatDistance(distanceKm)} de vous (à vol d'oiseau)")
                }
                InfoDivider()
                InfoBlock(
                    icon = Icons.Filled.Phone,
                    title = "Téléphone",
                    value = listOfNotNull(doctor.phone, doctor.secondaryPhone).joinToString("\n")
                        .ifBlank { "Non renseigné" },
                    action = if (doctor.secondaryPhone != null) {
                        { TextButton(onClick = { ExternalActions.dial(context, doctor.secondaryPhone) }) { Text("Appeler le 2ᵉ numéro") } }
                    } else null,
                )
                InfoDivider()
                InfoBlock(Icons.Filled.Schedule, "Horaires", doctor.openingHours ?: "Horaires non renseignés")
                if (doctor.clinic != null) {
                    InfoDivider()
                    InfoBlock(Icons.Filled.LocalHospital, "Établissement", doctor.clinic)
                }
                if (doctor.website != null) {
                    InfoDivider()
                    InfoBlock(
                        icon = Icons.Filled.Public,
                        title = "Site web",
                        value = doctor.website,
                        action = { TextButton(onClick = { ExternalActions.openWebsite(context, doctor.website) }) { Text("Ouvrir") } },
                    )
                }
                if (doctor.description != null) {
                    InfoDivider()
                    InfoBlock(Icons.Filled.Description, "À propos", doctor.description)
                }
            }
        }

        // Provenance des données (transparence)
        Spacer(Modifier.height(16.dp))
        SectionCard {
            Column(Modifier.padding(vertical = 4.dp)) {
                InfoBlock(
                    icon = Icons.Filled.Info,
                    title = "Source des informations",
                    value = buildString {
                        append(
                            when {
                                doctor.isDemo -> "Données de démonstration (profil fictif)"
                                doctor.source != null -> doctor.source
                                else -> "Non précisée"
                            }
                        )
                        append('\n')
                        append(
                            if (doctor.verificationDate != null) "Dernière vérification : ${doctor.verificationDate}"
                            else "Date de vérification non renseignée"
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "MedFind Maroc est un annuaire : il ne fournit ni diagnostic ni conseil médical. " +
                "En cas d'urgence, contactez immédiatement les secours (15, ou 112 depuis un mobile).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoBlock(
    icon: ImageVector,
    title: String,
    value: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
            if (action != null) action()
        }
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
