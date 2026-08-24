package com.cryonum.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.cryonum.BuildConfig
import com.cryonum.R
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.PolicyManager
import com.cryonum.content.ContentDownloadViewModel
import com.cryonum.pdf.ActivityPdfViewer
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.setAboutContent
import android.content.Intent

class ActivityAbout : AppCompatActivity() {
    private val contentViewModel: ContentDownloadViewModel by viewModels()

    companion object {
        private const val GITHUB_URL = "https://github.com/alcovaibe/Icy-Math"
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
        if (intent.getBooleanExtra(PolicyManager.EXTRA_OPEN_POLICY_FROM_NOTIFICATION, false)) {
            intent.removeExtra(PolicyManager.EXTRA_OPEN_POLICY_FROM_NOTIFICATION)
            contentViewModel.requestPolicy()
        }
    }

    private fun openVerifiedPdf(path: String, contentVersion: Int?) {
        val versionToAccept = contentVersion?.takeIf { it > PolicyManager.getAcceptedVersion(this) }
        val intent = Intent(this, ActivityPdfViewer::class.java).apply {
            putExtra(PolicyManager.EXTRA_PDF_PATH, path)
            putExtra(PolicyManager.EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END, false)
            putExtra(PolicyManager.EXTRA_FROM_NOTIFICATION, false)
            putExtra(PolicyManager.EXTRA_FROM_DIALOG_VIEW_ACTION, versionToAccept != null)
            versionToAccept?.let { putExtra(PolicyManager.EXTRA_POLICY_VERSION_TO_ACCEPT, it) }
        }
        startActivity(intent)
    }

    private fun openGitHub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
            startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
