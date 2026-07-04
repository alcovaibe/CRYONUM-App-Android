package com.icymath.pdf

import android.app.Activity
import android.content.ContentUris
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.icymath.R
import java.util.Locale

class ScreenProtection(private val activity: Activity) {

    // overlay
    private var blackOverlay: FrameLayout? = null

    // media observers
    private var observerThread: HandlerThread? = null
    private var observerHandler: Handler? = null
    private var imagesObserver: ContentObserver? = null
    private var videosObserver: ContentObserver? = null

    // monitoring active recording by MediaStore _ID
    @Volatile
    private var monitoredRecordingId = -1L
    private val monitorLock = Any()

    companion object {
        // tuning
        private const val RECENT_THRESHOLD_SECONDS = 6L // how recent file must be
        private const val MONITOR_POLL_INTERVAL_MS = 1000L // check size every 1s
        private const val STOP_GROWTH_THRESHOLD_MS = 3000L // if no growth for 3s => stop
    }

    fun start() {
        // primary: prevent standard screenshots/recordings
        try {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to add FLAG_SECURE", e)
        }

        // prepare overlay (inserted into activity content)
        try {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            if (root != null && blackOverlay == null) {
                blackOverlay = FrameLayout(activity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.BLACK)
                    visibility = View.GONE
                }
                root.addView(blackOverlay)
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to prepare overlay", e)
        }

        startObservers()
    }

    fun stop() {
        stopObservers()
        removeOverlay()
        // remove FLAG_SECURE? keep it - but if you want to remove:
        // activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun ensureObserverThread() {
        if (observerThread == null || observerThread?.isAlive == false) {
            observerThread = HandlerThread("screen-protection-observer").apply {
                start()
                observerHandler = Handler(looper)
            }
        }
    }

