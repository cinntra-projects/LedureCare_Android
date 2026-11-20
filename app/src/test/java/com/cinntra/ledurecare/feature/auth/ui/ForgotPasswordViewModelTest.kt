package com.cinntra.ledurecare.feature.auth.ui

import app.cash.turbine.test
import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.auth.domain.AuthRepository
import com.cinntra.ledurecare.feature.auth.domain.LoginWithPasswordUseCase
import com.cinntra.ledurecare.feature.auth.domain.OtpSent
import com.cinntra.ledurecare.feature.auth.domain.PasswordResetRepository
import com.cinntra.ledurecare.feature.auth.domain.RequestResetOtpUseCase
import com.cinntra.ledurecare.feature.auth.domain.ResetPasswordUseCase
import com.cinntra.ledurecare.feature.auth.domain.ResetToken
import com.cinntra.ledurecare.feature.auth.domain.User
import com.cinntra.ledurecare.feature.auth.domain.VerifyResetOtpUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private class FakeRepository : PasswordResetRepository {
        var requestResult: AppResult<Unit> = AppResult.Success(Unit)
        var verifyResult: AppResult<ResetToken> = AppResult.Success(ResetToken("token-1"))
        var resetResult: AppResult<Unit> = AppResult.Success(Unit)

        val requestedEmails = mutableListOf<String>()
        var verified: Pair<String, String>? = null
        var resetWith: Triple<ResetToken, String, String>? = null

        override suspend fun requestOtp(email: String): AppResult<Unit> {
            requestedEmails += email
            return requestResult
        }

        override suspend fun verifyOtp(email: String, otp: String): AppResult<ResetToken> {
            verified = email to otp
            return verifyResult
        }

        override suspend fun resetPassword(token: ResetToken, newPassword: String, confirmPassword: String): AppResult<Unit> {
            resetWith = Triple(token, newPassword, confirmPassword)
            return resetResult
        }
    }

    /** Only [login] matters here: it is the sign-in that follows a successful reset. */
    private class FakeAuth(var loginResult: AppResult<User>) : AuthRepository {
        var loggedInWith: Triple<String, String, Boolean>? = null

        override suspend fun login(userId: String, password: String, rememberMe: Boolean): AppResult<User> {
            loggedInWith = Triple(userId, password, rememberMe)
            return loginResult
        }

        override suspend fun sendOtp(phone: String): AppResult<OtpSent> = error("not used")
        override suspend fun verifyOtp(phone: String, otp: String): AppResult<User> = error("not used")
        override suspend fun rememberedLogin(): com.cinntra.ledurecare.feature.auth.domain.RememberedLogin? = null
        override suspend fun logout() = Unit
        override suspend fun clearSession() = Unit
        override suspend fun isLoggedIn() = false
        override fun observeUser(): Flow<User?> = emptyFlow()
    }

    private val user = User("12", "Ritanshu Juneja", "ritanshu@cinntra.com")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeRepository, auth: FakeAuth = FakeAuth(AppResult.Success(user))) =
        ForgotPasswordViewModel(
            RequestResetOtpUseCase(repo),
            VerifyResetOtpUseCase(repo),
            ResetPasswordUseCase(repo),
            LoginWithPasswordUseCase(auth),
        )

    private fun ForgotPasswordViewModel.toOtpStep() {
        onEvent(ForgotPasswordEvent.EmailChanged("ritanshu@cinntra.com"))
        onEvent(ForgotPasswordEvent.SendOtp)
    }

    private fun ForgotPasswordViewModel.toPasswordStep() {
        toOtpStep()
        onEvent(ForgotPasswordEvent.OtpChanged("390150"))
        onEvent(ForgotPasswordEvent.VerifyOtp)
    }

    private fun ForgotPasswordViewModel.enterNewPassword(password: String = "NewPass@123", confirm: String = password) {
        onEvent(ForgotPasswordEvent.NewPasswordChanged(password))
        onEvent(ForgotPasswordEvent.ConfirmPasswordChanged(confirm))
    }

    @Test
    fun `starts on the email step`() = runTest {
        assertEquals(ResetStep.Email, viewModel(FakeRepository()).state.value.step)
    }

    @Test
    fun `a blank or malformed email is rejected without calling the server`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        listOf("", "   ", "nobody", "a@b", "a b@c.com", "@c.com", "a@@c.com").forEach { bad ->
            vm.onEvent(ForgotPasswordEvent.EmailChanged(bad))
            vm.onEvent(ForgotPasswordEvent.SendOtp)
            assertEquals(bad, AppError.Validation(AppError.Validation.Field.Email), vm.state.value.error)
        }
        assertTrue(repo.requestedEmails.isEmpty())
        assertEquals(ResetStep.Email, vm.state.value.step)
    }

    @Test
    fun `a valid email opens the otp step showing the address the code was sent to`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.onEvent(ForgotPasswordEvent.EmailChanged("  ritanshu@cinntra.com "))
        vm.onEvent(ForgotPasswordEvent.SendOtp)

        assertEquals(listOf("ritanshu@cinntra.com"), repo.requestedEmails) // trimmed
        assertEquals(ResetStep.Otp, vm.state.value.step)
        assertEquals("ritanshu@cinntra.com", vm.state.value.otpSentTo)
        assertEquals(30, vm.state.value.resendSeconds)
    }

    @Test
    fun `a server error on submit shows its message and stays on the email step`() = runTest {
        val repo = FakeRepository().apply {
            requestResult = AppResult.Failure(AppError.Http(429, "Too many requests. Try again later."))
        }
        val vm = viewModel(repo)
        vm.toOtpStep()
        assertEquals(AppError.Http(429, "Too many requests. Try again later."), vm.state.value.error)
        assertEquals(ResetStep.Email, vm.state.value.step)
    }

    @Test
    fun `change email goes back and clears the code`() = runTest {
        val vm = viewModel(FakeRepository())
        vm.toOtpStep()
        vm.onEvent(ForgotPasswordEvent.OtpChanged("123"))
        vm.onEvent(ForgotPasswordEvent.ChangeEmail)
        assertEquals(ResetStep.Email, vm.state.value.step)
        assertEquals("", vm.state.value.otp)
        assertEquals(0, vm.state.value.resendSeconds)
    }

    @Test
    fun `the otp takes six digits only, and needs all six`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.toOtpStep()

        vm.onEvent(ForgotPasswordEvent.OtpChanged("39a0"))
        assertEquals("390", vm.state.value.otp)
        vm.onEvent(ForgotPasswordEvent.VerifyOtp)
        assertEquals(AppError.Validation(AppError.Validation.Field.Otp), vm.state.value.error)
        assertNull(repo.verified)

        vm.onEvent(ForgotPasswordEvent.OtpChanged("3901509"))
        assertEquals("390150", vm.state.value.otp)
    }

    @Test
    fun `a wrong or expired otp shows the server message and stays put`() = runTest {
        val repo = FakeRepository().apply {
            verifyResult = AppResult.Failure(AppError.Http(400, "Invalid or expired code."))
        }
        val vm = viewModel(repo)
        vm.toPasswordStep()
        assertEquals(AppError.Http(400, "Invalid or expired code."), vm.state.value.error)
        assertEquals(ResetStep.Otp, vm.state.value.step)
    }

    @Test
    fun `a correct otp opens the new password step`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.toPasswordStep()
        assertEquals("ritanshu@cinntra.com" to "390150", repo.verified)
        assertEquals(ResetStep.NewPassword, vm.state.value.step)
    }

    @Test
    fun `resend waits for the countdown`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.toOtpStep()

        vm.onEvent(ForgotPasswordEvent.ResendOtp)
        assertEquals(1, repo.requestedEmails.size) // still counting down

        advanceTimeBy(31_000)
        assertEquals(0, vm.state.value.resendSeconds)
        vm.onEvent(ForgotPasswordEvent.ResendOtp)
        assertEquals(2, repo.requestedEmails.size)
        assertEquals(30, vm.state.value.resendSeconds) // counting again
    }

    @Test
    fun `a short password is rejected`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.toPasswordStep()
        vm.enterNewPassword("short1")
        vm.onEvent(ForgotPasswordEvent.ResetPassword)
        assertEquals(AppError.Validation(AppError.Validation.Field.PasswordTooShort), vm.state.value.error)
        assertNull(repo.resetWith)
    }

    @Test
    fun `passwords that do not match are rejected before calling the server`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.toPasswordStep()
        vm.enterNewPassword("NewPass@123", "NewPass@124")
        vm.onEvent(ForgotPasswordEvent.ResetPassword)
        assertEquals(AppError.Validation(AppError.Validation.Field.PasswordMismatch), vm.state.value.error)
        assertNull(repo.resetWith)
    }

    @Test
    fun `resetting sends the reset token and both passwords, then signs in with the new password`() = runTest {
        val repo = FakeRepository()
        val auth = FakeAuth(AppResult.Success(user))
        val vm = viewModel(repo, auth)
        vm.toPasswordStep()
        vm.enterNewPassword("NewPass@123")

        vm.effects.test {
            vm.onEvent(ForgotPasswordEvent.ResetPassword)
            assertEquals(ForgotPasswordEffect.SignedIn, awaitItem())
        }
        assertEquals(Triple(ResetToken("token-1"), "NewPass@123", "NewPass@123"), repo.resetWith)
        assertEquals(Triple("ritanshu@cinntra.com", "NewPass@123", false), auth.loggedInWith)
    }

    @Test
    fun `if signing in with the new password fails the user is sent to the login screen`() = runTest {
        val auth = FakeAuth(AppResult.Failure(AppError.Http(401, "Invalid email or password.")))
        val vm = viewModel(FakeRepository(), auth)
        vm.toPasswordStep()
        vm.enterNewPassword()

        vm.effects.test {
            vm.onEvent(ForgotPasswordEvent.ResetPassword)
            assertEquals(ForgotPasswordEffect.SignInAgain, awaitItem())
        }
    }

    @Test
    fun `an expired reset token is shown and nothing is signed in`() = runTest {
        val repo = FakeRepository().apply {
            resetResult = AppResult.Failure(AppError.Http(400, "Invalid or expired reset token."))
        }
        val auth = FakeAuth(AppResult.Success(user))
        val vm = viewModel(repo, auth)
        vm.toPasswordStep()
        vm.enterNewPassword()
        vm.onEvent(ForgotPasswordEvent.ResetPassword)

        assertEquals(AppError.Http(400, "Invalid or expired reset token."), vm.state.value.error)
        assertEquals(ResetStep.NewPassword, vm.state.value.step)
        assertNull(auth.loggedInWith)
    }

    @Test
    fun `password visibility toggles are independent`() = runTest {
        val vm = viewModel(FakeRepository())
        vm.onEvent(ForgotPasswordEvent.ToggleNewPasswordVisibility)
        assertEquals(true, vm.state.value.newPasswordVisible)
        assertEquals(false, vm.state.value.confirmPasswordVisible)
        vm.onEvent(ForgotPasswordEvent.ToggleConfirmPasswordVisibility)
        assertEquals(true, vm.state.value.confirmPasswordVisible)
    }
}
