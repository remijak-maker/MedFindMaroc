package com.medfind.maroc.ui.directory

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Renvoie une action qui obtient la position de l'utilisateur.
 * La permission n'est demandée qu'au moment où l'utilisateur en a besoin,
 * jamais au démarrage de l'application.
 */
@Composable
fun rememberLocationRequester(viewModel: DirectoryViewModel): () -> Unit {
    val currentVm by rememberUpdatedState(viewModel)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) currentVm.refreshLocation()
        else currentVm.onLocationPermissionDenied()
    }
    return remember(launcher) {
        {
            if (currentVm.hasLocationPermission()) {
                currentVm.refreshLocation()
            } else {
                try {
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                } catch (e: Exception) {
                    currentVm.onLocationPermissionDenied()
                }
            }
        }
    }
}
