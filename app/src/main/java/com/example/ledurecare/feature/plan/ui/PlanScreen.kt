package com.example.ledurecare.feature.plan.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.message
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.feature.plan.domain.PlannedVisit
import com.example.ledurecare.feature.plan.domain.VisitStatus
import com.example.ledurecare.ui.theme.Green600
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.LedureCareTheme
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import java.time.LocalDate

@Composable
fun PlanRoute(
    onStartAudit: (visitId: Long) -> Unit,
    onViewDli: (returnId: Int) -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlanEffect.OpenAudit -> onStartAudit(effect.visitId)
                is PlanEffect.OpenDli -> onViewDli(effect.returnId)
                is PlanEffect.ShowMessage -> toast(context, context.getString(effect.messageRes))
                is PlanEffect.ShowError -> toast(context, effect.error.message(context))
                PlanEffect.VisitUpdated -> toast(context, context.getString(R.string.plan_toast_updated))
                is PlanEffect.PlansAdded -> toast(context, plansAddedText(context, effect))
            }
        }
    }

    PlanScreen(state = state, onEvent = viewModel::onEvent)
}

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

/** "2 dealers added", or "2 dealers added · 1 already planned" when the server left some out. */
private fun plansAddedText(context: Context, effect: PlanEffect.PlansAdded): String {
    if (effect.created == 0) return context.getString(R.string.plan_toast_none_added)
    val added = context.resources.getQuantityString(R.plurals.plan_toast_added, effect.created, effect.created)
    return if (effect.skipped == 0) added else context.getString(R.string.plan_toast_added_skipped, added, effect.skipped)
}

@Composable
fun PlanScreen(state: PlanUiState, onEvent: (PlanEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green500)
        when (state.view) {
            PlanView.Day -> DayContent(state, onEvent)
            PlanView.Week -> WeekContent(state, onEvent)
            PlanView.Month -> MonthContent(state, onEvent)
        }
    }

    state.sheet?.let { AddDealersSheet(it, state.dealerSearch, onEvent) }
    state.edit?.let { EditPlanSheet(it, state.dealerSearch, onEvent) }
}

@Composable
private fun DayContent(state: PlanUiState, onEvent: (PlanEvent) -> Unit) {
    val date = state.selectedDate
    PlanCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PeriodHeader(
                title = date.dayLabel(),
                view = state.view,
                onPrevious = { onEvent(PlanEvent.Previous) },
                onNext = { onEvent(PlanEvent.Next) },
                onViewChange = { onEvent(PlanEvent.ViewChanged(it)) },
            )
            WeekStrip(weekDates(date), date, onSelect = { onEvent(PlanEvent.DateSelected(it)) })
        }
    }

    val visits = state.visitsOn(date)
    val justAddedCount = visits.count { it.id in state.justAdded }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.plan_day_title, date.dayLabel(), dealerCountText(visits.size)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        if (justAddedCount > 0) {
            Text(stringResource(R.string.plan_just_added_note, justAddedCount), fontSize = 11.sp, color = Green600)
        }
    }

    if (visits.isEmpty()) {
        Text(stringResource(R.string.plan_empty_day), fontSize = 12.sp, color = Muted)
    }
    visits.forEach { visit ->
        DayVisitCard(
            visit = visit,
            isUpNext = visit.id == state.upNextId && date == state.today,
            isJustAdded = visit.id in state.justAdded,
            onStart = { onEvent(PlanEvent.StartAudit(visit.id)) },
            onViewDli = { onEvent(PlanEvent.ViewDli(visit.id)) },
            onEdit = { onEvent(PlanEvent.OpenEdit(visit.id)) },
        )
    }
    AddDealersButton(stringResource(R.string.plan_add_for_day, date.shortLabel()), { onEvent(PlanEvent.OpenSheet) })
}

