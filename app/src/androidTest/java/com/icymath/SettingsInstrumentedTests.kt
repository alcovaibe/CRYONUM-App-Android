package com.icymath

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.icymath.managers.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальные тесты для проверки настроек приложения.
 */
@RunWith(AndroidJUnit4::class)
class SettingsInstrumentedTests {

    @Test
    fun testThemePersistence() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Сохраняем тему
        val targetTheme = ThemeManager.AppTheme.SANDY_BROWN
        ThemeManager.saveTheme(appContext, targetTheme)
        
        // Загружаем тему заново и проверяем
        val loadedTheme = ThemeManager.loadTheme(appContext)
        assertEquals(targetTheme, loadedTheme)
    }

    @Test
    fun testLanguagePersistence() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = appContext.getSharedPreferences("locale_prefs", Context.MODE_PRIVATE)
        
        val targetLang = "fr"
        prefs.edit().putString("saved_lang", targetLang).commit()
        
        val loadedLang = prefs.getString("saved_lang", "ru")
        assertEquals(targetLang, loadedLang)
    }
}
