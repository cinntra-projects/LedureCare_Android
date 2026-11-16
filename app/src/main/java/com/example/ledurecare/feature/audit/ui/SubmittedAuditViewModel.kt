package com.example.ledurecare.feature.audit.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.GetSubmittedAuditUseCase
import com.example.ledurecare.feature.audit.domain.SubmittedAudit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the viewer is showing: the generated audit form, or one of the files attached to it. */
sealed interface ViewedFile {
    data object AuditForm : ViewedFile
    data class Attachment(val file: AuditFile) : ViewedFile
}

data class SubmittedUiState(
    val isLoading: Boolean = true,
    val audit: SubmittedAudit? = null,
    /** Set on the viewer screen only. */
    val viewed: ViewedFile? = null,
    val error: AppError? = null,
)

/** Index meaning "the audit form" in [AuditFileDestination]; attachments are 0, 1, 2... */
const val AUDIT_FORM_INDEX = -1

/**
 * Serves both the confirmation screen and the viewer. The viewer's navigation arguments are
 * `dliNo` and `fileIndex`; the confirmation screen has `dliNo` only.
 */
@HiltViewModel
class SubmittedAuditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubmitted: GetSubmittedAuditUseCase,
) : ViewModel() {

    private val dliNo: String = checkNotNull(savedStateHandle.get<String>("dliNo")) { "DLI number missing" }
    private val fileIndex: Int? = savedStateHandle.get<Int>("fileIndex")

    private val _state = MutableStateFlow(SubmittedUiState())
    val state: StateFlow<SubmittedUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getSubmitted(dliNo)) {
                is AppResult.Success -> {
                    val audit = result.data
                    val viewed = when {
                        fileIndex == null -> null
                        fileIndex == AUDIT_FORM_INDEX -> ViewedFile.AuditForm
                        else -> audit.attachments.getOrNull(fileIndex)?.let { ViewedFile.Attachment(it) }
                    }
                    if (fileIndex != null && viewed == null) {
                        _state.update { it.copy(isLoading = false, error = AppError.Http(404, "File not found.")) }
                    } else {
                        _state.update { SubmittedUiState(isLoading = false, audit = audit, viewed = viewed) }
                    }
                }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
