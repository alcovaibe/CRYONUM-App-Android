package com.icymath.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

object SecurityManager {
    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
    private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
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
            it.remove(PIN_SALT_KEY)
            it.remove(FAILED_ATTEMPTS)
            it.remove(LOCKOUT_UNTIL)
            it.remove(IS_BIOMETRIC_ENABLED)
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
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = pbkdf2(pin, salt)
        val saltString = bytesToHex(salt)
        val hashString = bytesToHex(hash)
        
        context.dataStore.edit { 
            it[PIN_HASH_KEY] = hashString
            it[PIN_SALT_KEY] = saltString
            it[FAILED_ATTEMPTS] = 0
            it[LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(context: Context, pin: String): Boolean {
        // S-04: Enforce lockout in the domain method
        if (getRemainingLockoutTime(context) > 0) return false
        
        val prefs = context.dataStore.data.first()
        val savedHash = prefs[PIN_HASH_KEY] ?: return false
        val savedSalt = prefs[PIN_SALT_KEY] ?: return false
        
        val salt = hexToBytes(savedSalt)
        val currentHash = pbkdf2(pin, salt)
        val currentHashString = bytesToHex(currentHash)
        
        // C-02: Use MessageDigest.isEqual for constant-time comparison (prevention of timing attacks)
        val isCorrect = MessageDigest.isEqual(savedHash.toByteArray(), currentHashString.toByteArray())
        
        if (isCorrect) {
            context.dataStore.edit { 
                it[FAILED_ATTEMPTS] = 0 
                it[LOCKOUT_UNTIL] = 0L
            }
        } else {
            val attempts = (prefs[FAILED_ATTEMPTS] ?: 0) + 1
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

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
