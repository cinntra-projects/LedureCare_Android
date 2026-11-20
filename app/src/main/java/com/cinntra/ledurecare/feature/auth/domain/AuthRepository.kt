package com.cinntra.ledurecare.feature.auth.domain

import com.cinntra.ledurecare.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** Where the one-time code went: the account's registered email, partly masked by the server. */
data class OtpSent(val maskedEmail: String)

interface AuthRepository {
    suspend fun login(userId: String, password: String, rememberMe: Boolean): AppResult<User>

    /** [phone] is the 10-digit number without a country code. */
    suspend fun sendOtp(phone: String): AppResult<OtpSent>
    /** [phone] is the 10-digit number the OTP was sent for; signs the user in on success. */
    suspend fun verifyOtp(phone: String, otp: String): AppResult<User>
    /** The user ID and password saved by "Remember me", kept encrypted on this phone. */
    suspend fun rememberedLogin(): RememberedLogin?
    /** Tells the server to revoke the token, then clears the session on this phone. */
    suspend fun logout()

    /** Clears the session on this phone only, for when the server already ended it. */
    suspend fun clearSession()
    suspend fun isLoggedIn(): Boolean
    fun observeUser(): Flow<User?>
}
