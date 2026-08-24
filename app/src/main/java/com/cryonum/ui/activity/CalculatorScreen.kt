package com.cryonum.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.managers.PolicyManager
import com.cryonum.ui.components.keyboard.CalculatorLandscapeKeyboard
import com.cryonum.ui.components.keyboard.CalculatorPortraitKeyboard
import com.cryonum.ui.theme.IcyMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    input: String,
    result: String,
    isInverted: Boolean,
    isRadians: Boolean,
    onBackClick: () -> Unit,
    onToggleOrientation: () -> Unit,
    onKeyClick: (String) -> Unit,
    onLaunchPolicyViewer: (Boolean) -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = IcyMathTheme.colors

    PolicyManager.PolicyDialogHandler(
        onLaunchViewer = onLaunchPolicyViewer,
        onExitApp = onExitApp
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = colors.titleColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleOrientation) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_expand),
                            contentDescription = stringResource(R.string.expand),
                            tint = colors.titleColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            CalculatorLandscape(
                modifier = Modifier.padding(padding),
                input = input,
                result = result,
                isInverted = isInverted,
                isRadians = isRadians,
                onKeyClick = onKeyClick
            )
        } else {
            CalculatorPortrait(
                modifier = Modifier.padding(padding),
                input = input,
                result = result,
                onKeyClick = onKeyClick
            )
        }
    }
}

@Composable
private fun CalculatorPortrait(
    modifier: Modifier = Modifier,
    input: String,
    result: String,
    onKeyClick: (String) -> Unit
) {
    val colors = IcyMathTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (input.isEmpty()) stringResource(R.string.zero) else input,
                fontSize = 32.sp,
                color = colors.titleColor,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                fontSize = 28.sp,
                color = colors.titleColor.copy(alpha = 0.7f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CalculatorPortraitKeyboard(onKeyClick = onKeyClick)
    }
}

@Composable
private fun CalculatorLandscape(
    modifier: Modifier = Modifier,
    input: String,
    result: String,
    isInverted: Boolean,
    isRadians: Boolean,
    onKeyClick: (String) -> Unit
) {
    val colors = IcyMathTheme.colors

    Column(modifier = modifier.fillMaxSize()) {
        // Display Area (Scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (input.isEmpty()) stringResource(R.string.input_display_label) else input,
                    fontSize = 28.sp,
                    color = colors.titleColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = result,
                    fontSize = 24.sp,
                    color = colors.titleColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        CalculatorLandscapeKeyboard(
            isInverted = isInverted,
            isRadians = isRadians,
            onKeyClick = onKeyClick
        )
    }
}

object CalculatorScreenBridge {
    @JvmStatic
    fun setCalculatorContent(
        composeView: ComposeView,
        input: String,
        result: String,
        isInverted: Boolean,
        isRadians: Boolean,
        onBackClick: () -> Unit,
        onToggleOrientation: () -> Unit,
        onKeyClick: (String) -> Unit,
        onLaunchPolicyViewer: (Boolean) -> Unit,
        onExitApp: () -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                CalculatorScreen(
                    input,
                    result,
                    isInverted,
                    isRadians,
                    onBackClick,
                    onToggleOrientation,
                    onKeyClick,
                    onLaunchPolicyViewer,
                    onExitApp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun CalculatorPortraitPreview() {
    IcyMathTheme {
        Surface(color = Color.White) {
            CalculatorScreen("2+2", "4", false, true, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
fun CalculatorLandscapePreview() {
    IcyMathTheme {
        Surface(color = Color.White) {
            CalculatorScreen("sin(45)", "0.707", false, true, {}, {}, {})
        }
    }
}
