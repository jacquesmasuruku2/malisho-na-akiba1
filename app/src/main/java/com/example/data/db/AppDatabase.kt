package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppNotificationDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.BusinessSettingsDao
import com.example.data.dao.CashDao
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ClientDao
import com.example.data.dao.CreditCycleDao
import com.example.data.dao.LoanApplicationDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.UserDao
import com.example.data.model.AppNotificationEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CashMovementCategory
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashMovementType
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ClientEntity
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanStatus
import com.example.data.model.NotificationType
import com.example.data.model.PaymentEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserInvitationEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        UserInvitationEntity::class,
        ClientEntity::class,
        LoanApplicationEntity::class,
        CreditCycleEntity::class,
        PaymentEntity::class,
        CashMovementEntity::class,
        BusinessSettingsEntity::class,
        ChatMessageEntity::class,
        AppNotificationEntity::class,
        AuditLogEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun clientDao(): ClientDao
    abstract fun loanApplicationDao(): LoanApplicationDao
    abstract fun creditCycleDao(): CreditCycleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cashDao(): CashDao
    abstract fun businessSettingsDao(): BusinessSettingsDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: getDatabase(context, CoroutineScope(Dispatchers.IO + SupervisorJob()))
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "malaki_microcredit_clean.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // Also trigger background verify & seed to guarantee starter data availability
                scope.launch(Dispatchers.IO) {
                    seedDatabaseIfEmpty(instance)
                }
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabaseIfEmpty(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabaseIfEmpty(database)
                    }
                }
            }
        }

        suspend fun seedDatabaseIfEmpty(database: AppDatabase) {
            val now = System.currentTimeMillis()

            // 1. Paramétrage institutionnel officiel MALISHO NA AKIBA
            val existingSettings = database.businessSettingsDao().getSettingsDirect()
            if (existingSettings == null) {
                val settings = BusinessSettingsEntity(
                    id = 1,
                    organizationName = "MALISHO NA AKIBA",
                    currency = "FC",
                    initialCapital = 5000000.0,
                    targetCapital = 15000000.0,
                    defaultDailyRate = 0.035, // 3.5% journalier
                    minInitialLoan = 50000.0,
                    maxInitialLoan = 400000.0,
                    defaultDurationDays = 30,
                    enterpriseShareRate = 0.50,
                    clientShareRate = 0.50,
                    expenseAllowanceRate = 0.05,
                    lossProvisionRate = 0.02
                )
                database.businessSettingsDao().insertOrUpdateSettings(settings)
            }

            // 2. Utilisateurs : Directeur et Agent de terrain
            val existingUsers = database.userDao().getAllUsersDirect()
            if (existingUsers.isEmpty()) {
                val primaryAdmin = UserEntity(
                    id = "USR-001",
                    fullName = "Jacques Masuruku",
                    phone = "+243 970 000 001",
                    email = "jacquesmasuruku2@gmail.com",
                    role = UserRole.GESTIONNAIRE,
                    marketZone = "Direction Générale",
                    isActive = true,
                    createdDate = now,
                    pinCode = "1234",
                    username = "directeur",
                    isOnline = true,
                    lastActiveTimestamp = now
                )
                val fieldAgent = UserEntity(
                    id = "USR-002",
                    fullName = "Erick Mugisho",
                    phone = "+243 990 000 002",
                    email = "erick.mugisho@malisho.cd",
                    role = UserRole.AGENT,
                    marketZone = "Marché de Kituku",
                    isActive = true,
                    createdDate = now,
                    pinCode = "1234",
                    username = "agent1",
                    isOnline = true,
                    lastActiveTimestamp = now
                )
                database.userDao().insertUser(primaryAdmin)
                database.userDao().insertUser(fieldAgent)
            }

            // Vérifier et garantir la présence du compte Gestionnaire pour Bernard Bwema
            val bernardUser = database.userDao().findUserByIdentifier("bernardbwema@gmail.com")
            if (bernardUser == null) {
                val bernardAdmin = UserEntity(
                    id = "USR-000",
                    fullName = "Bernard Bwema",
                    phone = "+243 970 000 000",
                    email = "bernardbwema@gmail.com",
                    role = UserRole.GESTIONNAIRE,
                    marketZone = "Direction Générale",
                    isActive = true,
                    createdDate = now,
                    pinCode = "1234",
                    username = "bernard",
                    isOnline = true,
                    lastActiveTimestamp = now
                )
                database.userDao().insertUser(bernardAdmin)
            }

            // 3. Clients initiaux
            val existingClients = database.clientDao().getAllClientsDirect()
            if (existingClients.isEmpty()) {
                val client1 = ClientEntity(
                    id = "CLI-001",
                    fullName = "Furaha Zawadi",
                    phone = "+243 971 123 456",
                    email = "furaha.zawadi@gmail.com",
                    quarter = "Kyeshero",
                    marketZone = "Marché de Kituku",
                    activityType = "Vente de poissons frais",
                    gender = "Femme",
                    idCardNumber = "ID-NK-084920",
                    guarantorName = "Justin Zawadi",
                    guarantorPhone = "+243 998 111 222",
                    registeredDate = now - 20 * 86400000L,
                    status = "ACTIF",
                    assignedAgentId = "USR-002",
                    isOnline = true,
                    lastActiveTimestamp = now - 60000L
                )
                val client2 = ClientEntity(
                    id = "CLI-002",
                    fullName = "Neema Bahati",
                    phone = "+243 992 234 567",
                    email = "neema.bahati@gmail.com",
                    quarter = "Ndosho",
                    marketZone = "Marché Alanine",
                    activityType = "Commerce de vivres frais",
                    gender = "Femme",
                    idCardNumber = "ID-NK-051833",
                    guarantorName = "Amani Bahati",
                    guarantorPhone = "+243 973 444 555",
                    registeredDate = now - 15 * 86400000L,
                    status = "ACTIF",
                    assignedAgentId = "USR-002",
                    isOnline = true,
                    lastActiveTimestamp = now - 180000L
                )
                val client3 = ClientEntity(
                    id = "CLI-003",
                    fullName = "Faida Tumaini",
                    phone = "+243 813 345 678",
                    email = "faida.tumaini@gmail.com",
                    quarter = "Birere",
                    marketZone = "Grand Marché Birere",
                    activityType = "Vente pagnes et friperie",
                    gender = "Femme",
                    idCardNumber = "ID-NK-092711",
                    guarantorName = "Baraka Tumaini",
                    guarantorPhone = "+243 821 777 888",
                    registeredDate = now - 10 * 86400000L,
                    status = "ACTIF",
                    assignedAgentId = "USR-002",
                    isOnline = false,
                    lastActiveTimestamp = now - 3600000L
                )
                database.clientDao().insertClient(client1)
                database.clientDao().insertClient(client2)
                database.clientDao().insertClient(client3)

                // 4. Cycles de crédit actifs à 3,5% / jour
                val cycle1 = CreditCycleEntity(
                    id = "CY-1001",
                    clientId = client1.id,
                    clientName = client1.fullName,
                    clientPhone = client1.phone,
                    marketZone = client1.marketZone,
                    agentId = "USR-002",
                    agentName = "Erick Mugisho",
                    disbursementDate = now - 3 * 86400000L,
                    principalAmount = 150000.0,
                    dailyPaymentAmount = 5250.0, // 3,5% de 150 000 FC
                    durationDays = 30,
                    totalContractual = 157500.0,
                    totalCollected = 15750.0,
                    balanceRemaining = 141750.0,
                    recoveryRate = 10.0,
                    status = CycleStatus.EN_COURS,
                    daysElapsed = 3,
                    daysLate = 0,
                    notes = "Crédit commercial Kituku. Taux 3,5% respecté."
                )
                val cycle2 = CreditCycleEntity(
                    id = "CY-1002",
                    clientId = client2.id,
                    clientName = client2.fullName,
                    clientPhone = client2.phone,
                    marketZone = client2.marketZone,
                    agentId = "USR-002",
                    agentName = "Erick Mugisho",
                    disbursementDate = now - 6 * 86400000L,
                    principalAmount = 100000.0,
                    dailyPaymentAmount = 3500.0, // 3,5% de 100 000 FC
                    durationDays = 30,
                    totalContractual = 105000.0,
                    totalCollected = 21000.0,
                    balanceRemaining = 84000.0,
                    recoveryRate = 20.0,
                    status = CycleStatus.EN_COURS,
                    daysElapsed = 6,
                    daysLate = 0,
                    notes = "Crédit régulier Marché Alanine."
                )
                database.creditCycleDao().insertCycle(cycle1)
                database.creditCycleDao().insertCycle(cycle2)

                // 5. Mouvements de caisse initiaux
                database.cashDao().insertMovement(
                    CashMovementEntity(
                        id = "CSH-INIT-CAPITAL",
                        date = now - 20 * 86400000L,
                        dateString = "01/10/2026",
                        type = CashMovementType.ENTREE,
                        category = CashMovementCategory.APPORT_CAPITAL,
                        reference = "DOTATION-01",
                        description = "Apport de capital de lancement MALISHO NA AKIBA",
                        amount = 3500000.0,
                        paymentMethod = PaymentMethod.BANQUE,
                        recordedByUserId = "USR-001",
                        recordedByName = "Jacques Masuruku",
                        isApproved = true
                    )
                )

                // 6. Messages de bienvenue et discussion privée de test
                val welcomeMsg = ChatMessageEntity(
                    id = "MSG-WELCOME-01",
                    senderId = "USR-001",
                    senderName = "Jacques Masuruku",
                    senderRole = UserRole.GESTIONNAIRE,
                    clientId = "CLI-001",
                    content = "Bonjour Madame Furaha Zawadi, bienvenue chez MALISHO NA AKIBA. Votre crédit de 150 000 FC au Marché de Kituku est actif. Vous pouvez nous écrire ou envoyer vos messages vocaux directement ici.",
                    timestamp = now - 3600000L,
                    isRead = true
                )
                val directTestMsg = ChatMessageEntity(
                    id = "MSG-DIRECT-01",
                    senderId = "USR-002",
                    senderName = "Erick Mugisho",
                    senderRole = UserRole.AGENT,
                    clientId = "DIRECT_USR-001_USR-002",
                    content = "Bonjour Monsieur le Directeur, tous les recouvrements du marché de Kituku sont à jour pour ce matin.",
                    timestamp = now - 1800000L,
                    isRead = true
                )
                val communityYesterdayMsg = ChatMessageEntity(
                    id = "MSG-COMMUNITY-YESTERDAY",
                    senderId = "USR-002",
                    senderName = "Erick Mugisho",
                    senderRole = UserRole.AGENT,
                    clientId = "COMMUNITY_GROUP",
                    content = "Rapport de terrain d'hier : Tous les commerçants du Marché de Kituku ont été sensibilisés au remboursement ponctuel.",
                    timestamp = now - 86400000L - 3600000L,
                    isRead = true
                )
                val communityWelcomeMsg = ChatMessageEntity(
                    id = "MSG-COMMUNITY-01",
                    senderId = "USR-001",
                    senderName = "Jacques Masuruku",
                    senderRole = UserRole.GESTIONNAIRE,
                    clientId = "COMMUNITY_GROUP",
                    content = "Bienvenue dans le salon communautaire MALISHO NA AKIBA ! Cet espace d'échange est ouvert à toute l'équipe et aux membres pour les annonces officielles, partages d'expériences et entraide mutuelle.",
                    timestamp = now - 7200000L,
                    isRead = true,
                    isPinned = true,
                    pinnedAt = now - 7200000L
                )
                val communityReplyMsg = ChatMessageEntity(
                    id = "MSG-COMMUNITY-REPLY-01",
                    senderId = "USR-001",
                    senderName = "Jacques Masuruku",
                    senderRole = UserRole.GESTIONNAIRE,
                    clientId = "COMMUNITY_GROUP",
                    content = "Félicitations pour ces résultats Erick ! La direction valide la progression.",
                    timestamp = now - 1800000L,
                    isRead = true,
                    replyToMessageId = "MSG-COMMUNITY-YESTERDAY",
                    replyToSenderName = "Erick Mugisho",
                    replyToContentSnippet = "Rapport de terrain d'hier : Tous les commerçants..."
                )
                database.chatMessageDao().insertMessage(welcomeMsg)
                database.chatMessageDao().insertMessage(directTestMsg)
                database.chatMessageDao().insertMessage(communityYesterdayMsg)
                database.chatMessageDao().insertMessage(communityWelcomeMsg)
                database.chatMessageDao().insertMessage(communityReplyMsg)

                // 7. Activités et notifications initiales avec destination et adresse précises
                val notifWelcome = AppNotificationEntity(
                    id = "NOTIF-WELCOME-01",
                    targetRole = null, // Visible par tous les membres
                    destinationAddress = "Tous les membres • Réseau MALISHO NA AKIBA",
                    senderId = "SYSTEM",
                    senderName = "Direction Générale",
                    title = "Bienvenue sur MALISHO NA AKIBA",
                    message = "Plateforme opérationnelle de microcrédits et épargne pour les marchés de Goma.",
                    type = NotificationType.SYSTEM,
                    timestamp = now - 86400000L,
                    isRead = false,
                    actionRoute = "dashboard"
                )
                val notifDisburse = AppNotificationEntity(
                    id = "NOTIF-DISBURSE-01",
                    targetRole = UserRole.GESTIONNAIRE,
                    targetClientId = "CLI-001",
                    destinationAddress = "Furaha Zawadi • Marché de Kituku",
                    senderId = "USR-002",
                    senderName = "Erick Mugisho",
                    title = "Décaissement validé",
                    message = "Crédit de 150 000 FC octroyé à Furaha Zawadi (Marché de Kituku).",
                    type = NotificationType.DECAISSEMENT,
                    timestamp = now - 43200000L,
                    isRead = false,
                    actionRoute = "cycles"
                )
                val notifRecouvr = AppNotificationEntity(
                    id = "NOTIF-RECOUVR-01",
                    targetRole = UserRole.GESTIONNAIRE,
                    targetClientId = "CLI-001",
                    destinationAddress = "Direction • Encaissement Furaha Zawadi",
                    senderId = "USR-002",
                    senderName = "Erick Mugisho",
                    title = "Recouvrement journalier enregistré",
                    message = "Paiement de 5 250 FC perçu par Erick Mugisho auprès de Furaha Zawadi.",
                    type = NotificationType.PAIEMENT,
                    timestamp = now - 7200000L,
                    isRead = false,
                    actionRoute = "record_payment"
                )
                val notifCaisse = AppNotificationEntity(
                    id = "NOTIF-CAISSE-01",
                    targetRole = UserRole.GESTIONNAIRE,
                    destinationAddress = "Caisse Centrale • Direction Générale",
                    senderId = "USR-001",
                    senderName = "Jacques Masuruku",
                    title = "Solde de caisse opérationnel",
                    message = "Dotation initiale de capital de 5 000 000 FC enregistrée en caisse principale.",
                    type = NotificationType.CAISSE,
                    timestamp = now - 3600000L,
                    isRead = false,
                    actionRoute = "caisse"
                )
                database.appNotificationDao().insertNotification(notifWelcome)
                database.appNotificationDao().insertNotification(notifDisburse)
                database.appNotificationDao().insertNotification(notifRecouvr)
                database.appNotificationDao().insertNotification(notifCaisse)

                // 8. Activités initiales dans le journal d'audit avec destinations
                val logCapital = AuditLogEntity(
                    id = "AUD-INIT-01",
                    userId = "USR-001",
                    userName = "Jacques Masuruku",
                    userRole = UserRole.GESTIONNAIRE,
                    action = "DOTATION_CAPITAL",
                    entityType = "CAISSE",
                    entityId = "CSH-INIT-001",
                    details = "Dotation initiale de capital de 5 000 000 FC en caisse centrale.",
                    destination = "Caisse Centrale • Direction Générale",
                    targetRole = UserRole.GESTIONNAIRE,
                    timestamp = now - 86400000L
                )
                val logDisb = AuditLogEntity(
                    id = "AUD-INIT-02",
                    userId = "USR-001",
                    userName = "Jacques Masuruku",
                    userRole = UserRole.GESTIONNAIRE,
                    action = "DECAISSEMENT_EFFECTUE",
                    entityType = "CREDIT_CYCLE",
                    entityId = "CY-001",
                    details = "Décaissement de 150 000 FC accordé à Furaha Zawadi (Marché de Kituku).",
                    destination = "Furaha Zawadi • Marché de Kituku",
                    targetClientId = "CLI-001",
                    targetUserId = "USR-CLI-001",
                    timestamp = now - 43200000L
                )
                val logPayment = AuditLogEntity(
                    id = "AUD-INIT-03",
                    userId = "USR-002",
                    userName = "Erick Mugisho",
                    userRole = UserRole.AGENT,
                    action = "PAIEMENT_ENREGISTRE",
                    entityType = "PAYMENT",
                    entityId = "PAY-INIT-001",
                    details = "Recouvrement journalier de 5 250 FC (Jour 1) perçu auprès de Furaha Zawadi.",
                    destination = "Furaha Zawadi • Client CLI-001",
                    targetClientId = "CLI-001",
                    targetUserId = "USR-CLI-001",
                    timestamp = now - 7200000L
                )
                database.auditLogDao().insertLog(logCapital)
                database.auditLogDao().insertLog(logDisb)
                database.auditLogDao().insertLog(logPayment)
            }
        }
    }
}
