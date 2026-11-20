package com.cinntra.ledurecare.feature.auth.data

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.feature.auth.data.remote.ApiEnvelope
import com.cinntra.ledurecare.feature.auth.data.remote.AuthApi
import com.cinntra.ledurecare.feature.auth.data.remote.ForgotPasswordRequestDto
import com.cinntra.ledurecare.feature.auth.data.remote.ResetPasswordRequestDto
import com.cinntra.ledurecare.feature.auth.data.remote.ResetTokenDto
import com.cinntra.ledurecare.feature.auth.data.remote.VerifyResetOtpRequestDto
import com.cinntra.ledurecare.feature.auth.domain.ResetToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetRepositoryTest {

    private val json = NetworkModule.provideJson()
    private val api = mockk<AuthApi>()

    private fun repository(scope: kotlinx.coroutines.test.TestScope) =
        PasswordResetRepositoryImpl(api, UnconfinedTestDispatcher(scope.testScheduler))

    private fun http400(body: String) =
        HttpException(Response.error<Any>(400, body.toResponseBody("application/json".toMediaType())))

    // ---- request bodies, exactly as the API documents them

    @Test
    fun `request bodies match the API spec`() {
        assertEquals("""{"email":"user@example.com"}""", json.encodeToString(ForgotPasswordRequestDto("user@example.com")))
        assertEquals(
            """{"email":"user@example.com","otp":"123456"}""",
            json.encodeToString(VerifyResetOtpRequestDto("user@example.com", "123456")),
        )
        assertEquals(
            """{"new_password":"NewPass@123","confirm_password":"NewPass@123"}""",
            json.encodeToString(ResetPasswordRequestDto("NewPass@123", "NewPass@123")),
        )
    }

    @Test
    fun `verify response gives the reset token`() {
        val body = """
            {"status": 200, "message": "Code verified.", "data": {"reset_token": "a1b2c3d4e5f6"}, "errors": {}}
        """.trimIndent()
        assertEquals("a1b2c3d4e5f6", json.decodeFromString<ApiEnvelope<ResetTokenDto>>(body).data?.resetToken)
    }

    // ---- the three calls

    @Test
    fun `step 1 sends the email`() = runTest {
        coEvery { api.forgotPassword(any()) } returns Unit

        val result = repository(this).requestOtp("user@example.com")

        assertEquals(AppResult.Success(Unit), result)
        coVerify { api.forgotPassword(ForgotPasswordRequestDto("user@example.com")) }
    }

    @Test
    fun `step 2 returns the reset token`() = runTest {
        coEvery { api.verifyResetOtp(any()) } returns ApiEnvelope(200, "Code verified.", ResetTokenDto("tok-123"))

        val result = repository(this).verifyOtp("user@example.com", "123456")

        assertEquals(AppResult.Success(ResetToken("tok-123")), result)
        coVerify { api.verifyResetOtp(VerifyResetOtpRequestDto("user@example.com", "123456")) }
    }

    @Test
    fun `step 2 treats a success without a token as a failure`() = runTest {
        coEvery { api.verifyResetOtp(any()) } returns ApiEnvelope(200, "Code verified.", ResetTokenDto(null))
        assertTrue(repository(this).verifyOtp("user@example.com", "123456") is AppResult.Failure)
    }

    @Test
    fun `step 2 shows the server message for a wrong or expired code`() = runTest {
        coEvery { api.verifyResetOtp(any()) } throws http400(
            """{"status": 400, "message": "Invalid or expired code.", "data": {}, "errors": {}}"""
        )

        val result = repository(this).verifyOtp("user@example.com", "000000")

        assertEquals(AppResult.Failure(AppError.Http(400, "Invalid or expired code.")), result)
    }

    @Test
    fun `step 3 sends the reset token as the bearer header, not in the body`() = runTest {
        coEvery { api.resetPassword(any(), any()) } returns Unit

        val result = repository(this).resetPassword(ResetToken("tok-123"), "NewPass@123", "NewPass@123")

        assertEquals(AppResult.Success(Unit), result)
        coVerify {
            api.resetPassword("Bearer tok-123", ResetPasswordRequestDto("NewPass@123", "NewPass@123"))
        }
    }

    @Test
    fun `step 3 shows the server message for an expired token`() = runTest {
        coEvery { api.resetPassword(any(), any()) } throws http400(
            """{"status": 400, "message": "Invalid or expired reset token.", "data": {}, "errors": {}}"""
        )
        val result = repository(this).resetPassword(ResetToken("old"), "NewPass@123", "NewPass@123")
        assertEquals(AppResult.Failure(AppError.Http(400, "Invalid or expired reset token.")), result)
    }

    @Test
    fun `step 3 shows the field error rather than the generic validation message`() = runTest {
        coEvery { api.resetPassword(any(), any()) } throws http400(
            """{"status": 400, "message": "Validation failed.", "data": {},
                "errors": {"confirm_password": ["Passwords do not match."]}}"""
        )
        val result = repository(this).resetPassword(ResetToken("tok"), "NewPass@123", "NewPass@124")
        assertEquals(AppResult.Failure(AppError.Http(400, "Passwords do not match.")), result)
    }
}
