package com.cryonum.managers

import androidx.activity.enableEdgeToEdge
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
        // Используем современный метод для включения Edge-to-Edge
        activity.enableEdgeToEdge()

        val window = activity.window
        
        // Настройка поведения системных баров (например, скрытие по свайпу)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
