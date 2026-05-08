package com.icymath.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.ui.theme.IcyMathTheme

@Composable
fun PolicyDialogContent(
    onViewPolicy: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = IcyMathTheme.colors.dialogBackground,
        contentColor = IcyMathTheme.colors.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.policy_update_message),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = IcyMathTheme.colors.onSurface
            )

            Button(
                onClick = onViewPolicy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.view_policy))
            }

            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.accept))
            }

            Button(
                onClick = onDecline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(text = stringResource(R.string.decline))
            }
        }
    }
}
