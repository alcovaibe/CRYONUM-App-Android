package com.icymath.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.managers.ThemeManager
import com.icymath.ui.theme.IcyMathTheme
import com.icymath.ui.theme.LocalAppTheme

@Composable
fun Keyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("C", "0", "backspace")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        text = key,
                        onClick = {
                            when (key) {
                                "backspace" -> onBackspaceClick()
                                "C" -> onClearClick()
                                else -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IcyMathTheme.colors
    
    val containerColor = colors.keyboardKeyBackground
    val contentColor = colors.keyboardKeyText

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, colors.cardStroke.copy(alpha = 0.3f))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when (text) {
                "backspace" -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(24.dp)
                    )
                }
                "C" -> {
                    Text(
                        text = text, // Оставляем "C" как на скриншоте
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    Text(
                        text = text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun KeyboardPreviewLight() {
    IcyMathTheme {
        Surface(color = Color.White) {
            Keyboard({}, {}, {})
        }
    }
}

@Preview(showBackground = true, name = "Amoled Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun KeyboardPreviewAmoled() {
    IcyMathTheme {
        Surface(color = Color.Black) {
            Keyboard({}, {}, {})
        }
    }
}

@Preview(showBackground = true, name = "SandyBrown Theme")
@Composable
fun KeyboardPreviewSandy() {
    // Явно задаем тему SANDY_BROWN через провайдер для превью
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.SANDY_BROWN) {
        Surface(color = Color(0xFFF4A460)) { // Цвет фона SandyBrown
            Keyboard({}, {}, {})
        }
    }
}
