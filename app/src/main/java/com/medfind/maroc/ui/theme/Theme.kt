package com.medfind.maroc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.medfind.maroc.data.repository.ThemeMode

// Palette « Modern Medical » : bleu médical, bleu clair, blancs et gris doux.
private val MedicalBlue = Color(0xFF1F6FE0)
private val MedicalBlueDark = Color(0xFF0F4FB0)
private val LightBlue = Color(0xFFE6F0FD)
private val Ink = Color(0xFF0F1B2D)
private val Slate = Color(0xFF5B6B80)
private val Mist = Color(0xFFF4F7FB)
private val Line = Color(0xFFE2E8F0)

private val LightColors = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    onPrimaryContainer = MedicalBlueDark,
    secondary = Color(0xFF3D7BC9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF2FC),
    onSecondaryContainer = Color(0xFF123A6B),
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Mist,
    surfaceContainerHigh = Color(0xFFEEF2F8),
    surfaceContainerHighest = Color(0xFFE8EDF4),
    outline = Color(0xFFC5CFDC),
    outlineVariant = Line,
    error = Color(0xFFC62828),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB8FF),
    onPrimary = Color(0xFF00305F),
    primaryContainer = Color(0xFF1A3B66),
    onPrimaryContainer = Color(0xFFD6E5FF),
    secondary = Color(0xFFA9C7F0),
    onSecondary = Color(0xFF0E2A4D),
    secondaryContainer = Color(0xFF203247),
    onSecondaryContainer = Color(0xFFD3E4FA),
    background = Color(0xFF0E141B),
    onBackground = Color(0xFFE4EAF2),
    surface = Color(0xFF0E141B),
    onSurface = Color(0xFFE4EAF2),
    surfaceVariant = Color(0xFF1B2430),
    onSurfaceVariant = Color(0xFFA9B6C6),
    surfaceContainerLowest = Color(0xFF0A0F15),
    surfaceContainerLow = Color(0xFF131B24),
    surfaceContainer = Color(0xFF17202B),
    surfaceContainerHigh = Color(0xFF1E2833),
    surfaceContainerHighest = Color(0xFF26313D),
    outline = Color(0xFF4A5868),
    outlineVariant = Color(0xFF2A3542),
    error = Color(0xFFFF8A80),
)

/** Couleurs propres à l'application (le vert est réservé aux informations vérifiées). */
@Immutable
data class MedFindColors(
    val verified: Color,
    val verifiedContainer: Color,
    val demo: Color,
    val demoContainer: Color,
)

private val LightExtra = MedFindColors(
    verified = Color(0xFF1E7F52),
    verifiedContainer = Color(0xFFE5F5EC),
    demo = Color(0xFF8A5A00),
    demoContainer = Color(0xFFFFF3D6),
)

private val DarkExtra = MedFindColors(
    verified = Color(0xFF6FD6A0),
    verifiedContainer = Color(0xFF173528),
    demo = Color(0xFFFFCF70),
    demoContainer = Color(0xFF3A2C10),
)

val LocalMedFindColors = staticCompositionLocalOf { LightExtra }

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = bodyMedium.copy(lineHeight = 20.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MedFindTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalMedFindColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object MedFindThemeExtras {
    val colors: MedFindColors
        @Composable get() = LocalMedFindColors.current
}
