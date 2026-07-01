package com.icymath.managers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.icymath.R
import com.icymath.activity.ActivitySubstitutions
import androidx.core.content.edit

/**
 * ThemeManager — безопасная переработанная версия на Kotlin.
 */
object ThemeManager {

    enum class AppTheme(val styleResId: Int) {
        LIGHT(R.style.Theme_IcyMath_Light),
        AMOLED(R.style.Theme_IcyMath_Amoled),
        SANDY_BROWN(R.style.Theme_IcyMath_SandyBrown),
        SYSTEM(R.style.Theme_IcyMath_System);

        companion object {
            @JvmStatic
            fun fromValue(value: String?): AppTheme {
                if (value == null) return LIGHT
                return entries.find { it.name.equals(value, ignoreCase = true) } ?: LIGHT
            }
        }
    }

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_CURRENT_THEME = "current_theme"
    private const val KEY_SKIP_SPLASH = "skip_splash"

    @Volatile
    private var cachedTheme: AppTheme? = null
    
    @Volatile
    private var initialized = false

    @Volatile
    private var registeredLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    /**
     * Save theme selection (persist and update cache).
     */
    @JvmStatic
    fun saveTheme(context: Context, theme: AppTheme) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_CURRENT_THEME, theme.name) }

        cachedTheme = theme
    }

    /**
     * Load theme. Uses cached value when available, otherwise reads from prefs.
     */
    @JvmStatic
    fun loadTheme(context: Context): AppTheme {
        cachedTheme?.let { return it }

        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val v = prefs.getString(KEY_CURRENT_THEME, null)
        val t = AppTheme.fromValue(v)

        cachedTheme = t
        return t
    }

    /**
     * Apply activity theme.
     */
    @JvmStatic
    fun applyTheme(activity: Activity) {
        val theme = loadTheme(activity.applicationContext)
        activity.setTheme(theme.styleResId)
    }

    /**
     * Apply global night mode according to the provided AppTheme.
     */
    @JvmStatic
    fun applyNightMode(theme: AppTheme?) {
        val effective = theme ?: AppTheme.LIGHT

        val mode = when (effective) {
            AppTheme.AMOLED -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    /**
     * Persist theme, set skip splash flag, apply night mode and restart application flow.
     */
    @JvmStatic
    fun restartWithNewTheme(activity: Activity, theme: AppTheme) {
        val appCtx = activity.applicationContext
        saveTheme(appCtx, theme)

        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SKIP_SPLASH, true) }

        // Keep session unlocked during restart
        SecurityManager.setUnlocked(SecurityManager.isSessionUnlocked())

        applyNightMode(theme)

        val intent = Intent(activity, ActivitySubstitutions::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        activity.finishAffinity()
    }

    /**
     * Initialize ThemeManager.
     */
    @JvmStatic
    fun init(application: Application) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return
            val appCtx = application.applicationContext

            cachedTheme = loadTheme(appCtx)
            applyNightMode(cachedTheme)

            registeredLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    // Не применяем общую тему к SplashActivity, чтобы не перебивать белый фон
                    if (activity.javaClass.simpleName != "SplashActivity") {
                        applyTheme(activity)
                    }
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }.also {
                application.registerActivityLifecycleCallbacks(it)
            }
            initialized = true
        }
    }

    @JvmStatic
    fun getSplashTheme(context: Context): Int {
        return R.style.Theme_IcyMath_Splash
    }

    @JvmStatic
    fun shouldSkipSplash(context: Context): Boolean {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SKIP_SPLASH, false)
    }

    @JvmStatic
    fun clearSkipSplashFlag(context: Context) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SKIP_SPLASH).apply()
    }
}
