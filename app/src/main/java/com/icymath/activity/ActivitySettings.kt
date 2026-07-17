package com.icymath.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.icymath.R
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.SettingItemCompose
import com.icymath.ui.activity.SettingsScreenBridge

class ActivitySettings : AppCompatActivity() {

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

        // --- Инициализация списка настроек ---
        val rawSettings = listOf(
            SettingItemCompose(
                R.string.Themes,
                R.string.Themes,
                onClick = {
                    startActivity(Intent(this, ActivityThemeSelection::class.java))
                }
            ),
            SettingItemCompose(
                R.string.language,
                R.string.language,
                onClick = {
                    startActivity(Intent(this, ActivityLanguage::class.java))
                }
            ),
            SettingItemCompose(
                R.string.analytics,
                R.string.analytics,
                onClick = {
                    startActivity(Intent(this, ActivityAnalytics::class.java))
                }
            ),
            SettingItemCompose(
                R.string.security,
                R.string.security,
                onClick = {
                    val intent = Intent(this, ActivitySecurity::class.java).apply {
                        putExtra("MODE", ActivitySecurity.MODE_SETTINGS)
                    }
                    startActivity(intent)
                }
            ),
            SettingItemCompose(
                R.string.accessibility,
                R.string.accessibility,
                onClick = {
                    Toast.makeText(this, "Coming soon...", Toast.LENGTH_SHORT).show()
                }
            )
        )

        // Сортировка по алфавиту
        val sortedSettings = rawSettings.sortedBy { getString(it.nameRes) }

        // --- Инициализация Compose интерфейса ---
        val composeView = ComposeView(this)
        
        SettingsScreenBridge.setSettingsContent(
            composeView = composeView,
            onBack = { finish() },
            settings = sortedSettings
        )

        setContentView(composeView)
    }
}
