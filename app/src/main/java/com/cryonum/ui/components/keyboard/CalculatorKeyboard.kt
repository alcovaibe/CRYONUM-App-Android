package com.cryonum.ui.components.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.ui.theme.IcyMathTheme

@Composable
fun CalculatorPortraitKeyboard(
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IcyMathTheme.colors

    // Keyboard keys (exact same list from CalculatorScreen)
    val keys = listOf(
        stringResource(R.string.Clean), stringResource(R.string.symbol_percent), stringResource(R.string.symbol_power2),
        "9", "8", "7",
        "6", "5", "4",
        "3", "2", "1",
        "(", "0", ")",
        "+", "-", ":",
        stringResource(R.string.symbol_multiplication), stringResource(R.string.module), "="
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
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

@Composable
fun CalculatorLandscapeKeyboard(
    isInverted: Boolean,
    isRadians: Boolean,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IcyMathTheme.colors

    Column(
        modifier = modifier
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

        // Row 2: %, x^2, +, -, ×, :, (, ), ,, pi
        val row2 = listOf(
            stringResource(R.string.symbol_percent), stringResource(R.string.symbol_power2),
            "+", "-", stringResource(R.string.symbol_multiplication), ":", "(", ")", ",", "π"
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
            stringResource(R.string.module)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row3.forEach { key ->
                CalcButton(text = key, onClick = { onKeyClick(key) }, modifier = Modifier.weight(1f))
            }
        }

        // Row 4: Menu, inv, deg/rad, MC, M+, M-, MR, C, =
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            CalcButton(text = "⋮", onClick = { onKeyClick("menu") }, modifier = Modifier.weight(1f))
            CalcButton(
                text = stringResource(R.string.btn_inv),
                onClick = { onKeyClick("inv") },
                modifier = Modifier.weight(1f),
                containerColor = if (isInverted) colors.confirmButtonBackground else colors.keyboardKeyBackground,
                contentColor = if (isInverted) colors.confirmButtonText else colors.keyboardKeyText
            )
            CalcButton(
                text = if (isRadians) "Rad" else "Deg",
                onClick = { onKeyClick("deg_rad") },
                modifier = Modifier.weight(1f),
                containerColor = if (isRadians) colors.confirmButtonBackground else colors.keyboardKeyBackground,
                contentColor = if (isRadians) colors.confirmButtonText else colors.keyboardKeyText
            )
            CalcButton(text = "MC", onClick = { onKeyClick("MC") }, modifier = Modifier.weight(1f))
            CalcButton(text = "M+", onClick = { onKeyClick("M+") }, modifier = Modifier.weight(1f))
            CalcButton(text = "M-", onClick = { onKeyClick("M-") }, modifier = Modifier.weight(1f))
            CalcButton(text = "MR", onClick = { onKeyClick("MR") }, modifier = Modifier.weight(1f))
            CalcButton(text = "C", onClick = { onKeyClick("C") }, modifier = Modifier.weight(1f))
            CalcButton(text = "=", onClick = { onKeyClick("=") }, modifier = Modifier.weight(2f), containerColor = colors.confirmButtonBackground, contentColor = colors.confirmButtonText)
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
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}
