package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.feature.auth.data.remote.AuthApi
import com.example.ledurecare.feature.auth.data.remote.ForgotPasswordRequestDto
import com.example.ledurecare.feature.auth.data.remote.ResetPasswordRequestDto
import com.example.ledurecare.feature.auth.data.remote.VerifyResetOtpRequestDto
import com.example.ledurecare.feature.auth.domain.PasswordResetRepository
import com.example.ledurecare.feature.auth.domain.ResetToken
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PasswordResetRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PasswordResetRepository {

    override suspend fun requestOtp(email: String): AppResult<Unit> = safeCall(io) {
        api.forgotPassword(ForgotPasswordRequestDto(email))
    }

    override suspend fun verifyOtp(email: String, otp: String): AppResult<ResetToken> = safeCall(io) {
        val token = api.verifyResetOtp(VerifyResetOtpRequestDto(email, otp)).data?.resetToken
        ResetToken(checkNotNull(token?.takeIf { it.isNotBlank() }) { "The server sent no reset token" })
    }

    override suspend fun resetPassword(
        token: ResetToken,
        newPassword: String,
        confirmPassword: String,
    ): AppResult<Unit> = safeCall(io) {
        api.resetPassword("Bearer ${token.value}", ResetPasswordRequestDto(newPassword, confirmPassword))
    }
}
