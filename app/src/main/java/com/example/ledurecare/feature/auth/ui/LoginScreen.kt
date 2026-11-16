package com.example.ledurecare.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.Green600
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.LedureCareTheme
import com.example.ledurecare.ui.theme.Muted

@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.SignedIn -> onSignedIn()
                LoginEffect.OpenForgotPassword -> onForgotPassword()
            }
        }
    }

    LoginScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenFrame(modifier) {
        when (state.step) {
            LoginStep.Details -> DetailsContent(state, onEvent)
            LoginStep.Otp -> OtpContent(state, onEvent)
        }
    }
}

@Composable
private fun DetailsContent(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.login_welcome),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            Text(stringResource(R.string.login_subtitle), fontSize = 12.sp, color = Muted)
        }

        ModeTabs(
            selected = state.mode,
            onSelect = { onEvent(LoginEvent.ModeChanged(it)) },
            userIdLabel = stringResource(R.string.login_tab_user_id),
            userIdIcon = Icons.Outlined.Person,
            mobileLabel = stringResource(R.string.login_tab_mobile),
            mobileIcon = Icons.Outlined.PhoneAndroid,
        )

        when (state.mode) {
            LoginMode.UserId -> UserIdForm(state, onEvent)
            LoginMode.Mobile -> MobileForm(state, onEvent)
        }
    }
}

@Composable
private fun UserIdForm(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(R.string.login_user_id_label))
        LedureTextField(
            value = state.userId,
            onValueChange = { onEvent(LoginEvent.UserIdChanged(it)) },
            placeholder = stringResource(R.string.login_user_id_hint),
            leadingIcon = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
        )

        Spacer(Modifier.height(2.dp))
        FieldLabel(stringResource(R.string.login_password_label))
        LedureTextField(
            value = state.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.login_password_hint),
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(LoginEvent.SignIn) }),
            trailing = {
                IconButton(
                    onClick = { onEvent(LoginEvent.TogglePasswordVisibility) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(
                            if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password
                        ),
                        modifier = Modifier.size(18.dp),
                        tint = Muted,
                    )
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RememberMe(state.rememberMe) { onEvent(LoginEvent.RememberMeChanged(it)) }
            Text(
                stringResource(R.string.login_forgot_password),
                modifier = Modifier.clickable { onEvent(LoginEvent.ForgotPassword) },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Green600,
            )
        }

        ErrorMessage(state)
        PrimaryButton(
            text = stringResource(R.string.login_sign_in),
            onClick = { onEvent(LoginEvent.SignIn) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun RememberMe(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(18.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Green500,
                    uncheckedColor = FieldBorder
                ),
            )
        }
        Text(stringResource(R.string.login_remember_me), fontSize = 12.sp, color = Ink)
    }
}

@Composable
private fun MobileForm(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(R.string.login_mobile_label))
        LedureTextField(
            value = state.mobile,
            onValueChange = { onEvent(LoginEvent.MobileChanged(it)) },
            placeholder = stringResource(R.string.login_mobile_hint),
            prefix = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "IN ${state.countryCode}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Muted
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(FieldBorder)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(LoginEvent.SendOtp) }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Muted
            )
            Text(
                stringResource(R.string.login_mobile_info),
                fontSize = 11.sp,
                color = Muted,
                lineHeight = 14.sp
            )
        }

        ErrorMessage(state)
        PrimaryButton(
            text = stringResource(R.string.login_send_otp),
            onClick = { onEvent(LoginEvent.SendOtp) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun OtpContent(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.clickable { onEvent(LoginEvent.ChangeNumber) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Ink
            )
            Text(
                stringResource(R.string.otp_change_number),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.otp_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Text(
                stringResource(
                    R.string.otp_sent_to,
                    state.otpEmail.ifBlank {
                        "${state.countryCode} ${state.mobile.take(5)} ${
                            state.mobile.drop(5)
                        }"
                    },
                ),
                fontSize = 12.sp,
                color = Muted,
            )
        }

        OtpInput(
            value = state.otp,
            onValueChange = { onEvent(LoginEvent.OtpChanged(it)) },
            focusRequester = focusRequester,
        )

        ResendRow(state.resendSeconds) { onEvent(LoginEvent.ResendOtp) }

        ErrorMessage(state)
        PrimaryButton(
            text = stringResource(R.string.otp_verify),
            onClick = { onEvent(LoginEvent.VerifyOtp) },
            isLoading = state.isLoading,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Green500
            )
            Text(stringResource(R.string.otp_valid_for), fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
internal fun ResendRow(seconds: Int, onResend: () -> Unit) {
    Row {
        Text(stringResource(R.string.otp_didnt_get), fontSize = 12.sp, color = Muted)
        if (seconds > 0) {
            Text(
                stringResource(
                    R.string.otp_resend_in,
                    "%02d:%02d".format(seconds / 60, seconds % 60)
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        } else {
            Text(
                stringResource(R.string.otp_resend),
                modifier = Modifier.clickable(onClick = onResend),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Green600,
            )
        }
    }
}

@Composable
private fun ErrorMessage(state: LoginUiState) {
    state.error?.let {
        Text(it.asMessage(), fontSize = 12.sp, color = ErrorRed)
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun LoginUserIdPreview() {
    LedureCareTheme { LoginScreen(state = LoginUiState(), onEvent = {}) }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun LoginMobilePreview() {
    LedureCareTheme {
        LoginScreen(
            state = LoginUiState(mode = LoginMode.Mobile, mobile = "9876543210"),
            onEvent = {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun LoginOtpPreview() {
    LedureCareTheme {
        LoginScreen(
            state = LoginUiState(
                mode = LoginMode.Mobile,
                step = LoginStep.Otp,
                mobile = "9876543210",
                otp = "482",
                resendSeconds = 24,
            ),
            onEvent = {},
        )
    }
}
