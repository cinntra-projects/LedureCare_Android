package com.example.ledurecare.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.GetRememberedLoginUseCase
import com.example.ledurecare.feature.auth.domain.LoginWithPasswordUseCase
import com.example.ledurecare.feature.auth.domain.SendOtpUseCase
import com.example.ledurecare.feature.auth.domain.VerifyOtpUseCase
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
class LoginViewModel @Inject constructor(
    private val loginWithPassword: LoginWithPasswordUseCase,
    private val sendOtp: SendOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val getRememberedLogin: GetRememberedLoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var resendJob: Job? = null

    init {
        viewModelScope.launch {
            getRememberedLogin()?.let { saved ->
                _state.update { it.copy(userId = saved.userId, password = saved.password, rememberMe = true) }
            }
        }
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.ModeChanged -> _state.update { it.copy(mode = event.mode, error = null) }
            is LoginEvent.UserIdChanged -> _state.update { it.copy(userId = event.value, error = null) }
            is LoginEvent.PasswordChanged -> _state.update { it.copy(password = event.value, error = null) }
            LoginEvent.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            is LoginEvent.RememberMeChanged -> _state.update { it.copy(rememberMe = event.value) }
            LoginEvent.ForgotPassword -> viewModelScope.launch { _effects.send(LoginEffect.OpenForgotPassword) }
            LoginEvent.SignIn -> signIn()
            is LoginEvent.MobileChanged -> _state.update {
                it.copy(mobile = event.value.filter(Char::isDigit).take(MOBILE_LENGTH), error = null)
            }
            LoginEvent.SendOtp -> requestOtp()
            is LoginEvent.OtpChanged -> _state.update {
                it.copy(otp = event.value.filter(Char::isDigit).take(OTP_LENGTH), error = null)
            }
            LoginEvent.VerifyOtp -> verify()
            LoginEvent.ResendOtp -> if (_state.value.resendSeconds == 0) requestOtp()
            LoginEvent.ChangeNumber -> {
                resendJob?.cancel()
                _state.update { it.copy(step = LoginStep.Details, otp = "", resendSeconds = 0, error = null) }
            }
        }
    }

    private fun signIn() = launchLoading {
        val s = _state.value
        handleSignedIn(loginWithPassword(s.userId, s.password, s.rememberMe))
    }

    private fun requestOtp() = launchLoading {
        val s = _state.value
        when (val result = sendOtp(s.mobile)) {
            is AppResult.Success -> {
                _state.update { it.copy(step = LoginStep.Otp, otp = "", otpEmail = result.data.maskedEmail) }
                startResendTimer()
            }
            is AppResult.Failure -> fail(result.error)
        }
    }

    private fun verify() = launchLoading {
        val s = _state.value
        handleSignedIn(verifyOtp(s.mobile, s.otp))
    }

    private suspend fun handleSignedIn(result: AppResult<*>) {
        when (result) {
            is AppResult.Success -> _effects.send(LoginEffect.SignedIn)
            is AppResult.Failure -> fail(result.error)
        }
    }

    private fun fail(error: AppError) = _state.update { it.copy(error = error) }

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
