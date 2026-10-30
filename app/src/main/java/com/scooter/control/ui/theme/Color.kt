package com.scooter.control.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class Palette(
    val id: String,
    val title: String,
    val isLight: Boolean,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color,
)

private val Danger = Color(0xFFFF453A)

val OrangePalette = Palette(
    "orange", "Orange", false,
    Color(0xFFFF7A00), Color.Black,
    Color(0xFF0F0F0F), Color(0xFF1C1C1E), Color(0xFF262628),
    Color(0xFFF5F5F5), Color(0xFF9A9A9E), Danger,
)
val BluePalette = Palette(
    "blue", "Ocean", false,
    Color(0xFF3D9BFF), Color.Black,
    Color(0xFF0B0F17), Color(0xFF141B27), Color(0xFF1D2636),
    Color(0xFFF2F6FF), Color(0xFF8F9BB3), Danger,
)
val GreenPalette = Palette(
    "green", "Matrix", false,
    Color(0xFF2ED573), Color.Black,
    Color(0xFF0A120D), Color(0xFF121C16), Color(0xFF1A2820),
    Color(0xFFF0FFF5), Color(0xFF8FA597), Danger,
)
val PurplePalette = Palette(
    "purple", "Violet", false,
    Color(0xFFB06BFF), Color.Black,
    Color(0xFF100B17), Color(0xFF1A1325), Color(0xFF251B34),
    Color(0xFFF7F0FF), Color(0xFF9D8FB3), Danger,
)
val LightPalette = Palette(
    "light", "Light", true,
    Color(0xFFFF7A00), Color.White,
    Color(0xFFF4F4F6), Color(0xFFFFFFFF), Color(0xFFE8E8EC),
    Color(0xFF1A1A1A), Color(0xFF6B6B70), Color(0xFFD92D20),
)

/** Секретная тема. Открывается долгим тапом по логотипу (5 раз) — нигде не анонсируется. */
val KawaiiPalette = Palette(
    "kawaii", "Kawaii", false,
    Color(0xFFFF8FD6), Color(0xFF3A0F2B),
    Color(0xFF2B0F23), Color(0xFF3D1733), Color(0xFF4F1E43),
    Color(0xFFFFE6F7), Color(0xFFE3A6D2), Color(0xFFFF6F91),
)

val AllPalettes = listOf(OrangePalette, BluePalette, GreenPalette, PurplePalette, LightPalette)

fun paletteById(id: String?): Palette = AllPalettes.firstOrNull { it.id == id } ?: OrangePalette

val LocalPalette = compositionLocalOf { OrangePalette }
val LocalAnimations = compositionLocalOf { true }
