package com.cryonum.utils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cryonum.activity.ActivitySecurity
import com.cryonum.managers.SecurityManager
import kotlinx.coroutines.launch

object SecurityUtils {
    fun checkLock(activity: AppCompatActivity) {
        if (activity is ActivitySecurity && activity.intent.getIntExtra("MODE", ActivitySecurity.MODE_SETTINGS) == ActivitySecurity.MODE_UNLOCK) return
        
        activity.window.decorView.visibility = android.view.View.INVISIBLE
        activity.lifecycleScope.launch {
            if (SecurityManager.shouldLock(activity)) {
                if (SecurityManager.checkAndMarkLocking()) {
                    val intent = Intent(activity, ActivitySecurity::class.java).apply {
                        putExtra("MODE", ActivitySecurity.MODE_UNLOCK)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    activity.startActivity(intent)
                }
            } else {
                activity.window.decorView.visibility = android.view.View.VISIBLE
            }
        }
    }
}
