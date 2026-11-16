package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.PasswordResetRepository
import com.example.ledurecare.feature.auth.domain.ResetToken
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sample forgot-password flow, used while `BuildConfig.USE_STATIC_PASSWORD_RESET` is on. Any plausible
 * email works, any 6-digit OTP works except 000000 (which fails, to exercise the error state), and no
 * password is actually changed.
 */
@Singleton
class FakePasswordResetRepository @Inject constructor() : PasswordResetRepository {

    override suspend fun requestOtp(email: String): AppResult<Unit> {
        delay(400)
        return AppResult.Success(Unit)
    }

    override suspend fun verifyOtp(email: String, otp: String): AppResult<ResetToken> {
        delay(400)
        if (otp == "000000") return AppResult.Failure(AppError.Http(400, "Invalid or expired code."))
        return AppResult.Success(ResetToken("static-reset-token"))
    }

    override suspend fun resetPassword(
        token: ResetToken,
        newPassword: String,
        confirmPassword: String,
    ): AppResult<Unit> {
        delay(500)
        return AppResult.Success(Unit)
    }
}
