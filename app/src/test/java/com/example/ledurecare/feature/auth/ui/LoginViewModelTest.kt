package com.example.ledurecare.feature.auth.ui

import app.cash.turbine.test
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.auth.domain.GetRememberedLoginUseCase
import com.example.ledurecare.feature.auth.domain.RememberedLogin
import com.example.ledurecare.feature.auth.domain.LoginWithPasswordUseCase
import com.example.ledurecare.feature.auth.domain.OtpSent
import com.example.ledurecare.feature.auth.domain.SendOtpUseCase
import com.example.ledurecare.feature.auth.domain.User
import com.example.ledurecare.feature.auth.domain.VerifyOtpUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val user = User("1", "Test", "t@example.com")

    private class FakeAuthRepository(
        var loginResult: AppResult<User>,
        var sendOtpResult: AppResult<OtpSent> = AppResult.Success(OtpSent("r****2@cinntra.com")),
        var verifyResult: AppResult<User> = loginResult,
        var remembered: RememberedLogin? = null,
    ) : AuthRepository {
        var lastSentMobile: String? = null
        override suspend fun login(userId: String, password: String, rememberMe: Boolean) = loginResult
        override suspend fun sendOtp(phone: String): AppResult<OtpSent> {
            lastSentMobile = phone
            return sendOtpResult
        }
        override suspend fun verifyOtp(phone: String, otp: String) = verifyResult
        override suspend fun rememberedLogin() = remembered
        override suspend fun logout() = Unit
        override suspend fun clearSession() = Unit
        override suspend fun isLoggedIn() = false
        override fun observeUser(): Flow<User?> = emptyFlow()
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeAuthRepository) = LoginViewModel(
        LoginWithPasswordUseCase(repo),
        SendOtpUseCase(repo),
        VerifyOtpUseCase(repo),
        GetRememberedLoginUseCase(repo),
    )

    @Test
    fun `blank credentials produce validation error`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Success(user)))
        vm.onEvent(LoginEvent.SignIn)
        assertEquals(AppError.Validation(AppError.Validation.Field.Credentials), vm.state.value.error)
    }

    @Test
    fun `remembered user id and password are prefilled`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Success(user), remembered = RememberedLogin("ops-1", "secret1")))
        assertEquals("ops-1", vm.state.value.userId)
        assertEquals("secret1", vm.state.value.password)
        assertEquals(true, vm.state.value.rememberMe)
    }

    @Test
    fun `successful password login emits signed in effect`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Success(user)))
        vm.onEvent(LoginEvent.UserIdChanged("ops-1"))
        vm.onEvent(LoginEvent.PasswordChanged("secret"))
        vm.effects.test {
            vm.onEvent(LoginEvent.SignIn)
            assertEquals(LoginEffect.SignedIn, awaitItem())
        }
    }

    @Test
    fun `network failure is exposed in state`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Failure(AppError.Network)))
        vm.onEvent(LoginEvent.UserIdChanged("ops-1"))
        vm.onEvent(LoginEvent.PasswordChanged("secret"))
        vm.onEvent(LoginEvent.SignIn)
        assertEquals(AppError.Network, vm.state.value.error)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun `short mobile number is rejected`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Success(user)))
        vm.onEvent(LoginEvent.MobileChanged("98765"))
        vm.onEvent(LoginEvent.SendOtp)
        assertEquals(AppError.Validation(AppError.Validation.Field.Mobile), vm.state.value.error)
        assertEquals(LoginStep.Details, vm.state.value.step)
    }

    @Test
    fun `send otp sends the ten digit number, moves to the otp step and keeps the masked email`() = runTest {
        val repo = FakeAuthRepository(AppResult.Success(user))
        val vm = viewModel(repo)
        vm.onEvent(LoginEvent.MobileChanged("8750290129"))
        vm.onEvent(LoginEvent.SendOtp)
        assertEquals(LoginStep.Otp, vm.state.value.step)
        assertEquals("8750290129", repo.lastSentMobile) // no country code
        assertEquals("r****2@cinntra.com", vm.state.value.otpEmail)
        vm.onEvent(LoginEvent.ChangeNumber)
        assertEquals(LoginStep.Details, vm.state.value.step)
    }

    @Test
    fun `a number the server rejects shows its message and stays on the number step`() = runTest {
        val repo = FakeAuthRepository(
            AppResult.Success(user),
            sendOtpResult = AppResult.Failure(AppError.Http(404, "No account found for this phone number.")),
        )
        val vm = viewModel(repo)
        vm.onEvent(LoginEvent.MobileChanged("8750290129"))
        vm.onEvent(LoginEvent.SendOtp)
        assertEquals(AppError.Http(404, "No account found for this phone number."), vm.state.value.error)
        assertEquals(LoginStep.Details, vm.state.value.step)
    }

    @Test
    fun `incomplete otp is rejected and complete otp signs in`() = runTest {
        val vm = viewModel(FakeAuthRepository(AppResult.Success(user)))
        vm.onEvent(LoginEvent.MobileChanged("9876543210"))
        vm.onEvent(LoginEvent.SendOtp)
        vm.onEvent(LoginEvent.OtpChanged("48a2"))
        assertEquals("482", vm.state.value.otp)
        vm.onEvent(LoginEvent.VerifyOtp)
        assertEquals(AppError.Validation(AppError.Validation.Field.Otp), vm.state.value.error)

        vm.onEvent(LoginEvent.OtpChanged("4829101"))
        assertEquals("482910", vm.state.value.otp) // six digits at most
        vm.onEvent(LoginEvent.OtpChanged("482910"))
        vm.effects.test {
            vm.onEvent(LoginEvent.VerifyOtp)
            assertEquals(LoginEffect.SignedIn, awaitItem())
        }
    }
}
