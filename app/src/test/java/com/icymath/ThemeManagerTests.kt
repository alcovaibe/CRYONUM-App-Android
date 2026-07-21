package com.icymath

import com.icymath.managers.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Модульные тесты для ThemeManager.
 */
class ThemeManagerTests {

    @Test
    fun testAppThemeFromValue() {
        // Проверка корректного сопоставления строковых имен и Enum
        assertEquals(ThemeManager.AppTheme.LIGHT, ThemeManager.AppTheme.fromValue("LIGHT"))
        assertEquals(ThemeManager.AppTheme.AMOLED, ThemeManager.AppTheme.fromValue("AMOLED"))
        assertEquals(ThemeManager.AppTheme.SANDY_BROWN, ThemeManager.AppTheme.fromValue("SANDY_BROWN"))
        assertEquals(ThemeManager.AppTheme.SYSTEM, ThemeManager.AppTheme.fromValue("SYSTEM"))
    }

    @Test
    fun testAppThemeFromValueCaseInsensitive() {
        // Проверка нечувствительности к регистру
        assertEquals(ThemeManager.AppTheme.AMOLED, ThemeManager.AppTheme.fromValue("amoled"))
        assertEquals(ThemeManager.AppTheme.LIGHT, ThemeManager.AppTheme.fromValue("Light"))
    }

    @Test
    fun testAppThemeFromValueDefault() {
        // Проверка возврата темы по умолчанию при неверных значениях
        assertEquals(ThemeManager.AppTheme.LIGHT, ThemeManager.AppTheme.fromValue(null))
        assertEquals(ThemeManager.AppTheme.LIGHT, ThemeManager.AppTheme.fromValue("NON_EXISTENT_THEME"))
    }
}
