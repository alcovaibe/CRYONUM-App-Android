package com.icymath.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import android.os.Build
import androidx.core.view.WindowCompat
import com.icymath.managers.ThemeManager
import com.icymath.ui.colors.AmoledPalette
import com.icymath.ui.colors.AppColors
import com.icymath.ui.colors.LightPalette
import com.icymath.ui.colors.SandyPalette

val LocalAppColors = staticCompositionLocalOf { LightPalette }

val LocalAppTheme = staticCompositionLocalOf { ThemeManager.AppTheme.LIGHT }

object IcyMathTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
    
    val theme: ThemeManager.AppTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current
}

@Composable
fun IcyMathTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    
    val currentTheme = if (isInPreview) {
        if (isSystemInDarkTheme()) ThemeManager.AppTheme.AMOLED else ThemeManager.AppTheme.LIGHT
    } else {
        ThemeManager.loadTheme(context)
    }
    
    val appColors = when (currentTheme) {
        ThemeManager.AppTheme.LIGHT -> LightPalette
        ThemeManager.AppTheme.SANDY_BROWN -> SandyPalette
        ThemeManager.AppTheme.AMOLED -> AmoledPalette
        ThemeManager.AppTheme.SYSTEM -> if (isSystemInDarkTheme()) AmoledPalette else LightPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }

                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = appColors.isLight
                controller.isAppearanceLightNavigationBars = appColors.isLight
            }
        }
    }

    val colorScheme = if (!appColors.isLight) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            background = appColors.background,
            onBackground = appColors.onBackground,
            surface = appColors.surface,
            onSurface = appColors.onSurface
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            background = appColors.background,
            onBackground = appColors.onBackground,
            surface = appColors.surface,
            onSurface = appColors.onSurface
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTheme provides currentTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
