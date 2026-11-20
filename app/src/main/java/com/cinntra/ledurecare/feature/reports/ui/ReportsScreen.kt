package com.cinntra.ledurecare.feature.reports.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.ui.ErrorBox
import com.cinntra.ledurecare.core.ui.LoadingBox
import com.cinntra.ledurecare.feature.reports.domain.ReturnStatus
import com.cinntra.ledurecare.feature.reports.domain.StageSummary
import com.cinntra.ledurecare.ui.theme.ChipPlannedBg
import com.cinntra.ledurecare.ui.theme.ChipPlannedText
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.LedureCareTheme
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.PageBg
import com.cinntra.ledurecare.ui.theme.PlanGreen
import com.cinntra.ledurecare.ui.theme.Placeholder

@Composable
fun ReportsRoute(
    onOpenStage: (ReturnStatus) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportsScreen(state, onOpenStage, onOpenSearch, viewModel::refresh)
}

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onOpenStage: (ReturnStatus) -> Unit,
    onOpenSearch: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.reports_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(stringResource(R.string.reports_subtitle), fontSize = 12.sp, color = Muted)
        }

        SearchEntry(onOpenSearch)

        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry)
            else -> state.summaries.chunked(2).forEach { pair ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { summary ->
                        StageCard(summary, { onOpenStage(summary.status) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Looks like a search field but just opens the DLI list, where the real field lives. */
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Muted)
            Text(stringResource(R.string.reports_search_hint), Modifier.weight(1f), fontSize = 14.sp, color = Placeholder)
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GreenMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = PlanGreen)
            }
        }
    }
}

@Composable
private fun StageCard(summary: StageSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val completed = summary.status == ReturnStatus.Completed
    val primary = if (completed) Color.White else Ink
    val secondary = if (completed) Color.White.copy(alpha = 0.8f) else Muted
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (completed) PlanGreen else Color.White,
        border = if (completed) null else BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reports_step, summary.status.step),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (completed) Color.White.copy(alpha = 0.18f) else ChipPlannedBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (completed) Color.White else ChipPlannedText,
                )
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (completed) Color.White.copy(alpha = 0.18f) else GreenMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        summary.status.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (completed) Color.White else PlanGreen,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(summary.count.toString(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = primary)
                Text(stringResource(R.string.reports_dlis_unit), fontSize = 11.sp, color = secondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                stringResource(summary.status.titleRes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(summary.status.ownerRes), fontSize = 11.sp, color = secondary)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (completed) Color.White.copy(alpha = 0.14f) else Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(11.dp), tint = secondary)
                Text(
                    text = if (completed) {
                        summary.avgTurnaroundDays
                            ?.let { stringResource(R.string.reports_avg_turnaround, "%.1f".format(it)) }
                            ?: stringResource(R.string.reports_avg_turnaround_none)
                    } else {
                        stringResource(R.string.reports_oldest_avg, summary.oldestDays, summary.avgDays)
                    },
                    fontSize = 10.sp,
                    color = if (completed) Color.White else ChipPlannedText,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReportsPreview() = LedureCareTheme {
    ReportsScreen(
        state = ReportsUiState(
            isLoading = false,
            summaries = ReturnStatus.entries.mapIndexed { i, s -> StageSummary(s, i, i * 2, i) },
        ),
        onOpenStage = {},
        onOpenSearch = {},
        onRetry = {},
    )
}
