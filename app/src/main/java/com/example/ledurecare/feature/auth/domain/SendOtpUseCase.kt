package com.example.ledurecare.feature.auth.domain

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    /** [mobile] is the 10-digit number as typed; the server takes it without a country code. */
    suspend operator fun invoke(mobile: String): AppResult<OtpSent> {
        if (mobile.length != MOBILE_LENGTH || !mobile.all(Char::isDigit)) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.Mobile))
        }
        return repository.sendOtp(mobile)
    }

    private companion object {
        const val MOBILE_LENGTH = 10
    }
}
