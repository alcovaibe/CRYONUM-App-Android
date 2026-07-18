package com.icymath.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.icymath.R
import com.icymath.ui.theme.IcyMathTheme

@Composable
fun SubstitutionsInputMethodDialog(
    onManualEntry: () -> Unit,
    onMaxValueEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false // Позволяет диалогу заходить под системные бары для лучшего прилегания
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Контейнер поглощает клики, чтобы не закрывать диалог при нажатии на него */ },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // Закругления только сверху
                color = IcyMathTheme.colors.dialogBackground,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding() // Учитываем системную панель навигации
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.enter_quistion),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = IcyMathTheme.colors.titleColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    SelectionCard(
                        text = stringResource(R.string.enter_the_string_manually_2),
                        onClick = onManualEntry
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectionCard(
                        text = stringResource(R.string.enter_maximum_2),
                        onClick = onMaxValueEntry
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = IcyMathTheme.colors.confirmButtonBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(text: String, onClick: () -> Unit) {
    val colors = IcyMathTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.cardStroke.copy(alpha = 0.5f)),
        color = colors.surface.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inputFieldText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun SubstitutionsInputMethodDialogPreview() {
    IcyMathTheme {
        SubstitutionsInputMethodDialog(
            onManualEntry = {},
            onMaxValueEntry = {},
            onDismiss = {}
        )
    }
}
