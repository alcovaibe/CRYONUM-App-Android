package com.icymath

import android.app.Application
import com.icymath.analytics.AnalyticsManager
import com.icymath.managers.LocaleManager
import com.icymath.managers.ThemeManager

class MyApplicationInfo : Application() {

    override fun onCreate() {
        super.onCreate()

        LocaleManager.init(this)
        ThemeManager.init(this)
        AnalyticsManager.init(this)
    }
}
