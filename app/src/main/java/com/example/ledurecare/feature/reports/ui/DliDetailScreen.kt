package com.example.ledurecare.feature.reports.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledurecare.BuildConfig
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.BackButton
import com.example.ledurecare.core.ui.ErrorBox
import com.example.ledurecare.core.ui.LoadingBox
import com.example.ledurecare.core.ui.numericDate
import com.example.ledurecare.core.ui.shortDate
import com.example.ledurecare.feature.reports.domain.Attachment
import com.example.ledurecare.feature.reports.domain.AttachmentKind
import com.example.ledurecare.feature.reports.domain.DliDetail
import com.example.ledurecare.feature.reports.domain.DliItem
import com.example.ledurecare.feature.reports.domain.StepState
import com.example.ledurecare.feature.reports.domain.TrackerStep
import com.example.ledurecare.ui.theme.BpCodeBg
import com.example.ledurecare.ui.theme.BpCodeText
import com.example.ledurecare.ui.theme.ErrorBg
import com.example.ledurecare.ui.theme.ErrorBorder
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.FieldBg
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.Green600
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.GreenMintBorder
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import com.example.ledurecare.ui.theme.PdfRedBg
import com.example.ledurecare.ui.theme.Placeholder
import com.example.ledurecare.ui.theme.PlanGreen

@Composable
fun DliDetailRoute(
    onBack: () -> Unit,
    viewModel: DliDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cannotOpen = stringResource(R.string.detail_open_failed)

    DliDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOpenAttachment = { attachment ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolveUrl(attachment.url))))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, cannotOpen, Toast.LENGTH_SHORT).show()
            }
        },
    )
}

/** Attachment links may be relative to the API host. */
private fun resolveUrl(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url
    else BuildConfig.BASE_URL.trimEnd('/') + "/" + url.trimStart('/')

@Composable
fun DliDetailScreen(
    state: DliDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            if (detail != null) {
                Column {
                    Text(
                        detail.dliNo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (detail.isOverSla) ErrorRed else Ink,
                    )
                    Text(detail.dealerName, fontSize = 12.sp, color = Muted)
                }
            }
        }

        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry)
            detail != null -> {
                StageBanner(detail)
                TrackerCard(detail)
                DetailsCard(detail)
                if (detail.items.isNotEmpty()) ItemsCard(detail.items)
                if (detail.attachments.isNotEmpty()) AttachmentsCard(detail.attachments, onOpenAttachment)
            }
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
}

@Composable
private fun StageBanner(detail: DliDetail) {
    val alert = detail.isOverSla
    val accent = if (alert) ErrorRed else PlanGreen
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (alert) ErrorBg else GreenMint,
        border = BorderStroke(1.dp, if (alert) ErrorBorder else GreenMintBorder),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.detail_now, stringResource(trackerLabelRes(detail.status))),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                val days = pluralStringResource(R.plurals.dli_days, detail.daysInStage, detail.daysInStage)
                Text(
                    detail.stageSince?.let { stringResource(R.string.detail_in_stage, days, it.shortDate()) }
                        ?: stringResource(R.string.detail_in_stage_no_date, days),
                    fontSize = 11.sp,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun TrackerCard(detail: DliDetail) {
    SectionCard {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            SectionTitle(stringResource(R.string.detail_tracker))
            Column(Modifier.padding(top = 8.dp)) {
                val steps = detail.tracker
                steps.forEachIndexed { index, step ->
                    TrackerRow(
                        step = step,
                        alert = detail.isOverSla,
                        lineAbove = if (index > 0) lineColor(step.state) else null,
                        lineBelow = steps.getOrNull(index + 1)?.let { lineColor(it.state) },
                    )
                }
            }
        }
    }
}

private fun lineColor(next: StepState): Color = if (next == StepState.Upcoming) FieldBorder else Green500

