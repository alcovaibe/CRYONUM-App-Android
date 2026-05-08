package com.icymath.items

import com.icymath.managers.ThemeManager

data class ThemeItem(
    val nameResId: Int,
    val descriptionResId: Int,
    val theme: ThemeManager.AppTheme
)
