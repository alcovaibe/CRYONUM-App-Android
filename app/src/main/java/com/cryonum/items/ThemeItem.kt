package com.cryonum.items

import com.cryonum.managers.ThemeManager

data class ThemeItem(
    val nameResId: Int,
    val descriptionResId: Int,
    val theme: ThemeManager.AppTheme
)
