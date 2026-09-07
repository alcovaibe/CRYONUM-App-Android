package com.cryonum.pdf

import android.app.Activity
import android.view.WindowManager

/** Platform screenshot protection. Does not monitor unrelated media or claim root resistance. */
class ScreenProtection(private val activity: Activity) {
    fun start() { activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    fun stop() { /* The window owns the flag for its lifetime. */ }
}
