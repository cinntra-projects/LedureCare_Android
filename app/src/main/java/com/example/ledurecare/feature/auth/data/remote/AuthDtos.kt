package com.example.ledurecare.feature.auth.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val status: Int = 0,
    val message: String = "",
    val data: T? = null,
)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

/** `POST api/auth/phone-login/send-otp/`. [phone] is the 10-digit number, without a country code. */
@Serializable
data class SendOtpRequestDto(val phone: String)

/** `data` of the send-OTP response: the registered email the code was sent to, partly masked. */
@Serializable
data class OtpSentDto(val email: String? = null)

/** `POST api/auth/phone-login/verify-otp/`. [phone] is the same 10-digit number the OTP was sent for. */
@Serializable
data class VerifyOtpRequestDto(val phone: String, val otp: String)

/** `POST api/auth/forgot-password/`. The server answers the same whether or not the account exists. */
@Serializable
data class ForgotPasswordRequestDto(val email: String)

/** `POST api/auth/verify-otp/`. */
@Serializable
data class VerifyResetOtpRequestDto(val email: String, val otp: String)

/** `data` of a successful verify: the token that authorises the final reset call. */
@Serializable
data class ResetTokenDto(@SerialName("reset_token") val resetToken: String? = null)

/** `POST api/auth/reset-password/`, sent with `Authorization: Bearer <reset_token>`. */
@Serializable
data class ResetPasswordRequestDto(
    @SerialName("new_password") val newPassword: String,
    @SerialName("confirm_password") val confirmPassword: String,
)

@Serializable
data class RoleDto(val id: Int = 0, val name: String = "", val code: String = "")

@Serializable
data class EmployeeDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: RoleDto? = null,
)

/** The login response's `permissions` list is deliberately not read: the app does not gate anything on it. */
@Serializable
data class LoginDataDto(
    val access: String,
    val employee: EmployeeDto,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)
