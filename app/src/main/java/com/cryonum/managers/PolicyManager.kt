package com.cryonum.managers

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.cryonum.R
import com.cryonum.activity.ActivityAbout
import com.cryonum.content.AtomicContentPublisher
import com.cryonum.content.ContentDependencies
import com.cryonum.pdf.ActivityPdfViewer
import com.cryonum.ui.components.dialogs.FinalDeclineDialog
import com.cryonum.ui.components.dialogs.FirstLaunchPolicyDialog
import com.cryonum.ui.components.dialogs.PolicyDialogContent
import com.cryonum.ui.theme.IcyMathTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * PolicyManager — менеджер управления политикой конфиденциальности.
 */
object PolicyManager {

    private const val TAG = "PolicyManager"

    enum class PolicyDialogType { FIRST_LAUNCH, UPDATE, FINAL_DECLINE }
    var currentDialogType by mutableStateOf<PolicyDialogType?>(null)
        private set

    const val EXTRA_PDF_PATH = "pdf_path"
    const val EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END = "show_accept_dialog_on_scroll_end"
    const val EXTRA_FROM_NOTIFICATION = "from_notification"
    const val EXTRA_FROM_DIALOG_VIEW_ACTION = "from_dialog_view_action"
    const val EXTRA_OPEN_POLICY_FROM_NOTIFICATION = "open_policy_from_notification"
    const val EXTRA_POLICY_VERSION_TO_ACCEPT = "policy_version_to_accept"

    private const val BUNDLED_POLICY_VERSION = 4
    private const val PREF_NAME = "policy_prefs"
    private const val KEY_ACCEPTED_VERSION = "accepted_policy_version"
    private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

    private const val NOTIFICATION_CHANNEL_ID = "policy_update_channel"
    private const val NOTIFICATION_ID = 2025

    @JvmStatic
    fun getAcceptedVersion(context: Context?): Int {
        if (context == null) return 0
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACCEPTED_VERSION, 0)
    }

    @JvmStatic
    fun isPolicyAccepted(context: Context?): Boolean {
        if (context == null) return true
        return getAcceptedVersion(context) > 0
    }

    @JvmStatic
    @JvmOverloads
    fun acceptPolicy(context: Context?, versionCode: Int = BUNDLED_POLICY_VERSION) {
        if (context == null) return
        if (versionCode <= 0) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val acceptedVersion = maxOf(versionCode, getAcceptedVersion(context))
        prefs.edit { putInt(KEY_ACCEPTED_VERSION, acceptedVersion) }
        ContentDependencies.get(context).policyUpdateCoordinator.checkNow()
        dismissDialog()
    }

    @JvmStatic
    fun shouldRequestNotificationPermission(context: Context): Boolean {
        if (getAcceptedVersion(context) <= 0) return false
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    @JvmStatic
    fun markNotificationPermissionRequested(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true) }
    }

    @JvmStatic
    fun requestFirstLaunchDialog() {
        currentDialogType = PolicyDialogType.FIRST_LAUNCH
    }

    @JvmStatic
    fun requestAcceptDialog() {
        currentDialogType = PolicyDialogType.UPDATE
    }

    @JvmStatic
    fun requestFinalDeclineDialog() {
        currentDialogType = PolicyDialogType.FINAL_DECLINE
    }

    @JvmStatic
    fun dismissDialog() {
        currentDialogType = null
    }

    @Composable
    fun PolicyDialogHandler(
        onLaunchViewer: (isFirstLaunch: Boolean) -> Unit,
        onExitApp: () -> Unit
    ) {
        val dialogType = currentDialogType ?: return
        val context = LocalContext.current

        Dialog(
            onDismissRequest = { /* Critical dialogs */ },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            IcyMathTheme {
                when (dialogType) {
                    PolicyDialogType.FIRST_LAUNCH -> {
                        FirstLaunchPolicyDialog(
                            onReadClick = {
                                dismissDialog()
                                onLaunchViewer(true)
                            }
                        )
                    }
                    PolicyDialogType.UPDATE -> {
                        PolicyDialogContent(
                            onViewPolicy = {
                                dismissDialog()
                                onLaunchViewer(false)
                            },
                            onAccept = {
                                acceptPolicy(context)
                            },
                            onDecline = {
                                requestFinalDeclineDialog()
                            }
                        )
                    }
                    PolicyDialogType.FINAL_DECLINE -> {
                        FinalDeclineDialog(
                            onAccept = {
                                acceptPolicy(context)
                            },
                            onDecline = {
                                onExitApp()
                            }
                        )
                    }
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun launchPolicyViewer(
        activity: Activity?,
        showAcceptDialogOnScrollEnd: Boolean = true,
        fromNotification: Boolean = false,
        fromDialogViewAction: Boolean = false,
        isFirstLaunchMode: Boolean = false
    ) {
        if (activity == null) return

        val assetFile = "privacy_policy.4.0.pdf"
        val privacyDirectory = File(activity.filesDir, "icy_content/privacy")
        val outFile = File(privacyDirectory, "privacy-policy.pdf")

        // Publish the trusted bundled bootstrap copy in persistent internal storage.
        if (!outFile.exists()) {
            if (!privacyDirectory.exists() && !privacyDirectory.mkdirs()) {
                Log.e(TAG, "Failed to create persistent policy directory")
                return
            }
            val temporary = File(privacyDirectory, "privacy-policy.bootstrap.tmp")
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = activity.assets.open(assetFile)
                outputStream = FileOutputStream(temporary)
                val buf = ByteArray(4096)
                var r: Int
                while (inputStream.read(buf).also { r = it } != -1) {
                    outputStream.write(buf, 0, r)
                }
                outputStream.flush()
                outputStream.fd.sync()
                outputStream.close()
                outputStream = null
                AtomicContentPublisher.move(temporary, outFile)
            } catch (e: Exception) {
                temporary.delete()
                Log.e(TAG, "Failed to publish bundled policy: $assetFile", e)
            } finally {
                try {
                    inputStream?.close()
                } catch (_: Exception) {
                }
                try {
                    outputStream?.close()
                } catch (_: Exception) {
                }
            }
        }

        val intent = Intent(activity, ActivityPdfViewer::class.java).apply {
            putExtra(EXTRA_PDF_PATH, outFile.absolutePath)
            putExtra(EXTRA_SHOW_ACCEPT_DIALOG_ON_SCROLL_END, showAcceptDialogOnScrollEnd)
            putExtra(EXTRA_FROM_NOTIFICATION, fromNotification)
            putExtra(EXTRA_FROM_DIALOG_VIEW_ACTION, fromDialogViewAction)
            putExtra("is_first_launch_mode", isFirstLaunchMode)
        }

        activity.startActivity(intent)
    }

    @JvmStatic
    fun showPolicyUpdateNotification(context: Context?, versionCode: Int, versionName: String): Boolean {
        if (context == null) return false
        if (versionCode <= getAcceptedVersion(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) return false

        val intent = Intent(context, ActivityAbout::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_POLICY_FROM_NOTIFICATION, true)
            putExtra(EXTRA_POLICY_VERSION_TO_ACCEPT, versionCode)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pending = PendingIntent.getActivity(
            context,
            versionName.hashCode(),
            intent,
            pendingFlags
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

        // Create notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.policy_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.policy_channel_description)
        }
        nm.createNotificationChannel(channel)

        val nb = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo) // Use appropriate icon
            .setContentTitle(context.getString(R.string.policy_update_title))
            .setContentText(context.getString(R.string.policy_update_notification_text, versionName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        notificationManagerCompat.notify(NOTIFICATION_ID, nb.build())
        return true
    }

}
