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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.feature.plan.domain.PlannedVisit
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import com.cinntra.ledurecare.ui.theme.BpCodeBg
import com.cinntra.ledurecare.ui.theme.BpCodeText
import com.cinntra.ledurecare.ui.theme.ChipPlannedBg
import com.cinntra.ledurecare.ui.theme.ChipPlannedText
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.Green500
import com.cinntra.ledurecare.ui.theme.Green600
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.GreenMintBorder
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.PlanGreen
import com.cinntra.ledurecare.ui.theme.TabTrack
import java.time.LocalDate
import java.time.YearMonth

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun PlanCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) { content() }
}

// ---------------------------------------------------------------- period header

@Composable
fun PeriodHeader(
    title: String,
    view: PlanView,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onViewChange: (PlanView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // The title takes only the room the view switch leaves, so neither of them ever wraps onto a second line.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.plan_previous), onPrevious)
            Text(
                title,
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            SquareIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.plan_next), onNext)
        }
        ViewSwitch(view, onViewChange)
    }
}

@Composable
private fun SquareIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp), tint = Ink)
        }
    }
}

@Composable
private fun ViewSwitch(selected: PlanView, onSelect: (PlanView) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TabTrack)
            .padding(2.dp),
    ) {
        PlanView.entries.forEach { view ->
            val isSelected = view == selected
            Surface(
                onClick = { onSelect(view) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color.White else Color.Transparent,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
            ) {
                Text(
                    stringResource(
                        when (view) {
                            PlanView.Day -> R.string.plan_view_day
                            PlanView.Week -> R.string.plan_view_week
                            PlanView.Month -> R.string.plan_view_month
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Ink else Muted,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- week strip / month grid

@Composable
fun WeekStrip(
    dates: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        dates.forEach { date ->
            val isSelected = date == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PlanGreen else Color.Transparent)
                    .clickable { onSelect(date) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    date.narrowWeekday(),
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Muted,
                )
                Text(
                    date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Ink,
                )
            }
        }
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekDates(month.atDay(1)).forEach {
                Text(
                    it.narrowWeekday(),
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        monthGrid(month).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Box(Modifier.weight(1f))
                    } else {
                        val isSelected = date == selected
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PlanGreen else Color.Transparent)
                                .clickable { onSelect(date) }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || date == today) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Ink,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    PlanCard(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(label, fontSize = 11.sp, color = Muted)
        }
    }
}

// ---------------------------------------------------------------- visits

@Composable
private fun StatusChip(text: String, emphasised: Boolean) {
    Text(
        text,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (emphasised) GreenMint else ChipPlannedBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (emphasised) PlanGreen else ChipPlannedText,
    )
}

@Composable
private fun BpCodeChip(code: String) {
    Text(
        code,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BpCodeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = BpCodeText,
    )
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (filled) PlanGreen else Color.White,
        border = if (filled) null else BorderStroke(1.dp, PlanGreen.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (filled) Color.White else PlanGreen)
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (filled) Color.White else PlanGreen)
        }
    }
}

/** Full card used by the Day view. */
@Composable
fun DayVisitCard(
    visit: PlannedVisit,
    isUpNext: Boolean,
    isJustAdded: Boolean,
    onStart: () -> Unit,
    onViewDli: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlighted = isUpNext || isJustAdded
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isJustAdded) Color(0xFFF0FDF4) else Color.White,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Green500 else FieldBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    visit.dealerName,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // A completed plan is read-only: only a planned one can be edited.
                if (visit.status == VisitStatus.Planned) EditButton(onEdit)
            }
            DealerMeta(visit)
            if (visit.status == VisitStatus.Done) {
                ActionButton(stringResource(R.string.plan_view_submitted_dli), Icons.Outlined.Visibility, false, onViewDli, Modifier.fillMaxWidth())
            } else {
                ActionButton(stringResource(R.string.plan_start_audit), Icons.Outlined.Description, isUpNext, onStart, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Opens the edit sheet for a plan. Takes the place the status chip used to have. */
@Composable
private fun EditButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PlanGreen.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = PlanGreen)
            Text(stringResource(R.string.plan_edit), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
        }
    }
}

@Composable
private fun DealerMeta(visit: PlannedVisit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BpCodeChip(visit.bpCode)
        if (visit.city.isNotBlank()) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Muted)
            Text(visit.city, fontSize = 11.sp, color = Muted)
        }
    }
}

/** Compact row used by the Week and Month lists. */
@Composable
fun VisitRow(
    visit: PlannedVisit,
    onStart: () -> Unit,
    onViewDli: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(visit.dealerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(visit.bpCode, visit.city).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            if (visit.status == VisitStatus.Done) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onViewDli).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = Green600)
                    Text(stringResource(R.string.plan_view_dli), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Green600)
                }
            } else {
                ActionButton(stringResource(R.string.plan_start), Icons.Filled.PlayArrow, true, onStart, Modifier.height(32.dp))
            }
            if (visit.status == VisitStatus.Planned) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.plan_edit),
                        modifier = Modifier.size(17.dp),
                        tint = PlanGreen,
                    )
                }
            }
        }
    }
}

/** Visits grouped under a date heading. */
@Composable
fun VisitGroups(
    groups: List<Pair<LocalDate, List<PlannedVisit>>>,
    today: LocalDate,
    onStart: (PlannedVisit) -> Unit,
    onViewDli: (PlannedVisit) -> Unit,
    onEdit: (PlannedVisit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { (date, visits) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(date.dayLabel(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                    if (date == today) StatusChip(stringResource(R.string.plan_today), true)
                }
                visits.forEach { visit ->
                    VisitRow(visit, onStart = { onStart(visit) }, onViewDli = { onViewDli(visit) }, onEdit = { onEdit(visit) })
                }
            }
        }
    }
}

@Composable
fun AddDealersButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PlanGreen.copy(alpha = 0.5f)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PlanGreen)
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
        }
    }
}

@Composable
fun dealerCountText(count: Int): String = pluralStringResource(R.plurals.plan_dealer_count, count, count)