@Composable
private fun WeekContent(state: PlanUiState, onEvent: (PlanEvent) -> Unit) {
    val date = state.selectedDate
    val range = weekRange(date)
    val dates = weekDates(date)
    PlanCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PeriodHeader(
                title = range.label(),
                view = state.view,
                onPrevious = { onEvent(PlanEvent.Previous) },
                onNext = { onEvent(PlanEvent.Next) },
                onViewChange = { onEvent(PlanEvent.ViewChanged(it)) },
            )
            WeekStrip(dates, date, onSelect = { onEvent(PlanEvent.DateSelected(it)) })
        }
    }

    val weekVisits = state.visitsIn(dates)
    val isCurrentWeek = state.today in range.start..range.end
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(
                if (isCurrentWeek) R.string.plan_week_title_current else R.string.plan_week_title_other,
                weekVisits.size,
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            stringResource(R.string.plan_week_subtitle, range.label(), weekVisits.count { it.status == VisitStatus.Done }),
            fontSize = 11.sp,
            color = Muted,
        )
    }

    VisitGroups(
        groups = dates.map { it to state.visitsOn(it) }.filter { it.second.isNotEmpty() },
        today = state.today,
        onStart = { onEvent(PlanEvent.StartAudit(it.id)) },
        onViewDli = { onEvent(PlanEvent.ViewDli(it.id)) },
        onEdit = { onEvent(PlanEvent.OpenEdit(it.id)) },
    )
    AddDealersButton(stringResource(R.string.plan_add_to_week), { onEvent(PlanEvent.OpenSheet) })
}

@Composable
private fun MonthContent(state: PlanUiState, onEvent: (PlanEvent) -> Unit) {
    val month = state.month
    PlanCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PeriodHeader(
                title = month.label(),
                view = state.view,
                onPrevious = { onEvent(PlanEvent.Previous) },
                onNext = { onEvent(PlanEvent.Next) },
                onViewChange = { onEvent(PlanEvent.ViewChanged(it)) },
            )
            MonthGrid(month, state.selectedDate, state.today, onSelect = { onEvent(PlanEvent.DateSelected(it)) })
        }
    }

    val monthVisits = state.visitsByDate.values.flatten()
    val done = monthVisits.count { it.status == VisitStatus.Done }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(monthVisits.size, stringResource(R.string.plan_stat_planned), Modifier.weight(1f))
        StatCard(done, stringResource(R.string.plan_stat_done), Modifier.weight(1f))
        StatCard(monthVisits.size - done, stringResource(R.string.plan_stat_left), Modifier.weight(1f))
    }

    val remaining = state.visitsByDate
        .filterKeys { !it.isBefore(state.today) }
        .toSortedMap()
        .map { (date, visits) -> date to visits.filter { it.status == VisitStatus.Planned } }
        .filter { it.second.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.plan_remaining_title, month.nameLabel(), remaining.sumOf { it.second.size }),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(stringResource(R.string.plan_remaining_hint), fontSize = 11.sp, color = Muted)
    }

    VisitGroups(
        groups = remaining,
        today = state.today,
        onStart = { onEvent(PlanEvent.StartAudit(it.id)) },
        onViewDli = { onEvent(PlanEvent.ViewDli(it.id)) },
        onEdit = { onEvent(PlanEvent.OpenEdit(it.id)) },
    )
    AddDealersButton(stringResource(R.string.plan_add_to_date), { onEvent(PlanEvent.OpenSheet) })
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 28)

private fun previewVisit(id: Long, name: String, bp: String, city: String, date: LocalDate, status: VisitStatus) =
    PlannedVisit(id, id.toString(), name, bp, city, date, status)

private fun previewState(view: PlanView) = PlanUiState(
    view = view,
    today = PreviewToday,
    selectedDate = PreviewToday,
    visitsByDate = listOf(
        previewVisit(1, "Amit Electric", "BP-23125", "Noida", PreviewToday, VisitStatus.Done),
        previewVisit(2, "Sumit Electric", "BP-23128", "Meerut", PreviewToday, VisitStatus.Planned),
        previewVisit(3, "ABM Electrical", "BP-21222", "Bijnor", PreviewToday, VisitStatus.Planned),
        previewVisit(4, "Verma Motors", "BP-22310", "Ghaziabad", PreviewToday.plusDays(1), VisitStatus.Planned),
    ).groupBy { it.date },
    justAdded = setOf(3L),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PlanDayPreview() = LedureCareTheme { PlanScreen(previewState(PlanView.Day), {}) }

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PlanWeekPreview() = LedureCareTheme { PlanScreen(previewState(PlanView.Week), {}) }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PlanMonthPreview() = LedureCareTheme { PlanScreen(previewState(PlanView.Month), {}) }
