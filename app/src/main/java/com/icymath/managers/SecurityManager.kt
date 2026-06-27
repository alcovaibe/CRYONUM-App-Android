package com.icymath.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

data class SecuritySettings(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

object SecurityManager {
    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
    private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
    private val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
    private val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    private val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
    private val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    private val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

    private val isUnlockedSession = AtomicBoolean(false)
    private val isLockActivityVisible = AtomicBoolean(false)
    private val firstCheckPerformed = AtomicBoolean(false)
    private const val LOCK_TIMEOUT_MS = 60_000L // 1 minute
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds

    private val _securitySettings = MutableStateFlow(SecuritySettings())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    suspend fun refreshSecuritySettings(context: Context) {
        _securitySettings.value = SecuritySettings(
            isAppLockEnabled = isAppLockEnabled(context),
            isBiometricEnabled = isBiometricEnabled(context)
        )
    }

    fun setUnlocked(unlocked: Boolean) {
        isUnlockedSession.set(unlocked)
        if (unlocked) firstCheckPerformed.set(true)
    }

    fun isSessionUnlocked(): Boolean = isUnlockedSession.get()

    /**
     * Tries to "reserve" the lock screen showing action.
     * Returns true if successful (no lock screen is currently showing or pending).
     */
    fun checkAndMarkLocking(): Boolean {
        return isLockActivityVisible.compareAndSet(false, true)
    }

    fun setLockActivityVisible(visible: Boolean) {
        isLockActivityVisible.set(visible)
    }

    fun isLockActivityVisible(): Boolean = isLockActivityVisible.get()

    fun isFirstCheckPerformed(): Boolean = firstCheckPerformed.get()

    suspend fun clearPin(context: Context) {
        context.dataStore.edit { 
            it.remove(PIN_HASH_KEY)
            it.remove(PIN_SALT_KEY)
            it.remove(FAILED_ATTEMPTS)
            it.remove(LOCKOUT_UNTIL)
            it.remove(IS_BIOMETRIC_ENABLED)
        }
        _securitySettings.value = _securitySettings.value.copy(isBiometricEnabled = false)
        isUnlockedSession.set(false)
        firstCheckPerformed.set(false)
    }

    suspend fun isAppLockEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_APP_LOCK_ENABLED] ?: false }.first()
    }

    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_APP_LOCK_ENABLED] = enabled }
        _securitySettings.value = _securitySettings.value.copy(isAppLockEnabled = enabled)
        
        // Reset state so that next time it's enabled, it performs a fresh check
        isUnlockedSession.set(false)
        firstCheckPerformed.set(false)
    }

    suspend fun isBiometricEnabled(context: Context): Boolean {
        return context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }.first()
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
        _securitySettings.value = _securitySettings.value.copy(isBiometricEnabled = enabled)
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

        // 1. If we are already unlocked in this session, we only lock if we've been in background too long
        if (isUnlockedSession.get()) {
            val lastTime = context.dataStore.data.map { it[LAST_BACKGROUND_TIME] ?: 0L }.first()
            if (lastTime == 0L) return false // Active session, never backgrounded yet

            val diff = System.currentTimeMillis() - lastTime
            if (diff > LOCK_TIMEOUT_MS) {
                // Background timeout reached, invalidate session
                isUnlockedSession.set(false)
                return true
            }
            // Still within valid session time
            return false
        }

        // 2. If not unlocked yet, we check if it's a cold start or a genuine lock condition
        if (!firstCheckPerformed.get()) {
            return true
        }

        // Session was invalidated or never established (e.g. process death)
        return true
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val pinChars = pin.toCharArray()
        val spec = PBEKeySpec(pinChars, salt, 10000, 256)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            pinChars.fill('\u0000')
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            result.append(HEX_CHARS[(byte.toInt() ushr 4) and 0x0F])
            result.append(HEX_CHARS[byte.toInt() and 0x0F])
        }
        return result.toString()
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
