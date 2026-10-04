package com.medfind.maroc.ui.cities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medfind.maroc.domain.search.TextNormalizer
import com.medfind.maroc.ui.components.BackTopBar
import com.medfind.maroc.ui.components.CityCard
import com.medfind.maroc.ui.components.EmptyState
import com.medfind.maroc.ui.components.SearchBar
import com.medfind.maroc.ui.directory.DirectoryUiState
import com.medfind.maroc.ui.specialties.GroupTitle

@Composable
fun CitiesScreen(
    state: DirectoryUiState,
    onBack: () -> Unit,
    onCitySelected: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val counts = remember(state.catalog) {
        state.catalog.doctors.groupingBy { it.city?.id }.eachCount()
    }
    val normalized = TextNormalizer.normalize(filter)
    val grouped = remember(state.catalog, normalized) {
        state.catalog.cities
            .filter {
                normalized.isEmpty() ||
                    TextNormalizer.normalize(it.name).contains(normalized) ||
                    TextNormalizer.normalize(it.regionName).contains(normalized)
            }
            .groupBy { it.regionName }
            .toSortedMap()
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        BackTopBar("Villes", onBack)
        SearchBar(
            query = filter,
            onQueryChange = { filter = it },
            placeholder = "Rechercher une ville ou une région",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (grouped.isEmpty()) {
            EmptyState(icon = Icons.Filled.LocationCity, title = "Aucune ville trouvée.")
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            grouped.forEach { (region, cities) ->
                item(key = "region-$region") { GroupTitle("Région $region") }
                items(cities.sortedBy { it.name }, key = { it.id }) { city ->
                    CityCard(
                        city = city,
                        doctorCount = counts[city.id] ?: 0,
                        onClick = { onCitySelected(city.id) },
                    )
                }
            }
        }
    }
}
