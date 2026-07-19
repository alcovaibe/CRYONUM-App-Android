package com.icymath.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.icymath.BuildConfig
import com.icymath.R
import com.icymath.managers.LocaleManager
import com.icymath.managers.PolicyManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.setAboutContent
import android.content.Intent

class ActivityAbout : AppCompatActivity() {

    companion object {
        private const val GITHUB_URL = "https://github.com/alcovaibe/Icy-Math"
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

        // --- Подготовка данных ---
        val versionText = getString(R.string.app_version, BuildConfig.APP_VERSION)

        // --- Инициализация Compose интерфейса ---
        val composeView = ComposeView(this)
        
        setAboutContent(
            composeView = composeView,
            appVersion = versionText,
            onBackClick = { finish() },
            onPrivacyClick = {
                try {
                    // isFirstLaunchMode = false, showAcceptDialogOnScrollEnd = false
                    PolicyManager.launchPolicyViewer(
                        activity = this,
                        showAcceptDialogOnScrollEnd = false,
                        fromNotification = false,
                        fromDialogViewAction = false,
                        isFirstLaunchMode = false
                    )
                } catch (_: Exception) { }
            },
            onSourceCodeClick = { openGitHub() }
        )

        setContentView(composeView)
    }

    private fun openGitHub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
            startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
