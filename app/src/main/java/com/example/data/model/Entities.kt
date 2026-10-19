package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val role: UserRole,
    val marketZone: String = "Marché de Kituku",
    val isActive: Boolean = true,
    val createdDate: Long = System.currentTimeMillis(),
    val lastLoginDate: Long = System.currentTimeMillis(),
    val invitedBy: String? = null,
    val pinCode: String = "1234",
    val username: String = "",
    val isOnline: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val avatarKey: String = "bybit_vip"
)

@Entity(tableName = "user_invitations")
data class UserInvitationEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val role: UserRole,
    val marketZone: String,
    val invitedBy: String,
    val inviteCode: String,
    val status: String = "EN_ATTENTE", // EN_ATTENTE, ACCEPTEE, EXPIREE
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String, // e.g. "CLI-001"
    val fullName: String,
    val phone: String,
    val email: String = "",
    val quarter: String = "", // Quartier de résidence
    val marketZone: String, // e.g. "Marché de Kituku", "Marché Alanine", "Marché Virunga"...
    val activityType: String, // e.g. "Commerce de vivres", "Couture & Vente pagne", "Boutique"
    val gender: String = "Femme", // "Homme" ou "Femme"
    val registeredDate: Long = System.currentTimeMillis(),
    val isIdVerified: Boolean = false,
    val idCardNumber: String = "", // Numéro de pièce d'identité (facultatif)
    val guarantorName: String = "",
    val guarantorPhone: String = "",
    val status: String = "ACTIF", // ACTIF, INACTIF, EN_RETARD
    val assignedAgentId: String? = null,
    val notes: String = "",
    val avatarUrl: String? = null,
    val avatarKey: String = "bybit_female",
    val isOnline: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "loan_applications")
data class LoanApplicationEntity(
    @PrimaryKey val id: String, // e.g. "APP-001"
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val amountRequested: Double,
    val durationDays: Int = 30,
    val purpose: String,
    val activityType: String,
    val gender: String = "Femme",
    val marketZone: String,
    val quarter: String = "", // Quartier
    val guarantorName: String = "",
    val guarantorPhone: String = "",
    val idCardNumber: String = "", // Numéro de pièce d'identité (facultatif)
    val documentUri: String? = null,
    val clientPhotoUri: String? = null, // Photo du client
    val dailyRate: Double = 0.035, // 3.5% par jour
    val dailyPaymentAmount: Double = 0.0, // Montant quotidien (ex: 200 000 * 3.5% = 7 000 FC)
    val acceptedTerms: Boolean = true, // Validation des règles et modalités contractuelles
    val status: LoanStatus = LoanStatus.SOUMISE,
    val submissionDate: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null,
    val reviewDate: Long? = null,
    val reviewNotes: String? = null,

    // NOUVEAUX CHAMPS PROFESSIONNELS MALAKI :
    // 2. Informations personnelles
    val lastName: String = "",
    val postName: String = "",
    val firstName: String = "",
    val birthDate: String = "",
    val maritalStatus: String = "Mariée", // Célibataire, Mariée, Veuve, Autre
    val dependentsCount: Int = 0,

    // 3. Localisation de la résidence
    val commune: String = "Goma", // Goma, Karisimbi, Nyiragongo, etc.
    val avenue: String = "",
    val landmark: String = "",
    val residenceDuration: String = "Plus de 3 ans", // Moins de 1 an, 1 à 3 ans, Plus de 3 ans

    // 4. Activité commerciale
    val businessType: String = "Alimentation", // Alimentation, Vente de vêtements, Produits agricoles, Restaurant / alimentation préparée, Petit commerce, Autre
    val businessLocation: String = "",
    val businessSeniority: String = "1 à 2 ans",
    val estimatedCapital: Double = 0.0,
    val dailyRevenue: Double = 0.0,

    // 5. Demande de crédit
    val currency: String = "CDF", // CDF ou USD
    val repaymentFrequency: String = "Quotidienne", // Quotidienne, Hebdomadaire, Mensuelle

    // 7 & 8 & 9. Signature biométrique & protection contre les modifications
    val signatureId: String? = null,
    val signatureMethod: String = "BIOMETRIC",
    val signedAt: Long? = null,
    val documentHash: String? = null, // SHA-256 du dossier
    val isLocked: Boolean = true, // Dossier verrouillé contre modification silencieuse
    val version: Int = 1,
    val parentApplicationId: String? = null
)

/**
 * Structure de preuve de signature électronique biométrique
 */
