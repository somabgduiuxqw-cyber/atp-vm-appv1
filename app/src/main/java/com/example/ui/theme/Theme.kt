package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AtpDarkColorScheme = darkColorScheme(
    primary = AtpTealPrimary,
    onPrimary = AtpTealOnPrimary,
    primaryContainer = AtpTealContainer,
    onPrimaryContainer = AtpTealOnContainer,
    secondary = AtpSkySecondary,
    onSecondary = AtpSkyOnSecondary,
    secondaryContainer = AtpSkyContainer,
    onSecondaryContainer = AtpSkyOnContainer,
    tertiary = AtpAmberTertiary,
    onTertiary = AtpAmberOnTertiary,
    tertiaryContainer = AtpAmberContainer,
    onTertiaryContainer = AtpAmberOnContainer,
    background = AtpDarkBackground,
    onBackground = AtpDarkOnSurface,
    surface = AtpDarkSurface,
    onSurface = AtpDarkOnSurface,
    surfaceVariant = AtpDarkSurfaceVariant,
    onSurfaceVariant = AtpDarkOnSurfaceMuted,
    surfaceContainerHigh = AtpDarkSurfaceElevated,
    outline = AtpDarkOutline,
    error = AtpErrorDark,
    onError = AtpOnErrorDark,
    errorContainer = AtpErrorContainerDark,
    onErrorContainer = AtpOnErrorContainerDark
)

private val AtpLightColorScheme = lightColorScheme(
    primary = AtpTealLightPrimary,
    onPrimary = AtpTealLightOnPrimary,
    primaryContainer = AtpTealLightContainer,
    onPrimaryContainer = AtpTealLightOnContainer,
    secondary = AtpSkyLightSecondary,
    onSecondary = AtpSkyLightOnSecondary,
    secondaryContainer = AtpSkyLightContainer,
    onSecondaryContainer = AtpSkyLightOnContainer,
    tertiary = AtpAmberLightTertiary,
    onTertiary = AtpAmberLightOnTertiary,
    tertiaryContainer = AtpAmberLightContainer,
    onTertiaryContainer = AtpAmberLightOnContainer,
    background = AtpLightBackground,
    onBackground = AtpLightOnSurface,
    surface = AtpLightSurface,
    onSurface = AtpLightOnSurface,
    surfaceVariant = AtpLightSurfaceVariant,
    onSurfaceVariant = AtpLightOnSurfaceMuted,
    outline = AtpLightOutline
)

val AtpShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun AtpVmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AtpDarkColorScheme
        else -> AtpLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AtpTypography,
        shapes = AtpShapes,
        content = content
    )
}
