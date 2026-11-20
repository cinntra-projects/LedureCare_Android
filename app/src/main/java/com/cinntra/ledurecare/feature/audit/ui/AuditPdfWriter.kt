package com.cinntra.ledurecare.feature.audit.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cinntra.ledurecare.core.ui.numericDate
import com.cinntra.ledurecare.feature.audit.domain.SubmittedAudit
import java.io.OutputStream

/**
 * Draws the audit form as an A4 PDF. The wording is the document's own, not app UI,
 * so it is written here rather than in the string resources.
 */
object AuditPdfWriter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private val Green = 0xFF22C55E.toInt()
    private val Ink = 0xFF053F2A.toInt()
    private val Muted = 0xFF6B7280.toInt()
    private val Line = 0xFFE5E7EB.toInt()
    private val HeaderBg = 0xFFF3F4F6.toInt()

    fun write(audit: SubmittedAudit, out: OutputStream) {
        val doc = PdfDocument()
        val pager = Pager(doc)
        try {
            draw(audit, pager)
            pager.finish()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private class Pager(private val doc: PdfDocument) {
        private var number = 0
        private var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = MARGIN

        init {
            next()
        }

        fun next() {
            finish()
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
            canvas = page!!.canvas
            y = MARGIN
        }

        /** Starts a new page if [height] more points will not fit. */
        fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) next()
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun draw(audit: SubmittedAudit, p: Pager) {
        val right = PAGE_WIDTH - MARGIN
        val rule = Paint().apply { color = Line; strokeWidth = 1f }

        // Header: logo block on the left, form title on the right.
        p.canvas.drawRoundRect(RectF(MARGIN, p.y, MARGIN + 96f, p.y + 26f), 5f, 5f, Paint().apply { color = Green })
        p.canvas.drawText("LEDURE", MARGIN + 10f, p.y + 18f, paint(14f, 0xFFFFFFFF.toInt(), bold = true))
        val title = paint(15f, Ink, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val meta = paint(10f, Muted).apply { textAlign = Paint.Align.RIGHT }
        p.canvas.drawText("DLI AUDIT FORM", right, p.y + 14f, title)
        p.canvas.drawText("${audit.dliNo} · ${audit.inspectionDate.numericDate()}", right, p.y + 28f, meta)
        p.y += 44f
        p.canvas.drawLine(MARGIN, p.y, right, p.y, rule)
        p.y += 20f

        // Key facts.
        val label = paint(10f, Muted)
        val value = paint(10f, Ink, bold = true)
        listOf(
            "Dealer" to "${audit.dealerName} (${audit.bpCode})",
            "Address" to audit.address,
            "Date of inspection" to audit.inspectionDate.numericDate(),
            "Audited by" to "${audit.auditedBy} (${audit.auditorRole})",
        ).forEach { (name, text) ->
            p.canvas.drawText(name, MARGIN, p.y, label)
            p.canvas.drawText(text, MARGIN + 120f, p.y, value)
            p.y += 18f
        }
        p.y += 4f
        p.canvas.drawLine(MARGIN, p.y, right, p.y, rule)
        p.y += 20f

        // Items table.
        p.canvas.drawText("Audited items", MARGIN, p.y, paint(12f, Ink, bold = true))
        p.y += 14f
        val columns = floatArrayOf(MARGIN + 6f, 330f, 385f, 435f, 475f)
        val head = paint(9f, Muted, bold = true)
        val cell = paint(9f, Ink)

        fun row(texts: List<String>, bold: Boolean) {
            val painter = if (bold) head else cell
            texts.forEachIndexed { i, t -> p.canvas.drawText(t, columns[i], p.y + 13f, painter) }
        }

        p.canvas.drawRect(MARGIN, p.y, right, p.y + 20f, Paint().apply { color = HeaderBg })
        row(listOf("Product", "Qty", "Appr.", "Rej.", "Condition"), bold = true)
        p.y += 20f
        audit.items.forEach { item ->
            p.ensure(20f)
            row(
                listOf(
                    "${item.product.code} ${item.product.name}".take(46),
                    item.qty.toString(),
                    item.approved.toString(),
                    item.rejected.toString(),
                    item.condition,
                ),
                bold = false,
            )
            p.y += 20f
            p.canvas.drawLine(MARGIN, p.y, right, p.y, rule)
        }
        p.y += 22f

        // Notes, wrapped to the page width.
        p.ensure(80f)
        p.canvas.drawText("Audit notes", MARGIN, p.y, paint(12f, Ink, bold = true))
        p.y += 16f
        val body = paint(10f, Muted)
        wrap(audit.notes.ifBlank { "—" }, body, right - MARGIN).forEach { line ->
            p.ensure(14f)
            p.canvas.drawText(line, MARGIN, p.y, body)
            p.y += 14f
        }

        // Signatures.
        p.ensure(90f)
        p.y += 44f
        val half = (right - MARGIN - 30f) / 2
        p.canvas.drawLine(MARGIN, p.y, MARGIN + half, p.y, rule)
        p.canvas.drawLine(right - half, p.y, right, p.y, rule)
        p.canvas.drawText(audit.auditedBy, MARGIN, p.y + 14f, paint(10f, Ink, bold = true))
        p.canvas.drawText(audit.auditorRole, MARGIN, p.y + 27f, paint(9f, Muted))
        p.canvas.drawText(audit.dealerName, right - half, p.y + 14f, paint(10f, Ink, bold = true))
        p.canvas.drawText("Dealer", right - half, p.y + 27f, paint(9f, Muted))
    }

    /** Greedy word wrap that also honours explicit line breaks. */
    private fun wrap(text: String, paint: Paint, width: Float): List<String> = text.lines().flatMap { paragraph ->
        val lines = mutableListOf<String>()
        var current = ""
        paragraph.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= width || current.isEmpty()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        lines += current
        lines
    }
}
