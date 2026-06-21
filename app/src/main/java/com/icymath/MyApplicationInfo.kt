package com.icymath

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.icymath.activity.ActivitySecurity
import com.icymath.analytics.AnalyticsManager
import com.icymath.managers.AppLockObserver
import com.icymath.managers.LocaleManager
import com.icymath.managers.SecurityManager
import com.icymath.managers.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplicationInfo : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        LocaleManager.init(this)
        ThemeManager.init(this)
        AnalyticsManager.init(this)

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(AppLockObserver(this))
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applySecureFlag(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                applySecureFlag(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}

            private fun applySecureFlag(activity: Activity) {
                applicationScope.launch {
                    if (SecurityManager.isAppLockEnabled(activity.applicationContext)) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        // Normally we don't remove it if it was set by activity itself, 
                        // but here we want global control.
                        if (activity !is ActivitySecurity) {
                            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
            }
        })
    }
}
