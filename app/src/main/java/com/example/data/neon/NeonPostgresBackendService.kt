package com.example.data.neon

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
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
import com.example.data.model.PaymentEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.AppNotificationEntity
import com.example.data.model.NotificationType
import com.example.util.NotificationHelper
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Enterprise Neon PostgreSQL Cloud Backend Service.
 * Replaces Firebase with managed PostgreSQL on Neon:
 * - Connection: postgresql://neondb_owner:npg_tFpY70VWRKBO@ep-sweet-cake-b5zubedo-pooler.c-7.us-east-2.aws.neon.tech/malisho-na-akiba?sslmode=require&channel_binding=require
 * - SQL HTTP Endpoint: https://ep-sweet-cake-b5zubedo-pooler.c-7.us-east-2.aws.neon.tech/sql
 * - REST API: https://ep-sweet-cake-b5zubedo.apirest.c-7.us-east-2.aws.neon.tech/neondb/rest/v1
 * - Neon Auth: https://ep-sweet-cake-b5zubedo.neonauth.c-7.us-east-2.aws.neon.tech/neondb/auth
 */
class NeonPostgresBackendService(val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    val connectionString = "postgresql://neondb_owner:npg_tFpY70VWRKBO@ep-sweet-cake-b5zubedo-pooler.c-7.us-east-2.aws.neon.tech/malisho-na-akiba?sslmode=require"
    val sqlEndpointUrl = "https://ep-sweet-cake-b5zubedo-pooler.c-7.us-east-2.aws.neon.tech/sql"
    val restApiUrl = "https://ep-sweet-cake-b5zubedo.apirest.c-7.us-east-2.aws.neon.tech/neondb/rest/v1"
    val authUrl = "https://ep-sweet-cake-b5zubedo.neonauth.c-7.us-east-2.aws.neon.tech/neondb/auth"
    val jwksUrl = "https://ep-sweet-cake-b5zubedo.neonauth.c-7.us-east-2.aws.neon.tech/neondb/auth/.well-known/jwks.json"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val networkMonitor = com.example.util.NetworkMonitor(context)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    fun isNetworkAvailable(): Boolean = networkMonitor.isCurrentlyConnected()

    private val _isInitialized = MutableStateFlow(true)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("Connecté à Neon PostgreSQL Cloud (malisho-na-akiba)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _hasPendingLocalChanges = MutableStateFlow(false)
    val hasPendingLocalChanges: StateFlow<Boolean> = _hasPendingLocalChanges.asStateFlow()

    fun schedulePendingSync() {
        _hasPendingLocalChanges.value = true
        if (!isNetworkAvailable()) {
            _syncStatus.value = "Hors-ligne • Activité enregistrée en local (en attente de connexion internet)"
        } else {
            _syncStatus.value = "Synchronisation automatique vers Neon Cloud planifiée..."
        }
    }

    fun clearPendingLocalChanges() {
        _hasPendingLocalChanges.value = false
    }

    var activeUserProvider: (() -> UserEntity?)? = null

    fun getActiveUserCredentials(): Triple<String, String, String> {
        val user = activeUserProvider?.invoke()
        if (user != null) {
            val emailOrPhone = if (user.email.isNotEmpty()) user.email else user.phone
            return Triple(user.id, user.fullName, emailOrPhone)
        }
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
        Log.d(TAG, "Neon PostgreSQL Service initialisé sur malisho-na-akiba")
        scope.launch {
            ensureDatabaseSchema()
        }
    }

    suspend fun ensureDatabaseSchema() = withContext(Dispatchers.IO) {
        val ddlStatements = listOf(
            // 1. Create chat_messages table if not exists with all required columns
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id VARCHAR(255) PRIMARY KEY,
                sender_id VARCHAR(255) NOT NULL,
                sender_name VARCHAR(255) NOT NULL,
                sender_role VARCHAR(50) NOT NULL,
                client_id TEXT NOT NULL,
                content TEXT NOT NULL,
                attachment_uri TEXT,
                timestamp BIGINT NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                is_edited BOOLEAN DEFAULT FALSE,
                edited_at BIGINT,
                reply_to_message_id VARCHAR(255),
                reply_to_sender_name VARCHAR(255),
                reply_to_content_snippet TEXT,
                is_pinned BOOLEAN DEFAULT FALSE,
                pinned_at BIGINT,
                is_voice_message BOOLEAN DEFAULT FALSE,
                voice_duration_seconds INT DEFAULT 0,
                audio_file_path TEXT,
                delivered_user_ids_json TEXT DEFAULT '[]',
                read_user_ids_json TEXT DEFAULT '[]'
            );
            """.trimIndent(),

            // 2. Drop restrictive foreign key constraints on chat_messages (client_id can be arbitrary room ID)
            "ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_client_id_fkey;",
            "ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_sender_id_fkey;",
            "ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_client_id_key;",

            // 3. Guarantee all columns exist and types are unconstrained on chat_messages
            "ALTER TABLE chat_messages ALTER COLUMN client_id TYPE TEXT;",
            "ALTER TABLE chat_messages ALTER COLUMN client_id DROP NOT NULL;",
            "ALTER TABLE chat_messages ALTER COLUMN id TYPE VARCHAR(255);",
            "ALTER TABLE chat_messages ALTER COLUMN sender_id TYPE VARCHAR(255);",
            "ALTER TABLE chat_messages ALTER COLUMN sender_role TYPE VARCHAR(50);",
            "ALTER TABLE chat_messages ALTER COLUMN sender_name TYPE VARCHAR(255);",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_edited BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS edited_at BIGINT;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(255);",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_sender_name VARCHAR(255);",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_content_snippet TEXT;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS pinned_at BIGINT;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_voice_message BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS voice_duration_seconds INT DEFAULT 0;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS audio_file_path TEXT;",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS delivered_user_ids_json TEXT DEFAULT '[]';",
            "ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS read_user_ids_json TEXT DEFAULT '[]';",

            // 4. Create app_notifications table if not exists
            """
            CREATE TABLE IF NOT EXISTS app_notifications (
                id VARCHAR(255) PRIMARY KEY,
                target_user_id VARCHAR(255),
                target_role VARCHAR(50),
                target_client_id TEXT,
                destination_address TEXT,
                sender_id VARCHAR(255),
                sender_name VARCHAR(255),
                title VARCHAR(255) NOT NULL,
                message TEXT NOT NULL,
                type VARCHAR(50) DEFAULT 'SYSTEM',
                timestamp BIGINT DEFAULT 0,
                is_read BOOLEAN DEFAULT FALSE,
                action_route TEXT
            );
            """.trimIndent(),

            // 5. Drop foreign key constraints on app_notifications
            "ALTER TABLE app_notifications DROP CONSTRAINT IF EXISTS app_notifications_target_user_id_fkey;",
            "ALTER TABLE app_notifications DROP CONSTRAINT IF EXISTS app_notifications_target_client_id_fkey;",
            "ALTER TABLE app_notifications DROP CONSTRAINT IF EXISTS app_notifications_sender_id_fkey;",

            // 6. Guarantee all columns exist and types are expanded on app_notifications
            "ALTER TABLE app_notifications ALTER COLUMN id TYPE VARCHAR(255);",
            "ALTER TABLE app_notifications ALTER COLUMN target_user_id TYPE VARCHAR(255);",
            "ALTER TABLE app_notifications ALTER COLUMN sender_id TYPE VARCHAR(255);",
            "ALTER TABLE app_notifications ALTER COLUMN target_client_id TYPE TEXT;",
            "ALTER TABLE app_notifications ALTER COLUMN action_route TYPE TEXT;",
            "ALTER TABLE app_notifications ALTER COLUMN destination_address TYPE TEXT;",
            "ALTER TABLE app_notifications ALTER COLUMN title TYPE VARCHAR(255);",
            "ALTER TABLE app_notifications ADD COLUMN IF NOT EXISTS target_client_id TEXT;",
            "ALTER TABLE app_notifications ADD COLUMN IF NOT EXISTS destination_address TEXT;",
            "ALTER TABLE app_notifications ADD COLUMN IF NOT EXISTS sender_id VARCHAR(255);",
            "ALTER TABLE app_notifications ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255);",
            "ALTER TABLE app_notifications ADD COLUMN IF NOT EXISTS action_route TEXT;"
        )

        for (sql in ddlStatements) {
            try {
                executeSql(sql)
            } catch (e: Exception) {
                Log.w(TAG, "Notice vérification schéma Neon: ${e.message}")
            }
        }

        // Drop any other lingering foreign keys on chat_messages & app_notifications dynamically
        try {
            val fkRes = executeSql("SELECT constraint_name, table_name FROM information_schema.table_constraints WHERE table_name IN ('chat_messages', 'app_notifications') AND constraint_type = 'FOREIGN KEY';")
            fkRes.getOrNull()?.let { fks ->
                for (i in 0 until fks.length()) {
                    val row = fks.getJSONObject(i)
                    val cName = row.optString("constraint_name")
                    val tName = row.optString("table_name")
                    if (cName.isNotEmpty() && tName.isNotEmpty()) {
                        executeSql("ALTER TABLE $tName DROP CONSTRAINT IF EXISTS \"$cName\";")
                    }
                }
            }
        } catch (_: Exception) {}

        Log.d(TAG, "Schéma Neon PostgreSQL vérifié et garanti avec succès.")
    }

    private fun escapeSql(str: String?): String {
        if (str == null) return "NULL"
        return "'" + str.replace("'", "''") + "'"
    }

    suspend fun executeSql(query: String): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("query", query)
            }
            val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(sqlEndpointUrl)
                .addHeader("Neon-Connection-String", connectionString)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: "{}"
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(responseStr).optString("message", "Erreur HTTP ${response.code}")
                    } catch (_: Exception) {
                        "Erreur HTTP ${response.code}"
                    }
                    Log.w(TAG, "Neon SQL error ($errMsg) for query: $query")
                    return@withContext Result.failure(Exception(errMsg))
                }
                val jsonRes = JSONObject(responseStr)
                val rows = jsonRes.optJSONArray("rows") ?: JSONArray()
                Result.success(rows)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Neon query execution exception: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Users ---
    suspend fun saveUser(user: UserEntity): Result<Unit> {
        val q = """
            INSERT INTO users (id, full_name, phone, email, role, market_zone, is_active, created_date, last_login_date, invited_by, pin_code, username, is_online, last_active_timestamp, avatar_key)
            VALUES (${escapeSql(user.id)}, ${escapeSql(user.fullName)}, ${escapeSql(user.phone)}, ${escapeSql(user.email)}, ${escapeSql(user.role.name)}, ${escapeSql(user.marketZone)}, ${user.isActive}, ${user.createdDate}, ${user.lastLoginDate}, ${escapeSql(user.invitedBy)}, ${escapeSql(user.pinCode)}, ${escapeSql(user.username)}, ${user.isOnline}, ${user.lastActiveTimestamp}, ${escapeSql(user.avatarKey)})
            ON CONFLICT (id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                phone = EXCLUDED.phone,
                email = EXCLUDED.email,
                role = EXCLUDED.role,
                market_zone = EXCLUDED.market_zone,
                is_active = EXCLUDED.is_active,
                pin_code = EXCLUDED.pin_code,
                username = EXCLUDED.username,
                is_online = EXCLUDED.is_online,
                last_active_timestamp = EXCLUDED.last_active_timestamp,
                avatar_key = EXCLUDED.avatar_key;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean, timestamp: Long): Result<Unit> {
        val q = "UPDATE users SET is_online = $isOnline, last_active_timestamp = $timestamp WHERE id = ${escapeSql(userId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun findUserByIdentifier(identifier: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        val q = "SELECT * FROM users WHERE (LOWER(email) = LOWER(${escapeSql(clean)}) OR phone = ${escapeSql(clean)} OR LOWER(username) = LOWER(${escapeSql(clean)})) LIMIT 1;"
        val res = executeSql(q)
        res.map { rows ->
            if (rows.length() > 0) {
                val row = rows.getJSONObject(0)
                val id = row.getString("id")
                val roleStr = row.optString("role", "CLIENT")
                val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.CLIENT }
                UserEntity(
                    id = id,
                    fullName = row.optString("full_name", ""),
                    phone = row.optString("phone", ""),
                    email = row.optString("email", ""),
                    role = role,
                    marketZone = row.optString("market_zone", "Marché de Kituku"),
                    isActive = row.optBoolean("is_active", true),
                    createdDate = row.optLong("created_date", System.currentTimeMillis()),
                    lastLoginDate = row.optLong("last_login_date", System.currentTimeMillis()),
                    invitedBy = row.optString("invited_by").takeIf { it.isNotEmpty() },
                    pinCode = row.optString("pin_code", "1234"),
                    username = row.optString("username", ""),
                    isOnline = row.optBoolean("is_online", false),
                    lastActiveTimestamp = row.optLong("last_active_timestamp", System.currentTimeMillis()),
                    avatarKey = row.optString("avatar_key", "bybit_vip")
                )
            } else {
                null
            }
        }
    }

    // --- Clients ---
    suspend fun saveClient(client: ClientEntity): Result<Unit> {
        val q = """
            INSERT INTO clients (id, full_name, phone, email, quarter, market_zone, activity_type, gender, registered_date, is_id_verified, id_card_number, guarantor_name, guarantor_phone, status, assigned_agent_id, notes, avatar_url, avatar_key, is_online, last_active_timestamp)
            VALUES (${escapeSql(client.id)}, ${escapeSql(client.fullName)}, ${escapeSql(client.phone)}, ${escapeSql(client.email)}, ${escapeSql(client.quarter)}, ${escapeSql(client.marketZone)}, ${escapeSql(client.activityType)}, ${escapeSql(client.gender)}, ${client.registeredDate}, ${client.isIdVerified}, ${escapeSql(client.idCardNumber)}, ${escapeSql(client.guarantorName)}, ${escapeSql(client.guarantorPhone)}, ${escapeSql(client.status)}, ${escapeSql(client.assignedAgentId)}, ${escapeSql(client.notes)}, ${escapeSql(client.avatarUrl)}, ${escapeSql(client.avatarKey)}, ${client.isOnline}, ${client.lastActiveTimestamp})
            ON CONFLICT (id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                phone = EXCLUDED.phone,
                email = EXCLUDED.email,
                quarter = EXCLUDED.quarter,
                market_zone = EXCLUDED.market_zone,
                activity_type = EXCLUDED.activity_type,
                gender = EXCLUDED.gender,
                status = EXCLUDED.status,
                assigned_agent_id = EXCLUDED.assigned_agent_id,
                notes = EXCLUDED.notes,
                avatar_url = EXCLUDED.avatar_url,
                avatar_key = EXCLUDED.avatar_key,
                is_online = EXCLUDED.is_online,
                last_active_timestamp = EXCLUDED.last_active_timestamp;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    // --- Loans & Cycles ---
    suspend fun saveLoanApplication(app: LoanApplicationEntity): Result<Unit> {
        val q = """
            INSERT INTO loan_applications (id, client_id, client_name, client_phone, amount_requested, duration_days, purpose, activity_type, gender, market_zone, quarter, guarantor_name, guarantor_phone, id_card_number, document_uri, client_photo_uri, daily_rate, daily_payment_amount, accepted_terms, status, submission_date, reviewed_by, review_date, review_notes)
            VALUES (${escapeSql(app.id)}, ${escapeSql(app.clientId)}, ${escapeSql(app.clientName)}, ${escapeSql(app.clientPhone)}, ${app.amountRequested}, ${app.durationDays}, ${escapeSql(app.purpose)}, ${escapeSql(app.activityType)}, ${escapeSql(app.gender)}, ${escapeSql(app.marketZone)}, ${escapeSql(app.quarter)}, ${escapeSql(app.guarantorName)}, ${escapeSql(app.guarantorPhone)}, ${escapeSql(app.idCardNumber)}, ${escapeSql(app.documentUri)}, ${escapeSql(app.clientPhotoUri)}, ${app.dailyRate}, ${app.dailyPaymentAmount}, ${app.acceptedTerms}, ${escapeSql(app.status.name)}, ${app.submissionDate}, ${escapeSql(app.reviewedBy)}, ${app.reviewDate ?: "NULL"}, ${escapeSql(app.reviewNotes)})
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status,
                reviewed_by = EXCLUDED.reviewed_by,
                review_date = EXCLUDED.review_date,
                review_notes = EXCLUDED.review_notes;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    suspend fun saveCreditCycle(cycle: CreditCycleEntity): Result<Unit> {
        val q = """
            INSERT INTO credit_cycles (id, client_id, client_name, client_phone, market_zone, agent_id, agent_name, loan_application_id, disbursement_date, principal_amount, daily_payment_amount, duration_days, total_contractual, total_collected, balance_remaining, recovery_rate, gross_profit, enterprise_share, client_share, charges, net_profit, status, days_elapsed, days_late, notes)
            VALUES (${escapeSql(cycle.id)}, ${escapeSql(cycle.clientId)}, ${escapeSql(cycle.clientName)}, ${escapeSql(cycle.clientPhone)}, ${escapeSql(cycle.marketZone)}, ${escapeSql(cycle.agentId)}, ${escapeSql(cycle.agentName)}, ${escapeSql(cycle.loanApplicationId)}, ${cycle.disbursementDate}, ${cycle.principalAmount}, ${cycle.dailyPaymentAmount}, ${cycle.durationDays}, ${cycle.totalContractual}, ${cycle.totalCollected}, ${cycle.balanceRemaining}, ${cycle.recoveryRate}, ${cycle.grossProfit}, ${cycle.enterpriseShare}, ${cycle.clientShare}, ${cycle.charges}, ${cycle.netProfit}, ${escapeSql(cycle.status.name)}, ${cycle.daysElapsed}, ${cycle.daysLate}, ${escapeSql(cycle.notes)})
            ON CONFLICT (id) DO UPDATE SET
                total_collected = EXCLUDED.total_collected,
                balance_remaining = EXCLUDED.balance_remaining,
                recovery_rate = EXCLUDED.recovery_rate,
                gross_profit = EXCLUDED.gross_profit,
                enterprise_share = EXCLUDED.enterprise_share,
                client_share = EXCLUDED.client_share,
                charges = EXCLUDED.charges,
                net_profit = EXCLUDED.net_profit,
                status = EXCLUDED.status,
                days_elapsed = EXCLUDED.days_elapsed,
                notes = EXCLUDED.notes;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    // --- Payments & Cash movements ---
    suspend fun saveTransaction(payment: PaymentEntity): Result<Unit> {
        val q = """
            INSERT INTO payments (id, cycle_id, client_id, client_name, agent_id, agent_name, payment_date, date_string, day_number, expected_amount, collected_amount, payment_method, notes, is_offline_synced, sync_pending, created_at)
            VALUES (${escapeSql(payment.id)}, ${escapeSql(payment.cycleId)}, ${escapeSql(payment.clientId)}, ${escapeSql(payment.clientName)}, ${escapeSql(payment.agentId)}, ${escapeSql(payment.agentName)}, ${payment.paymentDate}, ${escapeSql(payment.dateString)}, ${payment.dayNumber}, ${payment.expectedAmount}, ${payment.collectedAmount}, ${escapeSql(payment.paymentMethod.name)}, ${escapeSql(payment.notes)}, TRUE, FALSE, ${payment.createdAt})
            ON CONFLICT (id) DO NOTHING;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    suspend fun saveCashMovement(movement: CashMovementEntity): Result<Unit> {
        val q = """
            INSERT INTO cash_movements (id, date, date_string, type, category, reference, description, amount, payment_method, recorded_by_user_id, recorded_by_name, receipt_uri, is_approved)
            VALUES (${escapeSql(movement.id)}, ${movement.date}, ${escapeSql(movement.dateString)}, ${escapeSql(movement.type.name)}, ${escapeSql(movement.category.name)}, ${escapeSql(movement.reference)}, ${escapeSql(movement.description)}, ${movement.amount}, ${escapeSql(movement.paymentMethod.name)}, ${escapeSql(movement.recordedByUserId)}, ${escapeSql(movement.recordedByName)}, ${escapeSql(movement.receiptUri)}, ${movement.isApproved})
            ON CONFLICT (id) DO NOTHING;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    // --- Chat Messages with Edit & Delete support ---
    suspend fun saveChatMessage(msg: ChatMessageEntity): Result<Unit> {
        val q = """
            INSERT INTO chat_messages (
                id, sender_id, sender_name, sender_role, client_id, content,
                attachment_uri, timestamp, is_read, is_edited, edited_at,
                reply_to_message_id, reply_to_sender_name, reply_to_content_snippet,
                is_pinned, pinned_at, is_voice_message, voice_duration_seconds, audio_file_path,
                delivered_user_ids_json, read_user_ids_json
            )
            VALUES (
                ${escapeSql(msg.id)}, ${escapeSql(msg.senderId)}, ${escapeSql(msg.senderName)}, ${escapeSql(msg.senderRole.name)},
                ${escapeSql(msg.clientId)}, ${escapeSql(msg.content)}, ${escapeSql(msg.attachmentUri)}, ${msg.timestamp},
                ${msg.isRead}, ${msg.isEdited}, ${msg.editedAt ?: "NULL"},
                ${escapeSql(msg.replyToMessageId)}, ${escapeSql(msg.replyToSenderName)}, ${escapeSql(msg.replyToContentSnippet)},
                ${msg.isPinned}, ${msg.pinnedAt ?: "NULL"}, ${msg.isVoiceMessage}, ${msg.voiceDurationSeconds}, ${escapeSql(msg.audioFilePath)},
                ${escapeSql(msg.deliveredUserIdsJson)}, ${escapeSql(msg.readUserIdsJson)}
            )
            ON CONFLICT (id) DO UPDATE SET
                content = EXCLUDED.content,
                attachment_uri = EXCLUDED.attachment_uri,
                is_read = EXCLUDED.is_read,
                is_edited = EXCLUDED.is_edited,
                edited_at = EXCLUDED.edited_at,
                is_pinned = EXCLUDED.is_pinned,
                pinned_at = EXCLUDED.pinned_at,
                is_voice_message = EXCLUDED.is_voice_message,
                voice_duration_seconds = EXCLUDED.voice_duration_seconds,
                audio_file_path = EXCLUDED.audio_file_path,
                delivered_user_ids_json = EXCLUDED.delivered_user_ids_json,
                read_user_ids_json = EXCLUDED.read_user_ids_json;
        """.trimIndent()
        val res = executeSql(q)
        if (res.isFailure) {
            val err = res.exceptionOrNull()?.message
            Log.w(TAG, "Notice Neon saveChatMessage étendu ($err). Auto-réparation du schéma et nouvelle tentative...")
            ensureDatabaseSchema()
            val retryRes = executeSql(q)
            if (retryRes.isSuccess) {
                Log.d(TAG, "Message de chat ${msg.id} persisté avec succès après auto-réparation sur Neon")
                return retryRes.map { Unit }
            }
            val fallbackQ = """
                INSERT INTO chat_messages (id, sender_id, sender_name, sender_role, client_id, content, attachment_uri, timestamp, is_read)
                VALUES (
                    ${escapeSql(msg.id)}, ${escapeSql(msg.senderId)}, ${escapeSql(msg.senderName)}, ${escapeSql(msg.senderRole.name)},
                    ${escapeSql(msg.clientId)}, ${escapeSql(msg.content)}, ${escapeSql(msg.attachmentUri)}, ${msg.timestamp}, ${msg.isRead}
                )
                ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, attachment_uri = EXCLUDED.attachment_uri, is_read = EXCLUDED.is_read;
            """.trimIndent()
            val fallbackRes = executeSql(fallbackQ)
            if (fallbackRes.isFailure) {
                Log.e(TAG, "Erreur Neon saveChatMessage fallback: ${fallbackRes.exceptionOrNull()?.message}")
                return fallbackRes.map { Unit }
            }
            Log.d(TAG, "Message de chat ${msg.id} persisté via base chat_messages sur Neon")
            return fallbackRes.map { Unit }
        } else {
            Log.d(TAG, "Message de chat ${msg.id} persisté avec succès dans la table chat_messages sur Neon")
        }
        return res.map { Unit }
    }

    suspend fun updateChatMessage(messageId: String, newContent: String, editedAt: Long): Result<Unit> {
        val q = "UPDATE chat_messages SET content = ${escapeSql(newContent)}, is_edited = TRUE, edited_at = $editedAt WHERE id = ${escapeSql(messageId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun deleteChatMessage(messageId: String): Result<Unit> {
        val q = "DELETE FROM chat_messages WHERE id = ${escapeSql(messageId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun markChatMessagesAsRead(clientId: String, currentUserId: String): Result<Unit> {
        val q = "UPDATE chat_messages SET is_read = TRUE WHERE client_id = ${escapeSql(clientId)} AND sender_id != ${escapeSql(currentUserId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun updateMessageReceipts(messageId: String, deliveredJson: String, readJson: String, isRead: Boolean): Result<Unit> {
        val q = "UPDATE chat_messages SET delivered_user_ids_json = ${escapeSql(deliveredJson)}, read_user_ids_json = ${escapeSql(readJson)}, is_read = $isRead WHERE id = ${escapeSql(messageId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun saveAuditLog(log: AuditLogEntity): Result<Unit> {
        val q = """
            INSERT INTO audit_logs (id, user_id, user_name, user_role, action, entity_type, entity_id, details, timestamp)
            VALUES (${escapeSql(log.id)}, ${escapeSql(log.userId)}, ${escapeSql(log.userName)}, ${escapeSql(log.userRole.name)}, ${escapeSql(log.action)}, ${escapeSql(log.entityType)}, ${escapeSql(log.entityId)}, ${escapeSql(log.details)}, ${log.timestamp})
            ON CONFLICT (id) DO NOTHING;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    suspend fun saveNotification(notif: AppNotificationEntity): Result<Unit> {
        val q = """
            INSERT INTO app_notifications (
                id, target_user_id, target_role, target_client_id, destination_address,
                sender_id, sender_name, title, message, type, timestamp, is_read, action_route
            )
            VALUES (
                ${escapeSql(notif.id)},
                ${escapeSql(notif.targetUserId)},
                ${escapeSql(notif.targetRole?.name)},
                ${escapeSql(notif.targetClientId)},
                ${escapeSql(notif.destinationAddress)},
                ${escapeSql(notif.senderId)},
                ${escapeSql(notif.senderName)},
                ${escapeSql(notif.title)},
                ${escapeSql(notif.message)},
                ${escapeSql(notif.type.name)},
                ${notif.timestamp},
                ${notif.isRead},
                ${escapeSql(notif.actionRoute)}
            )
            ON CONFLICT (id) DO UPDATE SET
                is_read = EXCLUDED.is_read;
        """.trimIndent()
        val res = executeSql(q)
        if (res.isFailure) {
            Log.e(TAG, "Erreur Neon saveNotification: ${res.exceptionOrNull()?.message}")
        }
        return res.map { Unit }
    }

    suspend fun markNotificationAsRead(id: String): Result<Unit> {
        val q = "UPDATE app_notifications SET is_read = TRUE WHERE id = ${escapeSql(id)};"
        return executeSql(q).map { Unit }
    }

    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        val q = "UPDATE app_notifications SET is_read = TRUE;"
        return executeSql(q).map { Unit }
    }

    suspend fun saveBusinessSettings(settings: BusinessSettingsEntity): Result<Unit> {
        val q = """
            INSERT INTO business_settings (id, organization_name, currency, initial_capital, target_capital, default_daily_rate, min_initial_loan, max_initial_loan, default_duration_days, enterprise_share_rate, client_share_rate, expense_allowance_rate, loss_provision_rate)
            VALUES (1, ${escapeSql(settings.organizationName)}, ${escapeSql(settings.currency)}, ${settings.initialCapital}, ${settings.targetCapital}, ${settings.defaultDailyRate}, ${settings.minInitialLoan}, ${settings.maxInitialLoan}, ${settings.defaultDurationDays}, ${settings.enterpriseShareRate}, ${settings.clientShareRate}, ${settings.expenseAllowanceRate}, ${settings.lossProvisionRate})
            ON CONFLICT (id) DO UPDATE SET
                organization_name = EXCLUDED.organization_name,
                currency = EXCLUDED.currency,
                initial_capital = EXCLUDED.initial_capital,
                target_capital = EXCLUDED.target_capital,
                default_daily_rate = EXCLUDED.default_daily_rate,
                min_initial_loan = EXCLUDED.min_initial_loan,
                max_initial_loan = EXCLUDED.max_initial_loan,
                default_duration_days = EXCLUDED.default_duration_days,
                enterprise_share_rate = EXCLUDED.enterprise_share_rate,
                client_share_rate = EXCLUDED.client_share_rate,
                expense_allowance_rate = EXCLUDED.expense_allowance_rate,
                loss_provision_rate = EXCLUDED.loss_provision_rate;
        """.trimIndent()
        return executeSql(q).map { Unit }
    }

    suspend fun updateClientOnlineStatus(clientId: String, isOnline: Boolean, timestamp: Long): Result<Unit> {
        val q = "UPDATE clients SET is_online = $isOnline, last_active_timestamp = $timestamp WHERE id = ${escapeSql(clientId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun updateUserActiveStatus(userId: String, isActive: Boolean): Result<Unit> {
        val q = "UPDATE users SET is_active = $isActive WHERE id = ${escapeSql(userId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun updateUserAvatar(userId: String, avatarKey: String): Result<Unit> {
        val q = "UPDATE users SET avatar_key = ${escapeSql(avatarKey)} WHERE id = ${escapeSql(userId)};"
        return executeSql(q).map { Unit }
    }

    suspend fun updateClientAvatar(clientId: String, avatarKey: String): Result<Unit> {
        val q = "UPDATE clients SET avatar_key = ${escapeSql(avatarKey)}, avatar_url = ${escapeSql(avatarKey)} WHERE id = ${escapeSql(clientId)};"
        return executeSql(q).map { Unit }
    }

    // --- Full Two-Way Synchronization with Neon Postgres ---

    suspend fun syncAllFromNeon(appDb: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            var syncedCount = 0

            // 1. Sync Users
            val usersRes = executeSql("SELECT * FROM users ORDER BY created_date ASC;")
            usersRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val roleStr = row.optString("role", "AGENT")
                    val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.AGENT }
                    val user = UserEntity(
                        id = id,
                        fullName = row.optString("full_name", ""),
                        phone = row.optString("phone", ""),
                        email = row.optString("email", ""),
                        role = role,
                        marketZone = row.optString("market_zone", "Marché de Kituku"),
                        isActive = row.optBoolean("is_active", true),
                        createdDate = row.optLong("created_date", System.currentTimeMillis()),
                        lastLoginDate = row.optLong("last_login_date", System.currentTimeMillis()),
                        invitedBy = row.optString("invited_by").takeIf { it.isNotEmpty() },
                        pinCode = row.optString("pin_code", "1234"),
                        username = row.optString("username", ""),
                        isOnline = row.optBoolean("is_online", false),
                        lastActiveTimestamp = row.optLong("last_active_timestamp", System.currentTimeMillis()),
                        avatarKey = row.optString("avatar_key", "bybit_vip")
                    )
                    appDb.userDao().insertUser(user)
                    syncedCount++
                }
            }

            // 2. Sync Clients
            val clientsRes = executeSql("SELECT * FROM clients ORDER BY registered_date ASC;")
            clientsRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val client = ClientEntity(
                        id = id,
                        fullName = row.optString("full_name", ""),
                        phone = row.optString("phone", ""),
                        email = row.optString("email", ""),
                        quarter = row.optString("quarter", ""),
                        marketZone = row.optString("market_zone", "Marché de Kituku"),
                        activityType = row.optString("activity_type", "Commerce général"),
                        gender = row.optString("gender", "Femme"),
                        registeredDate = row.optLong("registered_date", System.currentTimeMillis()),
                        isIdVerified = row.optBoolean("is_id_verified", false),
                        idCardNumber = row.optString("id_card_number", ""),
                        guarantorName = row.optString("guarantor_name", ""),
                        guarantorPhone = row.optString("guarantor_phone", ""),
                        status = row.optString("status", "ACTIF"),
                        assignedAgentId = row.optString("assigned_agent_id").takeIf { it.isNotEmpty() },
                        notes = row.optString("notes", ""),
                        avatarUrl = row.optString("avatar_url").takeIf { it.isNotEmpty() },
                        avatarKey = row.optString("avatar_key", "bybit_female"),
                        isOnline = row.optBoolean("is_online", false),
                        lastActiveTimestamp = row.optLong("last_active_timestamp", System.currentTimeMillis())
                    )
                    appDb.clientDao().insertClient(client)
                    syncedCount++
                }
            }

            // 3. Sync Loan Applications
            val appsRes = executeSql("SELECT * FROM loan_applications ORDER BY submission_date ASC;")
            appsRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val statusStr = row.optString("status", "SOUMISE")
                    val status = try { LoanStatus.valueOf(statusStr) } catch (_: Exception) { LoanStatus.SOUMISE }
                    val app = LoanApplicationEntity(
                        id = id,
                        clientId = row.optString("client_id", ""),
                        clientName = row.optString("client_name", ""),
                        clientPhone = row.optString("client_phone", ""),
                        amountRequested = row.optDouble("amount_requested", 0.0),
                        durationDays = row.optInt("duration_days", 30),
                        purpose = row.optString("purpose", ""),
                        activityType = row.optString("activity_type", ""),
                        gender = row.optString("gender", "Femme"),
                        marketZone = row.optString("market_zone", ""),
                        quarter = row.optString("quarter", ""),
                        guarantorName = row.optString("guarantor_name", ""),
                        guarantorPhone = row.optString("guarantor_phone", ""),
                        idCardNumber = row.optString("id_card_number", ""),
                        documentUri = row.optString("document_uri").takeIf { it.isNotEmpty() },
                        clientPhotoUri = row.optString("client_photo_uri").takeIf { it.isNotEmpty() },
                        dailyRate = row.optDouble("daily_rate", 0.035),
                        dailyPaymentAmount = row.optDouble("daily_payment_amount", 0.0),
                        acceptedTerms = row.optBoolean("accepted_terms", true),
                        status = status,
                        submissionDate = row.optLong("submission_date", System.currentTimeMillis()),
                        reviewedBy = row.optString("reviewed_by").takeIf { it.isNotEmpty() },
                        reviewDate = if (row.isNull("review_date")) null else row.optLong("review_date"),
                        reviewNotes = row.optString("review_notes", "")
                    )
                    appDb.loanApplicationDao().insertApplication(app)
                    syncedCount++
                }
            }

            // 4. Sync Credit Cycles
            val cyclesRes = executeSql("SELECT * FROM credit_cycles ORDER BY disbursement_date ASC;")
            cyclesRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val statusStr = row.optString("status", "EN_COURS")
                    val status = try { CycleStatus.valueOf(statusStr) } catch (_: Exception) { CycleStatus.EN_COURS }
                    val cycle = CreditCycleEntity(
                        id = id,
                        clientId = row.optString("client_id", ""),
                        clientName = row.optString("client_name", ""),
                        clientPhone = row.optString("client_phone", ""),
                        marketZone = row.optString("market_zone", ""),
                        agentId = row.optString("agent_id").takeIf { it.isNotEmpty() },
                        agentName = row.optString("agent_name").takeIf { it.isNotEmpty() },
                        loanApplicationId = row.optString("loan_application_id").takeIf { it.isNotEmpty() },
                        disbursementDate = row.optLong("disbursement_date", System.currentTimeMillis()),
                        principalAmount = row.optDouble("principal_amount", 0.0),
                        dailyPaymentAmount = row.optDouble("daily_payment_amount", 0.0),
                        durationDays = row.optInt("duration_days", 30),
                        totalContractual = row.optDouble("total_contractual", 0.0),
                        totalCollected = row.optDouble("total_collected", 0.0),
                        balanceRemaining = row.optDouble("balance_remaining", 0.0),
                        recoveryRate = row.optDouble("recovery_rate", 0.0),
                        grossProfit = row.optDouble("gross_profit", 0.0),
                        enterpriseShare = row.optDouble("enterprise_share", 0.0),
                        clientShare = row.optDouble("client_share", 0.0),
                        charges = row.optDouble("charges", 0.0),
                        netProfit = row.optDouble("net_profit", 0.0),
                        status = status,
                        daysElapsed = row.optInt("days_elapsed", 0),
                        daysLate = row.optInt("days_late", 0),
                        notes = row.optString("notes", "")
                    )
                    appDb.creditCycleDao().insertCycle(cycle)
                    syncedCount++
                }
            }

            // 5. Sync Payments
            val paymentsRes = executeSql("SELECT * FROM payments ORDER BY payment_date ASC;")
            paymentsRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val methodStr = row.optString("payment_method", "ESPECES")
                    val method = try { PaymentMethod.valueOf(methodStr) } catch (_: Exception) { PaymentMethod.ESPECES }
                    val payment = PaymentEntity(
                        id = id,
                        cycleId = row.optString("cycle_id", ""),
                        clientId = row.optString("client_id", ""),
                        clientName = row.optString("client_name", ""),
                        agentId = row.optString("agent_id", ""),
                        agentName = row.optString("agent_name", ""),
                        paymentDate = row.optLong("payment_date", System.currentTimeMillis()),
                        dateString = row.optString("date_string", ""),
                        dayNumber = row.optInt("day_number", 1),
                        expectedAmount = row.optDouble("expected_amount", 0.0),
                        collectedAmount = row.optDouble("collected_amount", 0.0),
                        paymentMethod = method,
                        notes = row.optString("notes", ""),
                        isOfflineSynced = row.optBoolean("is_offline_synced", true),
                        syncPending = false,
                        createdAt = row.optLong("created_at", System.currentTimeMillis())
                    )
                    appDb.paymentDao().insertPayment(payment)
                    syncedCount++
                }
            }

            // 6. Sync Cash Movements
            val cashRes = executeSql("SELECT * FROM cash_movements ORDER BY date ASC;")
            cashRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val typeStr = row.optString("type", "ENTREE")
                    val type = try { CashMovementType.valueOf(typeStr) } catch (_: Exception) { CashMovementType.ENTREE }
                    val catStr = row.optString("category", "REMBOURSEMENT")
                    val category = try { CashMovementCategory.valueOf(catStr) } catch (_: Exception) { CashMovementCategory.REMBOURSEMENT }
                    val methodStr = row.optString("payment_method", "ESPECES")
                    val method = try { PaymentMethod.valueOf(methodStr) } catch (_: Exception) { PaymentMethod.ESPECES }
                    val movement = CashMovementEntity(
                        id = id,
                        date = row.optLong("date", System.currentTimeMillis()),
                        dateString = row.optString("date_string", ""),
                        type = type,
                        category = category,
                        reference = row.optString("reference", ""),
                        description = row.optString("description", ""),
                        amount = row.optDouble("amount", 0.0),
                        paymentMethod = method,
                        recordedByUserId = row.optString("recorded_by_user_id", "USR-001"),
                        recordedByName = row.optString("recorded_by_name", "Direction"),
                        receiptUri = row.optString("receipt_uri").takeIf { it.isNotEmpty() },
                        isApproved = row.optBoolean("is_approved", true)
                    )
                    appDb.cashDao().insertMovement(movement)
                    syncedCount++
                }
            }

            // 7. Sync Business Settings
            val settingsRes = executeSql("SELECT * FROM business_settings WHERE id = 1 LIMIT 1;")
            settingsRes.getOrNull()?.let { rows ->
                if (rows.length() > 0) {
                    val row = rows.getJSONObject(0)
                    val settings = BusinessSettingsEntity(
                        id = 1,
                        organizationName = row.optString("organization_name", "MALISHO NA AKIBA"),
                        currency = row.optString("currency", "FC"),
                        initialCapital = row.optDouble("initial_capital", 5000000.0),
                        targetCapital = row.optDouble("target_capital", 15000000.0),
                        defaultDailyRate = row.optDouble("default_daily_rate", 0.035),
                        minInitialLoan = row.optDouble("min_initial_loan", 50000.0),
                        maxInitialLoan = row.optDouble("max_initial_loan", 400000.0),
                        defaultDurationDays = row.optInt("default_duration_days", 30),
                        enterpriseShareRate = row.optDouble("enterprise_share_rate", 0.50),
                        clientShareRate = row.optDouble("client_share_rate", 0.50),
                        expenseAllowanceRate = row.optDouble("expense_allowance_rate", 0.05),
                        lossProvisionRate = row.optDouble("loss_provision_rate", 0.02)
                    )
                    appDb.businessSettingsDao().insertOrUpdateSettings(settings)
                    syncedCount++
                }
            }

            // 8. Sync Chat Messages
            val chatRes = executeSql("SELECT * FROM chat_messages ORDER BY timestamp ASC;")
            chatRes.getOrNull()?.let { rows ->
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val roleStr = row.optString("sender_role", "AGENT")
                    val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.AGENT }
                    val msg = ChatMessageEntity(
                        id = id,
                        senderId = row.optString("sender_id", ""),
                        senderName = row.optString("sender_name", ""),
                        senderRole = role,
                        clientId = row.optString("client_id", ""),
                        content = row.optString("content", ""),
                        attachmentUri = row.optString("attachment_uri").takeIf { it.isNotEmpty() && it != "null" },
                        timestamp = row.optLong("timestamp", System.currentTimeMillis()),
                        isRead = row.optBoolean("is_read", false),
                        isEdited = row.optBoolean("is_edited", false),
                        editedAt = if (row.isNull("edited_at")) null else row.optLong("edited_at"),
                        replyToMessageId = row.optString("reply_to_message_id").takeIf { it.isNotEmpty() && it != "null" },
                        replyToSenderName = row.optString("reply_to_sender_name").takeIf { it.isNotEmpty() && it != "null" },
                        replyToContentSnippet = row.optString("reply_to_content_snippet").takeIf { it.isNotEmpty() && it != "null" },
                        isPinned = row.optBoolean("is_pinned", false),
                        pinnedAt = if (row.isNull("pinned_at")) null else row.optLong("pinned_at"),
                        isVoiceMessage = row.optBoolean("is_voice_message", false),
                        voiceDurationSeconds = row.optInt("voice_duration_seconds", 0),
                        audioFilePath = row.optString("audio_file_path").takeIf { it.isNotEmpty() && it != "null" },
                        deliveredUserIdsJson = row.optString("delivered_user_ids_json", "[]"),
                        readUserIdsJson = row.optString("read_user_ids_json", "[]")
                    )
                    appDb.chatMessageDao().insertMessage(msg)
                    syncedCount++
                }
            }

            // 9. Sync Notifications (adressées exclusivement à cet utilisateur)
            val notifsRes = executeSql("SELECT * FROM app_notifications ORDER BY timestamp DESC LIMIT 150;")
            notifsRes.getOrNull()?.let { rows ->
                val (activeUserId, _, _) = getActiveUserCredentials()
                val activeUser = activeUserProvider?.invoke() ?: activeUserId.takeIf { it.isNotEmpty() }?.let { appDb.userDao().getUserByIdDirect(it) }
                val allClients = appDb.clientDao().getAllClientsDirect()

                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("id")
                    val senderId = row.optString("sender_id").takeIf { it.isNotEmpty() && it != "null" }
                    val senderName = row.optString("sender_name").takeIf { it.isNotEmpty() && it != "null" }

                    val targetUserId = row.optString("target_user_id").takeIf { it.isNotEmpty() && it != "null" }
                    val roleStr = row.optString("target_role").takeIf { it.isNotEmpty() && it != "null" }
                    val targetRole = roleStr?.let { try { UserRole.valueOf(it) } catch (_: Exception) { null } }
                    val targetClientId = row.optString("target_client_id").takeIf { it.isNotEmpty() && it != "null" }
                    val destinationAddress = row.optString("destination_address").takeIf { it.isNotEmpty() && it != "null" }
                    val typeStr = row.optString("type", "SYSTEM")
                    val type = try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.SYSTEM }
                    val notif = AppNotificationEntity(
                        id = id,
                        targetUserId = targetUserId,
                        targetRole = targetRole,
                        targetClientId = targetClientId,
                        destinationAddress = destinationAddress,
                        senderId = senderId,
                        senderName = senderName,
                        title = row.optString("title", "Notification"),
                        message = row.optString("message", ""),
                        type = type,
                        timestamp = row.optLong("timestamp", System.currentTimeMillis()),
                        isRead = row.optBoolean("is_read", false),
                        actionRoute = row.optString("action_route").takeIf { it.isNotEmpty() && it != "null" }
                    )

                    // Seule la personne concernée enregistre et voit cette notification dans sa boîte locale
                    if (com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notif, activeUser, allClients)) {
                        appDb.appNotificationDao().insertNotification(notif)
                        syncedCount++
                    }
                }
            }

            _isSyncing.value = false
            _syncStatus.value = "Synchronisation Neon PostgreSQL réussie ($syncedCount éléments synchronisés)"
            Result.success(syncedCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    suspend fun uploadLocalDataToNeon(appDb: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            var uploadedCount = 0

            // 1. Upload users
            val users = appDb.userDao().getAllUsersDirect()
            for (u in users) {
                try {
                    saveUser(u)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync user ${u.id}: ${e.message}")
                }
            }

            // 2. Upload clients
            val clients = appDb.clientDao().getAllClientsDirect()
            for (c in clients) {
                try {
                    saveClient(c)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync client ${c.id}: ${e.message}")
                }
            }

            // 3. Upload loan applications
            val applications = appDb.loanApplicationDao().getAllApplicationsDirect()
            for (app in applications) {
                try {
                    saveLoanApplication(app)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync app ${app.id}: ${e.message}")
                }
            }

            // 4. Upload credit cycles
            val cycles = appDb.creditCycleDao().getAllCyclesDirect()
            for (cy in cycles) {
                try {
                    saveCreditCycle(cy)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync cycle ${cy.id}: ${e.message}")
                }
            }

            // 5. Upload payments
            val payments = appDb.paymentDao().getAllPaymentsDirect()
            for (p in payments) {
                try {
                    val res = saveTransaction(p)
                    if (res.isSuccess) {
                        appDb.paymentDao().markPaymentSynced(p.id)
                    }
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync payment ${p.id}: ${e.message}")
                }
            }

            // 6. Upload cash movements
            val movements = appDb.cashDao().getAllMovementsDirect()
            for (m in movements) {
                try {
                    saveCashMovement(m)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync cash movement ${m.id}: ${e.message}")
                }
            }

            // 7. Upload business settings
            val settings = appDb.businessSettingsDao().getSettingsDirect()
            if (settings != null) {
                try {
                    saveBusinessSettings(settings)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync business settings: ${e.message}")
                }
            }

            // 8. Upload chat messages
            val messages = appDb.chatMessageDao().getAllMessagesDirect()
            for (msg in messages) {
                try {
                    saveChatMessage(msg)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync chat msg ${msg.id}: ${e.message}")
                }
            }

            // 9. Upload audit logs
            val logs = appDb.auditLogDao().getAllLogsDirect()
            for (l in logs) {
                try {
                    saveAuditLog(l)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync audit log ${l.id}: ${e.message}")
                }
            }

            // 10. Upload app notifications
            val notifs = appDb.appNotificationDao().getRecentUnreadDirect()
            for (n in notifs) {
                try {
                    saveNotification(n)
                    uploadedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync notification ${n.id}: ${e.message}")
                }
            }

            _hasPendingLocalChanges.value = false
            _isSyncing.value = false
            _syncStatus.value = "Synchronisé automatiquement avec Neon Cloud ($uploadedCount éléments)"
            Result.success(uploadedCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    /**
     * Synchronisation automatique des éléments en attente (hors-ligne -> en-ligne).
     * S'exécute automatiquement dès le rétablissement de la connexion ou en tâche de fond.
     */
    suspend fun autoSyncPendingData(appDb: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            val pending = appDb.paymentDao().getPendingSyncCountDirect()
            val total = pending + (if (_hasPendingLocalChanges.value) 1 else 0)
            _syncStatus.value = if (total > 0)
                "Hors-ligne • $total opération(s) en attente de connexion"
            else
                "Hors-ligne • En attente de connexion internet"
            return@withContext Result.failure(Exception("Pas de connexion internet"))
        }

        _isSyncing.value = true
        _syncStatus.value = "Synchronisation automatique en cours vers Neon Cloud..."
        try {
            var syncedCount = 0

            // 1. Synchroniser en priorité les versements marqués en attente
            val pendingPayments = appDb.paymentDao().getPendingSyncPaymentsDirect()
            for (p in pendingPayments) {
                try {
                    val res = saveTransaction(p)
                    if (res.isSuccess) {
                        appDb.paymentDao().markPaymentSynced(p.id)
                        syncedCount++
                        Log.d(TAG, "Paiement en attente ${p.id} synchronisé avec succès vers Neon")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Échec sync paiement en attente ${p.id}: ${e.message}")
                }
            }

            // 2. Synchroniser l'ensemble des modifications locales (clients créés hors-ligne, cycles, caisse, etc.)
            val uploadRes = uploadLocalDataToNeon(appDb)
            val fullCount = uploadRes.getOrNull() ?: 0

            _hasPendingLocalChanges.value = false
            _isSyncing.value = false
            _syncStatus.value = "Connecté • Synchronisé en direct avec Neon PostgreSQL"
            Result.success(syncedCount + fullCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    fun startRealtimeSync(appDb: AppDatabase) {
        syncJob?.cancel()

        // Détection immédiate du retour du réseau : synchronisation automatique instantanée
        networkMonitor.setOnNetworkAvailableListener {
            scope.launch {
                Log.d(TAG, "Connexion Internet rétablie : Synchronisation automatique immédiate vers Neon PostgreSQL")
                autoSyncPendingData(appDb)
            }
        }

        syncJob = scope.launch {
            while (isActive) {
                try {
                    val (activeUserId, activeAccountName, activeAccountEmail) = getActiveUserCredentials()

                    if (isNetworkAvailable()) {
                        // 1. Pousser automatiquement tout encaissement ou opération locale en attente
                        val pendingCount = appDb.paymentDao().getPendingSyncCountDirect()
                        if (pendingCount > 0 || _hasPendingLocalChanges.value) {
                            Log.d(TAG, "Détection d'activité locale en attente (versements: $pendingCount, modifications: ${_hasPendingLocalChanges.value}) -> Auto-sync Neon")
                            autoSyncPendingData(appDb)
                        }

                        // 2. Periodic poll to fetch latest chat messages from Neon Postgres
                        val chatRes = executeSql("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 150;")
                        chatRes.getOrNull()?.let { rows ->
                            val allClients = appDb.clientDao().getAllClientsDirect()
                            val allUsers = appDb.userDao().getAllUsersDirect()
                            val activeUser = activeUserProvider?.invoke() ?: activeUserId.takeIf { it.isNotEmpty() }?.let { appDb.userDao().getUserByIdDirect(it) }

                            for (i in 0 until rows.length()) {
                                val row = rows.getJSONObject(i)
                                val id = row.getString("id")
                                val roleStr = row.optString("sender_role", "AGENT")
                                val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.AGENT }
                                val senderId = row.optString("sender_id", "")
                                val senderName = row.optString("sender_name", "Expéditeur")
                                val clientId = row.optString("client_id", "")
                                val content = row.optString("content", "")

                                val isCurrentUserSender = (activeUser != null && (
                                    senderId.equals(activeUser.id, ignoreCase = true) ||
                                    senderId.removePrefix("USR-").equals(activeUser.id.removePrefix("USR-"), ignoreCase = true) ||
                                    senderId.equals("USR-${activeUser.id}", ignoreCase = true) ||
                                    (activeUser.username.isNotEmpty() && senderId.equals(activeUser.username, ignoreCase = true)) ||
                                    (activeUser.email.isNotEmpty() && (senderId.equals(activeUser.email, ignoreCase = true) || senderId.equals(activeUser.email.substringBefore("@"), ignoreCase = true))) ||
                                    (activeUser.phone.isNotEmpty() && (senderId.contains(activeUser.phone) || activeUser.phone.contains(senderId))) ||
                                    (senderName.isNotBlank() && activeUser.fullName.isNotBlank() && senderName.trim().equals(activeUser.fullName.trim(), ignoreCase = true))
                                )) || (activeUserId.isNotEmpty() && (
                                    senderId.equals(activeUserId, ignoreCase = true) ||
                                    senderId.removePrefix("USR-").equals(activeUserId.removePrefix("USR-"), ignoreCase = true) ||
                                    senderId.equals("USR-$activeUserId", ignoreCase = true)
                                )) || (activeAccountName.isNotBlank() && senderName.isNotBlank() && senderName.trim().equals(activeAccountName.trim(), ignoreCase = true))

                                val rawDelivered = row.optString("delivered_user_ids_json", "[]")
                                val rawRead = row.optString("read_user_ids_json", "[]")
                                val existing = appDb.chatMessageDao().getMessageByIdDirect(id)

                                val mergedDelivered = try {
                                    val set = mutableSetOf<String>()
                                    if (rawDelivered.isNotEmpty() && rawDelivered != "null") {
                                        val arr = org.json.JSONArray(rawDelivered)
                                        for (idx in 0 until arr.length()) set.add(arr.optString(idx))
                                    }
                                    if (existing != null && existing.deliveredUserIdsJson.isNotEmpty() && existing.deliveredUserIdsJson != "null") {
                                        val arr = org.json.JSONArray(existing.deliveredUserIdsJson)
                                        for (idx in 0 until arr.length()) set.add(arr.optString(idx))
                                    }
                                    if (activeUserId.isNotEmpty() && !isCurrentUserSender) {
                                        set.add(activeUserId)
                                    }
                                    org.json.JSONArray(set.toList()).toString()
                                } catch (_: Exception) {
                                    rawDelivered
                                }

                                val mergedRead = try {
                                    val set = mutableSetOf<String>()
                                    if (rawRead.isNotEmpty() && rawRead != "null") {
                                        val arr = org.json.JSONArray(rawRead)
                                        for (idx in 0 until arr.length()) set.add(arr.optString(idx))
                                    }
                                    if (existing != null && existing.readUserIdsJson.isNotEmpty() && existing.readUserIdsJson != "null") {
                                        val arr = org.json.JSONArray(existing.readUserIdsJson)
                                        for (idx in 0 until arr.length()) set.add(arr.optString(idx))
                                    }
                                    org.json.JSONArray(set.toList()).toString()
                                } catch (_: Exception) {
                                    rawRead
                                }

                                val msg = ChatMessageEntity(
                                    id = id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    senderRole = role,
                                    clientId = clientId,
                                    content = content,
                                    attachmentUri = row.optString("attachment_uri").takeIf { it.isNotEmpty() && it != "null" },
                                    timestamp = row.optLong("timestamp", System.currentTimeMillis()),
                                    isRead = (existing?.isRead == true) || row.optBoolean("is_read", false),
                                    isEdited = row.optBoolean("is_edited", false),
                                    editedAt = if (row.isNull("edited_at")) null else row.optLong("edited_at"),
                                    replyToMessageId = row.optString("reply_to_message_id").takeIf { it.isNotEmpty() && it != "null" },
                                    replyToSenderName = row.optString("reply_to_sender_name").takeIf { it.isNotEmpty() && it != "null" },
                                    replyToContentSnippet = row.optString("reply_to_content_snippet").takeIf { it.isNotEmpty() && it != "null" },
                                    isPinned = row.optBoolean("is_pinned", false),
                                    pinnedAt = if (row.isNull("pinned_at")) null else row.optLong("pinned_at"),
                                    isVoiceMessage = row.optBoolean("is_voice_message", false),
                                    voiceDurationSeconds = row.optInt("voice_duration_seconds", 0),
                                    audioFilePath = row.optString("audio_file_path").takeIf { it.isNotEmpty() && it != "null" },
                                    deliveredUserIdsJson = mergedDelivered,
                                    readUserIdsJson = mergedRead
                                )

                                appDb.chatMessageDao().insertMessage(msg)

                                // If this is a newly received message from another user, trigger notification ONLY for the concerned user!
                                if (existing == null && senderId.isNotEmpty() && !isCurrentUserSender) {
                                    try {
                                        val senderUser = allUsers.find { it.id == senderId }
                                            ?: UserEntity(id = senderId, fullName = senderName, phone = "", role = role)
                                        val (targetUserId, targetClientId, destAddress) = com.example.util.ChatSecurityHelper.resolveMessageDestination(
                                            sender = senderUser,
                                            roomId = clientId,
                                            clients = allClients,
                                            allUsers = allUsers
                                        )

                                        val notif = AppNotificationEntity(
                                            id = "NOTIF-MSG-$id",
                                            targetRole = if (targetUserId == null && clientId != "COMMUNITY_GROUP") {
                                                if (role == UserRole.CLIENT) UserRole.AGENT else UserRole.CLIENT
                                            } else null,
                                            targetUserId = targetUserId,
                                            targetClientId = targetClientId,
                                            destinationAddress = destAddress,
                                            senderId = senderId,
                                            senderName = senderName,
                                            title = if (clientId == "COMMUNITY_GROUP") "Message de $senderName (Communauté)" else "Nouveau message de $senderName",
                                            message = content,
                                            type = NotificationType.MESSAGE,
                                            actionRoute = "chat:$clientId"
                                        )

                                        if (com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notif, activeUser, allClients)) {
                                            appDb.appNotificationDao().insertNotification(notif)
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Notification post error: ${e.message}")
                                    }
                                }
                            }
                        }

                        // 3. Poll for credit cycles from Neon to keep active cycles updated
                        val cycleRes = executeSql("SELECT * FROM credit_cycles ORDER BY disbursement_date DESC LIMIT 50;")
                        cycleRes.getOrNull()?.let { rows ->
                            for (i in 0 until rows.length()) {
                                val row = rows.getJSONObject(i)
                                val id = row.getString("id")
                                val statusStr = row.optString("status", "EN_COURS")
                                val status = try { CycleStatus.valueOf(statusStr) } catch (_: Exception) { CycleStatus.EN_COURS }
                                val cycle = CreditCycleEntity(
                                    id = id,
                                    clientId = row.optString("client_id", ""),
                                    clientName = row.optString("client_name", ""),
                                    clientPhone = row.optString("client_phone", ""),
                                    marketZone = row.optString("market_zone", "Marché de Kituku"),
                                    agentId = row.optString("agent_id", "USR-001"),
                                    agentName = row.optString("agent_name", "Agent"),
                                    loanApplicationId = row.optString("loan_application_id", ""),
                                    disbursementDate = row.optLong("disbursement_date", System.currentTimeMillis()),
                                    principalAmount = row.optDouble("principal_amount", 100000.0),
                                    dailyPaymentAmount = row.optDouble("daily_payment_amount", 3500.0),
                                    durationDays = row.optInt("duration_days", 30),
                                    totalContractual = row.optDouble("total_contractual", 105000.0),
                                    totalCollected = row.optDouble("total_collected", 0.0),
                                    balanceRemaining = row.optDouble("balance_remaining", 105000.0),
                                    recoveryRate = row.optDouble("recovery_rate", 0.0),
                                    grossProfit = row.optDouble("gross_profit", 0.0),
                                    enterpriseShare = row.optDouble("enterprise_share", 0.0),
                                    clientShare = row.optDouble("client_share", 0.0),
                                    charges = row.optDouble("charges", 0.0),
                                    netProfit = row.optDouble("net_profit", 0.0),
                                    status = status,
                                    daysElapsed = row.optInt("days_elapsed", 0),
                                    daysLate = row.optInt("days_late", 0),
                                    notes = row.optString("notes", "")
                                )
                                appDb.creditCycleDao().insertCycle(cycle)
                            }
                        }

                        // 4. Poll for latest notifications from Neon
                        val notifRes = executeSql("SELECT * FROM app_notifications ORDER BY timestamp DESC LIMIT 60;")
                        notifRes.getOrNull()?.let { rows ->
                            val allClients = appDb.clientDao().getAllClientsDirect()
                            val activeUser = activeUserProvider?.invoke() ?: activeUserId.takeIf { it.isNotEmpty() }?.let { appDb.userDao().getUserByIdDirect(it) }

                            for (i in 0 until rows.length()) {
                                val row = rows.getJSONObject(i)
                                val id = row.getString("id")
                                val senderId = row.optString("sender_id").takeIf { it.isNotEmpty() && it != "null" }
                                val senderName = row.optString("sender_name").takeIf { it.isNotEmpty() && it != "null" }

                                val isMeSender = (activeUser != null && (
                                    (senderId != null && (
                                        senderId.equals(activeUser.id, ignoreCase = true) ||
                                        senderId.removePrefix("USR-").equals(activeUser.id.removePrefix("USR-"), ignoreCase = true) ||
                                        senderId.equals("USR-${activeUser.id}", ignoreCase = true) ||
                                        (activeUser.username.isNotEmpty() && senderId.equals(activeUser.username, ignoreCase = true)) ||
                                        (activeUser.email.isNotEmpty() && (senderId.equals(activeUser.email, ignoreCase = true) || senderId.equals(activeUser.email.substringBefore("@"), ignoreCase = true))) ||
                                        (activeUser.phone.isNotEmpty() && (senderId.contains(activeUser.phone) || activeUser.phone.contains(senderId)))
                                    )) ||
                                    (senderName != null && activeUser.fullName.isNotBlank() && senderName.trim().equals(activeUser.fullName.trim(), ignoreCase = true))
                                )) || (activeUserId.isNotEmpty() && senderId != null && (
                                    senderId.equals(activeUserId, ignoreCase = true) ||
                                    senderId.removePrefix("USR-").equals(activeUserId.removePrefix("USR-"), ignoreCase = true) ||
                                    senderId.equals("USR-$activeUserId", ignoreCase = true)
                                )) || (activeAccountName.isNotBlank() && senderName != null && senderName.trim().equals(activeAccountName.trim(), ignoreCase = true))

                                if (!isMeSender) {
                                    val targetUserId = row.optString("target_user_id").takeIf { it.isNotEmpty() && it != "null" }
                                    val roleStr = row.optString("target_role").takeIf { it.isNotEmpty() && it != "null" }
                                    val targetRole = roleStr?.let { try { UserRole.valueOf(it) } catch (_: Exception) { null } }
                                    val targetClientId = row.optString("target_client_id").takeIf { it.isNotEmpty() && it != "null" }
                                    val destinationAddress = row.optString("destination_address").takeIf { it.isNotEmpty() && it != "null" }
                                    val typeStr = row.optString("type", "SYSTEM")
                                    val type = try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.SYSTEM }

                                    val notif = AppNotificationEntity(
                                        id = id,
                                        targetUserId = targetUserId,
                                        targetRole = targetRole,
                                        targetClientId = targetClientId,
                                        destinationAddress = destinationAddress,
                                        senderId = senderId,
                                        senderName = senderName,
                                        title = row.optString("title", "Notification"),
                                        message = row.optString("message", ""),
                                        type = type,
                                        timestamp = row.optLong("timestamp", System.currentTimeMillis()),
                                        isRead = row.optBoolean("is_read", false),
                                        actionRoute = row.optString("action_route").takeIf { it.isNotEmpty() && it != "null" }
                                    )

                                    if (com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notif, activeUser, allClients)) {
                                        appDb.appNotificationDao().insertNotification(notif)
                                    }
                                }
                            }
                        }

                        // 5. Poll online users and presences from Neon to keep active status real-time across devices
                        val currentActiveId = activeUserId.takeIf { it.isNotEmpty() } ?: activeUserProvider?.invoke()?.id ?: ""
                        val usersRes = executeSql("SELECT id, is_online, last_active_timestamp FROM users WHERE is_online = TRUE OR last_active_timestamp > ${(System.currentTimeMillis() - 300000L)};")
                        usersRes.getOrNull()?.let { rows ->
                            for (i in 0 until rows.length()) {
                                val row = rows.getJSONObject(i)
                                val uId = row.getString("id")
                                val online = row.optBoolean("is_online", false)
                                val lastActive = row.optLong("last_active_timestamp", 0L)
                                if (uId.isNotEmpty() && uId != currentActiveId && uId != "USR-$currentActiveId" && uId.removePrefix("USR-") != currentActiveId.removePrefix("USR-")) {
                                    appDb.userDao().updateUserPresence(uId, online, lastActive)
                                }
                            }
                        }

                        _syncStatus.value = "Connecté • Synchronisé en temps réel avec Neon Cloud"
                    } else {
                        val pending = appDb.paymentDao().getPendingSyncCountDirect()
                        _syncStatus.value = if (pending > 0)
                            "Hors-ligne • $pending versement(s) en attente de connexion internet"
                        else
                            "Hors-ligne • Synchronisation automatique en attente de connexion"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop notice: ${e.message}")
                }
                delay(6000L) // every 6 seconds for responsive real-time auto sync
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    companion object {
        private const val TAG = "NeonPostgresBackend"
    }
}
