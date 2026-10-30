package com.scooter.control.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Анимация, которая отключается настройкой «Анимации». */
@Composable
fun <T> motion(ms: Int = 300): AnimationSpec<T> =
    if (LocalAnimations.current) tween<T>(ms) else snap<T>()

@Composable
fun LastBotTheme(
    palette: Palette,
    animations: Boolean,
    content: @Composable () -> Unit,
) {
    val spec: AnimationSpec<Color> = if (animations) tween(450) else snap()
    val accent by animateColorAsState(palette.accent, spec, label = "accent")
    val onAccent by animateColorAsState(palette.onAccent, spec, label = "onAccent")
    val background by animateColorAsState(palette.background, spec, label = "bg")
    val surface by animateColorAsState(palette.surface, spec, label = "surface")
    val surfaceVariant by animateColorAsState(palette.surfaceVariant, spec, label = "surfaceVariant")
    val textPrimary by animateColorAsState(palette.textPrimary, spec, label = "text")
    val textSecondary by animateColorAsState(palette.textSecondary, spec, label = "textSecondary")
    val danger by animateColorAsState(palette.danger, spec, label = "danger")

    val live = palette.copy(
        accent = accent,
        onAccent = onAccent,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        danger = danger,
    )

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = live.accent, onPrimary = live.onAccent,
            background = live.background, onBackground = live.textPrimary,
            surface = live.surface, onSurface = live.textPrimary,
            surfaceVariant = live.surfaceVariant, onSurfaceVariant = live.textSecondary,
            error = live.danger,
        )
    } else {
        darkColorScheme(
            primary = live.accent, onPrimary = live.onAccent,
            background = live.background, onBackground = live.textPrimary,
            surface = live.surface, onSurface = live.textPrimary,
            surfaceVariant = live.surfaceVariant, onSurfaceVariant = live.textSecondary,
            error = live.danger,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = live.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = palette.isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalPalette provides live,
        LocalAnimations provides animations,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScooterTypography,
            content = content,
        )
    }
}
