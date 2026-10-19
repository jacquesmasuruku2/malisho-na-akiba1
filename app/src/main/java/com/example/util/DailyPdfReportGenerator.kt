package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.PaymentEntity
import com.example.data.repository.MicrocreditRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailyPdfReportGenerator {

    data class DailyReportData(
        val dateString: String,
        val totalExpected: Double,
        val totalCollected: Double,
        val totalVariance: Double,
        val recoveryRate: Double,
        val activeLoansCount: Int,
        val payments: List<PaymentEntity>,
        val organizationName: String = "MALISHO NA AKIBA (MALAKI)",
        val generatedBy: String = "Direction des Opérations"
    )

    fun generateAndShareDailyReportPdf(
        context: Context,
        reportData: DailyReportData
    ): Result<File> {
        return try {
            val reportsDir = File(context.filesDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val safeDate = reportData.dateString.replace("/", "-")
            val fileName = "Rapport_Journalier_${safeDate}_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(reportsDir, fileName)

            val document = PdfDocument()

            // Standard A4 dimensions in PostScript points: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // 1. HEADER BANNER (Corporate Navy #0D2137)
            paint.color = Color.rgb(13, 33, 55)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            // Gold accent stripe (#E5A93C)
            paint.color = Color.rgb(229, 169, 60)
            canvas.drawRect(0f, 90f, 595f, 95f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 17f
            paint.isFakeBoldText = true
            canvas.drawText(reportData.organizationName, 30f, 38f, paint)

            // Subtitle
            paint.textSize = 11f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(220, 230, 245)
            canvas.drawText("RAPPORT JOURNALIER DES TRANSACTIONS & RECOUVREMENTS", 30f, 58f, paint)
            canvas.drawText("Système de Microcrédit & Inclusion Financière", 30f, 75f, paint)

            // Date Badge on the top right
            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.isFakeBoldText = true
            val dateLabel = "Date : ${reportData.dateString}"
            val dateWidth = paint.measureText(dateLabel)
            canvas.drawText(dateLabel, 565f - dateWidth, 40f, paint)

            val genTime = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
            paint.isFakeBoldText = false
            paint.color = Color.rgb(200, 220, 240)
            val timeLabel = "Généré à : $genTime"
            val timeWidth = paint.measureText(timeLabel)
            canvas.drawText(timeLabel, 565f - timeWidth, 58f, paint)

            // 2. SUMMARY KPI METRICS CARD (Background #F3F6F9)
            paint.color = Color.rgb(243, 246, 249)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(30f, 110f, 565f, 225f, 8f, 8f, paint)

            // Card border
            paint.color = Color.rgb(215, 225, 235)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(30f, 110f, 565f, 225f, 8f, 8f, paint)

            // KPI Title
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(13, 33, 55)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("SYNTHÈSE FINANCIÈRE DE LA JOURNÉE", 45f, 132f, paint)

            // Row 1: Attendu vs Encaissé
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Total Attendu :", 45f, 155f, paint)
            paint.color = Color.rgb(15, 30, 45)
            paint.isFakeBoldText = true
            canvas.drawText(MicrocreditRepository.formatAmount(reportData.totalExpected), 150f, 155f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Total Encaissé :", 310f, 155f, paint)
            paint.color = Color.rgb(16, 120, 60) // Green
            paint.isFakeBoldText = true
            canvas.drawText(MicrocreditRepository.formatAmount(reportData.totalCollected), 420f, 155f, paint)

            // Row 2: Écart & Taux de Recouvrement
            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Écart de Caisse :", 45f, 180f, paint)
            paint.color = if (reportData.totalVariance >= 0) Color.rgb(16, 120, 60) else Color.rgb(195, 35, 35)
            paint.isFakeBoldText = true
            val variancePrefix = if (reportData.totalVariance > 0) "+" else ""
            canvas.drawText("$variancePrefix${MicrocreditRepository.formatAmount(reportData.totalVariance)}", 150f, 180f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Taux Réalisé :", 310f, 180f, paint)
            paint.color = Color.rgb(13, 33, 55)
            paint.isFakeBoldText = true
            canvas.drawText(MicrocreditRepository.formatPercent(reportData.recoveryRate), 420f, 180f, paint)

            // Row 3: Nombre de transactions & Crédits actifs
            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Versements Réalisés :", 45f, 205f, paint)
            paint.color = Color.rgb(15, 30, 45)
            paint.isFakeBoldText = true
            canvas.drawText("${reportData.payments.size} transaction(s)", 150f, 205f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.rgb(90, 105, 120)
            canvas.drawText("Crédits Actifs :", 310f, 205f, paint)
            paint.color = Color.rgb(15, 30, 45)
            paint.isFakeBoldText = true
            canvas.drawText("${reportData.activeLoansCount} dossier(s)", 420f, 205f, paint)

            // 3. TABLE OF TRANSACTIONS
            paint.color = Color.rgb(13, 33, 55)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("DÉTAIL DES VERSEMENTS DU JOUR", 30f, 255f, paint)

            // Table Header Bar (#1F3D5C)
            paint.color = Color.rgb(31, 61, 92)
            paint.style = Paint.Style.FILL
            canvas.drawRect(30f, 268f, 565f, 290f, paint)

            // Table Header Texts
            paint.color = Color.WHITE
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText("N° Reçu", 35f, 283f, paint)
            canvas.drawText("Bénéficiaire / Client", 95f, 283f, paint)
            canvas.drawText("Agent de Terrain", 235f, 283f, paint)
            canvas.drawText("Mode", 355f, 283f, paint)
            canvas.drawText("Attendu", 420f, 283f, paint)
            canvas.drawText("Encaissé", 495f, 283f, paint)

            // Table Rows
            var yPos = 305f
            paint.isFakeBoldText = false

            if (reportData.payments.isEmpty()) {
                paint.color = Color.rgb(130, 140, 150)
                paint.textSize = 10f
                canvas.drawText("Aucune transaction enregistrée pour cette journée.", 45f, yPos + 10f, paint)
                yPos += 30f
            } else {
                val maxItemsToDisplay = 22
                val displayedList = reportData.payments.take(maxItemsToDisplay)

                displayedList.forEachIndexed { index, payment ->
                    // Alternate zebra striping
                    if (index % 2 == 1) {
                        paint.color = Color.rgb(248, 250, 252)
                        paint.style = Paint.Style.FILL
                        canvas.drawRect(30f, yPos - 12f, 565f, yPos + 8f, paint)
                    }

                    // Bottom subtle line
                    paint.color = Color.rgb(230, 235, 240)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(30f, yPos + 8f, 565f, yPos + 8f, paint)

                    paint.style = Paint.Style.FILL
                    paint.textSize = 8.5f
                    paint.color = Color.rgb(25, 35, 45)

                    // Reference
                    val receiptId = if (payment.id.length > 8) payment.id.take(8) else payment.id
                    canvas.drawText(receiptId, 35f, yPos, paint)

                    // Client
                    val clientText = if (payment.clientName.length > 22) payment.clientName.take(20) + ".." else payment.clientName
                    paint.isFakeBoldText = true
                    canvas.drawText(clientText, 95f, yPos, paint)

                    // Agent
                    paint.isFakeBoldText = false
                    val agentText = if (payment.agentName.length > 18) payment.agentName.take(16) + ".." else payment.agentName
                    canvas.drawText(agentText, 235f, yPos, paint)

                    // Payment Method
                    canvas.drawText(payment.paymentMethod.name, 355f, yPos, paint)

                    // Expected
                    paint.color = Color.rgb(100, 110, 120)
                    canvas.drawText(MicrocreditRepository.formatAmount(payment.expectedAmount), 420f, yPos, paint)

                    // Collected
                    paint.color = Color.rgb(16, 120, 60)
                    paint.isFakeBoldText = true
                    canvas.drawText(MicrocreditRepository.formatAmount(payment.collectedAmount), 495f, yPos, paint)

                    yPos += 20f
                }

                if (reportData.payments.size > maxItemsToDisplay) {
                    val remaining = reportData.payments.size - maxItemsToDisplay
                    paint.color = Color.rgb(100, 110, 120)
                    paint.textSize = 8.5f
                    paint.isFakeBoldText = false
                    canvas.drawText("... et $remaining autre(s) transaction(s) détaillée(s) dans la base.", 35f, yPos + 2f, paint)
                    yPos += 18f
                }
            }

            // 4. FOOTER NOTE & SIGNATURE STAMP
            val footerY = 760f
            paint.color = Color.rgb(200, 210, 220)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(30f, footerY, 565f, footerY, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 8.5f
            paint.color = Color.rgb(100, 115, 130)
            paint.isFakeBoldText = false
            canvas.drawText("Document officiel généré automatiquement par l'application MALISHO NA AKIBA (MALAKI).", 30f, footerY + 16f, paint)
            canvas.drawText("Certifié conforme pour le contrôle de gestion et l'audit financier quotidien.", 30f, footerY + 28f, paint)

            // Right signature box
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            paint.color = Color.rgb(13, 33, 55)
            val authText = "Visa & Contrôle Comptable"
            val authWidth = paint.measureText(authText)
            canvas.drawText(authText, 565f - authWidth, footerY + 16f, paint)

            paint.textSize = 7.5f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(120, 130, 140)
            val approvedText = "Statut : Validé électroniquement"
            val approvedWidth = paint.measureText(approvedText)
            canvas.drawText(approvedText, 565f - approvedWidth, footerY + 28f, paint)

            document.finishPage(page)

            // Write to file
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            // Share intent via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rapport Journalier MALAKI - ${reportData.dateString}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Veuillez trouver ci-joint le rapport officiel des transactions journalières (${reportData.dateString}).\n" +
                            "Total Encaissé : ${MicrocreditRepository.formatAmount(reportData.totalCollected)}\n" +
                            "Taux de réalisation : ${MicrocreditRepository.formatPercent(reportData.recoveryRate)}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Partager le rapport PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Result.success(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
