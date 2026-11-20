package com.cinntra.ledurecare.feature.auth.domain

import javax.inject.Inject

/** Ends the session on this phone without contacting the server; used after the server expired it. */
class ClearSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.clearSession()
}
