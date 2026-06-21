package com.icymath.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icymath.R
import com.icymath.ui.styles.AppStyles
import com.icymath.ui.theme.IcyMathTheme

@Composable
fun AppBottomNavigation(
    currentRoute: Int,
    onItemSelected: (Int) -> Unit,
    showHome: Boolean = true
) {
    val colors = IcyMathTheme.colors
    
    // Получаем отступ системной навигации
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bottomNavBackground) // Основной фон меню
            .navigationBarsPadding() // Отступ для системной навигации ТЕПЕРЬ ТУТ
    ) {
        NavigationBar(
            containerColor = Color.Transparent, 
            tonalElevation = 0.dp,
            modifier = Modifier.height(56.dp) // Уменьшил с 64 до 56
        ) {
            BottomNavItem(
                id = R.id.nav_reference,
                titleRes = R.string.reference_material,
                iconRes = R.drawable.ic_reference_material,
                isSelected = currentRoute == R.id.nav_reference,
                onClick = { onItemSelected(R.id.nav_reference) }
            )
            BottomNavItem(
                id = R.id.nav_camera,
                titleRes = R.string.photo_camera,
                iconRes = R.drawable.ic_camera,
                isSelected = currentRoute == R.id.nav_camera,
                onClick = { onItemSelected(R.id.nav_camera) }
            )
            
            if (showHome) {
                BottomNavItem(
                    id = R.id.nav_home,
                    titleRes = R.string.to_home,
                    iconRes = R.drawable.ic_home,
                    isSelected = currentRoute == R.id.nav_home,
                    onClick = { onItemSelected(R.id.nav_home) }
                )
            }

            BottomNavItem(
                id = R.id.nav_gallery,
                titleRes = R.string.Gallery,
                iconRes = R.drawable.ic_gallery,
                isSelected = currentRoute == R.id.nav_gallery,
                onClick = { onItemSelected(R.id.nav_gallery) }
            )
            BottomNavItem(
                id = R.id.nav_history,
                titleRes = R.string.history,
                iconRes = R.drawable.ic_history,
                isSelected = currentRoute == R.id.nav_history,
                onClick = { onItemSelected(R.id.nav_history) }
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    id: Int,
    titleRes: Int,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = IcyMathTheme.colors
    
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(titleRes),
                modifier = Modifier.size(AppStyles.BottomNav.IconSizeDp),
                tint = if (isSelected) colors.confirmButtonBackground else colors.titleColor.copy(alpha = 0.6f)
            )
        },
        label = null,
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = colors.confirmButtonBackground,
            unselectedIconColor = colors.titleColor.copy(alpha = 0.6f)
        )
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun AppBottomNavigationPreview() {
    IcyMathTheme {
        AppBottomNavigation(
            currentRoute = R.id.nav_home,
            onItemSelected = {}
        )
    }
}
