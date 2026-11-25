package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AtpSilverNavyColorScheme = darkColorScheme(
    primary = AtpMetallicSilver,
    onPrimary = AtpDarkBlueBg,
    primaryContainer = AtpSilverContainer,
    onPrimaryContainer = AtpLightSilver,
    secondary = AtpLightSilver,
    onSecondary = AtpDarkBlueBg,
    secondaryContainer = AtpElevatedNavy,
    onSecondaryContainer = AtpPureWhite,
    tertiary = AtpAmberWarning,
    onTertiary = AtpOnAmberWarning,
    tertiaryContainer = AtpAmberContainer,
    onTertiaryContainer = AtpOnAmberContainer,
    background = AtpDarkBlueBg,
    onBackground = AtpPureWhite,
    surface = AtpDeepNavySurface,
    onSurface = AtpPureWhite,
    surfaceVariant = AtpElevatedNavy,
    onSurfaceVariant = AtpMetallicSilver,
    surfaceContainerLowest = AtpDarkBlueBg,
    surfaceContainerLow = AtpDeepNavySurface,
    surfaceContainer = AtpDeepNavySurface,
    surfaceContainerHigh = AtpElevatedNavy,
    outline = AtpSilverBorder,
    outlineVariant = AtpSilverOutlineGlow,
    error = AtpErrorRose,
    onError = AtpOnErrorRose,
    errorContainer = AtpErrorContainerNavy,
    onErrorContainer = AtpOnErrorContainerNavy
)

private val AtpLightSilverColorScheme = lightColorScheme(
    primary = AtpLightPrimary,
    onPrimary = AtpPureWhite,
    primaryContainer = AtpLightSurfaceVariant,
    onPrimaryContainer = AtpDarkBlueBg,
    secondary = AtpLightSecondary,
    onSecondary = AtpPureWhite,
    secondaryContainer = AtpLightSilver,
    onSecondaryContainer = AtpDarkBlueBg,
    background = AtpLightBg,
    onBackground = AtpDarkBlueBg,
    surface = AtpLightSurface,
    onSurface = AtpDarkBlueBg,
    surfaceVariant = AtpLightSurfaceVariant,
    onSurfaceVariant = AtpLightSecondary,
    outline = AtpLightOutline
)

val AtpShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AtpVmTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AtpSilverNavyColorScheme else AtpLightSilverColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AtpTypography,
        shapes = AtpShapes,
        content = content
    )
}
