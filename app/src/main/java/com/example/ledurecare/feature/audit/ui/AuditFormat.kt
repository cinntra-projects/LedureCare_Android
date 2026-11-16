package com.example.ledurecare.feature.audit.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.AuditItem
import com.example.ledurecare.feature.audit.domain.MfgType
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** "Mar" for a month, "Q2" for a quarter. */
fun periodLabel(type: MfgType, period: Int): String = when (type) {
    MfgType.Month -> Month.of(period).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    MfgType.Quarter -> "Q$period"
}

/** "Q2-2025" or "Mar-2025". */
fun AuditItem.mfgLabel(): String = "${periodLabel(mfgType, mfgPeriod)}-$mfgYear"

/** "340 KB" or "1.2 MB". */
fun formatFileSize(bytes: Long): String =
    if (bytes < 1024 * 1024) "${(bytes / 1024.0).roundToInt().coerceAtLeast(1)} KB"
    else "%.1f MB".format(Locale.getDefault(), bytes / (1024.0 * 1024.0))

fun AuditFile.isPdf(): Boolean = mimeType.contains("pdf", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true)

/** Reads the display name and size of a picked document. */
fun Context.toAuditFile(uri: Uri): AuditFile {
    var name: String? = null
    var size = 0L
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it) }
        }
    }
    return AuditFile(
        uri = uri.toString(),
        name = name ?: uri.lastPathSegment.orEmpty(),
        sizeBytes = size,
        mimeType = contentResolver.getType(uri).orEmpty(),
    )
}
