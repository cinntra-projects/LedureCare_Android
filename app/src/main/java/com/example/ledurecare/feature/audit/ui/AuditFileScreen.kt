package com.example.ledurecare.feature.audit.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.BackButton
import com.example.ledurecare.core.ui.ErrorBox
import com.example.ledurecare.core.ui.LoadingBox
import com.example.ledurecare.core.ui.numericDate
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.SubmittedAudit
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg
import com.example.ledurecare.ui.theme.PlanGreen
import com.example.ledurecare.ui.theme.TabTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuditFileRoute(onBack: () -> Unit, viewModel: SubmittedAuditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun toast(message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    AuditFileScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onDownload = {
            val audit = state.audit ?: return@AuditFileScreen
            val viewed = state.viewed ?: return@AuditFileScreen
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    when (viewed) {
                        ViewedFile.AuditForm -> context.downloadAuditForm(audit)
                        is ViewedFile.Attachment -> context.downloadAttachment(viewed.file)
                    }
                }
                toast(if (saved) R.string.viewer_saved else R.string.viewer_save_failed)
            }
        },
        onShare = {
            val audit = state.audit ?: return@AuditFileScreen
            val viewed = state.viewed ?: return@AuditFileScreen
            scope.launch {
                val shared = withContext(Dispatchers.IO) {
                    when (viewed) {
                        ViewedFile.AuditForm -> context.shareAuditForm(audit)
                        is ViewedFile.Attachment -> context.shareAttachment(viewed.file)
                    }
                }
                if (!shared) toast(R.string.viewer_share_failed)
            }
        },
    )
}

@Composable
fun AuditFileScreen(
    state: SubmittedUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audit = state.audit
    val viewed = state.viewed
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
            Column(Modifier.weight(1f)) {
                if (audit != null && viewed != null) {
                    Text(viewerTitle(viewed), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(fileName(audit, viewed), fontSize = 11.sp, color = Muted, maxLines = 1)
                }
            }
            if (audit != null && viewed != null) {
                HeaderAction(Icons.Outlined.FileDownload, stringResource(R.string.viewer_download), onDownload)
                HeaderAction(Icons.Outlined.Share, stringResource(R.string.viewer_share), onShare)
            }
        }

        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry)
            audit != null && viewed != null -> {
                when (viewed) {
                    ViewedFile.AuditForm -> AuditDocument(audit)
                    is ViewedFile.Attachment -> AttachmentPreview(viewed.file)
                }
                DownloadButton(
                    text = stringResource(
                        if (viewed == ViewedFile.AuditForm) R.string.viewer_download_pdf else R.string.viewer_download_file
                    ),
                    onClick = onDownload,
                )
            }
        }
    }
}

@Composable
private fun viewerTitle(viewed: ViewedFile): String = stringResource(
    when {
        viewed == ViewedFile.AuditForm -> R.string.viewer_title_form
        viewed is ViewedFile.Attachment && viewed.file.mimeType.startsWith("image", ignoreCase = true) -> R.string.viewer_title_photo
        else -> R.string.viewer_title_file
    }
)

private fun fileName(audit: SubmittedAudit, viewed: ViewedFile): String = when (viewed) {
    ViewedFile.AuditForm -> audit.form.name
    is ViewedFile.Attachment -> viewed.file.name
}

@Composable
private fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp), tint = PlanGreen)
        }
    }
}

@Composable
private fun DownloadButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = PlanGreen,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ---------------------------------------------------------------- the audit form as a document

@Composable
private fun PaperCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, FieldBorder),
    ) { content() }
}

