package com.cinntra.ledurecare.feature.plan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.ui.LedureTextField
import com.cinntra.ledurecare.core.ui.asMessage
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.dealers.ui.DealerSearchList
import com.cinntra.ledurecare.feature.dealers.ui.DealerSearchState
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.Green500
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.PlanGreen
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDealersSheet(sheet: AddSheetState, search: DealerSearchState, onEvent: (PlanEvent) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onEvent(PlanEvent.CloseSheet) },
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
                    Text(stringResource(R.string.sheet_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(stringResource(R.string.sheet_subtitle), fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = { onEvent(PlanEvent.CloseSheet) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sheet_close), tint = Muted)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DateChip(sheet.date.dayLabel()) { showPicker = true }
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
                modifier = Modifier.heightIn(max = 300.dp),
                // Dealers already planned on this date are not offered again.
                dealers = search.items.filter { it.id !in sheet.alreadyPlanned },
            ) { dealer ->
                DealerRow(dealer, selected = dealer.id in sheet.selected) { onEvent(PlanEvent.ToggleDealer(dealer)) }
            }

            ConfirmButton(sheet) { onEvent(PlanEvent.ConfirmAdd) }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = sheet.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onEvent(PlanEvent.SheetDateChanged(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.sheet_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.sheet_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
internal fun DateChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PlanGreen.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = PlanGreen)
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = PlanGreen)
        }
    }
}

@Composable
private fun DealerRow(dealer: Dealer, selected: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GreenMint.copy(alpha = 0.6f) else Color.White,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Green500 else FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.padding(8.dp),
                colors = CheckboxDefaults.colors(checkedColor = PlanGreen, uncheckedColor = FieldBorder),
            )
            Column(Modifier.weight(1f)) {
                Text(dealer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    listOf(dealer.bpCode, dealer.city).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun ConfirmButton(sheet: AddSheetState, onClick: () -> Unit) {
    val count = sheet.selectedCount
    Surface(
        onClick = onClick,
        enabled = count > 0 && !sheet.isSaving,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (count > 0) PlanGreen else PlanGreen.copy(alpha = 0.4f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (count == 0) {
                    stringResource(R.string.sheet_select_prompt)
                } else {
                    pluralStringResource(R.plurals.sheet_add_button, count, count, sheet.date.shortLabel())
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
