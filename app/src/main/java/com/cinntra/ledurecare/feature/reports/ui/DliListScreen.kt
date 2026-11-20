package com.cinntra.ledurecare.feature.reports.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import com.cinntra.ledurecare.core.ui.message
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import com.cinntra.ledurecare.feature.reports.domain.ReturnView
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.ui.BackButton
import com.cinntra.ledurecare.core.ui.ErrorBox
import com.cinntra.ledurecare.core.ui.LedureTextField
import com.cinntra.ledurecare.core.ui.LoadingBox
import com.cinntra.ledurecare.core.ui.shortDate
import com.cinntra.ledurecare.feature.reports.domain.Dli
import com.cinntra.ledurecare.feature.reports.domain.ReturnStatus
import com.cinntra.ledurecare.ui.theme.ErrorBg
import com.cinntra.ledurecare.ui.theme.ErrorBorder
import com.cinntra.ledurecare.ui.theme.ErrorRed
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.LedureCareTheme
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.PageBg
import com.cinntra.ledurecare.ui.theme.PlanGreen
import java.time.LocalDate

@Composable
fun DliListRoute(
    onBack: () -> Unit,
    onOpenDli: (Int) -> Unit,
    onEditDli: (Int) -> Unit,
    viewModel: DliListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DliListScreen(state, viewModel::onEvent, onBack, onOpenDli, onEditDli)
}

@Composable
fun DliListScreen(
    state: DliListUiState,
    onEvent: (DliListEvent) -> Unit,
    onBack: () -> Unit,
    onOpenDli: (Int) -> Unit,
    onEditDli: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(state.focusSearch) {
        if (state.focusSearch) runCatching { searchFocus.requestFocus() }
    }

    Column(
        modifier = modifier.fillMaxSize().background(PageBg).padding(horizontal = 12.dp).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        var pickingDates by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dli_list_title), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(stringResource(R.string.dli_list_subtitle), fontSize = 12.sp, color = Muted)
            }
            IconButton(onClick = { pickingDates = true }) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.dli_pick_dates),
                    tint = if (state.filter.hasDates) PlanGreen else Ink,
                )
            }
            ReturnViewMenu(state.filter.view) { onEvent(DliListEvent.ViewSelected(it)) }
        }
        if (pickingDates) {
            DateRangeDialog(
                from = state.filter.from,
                to = state.filter.to,
                onApply = { from, to ->
                    pickingDates = false
                    onEvent(DliListEvent.DateRangeSelected(from, to))
                },
                onClear = {
                    pickingDates = false
                    onEvent(DliListEvent.DateRangeCleared)
                },
                onDismiss = { pickingDates = false },
            )
        }
        if (state.filter.isActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.filter.view != ReturnView.All) {
                    FilterChip(
                        stringResource(if (state.filter.view == ReturnView.New) R.string.dli_new_returns else R.string.dli_old_returns),
                    ) { onEvent(DliListEvent.ViewSelected(state.filter.view)) }
                }
                state.filter.from?.let { from ->
                    val to = state.filter.to ?: from
                    FilterChip(if (to == from) from.shortDate() else "${from.shortDate()} - ${to.shortDate()}") {
                        onEvent(DliListEvent.DateRangeCleared)
                    }
                }
            }
        }

        LedureTextField(
            value = state.query,
            onValueChange = { onEvent(DliListEvent.QueryChanged(it)) },
            placeholder = stringResource(R.string.dli_search_hint),
            modifier = Modifier.focusRequester(searchFocus),
            leadingIcon = Icons.Filled.Search,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailing = { StageFilterButton(state.stage) { onEvent(DliListEvent.StageSelected(it)) } },
        )

        state.stage?.let { stage ->
            StageChip(
                stage = stage,
                count = state.visible.size,
                onClear = { onEvent(DliListEvent.StageSelected(null)) },
            )
        }

        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, { onEvent(DliListEvent.Retry) })
            // A filter with no match yet, while more pages are still on their way.
            state.visible.isEmpty() && state.hasMore && state.loadMoreError == null -> LoadingBox()
            state.visible.isEmpty() -> Text(stringResource(R.string.dli_empty), fontSize = 12.sp, color = Muted)
            else -> {
                val listState = rememberLazyListState()
                // Ask for the next page when the last rows come into view.
                val nearEnd by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 4
                    }
                }
                LaunchedEffect(nearEnd, state.all.size, state.hasMore) {
                    if (nearEnd && state.hasMore) onEvent(DliListEvent.LoadMore)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                ) {
                    items(state.visible, key = { it.id }) { dli ->
                        DliCard(
                            dli = dli,
                            onClick = { onOpenDli(dli.id) },
                            // Edit is for service engineers, on DLIs the server marks editable.
                            onEdit = if (state.canEdit(dli)) ({ onEditDli(dli.id) }) else null,
                        )
                    }
                    if (state.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = PlanGreen, strokeWidth = 2.dp)
                            }
                        }
                    }
                    state.loadMoreError?.let { error ->
                        item(key = "load-more-error") {
                            Text(
                                text = error.message(LocalContext.current) + " · " + stringResource(R.string.dli_load_more_retry),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(DliListEvent.Retry) }
                                    .padding(12.dp),
                                fontSize = 12.sp,
                                color = ErrorRed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageFilterButton(selected: ReturnStatus?, onSelect: (ReturnStatus?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GreenMint).clickable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.dli_filter_stage),
                modifier = Modifier.size(16.dp),
                tint = PlanGreen,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dli_clear_stage)) },
                onClick = {
                    open = false
                    onSelect(null)
                },
            )
            ReturnStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(status.titleRes),
                            fontWeight = if (status == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(status)
                    },
                )
            }
        }
    }
}

