package com.knot.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = KnotDarkBrown,
    onPrimary = KnotCream,
    primaryContainer = KnotSand,
    onPrimaryContainer = KnotInk,
    secondary = KnotSage,
    background = KnotCream,
    onBackground = KnotInk,
    surface = KnotCream,
    onSurface = KnotInk,
    surfaceVariant = KnotSand,
    error = KnotAlert
)

private val DarkColors = darkColorScheme(
    primary = KnotClay,
    onPrimary = KnotInk,
    primaryContainer = KnotClayDark,
    onPrimaryContainer = KnotCream,
    secondary = KnotSage,
    background = KnotDarkBackground,
    onBackground = KnotCream,
    surface = KnotDarkSurface,
    onSurface = KnotCream,
    error = KnotAlert
)

private val MonochromeColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF666666),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF444444),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF888888),
    error = Color(0xFF333333),
    onError = Color(0xFFFFFFFF)
)

private val InvertColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2B2B2B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFAAAAAA),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFCCCCCC),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFF888888),
    error = Color(0xFFCCCCCC),
    onError = Color(0xFF000000)
)

@Composable
fun KnotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // off by default to keep the intentional soft/neutral palette
    appTheme: String = "Default",
    textSizeMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        appTheme == "Monochrome" -> MonochromeColors
        appTheme == "Invert" -> InvertColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textSizeMultiplier
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalTextStyle provides TextStyle(fontFamily = JudsonFontFamily)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KnotTypography,
            content = content
        )
    }
}
