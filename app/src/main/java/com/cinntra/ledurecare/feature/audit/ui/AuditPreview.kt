package com.cinntra.ledurecare.feature.audit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.ui.shortDate
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.Muted

/** A read-only summary of the whole form, shown before it is sent. */
@Composable
fun AuditPreviewContent(state: AuditUiState) {
    val dealer = state.dealer
    PreviewCard(stringResource(R.string.audit_preview_dealer)) {
        Text(dealer?.name.orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        PreviewLine(stringResource(R.string.audit_preview_code, state.customerCode))
        PreviewLine(stringResource(R.string.audit_preview_address, state.address))
        PreviewLine(stringResource(R.string.audit_preview_sales, state.salesPerson))
        if (state.zone.isNotBlank()) PreviewLine(stringResource(R.string.audit_preview_zone, state.zone))
        PreviewLine(stringResource(R.string.audit_preview_date, state.inspectionDate.shortDate()))
    }

    PreviewCard(stringResource(R.string.audit_preview_items, state.items.size)) {
        state.items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${item.product.code} · ${item.product.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                PreviewLine(
                    stringResource(R.string.audit_preview_item_meta, item.qty, item.mfgLabel(), item.condition, warrantyLabel(item.warranty))
                )
                if (item.remark.isNotBlank()) PreviewLine(stringResource(R.string.audit_preview_remark, item.remark))
            }
        }
    }

    PreviewCard(stringResource(R.string.audit_preview_totals)) {
        PreviewLine(stringResource(R.string.audit_preview_warehouse, state.warehouse?.name.orEmpty()))
        PreviewLine(stringResource(R.string.audit_preview_boxes, state.totalBox.toIntOrNull() ?: 0, state.totalQty))
    }

    PreviewCard(stringResource(R.string.audit_preview_notes)) {
        PreviewLine(state.notes.ifBlank { stringResource(R.string.audit_preview_none) })
    }

    PreviewCard(stringResource(R.string.audit_preview_files, state.files.size)) {
        if (state.files.isEmpty()) PreviewLine(stringResource(R.string.audit_preview_none))
        state.files.forEach { PreviewLine("${it.name} · ${formatFileSize(it.sizeBytes)}") }
    }
}

@Composable
private fun PreviewCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            content()
        }
    }
}

@Composable
private fun PreviewLine(text: String) = Text(text, fontSize = 12.sp, color = Muted)
