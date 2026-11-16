package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.auth.domain.OtpSent
import com.example.ledurecare.feature.auth.domain.RememberedLogin
import com.example.ledurecare.feature.auth.domain.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sample sign-in used while `BuildConfig.USE_STATIC_DATA` is on: any non-blank user ID and password
 * work, and any 6-digit OTP except 000000 (which fails, to exercise the error state).
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val user = MutableStateFlow<User?>(null)
    private var remembered: RememberedLogin? = null

    override suspend fun login(userId: String, password: String, rememberMe: Boolean): AppResult<User> {
        delay(400)
        remembered = RememberedLogin(userId, password).takeIf { rememberMe }
        return AppResult.Success(SampleUser.also { user.value = it })
    }

    override suspend fun sendOtp(phone: String): AppResult<OtpSent> {
        delay(400)
        return AppResult.Success(OtpSent("d***@ledure.com"))
    }

    override suspend fun verifyOtp(phone: String, otp: String): AppResult<User> {
        delay(400)
        if (otp == "000000") return AppResult.Failure(AppError.Http(401, "Invalid OTP."))
        return AppResult.Success(SampleUser.also { user.value = it })
    }

    override suspend fun rememberedLogin(): RememberedLogin? = remembered

    override suspend fun logout() = clearSession()

    override suspend fun clearSession() {
        user.value = null
    }

    override suspend fun isLoggedIn(): Boolean = user.value != null

    override fun observeUser(): Flow<User?> = user

    private companion object {
        val SampleUser = User(
            id = "1",
            name = "Golden Electroworld Pvt. Ltd. (R)",
            email = "demo@ledure.com",
        )
    }
}
