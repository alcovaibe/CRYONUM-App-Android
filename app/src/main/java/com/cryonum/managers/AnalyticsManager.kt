package com.cryonum.managers

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AnalyticsManager {

    private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val totalRamMB: Long,
        val totalStorageMB: Long,
        val appVersionName: String,
        val appVersionCode: Long,
        val androidSdk: Int
    )

    @Entity(tableName = "user_events")
    data class UserEvent(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val timestamp: Long,
        val screen: String = "",
        val action: String = "",
        val details: String? = null
    )

    @Entity(tableName = "crash_reports")
    data class CrashReport(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val timestamp: Long,
        val deviceInfoJson: String = "",
        val stacktrace: String = "",
        val recentEventsJson: String = ""
    )

    @Dao
    interface AnalyticsDao {
        @Insert
        fun insertEvent(event: UserEvent)

        @Query("SELECT * FROM user_events ORDER BY timestamp DESC LIMIT :limit")
        fun getRecentEvents(limit: Int): List<UserEvent>

        @Insert
        fun insertCrash(crash: CrashReport)

        @Query("DELETE FROM user_events WHERE timestamp < :before OR id NOT IN (SELECT id FROM user_events ORDER BY id DESC LIMIT 2000)")
        fun pruneEvents(before: Long)

        @Query("DELETE FROM crash_reports WHERE timestamp < :before OR id NOT IN (SELECT id FROM crash_reports ORDER BY id DESC LIMIT 20)")
        fun pruneCrashes(before: Long)
    }

    @Database(entities = [UserEvent::class, CrashReport::class], version = 1, exportSchema = false)
    abstract class AnalyticsDatabase : RoomDatabase() {
        abstract fun analyticsDao(): AnalyticsDao
    }

    private lateinit var appContext: Context
    private var db: AnalyticsDatabase? = null
    private var deviceInfo: DeviceInfo? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var previousUncaughtHandler: Thread.UncaughtExceptionHandler? = null

    @Volatile
    private var analyticsEnabled = false
    private val gson = Gson()

    @JvmStatic
    @Synchronized
    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabled = enabled
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
        if (!enabled) firebaseAnalytics?.resetAnalyticsData()
    }

    @JvmStatic
    fun init(application: Application) {
        appContext = application.applicationContext
        firebaseAnalytics = if (com.cryonum.BuildConfig.LOCAL_AUDIT) null else FirebaseAnalytics.getInstance(appContext)

        db = Room.databaseBuilder(appContext, AnalyticsDatabase::class.java, "analytics_db")
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .build()

        deviceInfo = collectDeviceInfo(appContext)

        // Initial setup for Firebase based on saved preference
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("analytics_enabled", false)
        setAnalyticsEnabled(enabled)
        if (!com.cryonum.BuildConfig.LOCAL_AUDIT) FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val f = executor.submit {
                    try {
                        saveCrashBlocking(throwable)
                    } catch (ignored: Throwable) {
                    }
                }
                try {
                    f.get(2000, TimeUnit.MILLISECONDS)
                } catch (ignored: Throwable) {
                }
            } catch (ignored: Throwable) {
            } finally {
                previousUncaughtHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    @JvmStatic
    @Synchronized
    fun logEvent(screen: String, action: String, details: String? = null) {
        // Never retain free-form details (expressions, OCR text, URI or PIN) in either build.
        val safeScreen = screen.take(64).filter { it.isLetterOrDigit() || it == '_' }
        val safeAction = action.take(64).filter { it.isLetterOrDigit() || it == '_' }
        val event = UserEvent(timestamp = System.currentTimeMillis(), screen = safeScreen, action = safeAction)
        if (analyticsEnabled) {
            firebaseAnalytics?.logEvent("user_action", Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, safeScreen)
                putString("action", safeAction)
            })
        }
        // No duplicate user-event channel through Crashlytics logs or custom keys.
        executor.execute {
            try {
                db?.analyticsDao()?.apply {
                    insertEvent(event)
                    pruneEvents(System.currentTimeMillis() - RETENTION_MS)
                }
            } catch (_: Exception) { }
        }
    }

    private fun saveCrashBlocking(throwable: Throwable) {
        val recentEvents = try {
            db?.analyticsDao()?.getRecentEvents(35) ?: emptyList()
        } catch (t: Throwable) {
            emptyList()
        }

        val recentEventsJson = toJson(recentEvents)
        val deviceJson = toJson(deviceInfo)

        val sw = StringWriter()
        val stacktrace = (throwable.javaClass.name + "\n" + throwable.stackTrace.take(100).joinToString("\n")).take(16000)

        val crash = CrashReport(
            timestamp = System.currentTimeMillis(),
            deviceInfoJson = deviceJson,
            stacktrace = stacktrace,
            recentEventsJson = recentEventsJson
        )

        try {
            val database = db
            if (database != null) {
                database.analyticsDao().insertCrash(crash)
                database.analyticsDao().pruneCrashes(System.currentTimeMillis() - RETENTION_MS)
            } else {
                writeCrashToFileSync(crash)
            }
        } catch (_: Throwable) {
            writeCrashToFileSync(crash)
        }
    }

    private fun writeCrashToFileSync(crash: CrashReport) {
        try {
            val dir = File(appContext.filesDir, "crash_reports")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(19)?.forEach { it.delete() }
            val file = File(dir, "crash_${crash.timestamp}.json")
            FileWriter(file).use { fw ->
                fw.write(toJson(crash))
                fw.flush()
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun collectDeviceInfo(ctx: Context): DeviceInfo {
        var appVersionName = "Unknown"
        var appVersionCode = 0L
        try {
            val pkgInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (pkgInfo != null) {
                appVersionName = pkgInfo.versionName ?: "Unknown"
                appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    pkgInfo.versionCode.toLong()
                }
            }
        } catch (ignored: Throwable) {
        }

        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val model = Build.MODEL ?: "Unknown"
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val androidSdk = Build.VERSION.SDK_INT
        val totalRam = getTotalRAM()
        val totalStorage = getTotalStorage()

        return DeviceInfo(
            manufacturer, model, androidVersion, totalRam, totalStorage,
            appVersionName, appVersionCode, androidSdk
        )
    }

    private fun getTotalRAM(): Long {
        return try {
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024L * 1024L)
        } catch (_: Throwable) {
            0L
        }
    }

    private fun getTotalStorage(): Long {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val bytes = statFs.blockCountLong * statFs.blockSizeLong
            bytes / (1024L * 1024L)
        } catch (ignored: Throwable) {
            0L
        }
    }

    private fun toJson(obj: Any?): String {
        return try {
            gson.toJson(obj)
        } catch (t: Throwable) {
            "{}"
        }
    }

    @JvmStatic
    fun shutdown() {
        try {
            executor.shutdownNow()
        } catch (ignored: Throwable) {
        }
    }
}
