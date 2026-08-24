package com.cryonum.content

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.system.ErrnoException
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cryonum.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.Call
import java.io.IOException

class ContentDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    @Volatile private var activeCall: Call? = null

    override suspend fun doWork(): Result {
        val bundle = inputData.getString(KEY_BUNDLE)?.let { runCatching { ContentBundle.valueOf(it) }.getOrNull() }
            ?: return Result.failure(errorData(ContentErrorCategory.SECURITY, "Invalid work input"))
        val repository = ContentDependencies.get(applicationContext).repository
        return try {
            setForeground(createForegroundInfo(bundle))
            repository.downloadBundle(
                bundle = bundle,
                onCallChanged = { activeCall = it },
                onProgress = { progress -> setProgress(progress.toData()) }
            )
            Result.success(workDataOf(KEY_PHASE to "COMPLETED"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ContentException) {
            if (e.retryable && runAttemptCount < MAX_AUTOMATIC_ATTEMPTS) {
                e.retryAfterMillis?.takeIf { it > 0 }?.let { delay(it) }
                Result.retry()
            } else {
                Result.failure(errorData(e.category, e.message))
            }
        } catch (e: IOException) {
            if (isNoSpace(e)) Result.failure(errorData(ContentErrorCategory.INSUFFICIENT_SPACE, e.message))
            else if (runAttemptCount < MAX_AUTOMATIC_ATTEMPTS) Result.retry()
            else Result.failure(errorData(ContentErrorCategory.NETWORK, e.message))
        } catch (e: Exception) {
            Result.failure(errorData(ContentErrorCategory.FILE_SYSTEM, e.message))
        } finally {
            activeCall = null
        }
    }

    override fun onStopped() {
        activeCall?.cancel()
        super.onStopped()
    }

    private fun DownloadProgress.toData(): Data = Data.Builder()
        .putString(KEY_PHASE, phase)
        .apply { fileId?.let { putString(KEY_FILE_ID, it) } }
        .putInt(KEY_FILE_INDEX, fileIndex)
        .putInt(KEY_FILE_COUNT, fileCount)
        .putLong(KEY_FILE_BYTES, fileBytes)
        .putLong(KEY_FILE_TOTAL_BYTES, fileTotalBytes)
        .putLong(KEY_OVERALL_BYTES, overallBytes)
        .putLong(KEY_OVERALL_TOTAL_BYTES, overallTotalBytes)
        .putInt(KEY_COMPLETED_FILES, completedFiles)
        .build()

    private fun errorData(category: ContentErrorCategory, detail: String?): Data = Data.Builder()
        .putString(KEY_ERROR_CATEGORY, category.name)
        .apply { detail?.take(160)?.let { putString(KEY_ERROR_DETAIL, it) } }
        .build()

    private fun createForegroundInfo(bundle: ContentBundle): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    applicationContext.getString(R.string.content_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(applicationContext.getString(R.string.content_notification_title))
            .setContentText(
                applicationContext.getString(
                    if (bundle == ContentBundle.LECTURES) R.string.content_lectures_download_title else R.string.content_policy_download_title
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .addAction(android.R.drawable.ic_delete, applicationContext.getString(R.string.cancel), cancelIntent)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun isNoSpace(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is ErrnoException && current.errno == OsConstants.ENOSPC) return true
            current = current.cause
        }
        return false
    }

    companion object {
        const val KEY_BUNDLE = "content_bundle"
        const val KEY_PHASE = "phase"
        const val KEY_FILE_ID = "file_id"
        const val KEY_FILE_INDEX = "file_index"
        const val KEY_FILE_COUNT = "file_count"
        const val KEY_FILE_BYTES = "file_bytes"
        const val KEY_FILE_TOTAL_BYTES = "file_total_bytes"
        const val KEY_OVERALL_BYTES = "overall_bytes"
        const val KEY_OVERALL_TOTAL_BYTES = "overall_total_bytes"
        const val KEY_COMPLETED_FILES = "completed_files"
        const val KEY_ERROR_CATEGORY = "error_category"
        const val KEY_ERROR_DETAIL = "error_detail"
        private const val MAX_AUTOMATIC_ATTEMPTS = 4
        private const val NOTIFICATION_CHANNEL_ID = "content_downloads"
        private const val NOTIFICATION_ID = 2026
    }
}
