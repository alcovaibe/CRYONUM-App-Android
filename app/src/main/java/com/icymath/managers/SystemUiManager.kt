package com.icymath.managers

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * SystemUiManager - современная реализация для Compose и Edge-to-Edge.
 * Теперь он в основном подготавливает Window, а основная логика отступов
 * реализуется через WindowInsets в Compose.
 */
object SystemUiManager {

    @JvmStatic
    fun applyEdgeToEdge(activity: AppCompatActivity) {
        val window = activity.window
        
        // Включаем отрисовку под системными барами
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Делаем системные бары полностью прозрачными
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Отключаем принудительный контраст для Navigation Bar (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Настройка цвета иконок (будет переопределяться в IcyMathTheme)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // По умолчанию ставим системное поведение
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
