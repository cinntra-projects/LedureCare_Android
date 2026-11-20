package com.cinntra.ledurecare.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the server has rejected the signed-in user's token. The network layer marks it;
 * the app shows one "log in again" dialog however many calls fail at once.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _expired = MutableStateFlow(false)
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    fun markExpired() {
        _expired.value = true
    }

    fun reset() {
        _expired.value = false
    }
}
