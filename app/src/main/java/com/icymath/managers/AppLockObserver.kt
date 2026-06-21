package com.icymath.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.icymath.activity.ActivitySecurity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLockObserver(context: Context) : DefaultLifecycleObserver {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            if (SecurityManager.shouldLock(appContext)) {
                val intent = Intent(appContext, ActivitySecurity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("MODE", ActivitySecurity.MODE_UNLOCK)
                }
                appContext.startActivity(intent)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            try {
                delay(100)
                SecurityManager.setLastBackgroundTime(appContext, System.currentTimeMillis())
                SecurityManager.setUnlocked(false)
            } catch (e: Exception) {
                Log.e("AppLockObserver", "Error in onStop handler", e)
            }
        }
    }
}
