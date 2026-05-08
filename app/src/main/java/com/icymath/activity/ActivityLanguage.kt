package com.icymath.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.icymath.items.LanguageItem
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.LanguageScreenBridge

class ActivityLanguage : AppCompatActivity() {

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
        
        LanguageScreenBridge.setLanguageContent(
            composeView = composeView,
            onBack = { finish() },
            languages = getLanguages(this),
            onLanguageSelected = { item ->
                LocaleManager.restartWithNewLocale(this, item.code)
            }
        )

        setContentView(composeView)
    }

    private fun getLanguages(context: Context): List<LanguageItem> {
        val selected = LocaleManager.getSavedLanguage(context)
        val list = mutableListOf<LanguageItem>()

        addIfSupported(list, LanguageItem("ru", "Русский", "🇷🇺", "ru" == selected))
        addIfSupported(list, LanguageItem("en", "English", "🇬🇧", "en" == selected))
        addIfSupported(list, LanguageItem("de", "Deutsch", "🇩🇪", "de" == selected))

        return list
    }

    private fun addIfSupported(list: MutableList<LanguageItem>, item: LanguageItem) {
        if (LocaleManager.isLanguageSupported(item.code)) {
            list.add(item)
        }
    }
}
