package com.cryonum.ui.styles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Определение всех стилей приложения в Kotlin для использования в Compose.
 */
object AppStyles {

    // Стили для карточек (ранее были в AppCardStyle)
    val CardCornerRadius = 12.dp
    val CardElevation = 4.dp

    // Стили текста (ранее были в CardText)
    val CardTitleStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    val CardSubtitleStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Color.Gray
    )

    // Стили кнопок клавиатуры (ранее были в KeyboardButton)
    object Keyboard {
        val KeyHeight = 56.dp
        val KeyFontSize = 20.sp
        val KeyCornerRadius = 8.dp
    }

    // Параметры нижнего меню (замена BottomNavigationView.CustomStyle)
    object BottomNav {
        val IconSizeDp = 24.dp
        val ItemRippleColor = Color.Transparent
        val IndicatorColor = Color.Transparent
    }
}
