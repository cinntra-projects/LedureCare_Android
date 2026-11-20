package com.cinntra.ledurecare.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinntra.ledurecare.core.session.SessionManager
import com.cinntra.ledurecare.feature.auth.domain.ClearSessionUseCase
import com.cinntra.ledurecare.feature.plan.domain.ClearPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the app-wide "session expired" dialog. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val clearSession: ClearSessionUseCase,
    private val clearPlan: ClearPlanUseCase,
) : ViewModel() {

    val expired: StateFlow<Boolean> = sessionManager.expired

    /** Wipes the local session and plan, closes the dialog, then calls [onDone] to show the login screen. */
    fun loginAgain(onDone: () -> Unit) {
        viewModelScope.launch {
            clearSession()
            clearPlan()
            sessionManager.reset()
            onDone()
        }
    }
}
