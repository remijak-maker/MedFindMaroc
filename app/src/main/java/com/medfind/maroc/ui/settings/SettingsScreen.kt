package com.medfind.maroc.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfind.maroc.MedFindApp
import com.medfind.maroc.data.repository.ThemeMode
import com.medfind.maroc.ui.components.BackTopBar
import com.medfind.maroc.ui.components.ListRow
import com.medfind.maroc.ui.components.SectionCard
import com.medfind.maroc.ui.directory.LocationStatus
import com.medfind.maroc.util.ExternalActions

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onRequestLocation: () -> Unit,
    locationStatus: LocationStatus,
) {
    val context = LocalContext.current
    val settings = (context.applicationContext as MedFindApp).container.settingsRepository
    val locationProvider = (context.applicationContext as MedFindApp).container.locationProvider
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }

    val permissionGranted = locationProvider.hasPermission()
    val locationSubtitle = when {
        !permissionGranted -> "Non autorisée — touchez pour autoriser"
        locationStatus is LocationStatus.Disabled -> "Autorisée, mais la localisation de l'appareil est désactivée"
        else -> "Autorisée — utilisée uniquement pour « Médecins près de moi »"
    }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Paramètres", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionCard {
                Column {
                    ListRow(
                        title = "Autorisation de localisation",
                        subtitle = locationSubtitle,
                        icon = Icons.Filled.MyLocation,
                        onClick = {
                            if (permissionGranted) ExternalActions.openAppSettings(context)
                            else onRequestLocation()
                        },
                    )
                    Divider()
                    ListRow(
                        title = "Apparence",
                        subtitle = themeMode.label,
                        icon = Icons.Filled.DarkMode,
                        onClick = { showThemeDialog = true },
                    )
                    Divider()
                    ListRow(
                        title = "Confidentialité",
                        subtitle = "Gérer les autorisations de l'application",
                        icon = Icons.Filled.Lock,
                        onClick = { ExternalActions.openAppSettings(context) },
                    )
                }
            }
            Spacer(Modifier.padding(top = 16.dp))
            SectionCard {
                Column {
                    ListRow(title = "À propos", icon = Icons.Filled.Info, onClick = onOpenAbout)
                    Divider()
                    ListRow(title = "Politique de confidentialité", icon = Icons.Filled.Description, onClick = onOpenPrivacy)
                    Divider()
                    ListRow(
                        title = "Évaluer l'application",
                        subtitle = "Donnez votre avis sur Google Play",
                        icon = Icons.Filled.Star,
                        onClick = { ExternalActions.openStoreListing(context) },
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Apparence") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = mode == themeMode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        settings.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = mode == themeMode, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Fermer") }
            },
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
