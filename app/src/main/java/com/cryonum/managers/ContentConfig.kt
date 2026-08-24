package com.cryonum.managers

import android.content.Context
import android.os.Build
import com.cryonum.BuildConfig
import androidx.core.content.edit


object ContentConfig {
    private const val PREFS_NAME = "app_config_prefs"
    private const val KEY_LAST_SYNC = "last_config_sync"
    private const val ACTIVATION_DELAY = 172800000L


    fun isExtraContentEnabled(context: Context): Boolean {
        // В режиме разработки контент доступен всегда
        if (BuildConfig.DEBUG) return true

        // Если это среда тестирования (эмулятор), отключаем расширенный контент
        if (isTestEnvironment()) return false

        // Проверка времени активации для релизных сборок
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var firstSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        
        if (firstSync == 0L) {
            firstSync = System.currentTimeMillis()
            prefs.edit { putLong(KEY_LAST_SYNC, firstSync) }
        }

        return (System.currentTimeMillis() - firstSync) >= ACTIVATION_DELAY
    }

    private fun isTestEnvironment(): Boolean {
        val finger = Build.FINGERPRINT
        val model = Build.MODEL
        val product = Build.PRODUCT
        val manu = Build.MANUFACTURER
        val brand = Build.BRAND
        val hardware = Build.HARDWARE

        return finger.contains("generic")
                || finger.contains("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manu.contains("Genymotion")
                || brand.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
    }
}