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
    onSurfaceVariant = KnotInk,
    error = KnotRed,
    onError = KnotCream
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2B2B2B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFAAAAAA),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFFFFFFF),
    //error = Color(0xFFCCCCCC),
    error = KnotRedDark,
    onError = Color(0xFFCCCCCC)
    //onError = Color(0xFF000000)
)

private val Mono = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF666666),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF000000),
    //error = Color(0xFF333333),
    error = KnotRed,
    onError = Color(0xFFFFFFFF)
   // onError = Color(0xFFFFFFFF)
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
        appTheme == "Mono" -> Mono
        appTheme == "Dark" -> DarkColors
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
