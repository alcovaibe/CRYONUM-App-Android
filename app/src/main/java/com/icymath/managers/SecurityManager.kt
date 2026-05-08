package com.icymath.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

object SecurityManager {
    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
    private val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")

    private var isUnlockedSession = false
    private const val LOCK_TIMEOUT_MS = 60_000L // 1 minute

    fun setUnlocked(unlocked: Boolean) {
        isUnlockedSession = unlocked
    }

    fun isSessionUnlocked(): Boolean = isUnlockedSession

    suspend fun isAppLockEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_APP_LOCK_ENABLED] ?: false }.first()
    }

    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun isBiometricEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }.first()
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun savePin(context: Context, pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { it[PIN_HASH_KEY] = hash }
    }

    suspend fun verifyPin(context: Context, pin: String): Boolean {
        val savedHash = context.dataStore.data.map { it[PIN_HASH_KEY] }.first()
        return savedHash == hashPin(pin)
    }

    suspend fun hasPin(context: Context): Boolean {
        return context.dataStore.data.map { it[PIN_HASH_KEY] != null }.first()
    }

    suspend fun setLastBackgroundTime(context: Context, time: Long) {
        context.dataStore.edit { it[LAST_BACKGROUND_TIME] = time }
    }

    suspend fun shouldLock(context: Context): Boolean {
        if (!isAppLockEnabled(context)) return false
        if (isUnlockedSession) return false
        
        val lastTime = context.dataStore.data.map { it[LAST_BACKGROUND_TIME] ?: 0L }.first()
        if (lastTime == 0L) return true // First launch or cleared
        
        return System.currentTimeMillis() - lastTime > LOCK_TIMEOUT_MS
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
