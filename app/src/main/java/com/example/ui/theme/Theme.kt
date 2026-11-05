package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.DarkModeOption

private val OledDarkColorScheme = darkColorScheme(
    primary = BoxingRed,
    onPrimary = Color.White,
    primaryContainer = BoxingRedContainer,
    onPrimaryContainer = BoxingRed,
    secondary = BoxingChampionGold,
    onSecondary = Color.Black,
    tertiary = BoxingRestCyan,
    onTertiary = Color.Black,
    background = PureBlack,
    onBackground = TextPrimaryDark,
    surface = Color(0xFF0D0F13),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF161920),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2B3240)
)

private val CarbonDarkColorScheme = darkColorScheme(
    primary = BoxingRed,
    onPrimary = Color.White,
    primaryContainer = BoxingRedContainer,
    onPrimaryContainer = BoxingRed,
    secondary = BoxingChampionGold,
    onSecondary = Color.Black,
    tertiary = BoxingRestCyan,
    onTertiary = Color.Black,
    background = CarbonDarkBackground,
    onBackground = TextPrimaryDark,
    surface = CarbonDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CarbonDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BoxingRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = BoxingRedDark,
    secondary = BoxingWarningOrange,
    onSecondary = Color.White,
    tertiary = BoxingRestBlue,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCFD8DC)
)

@Composable
fun BoxingTimerTheme(
    darkModeOption: DarkModeOption = DarkModeOption.OLED_DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme = when (darkModeOption) {
        DarkModeOption.OLED_DARK -> OledDarkColorScheme
        DarkModeOption.CARBON_DARK -> CarbonDarkColorScheme
        DarkModeOption.LIGHT -> LightColorScheme
        DarkModeOption.SYSTEM -> if (systemInDark) CarbonDarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
