package com.icymath.headband

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.icymath.activity.ActivitySubstitutions
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager

class SplashActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val contextWithLocale = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If skip flag passed -> go straight to main screen
        // Skip splash if app was restarted due to theme change
        if (ThemeManager.shouldSkipSplash(this)) {
            ThemeManager.clearSkipSplashFlag(this)
            startActivity(Intent(this, ActivitySubstitutions::class.java))
            finish()
            return
        }

        // Apply splash theme
        setTheme(ThemeManager.getSplashTheme(this))

        // Apply System UI settings similar to SystemUiManager / UiManager
        try {
            val window = window

            // allow drawing under system bars
            try {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } catch (t: Throwable) {
                Log.w(TAG, "setDecorFitsSystemWindows failed: ${t.message}")
            }

            // Edge-to-edge convenience
            try {
                enableEdgeToEdge()
            } catch (ignored: Throwable) {
            }

            // make navigation bar visually transparent where possible
            try {
                window.navigationBarColor = Color.TRANSPARENT
            } catch (t: Throwable) {
                Log.w(TAG, "setNavigationBarColor failed: ${t.message}")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    window.navigationBarDividerColor = Color.TRANSPARENT
                } catch (t: Throwable) {
                    Log.w(TAG, "setNavigationBarDividerColor failed: ${t.message}")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    window.isNavigationBarContrastEnforced = false
                } catch (t: Throwable) {
                    Log.w(TAG, "setNavigationBarContrastEnforced failed: ${t.message}")
                }
            }

            // determine night mode to choose light/dark icons
            val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val lightTheme = uiMode != Configuration.UI_MODE_NIGHT_YES

            try {
                val decor = window.decorView
                val controller = WindowInsetsControllerCompat(window, decor)
                controller.isAppearanceLightNavigationBars = lightTheme
                controller.isAppearanceLightStatusBars = lightTheme
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } catch (t: Throwable) {
                Log.w(TAG, "WindowInsetsController setup failed: ${t.message}")
            }

        } catch (t: Throwable) {
            Log.w(TAG, "apply system ui changes failed: ${t.message}")
        }

        // Small delay and then launch main activity
        window.decorView.postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this@SplashActivity, ActivitySubstitutions::class.java))
                finish()
            }
        }, SPLASH_DELAY_MS)
    }

    companion object {
        private const val SPLASH_DELAY_MS = 80L
        private const val TAG = "SplashActivity"
    }
}
