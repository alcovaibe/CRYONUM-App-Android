package com.cryonum.managers

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.cryonum.R
import java.io.File
import java.lang.ref.WeakReference
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class ImagePicker(activity: Activity, private val callback: Callback) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val processing = AtomicBoolean(false)
    private val activityRef = WeakReference(activity)

    interface Callback {
        fun onResult(upperLine: String?, lowerLine: String?)
    }

    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    
    private var photoUri: Uri? = null
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun registerLaunchers(
        requestCameraPermissionLauncher: ActivityResultLauncher<String>?,
        cameraLauncher: ActivityResultLauncher<Intent>?,
        galleryLauncher: ActivityResultLauncher<Intent>?
    ) {
        this.requestCameraPermissionLauncher = requestCameraPermissionLauncher
        this.cameraLauncher = cameraLauncher
        this.galleryLauncher = galleryLauncher
    }

    fun clear() {
        scope.cancel()
        photoUri?.let(::cleanupTempFile)
        requestCameraPermissionLauncher = null
        cameraLauncher = null
        galleryLauncher = null
        activityRef.clear()
        recognizer.close()
    }

    fun requestCamera() {
        val activity = activityRef.get() ?: return
        if (isCameraPermissionGranted) {
            startCamera()
            return
        }
        val cameraPerm = Manifest.permission.CAMERA
        requestCameraPermissionLauncher?.launch(cameraPerm)
        ?: run {
            ActivityCompat.requestPermissions(activity, arrayOf(cameraPerm), FALLBACK_CAMERA_REQUEST_CODE)
            Toast.makeText(activity, activity.getString(R.string.permission_explanation), Toast.LENGTH_LONG).show()
            Log.w(TAG, "requestCamera: no permission-launcher registered")
        }
    }

    fun startCamera() {
        val activity = activityRef.get() ?: return
        try {
            val photoFile = File.createTempFile("IMG_", ".jpg", File(activity.cacheDir, "camera").apply { mkdirs() })
            val authority = "${activity.packageName}.file_provider"
            photoUri = FileProvider.getUriForFile(activity, authority, photoFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            cameraLauncher?.launch(intent)
            ?: run {
                activity.startActivity(intent)
                Toast.makeText(activity, activity.getString(R.string.permission_explanation), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startCamera error", e)
            Toast.makeText(activity, activity.getString(R.string.error_photo_camera), Toast.LENGTH_SHORT).show()
        }
    }

    fun startGallery() {
        val activity = activityRef.get() ?: return
        val intent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        }
        galleryLauncher?.launch(intent)
        ?: run {
            activity.startActivity(intent)
            Toast.makeText(activity, activity.getString(R.string.permission_explanation), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraResult() {
        val activity = activityRef.get() ?: return
        val uri = photoUri
        if (uri != null) {
            recognizeText(uri)
        } else {
            Toast.makeText(activity, activity.getString(R.string.error_photo_camera), Toast.LENGTH_SHORT).show()
            callback.onResult(null, null)
        }
    }

    fun cancelCamera() { photoUri?.let(::cleanupTempFile) }

    fun handleGalleryResult(data: Intent?) {
        val activity = activityRef.get() ?: return
        val uri = data?.data
        if (uri != null) {
            try {
                val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (takeFlags != 0) {
                    activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: Exception) {
                Log.w(TAG, "takePersistableUriPermission error: ${e.message}")
            }
            recognizeText(uri)
        } else {
            Toast.makeText(activity, activity.getString(R.string.error_photo_galery), Toast.LENGTH_SHORT).show()
            callback.onResult(null, null)
        }
    }

    fun handleRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val activity = activityRef.get() ?: return
        if (requestCode != FALLBACK_CAMERA_REQUEST_CODE) return
        var granted = grantResults.isNotEmpty()
        for (r in grantResults) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                granted = false
                break
            }
        }
        if (!granted) {
            Toast.makeText(activity, activity.getString(R.string.permission_explanation), Toast.LENGTH_LONG).show()
            return
        }
        val cameraRequested = permissions.any { it == Manifest.permission.CAMERA }
        if (cameraRequested) {
            startCamera()
        } else {
            startGallery()
        }
    }

    private fun recognizeText(uri: Uri) {
        if (!processing.compareAndSet(false, true)) return
        val context = activityRef.get()?.applicationContext ?: run { processing.set(false); return }
        scope.launch {
            var bitmap: Bitmap? = null
            var handedOff = false
            try {
                require(uri.scheme == "content")
                withContext(Dispatchers.IO) {
                    val file = File.createTempFile("ocr_", ".image", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(32 * 1024); var total = 0L
                                while (true) {
                                    ensureActive()
                                    val count = input.read(buffer); if (count < 0) break
                                    total += count; require(total <= 20L * 1024 * 1024)
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.path, bounds)
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                        val decoded = requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }))
                        bitmap = decoded
                        val orientation = ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        val matrix = Matrix().apply {
                            when (orientation) {
                                2 -> setScale(-1f, 1f)
                                3 -> setRotate(180f)
                                4 -> setScale(1f, -1f)
                                5 -> { setRotate(90f); postScale(-1f, 1f) }
                                6 -> setRotate(90f)
                                7 -> { setRotate(270f); postScale(-1f, 1f) }
                                8 -> setRotate(270f)
                            }
                        }
                        if (!matrix.isIdentity) {
                            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                            bitmap = oriented
                            if (oriented !== decoded) decoded.recycle()
                        }
                    } finally { file.delete() }
                }
                val owned = requireNotNull(bitmap)
                val task = recognizer.process(InputImage.fromBitmap(owned, 0))
                handedOff = true
                bitmap = null // ML Kit owns its lifetime until the completion callback.
                task.addOnSuccessListener { visionText ->
                    if (activityRef.get()?.isFinishing == false) {
                        val lines = visionText.text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                        if (lines.size == 2 && lines.all { it.length <= 4096 }) callback.onResult(lines[0], lines[1])
                        else callback.onResult(null, null)
                    }
                }.addOnFailureListener {
                    if (activityRef.get()?.isFinishing == false) callback.onResult(null, null)
                }.addOnCompleteListener { owned.recycle(); processing.set(false); cleanupTempFile(uri) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { callback.onResult(null, null) }
            finally {
                bitmap?.recycle()
                if (!handedOff) processing.set(false)
                cleanupTempFile(uri)
            }
        }
    }

    private fun cleanupTempFile(uri: Uri) {
        // Only delete if it's our own temp file from camera (photoUri matches)
        if (uri == photoUri) {
            try {
                activityRef.get()?.contentResolver?.delete(uri, null, null)
                photoUri = null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup temp file: ${e.message}")
            }
        }
    }

    val isCameraPermissionGranted: Boolean
        get() = activityRef.get()?.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "ImagePicker"
        const val FALLBACK_CAMERA_REQUEST_CODE = 1001
    }
}
