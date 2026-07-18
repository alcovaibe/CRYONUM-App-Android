package com.icymath.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.icymath.R
import com.icymath.ui.theme.IcyMathTheme

@Composable
fun SubstitutionsMaxValueDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val isError = textValue.isNotEmpty() && (textValue.toIntOrNull() ?: 0) < 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
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
                    ) { /* Consumes click */ },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = IcyMathTheme.colors.dialogBackground,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.enter_maximum),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = IcyMathTheme.colors.titleColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                textValue = newValue
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.Maximum_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(text = stringResource(R.string.Please_enter_a_valid_number))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = IcyMathTheme.colors.inputFieldText,
                            unfocusedTextColor = IcyMathTheme.colors.inputFieldText,
                            focusedBorderColor = IcyMathTheme.colors.confirmButtonBackground,
                            unfocusedBorderColor = IcyMathTheme.colors.cardStroke,
                            focusedLabelColor = IcyMathTheme.colors.confirmButtonBackground,
                            unfocusedLabelColor = IcyMathTheme.colors.inputFieldText.copy(alpha = 0.6f)
                        )
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
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = {
                                val value = textValue.toIntOrNull()
                                if (value != null && value >= 1) {
                                    onConfirm(value)
                                }
                            },
                            enabled = textValue.isNotEmpty() && !isError
                        ) {
                            Text(
                                text = stringResource(R.string.ok),
                                color = if (textValue.isNotEmpty() && !isError) IcyMathTheme.colors.confirmButtonBackground else IcyMathTheme.colors.inputFieldText.copy(alpha = 0.38f),
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

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun SubstitutionsMaxValueDialogPreview() {
    IcyMathTheme {
        SubstitutionsMaxValueDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
