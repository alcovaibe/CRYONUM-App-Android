package com.icymath.managers

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.icymath.activity.ActivitySecurity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLockObserver(private val context: Context) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch {
            if (SecurityManager.shouldLock(context)) {
                val intent = Intent(context, ActivitySecurity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("MODE", ActivitySecurity.MODE_UNLOCK)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.IO).launch {
            SecurityManager.setLastBackgroundTime(context, System.currentTimeMillis())
            SecurityManager.setUnlocked(false)
        }
    }
}
