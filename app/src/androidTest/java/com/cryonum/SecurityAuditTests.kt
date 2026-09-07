package com.cryonum

import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryonum.managers.SecurityManager
import com.cryonum.managers.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class SecurityAuditTests {
    @Test fun concurrentPinAttemptsAreLimitedAndCorruptLockFailsClosed() = runBlocking {
        check(BuildConfig.LOCAL_AUDIT) { "Run only with -PlocalAudit=true on isolated test data" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val before = context.dataStore.data.first()
        try {
            SecurityManager.savePin(context, "2468")
            SecurityManager.setAppLockEnabled(context, true)
            coroutineScope { repeat(30) { launch(Dispatchers.Default) { assertFalse(SecurityManager.verifyPin(context,"1111")) } } }
            assertTrue(SecurityManager.getRemainingLockoutTime(context)>0)
            assertFalse(SecurityManager.verifyPin(context,"2468"))
            context.dataStore.edit { it[stringPreferencesKey("is_app_lock_enabled_enc")] = "corrupt" }
            assertTrue(SecurityManager.isAppLockEnabled(context))
            context.dataStore.edit { it[stringPreferencesKey("is_biometric_enabled_enc")] = "corrupt" }
            assertFalse(SecurityManager.isBiometricEnabled(context))
        } finally {
            context.dataStore.updateData { before }
            SecurityManager.setUnlocked(false)
        }
    }
}
