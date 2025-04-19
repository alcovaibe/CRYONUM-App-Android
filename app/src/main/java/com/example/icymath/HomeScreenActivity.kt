package com.example.icymath

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.icymath.ui.settings.SettingsFragment
import java.io.File


class HomeScreenActivity : AppCompatActivity() {


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.nav_home -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
    class HomeScreenActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.home_screen_activity)
            val btnMenu = findViewById<ImageView>(R.id.menu_icon)
            btnMenu.setOnClickListener {
                val intent = Intent(this, SettingsFragment::class.java)
                startActivity(intent)
            }
        }
    }
    class MainActivityProcessingButtons : AppCompatActivity() {

        private val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
        private val STORAGE_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private val PERMISSION_REQUEST_CODE = 100

        private val REQUEST_IMAGE_CAPTURE = 101
        private val REQUEST_IMAGE_PICK = 102

        // URI для временного хранения фото с камеры
        private var photoUri: Uri? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.home_screen_activity)

            val scanButton = findViewById<Button>(R.id.btn_take_photo)
            scanButton.setOnClickListener {
                startCamera()
            }

            val galleryButton = findViewById<Button>(R.id.btn_recognize_photo)
            galleryButton.setOnClickListener {
                startGallery()
            }

            // Можно сразу спросить разрешения, если нужно
            // checkAndRequestPermissions()
        }

        private fun checkAndRequestPermissions(permissionsNeeded: Array<String>, callback: () -> Unit) {
            val permissionsToRequest = permissionsNeeded.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            } else {
                callback()
            }
        }

        private fun startCamera() {
            checkAndRequestPermissions(arrayOf(CAMERA_PERMISSION)) {
                // Создаем файл для хранения фото
                val photoFile = File.createTempFile("IMG_", ".jpg", cacheDir)
                photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                intent.resolveActivity(packageManager)?.let {
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }

        private fun startGallery() {
            checkAndRequestPermissions(arrayOf(STORAGE_PERMISSION)) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                }
                startActivityForResult(intent, REQUEST_IMAGE_PICK)
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            // Тут можно реализовать обработку после выдачи разрешения, если нужно
        }

        @Deprecated("Используй registerForActivityResult для новых проектов")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    REQUEST_IMAGE_CAPTURE -> {
                        // Пользователь сделал снимок в камере и нажал галочку
                        // photoUri содержит Uri на сделанный снимок
                        // Здесь вставьте вызов обработчика снимков позже
                    }
                    REQUEST_IMAGE_PICK -> {
                        // Пользователь выбрал фото из галереи и нажал галочку
                        val selectedImageUri: Uri? = data?.data
                        // Здесь вставьте вызов обработчика снимков позже
                    }
                }
            }
        }
    }

    class MainActivity : AppCompatActivity() {

        private val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
        private val STORAGE_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private val PERMISSION_REQUEST_CODE = 100

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.home_screen_activity)

            val scanButton = findViewById<Button>(R.id.btn_take_photo)
            scanButton.setOnClickListener {
                checkAndRequestPermissions()
            }

            val galleryButton = findViewById<Button>(R.id.btn_recognize_photo)
            galleryButton.setOnClickListener {
                checkAndRequestPermissions()
            }

            // При запуске приложения сразу запросить все разрешения
            checkAndRequestPermissions()
        }

        private fun checkAndRequestPermissions() {
            val permissionsNeeded = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(CAMERA_PERMISSION)
            }
            if (ContextCompat.checkSelfPermission(this, STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(STORAGE_PERMISSION)
            }
            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            } else {
                // Все разрешения уже есть, можно сразу запускать логику камеры или галереи
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == PERMISSION_REQUEST_CODE) {
                var allGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }
                if (allGranted) {
                    // Все разрешения получены — запускай нужную логику
                } else {
                    // Не все разрешения даны — показать диалог с повторным запросом и выходом
                    AlertDialog.Builder(this)
                        .setTitle("Требуются разрешения")
                        .setMessage("Для корректной работы приложения предоставьте разрешения на камеру и галерею.")
                        .setCancelable(false)
                        .setPositiveButton("Повторить") { _, _ ->
                            // Повторно запросить разрешения
                            checkAndRequestPermissions()
                        }
                        .setNegativeButton("Выйти") { _, _ ->
                            finishAffinity()
                        }
                        .show()
                }
            }
        }
    }
    class MainActivityHistory : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.home_screen_activity)

            val historyButton = findViewById<Button>(R.id.btn_history)
            historyButton.setOnClickListener {
                val intent = Intent(this, ActivityHistory::class.java)
                startActivity(intent)
            }
        }
    }
}