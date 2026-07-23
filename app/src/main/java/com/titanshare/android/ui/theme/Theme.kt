package com.titanshare.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TitanDarkColorScheme = darkColorScheme(
    primary          = ElectricBlue,
    onPrimary        = TextPrimary,
    primaryContainer = NavyCard,
    onPrimaryContainer = TextPrimary,
    secondary        = PurpleAccent,
    onSecondary      = TextPrimary,
    secondaryContainer = NavyCardLight,
    onSecondaryContainer = TextPrimary,
    tertiary         = SuccessGreen,
    background       = NavyDeep,
    onBackground     = TextPrimary,
    surface          = NavySurface,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline          = GlassBorder,
    error            = DangerRed,
    onError          = TextPrimary,
)

@Composable
fun TitanShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TitanDarkColorScheme,
        typography  = TitanTypography,
        content     = content
    )
}
