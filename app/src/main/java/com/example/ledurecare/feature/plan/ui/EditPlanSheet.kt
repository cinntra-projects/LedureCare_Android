package com.example.ledurecare.feature.plan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureDatePickerDialog
import com.example.ledurecare.core.ui.LedureTextField
import com.example.ledurecare.feature.dealers.ui.DealerChoiceRow
import com.example.ledurecare.feature.dealers.ui.DealerSearchList
import com.example.ledurecare.feature.dealers.ui.DealerSearchState
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PlanGreen

/**
 * Edits one plan, like the add sheet but for a single visit: change its date, pick a different
 * dealer (one only), or both. The plan's current dealer starts selected and stays at the top of the
 * list while nothing is typed, so the current choice is always in view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanSheet(edit: EditSheetState, search: DealerSearchState, onEvent: (PlanEvent) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPicker by remember { mutableStateOf(false) }

    // The current dealer leads the list unless a search is under way; it is not repeated if the page has it.
    val dealers = remember(search.items, search.query, edit.visit) {
        val current = edit.currentDealer
        if (search.query.isBlank() && search.items.none { it.id == current.id }) listOf(current) + search.items else search.items
    }

    ModalBottomSheet(
        onDismissRequest = { onEvent(PlanEvent.CloseEdit) },
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.edit_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(stringResource(R.string.edit_subtitle), fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = { onEvent(PlanEvent.CloseEdit) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sheet_close), tint = Muted)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DateChip(edit.date.dayLabel()) { showPicker = true }
                Text(stringResource(R.string.sheet_change_date_hint), fontSize = 11.sp, color = Muted)
            }

            LedureTextField(
                value = search.query,
                onValueChange = { onEvent(PlanEvent.DealerQueryChanged(it)) },
                placeholder = stringResource(R.string.sheet_search_hint),
                leadingIcon = Icons.Filled.Search,
            )

            DealerSearchList(
                search = search,
                onLoadMore = { onEvent(PlanEvent.LoadMoreDealers) },
                onRetry = { onEvent(PlanEvent.RetryDealers) },
                modifier = Modifier.heightIn(max = 320.dp),
                dealers = dealers,
            ) { dealer ->
                DealerChoiceRow(dealer, selected = dealer.id == edit.dealer.id) {
                    onEvent(PlanEvent.EditDealerSelected(dealer))
                }
            }

            UpdateButton(enabled = edit.hasChanges && !edit.isSaving, isSaving = edit.isSaving) {
                onEvent(PlanEvent.ConfirmEdit)
            }
        }
    }

    if (showPicker) {
        LedureDatePickerDialog(
            initial = edit.date,
            onDismiss = { showPicker = false },
            onPick = {
                showPicker = false
                onEvent(PlanEvent.EditDateChanged(it))
            },
        )
    }
}

@Composable
private fun UpdateButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled || isSaving) PlanGreen else PlanGreen.copy(alpha = 0.4f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.edit_update), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
