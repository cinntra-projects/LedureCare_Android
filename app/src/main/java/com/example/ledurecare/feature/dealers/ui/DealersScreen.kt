package com.example.ledurecare.feature.dealers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureTextField
import com.example.ledurecare.core.ui.shortDate
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.ui.theme.BpCodeText
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.LedureCareTheme
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import com.example.ledurecare.ui.theme.PlanGreen
import java.time.LocalDate

@Composable
fun DealersRoute(viewModel: DealersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DealersScreen(state, viewModel::onEvent)
}

@Composable
fun DealersScreen(state: DealersUiState, onEvent: (DealersEvent) -> Unit, modifier: Modifier = Modifier) {
    val search = state.search
    val rowsById = remember(state.rows) { state.rows.associateBy { it.dealer.id } }

    Column(
        modifier = modifier.fillMaxSize().background(PageBg).padding(horizontal = 12.dp).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.dealers_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (search.hasLoaded && search.error == null) {
                Text(
                    pluralStringResource(R.plurals.dealers_count, search.totalCount, search.totalCount),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        }

        LedureTextField(
            value = search.query,
            onValueChange = { onEvent(DealersEvent.QueryChanged(it)) },
            placeholder = stringResource(R.string.dealers_search_hint),
            leadingIcon = Icons.Filled.Search,
        )

        DealerSearchList(
            search = search,
            onLoadMore = { onEvent(DealersEvent.LoadMore) },
            onRetry = { onEvent(DealersEvent.Retry) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) { dealer ->
            rowsById[dealer.id]?.let { DealerCard(it) }
        }
    }
}

@Composable
private fun DealerCard(row: DealerRowUi) {
    val dealer = row.dealer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GreenMint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    dealer.name.firstOrNull()?.uppercase().orEmpty(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlanGreen,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dealer.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(dealer.bpCode, dealer.city).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BpCodeText,
                )
                Text(
                    row.lastAudit?.let { stringResource(R.string.dealers_last_audit, it.shortDate()) }
                        ?: stringResource(R.string.dealers_no_audit),
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            if (row.planned != null) {
                Text(
                    stringResource(R.string.dealers_planned, row.planned.shortDate()),
                    modifier = Modifier.clip(CircleShape).background(GreenMint).padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlanGreen,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun DealersPreview() = LedureCareTheme {
    val today = LocalDate.of(2026, 10, 28)
    fun dealer(id: String, name: String, bp: String, city: String) = Dealer(id, name, bp, city)
    val rows = listOf(
        DealerRowUi(dealer("3", "ABM Electrical", "BP-21222", "Bijnor"), LocalDate.of(2026, 10, 2), today),
        DealerRowUi(dealer("1", "Amit Electric", "BP-23125", "Noida"), today, null),
        DealerRowUi(dealer("4", "Verma Motors", "BP-22310", "Ghaziabad"), null, null),
    )
    DealersScreen(
        state = DealersUiState(
            today = today,
            search = DealerSearchState(items = rows.map { it.dealer }, totalCount = 24, page = 1, hasMore = true, hasLoaded = true),
            rows = rows,
        ),
        onEvent = {},
    )
}
