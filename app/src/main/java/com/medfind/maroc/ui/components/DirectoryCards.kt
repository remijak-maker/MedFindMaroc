package com.medfind.maroc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.model.City
import com.medfind.maroc.domain.model.Doctor
import com.medfind.maroc.domain.model.Specialty
import com.medfind.maroc.domain.search.GeoUtils

/** Associe la clé « icone » du fichier specialties.json à une icône Material. */
fun specialtyIcon(key: String?): ImageVector = when (key) {
    "stethoscope" -> Icons.Filled.MedicalServices
    "heart" -> Icons.Filled.Favorite
    "skin" -> Icons.Filled.Face
    "hormone" -> Icons.Filled.Science
    "stomach" -> Icons.Filled.Restaurant
    "woman" -> Icons.Filled.PregnantWoman
    "blood" -> Icons.Filled.Opacity
    "brain" -> Icons.Filled.Psychology
    "kidney" -> Icons.Filled.Spa
    "oncology" -> Icons.Filled.VolunteerActivism
    "eye" -> Icons.Filled.Visibility
    "ear" -> Icons.Filled.Hearing
    "bone" -> Icons.Filled.Accessibility
    "child" -> Icons.Filled.ChildCare
    "lungs" -> Icons.Filled.FilterDrama
    "surgery" -> Icons.Filled.Healing
    "rehab" -> Icons.Filled.Accessible
    "scan" -> Icons.Filled.CenterFocusStrong
    "anesthesia" -> Icons.Filled.Hotel
    "lab" -> Icons.Filled.Biotech
    else -> Icons.Filled.LocalHospital
}

@Composable
fun DoctorCard(
    doctor: Doctor,
    distanceKm: Double?,
    isFavorite: Boolean,
    onOpenProfile: () -> Unit,
    onDirections: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpenProfile,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(specialtyIcon(doctor.specialty?.icon), size = 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        doctor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        doctor.specialtyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoLine(Icons.Filled.LocationOn, doctor.locationLabel)
                if (distanceKm != null) InfoLine(Icons.Filled.Straighten, GeoUtils.formatDistance(distanceKm))
            }
            if (doctor.verified || doctor.isDemo) {
                Spacer(Modifier.height(10.dp))
                if (doctor.verified) VerifiedBadge() else DemoBadge()
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Profil",
                    onClick = onOpenProfile,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Itinéraire",
                    onClick = onDirections,
                    icon = Icons.Filled.Navigation,
                    modifier = Modifier.weight(1f),
                    enabled = doctor.hasCoordinates || doctor.address != null || doctor.city != null,
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Grande tuile (accueil : « Spécialité », « Ville »). */
@Composable
fun ShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon, size = 40)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SpecialtyCard(
    specialty: Specialty,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(specialtyIcon(specialty.icon), size = 36)
            Spacer(Modifier.width(10.dp))
            Text(
                specialty.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CityCard(
    city: City,
    doctorCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = city.name,
        subtitle = when (doctorCount) {
            0 -> "Aucun médecin référencé"
            1 -> "1 médecin"
            else -> "$doctorCount médecins"
        },
        icon = Icons.Filled.LocationCity,
        onClick = onClick,
        modifier = modifier,
    )
}

/** Puce de filtre avec flèche (ouvre une liste de choix). */
@Composable
fun MedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
