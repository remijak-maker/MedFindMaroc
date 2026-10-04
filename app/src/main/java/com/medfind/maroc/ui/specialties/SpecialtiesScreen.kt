package com.medfind.maroc.ui.specialties

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.model.DoctorType
import com.medfind.maroc.ui.components.BackTopBar
import com.medfind.maroc.ui.components.EmptyState
import com.medfind.maroc.ui.components.ListRow
import com.medfind.maroc.ui.components.specialtyIcon
import com.medfind.maroc.ui.directory.DirectoryUiState

@Composable
fun SpecialtiesScreen(
    state: DirectoryUiState,
    onBack: () -> Unit,
    onSpecialtySelected: (String) -> Unit,
) {
    val counts = remember(state.catalog) {
        state.catalog.doctors.groupingBy { it.specialty?.id }.eachCount()
    }
    val generalists = state.catalog.specialties.filter { it.type == DoctorType.GENERALIST }
    val specialists = state.catalog.specialties.filter { it.type == DoctorType.SPECIALIST }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Spécialités", onBack)
        if (state.catalog.specialties.isEmpty()) {
            EmptyState(icon = Icons.Filled.MedicalServices, title = "Aucune spécialité disponible.")
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            if (generalists.isNotEmpty()) {
                item { GroupTitle("Médecine générale") }
                items(generalists, key = { it.id }) { s ->
                    ListRow(
                        title = s.name,
                        subtitle = countLabel(counts[s.id] ?: 0),
                        icon = specialtyIcon(s.icon),
                        onClick = { onSpecialtySelected(s.id) },
                    )
                }
            }
            if (specialists.isNotEmpty()) {
                item { GroupTitle("Spécialités médicales") }
                items(specialists, key = { it.id }) { s ->
                    ListRow(
                        title = s.name,
                        subtitle = countLabel(counts[s.id] ?: 0),
                        icon = specialtyIcon(s.icon),
                        onClick = { onSpecialtySelected(s.id) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

internal fun countLabel(count: Int): String = when (count) {
    0 -> "Aucun médecin référencé"
    1 -> "1 médecin"
    else -> "$count médecins"
}