data class SignatureProof(
    val signatureId: String,
    val customerId: String,
    val applicationId: String,
    val signatureMethod: String = "BIOMETRIC",
    val signedAt: String,
    val documentHash: String,
    val status: String = "SIGNED"
)

@Entity(tableName = "credit_cycles")
data class CreditCycleEntity(
    @PrimaryKey val id: String, // e.g. "CY0001"
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val marketZone: String = "",
    val agentId: String? = null,
    val agentName: String? = null,
    val loanApplicationId: String? = null,
    val disbursementDate: Long = System.currentTimeMillis(),
    val principalAmount: Double, // Montant du prêt (ex: 200 000 FC)
    val dailyPaymentAmount: Double, // Paiement quotidien (ex: 7 000 FC)
    val durationDays: Int = 30, // Nombre de jours prévus (ex: 30)
    val totalContractual: Double, // Total contractuel (ex: 210 000 FC)
    val totalCollected: Double = 0.0, // Total encaissé
    val balanceRemaining: Double, // Solde à encaisser
    val recoveryRate: Double = 0.0, // Taux de recouvrement en %
    val grossProfit: Double = 0.0, // Profit brut réalisé
    val enterpriseShare: Double = 0.0, // Part entreprise (ex: 70%)
    val clientShare: Double = 0.0, // Part client (ex: 30%)
    val charges: Double = 0.0, // Charges d'exploitation
    val netProfit: Double = 0.0, // Profit net entreprise
    val status: CycleStatus = CycleStatus.EN_COURS,
    val daysElapsed: Int = 0,
    val daysLate: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String, // e.g. "PAY-001"
    val cycleId: String,
    val clientId: String,
    val clientName: String,
    val agentId: String,
    val agentName: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val dateString: String = "", // e.g. "04/10/2026"
    val dayNumber: Int, // Jour du cycle (ex: 1, 2, 3...)
    val expectedAmount: Double, // Montant attendu (ex: 7 000 FC)
    val collectedAmount: Double, // Montant encaissé réel (ex: 7 000 FC)
    val paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
    val notes: String = "",
    val isOfflineSynced: Boolean = true,
    val syncPending: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey val id: String, // e.g. "CSH-001"
    val date: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val type: CashMovementType, // ENTREE or SORTIE
    val category: CashMovementCategory,
    val reference: String, // e.g. "PAY-001" or "DISB-CY0001"
    val description: String,
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
    val recordedByUserId: String,
    val recordedByName: String,
    val receiptUri: String? = null,
    val isApproved: Boolean = true
)

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val organizationName: String = "MicroFinance Pro",
    val currency: String = "FC",
    val initialCapital: Double = 5000000.0, // 5 000 000 FC
    val targetCapital: Double = 15000000.0, // 15 000 000 FC
    val defaultDailyRate: Double = 0.035, // 3.5% par jour
    val minInitialLoan: Double = 50000.0, // Minimum à partir de 50 000 FC
    val maxInitialLoan: Double = 400000.0, // Maximum jusqu'à 400 000 FC
    val defaultDurationDays: Int = 30, // 30 jours
    val enterpriseShareRate: Double = 0.50, // 50% part entreprise
    val clientShareRate: Double = 0.50, // 50% part client (redistribution de 50% des bénéfices)
    val expenseAllowanceRate: Double = 0.05, // 5% charges
    val lossProvisionRate: Double = 0.02 // 2% pertes
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val clientId: String, // Used as the conversation room id
    val content: String,
    val attachmentUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    // Enhanced Telegram-style features:
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContentSnippet: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val isVoiceMessage: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val audioFilePath: String? = null,
    val deliveredUserIdsJson: String = "[]",
    val readUserIdsJson: String = "[]"
)

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey val id: String,
    val targetUserId: String? = null, // null = all in role / broadcast
    val targetRole: UserRole? = null, // null = all users
    val targetClientId: String? = null, // ID du client ciblé si applicable
    val destinationAddress: String? = null, // Adresse ou description du destinataire concerné
    val senderId: String? = null, // ID de l'auteur pour ne jamais notifier l'émetteur
    val senderName: String? = null,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionRoute: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userRole: UserRole,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val destination: String = "Direction & Système", // Adresse ou destination de l'activité (bénéficiaire, marché, caisse, etc.)
    val targetUserId: String? = null, // Utilisateur concerné / destinataire
    val targetClientId: String? = null, // Client concerné / destinataire
    val targetRole: UserRole? = null, // Rôle ciblé
    val timestamp: Long = System.currentTimeMillis()
)

