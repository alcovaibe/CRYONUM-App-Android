package com.cryonum.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.cryonum.R
import com.cryonum.items.HistoryItem
import com.cryonum.managers.HistoryManager
import com.cryonum.managers.ImagePicker
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.SecurityManager
import com.cryonum.managers.SystemUiManager
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.activity.HistoryScreenBridge
import com.cryonum.utils.SecurityUtils
import kotlinx.coroutines.launch

class ActivityHistory : AppCompatActivity() {
    private var imagePicker: ImagePicker? = null

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        SystemUiManager.applyEdgeToEdge(this)
        super.onCreate(savedInstanceState)
        
        SecurityUtils.checkLock(this)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        imagePicker = ImagePicker(this, object : ImagePicker.Callback {
            override fun onResult(upperLine: String?, lowerLine: String?) {
                if (!upperLine.isNullOrEmpty() && !lowerLine.isNullOrEmpty()) {
                    val intent = Intent(this@ActivityHistory, ActivitySubstitutions::class.java).apply {
                        putExtra("upperLine", upperLine)
                        putExtra("lowerLine", lowerLine)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ActivityHistory, getString(R.string.error_photo_camera), Toast.LENGTH_LONG).show()
                }
            }
        })

        registerLaunchersAndBindToImagePicker()

        val composeView = ComposeView(this)
        val historyList = androidx.compose.runtime.mutableStateListOf<HistoryItem>().apply {
            addAll(HistoryManager.loadHistory(this@ActivityHistory))
        }

        HistoryScreenBridge.setHistoryContent(
            composeView = composeView,
            onBack = { finish() },
            historyItems = historyList,
            onItemClick = { item -> openItem(item) },
            onDeleteSelected = { selectedItems ->
                selectedItems.forEach { 
                    HistoryManager.deleteHistoryEntry(this, it)
                    historyList.remove(it)
                }
            },
            onMenuAction = { id ->
                when (id) {
                    R.id.nav_reference -> {
                        startActivity(Intent(this, ActivityReferenceMaterial::class.java))
                        finish()
                    }
                    R.id.nav_camera -> imagePicker?.requestCamera()
                    R.id.nav_home -> {
                        startActivity(Intent(this, ActivitySubstitutions::class.java))
                        finish()
                    }
                    R.id.nav_gallery -> imagePicker?.startGallery()
                    R.id.nav_history -> { /* Мы уже здесь */ }
                }
            }
        )

        setContentView(composeView)
    }

    override fun onResume() {
        super.onResume()
        SecurityUtils.checkLock(this)
    }

    private fun openItem(item: HistoryItem) {
        if (item.type == HistoryItem.HistoryType.SUBSTITUTION) {
            val intent = Intent(this, ActivitySubstitutions::class.java).apply {
                putExtra("upperLine", item.topRow)
                putExtra("lowerLine", item.bottomRow)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, ActivityCalculator::class.java).apply {
                putExtra("expression", item.expression)
                putExtra("result", item.result)
            }
            startActivity(intent)
        }
    }

    private fun registerLaunchersAndBindToImagePicker() {
        val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                imagePicker?.startCamera()
            } else {
                Toast.makeText(this, getString(R.string.permission_explanation), Toast.LENGTH_LONG).show()
            }
        }

        val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePicker?.handleCameraResult()
            }
        }

        val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePicker?.handleGalleryResult(result.data)
            }
        }

        imagePicker?.registerLaunchers(requestCameraPermissionLauncher, cameraLauncher, galleryLauncher)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        imagePicker?.handleRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        imagePicker?.clear()
        imagePicker = null
        super.onDestroy()
    }
}
