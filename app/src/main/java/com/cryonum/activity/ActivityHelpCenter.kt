package com.cryonum.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.cryonum.R
import com.cryonum.content.ContentDependencies
import com.cryonum.links.TelegramUrlPolicy
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.HelpCenterScreenBridge
import androidx.core.net.toUri
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

class ActivityHelpCenter : AppCompatActivity() {
    private var telegramOpenJob: Job? = null

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
        if (telegramOpenJob?.isActive == true) return
        telegramOpenJob = lifecycleScope.launch {
            val url = try {
                ContentDependencies.get(this@ActivityHelpCenter).remoteLinksRepository.telegramUrl()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to resolve Telegram link", e)
                Toast.makeText(
                    this@ActivityHelpCenter,
                    R.string.telegram_link_unavailable,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            openTelegramUrl(url)
        }
    }

    private fun openTelegramUrl(url: HttpUrl) {
        val channel = TelegramUrlPolicy.username(url)
        val tgIntent = Intent(
            Intent.ACTION_VIEW,
            "tg://resolve?domain=$channel".toUri()
        )

        try {
            startActivity(tgIntent)
        } catch (_: ActivityNotFoundException) {
            openTelegramInBrowser(url)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open Telegram app", e)
            openTelegramInBrowser(url)
        }
    }

    private fun openTelegramInBrowser(url: HttpUrl) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Telegram link", e)
            Toast.makeText(this, R.string.error_opening_link, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ActivityHelpCenter"
    }
}
