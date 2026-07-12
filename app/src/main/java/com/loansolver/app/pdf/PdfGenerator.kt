package com.loansolver.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.loansolver.app.model.EMIState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  PdfGenerator — Builds an A4 EMI report and returns a shareable Uri.
//
//  HOW IT WORKS:
//    1. We create an android.graphics.pdf.PdfDocument (built-in, no library needed).
//    2. We draw text + shapes on a Canvas — exactly like drawing to a View.
//    3. We write the finished PDF to the app's cacheDir (private, auto-cleaned).
//    4. FileProvider converts the private file path into a content:// Uri
//       that other apps (WhatsApp, Gmail, Files…) are allowed to read.
//
//  LAYOUT RULE:
//    All font sizes below are true PDF points — never scale textSize
//    independently of the row positions, or text will overlap/overflow.
//    Vertical positions advance through a running `y` cursor so spacing
//    always follows the actual text heights.
// ─────────────────────────────────────────────────────────────────────────────

object PdfGenerator {

    // A4 paper in PDF points (1 point = 1/72 inch)
    private const val PAGE_WIDTH  = 595f
    private const val PAGE_HEIGHT = 842f

    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    // Light report palette — prints cleanly and reads well in every viewer
    private val COL_TEXT       = Color.parseColor("#1C1C1E")   // near-black
    private val COL_GRAY       = Color.parseColor("#6E6E73")   // secondary text
    private val COL_LIGHT_GRAY = Color.parseColor("#AEAEB2")   // footer text
    private val COL_CARD       = Color.parseColor("#F6F6F8")   // card fill
    private val COL_BORDER     = Color.parseColor("#E2E2E7")   // card border / dividers
    private val COL_BLUE       = Color.parseColor("#1D6ADE")   // brand / EMI highlight
    private val COL_AMBER      = Color.parseColor("#B26A00")   // interest
    private val COL_GREEN      = Color.parseColor("#1E8E3E")   // total amount

    /**
     * Creates the PDF and returns a content:// Uri ready for ACTION_SEND.
     */
    fun create(context: Context, state: EMIState): Uri {
        val document = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1
        ).create()

        val page = document.startPage(pageInfo)
        drawReport(page.canvas, state)
        document.finishPage(page)

        // Write to cache — filename is static so repeated shares just overwrite
        val file = File(context.cacheDir, "emi_report.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private fun drawReport(canvas: Canvas, state: EMIState) {
        canvas.drawColor(Color.WHITE)

        var y = 64f

        // ── Header ────────────────────────────────────────────────────────────
        canvas.drawText(
            "LOANSOLVER",
            MARGIN, y,
            textPaint(COL_BLUE, 11f, bold = true, letterSpaced = true)
        )
        y += 30f

        canvas.drawText(
            "Loan Summary Report",
            MARGIN, y,
            textPaint(COL_TEXT, 24f, bold = true)
        )

        val date = SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText(
            date,
            PAGE_WIDTH - MARGIN, y,
            textPaint(COL_GRAY, 10f, align = Paint.Align.RIGHT)
        )
        y += 24f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, strokePaint(COL_BORDER, 1f))
        y += 36f

        // ── Loan details card ─────────────────────────────────────────────────
        val period = if (state.isYears) "${state.period} Years" else "${state.period} Months"
        y = drawDataCard(
            canvas, y, "LOAN DETAILS",
            listOf(
                Row("Loan Amount",   rupee(state.amount), COL_TEXT),
                Row("Interest Rate", "${state.interestRate} % p.a.", COL_TEXT),
                Row("Loan Period",   period, COL_TEXT)
            )
        )
        y += 28f

        // ── Results card ──────────────────────────────────────────────────────
        y = drawDataCard(
            canvas, y, "CALCULATION RESULTS",
            listOf(
                Row("Monthly EMI",    rupee(state.emi),           COL_BLUE, large = true),
                Row("Total Interest", rupee(state.totalInterest), COL_AMBER),
                Row("Total Amount",   rupee(state.totalAmount),   COL_GREEN)
            )
        )
        y += 40f

        // ── Payment breakdown bar (principal vs interest) ─────────────────────
        y = drawBreakdown(canvas, y, state)

