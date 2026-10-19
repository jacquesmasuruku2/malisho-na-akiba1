package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppNotificationEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashMovementType
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ClientEntity
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserInvitationEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdDate DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdDate DESC")
    suspend fun getAllUsersDirect(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE (phone = :identifier OR LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:identifier)) LIMIT 1")
    suspend fun findUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'AGENT' AND isActive = 1")
    fun getActiveAgents(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("DELETE FROM users WHERE id != 'USR-001'")
    suspend fun deleteNonAdminUsers()

    @Query("UPDATE users SET isOnline = 0 WHERE id != :currentUserId")
    suspend fun setAllOtherUsersOffline(currentUserId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET role = :newRole WHERE id = :userId")
    suspend fun updateUserRole(userId: String, newRole: UserRole)

    @Query("UPDATE users SET role = :newRole, marketZone = :marketZone WHERE id = :userId")
    suspend fun updateUserRoleAndMarkets(userId: String, newRole: UserRole, marketZone: String)

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdDirect(id: String): UserEntity?

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun toggleUserActive(userId: String, isActive: Boolean)

    @Query("UPDATE users SET isOnline = :isOnline, lastActiveTimestamp = :timestamp WHERE id = :userId")
    suspend fun updateUserPresence(userId: String, isOnline: Boolean, timestamp: Long)

    @Query("UPDATE users SET avatarKey = :avatarKey WHERE id = :userId")
    suspend fun updateUserAvatarKey(userId: String, avatarKey: String)

    @Query("SELECT * FROM user_invitations ORDER BY createdAt DESC")
    fun getAllInvitations(): Flow<List<UserInvitationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: UserInvitationEntity)

    @Query("UPDATE user_invitations SET status = :status WHERE id = :id")
    suspend fun updateInvitationStatus(id: String, status: String)
}

@Dao
interface ClientDao {
    @Query("""
        SELECT * FROM clients 
        ORDER BY 
            CASE 
                WHEN id LIKE 'CLI-%' THEN CAST(SUBSTR(id, 5) AS INTEGER) 
                ELSE 999999 
            END ASC, 
            registeredDate ASC, 
            id ASC
    """)
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("""
        SELECT * FROM clients 
        ORDER BY 
            CASE 
                WHEN id LIKE 'CLI-%' THEN CAST(SUBSTR(id, 5) AS INTEGER) 
                ELSE 999999 
            END ASC, 
            registeredDate ASC, 
            id ASC
    """)
    suspend fun getAllClientsDirect(): List<ClientEntity>

    @Query("""
        SELECT * FROM clients 
        WHERE assignedAgentId = :agentId 
        ORDER BY 
            CASE 
                WHEN id LIKE 'CLI-%' THEN CAST(SUBSTR(id, 5) AS INTEGER) 
                ELSE 999999 
            END ASC, 
            registeredDate ASC, 
            id ASC
    """)
    fun getClientsByAgent(agentId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientById(id: String): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientByIdDirect(id: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("UPDATE clients SET status = :status WHERE id = :id")
    suspend fun updateClientStatus(id: String, status: String)

    @Query("UPDATE clients SET isOnline = :isOnline, lastActiveTimestamp = :timestamp WHERE id = :id")
    suspend fun updateClientPresence(id: String, isOnline: Boolean, timestamp: Long)

    @Query("UPDATE clients SET avatarKey = :avatarKey, avatarUrl = :avatarKey WHERE id = :id")
    suspend fun updateClientAvatarKey(id: String, avatarKey: String)

    @Query("UPDATE clients SET avatarUrl = :avatarUrl, avatarKey = :avatarUrl WHERE id = :id")
    suspend fun updateClientAvatarUrl(id: String, avatarUrl: String)

    @Query("SELECT COUNT(*) FROM clients")
    fun countClients(): Flow<Int>

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: String)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Query("UPDATE clients SET isOnline = 0")
    suspend fun setAllClientsOffline()
}

@Dao
interface LoanApplicationDao {
    @Query("SELECT * FROM loan_applications ORDER BY submissionDate DESC")
    fun getAllApplications(): Flow<List<LoanApplicationEntity>>

    @Query("SELECT * FROM loan_applications WHERE clientId = :clientId ORDER BY submissionDate DESC")
    fun getApplicationsByClient(clientId: String): Flow<List<LoanApplicationEntity>>

    @Query("SELECT * FROM loan_applications WHERE id = :id LIMIT 1")
    fun getApplicationById(id: String): Flow<LoanApplicationEntity?>

