package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GeoPrimaryDark,
    onPrimary = GeoOnPrimaryDark,
    primaryContainer = GeoPrimaryContainerDark,
    onPrimaryContainer = GeoOnPrimaryContainerDark,
    secondary = GeoSecondaryDark,
    onSecondary = GeoOnSecondaryDark,
    secondaryContainer = GeoSecondaryContainerDark,
    onSecondaryContainer = GeoOnSecondaryContainerDark,
    background = GeoBackgroundDark,
    onBackground = GeoOnBackgroundDark,
    surface = GeoSurfaceDark,
    onSurface = GeoOnSurfaceDark,
    surfaceVariant = GeoSurfaceVariantDark,
    onSurfaceVariant = GeoOnSurfaceVariantDark,
    outline = GeoOutlineDark,
    outlineVariant = GeoOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimaryLight,
    onPrimary = GeoOnPrimaryLight,
    primaryContainer = GeoPrimaryContainerLight,
    onPrimaryContainer = GeoOnPrimaryContainerLight,
    secondary = GeoSecondaryLight,
    onSecondary = GeoOnSecondaryLight,
    secondaryContainer = GeoSecondaryContainerLight,
    onSecondaryContainer = GeoOnSecondaryContainerLight,
    background = GeoBackgroundLight,
    onBackground = GeoOnBackgroundLight,
    surface = GeoSurfaceLight,
    onSurface = GeoOnSurfaceLight,
    surfaceVariant = GeoSurfaceVariantLight,
    onSurfaceVariant = GeoOnSurfaceVariantLight,
    outline = GeoOutlineLight,
    outlineVariant = GeoOutlineVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default to false to ensure Geometric Balance styling is applied, not system dynamic color
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
