package com.example.ledurecare.feature.audit.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.BackButton
import com.example.ledurecare.core.ui.ErrorBox
import com.example.ledurecare.core.ui.LoadingBox
import com.example.ledurecare.core.ui.asMessage
import com.example.ledurecare.core.ui.shortDate
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import com.example.ledurecare.ui.theme.PlanGreen

@Composable
fun AuditRoute(
    onClose: () -> Unit,
    onSubmitted: (dliNo: String) -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmExit by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                is AuditEffect.Submitted -> {
                    onSubmitted(effect.dliNo)
                    null
                }
                is AuditEffect.FilesRejected -> context.getString(R.string.audit_files_too_large, effect.names.joinToString())
                AuditEffect.FixErrors -> context.getString(R.string.audit_fix_errors)
            }
            message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.onEvent(AuditEvent.FilesPicked(uris.map(context::toAuditFile)))
    }

    val leave = { if (state.isDirty) confirmExit = true else onClose() }
    BackHandler(onBack = leave)

    AuditScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = leave,
        onAddFiles = { filePicker.launch(arrayOf("image/*", "application/pdf")) },
    )

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.audit_discard_title)) },
            text = { Text(stringResource(R.string.audit_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onClose()
                }) { Text(stringResource(R.string.audit_discard), color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.audit_keep_editing)) }
            },
        )
    }
}

@Composable
fun AuditScreen(
    state: AuditUiState,
    onEvent: (AuditEvent) -> Unit,
    onBack: () -> Unit,
    onAddFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(PageBg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Column {
                    Text(stringResource(R.string.audit_title), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    state.startedFrom?.let {
                        Text(stringResource(R.string.audit_started_from, it.shortDate()), fontSize = 11.sp, color = Muted)
                    }
                }
            }

            when {
                state.isLoading -> LoadingBox()
                state.error != null -> ErrorBox(state.error, { onEvent(AuditEvent.Retry) })
                else -> {
                    DealerDetailsCard(state, onEvent)
                    AuditedItemsCard(state, onEvent)
                    WarehouseCard(state, onEvent)
                    NotesCard(state, onEvent)
                    AttachmentsCard(state, onAddFiles, onEvent)
                }
            }
        }

        if (!state.isLoading && state.error == null) {
            SubmitBar(state) { onEvent(AuditEvent.Submit) }
        }
    }
}

@Composable
private fun SubmitBar(state: AuditUiState, onSubmit: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.submitError?.let { Text(it.asMessage(), fontSize = 12.sp, color = ErrorRed) }
            Surface(
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (state.isSubmitting) PlanGreen.copy(alpha = 0.7f) else PlanGreen,
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.audit_submit), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
