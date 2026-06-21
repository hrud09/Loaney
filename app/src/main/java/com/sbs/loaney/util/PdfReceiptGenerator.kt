package com.sbs.loaney.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.core.content.FileProvider
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReceiptGenerator {

    private const val PAGE_WIDTH = 595  // A4
    private const val PAGE_HEIGHT = 842

    fun generateAndShare(
        context: Context,
        loan: LoanEntity,
        payments: List<PaymentEntity>,
        loanItems: List<LoanItemEntity>,
        currencySymbol: String = "৳"
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Paints
        val titlePaint = Paint().apply {
            textSize = 28f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#7C6EF6")
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0F172A")
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 14f; color = android.graphics.Color.parseColor("#334155")
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0F172A")
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0"); strokeWidth = 1f
        }
        val greenPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#10B981")
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#F43F5E")
            isAntiAlias = true
        }

        var y = 60f
        val leftMargin = 50f
        val rightMargin = PAGE_WIDTH - 50f

        // App name
        canvas.drawText("Loaney", leftMargin, y, titlePaint)
        y += 12f
        canvas.drawText("Loan Receipt", leftMargin, y + 18f, bodyPaint)
        y += 50f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 30f

        // Transaction info
        canvas.drawText("Transaction Details", leftMargin, y, headerPaint)
        y += 28f

        val infoRows = listOf(
            "Recipient" to loan.personName,
            "Type" to if (loan.type.name == "LEND") "Lent" else "Borrowed",
            "Original Amount" to "$currencySymbol${String.format("%,.0f", loan.amount)}",
            "Loan Date" to dateFormat.format(loan.loanDate),
            "Due Date" to dateFormat.format(loan.promisedReturnDate),
            "Status" to (loan.status?.name ?: "ACTIVE"),
            "Purpose" to (loan.purpose ?: "Not specified")
        )

        infoRows.forEach { (label, value) ->
            canvas.drawText("$label:", leftMargin, y, bodyPaint)
            canvas.drawText(value, leftMargin + 160f, y, boldBodyPaint)
            y += 22f
        }

        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 25f

        // Additional loan items
        if (loanItems.isNotEmpty()) {
            canvas.drawText("Additional Loans", leftMargin, y, headerPaint)
            y += 24f
            loanItems.forEach { item ->
                canvas.drawText(
                    "${dateFormat.format(item.date)}  •  +$currencySymbol${String.format("%,.0f", item.amount)}",
                    leftMargin, y, redPaint
                )
                y += 20f
            }
            y += 10f
            canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
            y += 25f
        }

        // Payment history
        canvas.drawText("Payment History", leftMargin, y, headerPaint)
        y += 24f

        if (payments.isEmpty()) {
            canvas.drawText("No payments recorded.", leftMargin, y, bodyPaint)
            y += 22f
        } else {
            payments.sortedBy { it.date }.forEach { payment ->
                canvas.drawText(
                    "${dateFormat.format(payment.date)}  •  -$currencySymbol${String.format("%,.0f", payment.amount)}  (${payment.method})",
                    leftMargin, y, greenPaint
                )
                y += 20f
            }
        }

        // Summary
        y += 15f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 30f

        val totalLoan = loan.amount + loanItems.sumOf { it.amount }
        val totalPaid = payments.sumOf { it.amount }
        val remaining = (totalLoan - totalPaid).coerceAtLeast(0.0)

        canvas.drawText("Total Loan:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", totalLoan)}", leftMargin + 160f, y, boldBodyPaint)
        y += 22f
        canvas.drawText("Total Paid:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", totalPaid)}", leftMargin + 160f, y, greenPaint)
        y += 22f
        canvas.drawText("Remaining:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", remaining)}", leftMargin + 160f, y, if (remaining > 0) redPaint else greenPaint)
        y += 40f

        // Footer
        canvas.drawText("Generated by Loaney on ${dateFormat.format(Date())}", leftMargin, y, bodyPaint)

        pdfDocument.finishPage(page)

        // Save to cache directory
        val pdfDir = File(context.cacheDir, "receipts")
        pdfDir.mkdirs()
        val fileName = "Loaney_Receipt_${loan.personName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(pdfDir, fileName)

        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        // Share via system share sheet
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            .logEvent("receipt_generated", null)
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
    }

    fun generatePdfBytes(
        context: Context,
        loan: LoanEntity,
        payments: List<PaymentEntity>,
        loanItems: List<LoanItemEntity>,
        currencySymbol: String = "৳"
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Paints
        val titlePaint = Paint().apply {
            textSize = 28f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#7C6EF6")
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0F172A")
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 14f; color = android.graphics.Color.parseColor("#334155")
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0F172A")
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0"); strokeWidth = 1f
        }
        val greenPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#10B981")
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#F43F5E")
            isAntiAlias = true
        }

        var y = 60f
        val leftMargin = 50f
        val rightMargin = PAGE_WIDTH - 50f

        // App name
        canvas.drawText("Loaney", leftMargin, y, titlePaint)
        y += 12f
        canvas.drawText("Loan Receipt", leftMargin, y + 18f, bodyPaint)
        y += 50f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 30f

        // Transaction info
        canvas.drawText("Transaction Details", leftMargin, y, headerPaint)
        y += 28f

        val infoRows = listOf(
            "Recipient" to loan.personName,
            "Type" to if (loan.type.name == "LEND") "Lent" else "Borrowed",
            "Original Amount" to "$currencySymbol${String.format("%,.0f", loan.amount)}",
            "Loan Date" to dateFormat.format(loan.loanDate ?: Date()),
            "Due Date" to dateFormat.format(loan.promisedReturnDate ?: Date()),
            "Status" to (loan.status?.name ?: "ACTIVE"),
            "Purpose" to (loan.purpose ?: "Not specified")
        )

        infoRows.forEach { (label, value) ->
            canvas.drawText("$label:", leftMargin, y, bodyPaint)
            canvas.drawText(value, leftMargin + 160f, y, boldBodyPaint)
            y += 22f
        }

        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 25f

        // Additional loan items
        if (loanItems.isNotEmpty()) {
            canvas.drawText("Additional Loans", leftMargin, y, headerPaint)
            y += 24f
            loanItems.forEach { item ->
                canvas.drawText(
                    "${dateFormat.format(item.date)}  •  +$currencySymbol${String.format("%,.0f", item.amount)}",
                    leftMargin, y, redPaint
                )
                y += 20f
            }
            y += 10f
            canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
            y += 25f
        }

        // Payment history
        canvas.drawText("Payment History", leftMargin, y, headerPaint)
        y += 24f

        if (payments.isEmpty()) {
            canvas.drawText("No payments recorded.", leftMargin, y, bodyPaint)
            y += 22f
        } else {
            payments.sortedBy { it.date }.forEach { payment ->
                canvas.drawText(
                    "${dateFormat.format(payment.date)}  •  -$currencySymbol${String.format("%,.0f", payment.amount)}  (${payment.method})",
                    leftMargin, y, greenPaint
                )
                y += 20f
            }
        }

        // Summary
        y += 15f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 30f

        val totalLoan = loan.amount + loanItems.sumOf { it.amount }
        val totalPaid = payments.sumOf { it.amount }
        val remaining = (totalLoan - totalPaid).coerceAtLeast(0.0)

        canvas.drawText("Total Loan:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", totalLoan)}", leftMargin + 160f, y, boldBodyPaint)
        y += 22f
        canvas.drawText("Total Paid:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", totalPaid)}", leftMargin + 160f, y, greenPaint)
        y += 22f
        canvas.drawText("Remaining:", leftMargin, y, bodyPaint)
        canvas.drawText("$currencySymbol${String.format("%,.0f", remaining)}", leftMargin + 160f, y, if (remaining > 0) redPaint else greenPaint)
        y += 40f

        // Footer
        canvas.drawText("Generated by Loaney on ${dateFormat.format(Date())}", leftMargin, y, bodyPaint)

        pdfDocument.finishPage(page)

        val outputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            .logEvent("receipt_generated", null)
            
        return outputStream.toByteArray()
    }

    fun openPdfFromBase64(context: Context, base64: String, personName: String) {
        try {
            val pdfBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val pdfDir = File(context.cacheDir, "receipts")
            pdfDir.mkdirs()
            val fileName = "Loaney_Receipt_${personName.replace(" ", "_")}_Shared.pdf"
            val pdfFile = File(pdfDir, fileName)

            FileOutputStream(pdfFile).use { outputStream ->
                outputStream.write(pdfBytes)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open Receipt"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error opening PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
