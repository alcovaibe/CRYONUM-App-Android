package com.icymath.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.ui.theme.IcyMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    input: String,
    result: String,
    isInverted: Boolean,
    isRadians: Boolean,
    onBackClick: () -> Unit,
    onToggleOrientation: () -> Unit,
    onKeyClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = IcyMathTheme.colors

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

        // Keyboard
        val keys = listOf(
            stringResource(R.string.Clean), stringResource(R.string.symbol_percent), stringResource(R.string.symbol_power2),
            "9", "8", "7",
            "6", "5", "4",
            "3", "2", "1",
            "(", "0", ")",
            "+", "-", "/",
            "*", "|", "="
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(keys) { key ->
                CalcButton(
                    text = key,
                    onClick = { onKeyClick(key) },
                    containerColor = if (key == "=") colors.confirmButtonBackground else colors.keyboardKeyBackground,
                    contentColor = if (key == "=") colors.confirmButtonText else colors.keyboardKeyText
                )
            }
        }
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

        // Fixed Keyboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: 0-9
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..9).forEach { num ->
                    CalcButton(text = num.toString(), onClick = { onKeyClick(num.toString()) }, modifier = Modifier.weight(1f))
                }
            }

            // Row 2: %, x^2, +, -, *, /, (, ), ,, pi
            val row2 = listOf(
                stringResource(R.string.symbol_percent), stringResource(R.string.symbol_power2),
                "+", "-", "*", "/", "(", ")", ",", "π"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row2.forEach { key ->
                    CalcButton(text = key, onClick = { onKeyClick(key) }, modifier = Modifier.weight(1f))
                }
            }

            // Row 3: e, !, n√, ln, log, sin, cos, tan, cot, | |
            val row3 = listOf(
                "e", "!", "ⁿ√", "ln", "log", 
                if (isInverted) "asin" else "sin",
                if (isInverted) "acos" else "cos",
                if (isInverted) "atan" else "tan",
                if (isInverted) "acot" else "cot",
                "|"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row3.forEach { key ->
                    CalcButton(text = key, onClick = { onKeyClick(key) }, modifier = Modifier.weight(1f))
                }
            }

            // Row 4: Menu, inv, deg/rad, MC, M+, M-, MR, C, =
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                CalcButton(text = "⋮", onClick = { onKeyClick("menu") }, modifier = Modifier.weight(1f))
                CalcButton(text = stringResource(R.string.btn_inv), onClick = { onKeyClick("inv") }, modifier = Modifier.weight(1f), containerColor = if(isInverted) colors.confirmButtonBackground else colors.keyboardKeyBackground)
                CalcButton(text = if(isRadians) "Rad" else "Deg", onClick = { onKeyClick("deg_rad") }, modifier = Modifier.weight(1f))
                CalcButton(text = "MC", onClick = { onKeyClick("MC") }, modifier = Modifier.weight(1f))
                CalcButton(text = "M+", onClick = { onKeyClick("M+") }, modifier = Modifier.weight(1f))
                CalcButton(text = "M-", onClick = { onKeyClick("M-") }, modifier = Modifier.weight(1f))
                CalcButton(text = "MR", onClick = { onKeyClick("MR") }, modifier = Modifier.weight(1f))
                CalcButton(text = "C", onClick = { onKeyClick("C") }, modifier = Modifier.weight(1f))
                CalcButton(text = "=", onClick = { onKeyClick("=") }, modifier = Modifier.weight(2f), containerColor = colors.confirmButtonBackground, contentColor = colors.confirmButtonText)
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = IcyMathTheme.colors.keyboardKeyBackground,
    contentColor: Color = IcyMathTheme.colors.keyboardKeyText
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
        onKeyClick: (String) -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                CalculatorScreen(input, result, isInverted, isRadians, onBackClick, onToggleOrientation, onKeyClick)
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
