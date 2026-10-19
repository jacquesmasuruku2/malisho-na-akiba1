package com.example.util

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CryptoHelper {

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calcule l'empreinte cryptographique SHA-256 du dossier de demande de crédit.
     * Cette empreinte scelle de manière infalsifiable les données du formulaire
     * au moment précis de l'authentification biométrique.
     */
    fun computeLoanDocumentHash(
        applicationId: String,
        customerId: String,
        fullName: String,
        phone: String,
        amount: Double,
        currency: String,
        purpose: String,
        commune: String,
        quarter: String,
        businessType: String,
        timestamp: Long,
        version: Int = 1
    ): String {
        val canonicalPayload = buildString {
            append("MALAKI_LOAN_V$version|")
            append("APP_ID:$applicationId|")
            append("CUSTOMER:$customerId|")
            append("NAME:${fullName.trim().uppercase(Locale.ROOT)}|")
            append("PHONE:${phone.trim()}|")
            append("AMOUNT:${"%.2f".format(Locale.US, amount)}$currency|")
            append("PURPOSE:${purpose.trim()}|")
            append("COMMUNE:${commune.trim()}|")
            append("QUARTER:${quarter.trim()}|")
            append("BUSINESS:${businessType.trim()}|")
            append("SIGNED_AT:$timestamp")
        }
        return sha256(canonicalPayload)
    }

    /**
     * Génère un identifiant unique et traçable pour la signature électronique.
     */
    fun generateSignatureId(): String {
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.ROOT)
        return "SIG-$datePrefix-$randomSuffix"
    }

    /**
     * Découpe joliment le hash SHA-256 pour affichage lisible sur les reçus
     */
    fun formatHashForDisplay(hash: String): String {
        return hash.chunked(8).joinToString(" ").uppercase(Locale.ROOT)
    }
}