@Composable
private fun TrackerRow(step: TrackerStep, alert: Boolean, lineAbove: Color?, lineBelow: Color?) {
    val current = step.state == StepState.Current
    val upcoming = step.state == StepState.Upcoming
    val accent = if (current && alert) ErrorRed else Green500

    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val mid = size.height / 2
                    val stroke = 2.dp.toPx()
                    lineAbove?.let { drawLine(it, Offset(x, 0f), Offset(x, mid), stroke) }
                    lineBelow?.let { drawLine(it, Offset(x, mid), Offset(x, size.height), stroke) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (upcoming) Color.White else accent)
                    .then(if (upcoming) Modifier.border(1.5.dp, FieldBorder, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (step.state == StepState.Done) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(9.dp), tint = Color.White)
                }
            }
        }
        Text(
            stringResource(trackerLabelRes(step.status)),
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            fontSize = 12.sp,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
            color = when {
                current -> accent
                upcoming -> Placeholder
                else -> Ink
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = when {
                upcoming || step.date == null -> "—"
                current -> stringResource(R.string.detail_since, step.date.shortDate())
                else -> step.date.shortDate()
            },
            fontSize = 11.sp,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) accent else Muted,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DetailsCard(detail: DliDetail) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.detail_details))
            DetailRow(stringResource(R.string.detail_customer_code), detail.customerCode)
            DetailRow(
                stringResource(R.string.detail_audited_by),
                listOfNotNull(detail.auditedBy, detail.auditDate?.numericDate()).joinToString(" · "),
            )
            DetailRow(stringResource(R.string.detail_box_qty), "${detail.totalBox} / ${detail.totalQty}")
            DetailRow(stringResource(R.string.detail_warehouse), detail.warehouse)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = Muted)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.End)
    }
}

@Composable
private fun ItemsCard(items: List<DliItem>) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.detail_audited_items, items.size))
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FieldBg)
                        .border(1.dp, FieldBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        listOf(item.code, item.name).filter { it.isNotBlank() }.joinToString(" · "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(itemQuantities(item), fontSize = 11.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun itemQuantities(item: DliItem): String = listOfNotNull(
    stringResource(R.string.detail_qty, item.qty),
    item.approved?.let { stringResource(R.string.detail_approved, it) },
    item.rejected?.let { stringResource(R.string.detail_rejected, it) },
).joinToString(" · ")

@Composable
private fun AttachmentsCard(attachments: List<Attachment>, onOpen: (Attachment) -> Unit) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionTitle(stringResource(R.string.detail_attachments, attachments.size))
                Text(stringResource(R.string.detail_attachments_hint), fontSize = 10.sp, color = Muted)
            }
            attachments.forEach { AttachmentRow(it) { onOpen(it) } }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, onOpen: () -> Unit) {
    val highlighted = attachment.isAuditForm && attachment.kind == AttachmentKind.Pdf
    val kindLabel = stringResource(
        when (attachment.kind) {
            AttachmentKind.Pdf -> R.string.detail_kind_pdf
            AttachmentKind.Image -> R.string.detail_kind_photo
            AttachmentKind.Other -> R.string.detail_kind_file
        }
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) GreenMint.copy(alpha = 0.5f) else Color.White,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Green500 else FieldBorder),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (tint, bg, icon) = when (attachment.kind) {
                AttachmentKind.Pdf -> Triple(ErrorRed, PdfRedBg, Icons.Outlined.Description)
                AttachmentKind.Image -> Triple(BpCodeText, BpCodeBg, Icons.Outlined.Image)
                AttachmentKind.Other -> Triple(Muted, FieldBg, Icons.Outlined.InsertDriveFile)
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
            }
            Column(Modifier.weight(1f)) {
                Text(attachment.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (attachment.isAuditForm) "${stringResource(R.string.detail_audit_form)} · $kindLabel" else kindLabel,
                    fontSize = 10.sp,
                    color = Muted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = Green600)
                Text(stringResource(R.string.detail_view), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Green600)
            }
        }
    }
}
