package com.medfind.maroc.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.model.City
import com.medfind.maroc.domain.model.Specialty
import com.medfind.maroc.domain.search.TextNormalizer
import com.medfind.maroc.ui.components.SearchBar

private data class PickerOption(val id: String?, val label: String, val group: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    options: List<PickerOption>,
    selectedId: String?,
    searchable: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filter by rememberSaveable { mutableStateOf("") }
    val normalized = TextNormalizer.normalize(filter)
    val visible = remember(options, normalized) {
        if (normalized.isEmpty()) options
        else options.filter { it.id == null || TextNormalizer.normalize(it.label).contains(normalized) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (searchable) {
                SearchBar(
                    query = filter,
                    onQueryChange = { filter = it },
                    placeholder = "Filtrer…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                var lastGroup: String? = null
                visible.forEach { option ->
                    if (option.group != null && option.group != lastGroup) {
                        lastGroup = option.group
                        item(key = "group-${option.group}") {
                            Text(
                                option.group,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                    }
                    item(key = "option-${option.id ?: "all"}") {
                        OptionRow(option.label, option.id == selectedId) { onSelect(option.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Check, contentDescription = "Sélectionné", tint = MaterialTheme.colorScheme.primary)
            }
        }
        HorizontalDivider(Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SpecialtyPickerSheet(
    specialties: List<Specialty>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(specialties) {
        listOf(PickerOption(null, "Toutes les spécialités")) +
            specialties.sortedBy { it.order }.map { PickerOption(it.id, it.name) }
    }
    PickerSheet("Spécialité", options, selectedId, searchable = true, onSelect = onSelect, onDismiss = onDismiss)
}

@Composable
fun CityPickerSheet(
    cities: List<City>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(cities) {
        listOf(PickerOption(null, "Toutes les villes")) +
            cities.sortedWith(compareBy<City>({ it.regionName }, { it.name }))
                .map { PickerOption(it.id, it.name, group = it.regionName) }
    }
    PickerSheet("Ville", options, selectedId, searchable = true, onSelect = onSelect, onDismiss = onDismiss)
}
