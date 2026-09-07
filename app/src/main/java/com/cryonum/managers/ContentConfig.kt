package com.cryonum.managers

import android.content.Context

object ContentConfig {
    // Partner content is identical on real devices and emulators, from the first launch.
    fun isExtraContentEnabled(context: Context): Boolean = true
}
