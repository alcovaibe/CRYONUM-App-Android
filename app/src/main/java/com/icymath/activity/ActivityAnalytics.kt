package com.icymath.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.icymath.analytics.AnalyticsManager
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.AnalyticsScreenBridge
import androidx.core.content.edit

class ActivityAnalytics : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Orientation lock
        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ANALYTICS_ENABLED, false)

        val composeView = ComposeView(this)
        AnalyticsScreenBridge.setAnalyticsContent(
            composeView = composeView,
            initialEnabled = enabled,
            onToggle = { isChecked ->
                prefs.edit { putBoolean(KEY_ANALYTICS_ENABLED, isChecked) }
                try {
                    AnalyticsManager.setAnalyticsEnabled(isChecked)
                } catch (_: Throwable) {
                }
            },
            onBack = { finish() }
        )

        setContentView(composeView)
    }
}
