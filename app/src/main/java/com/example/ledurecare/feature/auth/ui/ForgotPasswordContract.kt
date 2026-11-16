package com.example.ledurecare.feature.auth.ui

import com.example.ledurecare.core.common.AppError

enum class ResetStep { Email, Otp, NewPassword }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.Email,
    val email: String = "",
    val otp: String = "",
    /** The address the OTP was sent to, shown on the OTP step. */
    val otpSentTo: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val resendSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface ForgotPasswordEvent {
    data class EmailChanged(val value: String) : ForgotPasswordEvent
    data object SendOtp : ForgotPasswordEvent
    data class OtpChanged(val value: String) : ForgotPasswordEvent
    data object VerifyOtp : ForgotPasswordEvent
    data object ResendOtp : ForgotPasswordEvent
    data object ChangeEmail : ForgotPasswordEvent
    data class NewPasswordChanged(val value: String) : ForgotPasswordEvent
    data class ConfirmPasswordChanged(val value: String) : ForgotPasswordEvent
    data object ToggleNewPasswordVisibility : ForgotPasswordEvent
    data object ToggleConfirmPasswordVisibility : ForgotPasswordEvent
    data object ResetPassword : ForgotPasswordEvent
}

sealed interface ForgotPasswordEffect {
    /** The password was changed and the user is signed in with it; open the dashboard. */
    data object SignedIn : ForgotPasswordEffect

    /** The password was changed but signing in with it failed; send the user to the login screen. */
    data object SignInAgain : ForgotPasswordEffect
}
