package com.cryonum.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.cryonum.R
import com.cryonum.items.ThemeItem
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.ThemeSelectionScreenBridge

class ActivityThemeSelection : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val composeView = ComposeView(this)

        ThemeSelectionScreenBridge.setThemeSelectionContent(
            composeView = composeView,
            onBack = { finish() },
            themes = makeThemeList(),
            currentTheme = ThemeManager.loadTheme(this),
            onThemeSelected = { theme ->
                ThemeManager.restartWithNewTheme(this, theme)
            }
        )

        setContentView(composeView)
    }

    private fun makeThemeList(): List<ThemeItem> {
        return listOf(
            ThemeItem(
                R.string.ClassicWhite,
                R.string.desc_theme_classic_light,
                ThemeManager.AppTheme.LIGHT
            ),
            ThemeItem(
                R.string.DarkAMOLED,
                R.string.desc_theme_amoled,
                ThemeManager.AppTheme.AMOLED
            ),
            ThemeItem(
                R.string.SandyBrown,
                R.string.desc_theme_sandybrown,
                ThemeManager.AppTheme.SANDY_BROWN
            ),
            ThemeItem(
                R.string.SystemTheme,
                R.string.desc_theme_system,
                ThemeManager.AppTheme.SYSTEM
            )
        )
    }
}