/** The three dots: new returns or old returns. The active one is bold; choosing it again clears it. */
@Composable
private fun ReturnViewMenu(selected: ReturnView, onSelect: (ReturnView) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.dli_more), tint = Ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(ReturnView.New to R.string.dli_new_returns, ReturnView.Old to R.string.dli_old_returns).forEach { (view, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label), fontWeight = if (view == selected) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        open = false
                        onSelect(view)
                    },
                )
            }
        }
    }
}

/** A removable chip for an active server filter. */
@Composable
private fun FilterChip(text: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(PlanGreen)
            .clickable(onClick = onClear)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dli_clear_filter), modifier = Modifier.size(12.dp), tint = Color.White)
    }
}

/** Pick the first and last audit date; one tap twice on the same day is a single day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    from: LocalDate?,
    to: LocalDate?,
    onApply: (LocalDate, LocalDate) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun Long.date() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    // With no range applied yet the dialog opens on the last three months, but nothing is filtered until Apply.
    val today = remember { LocalDate.now() }
    val initialFrom = from ?: today.minusMonths(3)
    val initialTo = to ?: from ?: today
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom.millis(),
        initialSelectedEndDateMillis = initialTo.millis(),
    )
    val start = pickerState.selectedStartDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    val first = start!!.date()
                    onApply(first, pickerState.selectedEndDateMillis?.date() ?: first)
                },
            ) { Text(stringResource(R.string.dli_apply)) }
        },
        dismissButton = {
            Row {
                if (from != null) TextButton(onClick = onClear) { Text(stringResource(R.string.dli_clear_dates)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dli_cancel)) }
            }
        },
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun StageChip(stage: ReturnStatus, count: Int, onClear: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(PlanGreen)
                .clickable(onClick = onClear)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.dli_stage_chip, stringResource(stage.titleRes)),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.dli_clear_stage),
                modifier = Modifier.size(12.dp),
                tint = Color.White,
            )
        }
        Text(
            stringResource(R.string.dli_count_clear_hint, pluralStringResource(R.plurals.dli_count, count, count)),
            fontSize = 11.sp,
            color = Muted,
        )
    }
}

@Composable
private fun DliCard(dli: Dli, onClick: () -> Unit, onEdit: (() -> Unit)? = null) {
    val alert = dli.isOverSla
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (alert) ErrorBg else Color.White,
        border = BorderStroke(if (alert) 1.5.dp else 1.dp, if (alert) ErrorBorder else FieldBorder),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(dli.dliNo, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (alert) ErrorRed else Ink)
                Text(dli.dealerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    dli.auditDate?.let { stringResource(R.string.dli_items_audited, dli.itemsCount, it.shortDate()) }
                        ?: stringResource(R.string.dli_items_only, dli.itemsCount),
                    fontSize = 11.sp,
                    color = Muted,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (alert) ErrorRed else Muted)
                    Text(
                        stringResource(R.string.dli_in_stage_days, pluralStringResource(R.plurals.dli_days, dli.daysInStage, dli.daysInStage)),
                        fontSize = 11.sp,
                        fontWeight = if (alert) FontWeight.Bold else FontWeight.Normal,
                        color = if (alert) ErrorRed else Muted,
                    )
                }
            }
            if (onEdit != null) {
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PlanGreen.copy(alpha = 0.5f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = PlanGreen)
                        Text(stringResource(R.string.dli_edit), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
        }
    }
}

private fun previewDli(id: Int, no: String, dealer: String, days: Int, over: Boolean) = Dli(
    id, no, dealer, "BP-$id", ReturnStatus.Acc, LocalDate.of(2026, 10, 9), 14, days, over, "Ravi Kumar",
)

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun DliListPreview() = LedureCareTheme {
    DliListScreen(
        state = DliListUiState(
            isLoading = false,
            stage = ReturnStatus.Acc,
            all = listOf(
                previewDli(1, "DLI-1047", "Sharma Auto Agency, Agra", 9, true),
                previewDli(2, "DLI-1052", "Sumit Electric, Meerut", 2, false),
            ),
        ),
        onEvent = {},
        onBack = {},
        onOpenDli = {},
    )
}
