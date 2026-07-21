package com.icymath.ui.styles

import com.icymath.R

/**
 * Объект для управления системными темами (Splash, Manifest) на Kotlin.
 */
object AppThemes {

    /**
     * Возвращает ID ресурса темы Splash-экрана.
     */
    fun getSplashTheme(): Int {
        return R.style.Theme_App_Starting
    }

    /**
     * Основная тема приложения для Manifest.
     */
    val MainAppTheme = R.style.Theme_IcyMath
}
