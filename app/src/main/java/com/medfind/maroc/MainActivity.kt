package com.medfind.maroc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfind.maroc.ui.navigation.MedFindAppRoot
import com.medfind.maroc.ui.theme.MedFindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Affichage bord à bord : les marges système sont gérées via les WindowInsets.
        enableEdgeToEdge()
        val settings = (application as MedFindApp).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            MedFindTheme(themeMode = themeMode) {
                MedFindAppRoot()
            }
        }
    }
}
