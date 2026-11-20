package com.cinntra.ledurecare.feature.audit.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.cinntra.ledurecare.feature.audit.domain.AuditFile
import com.cinntra.ledurecare.feature.audit.domain.SubmittedAudit
import java.io.File
import java.io.OutputStream

private const val PDF = "application/pdf"

/** Saves a file into the public Downloads folder (or the app's own Downloads folder before Android 10). */
internal fun Context.saveToDownloads(fileName: String, mimeType: String, write: (OutputStream) -> Unit): Boolean =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val target = checkNotNull(contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            checkNotNull(contentResolver.openOutputStream(target)).use(write)
        } else {
            val dir = checkNotNull(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
            File(dir, fileName).outputStream().use(write)
        }
    }.isSuccess

private fun Context.copyTo(uri: String, out: OutputStream) {
    checkNotNull(contentResolver.openInputStream(Uri.parse(uri))).use { it.copyTo(out) }
}

internal fun Context.downloadAuditForm(audit: SubmittedAudit): Boolean =
    saveToDownloads(audit.form.name, PDF) { AuditPdfWriter.write(audit, it) }

internal fun Context.downloadAttachment(file: AuditFile): Boolean =
    saveToDownloads(file.name, file.mimeType.ifBlank { "application/octet-stream" }) { copyTo(file.uri, it) }

/** Opens the system share sheet for [uri]. */
private fun Context.share(uri: Uri, mimeType: String): Boolean = runCatching {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess

internal fun Context.shareAuditForm(audit: SubmittedAudit): Boolean = runCatching {
    val dir = File(cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, audit.form.name)
    file.outputStream().use { AuditPdfWriter.write(audit, it) }
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}.getOrNull()?.let { share(it, PDF) } ?: false

internal fun Context.shareAttachment(file: AuditFile): Boolean =
    share(Uri.parse(file.uri), file.mimeType.ifBlank { "application/octet-stream" })
