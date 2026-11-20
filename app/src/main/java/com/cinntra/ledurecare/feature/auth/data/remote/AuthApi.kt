package com.cinntra.ledurecare.feature.auth.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login/")
    suspend fun login(@Body request: LoginRequestDto): ApiEnvelope<LoginDataDto>

    @POST("api/auth/forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto)

    @POST("api/auth/verify-otp/")
    suspend fun verifyResetOtp(@Body request: VerifyResetOtpRequestDto): ApiEnvelope<ResetTokenDto>

    /**
     * [authorization] is `"Bearer <reset_token>"`, not the login token. The network layer leaves the
     * header alone on this path.
     */
    @POST("api/auth/reset-password/")
    suspend fun resetPassword(
        @Header("Authorization") authorization: String,
        @Body request: ResetPasswordRequestDto,
    )

    /** Revokes the access token sent in the Authorization header. No request body. */
    @POST("api/auth/logout/")
    suspend fun logout()

    @POST("api/auth/phone-login/send-otp/")
    suspend fun sendOtp(@Body request: SendOtpRequestDto): ApiEnvelope<OtpSentDto>

    /** Same response as [login]: access token and employee. */
    @POST("api/auth/phone-login/verify-otp/")
    suspend fun verifyOtp(@Body request: VerifyOtpRequestDto): ApiEnvelope<LoginDataDto>
}
