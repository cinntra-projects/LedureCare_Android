package com.example.ledurecare.feature.auth.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.example.ledurecare.R
import com.example.ledurecare.ui.theme.Green600

/** Cannot be dismissed: the only way on is to log in again. */
@Composable
fun SessionExpiredDialog(onLoginAgain: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.session_expired_title)) },
        text = { Text(stringResource(R.string.session_expired_body)) },
        confirmButton = {
            TextButton(onClick = onLoginAgain) {
                Text(stringResource(R.string.session_expired_action), color = Green600, fontWeight = FontWeight.Bold)
            }
        },
    )
}
