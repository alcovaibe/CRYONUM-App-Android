package com.icymath.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.icymath.activity.ActivitySecurity
import com.icymath.managers.SecurityManager
import kotlinx.coroutines.launch

object SecurityUtils {
    fun checkLock(activity: AppCompatActivity) {
        if (activity is ActivitySecurity) return // Don't lock the lock screen itself
        
        activity.lifecycleScope.launch {
            if (SecurityManager.shouldLock(activity)) {
                if (SecurityManager.checkAndMarkLocking()) {
                    val intent = Intent(activity, ActivitySecurity::class.java).apply {
                        putExtra("MODE", ActivitySecurity.MODE_UNLOCK)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    activity.startActivity(intent)
                }
            }
        }
    }
}
