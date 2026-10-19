package com.example

import com.example.data.model.CashMovementCategory
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashMovementType
import com.example.data.model.CycleStatus
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrocreditBusinessLogicTest {

    @Test
    fun testExcelMicrocreditContractualCalculation() {
        val principal = 200000.0 // 200 000 FC
        val dailyRate = 0.035 // 3.5%
        val durationDays = 30

        val dailyPayment = principal * dailyRate
        val totalContractual = dailyPayment * durationDays
        val grossProfit = totalContractual - principal

        assertEquals(7000.0, dailyPayment, 0.01)
        assertEquals(210000.0, totalContractual, 0.01)
        assertEquals(10000.0, grossProfit, 0.01)
    }

    @Test
    fun testRecoveryRateAndBalanceCalculation() {
        val totalContractual = 210000.0
        val collected = 140000.0 // 20 days paid

        val balanceRemaining = (totalContractual - collected).coerceAtLeast(0.0)
        val recoveryRate = (collected / totalContractual) * 100.0

        assertEquals(70000.0, balanceRemaining, 0.01)
        assertEquals(66.666, recoveryRate, 0.01)
    }

    @Test
    fun testCaisseBalanceCalculation() {
        val movements = listOf(
            CashMovementEntity(
                id = "CSH-1",
                date = 1000L,
                type = CashMovementType.ENTREE,
                category = CashMovementCategory.APPORT_CAPITAL,
                reference = "CAP-01",
                description = "Capital",
                amount = 5000000.0,
                recordedByUserId = "USR-1",
                recordedByName = "Manager"
            ),
            CashMovementEntity(
                id = "CSH-2",
                date = 2000L,
                type = CashMovementType.SORTIE,
                category = CashMovementCategory.DECAISSEMENT_PRET,
                reference = "DISB-01",
                description = "Prêt",
                amount = 200000.0,
                recordedByUserId = "USR-1",
                recordedByName = "Manager"
            ),
            CashMovementEntity(
                id = "CSH-3",
                date = 3000L,
                type = CashMovementType.ENTREE,
                category = CashMovementCategory.REMBOURSEMENT,
                reference = "PAY-01",
                description = "Remboursement",
                amount = 70000.0,
                recordedByUserId = "USR-2",
                recordedByName = "Agent"
            )
        )

        val totalEntrees = movements.filter { it.type == CashMovementType.ENTREE }.sumOf { it.amount }
        val totalSorties = movements.filter { it.type == CashMovementType.SORTIE }.sumOf { it.amount }
        val balance = totalEntrees - totalSorties

        assertEquals(5070000.0, totalEntrees, 0.01)
        assertEquals(200000.0, totalSorties, 0.01)
        assertEquals(4870000.0, balance, 0.01)
    }

    @Test
    fun testCurrencyFormatting() {
        val amount = 210000.0
        val formatted = MicrocreditRepository.formatAmount(amount)
        assertTrue(formatted.contains("210") && formatted.contains("FC"))
    }

    @Test
    fun testLoanRange100000To400000AndDaily3Point5PercentCalculation() {
        val dailyRate = 0.035 // 3.5% par jour
        val durationDays = 30

        // Test 1: Minimum loan = 100 000 FC
        val minLoan = 100000.0
        val minDailyPayment = minLoan * dailyRate
        val minTotalContractual = minDailyPayment * durationDays
        val minGrossProfit = minTotalContractual - minLoan
        val minClientShare = minGrossProfit * 0.50

        assertEquals(3500.0, minDailyPayment, 0.01) // 3 500 FC / jour
        assertEquals(105000.0, minTotalContractual, 0.01)
        assertEquals(5000.0, minGrossProfit, 0.01)
        assertEquals(2500.0, minClientShare, 0.01) // 50% partagé

        // Test 2: Maximum loan = 400 000 FC
        val maxLoan = 400000.0
        val maxDailyPayment = maxLoan * dailyRate
        val maxTotalContractual = maxDailyPayment * durationDays
        val maxGrossProfit = maxTotalContractual - maxLoan
        val maxClientShare = maxGrossProfit * 0.50

        assertEquals(14000.0, maxDailyPayment, 0.01) // 14 000 FC / jour
        assertEquals(420000.0, maxTotalContractual, 0.01)
        assertEquals(200000.0 * 0.10, maxGrossProfit, 0.01) // 20 000 FC
        assertEquals(10000.0, maxClientShare, 0.01) // 50% partagé
    }

    @Test
    fun testUserRoleLabels() {
        assertEquals("Gestionnaire / Propriétaire", UserRole.GESTIONNAIRE.label)
        assertEquals("Agent de terrain", UserRole.AGENT.label)
        assertEquals("Caissier", UserRole.CAISSIER.label)
        assertEquals("Client", UserRole.CLIENT.label)
    }

    @Test
    fun testVoiceMessageEntityAttributes() {
        val voiceMsg = com.example.data.model.ChatMessageEntity(
            id = "MSG-VOICE-01",
            senderId = "USR-001",
            senderName = "Jacques Masuruku",
            senderRole = UserRole.GESTIONNAIRE,
            clientId = "CLI-001",
            content = "Message vocal (8s)",
            isVoiceMessage = true,
            voiceDurationSeconds = 8,
            audioFilePath = "/data/user/0/com.example/cache/voice_messages/voice_123.m4a"
        )

        assertTrue(voiceMsg.isVoiceMessage)
        assertEquals(8, voiceMsg.voiceDurationSeconds)
        assertTrue(voiceMsg.audioFilePath?.endsWith(".m4a") == true)
        val formattedDuration = String.format(java.util.Locale.getDefault(), "%02d:%02d", voiceMsg.voiceDurationSeconds / 60, voiceMsg.voiceDurationSeconds % 60)
        assertEquals("00:08", formattedDuration)
    }

    @Test
    fun testMediaAttachmentEntityAttributes() {
        val photoMsg = com.example.data.model.ChatMessageEntity(
            id = "MSG-PHOTO-01",
            senderId = "USR-002",
            senderName = "Agent Goma",
            senderRole = UserRole.AGENT,
            clientId = "CLI-002",
            content = "Photo_recu.jpg",
            attachmentUri = "/data/user/0/com.example/files/chat_images/recu.jpg"
        )

        assertEquals("Photo_recu.jpg", photoMsg.content)
        assertEquals("/data/user/0/com.example/files/chat_images/recu.jpg", photoMsg.attachmentUri)
        assertTrue(!photoMsg.isVoiceMessage)
    }

    @Test
    fun testChatSecurityClientAndManagerAccessRules() {
        val clientUser = com.example.data.model.UserEntity(
            id = "USR-CLI-001",
            fullName = "Amina Mwamba",
            phone = "+243990000001",
            email = "amina@malaki.cd",
            role = UserRole.CLIENT,
            marketZone = "Marché Central"
        )
        val managerUser = com.example.data.model.UserEntity(
            id = "USR-MGR-001",
            fullName = "Jacques Masuruku",
            phone = "+243990000000",
            email = "jacques@malaki.cd",
            role = UserRole.GESTIONNAIRE,
            marketZone = "Direction"
        )
        val agentUser = com.example.data.model.UserEntity(
            id = "USR-AGT-001",
            fullName = "David Baraka",
            phone = "+243990000002",
            email = "david@malaki.cd",
            role = UserRole.AGENT,
            marketZone = "Marché Central"
        )

        val clients = listOf(
            com.example.data.model.ClientEntity(
                id = "CLI-001",
                fullName = "Amina Mwamba",
                phone = "+243990000001",
                marketZone = "Marché Central",
                activityType = "Commerce",
                assignedAgentId = "USR-AGT-001"
            ),
            com.example.data.model.ClientEntity(
                id = "CLI-002",
                fullName = "Bahati Safari",
                phone = "+243990000099",
                marketZone = "Marché Virunga",
                activityType = "Vente Légumes",
                assignedAgentId = "USR-AGT-001"
            )
        )

        // 1. Communauté : accessible pour tous (Client, Gestionnaire, Agent)
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(clientUser, "COMMUNITY_GROUP", clients))
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(managerUser, "COMMUNITY_GROUP", clients))
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(agentUser, "COMMUNITY_GROUP", clients))

        // 2. Client connecté : a accès à son propre salon privé (CLI-001)
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(clientUser, "CLI-001", clients))

        // 3. Client connecté : N'A PAS ACCÈS au salon privé d'un autre client (CLI-002)
        assertTrue(!com.example.util.ChatSecurityHelper.canUserAccessChatRoom(clientUser, "CLI-002", clients))

        // 4. Gestionnaire (Direction) : NE DOIT PAS avoir accès aux salons privés des clients
        assertTrue(!com.example.util.ChatSecurityHelper.canUserAccessChatRoom(managerUser, "CLI-001", clients))
        assertTrue(!com.example.util.ChatSecurityHelper.canUserAccessChatRoom(managerUser, "CLI-002", clients))

        // 5. Gestionnaire : Ne doit pas avoir accès aux conversations directes impliquant un client
        val directClientManagerRoom = "DIRECT_CLI-001_USR-MGR-001"
        assertTrue(!com.example.util.ChatSecurityHelper.canUserAccessChatRoom(managerUser, directClientManagerRoom, clients))

        // 6. Gestionnaire : A accès aux conversations directes avec les membres de l'équipe (Agents / Caissiers)
        val directStaffRoom = "DIRECT_USR-AGT-001_USR-MGR-001"
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(managerUser, directStaffRoom, clients))
        assertTrue(com.example.util.ChatSecurityHelper.canUserAccessChatRoom(agentUser, directStaffRoom, clients))
    }

    @Test
    fun testTargetedNotificationFiltering() {
        val agentUser = com.example.data.model.UserEntity(
            id = "USR-AGT-001",
            fullName = "Erick Mugisho",
            phone = "+243990000002",
            role = UserRole.AGENT,
            marketZone = "Marché Central"
        )
        val clientUser = com.example.data.model.UserEntity(
            id = "USR-CLI-001",
            fullName = "Amina Mwamba",
            phone = "+243990000001",
            role = UserRole.CLIENT,
            marketZone = "Marché Central"
        )
        val otherClientUser = com.example.data.model.UserEntity(
            id = "USR-CLI-002",
            fullName = "Bahati Safari",
            phone = "+243990000099",
            role = UserRole.CLIENT,
            marketZone = "Marché Virunga"
        )
        val managerUser = com.example.data.model.UserEntity(
            id = "USR-MGR-001",
            fullName = "Jacques Masuruku",
            phone = "+243970000001",
            role = UserRole.GESTIONNAIRE,
            marketZone = "Direction"
        )

        val clients = listOf(
            com.example.data.model.ClientEntity(
                id = "CLI-001",
                fullName = "Amina Mwamba",
                phone = "+243990000001",
                marketZone = "Marché Central",
                activityType = "Commerce",
                assignedAgentId = "USR-AGT-001"
            )
        )

        // Notification de message privé envoyé par l'Agent Erick Mugisho à la cliente Amina Mwamba
        val messageNotifForClient = com.example.data.model.AppNotificationEntity(
            id = "NOTIF-MSG-001",
            targetUserId = "USR-CLI-001",
            targetClientId = "CLI-001",
            destinationAddress = "Client : Amina Mwamba • Marché Central",
            senderId = agentUser.id,
            senderName = agentUser.fullName,
            title = "Message de Erick Mugisho",
            message = "Bonjour Amina, votre dossier est prêt.",
            type = com.example.data.model.NotificationType.MESSAGE
        )

        // 1. L'émetteur (Agent Erick) NE REÇOIT PAS la notification de son propre message
        assertTrue(!com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(messageNotifForClient, agentUser, clients))

        // 2. La destinataire ciblée (Amina Mwamba) REÇOIT la notification
        assertTrue(com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(messageNotifForClient, clientUser, clients))

        // 3. Un autre client (Bahati) NE REÇOIT PAS cette notification
        assertTrue(!com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(messageNotifForClient, otherClientUser, clients))

        // 4. Le gestionnaire NE REÇOIT PAS cette notification privée entre agent et client
        assertTrue(!com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(messageNotifForClient, managerUser, clients))
    }

    @Test
    fun testActivityDestinationAndConcernedUsers() {
        val clientUser = com.example.data.model.UserEntity(
            id = "USR-CLI-001",
            fullName = "Amina Mwamba",
            phone = "+243990000001",
            role = UserRole.CLIENT,
            marketZone = "Marché Central"
        )
        val otherClientUser = com.example.data.model.UserEntity(
            id = "USR-CLI-002",
            fullName = "Bahati Safari",
            phone = "+243990000099",
            role = UserRole.CLIENT,
            marketZone = "Marché Virunga"
        )
        val agentUser = com.example.data.model.UserEntity(
            id = "USR-AGT-001",
            fullName = "Erick Mugisho",
            phone = "+243990000002",
            role = UserRole.AGENT,
            marketZone = "Marché Central"
        )
        val managerUser = com.example.data.model.UserEntity(
            id = "USR-MGR-001",
            fullName = "Jacques Masuruku",
            phone = "+243970000001",
            role = UserRole.GESTIONNAIRE,
            marketZone = "Direction"
        )

        val clients = listOf(
            com.example.data.model.ClientEntity(
                id = "CLI-001",
                fullName = "Amina Mwamba",
                phone = "+243990000001",
                marketZone = "Marché Central",
                activityType = "Commerce",
                assignedAgentId = "USR-AGT-001"
            )
        )

        val activity = com.example.data.model.AuditLogEntity(
            id = "AUD-100",
            userId = agentUser.id,
            userName = agentUser.fullName,
            userRole = agentUser.role,
            action = "PAIEMENT_ENREGISTRE",
            entityType = "PAYMENT",
            entityId = "PAY-001",
            details = "Recouvrement journalier de 7 000 FC perçu auprès de Amina Mwamba.",
            destination = "Amina Mwamba • Client CLI-001",
            targetClientId = "CLI-001",
            targetUserId = "USR-CLI-001"
        )

        // 1. La Direction a la supervision générale
        assertTrue(com.example.util.ChatSecurityHelper.isActivityConcernedForUser(activity, managerUser, clients))

        // 2. L'auteur (Agent) voit l'activité
        assertTrue(com.example.util.ChatSecurityHelper.isActivityConcernedForUser(activity, agentUser, clients))

        // 3. La cliente concernée (Amina) voit son activité
        assertTrue(com.example.util.ChatSecurityHelper.isActivityConcernedForUser(activity, clientUser, clients))

        // 4. Un autre client non concerné (Bahati) NE VOIT PAS l'activité
        assertTrue(!com.example.util.ChatSecurityHelper.isActivityConcernedForUser(activity, otherClientUser, clients))
    }

    @Test
    fun testClientMatriculeAutoIncrementAndQuantiemeOrder() {
        // Test extraction of sequential number
        assertEquals(1, MicrocreditRepository.extractClientSequenceNumber("CLI-001"))
        assertEquals(2, MicrocreditRepository.extractClientSequenceNumber("CLI-002"))
        assertEquals(3, MicrocreditRepository.extractClientSequenceNumber("CLI-003"))
        assertEquals(15, MicrocreditRepository.extractClientSequenceNumber("CLI-015"))
        assertEquals(100, MicrocreditRepository.extractClientSequenceNumber("CLI-100"))
        assertEquals(1, MicrocreditRepository.extractClientSequenceNumber("USR-CLI-001"))

        // Test quantieme labels in French
        assertEquals("1er client", MicrocreditRepository.getClientQuantiemeLabel("CLI-001", com.example.util.AppLanguage.FRENCH))
        assertEquals("2ème client", MicrocreditRepository.getClientQuantiemeLabel("CLI-002", com.example.util.AppLanguage.FRENCH))
        assertEquals("3ème client", MicrocreditRepository.getClientQuantiemeLabel("CLI-003", com.example.util.AppLanguage.FRENCH))
        assertEquals("4ème client", MicrocreditRepository.getClientQuantiemeLabel("CLI-004", com.example.util.AppLanguage.FRENCH))
        assertEquals("15ème client", MicrocreditRepository.getClientQuantiemeLabel("CLI-015", com.example.util.AppLanguage.FRENCH))

        // Test quantieme labels in Swahili
        assertEquals("Mteja wa 1", MicrocreditRepository.getClientQuantiemeLabel("CLI-001", com.example.util.AppLanguage.SWAHILI))
        assertEquals("Mteja wa 2", MicrocreditRepository.getClientQuantiemeLabel("CLI-002", com.example.util.AppLanguage.SWAHILI))
        assertEquals("Mteja wa 3", MicrocreditRepository.getClientQuantiemeLabel("CLI-003", com.example.util.AppLanguage.SWAHILI))

        // Test simulated sequential auto-increment calculation
        val sampleClients = listOf(
            com.example.data.model.ClientEntity(id = "CLI-001", fullName = "Client 1", phone = "1", marketZone = "Z", activityType = "A"),
            com.example.data.model.ClientEntity(id = "CLI-002", fullName = "Client 2", phone = "2", marketZone = "Z", activityType = "A"),
            com.example.data.model.ClientEntity(id = "CLI-003", fullName = "Client 3", phone = "3", marketZone = "Z", activityType = "A")
        )
        val maxNumber = sampleClients.mapNotNull { MicrocreditRepository.extractClientSequenceNumber(it.id) }.maxOrNull() ?: 0
        val nextId = String.format(java.util.Locale.US, "CLI-%03d", maxNumber + 1)
        assertEquals("CLI-004", nextId)
        assertEquals("4ème client", MicrocreditRepository.getClientQuantiemeLabel(nextId, com.example.util.AppLanguage.FRENCH))

        // Test sorting order strictly puts CLI-001, CLI-002, CLI-003 in natural ascending numerical order
        val scrambled = listOf(
            com.example.data.model.ClientEntity(id = "CLI-003", fullName = "Client 3", phone = "3", marketZone = "Z", activityType = "A"),
            com.example.data.model.ClientEntity(id = "CLI-001", fullName = "Client 1", phone = "1", marketZone = "Z", activityType = "A"),
            com.example.data.model.ClientEntity(id = "CLI-002", fullName = "Client 2", phone = "2", marketZone = "Z", activityType = "A")
        )
        val sorted = scrambled.sortedBy { MicrocreditRepository.extractClientSequenceNumber(it.id) ?: 999999 }
        assertEquals("CLI-001", sorted[0].id)
        assertEquals("CLI-002", sorted[1].id)
        assertEquals("CLI-003", sorted[2].id)
    }
}

