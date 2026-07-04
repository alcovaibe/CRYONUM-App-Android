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
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

data class SecuritySettings(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

object SecurityManager {
    private const val KEYSTORE_ALIAS = "security_pref_key"
    
    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
    private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
    private val IS_APP_LOCK_ENABLED = stringPreferencesKey("is_app_lock_enabled_enc")
    private val IS_BIOMETRIC_ENABLED = stringPreferencesKey("is_biometric_enabled_enc")
    private val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
    private val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    private val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

    // --- Encryption Helpers ---

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val existing = keyStore.getEntry(KEYSTORE_ALIAS, null)
        if (existing is KeyStore.SecretKeyEntry) return existing.secretKey

        val keyGen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun encryptBoolean(value: Boolean): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toString().toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    private fun decryptBoolean(encryptedData: String?): Boolean {
        if (encryptedData == null) return false
        return try {
            val data = android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP)
            val iv = ByteArray(12)
            System.arraycopy(data, 0, iv, 0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(data, 12, data.size - 12)
            String(decrypted, StandardCharsets.UTF_8).toBoolean()
        } catch (_: Exception) {
            false
        }
    }

    // --- Original Logic ---

    private val isUnlockedSession = AtomicBoolean(false)
    private val isLockActivityVisible = AtomicBoolean(false)
    private val firstCheckPerformed = AtomicBoolean(false)
    
    @Volatile
    private var inMemoryBackgroundTime: Long = 0L

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
        if (unlocked) {
            firstCheckPerformed.set(true)
            inMemoryBackgroundTime = 0L // Start fresh session
        }
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
        return context.dataStore.data.map { decryptBoolean(it[IS_APP_LOCK_ENABLED]) }.first()
    }

    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_APP_LOCK_ENABLED] = encryptBoolean(enabled) }
        _securitySettings.value = _securitySettings.value.copy(isAppLockEnabled = enabled)
        
        // Reset state so that next time it's enabled, it performs a fresh check
        isUnlockedSession.set(false)
        firstCheckPerformed.set(false)
    }

    suspend fun isBiometricEnabled(context: Context): Boolean {
        return context.dataStore.data.map { decryptBoolean(it[IS_BIOMETRIC_ENABLED]) }.first()
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = encryptBoolean(enabled) }
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

    suspend fun setLastBackgroundTime(context: Context, time: Long) {
        inMemoryBackgroundTime = time
        context.dataStore.edit { it[LAST_BACKGROUND_TIME] = time }
    }

    suspend fun shouldLock(context: Context): Boolean {
        if (!isAppLockEnabled(context)) return false

        // 1. If we are already unlocked in this session, we only lock if we've been in background too long
        if (isUnlockedSession.get()) {
            // ONLY check in-memory time for active sessions. 
            // DataStore fallback is dangerous here during theme/locale restarts because
            // it might contain an old background time from a previous run.
            val lastTime = inMemoryBackgroundTime

            if (lastTime == 0L) return false // Active session, never backgrounded in this process run yet

            val diff = System.currentTimeMillis() - lastTime
            if (diff > LOCK_TIMEOUT_MS) {
                // Background timeout reached, invalidate session
                isUnlockedSession.set(false)
                return true
            }
            // Still within valid session time
            return false
        }

        // 2. If not unlocked yet, we check if it's a cold start.
        // If firstCheckPerformed is true, it means we already decided we need a lock,
        // but the lock activity might not have called setUnlocked(true) yet.
        if (!firstCheckPerformed.get()) {
            return true
        }

        // In case process was killed but firstCheckPerformed survived (though unlikely for AtomicBoolean),
        // or session was invalidated.
        return !isUnlockedSession.get()
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
