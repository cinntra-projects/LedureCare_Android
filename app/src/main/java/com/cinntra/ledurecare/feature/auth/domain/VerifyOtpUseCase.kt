package com.cinntra.ledurecare.feature.auth.domain

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(mobile: String, otp: String): AppResult<User> {
        if (otp.length != OTP_LENGTH || !otp.all(Char::isDigit)) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.Otp))
        }
        return repository.verifyOtp(mobile, otp)
    }

    private companion object {
        const val OTP_LENGTH = 6
    }
}
