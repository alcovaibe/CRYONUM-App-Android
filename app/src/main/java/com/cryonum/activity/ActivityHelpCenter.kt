package com.cryonum.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.HelpCenterScreenBridge
import androidx.core.net.toUri

class ActivityHelpCenter : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.applyLocale(
                newBase,
                LocaleManager.getSavedLanguage(newBase)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Lock orientation
        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val composeView = ComposeView(this)
        HelpCenterScreenBridge.setHelpCenterContent(
            composeView = composeView,
            onBack = { finish() },
            onTelegram = { openTelegram() }
        )

        setContentView(composeView)
    }

    private fun openTelegram() {
        val channel = "icy_math"
        val tgIntent = Intent(
            Intent.ACTION_VIEW,
            "tg://resolve?domain=$channel".toUri()
        )

        try {
            startActivity(tgIntent)
        } catch (_: ActivityNotFoundException) {
            // Telegram не установлен — fallback на браузер
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                "https://t.me/$channel".toUri()
            )
            startActivity(webIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Telegram", e)
        }
    }

    companion object {
        private const val TAG = "ActivityHelpCenter"
    }
}
