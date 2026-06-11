package de.schatzsuche.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import de.schatzsuche.app.data.model.QrCodeEntity
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val PAGE_WIDTH = 595  // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val CODES_PER_PAGE = 6
    private const val COLS = 2
    private const val ROWS = 3

    fun generateQrCardsPdf(context: Context, codes: List<QrCodeEntity>): File {
        val pdf = PdfDocument()
        val chunks = codes.chunked(CODES_PER_PAGE)
        val margin = 40f
        val cardWidth = (PAGE_WIDTH - margin * 2) / COLS
        val cardHeight = (PAGE_HEIGHT - margin * 2) / ROWS

        chunks.forEachIndexed { pageIndex, pageCodes ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            pageCodes.forEachIndexed { index, code ->
                val col = index % COLS
                val row = index / COLS
                val left = margin + col * cardWidth
                val top = margin + row * cardHeight
                drawCard(canvas, code, left, top, cardWidth, cardHeight)
            }

            pdf.finishPage(page)
        }

        val outputDir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, "qr_karten_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
        return outputFile
    }

    private fun drawCard(canvas: Canvas, code: QrCodeEntity, left: Float, top: Float, width: Float, height: Float) {
        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val rect = RectF(left + 8, top + 8, left + width - 8, top + height - 8)
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

        val qrSize = (width * 0.55f).toInt().coerceAtMost((height * 0.55f).toInt())
        val qrBitmap = QrCodeUtil.generateBitmap(code.payload, qrSize)
        val qrLeft = left + (width - qrSize) / 2f
        val qrTop = top + height * 0.15f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        val numberPaint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val numberText = "#${code.number.toString().padStart(2, '0')}"
        canvas.drawText(numberText, left + width / 2f, top + height - 28f, numberPaint)

        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Schatzsuche QR-Karte", left + width / 2f, top + height - 10f, labelPaint)
    }
}