    @Query("SELECT * FROM loan_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationByIdDirect(id: String): LoanApplicationEntity?

    @Query("SELECT * FROM loan_applications ORDER BY submissionDate DESC")
    suspend fun getAllApplicationsDirect(): List<LoanApplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(app: LoanApplicationEntity)

    @Update
    suspend fun updateApplication(app: LoanApplicationEntity)

    @Query("UPDATE loan_applications SET status = :status, reviewedBy = :reviewer, reviewDate = :date, reviewNotes = :notes WHERE id = :id")
    suspend fun updateStatus(id: String, status: LoanStatus, reviewer: String?, notes: String?, date: Long?)

    @Query("UPDATE loan_applications SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun updateApplicationClientId(oldClientId: String, newClientId: String)

    @Query("DELETE FROM loan_applications")
    suspend fun deleteAllApplications()
}

@Dao
interface CreditCycleDao {
    @Query("SELECT * FROM credit_cycles ORDER BY disbursementDate DESC")
    fun getAllCycles(): Flow<List<CreditCycleEntity>>

    @Query("SELECT * FROM credit_cycles ORDER BY disbursementDate DESC")
    suspend fun getAllCyclesDirect(): List<CreditCycleEntity>

    @Query("SELECT * FROM credit_cycles WHERE agentId = :agentId ORDER BY disbursementDate DESC")
    fun getCyclesByAgent(agentId: String): Flow<List<CreditCycleEntity>>

    @Query("SELECT * FROM credit_cycles WHERE clientId = :clientId ORDER BY disbursementDate DESC")
    fun getCyclesByClient(clientId: String): Flow<List<CreditCycleEntity>>

    @Query("SELECT * FROM credit_cycles WHERE id = :id LIMIT 1")
    fun getCycleById(id: String): Flow<CreditCycleEntity?>

    @Query("SELECT * FROM credit_cycles WHERE id = :id LIMIT 1")
    suspend fun getCycleByIdDirect(id: String): CreditCycleEntity?

    @Query("SELECT * FROM credit_cycles WHERE status = 'EN_COURS' OR status = 'EN_RETARD'")
    fun getActiveCycles(): Flow<List<CreditCycleEntity>>

    @Query("SELECT * FROM credit_cycles WHERE status = 'EN_RETARD'")
    fun getLateCycles(): Flow<List<CreditCycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CreditCycleEntity)

    @Update
    suspend fun updateCycle(cycle: CreditCycleEntity)

    @Query("UPDATE credit_cycles SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun updateCycleClientId(oldClientId: String, newClientId: String)

    @Query("DELETE FROM credit_cycles")
    suspend fun deleteAllCycles()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE cycleId = :cycleId ORDER BY dayNumber ASC")
    fun getPaymentsByCycle(cycleId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsByClient(clientId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE agentId = :agentId ORDER BY paymentDate DESC")
    fun getPaymentsByAgent(agentId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE dateString = :dateString ORDER BY paymentDate DESC")
    fun getPaymentsByDate(dateString: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsDirect(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE syncPending = 1")
    fun getPendingSyncPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT COUNT(*) FROM payments WHERE syncPending = 1")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE syncPending = 1")
    suspend fun getPendingSyncPaymentsDirect(): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments WHERE syncPending = 1")
    suspend fun getPendingSyncCountDirect(): Int

    @Query("UPDATE payments SET syncPending = 0, isOfflineSynced = 1 WHERE id = :id")
    suspend fun markPaymentSynced(id: String)

    @Query("UPDATE payments SET syncPending = 1, isOfflineSynced = 0 WHERE id = :id")
    suspend fun markPaymentPendingSync(id: String)

    @Query("UPDATE payments SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun updatePaymentClientId(oldClientId: String, newClientId: String)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_movements ORDER BY date DESC")
    fun getAllMovements(): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE type = :type ORDER BY date DESC")
    fun getMovementsByType(type: CashMovementType): Flow<List<CashMovementEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM cash_movements WHERE type = 'ENTREE'")
    fun getTotalEntrees(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM cash_movements WHERE type = 'SORTIE'")
    fun getTotalSorties(): Flow<Double>

    @Query("SELECT * FROM cash_movements ORDER BY date DESC")
    suspend fun getAllMovementsDirect(): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity)

    @Query("DELETE FROM cash_movements")
    suspend fun deleteAllMovements()
}

@Dao
interface BusinessSettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: BusinessSettingsEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE clientId = :clientId ORDER BY timestamp ASC")
    fun getMessagesForClient(clientId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesDirect(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageByIdDirect(messageId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET content = :newContent, isEdited = 1, editedAt = :editedAt WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String, editedAt: Long)

    @Query("UPDATE chat_messages SET audioFilePath = COALESCE(:newAudioPath, audioFilePath), attachmentUri = COALESCE(:newAttachmentUri, attachmentUri) WHERE id = :messageId")
    suspend fun updateMessageMedia(messageId: String, newAudioPath: String?, newAttachmentUri: String?)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE clientId = :clientId AND senderId != :currentUserId AND senderId != ('USR-' || :currentUserId) AND ('USR-' || senderId) != :currentUserId")
    suspend fun markMessagesAsRead(clientId: String, currentUserId: String)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markSingleMessageAsRead(messageId: String)

    @Query("UPDATE chat_messages SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE id = :messageId")
    suspend fun updateMessagePinStatus(messageId: String, isPinned: Boolean, pinnedAt: Long?)

    @Query("UPDATE chat_messages SET deliveredUserIdsJson = :deliveredJson, readUserIdsJson = :readJson, isRead = :isRead WHERE id = :messageId")
    suspend fun updateDeliveryAndReadReceipts(messageId: String, deliveredJson: String, readJson: String, isRead: Boolean)

    @Query("SELECT * FROM chat_messages WHERE clientId = :clientId AND isPinned = 1 ORDER BY pinnedAt DESC LIMIT 1")
    fun getLatestPinnedMessage(clientId: String): Flow<ChatMessageEntity?>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isRead = 0")
    suspend fun getUnreadCountDirect(): Int

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications WHERE targetUserId = :userId OR targetRole = :userRole OR (targetUserId IS NULL AND targetRole IS NULL) ORDER BY timestamp DESC")
    fun getNotificationsForRoleAndUser(userRole: UserRole, userId: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE (targetUserId = :userId OR targetRole = :userRole OR (targetUserId IS NULL AND targetRole IS NULL)) AND isRead = 0")
    fun getUnreadCount(userRole: UserRole, userId: String): Flow<Int>

    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsDirect(): List<AppNotificationEntity>

    @Query("SELECT * FROM app_notifications WHERE isRead = 0 ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentUnreadDirect(): List<AppNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE actionRoute = 'chat' AND (targetUserId = :userId OR targetClientId = :clientId OR destinationAddress LIKE '%' || :clientId || '%')")
    suspend fun markChatNotificationsAsRead(userId: String, clientId: String)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    suspend fun getAllLogsDirect(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllLogs()
}
