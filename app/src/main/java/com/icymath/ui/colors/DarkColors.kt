package com.icymath.ui.colors

import androidx.compose.ui.graphics.Color

// Базовые цвета и константы (общие для всех палитр)
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val SandyBrown = Color(0xFFF4A460)
val DarkGrey = Color(0xFF696969)
val SlateBlue = Color(0xFF6A5ACD)
val Red = Color(0xFFFF0000)

// Дополнительные HTML-цвета
val LightCoral = Color(0xFFF08080)
val Tomato = Color(0xFFFF6347)
val Orange = Color(0xFFFFA500)
val Gold = Color(0xFFFFD700)
val Khaki = Color(0xFFF0E68C)
val LimeGreen = Color(0xFF32CD32)
val Turquoise = Color(0xFF40E0D0)
val SkyBlue = Color(0xFF87CEEB)
val DodgerBlue = Color(0xFF1E90FF)
val MediumPurple = Color(0xFF9370DB)
val Orchid = Color(0xFFDA70D6)
val HotPink = Color(0xFFFF69B4)
val RosyBrown = Color(0xFFBC8F8F)
val Chocolate = Color(0xFFD2691E)
val DimGray = Color(0xFF696969)

val EmailRed = Color(0xFFFF0000)

/**
 * Структура семантических цветов приложения.
 */
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val titleColor: Color,
    val headerColor: Color,
    val bottomNavBackground: Color,
    val cardBackground: Color,
    val cardStroke: Color,
    val keyboardKeyBackground: Color,
    val keyboardKeyText: Color,
    val inputFieldBackground: Color,
    val inputFieldText: Color,
    val confirmButtonBackground: Color,
    val confirmButtonText: Color,
    val dialogBackground: Color,
    val outlineActive: Color,
    val outlineInactive: Color,
    val historyCardTextPrimary: Color,
    val historyCardTextSecondary: Color,
    val historyCardBackground: Color,
    val drawerBackground: Color,
    val bgLight: Color,
    val bgDark: Color,
    val isLight: Boolean
)

/**
 * Темная палитра (перенесено из res/values-night/colors.xml)
 */
val AmoledPalette = AppColors(
    primary = White,
    onPrimary = Black,
    background = Black,
    onBackground = White,
    surface = Color(0xFF1A1A1A),
    onSurface = White,
    titleColor = Color(0xFFF8F8FF),
    headerColor = Color(0xFF1A1A1A), // Темно-серый для Amoled, чтобы не сливался
    bottomNavBackground = Black,
    cardBackground = Color(0xFF1A1A1A),
    cardStroke = DarkGrey,
    keyboardKeyBackground = Color(0xFF1A1A1A),
    keyboardKeyText = White,
    inputFieldBackground = Color(0xFF141414),
    inputFieldText = White,
    confirmButtonBackground = Color(0xFF8B5CF6),
    confirmButtonText = White,
    dialogBackground = Color(0xFF1A1A1A),
    outlineActive = White,
    outlineInactive = Color(0xFF777777),
    historyCardTextPrimary = White,
    historyCardTextSecondary = Color(0xFFCCCCCC),
    historyCardBackground = Color(0xFF1A1A1A),
    drawerBackground = Black,
    bgLight = Color(0xFF121212),
    bgDark = White,
    isLight = false
)
