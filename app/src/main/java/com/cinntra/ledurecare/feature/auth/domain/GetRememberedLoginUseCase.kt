package com.cinntra.ledurecare.feature.auth.domain

import javax.inject.Inject

/** What "Remember me" saved on the last successful sign-in, or null if nothing is saved. */
data class RememberedLogin(val userId: String, val password: String)

class GetRememberedLoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): RememberedLogin? = repository.rememberedLogin()
}
