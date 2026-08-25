package com.cryonum.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.cryonum.BuildConfig
import com.cryonum.R
import com.cryonum.content.ContentDownloadViewModel
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.PolicyManager
import com.cryonum.managers.ThemeManager
import com.cryonum.pdf.ActivityPdfViewer
import com.cryonum.ui.activity.setAboutContent

class ActivityAbout : AppCompatActivity() {
    private val contentViewModel: ContentDownloadViewModel by viewModels()
    private var pendingPolicyLaunch: PendingPolicyLaunch? = null

    companion object {
        private const val GITHUB_URL = "https://github.com/alcovaibe/CRYONUM-App-Android"
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Orientation lock
        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // --- Подготовка данных ---
        val versionText = getString(R.string.app_version, BuildConfig.APP_VERSION)

        // --- Инициализация Compose интерфейса ---
        val composeView = ComposeView(this)
        
        setAboutContent(
            composeView = composeView,
            appVersion = versionText,
            onBackClick = { finish() },
            onPrivacyClick = {
                contentViewModel.requestPolicy()
            },
            onSourceCodeClick = { openGitHub() },
            downloadViewModel = contentViewModel,
            onOpenPdf = ::openVerifiedPdf
        )

        setContentView(composeView)
        handlePolicyIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePolicyIntent(intent)
    }

    private fun handlePolicyIntent(intent: Intent) {
        val fromNotification = intent.getBooleanExtra(
            PolicyManager.EXTRA_OPEN_POLICY_FROM_NOTIFICATION,
            false
        )
        val requestDownload = intent.getBooleanExtra(
            PolicyManager.EXTRA_REQUEST_POLICY_DOWNLOAD,
            false
        )
        if (!fromNotification && !requestDownload) return

        pendingPolicyLaunch = PendingPolicyLaunch(
            showAcceptDialogOnScrollEnd = intent.getBooleanExtra(
                PolicyManager.EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END,
                false
            ),
            fromNotification = fromNotification || intent.getBooleanExtra(
                PolicyManager.EXTRA_FROM_NOTIFICATION,
                false
            ),
            fromDialogViewAction = intent.getBooleanExtra(
                PolicyManager.EXTRA_FROM_DIALOG_VIEW_ACTION,
                false
            ),
            isFirstLaunchMode = intent.getBooleanExtra("is_first_launch_mode", false),
            policyVersionToAccept = intent.getIntExtra(
                PolicyManager.EXTRA_POLICY_VERSION_TO_ACCEPT,
                0
            )
        )
        intent.removeExtra(PolicyManager.EXTRA_OPEN_POLICY_FROM_NOTIFICATION)
        intent.removeExtra(PolicyManager.EXTRA_REQUEST_POLICY_DOWNLOAD)
        contentViewModel.requestPolicy()
    }

    private fun openVerifiedPdf(path: String, contentVersion: Int?) {
        val pending = pendingPolicyLaunch
        val versionToAccept = pending?.policyVersionToAccept
            ?.takeIf { it > 0 }
            ?: contentVersion?.takeIf { it > PolicyManager.getAcceptedVersion(this) }
        val intent = Intent(this, ActivityPdfViewer::class.java).apply {
            putExtra(PolicyManager.EXTRA_PDF_PATH, path)
            putExtra(
                PolicyManager.EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END,
                pending?.showAcceptDialogOnScrollEnd ?: false
            )
            putExtra(PolicyManager.EXTRA_FROM_NOTIFICATION, pending?.fromNotification ?: false)
            putExtra(
                PolicyManager.EXTRA_FROM_DIALOG_VIEW_ACTION,
                pending?.fromDialogViewAction ?: (versionToAccept != null)
            )
            putExtra("is_first_launch_mode", pending?.isFirstLaunchMode ?: false)
            versionToAccept?.let { putExtra(PolicyManager.EXTRA_POLICY_VERSION_TO_ACCEPT, it) }
        }
        pendingPolicyLaunch = null
        startActivity(intent)
    }

    private fun openGitHub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private data class PendingPolicyLaunch(
        val showAcceptDialogOnScrollEnd: Boolean,
        val fromNotification: Boolean,
        val fromDialogViewAction: Boolean,
        val isFirstLaunchMode: Boolean,
        val policyVersionToAccept: Int
    )
}
