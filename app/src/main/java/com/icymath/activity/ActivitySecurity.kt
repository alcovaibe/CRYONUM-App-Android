package com.icymath.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.icymath.R
import com.icymath.managers.LocaleManager
import com.icymath.managers.SecurityManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.SecurityScreenBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivitySecurity : AppCompatActivity() {

    private var mode = MODE_SETTINGS
    private var firstPin = ""
    private lateinit var composeView: ComposeView
    
    private val setupAppLockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshSecuritySettings()
        }
    }

    private var isBiometricDialogShowing = false
    private var biometricPrompt: BiometricPrompt? = null
    
    companion object {
        const val MODE_UNLOCK = 0
        const val MODE_SETUP = 1
        const val MODE_SETTINGS = 2
        private const val TAG = "ActivitySecurity"
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        // Anti-screenshot protection
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        mode = intent.getIntExtra("MODE", MODE_SETTINGS)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        composeView = ComposeView(this)
        setContentView(composeView)
        initUi(composeView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mode == MODE_UNLOCK) {
                    moveTaskToBack(true)
                } else {
                    finish()
                }
            }
        })
        
        if (mode == MODE_UNLOCK) {
            lifecycleScope.launch {
                if (SecurityManager.isBiometricEnabled(this@ActivitySecurity)) {
                    showBiometricPrompt()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mode == MODE_SETTINGS) {
            refreshSecuritySettings()
        }
    }

    private fun refreshSecuritySettings() {
        lifecycleScope.launch {
            SecurityManager.refreshSecuritySettings(this@ActivitySecurity)
        }
    }

    private fun initUi(composeView: ComposeView) {
        composeView.setContent {
            if (mode == MODE_SETTINGS) {
                val securitySettings by SecurityManager.securitySettings.collectAsState()
                val isAppLockEnabled = securitySettings.isAppLockEnabled
                val isBiometricEnabled = securitySettings.isBiometricEnabled

                LaunchedEffect(Unit) {
                    SecurityManager.refreshSecuritySettings(this@ActivitySecurity)
                }

                SecurityScreenBridge.SecuritySettingsContent(
                    isAppLockEnabled = isAppLockEnabled,
                    onAppLockToggle = { enabled ->
                        if (enabled) {
                            val intent = Intent(this@ActivitySecurity, ActivitySecurity::class.java).apply {
                                putExtra("MODE", MODE_SETUP)
                            }
                            setupAppLockLauncher.launch(intent)
                        } else {
                            lifecycleScope.launch {
                                SecurityManager.setAppLockEnabled(this@ActivitySecurity, false)
                                SecurityManager.clearPin(this@ActivitySecurity)
                            }
                        }
                    },
                    isBiometricEnabled = isBiometricEnabled,
                    onBiometricToggle = { enabled ->
                        lifecycleScope.launch {
                            SecurityManager.setBiometricEnabled(this@ActivitySecurity, enabled)
                        }
                    },
                    onBack = { finish() }
                )
            } else {
                var pin by remember { mutableStateOf("") }
                var isBio by remember { mutableStateOf(false) }
                var lockoutTime by remember { mutableStateOf(0L) }
                
                LaunchedEffect(Unit) {
                    isBio = SecurityManager.isBiometricEnabled(this@ActivitySecurity)
                    while(true) {
                        lockoutTime = SecurityManager.getRemainingLockoutTime(this@ActivitySecurity)
                        if (lockoutTime > 0) delay(1000) else delay(5000)
                    }
                }

                var title by remember { 
                    mutableStateOf(
                        if (mode == MODE_UNLOCK) getString(R.string.enter_pin) 
                        else getString(R.string.set_pin)
                    ) 
                }
                var showError by remember { mutableStateOf(false) }

                val displayTitle = if (lockoutTime > 0) {
                    getString(R.string.lockout_message, (lockoutTime / 1000) + 1)
                } else {
                    title
                }

                SecurityScreenBridge.SecurityContent(
                    title = displayTitle,
                    pin = if (lockoutTime > 0) "" else pin,
                    isBiometricEnabled = isBio && mode == MODE_UNLOCK && lockoutTime <= 0,
                    isModeUnlock = mode == MODE_UNLOCK,
                    onPinChange = { newPin ->
                        if (lockoutTime > 0) return@SecurityContent
                        if (showError) showError = false
                        pin = newPin
                        if (pin.length == 4) {
                            handlePinEntry(pin) {
                                pin = ""
                                if (it != null) {
                                    title = it
                                    showError = true
                                }
                            }
                        }
                    },
                    onBiometricClick = { if (lockoutTime <= 0) showBiometricPrompt() },
                    showError = showError,
                    onBack = { 
                        if (mode == MODE_UNLOCK) moveTaskToBack(true) else finish() 
                    }
                )
            }
        }
    }

    private fun handlePinEntry(pin: String, onResult: (String?) -> Unit) {
        lifecycleScope.launch {
            when (mode) {
                MODE_UNLOCK -> {
                    if (SecurityManager.verifyPin(this@ActivitySecurity, pin)) {
                        SecurityManager.setUnlocked(true)
                        finish()
                    } else {
                        onResult(getString(R.string.error_enter))
                        delay(1000)
                        onResult(getString(R.string.enter_pin))
                    }
                }
                MODE_SETUP -> {
                    if (firstPin.isEmpty()) {
                        firstPin = pin
                        onResult(null)
                        onResult(getString(R.string.confirm_pin))
                    } else {
                        if (pin == firstPin) {
                            SecurityManager.savePin(this@ActivitySecurity, pin)
                            SecurityManager.setAppLockEnabled(this@ActivitySecurity, true)
                            SecurityManager.setUnlocked(true)
                            setResult(Activity.RESULT_OK)
                            Toast.makeText(this@ActivitySecurity, R.string.ok, Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            firstPin = ""
                            onResult(getString(R.string.pin_mismatch))
                            delay(1000)
                            onResult(getString(R.string.set_pin))
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        if (isBiometricDialogShowing) {
            Log.d(TAG, "Biometric dialog is already showing, skipping duplicate request")
            return
        }

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No biometric hardware available")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware is currently unavailable")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometric data enrolled")
                return
            }
        }

        isBiometricDialogShowing = true
        val executor = ContextCompat.getMainExecutor(this)

        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded")
                isBiometricDialogShowing = false
                SecurityManager.setUnlocked(true)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricDialogShowing = false
                
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    Log.d(TAG, "User cancelled biometric authentication")
                    return
                }

                Log.e(TAG, "Biometric authentication error: Code=$errorCode, Message=$errString")
                Toast.makeText(this@ActivitySecurity, errString, Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                isBiometricDialogShowing = false
                Log.d(TAG, "Biometric authentication failed")
            }
        }

        biometricPrompt = BiometricPrompt(this, executor, authenticationCallback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        promptInfoBuilder.setAllowedAuthenticators(authenticators)
        
        // Ensure no negative button text is set when DEVICE_CREDENTIAL is used,
        // as per the BiometricPrompt API requirements.
        
        val promptInfo = try {
            promptInfoBuilder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Error building prompt info", e)
            // Fallback for older devices/versions
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.cancel))
                .build()
        }

        try {
            biometricPrompt?.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing biometric prompt", e)
            isBiometricDialogShowing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isBiometricDialogShowing = false
        biometricPrompt = null
    }
}
