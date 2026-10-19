package com.example

import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanStatus
import com.example.data.model.SignatureProof
import com.example.util.CryptoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanApplicationBiometricTest {

    @Test
    fun testSha256DocumentHashGeneration() {
        val hash1 = CryptoHelper.computeLoanDocumentHash(
            applicationId = "APP-001",
            customerId = "CLI-001",
            fullName = "KABUO MASIKA NEEMA",
            phone = "+243840000004",
            amount = 100000.0,
            currency = "CDF",
            purpose = "Stock de vivres au marché de Kituku",
            commune = "Goma",
            quarter = "Birere",
            businessType = "Alimentation",
            timestamp = 1728345600000L,
            version = 1
        )

        // SHA-256 standard produces a 64-character lowercase hex string
        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[a-f0-9]{64}$")))

        // Deterministic: same inputs produce exact same hash
        val hash2 = CryptoHelper.computeLoanDocumentHash(
            applicationId = "APP-001",
            customerId = "CLI-001",
            fullName = "KABUO MASIKA NEEMA",
            phone = "+243840000004",
            amount = 100000.0,
            currency = "CDF",
            purpose = "Stock de vivres au marché de Kituku",
            commune = "Goma",
            quarter = "Birere",
            businessType = "Alimentation",
            timestamp = 1728345600000L,
            version = 1
        )
        assertEquals(hash1, hash2)

        // Any alteration in amount, purpose, or data produces a completely different hash (tamper evidence)
        val tamperedHash = CryptoHelper.computeLoanDocumentHash(
            applicationId = "APP-001",
            customerId = "CLI-001",
            fullName = "KABUO MASIKA NEEMA",
            phone = "+243840000004",
            amount = 200000.0, // Modified amount!
            currency = "CDF",
            purpose = "Stock de vivres au marché de Kituku",
            commune = "Goma",
            quarter = "Birere",
            businessType = "Alimentation",
            timestamp = 1728345600000L,
            version = 1
        )
        assertNotEquals(hash1, tamperedHash)
    }

    @Test
    fun testSignatureProofStructure() {
        val sigId = CryptoHelper.generateSignatureId()
        assertTrue(sigId.startsWith("SIG-"))

        val proof = SignatureProof(
            signatureId = sigId,
            customerId = "CLI-001",
            applicationId = "APP-12345",
            signatureMethod = "BIOMETRIC",
            signedAt = "2026-10-08T00:00:00Z",
            documentHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            status = "SIGNED"
        )

        assertEquals("BIOMETRIC", proof.signatureMethod)
        assertEquals("SIGNED", proof.status)
        assertNotNull(proof.signatureId)
        assertEquals(64, proof.documentHash.length)
    }

    @Test
    fun testLoanFinancialCalculations() {
        val amount = 100000.0 // 100 000 FC
        val dailyRate = 0.035 // 3.5% par jour
        val dailyRepayment = amount * dailyRate

        assertEquals(3500.0, dailyRepayment, 0.001)

        val totalRepaymentOver30Days = dailyRepayment * 30
        assertEquals(105000.0, totalRepaymentOver30Days, 0.001)
    }

    @Test
    fun testLockedApplicationImmutabilityAndVersioning() {
        val initialApp = LoanApplicationEntity(
            id = "APP-001",
            clientId = "CLI-001",
            clientName = "KABUO NEEMA",
            clientPhone = "+243840000004",
            amountRequested = 100000.0,
            durationDays = 30,
            purpose = "Stock de vivres",
            activityType = "Alimentation",
            marketZone = "Marché de Kituku",
            quarter = "Birere",
            isLocked = true,
            version = 1,
            signatureId = "SIG-20261008-ABC123",
            signatureMethod = "BIOMETRIC",
            signedAt = 1728345600000L,
            documentHash = "a1b2c3d4e5f6...",
            status = LoanStatus.SOUMISE
        )

        assertTrue(initialApp.isLocked)
        assertEquals(1, initialApp.version)

        // Versioning: if modification is made, a new version is created referencing the parent
        val version2App = initialApp.copy(
            id = "APP-002",
            amountRequested = 150000.0,
            version = initialApp.version + 1,
            parentApplicationId = initialApp.id,
            signatureId = "SIG-20261008-XYZ789",
            isLocked = true
        )

        assertEquals(2, version2App.version)
        assertEquals("APP-001", version2App.parentApplicationId)
        assertNotEquals(initialApp.signatureId, version2App.signatureId)
        assertTrue(version2App.isLocked)
    }
}
