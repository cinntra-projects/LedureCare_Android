package com.example.ledurecare.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.auth.domain.LogoutUseCase
import com.example.ledurecare.feature.auth.domain.User
import com.example.ledurecare.feature.plan.domain.ClearPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val logout: LogoutUseCase,
    private val clearPlan: ClearPlanUseCase,
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Clears the session and the locally stored plan, then calls [onDone]. */
    fun onLogout(onDone: () -> Unit) {
        viewModelScope.launch {
            logout()
            clearPlan()
            onDone()
        }
    }
}
