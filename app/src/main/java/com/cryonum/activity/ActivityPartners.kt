package com.cryonum.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.PartnersScreenBridge

class ActivityPartners : AppCompatActivity() {

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
        PartnersScreenBridge.setPartnersContent(
            composeView = composeView,
            onBack = { finish() },
            onNafuLink = { openUrl("https://narfu.ru/") },
            onKometLink = { openUrl("https://t.me/TeamKomet") }
        )

        setContentView(composeView)
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
