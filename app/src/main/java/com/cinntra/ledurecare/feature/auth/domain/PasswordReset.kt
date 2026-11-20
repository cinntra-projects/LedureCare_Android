package com.cinntra.ledurecare.feature.auth.domain

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import javax.inject.Inject

/** Proof that the emailed OTP was right; the final step must present it. */
@JvmInline
value class ResetToken(val value: String)

const val MIN_PASSWORD_LENGTH = 8

/** The three steps of "forgot password": email, then OTP, then the new password. */
interface PasswordResetRepository {
    /**
     * Emails an OTP to the account. The server gives the same answer whether or not the account
     * exists, so there is nothing to show beyond "sent".
     */
    suspend fun requestOtp(email: String): AppResult<Unit>

    suspend fun verifyOtp(email: String, otp: String): AppResult<ResetToken>

    suspend fun resetPassword(token: ResetToken, newPassword: String, confirmPassword: String): AppResult<Unit>
}

class RequestResetOtpUseCase @Inject constructor(private val repository: PasswordResetRepository) {
    suspend operator fun invoke(email: String): AppResult<Unit> {
        val trimmed = email.trim()
        if (!trimmed.isPlausibleEmail()) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.Email))
        }
        return repository.requestOtp(trimmed)
    }
}

class VerifyResetOtpUseCase @Inject constructor(private val repository: PasswordResetRepository) {
    suspend operator fun invoke(email: String, otp: String): AppResult<ResetToken> {
        if (otp.length != OTP_DIGITS || !otp.all(Char::isDigit)) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.Otp))
        }
        return repository.verifyOtp(email.trim(), otp)
    }

    private companion object {
        const val OTP_DIGITS = 6
    }
}

class ResetPasswordUseCase @Inject constructor(private val repository: PasswordResetRepository) {
    suspend operator fun invoke(token: ResetToken, newPassword: String, confirmPassword: String): AppResult<Unit> {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.PasswordTooShort))
        }
        if (newPassword != confirmPassword) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.PasswordMismatch))
        }
        return repository.resetPassword(token, newPassword, confirmPassword)
    }
}

/** Not a full RFC check: something before an @, and a dotted domain after it. The server decides the rest. */
internal fun String.isPlausibleEmail(): Boolean {
    val at = indexOf('@')
    return at > 0 && at == lastIndexOf('@') && substring(at + 1).let { it.contains('.') && !it.startsWith('.') && !it.endsWith('.') } && none(Char::isWhitespace)
}
