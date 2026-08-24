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

class ImagePicker(activity: Activity, private val callback: Callback) {
    
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
            val photoFile = File.createTempFile("IMG_", ".jpg", activity.cacheDir)
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
        val activity = activityRef.get() ?: return
        try {
            val image = InputImage.fromFilePath(activity, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // S-08 Cleanup temp file after successful recognition if it was a camera photo
                    cleanupTempFile(uri)
                    
                    val full = visionText.text
                    val lines = full.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
                    
                    if (lines.size >= 2) {
                        var upperLine = lines[0]
                        var lowerLine = lines[1]
                        if (lowerLine.length > upperLine.length) {
                            val tmp = upperLine
                            upperLine = lowerLine
                            lowerLine = tmp
                        }
                        callback.onResult(upperLine, lowerLine)
                    } else {
                        Toast.makeText(activity, R.string.error_photo_camera, Toast.LENGTH_LONG).show()
                        callback.onResult(null, null)
                    }
                }
                .addOnFailureListener {
                    cleanupTempFile(uri)
                    Toast.makeText(activity, R.string.error_photo_galery, Toast.LENGTH_SHORT).show()
                    callback.onResult(null, null)
                }
        } catch (_: Exception) {
            cleanupTempFile(uri)
            Toast.makeText(activity, R.string.error_photo_galery, Toast.LENGTH_SHORT).show()
            callback.onResult(null, null)
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
