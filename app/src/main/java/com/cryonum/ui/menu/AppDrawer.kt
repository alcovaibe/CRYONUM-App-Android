package com.cryonum.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.managers.ThemeManager
import com.cryonum.ui.theme.IcyMathTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.cryonum.ui.theme.LocalAppTheme

@Composable
fun AppDrawer(
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IcyMathTheme.colors
    
    ModalDrawerSheet(
        modifier = modifier.width(290.dp).fillMaxHeight(),
        drawerContainerColor = colors.drawerBackground,
        drawerShape = RoundedCornerShape(0.dp),
        windowInsets = WindowInsets(0) // Убираем автоматические инсеты
    ) {
        DrawerHeader()
        HorizontalDivider(color = colors.cardStroke.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        
        val menuItems = remember {
            listOf(
                MenuItem(R.id.nav_settings, R.string.settings, R.drawable.ic_settings, isVisible = true),
                MenuItem(R.id.nav_help_center, R.string.help_center, R.drawable.ic_help, isVisible = true),
                MenuItem(R.id.partners, R.string.partners, R.drawable.ic_partners, isVisible = true),
                MenuItem(R.id.about, R.string.about_app, R.drawable.ic_about, isVisible = true),
                MenuItem(R.id.nav_schedule, R.string.schedule, R.drawable.ic_schedule, isVisible = true),
                MenuItem(R.id.nav_calculator, R.string.calculator_title, R.drawable.ic_delete, isVisible = true)
            )
        }

        menuItems.filter { it.isVisible }.forEach { item ->
            DrawerItem(
                titleRes = item.titleRes,
                icon = painterResource(item.iconRes),
                contentColor = colors.titleColor,
                onClick = { onItemSelected(item.id) }
            )
        }
        
        // Заполняем пространство под системной навигацией в Drawer
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

private data class MenuItem(
    val id: Int,
    val titleRes: Int,
    val iconRes: Int,
    val isVisible: Boolean
)

@Composable
private fun DrawerHeader() {
    val colors = IcyMathTheme.colors
    val headerBgColor = colors.headerColor
    val textColor = colors.titleColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBgColor)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun DrawerItem(
    titleRes: Int,
    icon: Painter,
    contentColor: Color,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { 
            Text(
                text = stringResource(titleRes),
                color = contentColor,
                fontSize = 16.sp
            ) 
        },
        selected = false,
        onClick = onClick,
        icon = { 
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            ) 
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Preview(name = "Light Mode - RU", locale = "ru")
@Preview(name = "Amoled Mode - EN", locale = "en")
@Preview(name = "Sandy Mode - DE", locale = "de")
@Composable
fun AppDrawerPreview() {
    IcyMathTheme {
        AppDrawer(onItemSelected = {})
    }
}

@Preview(name = "Amoled Mode - RU", locale = "ru")
@Composable
fun AppDrawerAmoledPreview() {
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.AMOLED) {
        IcyMathTheme {
            AppDrawer(onItemSelected = {})
        }
    }
}

@Preview(name = "Sandy Mode - RU", locale = "ru")
@Composable
fun AppDrawerSandyPreview() {
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.SANDY_BROWN) {
        IcyMathTheme {
            AppDrawer(onItemSelected = {})
        }
    }
}
