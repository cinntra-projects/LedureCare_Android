package com.cinntra.ledurecare.feature.auth.domain

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import javax.inject.Inject

class LoginWithPasswordUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(userId: String, password: String, rememberMe: Boolean): AppResult<User> {
        if (userId.isBlank() || password.isBlank()) {
            return AppResult.Failure(AppError.Validation(AppError.Validation.Field.Credentials))
        }
        return repository.login(userId.trim(), password, rememberMe)
    }
}
