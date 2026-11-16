package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.storage.SecureKeys
import com.example.ledurecare.core.storage.SecureStorage
import com.example.ledurecare.feature.auth.data.local.UserDao
import com.example.ledurecare.feature.auth.data.remote.AuthApi
import com.example.ledurecare.feature.auth.data.remote.LoginRequestDto
import com.example.ledurecare.feature.auth.data.remote.ApiEnvelope
import com.example.ledurecare.feature.auth.data.remote.LoginDataDto
import com.example.ledurecare.feature.auth.data.remote.SendOtpRequestDto
import com.example.ledurecare.feature.auth.data.remote.VerifyOtpRequestDto
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.auth.domain.OtpSent
import com.example.ledurecare.feature.auth.domain.RememberedLogin
import com.example.ledurecare.feature.auth.domain.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val userDao: UserDao,
    private val secureStorage: SecureStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(userId: String, password: String, rememberMe: Boolean): AppResult<User> =
        safeCall(io) {
            val user = persistSession(api.login(LoginRequestDto(userId, password)))
            if (rememberMe) {
                secureStorage.putString(KEY_REMEMBERED_USER_ID, userId)
                secureStorage.putString(KEY_REMEMBERED_PASSWORD, password)
            } else {
                secureStorage.remove(KEY_REMEMBERED_USER_ID)
                secureStorage.remove(KEY_REMEMBERED_PASSWORD)
            }
            user
        }

    override suspend fun sendOtp(phone: String): AppResult<OtpSent> = safeCall(io) {
        OtpSent(maskedEmail = api.sendOtp(SendOtpRequestDto(phone)).data?.email.orEmpty())
    }

    override suspend fun verifyOtp(phone: String, otp: String): AppResult<User> = safeCall(io) {
        persistSession(api.verifyOtp(VerifyOtpRequestDto(phone, otp)))
    }

    override suspend fun rememberedLogin(): RememberedLogin? {
        val userId = secureStorage.getString(KEY_REMEMBERED_USER_ID) ?: return null
        return RememberedLogin(userId, secureStorage.getString(KEY_REMEMBERED_PASSWORD).orEmpty())
    }

    override suspend fun logout() {
        // Best effort, so signing out always works: offline, with a token the server already
        // rejected, or when the server is slow. The call goes first because the network layer
        // reads the token from storage.
        withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { safeCall(io) { api.logout() } }
        clearSession()
    }

    override suspend fun clearSession() {
        secureStorage.remove(KEY_TOKEN)
        userDao.clear()
    }

    override suspend fun isLoggedIn(): Boolean = secureStorage.getString(KEY_TOKEN) != null

    override fun observeUser(): Flow<User?> = userDao.observe().map { it?.toDomain() }

    private suspend fun persistSession(response: ApiEnvelope<LoginDataDto>): User {
        val data = checkNotNull(response.data) { response.message.ifEmpty { "Empty response" } }
        secureStorage.putString(KEY_TOKEN, data.access)
        val entity = data.toEntity()
        userDao.upsert(entity)
        return entity.toDomain()
    }

    private companion object {
        const val KEY_TOKEN = SecureKeys.AUTH_TOKEN
        const val KEY_REMEMBERED_USER_ID = SecureKeys.REMEMBERED_USER_ID
        const val KEY_REMEMBERED_PASSWORD = SecureKeys.REMEMBERED_PASSWORD
        const val LOGOUT_TIMEOUT_MS = 3_000L
    }
}