    private fun startObservers() {
        ensureObserverThread()
        val handler = observerHandler ?: return

        imagesObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handleImagesChange()
            }
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                handleImagesChange()
            }
        }

        videosObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handleVideosChange()
            }
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                handleVideosChange()
            }
        }

        try {
            activity.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imagesObserver!!
            )
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to register images observer", e)
        }

        try {
            activity.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videosObserver!!
            )
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to register videos observer", e)
        }
    }

    private fun stopObservers() {
        try {
            imagesObserver?.let {
                activity.contentResolver.unregisterContentObserver(it)
                imagesObserver = null
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to unregister images observer", e)
        }
        try {
            videosObserver?.let {
                activity.contentResolver.unregisterContentObserver(it)
                videosObserver = null
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to unregister videos observer", e)
        }
        try {
            observerThread?.let {
                it.quitSafely()
                observerThread = null
                observerHandler = null
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to stop observer thread", e)
        }
        stopRecordingMonitor()
    }

    private fun removeOverlay() {
        try {
            blackOverlay?.let {
                val parent = it.parent as? ViewGroup
                parent?.removeView(it)
                blackOverlay = null
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Failed to remove overlay", e)
        }
    }

    // Images -> likely screenshot: show toast only
    private fun handleImagesChange() {
        try {
            val now = System.currentTimeMillis() / 1000L
            val threshold = now - RECENT_THRESHOLD_SECONDS

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                "relative_path",
                MediaStore.Images.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Images.Media.DATE_ADDED}>=?"
            val selArgs = arrayOf(threshold.toString())
            val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1"

            activity.contentResolver.query(uri, projection, selection, selArgs, sort)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    var rel: String? = null
                    try {
                        val idx = c.getColumnIndex("relative_path")
                        if (idx != -1) rel = c.getString(idx)
                    } catch (_: Throwable) {}

                    var likely = name?.lowercase(Locale.ROOT)?.contains("screenshot") == true
                    if (!likely && rel?.lowercase(Locale.ROOT)?.contains("screenshot") == true) likely = true

                    if (likely) {
                        // short toast on UI thread
                        activity.runOnUiThread {
                            val t = safeGetString(R.string.screenshot_blocked_toast, "Screenshots are not allowed")
                            Toast.makeText(activity, t, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Error in handleVideosChange", e)
        }
    }

    // Videos -> possible screen recording: start monitoring the newest recent video for growth
    private fun handleVideosChange() {
        try {
            val now = System.currentTimeMillis() / 1000L
            val threshold = now - RECENT_THRESHOLD_SECONDS

            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                "relative_path",
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE
            )
            val selection = "${MediaStore.Video.Media.DATE_ADDED}>=?"
            val selArgs = arrayOf(threshold.toString())
            val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT 1"

            activity.contentResolver.query(uri, projection, selection, selArgs, sort)?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    var rel: String? = null
                    try {
                        val idx = c.getColumnIndex("relative_path")
                        if (idx != -1) rel = c.getString(idx)
                    } catch (_: Throwable) {}

                    if (isLikelyScreenRecording(name, rel)) {
                        startRecordingMonitor(id)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("ScreenProtection", "Error in handleVideosChange", e)
        }
    }

    private fun isLikelyScreenRecording(name: String?, rel: String?): Boolean {
        if (name != null) {
            val low = name.lowercase(Locale.ROOT)
            if (low.contains("screen") || low.contains("record") || low.contains("screencast") || low.contains("screenrecord")) return true
        }
        if (rel != null) {
            val low = rel.lowercase(Locale.ROOT)
            if (low.contains("screen") || low.contains("record") || low.contains("screencast")) return true
        }
        return false
    }

    // Start monitor for size growth; keep overlay visible while file grows
    private fun startRecordingMonitor(id: Long) {
        synchronized(monitorLock) {
            if (monitoredRecordingId == id) return
            stopRecordingMonitorLocked()
            monitoredRecordingId = id

            val lastSize = longArrayOf(-1L)
            val lastChange = longArrayOf(System.currentTimeMillis())

            val monitor = object : Runnable {
                override fun run() {
                    try {
                        if (monitoredRecordingId == -1L) return
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, monitoredRecordingId)
                        val proj = arrayOf(MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DISPLAY_NAME)
                        activity.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                            if (c.moveToFirst()) {
                                var size = -1L
                                try {
                                    size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                                } catch (_: Throwable) {}
                                val now = System.currentTimeMillis()

                                if (lastSize[0] == -1L) {
                                    lastSize[0] = size
                                    lastChange[0] = now
                                    showOverlayWithToast()
                                } else {
                                    if (size > lastSize[0]) {
                                        lastSize[0] = size
                                        lastChange[0] = now
                                        ensureOverlayVisible()
                                    } else {
                                        if (now - lastChange[0] >= STOP_GROWTH_THRESHOLD_MS) {
                                            stopRecordingMonitor()
                                        }
                                    }
                                }
                            } else {
                                stopRecordingMonitor()
                            }
                        } ?: stopRecordingMonitor()
                    } catch (e: SecurityException) {
                        Log.w("ScreenProtection", "Lack of permission to monitor recording: ${e.message}")
                        stopRecordingMonitor()
                    } catch (e: Throwable) {
                        Log.e("ScreenProtection", "Error in recording monitor", e)
                    } finally {
                        synchronized(monitorLock) {
                            if (monitoredRecordingId != -1L && observerHandler != null) {
                                observerHandler?.postDelayed(this, MONITOR_POLL_INTERVAL_MS)
                            }
                        }
                    }
                }
            }

            // start immediate
            observerHandler?.post(monitor)
        }
    }

    private fun stopRecordingMonitor() {
        synchronized(monitorLock) {
            stopRecordingMonitorLocked()
        }
    }

    private fun stopRecordingMonitorLocked() {
        monitoredRecordingId = -1L
        // hide overlay on UI thread
        activity.runOnUiThread {
            try {
                if (blackOverlay?.visibility == View.VISIBLE) {
                    blackOverlay?.visibility = View.GONE
                }
            } catch (e: Throwable) {
                Log.e("ScreenProtection", "Failed to hide overlay", e)
            }
        }
    }

    private fun showOverlayWithToast() {
        activity.runOnUiThread {
            try {
                blackOverlay?.let {
                    it.visibility = View.VISIBLE
                    it.bringToFront()
                }
                val t = safeGetString(R.string.screen_recording_detected_toast, "Screen recording detected — content hidden")
                Toast.makeText(activity, t, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Log.e("ScreenProtection", "Failed to show overlay or toast", e)
            }
        }
    }

    private fun ensureOverlayVisible() {
        activity.runOnUiThread {
            try {
                if (blackOverlay?.visibility != View.VISIBLE) {
                    blackOverlay?.visibility = View.VISIBLE
                    blackOverlay?.bringToFront()
                }
            } catch (e: Throwable) {
                Log.e("ScreenProtection", "Failed to ensure overlay visibility", e)
            }
        }
    }

    private fun safeGetString(resId: Int, fallback: String): String {
        return try {
            activity.getString(resId)
        } catch (_: Throwable) {
            fallback
        }
    }
}
