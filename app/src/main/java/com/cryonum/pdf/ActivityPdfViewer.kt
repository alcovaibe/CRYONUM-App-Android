package com.cryonum.pdf

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.cryonum.R
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.PolicyManager
import kotlin.math.roundToInt

class ActivityPdfViewer : AppCompatActivity() {

    private var cameFromNotification = false
    private var cameFromDialogViewAction = false
    private var screenProtection: ScreenProtection? = null
    private var isFirstLaunchMode = false

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val contextWithLocale = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // edge-to-edge
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (ignored: Throwable) {
        }

        super.onCreate(savedInstanceState)

        // init protection and start it
        screenProtection = ScreenProtection(this).apply { start() }

        val filePath = intent.getStringExtra(PolicyManager.EXTRA_PDF_PATH)
        if (filePath == null) {
            finish()
            return
        }

        cameFromNotification = intent.getBooleanExtra(PolicyManager.EXTRA_FROM_NOTIFICATION, false)
        cameFromDialogViewAction = intent.getBooleanExtra(PolicyManager.EXTRA_FROM_DIALOG_VIEW_ACTION, false)
        isFirstLaunchMode = intent.getBooleanExtra("is_first_launch_mode", false)
        val shouldShowAcceptDialogOnScrollEnd =
            intent.getBooleanExtra(PolicyManager.EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END, false)
        val policyVersionToAccept = intent.getIntExtra(PolicyManager.EXTRA_POLICY_VERSION_TO_ACCEPT, 0)

        setContent {
            PdfViewerScreen(
                filePath = filePath,
                shouldShowAcceptDialogOnScrollEnd = shouldShowAcceptDialogOnScrollEnd,
                onBack = { handleBackPress() },
                onShowAcceptDialog = {
                    if (!cameFromDialogViewAction && !PolicyManager.isPolicyAccepted(this)) {
                        PolicyManager.requestAcceptDialog()
                    }
                },
                onPdfError = {
                    Toast.makeText(this, getString(R.string.error_rendering_pdf), Toast.LENGTH_LONG).show()
                    finish()
                },
                isFirstLaunchMode = isFirstLaunchMode,
                fromDialogViewAction = cameFromDialogViewAction,
                policyVersionToAccept = policyVersionToAccept.takeIf { it > 0 },
                onLaunchViewer = { isFirst ->
                    PolicyManager.launchPolicyViewer(this, isFirstLaunchMode = isFirst)
                },
                onExitApp = { finishAffinity() }
            )
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        finish()
    }


    override fun onDestroy() {
        screenProtection?.stop()
        screenProtection = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ActivityPdfViewer"
    }
}
