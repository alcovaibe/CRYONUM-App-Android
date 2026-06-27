package com.icymath.managers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.icymath.activity.ActivitySubstitutions
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Revised LocaleManager:
 * - remove expensive persistence call from applyLocale
 * - cache decrypted language and SecretKey (thread-safe)
 * - use applicationContext for SharedPreferences
 * - expose explicit saveLanguage(...) for user action
 * - restartWithNewLocale persists explicitly and then restarts
 * - init() loads cached value once and applies locale; lifecycle callbacks call applyLocale WITHOUT persisting
 */
object LocaleManager {

    private const val PREFS_NAME = "secure_settings"
    private const val KEY_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val DEFAULT_LANGUAGE = "ru"
    private const val KEYSTORE_ALIAS = "locale_lang_key"
    private const val TAG = "LocaleManager"

    // White-list supported languages
    private val supportedLanguages = setOf("ru", "en", "fr", "de")

    // cached values to avoid repeated expensive operations
    @Volatile
    private var cachedLanguage: String? = null

    @Volatile
    private var cachedKey: SecretKey? = null

    private var initialized = false

    @JvmStatic
    fun isLanguageSupported(code: String?): Boolean {
        return code != null && supportedLanguages.contains(code)
    }

    /**
     * Initialize LocaleManager. Must be called once (Application.onCreate).
     * Loads persisted language once (decrypts once) and applies to application context.
     */
    @JvmStatic
    fun init(application: Application) {
        if (initialized) return
        initialized = true

        val appCtx = application.applicationContext
        // load and cache saved language once (decryption happens here if needed)
        cachedLanguage = loadSavedLanguageOnce(appCtx)

        // apply locale to application context (no persisting here)
        applyLocale(appCtx, cachedLanguage)

        // Register lifecycle callbacks that only *apply* locale to each Activity
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Apply locale to the Activity's context (no persisting)
                applyLocale(activity, getSavedLanguage(activity))
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Apply locale to a context and return a configuration context.
     * IMPORTANT: this method does NOT persist language. Use saveLanguage(...) to persist.
     */
    @JvmStatic
    fun applyLocale(context: Context, language: String?): Context {
        val safeLanguage = if (isLanguageSupported(language)) language!! else DEFAULT_LANGUAGE

        val locale = Locale.forLanguageTag(safeLanguage)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Persist language (encrypted when possible) and update cache.
     * Use this when user explicitly chooses a new language.
     */
    @JvmStatic
    fun saveLanguage(context: Context, language: String) {
        val safe = if (isLanguageSupported(language)) language else DEFAULT_LANGUAGE
        val appCtx = context.applicationContext
        persistLanguage(appCtx, safe)
        cachedLanguage = safe
    }

    /**
     * Restart application flow with new locale chosen by user.
     * This persists the choice (encrypted if possible), applies locale and restarts the app.
     */
    @JvmStatic
    fun restartWithNewLocale(activity: Activity, language: String) {
        val safeLanguage = if (isLanguageSupported(language)) language else DEFAULT_LANGUAGE
        // persist explicitly
        saveLanguage(activity.applicationContext, safeLanguage)

        // Keep session unlocked during restart
        SecurityManager.setUnlocked(SecurityManager.isSessionUnlocked())

        // apply locale (no persisting inside)
        applyLocale(activity, safeLanguage)

        // Restart app / clear stack
        val intent = Intent(activity, ActivitySubstitutions::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finishAffinity()
    }

    /**
     * Get saved language. Uses cached value when available; otherwise reads from prefs and attempts decrypt.
     * If decryption fails we fall back to the raw stored string (to be tolerant on upgrades/fallbacks).
     */
    @JvmStatic
    fun getSavedLanguage(context: Context): String {
        cachedLanguage?.let { return it }

        val appCtx = context.applicationContext
        val stored = getPrefs(appCtx).getString(KEY_LANGUAGE, null)
        if (stored.isNullOrBlank()) {
            cachedLanguage = DEFAULT_LANGUAGE
            return DEFAULT_LANGUAGE
        }

        val dec = decrypt(stored)
        if (!dec.isNullOrBlank()) {
            cachedLanguage = dec
            return dec
        }

        // If decryption failed, assume stored is plaintext (backwards-compatible)
        cachedLanguage = stored
        return stored
    }

    // --- Internal helpers ---

    private fun loadSavedLanguageOnce(appCtx: Context): String {
        val stored = getPrefs(appCtx).getString(KEY_LANGUAGE, null)
        if (stored.isNullOrBlank()) {
            return DEFAULT_LANGUAGE
        }
        val dec = decrypt(stored)
        if (!dec.isNullOrBlank()) {
            return dec
        }
        // fallback to plaintext if decrypt fails
        return stored
    }

    private fun persistLanguage(appCtx: Context, language: String) {
        try {
            val enc = encrypt(language)
            getPrefs(appCtx).edit().putString(KEY_LANGUAGE, enc).apply()
        } catch (ex: Exception) {
            Log.w(TAG, "persistLanguage: encryption failed, saving plain. ${ex.message}")
            // fallback: save plain text (best effort). We avoid failing silently.
            getPrefs(appCtx).edit().putString(KEY_LANGUAGE, language).apply()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Key management & crypto (AES/GCM via AndroidKeyStore) ---
    // Caches SecretKey to avoid repeated KeyStore operations.
    @Throws(Exception::class)
    private fun getOrCreateKey(): SecretKey {
        cachedKey?.let { return it }

        synchronized(this) {
            cachedKey?.let { return it }

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val existing = keyStore.getEntry(KEYSTORE_ALIAS, null)
            if (existing is KeyStore.SecretKeyEntry) {
                cachedKey = existing.secretKey
                return cachedKey!!
            }

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGen.init(spec)
            cachedKey = keyGen.generateKey()
            return cachedKey!!
        }
    }

    @Throws(Exception::class)
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // typically 12 bytes
        val encrypted = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        val combo = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combo, 0, iv.size)
        System.arraycopy(encrypted, 0, combo, iv.size, encrypted.size)
        return Base64.encodeToString(combo, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            if (data.size < 13) return null
            val iv = ByteArray(12)
            System.arraycopy(data, 0, iv, 0, 12)
            val encSize = data.size - 12
            val encrypted = ByteArray(encSize)
            System.arraycopy(data, 12, encrypted, 0, encSize)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = getOrCreateKey()
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (ex: Exception) {
            Log.w(TAG, "decrypt failed: ${ex.message}")
            null
        }
    }
}
