package com.icymath.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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

    private var mode = MODE_UNLOCK
    private var firstPin = ""
    
    companion object {
        const val MODE_UNLOCK = 0
        const val MODE_SETUP = 1
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

        mode = intent.getIntExtra("MODE", MODE_UNLOCK)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val composeView = ComposeView(this)
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

    private fun initUi(composeView: ComposeView) {
        composeView.setContent {
            var pin by remember { mutableStateOf("") }
            var isBio by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                isBio = SecurityManager.isBiometricEnabled(this@ActivitySecurity)
            }

            var title by remember { 
                mutableStateOf(
                    if (mode == MODE_UNLOCK) getString(R.string.enter_pin) 
                    else getString(R.string.set_pin)
                ) 
            }
            var showError by remember { mutableStateOf(false) }

            SecurityScreenBridge.SecurityContent(
                title = title,
                pin = pin,
                isBiometricEnabled = isBio && mode == MODE_UNLOCK,
                isModeUnlock = mode == MODE_UNLOCK,
                onPinChange = { newPin ->
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
                onBiometricClick = { showBiometricPrompt() },
                showError = showError,
                onBack = { 
                    if (mode == MODE_UNLOCK) moveTaskToBack(true) else finish() 
                }
            )
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
                            Toast.makeText(this@ActivitySecurity, R.string.settings, Toast.LENGTH_SHORT).show()
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
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecurityManager.setUnlocked(true)
                    finish()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
