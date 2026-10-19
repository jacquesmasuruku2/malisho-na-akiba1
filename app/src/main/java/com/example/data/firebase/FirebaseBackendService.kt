package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.model.AppNotificationEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CashMovementEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ClientEntity
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Enterprise Firebase Backend Service for MALISHO NA AKIBA (MALAKI).
 * Handles:
 * 1. Firebase Authentication (Email/Password, Google Auth, Anonymous fallback)
 * 2. Cloud Firestore Real-time synchronization (users, loans, transactions, audit_logs)
 * 3. Role-Based Access Control (RBAC)
 * 4. Error handling and syncing state indicators
 */
class FirebaseBackendService(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("Prêt pour synchronisation Firebase")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val listeners = mutableListOf<ListenerRegistration>()

    fun getActiveUserCredentials(): Triple<String, String, String> {
        val prefs1 = context.getSharedPreferences("microcredit_prefs", Context.MODE_PRIVATE)
        val prefs2 = context.getSharedPreferences("remix_microfinance_prefs", Context.MODE_PRIVATE)
        val id = prefs1.getString("active_user_id", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2.getString("active_user_id", "") ?: ""
        val name = prefs1.getString("active_account_name", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2.getString("active_account_name", "") ?: ""
        val email = prefs1.getString("active_account_email", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2.getString("active_account_email", "") ?: ""
        return Triple(id, name, email)
    }

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val app = try {
                FirebaseApp.getInstance()
            } catch (_: Exception) {
                FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            }

            val projectId = app.options.projectId ?: "gen-lang-client-0243230243"
            firebaseAuth = FirebaseAuth.getInstance(app)
            val dbId = try {
                context.getString(com.example.R.string.firestore_database_id)
            } catch (_: Exception) {
                ""
            }
            firestore = if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance(app)
            }

            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore?.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Persistence setting warning: ${e.message}")
            }

            _isInitialized.value = true
            _syncStatus.value = "Connecté à Firebase Cloud ($projectId)"
            Log.d(TAG, "Firebase initialized successfully for project $projectId")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
            _lastError.value = e.message
            _syncStatus.value = "Mode résilient hors-ligne actif (cache local Room)"
            _isInitialized.value = true
        }
    }

    // -------------------------------------------------------------
    // Firebase Authentication
    // -------------------------------------------------------------

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth?.currentUser ?: try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
    }

    suspend fun ensureAuth(): FirebaseUser? {
        val auth = firebaseAuth ?: try { FirebaseAuth.getInstance() } catch (_: Exception) { null } ?: return null
        val current = auth.currentUser
        if (current != null) return current
        return null
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser?> {
        val auth = firebaseAuth ?: try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        } ?: return Result.failure(Exception("Service Firebase Auth non initialisé"))
        _isSyncing.value = true
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            _isSyncing.value = false
            _syncStatus.value = "Connecté via Google Firebase : ${user?.email ?: user?.displayName}"
            Result.success(user)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.localizedMessage
            Log.e(TAG, "Firebase Google Auth error: ${e.message}")
            Result.failure(e)
        }
    }



    fun signOut() {
        try {
            firebaseAuth?.signOut()
            _syncStatus.value = "Déconnecté de Firebase"
        } catch (e: Exception) {
            Log.w(TAG, "SignOut warning: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Cloud Firestore Operations
    // -------------------------------------------------------------

    suspend fun saveUserToFirestore(user: UserEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to user.id,
                "fullName" to user.fullName,
                "phone" to user.phone,
                "email" to user.email,
                "role" to user.role.name,
                "marketZone" to user.marketZone,
                "isActive" to user.isActive,
                "isOnline" to user.isOnline,
                "lastActiveTimestamp" to user.lastActiveTimestamp,
                "lastLoginDate" to user.lastLoginDate
            )
            db.collection("users").document(user.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean, timestamp: Long): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = mapOf(
                "isOnline" to isOnline,
                "lastActiveTimestamp" to timestamp
            )
            db.collection("users").document(userId).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveLoanApplicationToFirestore(app: LoanApplicationEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to app.id,
                "clientId" to app.clientId,
                "clientName" to app.clientName,
                "clientPhone" to app.clientPhone,
                "amountRequested" to app.amountRequested,
                "durationDays" to app.durationDays,
                "purpose" to app.purpose,
                "activityType" to app.activityType,
                "marketZone" to app.marketZone,
                "guarantorName" to app.guarantorName,
                "guarantorPhone" to app.guarantorPhone,
                "idCardNumber" to app.idCardNumber,
                "documentUri" to (app.documentUri ?: ""),
                "status" to app.status.name,
                "submissionDate" to app.submissionDate,
                "signatureId" to (app.signatureId ?: ""),
                "signatureMethod" to app.signatureMethod,
                "signedAt" to (app.signedAt ?: app.submissionDate),
                "documentHash" to (app.documentHash ?: ""),
                "isLocked" to app.isLocked,
                "commune" to app.commune,
                "quarter" to app.quarter,
                "currency" to app.currency,
                "repaymentFrequency" to app.repaymentFrequency
            )
            db.collection("loans").document(app.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun saveCreditCycleToFirestore(cycle: CreditCycleEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to cycle.id,
                "clientId" to cycle.clientId,
                "clientName" to cycle.clientName,
                "clientPhone" to cycle.clientPhone,
                "principalAmount" to cycle.principalAmount,
                "totalContractual" to cycle.totalContractual,
                "totalCollected" to cycle.totalCollected,
                "balanceRemaining" to cycle.balanceRemaining,
                "recoveryRate" to cycle.recoveryRate,
                "status" to cycle.status.name,
                "disbursementDate" to cycle.disbursementDate
            )
            db.collection("loans").document(cycle.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun saveTransactionToFirestore(payment: PaymentEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to payment.id,
                "cycleId" to payment.cycleId,
                "clientId" to payment.clientId,
                "clientName" to payment.clientName,
                "agentId" to payment.agentId,
                "agentName" to payment.agentName,
                "paymentDate" to payment.paymentDate,
                "dateString" to payment.dateString,
                "dayNumber" to payment.dayNumber,
                "collectedAmount" to payment.collectedAmount,
                "paymentMethod" to payment.paymentMethod.name,
                "isOfflineSynced" to true
            )
            db.collection("transactions").document(payment.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun saveCashMovementToFirestore(movement: CashMovementEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to movement.id,
                "date" to movement.date,
                "dateString" to movement.dateString,
                "type" to movement.type.name,
                "category" to movement.category.name,
                "reference" to movement.reference,
                "description" to movement.description,
                "amount" to movement.amount,
                "paymentMethod" to movement.paymentMethod.name,
                "recordedByUserId" to movement.recordedByUserId,
                "recordedByName" to movement.recordedByName,
                "receiptUri" to (movement.receiptUri ?: "")
            )
            db.collection("transactions").document(movement.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun saveAuditLogToFirestore(log: AuditLogEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to log.id,
                "userId" to log.userId,
                "userName" to log.userName,
                "userRole" to log.userRole.name,
                "action" to log.action,
                "entityType" to log.entityType,
                "entityId" to log.entityId,
                "details" to log.details,
                "timestamp" to log.timestamp
            )
            db.collection("audit_logs").document(log.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveClientToFirestore(client: ClientEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to client.id,
                "fullName" to client.fullName,
                "phone" to client.phone,
                "email" to client.email,
                "quarter" to client.quarter,
                "marketZone" to client.marketZone,
                "activityType" to client.activityType,
                "gender" to client.gender,
                "status" to client.status,
                "assignedAgentId" to (client.assignedAgentId ?: ""),
                "isOnline" to client.isOnline,
                "lastActiveTimestamp" to client.lastActiveTimestamp,
                "registeredDate" to client.registeredDate
            )
            db.collection("clients").document(client.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun saveChatMessageToFirestore(msg: ChatMessageEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to msg.id,
                "senderId" to msg.senderId,
                "senderName" to msg.senderName,
                "senderRole" to msg.senderRole.name,
                "clientId" to msg.clientId,
                "content" to msg.content,
                "attachmentUri" to (msg.attachmentUri ?: ""),
                "timestamp" to msg.timestamp,
                "isRead" to msg.isRead,
                "replyToMessageId" to (msg.replyToMessageId ?: ""),
                "replyToSenderName" to (msg.replyToSenderName ?: ""),
                "replyToContentSnippet" to (msg.replyToContentSnippet ?: ""),
                "isPinned" to msg.isPinned,
                "pinnedAt" to (msg.pinnedAt ?: 0L),
                "isVoiceMessage" to msg.isVoiceMessage,
                "voiceDurationSeconds" to msg.voiceDurationSeconds,
                "audioFilePath" to (msg.audioFilePath ?: ""),
                "deliveredUserIdsJson" to msg.deliveredUserIdsJson,
                "readUserIdsJson" to msg.readUserIdsJson
            )
            db.collection("chat_messages").document(msg.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun markChatMessagesAsReadInFirestore(clientId: String, currentUserId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val querySnap = db.collection("chat_messages")
                .whereEqualTo("clientId", clientId)
                .get()
                .await()
            for (doc in querySnap.documents) {
                val senderId = doc.getString("senderId") ?: ""
                val isSender = senderId == currentUserId || senderId.removePrefix("USR-") == currentUserId.removePrefix("USR-")
                if (!isSender && doc.getBoolean("isRead") != true) {
                    doc.reference.update("isRead", true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            db.collection("users").document(userId).set(mapOf("avatarKey" to avatarUrl), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNotificationToFirestore(notif: AppNotificationEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        return try {
            val data = hashMapOf(
                "id" to notif.id,
                "targetUserId" to (notif.targetUserId ?: ""),
                "targetRole" to (notif.targetRole?.name ?: ""),
                "targetClientId" to (notif.targetClientId ?: ""),
                "destinationAddress" to (notif.destinationAddress ?: ""),
                "senderId" to (notif.senderId ?: ""),
                "senderName" to (notif.senderName ?: ""),
                "title" to notif.title,
                "message" to notif.message,
                "type" to notif.type.name,
                "timestamp" to notif.timestamp,
                "isRead" to notif.isRead,
                "actionRoute" to (notif.actionRoute ?: "")
            )
            db.collection("app_notifications").document(notif.id).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    fun startRealtimeSync(appDb: AppDatabase) {
        val db = firestore ?: return
        clearListeners()
        try {
            // Realtime sync for clients
            val clientsListener = db.collection("clients")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Clients listen warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val fullName = doc.getString("fullName") ?: continue
                                val phone = doc.getString("phone") ?: ""
                                val email = doc.getString("email") ?: ""
                                val quarter = doc.getString("quarter") ?: ""
                                val marketZone = doc.getString("marketZone") ?: "Marché de Kituku"
                                val activityType = doc.getString("activityType") ?: "Commerce général"
                                val gender = doc.getString("gender") ?: "Femme"
                                val status = doc.getString("status") ?: "ACTIF"
                                val assignedAgent = doc.getString("assignedAgentId")
                                val regDate = doc.getLong("registeredDate") ?: System.currentTimeMillis()
                                val isOnline = doc.getBoolean("isOnline") ?: false
                                val lastActive = doc.getLong("lastActiveTimestamp") ?: System.currentTimeMillis()
                                val existing = appDb.clientDao().getClientByIdDirect(id)
                                val clientEntity = existing?.copy(
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    quarter = quarter,
                                    marketZone = marketZone,
                                    activityType = activityType,
                                    gender = gender,
                                    status = status,
                                    assignedAgentId = if (assignedAgent.isNullOrEmpty()) existing.assignedAgentId else assignedAgent,
                                    isOnline = isOnline,
                                    lastActiveTimestamp = lastActive
                                ) ?: ClientEntity(
                                    id = id,
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    quarter = quarter,
                                    marketZone = marketZone,
                                    activityType = activityType,
                                    gender = gender,
                                    status = status,
                                    assignedAgentId = assignedAgent,
                                    registeredDate = regDate,
                                    isOnline = isOnline,
                                    lastActiveTimestamp = lastActive
                                )
                                appDb.clientDao().insertClient(clientEntity)
                            }
                        }
                    }
                }
            listeners.add(clientsListener)

            // Realtime sync for loans and credit cycles
            val loansListener = db.collection("loans")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Loans listen warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val clientId = doc.getString("clientId") ?: continue
                                val clientName = doc.getString("clientName") ?: ""
                                val clientPhone = doc.getString("clientPhone") ?: ""
                                val principal = doc.getDouble("principalAmount") ?: doc.getDouble("amountRequested") ?: 0.0
                                val statusStr = doc.getString("status") ?: "EN_COURS"

                                if (id.startsWith("CY")) {
                                    val totalContractual = doc.getDouble("totalContractual") ?: (principal * 1.05)
                                    val dailyPayment = doc.getDouble("dailyPaymentAmount") ?: if (totalContractual > 0) totalContractual / 30.0 else 0.0
                                    val totalCollected = doc.getDouble("totalCollected") ?: 0.0
                                    val balanceRemaining = doc.getDouble("balanceRemaining") ?: (totalContractual - totalCollected)
                                    val recRate = doc.getDouble("recoveryRate") ?: if (totalContractual > 0) totalCollected / totalContractual else 0.0
                                    val cycleStatus = try { CycleStatus.valueOf(statusStr) } catch (_: Exception) { CycleStatus.EN_COURS }
                                    val existingCycle = appDb.creditCycleDao().getCycleByIdDirect(id)
                                    val cycle = existingCycle?.copy(
                                        totalCollected = totalCollected,
                                        balanceRemaining = balanceRemaining,
                                        recoveryRate = recRate,
                                        status = cycleStatus
                                    ) ?: CreditCycleEntity(
                                        id = id,
                                        clientId = clientId,
                                        clientName = clientName,
                                        clientPhone = clientPhone,
                                        principalAmount = principal,
                                        dailyPaymentAmount = dailyPayment,
                                        totalContractual = totalContractual,
                                        totalCollected = totalCollected,
                                        balanceRemaining = balanceRemaining,
                                        recoveryRate = recRate,
                                        status = cycleStatus,
                                        disbursementDate = doc.getLong("disbursementDate") ?: System.currentTimeMillis()
                                    )
                                    appDb.creditCycleDao().insertCycle(cycle)
                                } else {
                                    val loanStatus = try { LoanStatus.valueOf(statusStr) } catch (_: Exception) { LoanStatus.SOUMISE }
                                    val existingApp = appDb.loanApplicationDao().getApplicationByIdDirect(id)
                                    val app = existingApp?.copy(status = loanStatus) ?: LoanApplicationEntity(
                                        id = id,
                                        clientId = clientId,
                                        clientName = clientName,
                                        clientPhone = clientPhone,
                                        amountRequested = principal,
                                        purpose = doc.getString("purpose") ?: "Activité commerciale",
                                        activityType = doc.getString("activityType") ?: "Commerce",
                                        marketZone = doc.getString("marketZone") ?: "Marché de Kituku",
                                        guarantorName = doc.getString("guarantorName") ?: "",
                                        guarantorPhone = doc.getString("guarantorPhone") ?: "",
                                        status = loanStatus,
                                        submissionDate = doc.getLong("submissionDate") ?: System.currentTimeMillis()
                                    )
                                    appDb.loanApplicationDao().insertApplication(app)
                                }
                            }
                        }
                    }
                }
            listeners.add(loansListener)

            // 3. Realtime sync for Chat Messages (Community & Direct 1-to-1)
            val chatListener = db.collection("chat_messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Chat listen warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            val (activeUserId, activeAccountName, activeAccountEmail) = getActiveUserCredentials()
                            val allClients = appDb.clientDao().getAllClientsDirect()
                            val currentUser = if (activeUserId.isNotEmpty()) appDb.userDao().getUserByIdDirect(activeUserId) else null

                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: continue
                                val senderName = doc.getString("senderName") ?: ""
                                val senderRoleStr = doc.getString("senderRole") ?: "AGENT"
                                val senderRole = try { UserRole.valueOf(senderRoleStr) } catch (_: Exception) { UserRole.AGENT }
                                val clientId = doc.getString("clientId") ?: "COMMUNITY_GROUP"
                                val content = doc.getString("content") ?: ""
                                val attachmentUri = doc.getString("attachmentUri")
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isRead = doc.getBoolean("isRead") ?: false
                                val replyToMessageId = doc.getString("replyToMessageId")
                                val replyToSenderName = doc.getString("replyToSenderName")
                                val replyToContentSnippet = doc.getString("replyToContentSnippet")
                                val isPinned = doc.getBoolean("isPinned") ?: false
                                val pinnedAt = doc.getLong("pinnedAt")
                                val isVoiceMessage = doc.getBoolean("isVoiceMessage") ?: false
                                val voiceDurationSeconds = doc.getLong("voiceDurationSeconds")?.toInt() ?: 0
                                val audioFilePath = doc.getString("audioFilePath") ?: attachmentUri
                                val deliveredUserIdsJson = doc.getString("deliveredUserIdsJson") ?: "[]"
                                val readUserIdsJson = doc.getString("readUserIdsJson") ?: "[]"

                                val existing = appDb.chatMessageDao().getMessageByIdDirect(id)
                                val msgEntity = ChatMessageEntity(
                                    id = id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    senderRole = senderRole,
                                    clientId = clientId,
                                    content = content,
                                    attachmentUri = attachmentUri,
                                    timestamp = timestamp,
                                    isRead = isRead,
                                    replyToMessageId = replyToMessageId,
                                    replyToSenderName = replyToSenderName,
                                    replyToContentSnippet = replyToContentSnippet,
                                    isPinned = isPinned,
                                    pinnedAt = pinnedAt,
                                    isVoiceMessage = isVoiceMessage,
                                    voiceDurationSeconds = voiceDurationSeconds,
                                    audioFilePath = audioFilePath,
                                    deliveredUserIdsJson = deliveredUserIdsJson,
                                    readUserIdsJson = readUserIdsJson
                                )
                                appDb.chatMessageDao().insertMessage(msgEntity)

                                // L'émetteur ne reçoit JAMAIS de notification pour ses propres messages
                                val isSender = (activeUserId.isNotEmpty() && (
                                    senderId.equals(activeUserId, ignoreCase = true) ||
                                    senderId.removePrefix("USR-").equals(activeUserId.removePrefix("USR-"), ignoreCase = true) ||
                                    senderId.equals("USR-$activeUserId", ignoreCase = true)
                                )) || (activeAccountName.isNotBlank() && senderName.trim().equals(activeAccountName.trim(), ignoreCase = true)) ||
                                (activeAccountEmail.isNotEmpty() && (senderId.equals(activeAccountEmail, ignoreCase = true) || senderName.equals(activeAccountEmail, ignoreCase = true)))

                                if (existing == null && !isSender && currentUser != null) {
                                    val canAccess = com.example.util.ChatSecurityHelper.canUserAccessChatRoom(currentUser, clientId, allClients)
                                    if (canAccess) {
                                        val notifTitle = if (clientId == "COMMUNITY_GROUP") {
                                            "Message de $senderName (Communauté)"
                                        } else {
                                            "Nouveau message • $senderName"
                                        }
                                        val notifText = if (isVoiceMessage) "🎤 Message vocal (${voiceDurationSeconds}s)" else content
                                        com.example.util.NotificationHelper.postSystemNotification(
                                            context = context,
                                            title = notifTitle,
                                            message = notifText,
                                            notificationId = id.hashCode(),
                                            targetRoute = "chat:$clientId",
                                            clientId = clientId
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            listeners.add(chatListener)

            // 4. Realtime sync for App Notifications
            val notifsListener = db.collection("app_notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Notifications listen warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            val (activeUserId, activeAccountName, activeAccountEmail) = getActiveUserCredentials()
                            val allClients = appDb.clientDao().getAllClientsDirect()
                            val currentUser = if (activeUserId.isNotEmpty()) appDb.userDao().getUserByIdDirect(activeUserId) else null

                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val targetUserId = doc.getString("targetUserId")
                                val targetRoleStr = doc.getString("targetRole")
                                val targetRole = try { targetRoleStr?.let { UserRole.valueOf(it) } } catch (_: Exception) { null }
                                val targetClientId = doc.getString("targetClientId")
                                val destAddress = doc.getString("destinationAddress")
                                val senderId = doc.getString("senderId")
                                val senderName = doc.getString("senderName")
                                val title = doc.getString("title") ?: "Notification"
                                val message = doc.getString("message") ?: ""
                                val typeStr = doc.getString("type") ?: "SYSTEM"
                                val type = try { com.example.data.model.NotificationType.valueOf(typeStr) } catch (_: Exception) { com.example.data.model.NotificationType.SYSTEM }
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isRead = doc.getBoolean("isRead") ?: false
                                val actionRoute = doc.getString("actionRoute") ?: "notifications"

                                val isSender = (activeUserId.isNotEmpty() && (
                                    senderId?.equals(activeUserId, ignoreCase = true) == true ||
                                    senderId?.removePrefix("USR-")?.equals(activeUserId.removePrefix("USR-"), ignoreCase = true) == true ||
                                    senderId?.equals("USR-$activeUserId", ignoreCase = true) == true
                                )) || (!senderName.isNullOrBlank() && activeAccountName.isNotBlank() && senderName.trim().equals(activeAccountName.trim(), ignoreCase = true)) ||
                                (activeAccountEmail.isNotEmpty() && (senderId?.equals(activeAccountEmail, ignoreCase = true) == true || senderName?.equals(activeAccountEmail, ignoreCase = true) == true))

                                val notifEntity = AppNotificationEntity(
                                    id = id,
                                    targetUserId = targetUserId,
                                    targetRole = targetRole,
                                    targetClientId = targetClientId,
                                    destinationAddress = destAddress,
                                    senderId = senderId,
                                    senderName = senderName,
                                    title = title,
                                    message = message,
                                    type = type,
                                    timestamp = timestamp,
                                    isRead = isRead,
                                    actionRoute = actionRoute
                                )

                                if (!isSender && com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notifEntity, currentUser, allClients)) {
                                    appDb.appNotificationDao().insertNotification(notifEntity)
                                }
                            }
                        }
                    }
                }
            listeners.add(notifsListener)

            // 5. Realtime sync for Users
            val usersListener = db.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Users listen warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val fullName = doc.getString("fullName") ?: continue
                                val phone = doc.getString("phone") ?: ""
                                val email = doc.getString("email") ?: ""
                                val roleStr = doc.getString("role") ?: "AGENT"
                                val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.AGENT }
                                val marketZone = doc.getString("marketZone") ?: "Marché de Kituku"
                                val isActive = doc.getBoolean("isActive") ?: true
                                val avatarKey = doc.getString("avatarKey") ?: ""
                                val isOnline = doc.getBoolean("isOnline") ?: false
                                val lastActive = doc.getLong("lastActiveTimestamp") ?: System.currentTimeMillis()
                                val existing = appDb.userDao().getUserByIdDirect(id)
                                val user = existing?.copy(
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    role = role,
                                    marketZone = marketZone,
                                    isActive = isActive,
                                    avatarKey = if (avatarKey.isNotEmpty()) avatarKey else existing.avatarKey,
                                    isOnline = isOnline,
                                    lastActiveTimestamp = lastActive
                                ) ?: UserEntity(
                                    id = id,
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    role = role,
                                    marketZone = marketZone,
                                    isActive = isActive,
                                    avatarKey = avatarKey,
                                    isOnline = isOnline,
                                    lastActiveTimestamp = lastActive
                                )
                                appDb.userDao().insertUser(user)
                            }
                        }
                    }
                }
            listeners.add(usersListener)

            _syncStatus.value = "Synchronisation temps réel active avec Cloud Firestore"
        } catch (e: Exception) {
            Log.w(TAG, "Error starting realtime sync: ${e.message}")
        }
    }

    suspend fun syncAllFromFirestore(appDb: AppDatabase): Result<Int> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        _isSyncing.value = true
        return try {
            var syncedCount = 0

            // 1. Sync Users from Firestore
            try {
                val usersSnap = db.collection("users").get().await()
                for (doc in usersSnap.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val fullName = doc.getString("fullName") ?: continue
                    val phone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
                    val roleStr = doc.getString("role") ?: "AGENT"
                    val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.AGENT }
                    val marketZone = doc.getString("marketZone") ?: "Marché de Kituku"
                    val isActive = doc.getBoolean("isActive") ?: true
                    val existing = appDb.userDao().getUserByIdDirect(id)
                    val user = existing?.copy(
                        fullName = fullName,
                        phone = phone,
                        email = email,
                        role = role,
                        marketZone = marketZone,
                        isActive = isActive
                    ) ?: UserEntity(
                        id = id,
                        fullName = fullName,
                        phone = phone,
                        email = email,
                        role = role,
                        marketZone = marketZone,
                        isActive = isActive
                    )
                    appDb.userDao().insertUser(user)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Users sync notice: ${e.message}")
            }

            // 2. Sync Clients from Firestore
            try {
                val clientsSnap = db.collection("clients").get().await()
                for (doc in clientsSnap.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val fullName = doc.getString("fullName") ?: continue
                    val phone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
                    val quarter = doc.getString("quarter") ?: ""
                    val marketZone = doc.getString("marketZone") ?: "Marché de Kituku"
                    val activityType = doc.getString("activityType") ?: "Commerce"
                    val gender = doc.getString("gender") ?: "Femme"
                    val status = doc.getString("status") ?: "ACTIF"
                    val assignedAgent = doc.getString("assignedAgentId")
                    val existing = appDb.clientDao().getClientByIdDirect(id)
                    val client = existing?.copy(
                        fullName = fullName,
                        phone = phone,
                        email = email,
                        quarter = quarter,
                        marketZone = marketZone,
                        activityType = activityType,
                        gender = gender,
                        status = status
                    ) ?: ClientEntity(
                        id = id,
                        fullName = fullName,
                        phone = phone,
                        email = email,
                        quarter = quarter,
                        marketZone = marketZone,
                        activityType = activityType,
                        gender = gender,
                        status = status,
                        assignedAgentId = assignedAgent
                    )
                    appDb.clientDao().insertClient(client)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Clients sync notice: ${e.message}")
            }

            // 3. Sync Loans and Credit Cycles from Firestore
            try {
                val loansSnap = db.collection("loans").get().await()
                for (doc in loansSnap.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val clientId = doc.getString("clientId") ?: continue
                    val clientName = doc.getString("clientName") ?: ""
                    val clientPhone = doc.getString("clientPhone") ?: ""
                    val principal = doc.getDouble("principalAmount") ?: doc.getDouble("amountRequested") ?: 0.0
                    val statusStr = doc.getString("status") ?: "EN_COURS"

                    if (id.startsWith("CY")) {
                        val totalContractual = doc.getDouble("totalContractual") ?: (principal * 1.05)
                        val dailyPayment = doc.getDouble("dailyPaymentAmount") ?: if (totalContractual > 0) totalContractual / 30.0 else 0.0
                        val totalCollected = doc.getDouble("totalCollected") ?: 0.0
                        val balanceRemaining = doc.getDouble("balanceRemaining") ?: (totalContractual - totalCollected)
                        val recRate = doc.getDouble("recoveryRate") ?: if (totalContractual > 0) totalCollected / totalContractual else 0.0
                        val cycleStatus = try { CycleStatus.valueOf(statusStr) } catch (_: Exception) { CycleStatus.EN_COURS }
                        val existingCycle = appDb.creditCycleDao().getCycleByIdDirect(id)
                        val cycle = existingCycle?.copy(
                            totalCollected = totalCollected,
                            balanceRemaining = balanceRemaining,
                            recoveryRate = recRate,
                            status = cycleStatus
                        ) ?: CreditCycleEntity(
                            id = id,
                            clientId = clientId,
                            clientName = clientName,
                            clientPhone = clientPhone,
                            principalAmount = principal,
                            dailyPaymentAmount = dailyPayment,
                            totalContractual = totalContractual,
                            totalCollected = totalCollected,
                            balanceRemaining = balanceRemaining,
                            recoveryRate = recRate,
                            status = cycleStatus,
                            disbursementDate = doc.getLong("disbursementDate") ?: System.currentTimeMillis()
                        )
                        appDb.creditCycleDao().insertCycle(cycle)
                        syncedCount++
                    } else {
                        val loanStatus = try { LoanStatus.valueOf(statusStr) } catch (_: Exception) { LoanStatus.SOUMISE }
                        val existingApp = appDb.loanApplicationDao().getApplicationByIdDirect(id)
                        val app = existingApp?.copy(status = loanStatus) ?: LoanApplicationEntity(
                            id = id,
                            clientId = clientId,
                            clientName = clientName,
                            clientPhone = clientPhone,
                            amountRequested = principal,
                            purpose = doc.getString("purpose") ?: "Activité commerciale",
                            activityType = doc.getString("activityType") ?: "Commerce",
                            marketZone = doc.getString("marketZone") ?: "Marché de Kituku",
                            status = loanStatus,
                            submissionDate = doc.getLong("submissionDate") ?: System.currentTimeMillis()
                        )
                        appDb.loanApplicationDao().insertApplication(app)
                        syncedCount++
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Loans sync notice: ${e.message}")
            }

            // 4. Sync Chat Messages from Firestore
            try {
                val chatSnap = db.collection("chat_messages").get().await()
                for (doc in chatSnap.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val senderId = doc.getString("senderId") ?: continue
                    val senderName = doc.getString("senderName") ?: ""
                    val senderRoleStr = doc.getString("senderRole") ?: "AGENT"
                    val senderRole = try { UserRole.valueOf(senderRoleStr) } catch (_: Exception) { UserRole.AGENT }
                    val clientId = doc.getString("clientId") ?: "COMMUNITY_GROUP"
                    val content = doc.getString("content") ?: ""
                    val attachmentUri = doc.getString("attachmentUri")
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val isRead = doc.getBoolean("isRead") ?: false
                    val replyToMessageId = doc.getString("replyToMessageId")
                    val replyToSenderName = doc.getString("replyToSenderName")
                    val replyToContentSnippet = doc.getString("replyToContentSnippet")
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    val pinnedAt = doc.getLong("pinnedAt")
                    val isVoiceMessage = doc.getBoolean("isVoiceMessage") ?: false
                    val voiceDurationSeconds = doc.getLong("voiceDurationSeconds")?.toInt() ?: 0
                    val audioFilePath = doc.getString("audioFilePath") ?: attachmentUri
                    val deliveredUserIdsJson = doc.getString("deliveredUserIdsJson") ?: "[]"
                    val readUserIdsJson = doc.getString("readUserIdsJson") ?: "[]"

                    val msg = ChatMessageEntity(
                        id = id,
                        senderId = senderId,
                        senderName = senderName,
                        senderRole = senderRole,
                        clientId = clientId,
                        content = content,
                        attachmentUri = attachmentUri,
                        timestamp = timestamp,
                        isRead = isRead,
                        replyToMessageId = replyToMessageId,
                        replyToSenderName = replyToSenderName,
                        replyToContentSnippet = replyToContentSnippet,
                        isPinned = isPinned,
                        pinnedAt = pinnedAt,
                        isVoiceMessage = isVoiceMessage,
                        voiceDurationSeconds = voiceDurationSeconds,
                        audioFilePath = audioFilePath,
                        deliveredUserIdsJson = deliveredUserIdsJson,
                        readUserIdsJson = readUserIdsJson
                    )
                    appDb.chatMessageDao().insertMessage(msg)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Chat sync notice: ${e.message}")
            }

            _isSyncing.value = false
            _syncStatus.value = "Synchronisation Firestore réussie ($syncedCount éléments reçus)"
            Result.success(syncedCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun uploadLocalDataToFirestore(appDb: AppDatabase): Result<Int> {
        val db = firestore ?: return Result.failure(Exception("Firestore non initialisé"))
        _isSyncing.value = true
        return try {
            var uploadedCount = 0

            // Upload users
            try {
                val users = appDb.userDao().getAllUsersDirect()
                for (u in users) {
                    saveUserToFirestore(u)
                    uploadedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Upload users error: ${e.message}")
            }

            // Upload clients
            try {
                val clients = appDb.clientDao().getAllClientsDirect()
                for (c in clients) {
                    saveClientToFirestore(c)
                    uploadedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Upload clients error: ${e.message}")
            }

            // Upload credit cycles
            try {
                val cycles = appDb.creditCycleDao().getAllCyclesDirect()
                for (cy in cycles) {
                    saveCreditCycleToFirestore(cy)
                    uploadedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Upload cycles error: ${e.message}")
            }

            // Upload chat messages
            try {
                val messages = appDb.chatMessageDao().getAllMessagesDirect()
                for (m in messages) {
                    saveChatMessageToFirestore(m)
                    uploadedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Upload chat messages error: ${e.message}")
            }

            _isSyncing.value = false
            _syncStatus.value = "Données locales synchronisées vers Firestore ($uploadedCount éléments envoyés)"
            Result.success(uploadedCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    fun clearListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    companion object {
        private const val TAG = "FirebaseBackendService"
    }
}
