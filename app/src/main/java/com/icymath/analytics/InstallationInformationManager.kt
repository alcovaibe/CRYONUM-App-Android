package com.icymath.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.icymath.BuildConfig
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

object InstallationInformationManager {

    private const val TAG = "InstallationInfoMgr"

    private const val PREFS_NAME = "installation_info_prefs"

    private const val KEY_PENDING_SOURCE = "pending_source"
    private const val KEY_PENDING_VERSION = "pending_version"
    private const val KEY_PENDING_INSTALL_ID = "pending_install_id"

    private const val KEY_SENT_INSTALL_ID = "sent_install_id"

    // постоянный install id key
    private const val KEY_INSTALL_ID = "install_id"

    private const val UNIQUE_IMMEDIATE_WORK_NAME = "send_install_info_immediate"
    private const val UNIQUE_EVENING_WORK_NAME = "send_install_info_evening"

    fun processInstallationInfo(context: Context) {
        try {
            if (isDebugBuild()) {
                Log.d(TAG, "Debug build detected - пропускаем сбор/отправку install info")
                return
            }

            val info = collectInstallInfo(context) ?: run {
                Log.w(TAG, "Не удалось собрать InstallationInfo - отмена")
                return
            }

            val sentId = getSentInstallId(context)
            if (sentId != null && sentId == info.androidId) {
                Log.d(TAG, "Install ID уже отправлен ранее - отправка не требуется")
                clearPendingInfo(context)
                return
            }

            savePendingInfo(context, info)
            enqueueImmediateSendWork(context)
            Log.d(TAG, "Информация об установке сохранена и отправка инициализирована")
        } catch (t: Throwable) {
            Log.e(TAG, "Ошибка в processInstallationInfo", t)
        }
    }

    private fun collectInstallInfo(context: Context): InstallInfo? {
        return try {
            val installerPackage = getInstallerPackageNameSafe(context)
            val source = mapInstallerToStore(installerPackage)

            val version = try {
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                pi.versionName ?: "unknown"
            } catch (e: Exception) {
                Log.e(TAG, "Не удалось получить PackageInfo.versionName", e)
                "unknown"
            }

            // используем собственный install id
            val installId = getOrCreateInstallId(context)

            InstallInfo(source, version, installId)
        } catch (e: Exception) {
            Log.e(TAG, "collectInstallInfo failed", e)
            null
        }
    }

    private fun getOrCreateInstallId(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_INSTALL_ID, null)
            if (!existing.isNullOrEmpty()) return existing

            synchronized(this) {
                val doubleCheck = prefs.getString(KEY_INSTALL_ID, null)
                if (!doubleCheck.isNullOrEmpty()) return doubleCheck

                val newId = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_INSTALL_ID, newId).apply()
                Log.d(TAG, "Generated new install id: $newId")
                newId
            }
        } catch (t: Throwable) {
            Log.e(TAG, "getOrCreateInstallId failed", t)
            "unknown"
        }
    }

    private fun getInstallerPackageNameSafe(context: Context): String? {
        return try {
            val pm = context.packageManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val installSourceInfo = pm.getInstallSourceInfo(context.packageName)
                    installSourceInfo.installingPackageName
                } catch (e: Throwable) {
                    Log.w(TAG, "getInstallSourceInfo вызвал исключение", e)
                    null
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(context.getPackageName())
                } catch (e: Throwable) {
                    Log.w(TAG, "getInstallerPackageName вызвал исключение", e)
                    null
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Ошибка при получении installer package", t)
            null
        }
    }

    private fun mapInstallerToStore(installer: String?): String {
        if (installer == null) return "Telegram"
        return when (installer) {
            "com.android.vending" -> "Play Market"
            "com.huawei.appmarket" -> "Huawei AppGallery"
            "com.sec.android.app.samsungapps" -> "Samsung Store"
            "com.hihonor.appmarket" -> "Honor Store"
            "ru.vk.store", "ru.store.rustore" -> "RuStore"
            "org.trashbox" -> "Trashbox"
            "com.apkpure.aegon" -> "ApkPure"
            "com.xiaomi.mipicks", "com.xiaomi.market" -> "Xiaomi GetApps"
            else -> installer
        }
    }

    private fun savePendingInfo(context: Context, info: InstallInfo) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PENDING_SOURCE, info.source)
                .putString(KEY_PENDING_VERSION, info.version)
                .putString(KEY_PENDING_INSTALL_ID, info.androidId)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "savePendingInfo failed", t)
        }
    }

    fun clearPendingInfo(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_PENDING_SOURCE)
                .remove(KEY_PENDING_VERSION)
                .remove(KEY_PENDING_INSTALL_ID)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "clearPendingInfo failed", t)
        }
    }

    fun saveSentAndroidId(context: Context, androidId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SENT_INSTALL_ID, androidId)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "saveSentAndroidId failed", t)
        }
    }

    fun loadPendingInfo(context: Context): InstallInfo? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val s = prefs.getString(KEY_PENDING_SOURCE, null)
            val v = prefs.getString(KEY_PENDING_VERSION, null)
            val id = prefs.getString(KEY_PENDING_INSTALL_ID, null)
            if (s != null && v != null && id != null) {
                InstallInfo(s, v, id)
            } else {
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "loadPendingInfo failed", t)
            null
        }
    }

    private fun getSentInstallId(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_SENT_INSTALL_ID, null)
        } catch (t: Throwable) {
            Log.e(TAG, "getSentInstallId failed", t)
            null
        }
    }

    private fun enqueueImmediateSendWork(context: Context) {
        try {
            val workRequest = OneTimeWorkRequest.Builder(SendInstallInfoWorker::class.java)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        } catch (t: Throwable) {
            Log.e(TAG, "enqueueImmediateSendWork failed", t)
        }
    }

    fun enqueueEveningRetryAtMoscow22(context: Context) {
        try {
            val delayMillis = computeDelayMillisToNextMoscow22()
            val delay = if (delayMillis > 0L) delayMillis else TimeUnit.MINUTES.toMillis(1)

            val workRequest = OneTimeWorkRequest.Builder(SendInstallInfoWorker::class.java)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_EVENING_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)

            Log.d(TAG, "Evening retry scheduled in ${delay / 1000} seconds (to Moscow 22:00).")
        } catch (t: Throwable) {
            Log.e(TAG, "enqueueEveningRetryAtMoscow22 failed", t)
        }
    }

    private fun computeDelayMillisToNextMoscow22(): Long {
        return try {
            val moscow = ZoneId.of("Europe/Moscow")
            val nowMoscow = ZonedDateTime.now(moscow)
            var target = nowMoscow.withHour(22).withMinute(0).withSecond(0).withNano(0)
            if (!target.isAfter(nowMoscow)) {
                target = target.plusDays(1)
            }
            val duration = Duration.between(Instant.now(), target.toInstant())
            duration.toMillis()
        } catch (t: Throwable) {
            Log.e(TAG, "computeDelayMillisToNextMoscow22 failed", t)
            -1L
        }
    }

    private fun isDebugBuild(): Boolean = BuildConfig.DEBUG
}