        // ── Footer ────────────────────────────────────────────────────────────
        val footerY = PAGE_HEIGHT - 46f
        canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, strokePaint(COL_BORDER, 1f))
        canvas.drawText(
            "Generated by LoanSolver",
            MARGIN, footerY + 20f,
            textPaint(COL_LIGHT_GRAY, 9f)
        )
        canvas.drawText(
            "For reference only — actual figures may vary by lender",
            PAGE_WIDTH - MARGIN, footerY + 20f,
            textPaint(COL_LIGHT_GRAY, 9f, align = Paint.Align.RIGHT)
        )
    }

    private data class Row(
        val label: String,
        val value: String,
        val valueColor: Int,
        val large: Boolean = false
    )

    /**
     * Draws a rounded card with a section label and label/value rows.
     * Returns the y position just below the card.
     */
    private fun drawDataCard(canvas: Canvas, top: Float, title: String, rows: List<Row>): Float {
        val pad = 20f
        val titleHeight = 30f
        val rowHeights = rows.map { if (it.large) 40f else 32f }
        val cardHeight = pad + titleHeight + rowHeights.sum() + pad - 8f

        val rect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, top + cardHeight)
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint(COL_CARD))
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint(COL_BORDER, 1f))

        var y = top + pad + 6f
        canvas.drawText(title, MARGIN + pad, y, textPaint(COL_BLUE, 9f, bold = true, letterSpaced = true))
        y += titleHeight - 6f

        rows.forEachIndexed { i, row ->
            val h = rowHeights[i]
            val size = if (row.large) 16f else 12f
            // Baseline centred inside the row band
            val baseline = y + h / 2f + size / 3f

            canvas.drawText(row.label, MARGIN + pad, baseline, textPaint(COL_GRAY, 11f))
            canvas.drawText(
                row.value,
                PAGE_WIDTH - MARGIN - pad, baseline,
                textPaint(row.valueColor, size, bold = row.large, align = Paint.Align.RIGHT)
            )

            if (i < rows.lastIndex) {
                canvas.drawLine(
                    MARGIN + pad, y + h,
                    PAGE_WIDTH - MARGIN - pad, y + h,
                    strokePaint(COL_BORDER, 0.75f)
                )
            }
            y += h
        }

        return top + cardHeight
    }

    /**
     * Horizontal stacked bar showing principal vs interest share of the total
     * repayment. Skipped silently if the values can't be parsed.
     */
    private fun drawBreakdown(canvas: Canvas, top: Float, state: EMIState): Float {
        val principal = parseAmount(state.amount) ?: return top
        val interest  = parseAmount(state.totalInterest) ?: return top
        val total     = principal + interest
        if (principal <= 0 || interest < 0 || total <= 0) return top

        var y = top
        canvas.drawText(
            "PAYMENT BREAKDOWN",
            MARGIN, y,
            textPaint(COL_BLUE, 9f, bold = true, letterSpaced = true)
        )
        y += 18f

        val barHeight = 14f
        val principalWidth = (principal / total * CONTENT_WIDTH).toFloat()

        // Principal segment (left, blue) + interest segment (right, amber)
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + barHeight),
            4f, 4f, fillPaint(Color.parseColor("#F0B429"))
        )
        canvas.drawRoundRect(
            RectF(MARGIN, y, MARGIN + principalWidth, y + barHeight),
            4f, 4f, fillPaint(COL_BLUE)
        )
        y += barHeight + 22f

        val pctPrincipal = (principal / total * 100).toInt()
        val pctInterest  = 100 - pctPrincipal

        // Legend: coloured squares + labels
        val sq = 8f
        canvas.drawRect(MARGIN, y - sq, MARGIN + sq, y, fillPaint(COL_BLUE))
        canvas.drawText(
            "Principal  $pctPrincipal%",
            MARGIN + sq + 8f, y,
            textPaint(COL_GRAY, 10f)
        )

        val legendX2 = MARGIN + 150f
        canvas.drawRect(legendX2, y - sq, legendX2 + sq, y, fillPaint(Color.parseColor("#F0B429")))
        canvas.drawText(
            "Interest  $pctInterest%",
            legendX2 + sq + 8f, y,
            textPaint(COL_GRAY, 10f)
        )

        return y + 16f
    }

    /** "12,34,567.89" → 1234567.89; null if blank/unparseable */
    private fun parseAmount(text: String): Double? =
        text.replace(",", "").trim().toDoubleOrNull()

    private fun rupee(value: String) = "₹ $value"

    // ── Paint factories ───────────────────────────────────────────────────────

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style      = Paint.Style.FILL
    }

    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color  = color
        style       = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun textPaint(
        color: Int,
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpaced: Boolean = false
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize   = size            // true PDF points — positions depend on this
        typeface   = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        textAlign  = align
        if (letterSpaced) letterSpacing = 0.12f
    }
}
