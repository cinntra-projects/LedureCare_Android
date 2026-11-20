package com.cinntra.ledurecare.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.auth.domain.LoginWithPasswordUseCase
import com.cinntra.ledurecare.feature.auth.domain.RequestResetOtpUseCase
import com.cinntra.ledurecare.feature.auth.domain.ResetPasswordUseCase
import com.cinntra.ledurecare.feature.auth.domain.ResetToken
import com.cinntra.ledurecare.feature.auth.domain.VerifyResetOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestOtp: RequestResetOtpUseCase,
    private val verifyOtp: VerifyResetOtpUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val signIn: LoginWithPasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private val _effects = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var resendJob: Job? = null
    private var resetToken: ResetToken? = null

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> _state.update { it.copy(email = event.value, error = null) }
            ForgotPasswordEvent.SendOtp -> sendOtp()
            is ForgotPasswordEvent.OtpChanged -> _state.update {
                it.copy(otp = event.value.filter(Char::isDigit).take(OTP_LENGTH), error = null)
            }
            ForgotPasswordEvent.VerifyOtp -> verify()
            ForgotPasswordEvent.ResendOtp -> if (_state.value.resendSeconds == 0) sendOtp()
            ForgotPasswordEvent.ChangeEmail -> {
                resendJob?.cancel()
                _state.update { it.copy(step = ResetStep.Email, otp = "", resendSeconds = 0, error = null) }
            }
            is ForgotPasswordEvent.NewPasswordChanged -> _state.update { it.copy(newPassword = event.value, error = null) }
            is ForgotPasswordEvent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = event.value, error = null) }
            ForgotPasswordEvent.ToggleNewPasswordVisibility -> _state.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
            ForgotPasswordEvent.ToggleConfirmPasswordVisibility -> _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
            ForgotPasswordEvent.ResetPassword -> reset()
        }
    }

    private fun sendOtp() = launchLoading {
        when (val result = requestOtp(_state.value.email)) {
            is AppResult.Success -> {
                _state.update { it.copy(step = ResetStep.Otp, otp = "", otpSentTo = it.email.trim()) }
                startResendTimer()
            }
            is AppResult.Failure -> _state.update { it.copy(error = result.error) }
        }
    }

    private fun verify() = launchLoading {
        val s = _state.value
        when (val result = verifyOtp(s.email, s.otp)) {
            is AppResult.Success -> {
                resetToken = result.data
                resendJob?.cancel()
                _state.update { it.copy(step = ResetStep.NewPassword, resendSeconds = 0) }
            }
            is AppResult.Failure -> _state.update { it.copy(error = result.error) }
        }
    }

    private fun reset() = launchLoading {
        val s = _state.value
        val token = resetToken ?: return@launchLoading
        when (val result = resetPassword(token, s.newPassword, s.confirmPassword)) {
            is AppResult.Failure -> _state.update { it.copy(error = result.error) }
            is AppResult.Success -> {
                resetToken = null
                // The reset call returns no login token, so sign in with the password just chosen.
                val signedIn = signIn(s.email.trim(), s.newPassword, rememberMe = false)
                _effects.send(
                    if (signedIn is AppResult.Success) ForgotPasswordEffect.SignedIn else ForgotPasswordEffect.SignInAgain
                )
            }
        }
    }

    private fun launchLoading(block: suspend () -> Unit) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                block()
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startResendTimer() {
        resendJob?.cancel()
        resendJob = viewModelScope.launch {
            for (seconds in RESEND_SECONDS downTo 0) {
                _state.update { it.copy(resendSeconds = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private companion object {
        const val RESEND_SECONDS = 30
    }
}
