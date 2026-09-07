package com.cryonum

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.cryonum.activity.ActivitySecurity
import com.cryonum.managers.AnalyticsManager
import com.cryonum.managers.AppLockObserver
import com.cryonum.managers.LocaleManager
import com.cryonum.managers.SecurityManager
import com.cryonum.managers.ThemeManager
import com.cryonum.content.ContentDependencies
import com.cryonum.utils.SecurityUtils
import androidx.appcompat.app.AppCompatActivity
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
        ContentDependencies.get(this).policyUpdateCoordinator.schedule()

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(AppLockObserver(this))
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applySecureFlag(activity)
                if (activity is AppCompatActivity) SecurityUtils.checkLock(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                AnalyticsManager.logEvent(activity.javaClass.simpleName, "opened")
            }

            override fun onActivityResumed(activity: Activity) {
                applySecureFlag(activity)
                if (activity is AppCompatActivity) SecurityUtils.checkLock(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}

            private fun applySecureFlag(activity: Activity) {
                applicationScope.launch {
                    if (SecurityManager.isAppLockEnabled(activity.applicationContext)) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

                    }
                }
            }
        })
    }
}
