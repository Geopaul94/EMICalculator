package com.loansolver.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
//  WHY cacheDir and not filesDir?
//    cacheDir is the right place for temporary generated files. The OS will
//    clean it up when disk space is low. We don't need to keep the PDF
//    permanently — the user can regenerate it any time.
// ─────────────────────────────────────────────────────────────────────────────

object PdfGenerator {

    // A4 paper in PDF points (1 point = 1/72 inch)
    private const val PAGE_WIDTH  = 595f
    private const val PAGE_HEIGHT = 842f

    // Horizontal margins
    private const val MARGIN = 40f

    // Brand colours (same hex as Color.kt)
    private val COL_BACKGROUND = Color.parseColor("#1A1A1C")
    private val COL_SURFACE    = Color.parseColor("#2A2A2E")
    private val COL_DIVIDER    = Color.parseColor("#3A3A3C")
    private val COL_WHITE      = Color.parseColor("#E5E5E7")
    private val COL_GRAY       = Color.parseColor("#8E8E93")
    private val COL_BLUE       = Color.parseColor("#4A9EFF")
    private val COL_AMBER      = Color.parseColor("#FF9F0A")
    private val COL_GREEN      = Color.parseColor("#30D158")

    /**
     * Creates the PDF and returns a content:// Uri ready for ACTION_SEND.
     *
     * @param context  Any context (activity or application) — used for cacheDir + FileProvider.
     * @param state    Current UI state containing both inputs and calculated results.
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

        // Expose via FileProvider so external apps can read it
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private fun drawReport(canvas: Canvas, state: EMIState) {

        // ── Header band ───────────────────────────────────────────────────────
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 130f, fillPaint(COL_BACKGROUND))

        // App name (small, above title)
        canvas.drawText(
            "EMI CALCULATOR",
            MARGIN, 52f,
            textPaint(COL_BLUE, 12f, bold = true, letterSpaced = true)
        )

        // Report title
        canvas.drawText(
            "Loan Summary Report",
            MARGIN, 84f,
            textPaint(COL_WHITE, 26f, bold = true)
        )

        // Generation date
        val date = SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText(date, MARGIN, 108f, textPaint(COL_GRAY, 12f))

        // ── Inputs card ───────────────────────────────────────────────────────
        val cardTop = 150f
        drawCard(canvas, cardTop, 290f)

        sectionLabel(canvas, "LOAN DETAILS", cardTop + 28f)

        val period = if (state.isYears) "${state.period} Years" else "${state.period} Months"
        dataRow(canvas, "Loan Amount",   "₹ ${state.amount}",        cardTop + 68f,  COL_WHITE)
        divider(canvas, cardTop + 90f)
        dataRow(canvas, "Interest Rate", "${state.interestRate} % p.a.", cardTop + 118f, COL_WHITE)
        divider(canvas, cardTop + 140f)
        dataRow(canvas, "Loan Period",   period,                     cardTop + 168f, COL_WHITE)

        // ── Results card ─────────────────────────────────────────────────────
        val resTop = 318f
        drawCard(canvas, resTop, 200f)

        sectionLabel(canvas, "CALCULATION RESULTS", resTop + 28f)

        dataRow(canvas, "Monthly EMI",    "₹ ${state.emi}",           resTop + 68f,  COL_BLUE,  large = true)
        divider(canvas, resTop + 92f)
        dataRow(canvas, "Total Interest", "₹ ${state.totalInterest}", resTop + 122f, COL_AMBER)
        divider(canvas, resTop + 145f)
        dataRow(canvas, "Total Amount",   "₹ ${state.totalAmount}",   resTop + 175f, COL_GREEN)

        // ── Footer ────────────────────────────────────────────────────────────
        canvas.drawRect(0f, PAGE_HEIGHT - 42f, PAGE_WIDTH, PAGE_HEIGHT, fillPaint(COL_BACKGROUND))
        canvas.drawText(
            "Generated by EMI Calculator App",
            MARGIN, PAGE_HEIGHT - 16f,
            textPaint(COL_GRAY, 11f)
        )
        canvas.drawText(
            "For reference only",
            PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 16f,
            textPaint(COL_GRAY, 11f, align = Paint.Align.RIGHT)
        )
    }

    // ── Helper drawers ────────────────────────────────────────────────────────

    /** Rounded rectangle card background */
    private fun drawCard(canvas: Canvas, top: Float, height: Float) {
        val rect = RectF(MARGIN - 8f, top, PAGE_WIDTH - MARGIN + 8f, top + height)
        canvas.drawRoundRect(rect, 12f, 12f, fillPaint(COL_SURFACE))
        canvas.drawRoundRect(rect, 12f, 12f, strokePaint(COL_DIVIDER, 1f))
    }

    private fun sectionLabel(canvas: Canvas, text: String, y: Float) {
        canvas.drawText(text, MARGIN + 8f, y, textPaint(COL_BLUE, 10f, bold = true, letterSpaced = true))
    }

    private fun dataRow(
        canvas: Canvas,
        label: String,
        value: String,
        y: Float,
        valueColor: Int,
        large: Boolean = false
    ) {
        val size = if (large) 15f else 13f
        canvas.drawText(label, MARGIN + 8f, y, textPaint(COL_GRAY, size))
        canvas.drawText(value, PAGE_WIDTH - MARGIN - 8f, y,
            textPaint(valueColor, size, bold = large, align = Paint.Align.RIGHT))
    }

    private fun divider(canvas: Canvas, y: Float) {
        canvas.drawLine(MARGIN + 8f, y, PAGE_WIDTH - MARGIN - 8f, y, strokePaint(COL_DIVIDER, 0.5f))
    }

    // ── Paint factories ───────────────────────────────────────────────────────

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style      = Paint.Style.FILL
    }

    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color       = color
        style            = Paint.Style.STROKE
        strokeWidth      = width
    }

    private fun textPaint(
        color: Int,
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpaced: Boolean = false
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color      = color
        textSize        = size * 2.2f       // scale: PDF points → readable on A4
        isFakeBoldText  = bold
        textAlign       = align
        if (letterSpaced) letterSpacing = 0.12f
    }
}
