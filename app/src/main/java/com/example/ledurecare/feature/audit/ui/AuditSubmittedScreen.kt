package com.example.ledurecare.feature.audit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.ErrorBox
import com.example.ledurecare.core.ui.LoadingBox
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.SubmittedAudit
import com.example.ledurecare.ui.theme.BpCodeBg
import com.example.ledurecare.ui.theme.BpCodeText
import com.example.ledurecare.ui.theme.CopiedBg
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.FieldBg
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.Green600
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import com.example.ledurecare.ui.theme.PdfRedBg
import com.example.ledurecare.ui.theme.PlanGreen

@Composable
fun AuditSubmittedRoute(
    onBackToPlan: () -> Unit,
    onOpenFile: (dliNo: String, fileIndex: Int) -> Unit,
    viewModel: SubmittedAuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuditSubmittedScreen(state, onBackToPlan, onOpenFile, viewModel::retry)
}

@Composable
fun AuditSubmittedScreen(
    state: SubmittedUiState,
    onBackToPlan: () -> Unit,
    onOpenFile: (dliNo: String, fileIndex: Int) -> Unit,
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
        val audit = state.audit
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry)
            audit != null -> {
                Confirmation(audit)
                FormCard(audit) { onOpenFile(audit.dliNo, AUDIT_FORM_INDEX) }
                if (audit.attachments.isNotEmpty()) {
                    AttachmentsCard(audit.attachments) { index -> onOpenFile(audit.dliNo, index) }
                }
                BackToPlanButton(onBackToPlan)
            }
        }
    }
}

@Composable
private fun Confirmation(audit: SubmittedAudit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(GreenMint), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(40.dp), tint = Green600)
        }
        Text(
            stringResource(R.string.submitted_title),
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        Text(
            stringResource(R.string.submitted_dli_dealer, audit.dliNo, audit.dealerName),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PlanGreen,
        )
        Text(stringResource(R.string.submitted_sent_to), fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) { content() }
}

@Composable
private fun FormCard(audit: SubmittedAudit, onView: () -> Unit) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.submitted_form_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(stringResource(R.string.submitted_form_hint), fontSize = 11.sp, color = Muted)
            }
            FileTile(
                name = audit.form.name,
                meta = stringResource(R.string.submitted_form_meta, formatFileSize(audit.form.sizeBytes), audit.form.pages),
                pdf = true,
                highlighted = true,
                onView = onView,
            )
        }
    }
}

@Composable
private fun AttachmentsCard(files: List<AuditFile>, onView: (index: Int) -> Unit) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.submitted_attachments, files.size),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            files.forEachIndexed { index, file ->
                FileTile(
                    name = file.name,
                    meta = stringResource(R.string.submitted_file_meta, formatFileSize(file.sizeBytes), kindLabel(file)),
                    pdf = file.isPdf(),
                    highlighted = false,
                    onView = { onView(index) },
                )
            }
        }
    }
}

@Composable
private fun kindLabel(file: AuditFile): String = stringResource(
    when {
        file.isPdf() -> R.string.detail_kind_pdf
        file.mimeType.startsWith("image", ignoreCase = true) -> R.string.detail_kind_photo
        else -> R.string.detail_kind_file
    }
)

@Composable
private fun FileTile(name: String, meta: String, pdf: Boolean, highlighted: Boolean, onView: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) CopiedBg else FieldBg,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Green500 else FieldBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (pdf) PdfRedBg else BpCodeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (pdf) Icons.Outlined.Description else Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (pdf) ErrorRed else BpCodeText,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, fontSize = 11.sp, color = Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(15.dp), tint = PlanGreen)
                Text(stringResource(R.string.submitted_view), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
            }
        }
    }
}

@Composable
private fun BackToPlanButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = PlanGreen,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.submitted_back), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