@Composable
private fun AuditDocument(audit: SubmittedAudit) {
    PaperCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.background(Green500, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ledure_logo),
                        contentDescription = stringResource(R.string.login_logo_description),
                        modifier = Modifier.height(11.dp),
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.doc_title), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(
                        stringResource(R.string.doc_title_meta, audit.dliNo, audit.inspectionDate.numericDate()),
                        fontSize = 9.sp,
                        color = Muted,
                    )
                }
            }
            Divider(color = FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                DocRow(stringResource(R.string.doc_dealer), stringResource(R.string.doc_dealer_value, audit.dealerName, audit.bpCode))
                DocRow(stringResource(R.string.doc_address), audit.address)
                DocRow(stringResource(R.string.doc_date), audit.inspectionDate.numericDate())
                DocRow(stringResource(R.string.doc_audited_by), stringResource(R.string.doc_auditor_value, audit.auditedBy, audit.auditorRole))
            }
            Divider(color = FieldBorder)

            Text(stringResource(R.string.doc_items), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
            ItemsTable(audit)
            Divider(color = FieldBorder)

            Text(stringResource(R.string.doc_notes), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(audit.notes.ifBlank { "—" }, fontSize = 9.sp, color = Muted)

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Signature(audit.auditedBy, audit.auditorRole, Modifier.weight(1f))
                Signature(audit.dealerName, stringResource(R.string.doc_signature_dealer), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DocRow(label: String, value: String) {
    Row {
        Text(label, modifier = Modifier.weight(0.38f), fontSize = 9.sp, color = Muted)
        Text(value, modifier = Modifier.weight(0.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

private val TableWeights = listOf(3.2f, 0.8f, 0.9f, 0.8f, 1.6f)

@Composable
private fun ItemsTable(audit: SubmittedAudit) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color.White, border = BorderStroke(1.dp, FieldBorder)) {
        Column {
            TableRow(
                cells = listOf(
                    stringResource(R.string.doc_col_product),
                    stringResource(R.string.doc_col_qty),
                    stringResource(R.string.doc_col_approved),
                    stringResource(R.string.doc_col_rejected),
                    stringResource(R.string.doc_col_condition),
                ),
                header = true,
            )
            audit.items.forEach { item ->
                Divider(color = FieldBorder)
                TableRow(
                    cells = listOf(
                        stringResource(R.string.doc_product_value, item.product.code, item.product.name),
                        item.qty.toString(),
                        item.approved.toString(),
                        item.rejected.toString(),
                        item.condition,
                    ),
                    header = false,
                )
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(if (header) TabTrack else Color.Transparent).padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        cells.forEachIndexed { index, text ->
            Text(
                text,
                modifier = Modifier.weight(TableWeights[index]),
                fontSize = 8.sp,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                color = if (header) Muted else Ink,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun Signature(name: String, role: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Ink)
        Divider(color = Ink)
        Text(role, fontSize = 8.sp, color = Muted)
    }
}

// ---------------------------------------------------------------- attached photos and PDFs

@Composable
private fun AttachmentPreview(file: AuditFile) {
    PaperCard {
        Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
            when {
                file.mimeType.startsWith("image", ignoreCase = true) -> AsyncImage(
                    model = Uri.parse(file.uri),
                    contentDescription = stringResource(R.string.viewer_photo_description),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
                file.isPdf() -> PdfFirstPage(file)
                else -> UnavailablePreview()
            }
        }
    }
}

@Composable
private fun PdfFirstPage(file: AuditFile) {
    val context = LocalContext.current
    val page by produceState<Bitmap?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) { context.renderFirstPage(Uri.parse(file.uri)) }
    }
    val bitmap = page
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = file.name,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        UnavailablePreview()
    }
}

@Composable
private fun UnavailablePreview() {
    Text(
        stringResource(R.string.viewer_preview_unavailable),
        modifier = Modifier.padding(vertical = 40.dp),
        fontSize = 12.sp,
        color = Muted,
    )
}

/** Renders page one of a PDF, or null if it cannot be opened. */
private fun Context.renderFirstPage(uri: Uri): Bitmap? = runCatching {
    contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(0).use { page ->
                Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }
}.getOrNull()
