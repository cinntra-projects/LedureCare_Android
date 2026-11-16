package com.example.ledurecare.feature.dealers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.asMessage
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.Muted

/** How close to the end of the list the next page starts loading, in rows. */
private const val LOAD_MORE_THRESHOLD = 4

/**
 * Shows a [DealerSearchState]: a spinner for the first page, an error with Retry, "no dealers",
 * or the rows with a spinner under them while the next page loads. Scrolling near the end asks for
 * the next page through [onLoadMore]. [dealers] defaults to the loaded items; pass a changed list
 * to hide or add rows without touching the search itself.
 */
@Composable
fun DealerSearchList(
    search: DealerSearchState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    dealers: List<Dealer> = search.items,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    row: @Composable (Dealer) -> Unit,
) {
    when {
        dealers.isEmpty() && (search.isLoading || !search.hasLoaded) -> CenteredBox(modifier, 120.dp) {
            CircularProgressIndicator(color = Green500)
        }
        dealers.isEmpty() && search.error != null -> Column(
            modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(search.error.asMessage(), fontSize = 12.sp, color = Muted)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.sheet_retry)) }
        }
        dealers.isEmpty() && !search.hasMore -> CenteredBox(modifier, 80.dp) {
            Text(stringResource(R.string.sheet_empty), fontSize = 12.sp, color = Muted)
        }
        else -> {
            val listState = rememberLazyListState()
            val nearEnd by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
                }
            }
            // Also fires when every row fits on screen (nothing to scroll), until the pages run out.
            LaunchedEffect(nearEnd, dealers.size, search.hasMore, search.isLoadingMore) {
                if (nearEnd && search.hasMore) onLoadMore()
            }

            LazyColumn(
                modifier = modifier,
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = contentPadding,
            ) {
                items(dealers, key = { it.id }) { dealer -> row(dealer) }
                if (search.isLoadingMore) {
                    item(key = "loading-more") {
                        CenteredBox(Modifier, 48.dp) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Green500, strokeWidth = 2.dp)
                        }
                    }
                }
                if (search.error != null) {
                    item(key = "load-more-error") {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(search.error.asMessage(), fontSize = 12.sp, color = Muted)
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.sheet_retry)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, height: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { content() }
}
