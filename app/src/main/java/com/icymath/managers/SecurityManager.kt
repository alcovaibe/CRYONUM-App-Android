package com.icymath.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

object SecurityManager {
    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
    private val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
    private val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    private val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

    private val isUnlockedSession = AtomicBoolean(false)
    private var firstCheckPerformed = false
    private const val LOCK_TIMEOUT_MS = 60_000L // 1 minute
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds

    fun setUnlocked(unlocked: Boolean) {
        isUnlockedSession.set(unlocked)
        if (unlocked) firstCheckPerformed = true
    }

    fun isSessionUnlocked(): Boolean = isUnlockedSession.get()

    suspend fun clearPin(context: Context) {
        context.dataStore.edit { 
            it.remove(PIN_HASH_KEY)
            it.remove(FAILED_ATTEMPTS)
            it.remove(LOCKOUT_UNTIL)
        }
        isUnlockedSession.set(false)
        firstCheckPerformed = false
    }

    suspend fun isAppLockEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_APP_LOCK_ENABLED] ?: false }.first()
    }

    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_APP_LOCK_ENABLED] = enabled }
        if (!enabled) {
            isUnlockedSession.set(false)
            firstCheckPerformed = false
        }
    }

    suspend fun isBiometricEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }.first()
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun savePin(context: Context, pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { 
            it[PIN_HASH_KEY] = hash 
            it[FAILED_ATTEMPTS] = 0
            it[LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(context: Context, pin: String): Boolean {
        val savedHash = context.dataStore.data.map { it[PIN_HASH_KEY] }.first()
        val isCorrect = savedHash == hashPin(pin)
        
        if (isCorrect) {
            context.dataStore.edit { 
                it[FAILED_ATTEMPTS] = 0 
                it[LOCKOUT_UNTIL] = 0L
            }
        } else {
            val attempts = (context.dataStore.data.map { it[FAILED_ATTEMPTS] ?: 0 }.first()) + 1
            context.dataStore.edit { 
                it[FAILED_ATTEMPTS] = attempts 
                if (attempts >= MAX_ATTEMPTS) {
                    it[LOCKOUT_UNTIL] = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                }
            }
        }
        return isCorrect
    }

    suspend fun getRemainingLockoutTime(context: Context): Long {
        val lockoutUntil = context.dataStore.data.map { it[LOCKOUT_UNTIL] ?: 0L }.first()
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    suspend fun hasPin(context: Context): Boolean {
        return context.dataStore.data.map { it[PIN_HASH_KEY] != null }.first()
    }

    suspend fun setLastBackgroundTime(context: Context, time: Long) {
        context.dataStore.edit { it[LAST_BACKGROUND_TIME] = time }
    }

    suspend fun shouldLock(context: Context): Boolean {
        if (!isAppLockEnabled(context)) return false
        
        // Cold start check
        if (!firstCheckPerformed) return true
        
        // Session check
        if (isUnlockedSession.get()) return false
        
        val lastTime = context.dataStore.data.map { it[LAST_BACKGROUND_TIME] ?: 0L }.first()
        if (lastTime == 0L) return true
        
        val diff = System.currentTimeMillis() - lastTime
        return diff > LOCK_TIMEOUT_MS
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
