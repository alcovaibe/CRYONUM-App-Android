package com.icymath.ui.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.managers.ThemeManager
import com.icymath.ui.menu.AppBottomNavigation
import com.icymath.ui.menu.AppDrawer
import com.icymath.ui.components.Keyboard
import com.icymath.ui.components.dialogs.SubstitutionsInputMethodDialog
import com.icymath.ui.components.dialogs.SubstitutionsMaxValueDialog
import com.icymath.ui.styles.AppStyles
import com.icymath.ui.theme.IcyMathTheme
import com.icymath.ui.theme.LocalAppTheme
import com.icymath.utils.InputFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionsScreen(
    upperLine: String,
    lowerLine: String,
    onUpperChange: (String) -> Unit,
    onLowerChange: (String) -> Unit,
    onMenuAction: (Int) -> Unit,
    onConfirmClick: () -> Unit,
    onInputBoxClick: (Boolean) -> Unit,
    onGenerateLine: (Int) -> Unit,
    isFirstSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = IcyMathTheme.colors

    var showInputMethodDialog by remember { mutableStateOf(false) }
    var showMaxValueDialog by remember { mutableStateOf(false) }

    if (showInputMethodDialog) {
        SubstitutionsInputMethodDialog(
            onManualEntry = { showInputMethodDialog = false },
            onMaxValueEntry = {
                showInputMethodDialog = false
                showMaxValueDialog = true
            },
            onDismiss = { showInputMethodDialog = false }
        )
    }

    if (showMaxValueDialog) {
        SubstitutionsMaxValueDialog(
            onConfirm = { maxValue ->
                showMaxValueDialog = false
                onGenerateLine(maxValue)
            },
            onDismiss = { showMaxValueDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onItemSelected = { id ->
                    scope.launch { drawerState.close() }
                    onMenuAction(id)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0), // Отключаем автоматические инсеты, управляем вручную
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = stringResource(R.string.EnterPrimer).uppercase(),
                            style = AppStyles.CardTitleStyle.copy(fontSize = 24.sp),
                            color = colors.titleColor
                        )
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = colors.titleColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                AppBottomNavigation(
                    currentRoute = R.id.nav_home,
                    onItemSelected = { id ->
                        onMenuAction(id)
                    },
                    showHome = false
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.First_string),
                    color = colors.titleColor,
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = AppStyles.CardSubtitleStyle
                )
                InputBox(upperLine, isFirstSelected, onClick = { 
                    onSelectionChange(true)
                    if (upperLine.isEmpty()) {
                        showInputMethodDialog = true
                    }
                    onInputBoxClick(true)
                }, hint = stringResource(R.string.string_one))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.SecondString),
                    color = colors.titleColor,
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = AppStyles.CardSubtitleStyle
                )
                InputBox(lowerLine, !isFirstSelected, onClick = { 
                    onSelectionChange(false)
                    onInputBoxClick(false)
                }, hint = stringResource(R.string.string_one))

                Spacer(modifier = Modifier.height(32.dp))

                Keyboard(
                    onNumberClick = { digit ->
                        if (isFirstSelected) onUpperChange(upperLine + digit)
                        else onLowerChange(lowerLine + digit)
                    },
                    onBackspaceClick = {
                        if (isFirstSelected) {
                            if (upperLine.isNotEmpty()) onUpperChange(upperLine.dropLast(1))
                        } else {
                            if (lowerLine.isNotEmpty()) onLowerChange(lowerLine.dropLast(1))
                        }
                    },
                    onClearClick = {
                        onUpperChange("")
                        onLowerChange("")
                    },
                    modifier = Modifier.width(280.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier.width(280.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.confirmButtonBackground,
                        contentColor = colors.confirmButtonText
                    ),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Text(
                        stringResource(R.string.Confirm).uppercase(),
                        style = AppStyles.CardTitleStyle.copy(fontSize = 16.sp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun InputBox(text: String, isSelected: Boolean, onClick: () -> Unit, hint: String) {
    val colors = IcyMathTheme.colors
    Surface(
        modifier = Modifier.width(280.dp).height(52.dp).clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = colors.inputFieldBackground,
        border = if (isSelected) BorderStroke(2.dp, colors.confirmButtonBackground) else BorderStroke(1.dp, colors.cardStroke)
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = text.ifEmpty { hint },
                color = if (text.isEmpty()) colors.inputFieldText.copy(alpha = 0.5f) else colors.inputFieldText,
                fontSize = 16.sp,
                fontWeight = if (text.isEmpty()) FontWeight.Normal else FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode", locale = "ru")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", locale = "ru")
@Composable
fun SubstitutionsScreenPreview() {
    IcyMathTheme {
        SubstitutionsScreen(
            upperLine = "12345",
            lowerLine = "54321",
            onUpperChange = {},
            onLowerChange = {},
            onMenuAction = {},
            onConfirmClick = {},
            onInputBoxClick = {},
            onGenerateLine = {},
            isFirstSelected = true,
            onSelectionChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Sandy Brown", locale = "ru")
@Composable
fun SubstitutionsScreenSandyPreview() {
    IcyMathTheme {
        CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.SANDY_BROWN) {
            SubstitutionsScreen(
                upperLine = "12345",
                lowerLine = "54321",
                onUpperChange = {},
                onLowerChange = {},
                onMenuAction = {},
                onConfirmClick = {},
                onInputBoxClick = {},
                onGenerateLine = {},
                isFirstSelected = true,
                onSelectionChange = {}
            )
        }
    }
}

object SubstitutionsScreenBridge {
    /**
     * Bridge for calling Compose screen from Activity.
     */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        upperLineState: MutableState<String>,
        lowerLineState: MutableState<String>,
        isFirstSelectedState: MutableState<Boolean>,
        onMenuAction: (Int) -> Unit,
        onConfirmClick: () -> Unit,
        onInputBoxClick: (Boolean) -> Unit,
        onGenerateLine: (Int) -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                SubstitutionsScreen(
                    upperLine = upperLineState.value,
                    lowerLine = lowerLineState.value,
                    onUpperChange = { upperLineState.value = InputFilter.filterOnlyDigits(it) },
                    onLowerChange = { lowerLineState.value = InputFilter.filterOnlyDigits(it) },
                    onMenuAction = onMenuAction,
                    onConfirmClick = onConfirmClick,
                    onInputBoxClick = onInputBoxClick,
                    onGenerateLine = onGenerateLine,
                    isFirstSelected = isFirstSelectedState.value,
                    onSelectionChange = { isFirstSelectedState.value = it }
                )
            }
        }
    }
}
