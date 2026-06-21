package com.icymath.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.icymath.items.HistoryItem
import com.icymath.managers.HistoryManager
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.ResultScreenBridge
import com.icymath.utils.SecurityUtils

class ActivityResult : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onResume() {
        super.onResume()
        SecurityUtils.checkLock(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        SecurityUtils.checkLock(this)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val inversions = intent?.getIntExtra("inversions", 0) ?: 0
        val parity = intent?.getStringExtra("parity") ?: ""
        val firstLine = intent?.getStringExtra("firstLine") ?: ""
        val secondLine = intent?.getStringExtra("secondLine") ?: ""

        HistoryManager.addHistoryEntry(
            this,
            HistoryItem(firstLine, secondLine, inversions, parity)
        )

        val composeView = ComposeView(this)
        ResultScreenBridge.setResultContent(
            composeView = composeView,
            inversions = inversions,
            parity = parity,
            onBack = { finish() },
            onHistory = { startActivity(Intent(this, ActivityHistory::class.java)) }
        )

        setContentView(composeView)
    }
}
