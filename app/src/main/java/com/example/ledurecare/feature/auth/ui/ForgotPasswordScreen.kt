package com.example.ledurecare.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureTextField
import com.example.ledurecare.core.ui.PrimaryButton
import com.example.ledurecare.core.ui.asMessage
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.LedureCareTheme
import com.example.ledurecare.ui.theme.Muted

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onSignInAgain: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ForgotPasswordEffect.SignedIn -> onSignedIn()
                ForgotPasswordEffect.SignInAgain -> {
                    Toast.makeText(context, R.string.forgot_reset_sign_in_again, Toast.LENGTH_LONG).show()
                    onSignInAgain()
                }
            }
        }
    }

    ForgotPasswordScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEvent: (ForgotPasswordEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenFrame(modifier) {
        when (state.step) {
            ResetStep.Email -> EmailStep(state, onEvent, onBack)
            ResetStep.Otp -> OtpStep(state, onEvent)
            ResetStep.NewPassword -> NewPasswordStep(state, onEvent)
        }
    }
}

@Composable
private fun BackRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(subtitle, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun ErrorLine(state: ForgotPasswordUiState) {
    state.error?.let { Text(it.asMessage(), fontSize = 12.sp, color = ErrorRed) }
}

@Composable
private fun EmailStep(state: ForgotPasswordUiState, onEvent: (ForgotPasswordEvent) -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(stringResource(R.string.forgot_back_to_sign_in), onBack)
        Heading(stringResource(R.string.forgot_title), stringResource(R.string.forgot_subtitle))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(stringResource(R.string.forgot_email_label))
            LedureTextField(
                value = state.email,
                onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
                placeholder = stringResource(R.string.forgot_email_hint),
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onEvent(ForgotPasswordEvent.SendOtp) }),
            )
        }

        ErrorLine(state)
        PrimaryButton(
            text = stringResource(R.string.forgot_submit),
            onClick = { onEvent(ForgotPasswordEvent.SendOtp) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun OtpStep(state: ForgotPasswordUiState, onEvent: (ForgotPasswordEvent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BackRow(stringResource(R.string.forgot_change_email)) { onEvent(ForgotPasswordEvent.ChangeEmail) }
        Heading(
            stringResource(R.string.otp_title),
            stringResource(R.string.forgot_otp_sent_to, state.otpSentTo.ifBlank { state.email }),
        )

        OtpInput(
            value = state.otp,
            onValueChange = { onEvent(ForgotPasswordEvent.OtpChanged(it)) },
            focusRequester = focusRequester,
        )

        ResendRow(state.resendSeconds) { onEvent(ForgotPasswordEvent.ResendOtp) }

        ErrorLine(state)
        PrimaryButton(
            text = stringResource(R.string.forgot_verify),
            onClick = { onEvent(ForgotPasswordEvent.VerifyOtp) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun NewPasswordStep(state: ForgotPasswordUiState, onEvent: (ForgotPasswordEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading(stringResource(R.string.forgot_new_title), stringResource(R.string.forgot_new_subtitle))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(stringResource(R.string.forgot_new_password))
            PasswordField(
                value = state.newPassword,
                onValueChange = { onEvent(ForgotPasswordEvent.NewPasswordChanged(it)) },
                placeholder = stringResource(R.string.forgot_new_password_hint),
                visible = state.newPasswordVisible,
                onToggleVisible = { onEvent(ForgotPasswordEvent.ToggleNewPasswordVisibility) },
                imeAction = ImeAction.Next,
            )

            FieldLabel(stringResource(R.string.forgot_confirm_password))
            PasswordField(
                value = state.confirmPassword,
                onValueChange = { onEvent(ForgotPasswordEvent.ConfirmPasswordChanged(it)) },
                placeholder = stringResource(R.string.forgot_confirm_password_hint),
                visible = state.confirmPasswordVisible,
                onToggleVisible = { onEvent(ForgotPasswordEvent.ToggleConfirmPasswordVisibility) },
                imeAction = ImeAction.Done,
                onDone = { onEvent(ForgotPasswordEvent.ResetPassword) },
            )
            Text(stringResource(R.string.forgot_password_rule), fontSize = 11.sp, color = Muted)
        }

        ErrorLine(state)
        PrimaryButton(
            text = stringResource(R.string.forgot_reset),
            onClick = { onEvent(ForgotPasswordEvent.ResetPassword) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
) {
    LedureTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.Lock,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailing = {
            IconButton(onClick = onToggleVisible, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visible) R.string.login_hide_password else R.string.login_show_password),
                    modifier = Modifier.size(18.dp),
                    tint = Muted,
                )
            }
        },
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ForgotEmailPreview() = LedureCareTheme {
    ForgotPasswordScreen(ForgotPasswordUiState(email = "me@example.com"), {}, {})
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ForgotOtpPreview() = LedureCareTheme {
    ForgotPasswordScreen(
        ForgotPasswordUiState(step = ResetStep.Otp, email = "me@example.com", otpSentTo = "m*****e@example.com", otp = "482", resendSeconds = 24),
        {},
        {},
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ForgotNewPasswordPreview() = LedureCareTheme {
    ForgotPasswordScreen(ForgotPasswordUiState(step = ResetStep.NewPassword), {}, {})
}
