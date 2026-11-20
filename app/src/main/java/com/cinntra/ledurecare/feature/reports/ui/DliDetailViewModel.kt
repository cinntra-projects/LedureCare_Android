package com.cinntra.ledurecare.feature.reports.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.reports.domain.DliDetail
import com.cinntra.ledurecare.feature.reports.domain.GetDliDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DliDetailUiState(
    val isLoading: Boolean = true,
    val detail: DliDetail? = null,
    val error: AppError? = null,
)

@HiltViewModel
class DliDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDliDetail: GetDliDetailUseCase,
) : ViewModel() {

    private val id: Int = checkNotNull(savedStateHandle.get<Int>("id")) { "DLI id missing" }

    private val _state = MutableStateFlow(DliDetailUiState())
    val state: StateFlow<DliDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getDliDetail(id)) {
                is AppResult.Success -> _state.update { DliDetailUiState(isLoading = false, detail = result.data) }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
