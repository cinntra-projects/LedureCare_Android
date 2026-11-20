package com.cinntra.ledurecare.feature.auth.ui

import com.cinntra.ledurecare.core.common.AppError

enum class LoginMode { UserId, Mobile }

enum class LoginStep { Details, Otp }

const val OTP_LENGTH = 6
const val MOBILE_LENGTH = 10

data class LoginUiState(
    val mode: LoginMode = LoginMode.UserId,
    val step: LoginStep = LoginStep.Details,
    val userId: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val countryCode: String = "+91",
    val mobile: String = "",
    val otp: String = "",
    /** The masked email the code was sent to, as returned by the server. */
    val otpEmail: String = "",
    val resendSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface LoginEvent {
    data class ModeChanged(val mode: LoginMode) : LoginEvent
    data class UserIdChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data class RememberMeChanged(val value: Boolean) : LoginEvent
    data object ForgotPassword : LoginEvent
    data object SignIn : LoginEvent
    data class MobileChanged(val value: String) : LoginEvent
    data object SendOtp : LoginEvent
    data class OtpChanged(val value: String) : LoginEvent
    data object VerifyOtp : LoginEvent
    data object ResendOtp : LoginEvent
    data object ChangeNumber : LoginEvent
}

sealed interface LoginEffect {
    data object SignedIn : LoginEffect
    data object OpenForgotPassword : LoginEffect
}
