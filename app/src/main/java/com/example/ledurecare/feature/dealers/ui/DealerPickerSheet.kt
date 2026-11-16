package com.example.ledurecare.feature.dealers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureTextField
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PlanGreen

/**
 * A bottom sheet for choosing one dealer. The list is searched and paged on the server (see
 * [DealerSearchState]): typing calls [onQueryChange], scrolling to the end calls [onLoadMore].
 * Tapping a row picks it and closes the sheet; [selectedId] marks the current choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealerPickerSheet(
    search: DealerSearchState,
    selectedId: String?,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (Dealer) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    Text(stringResource(R.string.dealer_picker_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(stringResource(R.string.dealer_picker_subtitle), fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sheet_close), tint = Muted)
                }
            }

            LedureTextField(
                value = search.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.sheet_search_hint),
                leadingIcon = Icons.Filled.Search,
            )

            DealerSearchList(
                search = search,
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                modifier = Modifier.heightIn(max = 420.dp),
            ) { dealer ->
                DealerChoiceRow(dealer, selected = dealer.id == selectedId) { onSelect(dealer) }
            }
        }
    }
}

/** A dealer as a radio-style row; the whole row is the tap target. */
@Composable
internal fun DealerChoiceRow(dealer: Dealer, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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
            // The whole row is the tap target, so the radio only shows the state.
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(8.dp),
                colors = RadioButtonDefaults.colors(selectedColor = PlanGreen, unselectedColor = FieldBorder),
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
