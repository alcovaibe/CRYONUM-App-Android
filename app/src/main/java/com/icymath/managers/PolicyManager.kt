package com.icymath.managers

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.icymath.R
import com.icymath.activity.ActivitySubstitutions
import com.icymath.pdf.ActivityPdfViewer
import com.icymath.ui.components.dialogs.FinalDeclineDialog
import com.icymath.ui.components.dialogs.FirstLaunchPolicyDialog
import com.icymath.ui.components.dialogs.PolicyDialogContent
import com.icymath.ui.theme.IcyMathTheme
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

    const val REQUIRED_POLICY_VERSION = 4
    private const val PREF_NAME = "policy_prefs"
    private const val KEY_ACCEPTED_VERSION = "accepted_policy_version"

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
        return getAcceptedVersion(context) >= REQUIRED_POLICY_VERSION
    }

    @JvmStatic
    fun acceptPolicy(context: Context?) {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_ACCEPTED_VERSION, REQUIRED_POLICY_VERSION) }
        dismissDialog()
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
        val outFile = File(activity.cacheDir, assetFile)

        // copy asset to cache if missing
        if (!outFile.exists()) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = activity.assets.open(assetFile)
                outputStream = FileOutputStream(outFile)
                val buf = ByteArray(4096)
                var r: Int
                while (inputStream.read(buf).also { r = it } != -1) {
                    outputStream.write(buf, 0, r)
                }
                outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy asset to cache: $assetFile", e)
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
    fun showPolicyUpdateNotification(context: Context?) {
        if (context == null) return

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getInt(KEY_ACCEPTED_VERSION, 0)
        if (saved >= REQUIRED_POLICY_VERSION) return

        val intent = Intent(context, ActivitySubstitutions::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            pendingFlags
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Create notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.policy_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.policy_channel_description)
        }
        nm.createNotificationChannel(channel)

        val nb = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo) // Use appropriate icon
            .setContentTitle(context.getString(R.string.policy_update_title))
            .setContentText(context.getString(R.string.policy_update_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)

        nm.notify(NOTIFICATION_ID, nb.build())
    }

}
