package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.LoanApplicationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LoanApplicationPdfGenerator {

    /**
     * Génère le document récapitulatif officiel PDF de la Demande de Crédit MALAKI.
     * Inclut le sceau de Validation Électronique biométrique avec empreinte SHA-256.
     */
    fun generateLoanApplicationPdf(
        context: Context,
        application: LoanApplicationEntity
    ): Result<File> {
        return try {
            val docsDir = File(context.filesDir, "loan_documents")
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val fileName = "Demande_Credit_MALAKI_${application.id}_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(docsDir, fileName)

            val document = PdfDocument()
            // Format A4 standard : 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // 1. BANDEAU SUPÉRIEUR INSTITUTIONNEL (Vert forêt #0F5132)
            paint.color = Color.rgb(15, 81, 50)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 96f, paint)

            // Liseré or (#D97706)
            paint.color = Color.rgb(217, 119, 6)
            canvas.drawRect(0f, 96f, 595f, 101f, paint)

            // En-tête texte
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("MALAKI", 32f, 40f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(254, 243, 199)
            canvas.drawText("Malisho Na Akiba — (Ration et Épargne)", 32f, 60f, paint)

            paint.textSize = 9.5f
            paint.color = Color.rgb(220, 252, 231)
            canvas.drawText("Microcrédits pour les mamans de Goma dans les petits commerces", 32f, 78f, paint)

            // Numéro et date à droite
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("DOSSIER N° ${application.id}", 563f, 40f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 9f
            paint.color = Color.rgb(241, 245, 249)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH)
            val dateStr = dateFormat.format(Date(application.signedAt ?: application.submissionDate))
            canvas.drawText("Émis le : $dateStr", 563f, 58f, paint)
            canvas.drawText("Version : ${application.version}", 563f, 74f, paint)
            paint.textAlign = Paint.Align.LEFT

            // TITRE PRINCIPAL
            var curY = 128f
            paint.color = Color.rgb(15, 81, 50)
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("DEMANDE DE CRÉDIT — DOSSIER OFFICIEL", 32f, curY, paint)

            curY += 6f
            paint.color = Color.rgb(203, 213, 225)
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(32f, curY, 563f, curY, paint)
            paint.style = Paint.Style.FILL

            // --- SECTION 1 : INFORMATIONS DE LA CLIENTE ---
            curY += 20f
            drawSectionHeader(canvas, paint, "1. INFORMATIONS DE LA CLIENTE", curY)
            curY += 18f

            val fullName = if (application.lastName.isNotBlank() || application.firstName.isNotBlank()) {
                listOf(application.lastName, application.postName, application.firstName).filter { it.isNotBlank() }.joinToString(" ")
            } else application.clientName

            drawKeyValue(canvas, paint, "Nom complet :", fullName, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Téléphone :", application.clientPhone, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Date de naissance :", application.birthDate.ifBlank { "Non renseignée" }, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "État civil :", application.maritalStatus, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Personnes à charge :", "${application.dependentsCount} personne(s)", 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Identifiant cliente :", application.clientId, 310f, 563f, curY)

            // --- SECTION 2 : LOCALISATION DE LA RÉSIDENCE ---
            curY += 26f
            drawSectionHeader(canvas, paint, "2. LOCALISATION DE LA RÉSIDENCE", curY)
            curY += 18f
            drawKeyValue(canvas, paint, "Commune :", application.commune, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Quartier :", application.quarter, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Avenue / Rue :", application.avenue.ifBlank { "Non précisée" }, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Point de repère :", application.landmark.ifBlank { "Non précisé" }, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Ancienneté au domicile :", application.residenceDuration, 32f, 563f, curY)

            // --- SECTION 3 : ACTIVITÉ COMMERCIALE ---
            curY += 26f
            drawSectionHeader(canvas, paint, "3. ACTIVITÉ COMMERCIALE", curY)
            curY += 18f
            drawKeyValue(canvas, paint, "Type de commerce :", application.businessType, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Marché d'exercice :", application.marketZone, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Emplacement / Étal :", application.businessLocation.ifBlank { "Marché principal" }, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Ancienneté d'activité :", application.businessSeniority, 310f, 563f, curY)
            curY += 16f
            val capitalStr = if (application.estimatedCapital > 0) "${"%,.0f".format(Locale.FRENCH, application.estimatedCapital)} ${application.currency}" else "Non spécifié"
            val revStr = if (application.dailyRevenue > 0) "${"%,.0f".format(Locale.FRENCH, application.dailyRevenue)} ${application.currency}/jour" else "Non spécifié"
            drawKeyValue(canvas, paint, "Capital estimé :", capitalStr, 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Revenu journalier moyen :", revStr, 310f, 563f, curY)

            // --- SECTION 4 : MODALITÉS DU CRÉDIT ---
            curY += 26f
            drawSectionHeader(canvas, paint, "4. CONDITIONS DU CRÉDIT DEMANDÉ", curY)
            curY += 18f
            val formattedAmount = "${"%,.0f".format(Locale.FRENCH, application.amountRequested)} ${application.currency}"
            drawKeyValue(canvas, paint, "Montant sollicité :", formattedAmount, 32f, 290f, curY, isBoldValue = true)
            drawKeyValue(canvas, paint, "Objet du crédit :", application.purpose, 310f, 563f, curY)
            curY += 16f
            drawKeyValue(canvas, paint, "Durée du cycle :", "${application.durationDays} jours", 32f, 290f, curY)
            drawKeyValue(canvas, paint, "Fréquence remboursement :", application.repaymentFrequency, 310f, 563f, curY)
            curY += 16f
            val dailyPmt = if (application.dailyPaymentAmount > 0) application.dailyPaymentAmount else (application.amountRequested * 0.035)
            val dailyPmtStr = "${"%,.0f".format(Locale.FRENCH, dailyPmt)} ${application.currency}/jour (taux ration 3.5%)"
            drawKeyValue(canvas, paint, "Échéance journalière :", dailyPmtStr, 32f, 563f, curY, isBoldValue = true)

            // --- SECTION 5 : CADRE OFFICIEL DE VALIDATION ÉLECTRONIQUE & SIGNATURE ---
            curY += 32f
            val boxTop = curY
            val boxBottom = boxTop + 148f
            val boxRect = RectF(32f, boxTop, 563f, boxBottom)

            // Fond doux avec bordure verte
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(240, 253, 244) // Vert très clair #F0FDF4
            canvas.drawRoundRect(boxRect, 8f, 8f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.rgb(15, 81, 50)
            canvas.drawRoundRect(boxRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // En-tête du cadre de validation
            var bY = boxTop + 20f
            paint.color = Color.rgb(15, 81, 50)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("VALIDATION ÉLECTRONIQUE ET SIGNATURE SÉCURISÉE", 48f, bY, paint)

            bY += 18f
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = Color.rgb(51, 65, 85)

            // Statut & Méthode
            drawKeyValue(canvas, paint, "Statut :", "SIGNÉ ET SCIELLÉ", 48f, 290f, bY, isBoldValue = true, valColor = Color.rgb(15, 81, 50))
            drawKeyValue(canvas, paint, "Méthode de signature :", "Authentification biométrique native", 310f, 547f, bY)

            bY += 16f
            drawKeyValue(canvas, paint, "Date et heure :", dateStr, 48f, 290f, bY)
            val sigId = application.signatureId ?: "SIG-MALAKI-${application.id}"
            drawKeyValue(canvas, paint, "Identifiant de signature :", sigId, 310f, 547f, bY, isBoldValue = true)

            bY += 16f
            val rawHash = application.documentHash ?: CryptoHelper.computeLoanDocumentHash(
                applicationId = application.id,
                customerId = application.clientId,
                fullName = fullName,
                phone = application.clientPhone,
                amount = application.amountRequested,
                currency = application.currency,
                purpose = application.purpose,
                commune = application.commune,
                quarter = application.quarter,
                businessType = application.businessType,
                timestamp = application.signedAt ?: application.submissionDate,
                version = application.version
            )
            paint.isFakeBoldText = true
            paint.textSize = 9f
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("Empreinte numérique (SHA-256) :", 48f, bY, paint)

            bY += 14f
            paint.isFakeBoldText = false
            paint.textSize = 8.5f
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText(rawHash, 48f, bY, paint)

            // Mention obligatoire en grand
            bY += 24f
            paint.color = Color.rgb(15, 81, 50)
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            canvas.drawText("✓ Demande validée électroniquement par la cliente.", 48f, bY, paint)

            // PIED DE PAGE
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 8f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "MALAKI • Malisho Na Akiba — Document généré électroniquement • Ville de Goma, Nord-Kivu, RDC",
                297.5f,
                820f,
                paint
            )
            paint.textAlign = Paint.Align.LEFT

            document.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ouvre ou partage le document PDF généré via FileProvider
     */
    fun viewOrShareLoanApplicationPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Ouvrir la Demande de Crédit MALAKI").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager la Demande de Crédit").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun drawSectionHeader(canvas: android.graphics.Canvas, paint: Paint, title: String, y: Float) {
        paint.color = Color.rgb(15, 81, 50)
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText(title, 32f, y, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(32f, y + 4f, 563f, y + 4f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawKeyValue(
        canvas: android.graphics.Canvas,
        paint: Paint,
        key: String,
        value: String,
        xStart: Float,
        xEnd: Float,
        y: Float,
        isBoldValue: Boolean = false,
        valColor: Int = Color.rgb(15, 23, 42)
    ) {
        paint.isFakeBoldText = false
        paint.textSize = 9.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(key, xStart, y, paint)

        val keyWidth = paint.measureText(key) + 6f
        paint.isFakeBoldText = isBoldValue
        paint.color = valColor
        val availWidth = xEnd - (xStart + keyWidth)
        var displayVal = value
        if (paint.measureText(displayVal) > availWidth && availWidth > 20f) {
            while (displayVal.length > 3 && paint.measureText("$displayVal…") > availWidth) {
                displayVal = displayVal.dropLast(1)
            }
            displayVal = "$displayVal…"
        }
        canvas.drawText(displayVal, xStart + keyWidth, y, paint)
    }
}
