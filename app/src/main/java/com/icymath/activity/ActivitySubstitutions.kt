package com.icymath.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.icymath.R
import com.icymath.content.ContentDependencies
import com.icymath.managers.ImagePicker
import com.icymath.managers.LocaleManager
import com.icymath.managers.PolicyManager
import com.icymath.managers.SystemUiManager
import com.icymath.managers.ThemeManager
import com.icymath.math.PermutationUtils
import com.icymath.ui.activity.SubstitutionsScreenBridge
import com.icymath.utils.InputFilter
import com.icymath.utils.SecurityUtils

class ActivitySubstitutions : AppCompatActivity() {

    private val upperLineState = mutableStateOf("", structuralEqualityPolicy())
    private val lowerLineState = mutableStateOf("", structuralEqualityPolicy())
    private val isFirstSelectedState = mutableStateOf(true, structuralEqualityPolicy())

    private var firstConfirmed = false
    private var firstInputMethodDialogShown = false

    private lateinit var imagePicker: ImagePicker

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestNotificationPermissionLauncher: ActivityResultLauncher<String>

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val contextWithLocale = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        SystemUiManager.applyEdgeToEdge(this)
        super.onCreate(savedInstanceState)
        
        // S-01: Quick check to prevent bypass window
        SecurityUtils.checkLock(this)

        registerLaunchers()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val composeView = ComposeView(this)
        setContentView(composeView)

        imagePicker = ImagePicker(this, object : ImagePicker.Callback {
            override fun onResult(upperLine: String?, lowerLine: String?) {
                if (!upperLine.isNullOrEmpty() && !lowerLine.isNullOrEmpty()) {
                    upperLineState.value = InputFilter.filterOnlyDigits(upperLine)
                    lowerLineState.value = InputFilter.filterOnlyDigits(lowerLine)
                } else {
                    Toast.makeText(this@ActivitySubstitutions, getString(R.string.error_photo_camera), Toast.LENGTH_LONG).show()
                }
            }
        })

        imagePicker.registerLaunchers(
            requestCameraPermissionLauncher,
            cameraLauncher,
            galleryLauncher
        )

        intent.getStringExtra("upperLine")?.let { upperLineState.value = it }
        intent.getStringExtra("lowerLine")?.let { lowerLineState.value = it }

        SubstitutionsScreenBridge.setContent(
            composeView = composeView,
            upperLineState = upperLineState,
            lowerLineState = lowerLineState,
            isFirstSelectedState = isFirstSelectedState,
            onMenuAction = { id -> handleMenuAction(id) },
            onConfirmClick = { confirm() },
            onInputBoxClick = { isFirst ->
                if (isFirst && !firstInputMethodDialogShown && upperLineState.value.isEmpty() && !firstConfirmed) {
                    firstInputMethodDialogShown = true
                }
            },
            onGenerateLine = { maxValue ->
                if (maxValue >= 1) {
                    val sb = StringBuilder()
                    for (i in 1..maxValue) sb.append(i)
                    upperLineState.value = sb.toString()
                    Toast.makeText(this@ActivitySubstitutions, getString(R.string.The_first_line_is_filled), Toast.LENGTH_SHORT).show()
                }
            },
            onLaunchPolicyViewer = { isFirst ->
                PolicyManager.launchPolicyViewer(this, isFirstLaunchMode = isFirst)
            },
            onExitApp = { finishAffinity() }
        )

        requestNotificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    ContentDependencies.get(this).policyUpdateCoordinator.checkNow()
                }
            }

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        SecurityUtils.checkLock(this)
        checkPolicy()
        requestPolicyNotificationPermissionOnce()
    }

    private fun checkPolicy() {
        if (PolicyManager.getAcceptedVersion(this) == 0) {
            PolicyManager.requestFirstLaunchDialog()
        }
    }

    private fun requestPolicyNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            !PolicyManager.shouldRequestNotificationPermission(this)
        ) return

        PolicyManager.markNotificationPermissionRequested(this)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imagePicker.clear()
    }

    private fun handleMenuAction(id: Int) {
        when (id) {
            R.id.nav_settings -> startActivity(Intent(this, ActivitySettings::class.java))
            R.id.nav_help_center -> startActivity(Intent(this, ActivityHelpCenter::class.java))
            R.id.partners -> startActivity(Intent(this, ActivityPartners::class.java))
            R.id.nav_schedule -> startActivity(Intent(this, ActivitySchedule::class.java))
            R.id.nav_calculator -> startActivity(Intent(this, ActivityCalculator::class.java))
            R.id.about -> startActivity(Intent(this, ActivityAbout::class.java))
            R.id.nav_reference -> startActivity(Intent(this, ActivityReferenceMaterial::class.java))
            R.id.nav_camera -> imagePicker.requestCamera()
            R.id.nav_gallery -> imagePicker.startGallery()
            R.id.nav_history -> startActivity(Intent(this, ActivityHistory::class.java))
            R.id.nav_home -> { /* Already here */ }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PolicyManager.EXTRA_FROM_NOTIFICATION, false) == true) {
            PolicyManager.launchPolicyViewer(this, true)
        }
    }

    private fun registerLaunchers() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted == true) {
                    imagePicker.startCamera()
                } else {
                    Toast.makeText(this, getString(R.string.permission_explanation), Toast.LENGTH_LONG).show()
                }
            }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    imagePicker.handleCameraResult()
                }
            }

        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    imagePicker.handleGalleryResult(result.data)
                }
            }
    }

    private fun confirm() {
        val first = upperLineState.value
        val second = lowerLineState.value

        if (!firstConfirmed) {
            if (first.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_first_string), Toast.LENGTH_SHORT).show()
                return
            }
            firstConfirmed = true
            isFirstSelectedState.value = false
        } else {
            if (second.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_second_string), Toast.LENGTH_SHORT).show()
                return
            }
            if (first.length != second.length) {
                Toast.makeText(this, getString(R.string.same_length_error), Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val permutation = second.map { Character.getNumericValue(it) }

                val inversions = PermutationUtils.countInversions(permutation)
                val parity = if (PermutationUtils.calculateParity(inversions)) "even" else "odd"

                val intent = Intent(this, ActivityResult::class.java).apply {
                    putExtra("inversions", inversions)
                    putExtra("parity", parity)
                    putExtra("firstLine", first)
                    putExtra("secondLine", second)
                }
                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_enter), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
