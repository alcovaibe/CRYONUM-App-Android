package com.icymath.headband

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.icymath.activity.ActivitySecurity
import com.icymath.activity.ActivitySubstitutions
import com.icymath.managers.LocaleManager
import com.icymath.managers.SecurityManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.SplashScreenContent
import com.icymath.ui.theme.IcyMathTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val contextWithLocale = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If skip flag passed -> go straight to main screen
        if (ThemeManager.shouldSkipSplash(this)) {
            ThemeManager.clearSkipSplashFlag(this)
            startActivity(Intent(this, ActivitySubstitutions::class.java))
            finish()
            return
        }

        // Apply System UI settings
        enableEdgeToEdge()

        // Set Compose content immediately to avoid "empty" feel after XML splash
        setContent {
            IcyMathTheme {
                SplashScreenContent()
            }
        }

        // Reduced delay for faster transition, but enough for splash feel
        CoroutineScope(Dispatchers.Main).launch {
            // S-01: Delay long enough to show splash, but short enough to be fast
            delay(SPLASH_DELAY_MS)
            
            if (!isFinishing) {
                if (SecurityManager.shouldLock(this@SplashActivity)) {
                    if (SecurityManager.checkAndMarkLocking()) {
                        val intent = Intent(this@SplashActivity, ActivitySecurity::class.java).apply {
                            putExtra("MODE", ActivitySecurity.MODE_UNLOCK)
                            putExtra("LAUNCH_MAIN_ON_SUCCESS", true)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        finish()
                    }
                } else {
                    startActivity(Intent(this@SplashActivity, ActivitySubstitutions::class.java))
                    finish()
                }
            }
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 400L
    }
}
