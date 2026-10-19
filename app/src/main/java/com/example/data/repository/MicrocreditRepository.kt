package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.util.CryptoHelper

class MicrocreditRepository(
    private val db: AppDatabase,
    var firebaseService: com.example.data.firebase.FirebaseBackendService? = null,
    var neonService: com.example.data.neon.NeonPostgresBackendService? = null,
    var cloudinaryService: com.example.data.cloudinary.CloudinaryStorageService? = null,
    val context: android.content.Context? = null
) {
    var activeUserProvider: (() -> UserEntity?)? = null

    // DAOs
    val userDao = db.userDao()
    val clientDao = db.clientDao()
    val loanApplicationDao = db.loanApplicationDao()
    val creditCycleDao = db.creditCycleDao()
    val paymentDao = db.paymentDao()
    val cashDao = db.cashDao()
    val settingsDao = db.businessSettingsDao()
    val chatDao = db.chatMessageDao()
    val notificationDao = db.appNotificationDao()
    val auditDao = db.auditLogDao()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    // Real calculated cash balance Flow = Total Entrées - Total Sorties
    val calculatedCashBalance: Flow<Double> = combine(
        cashDao.getTotalEntrees(),
        cashDao.getTotalSorties()
    ) { entrees, sorties ->
        (entrees ?: 0.0) - (sorties ?: 0.0)
    }

    /**
     * Synchronise immédiatement vers Neon PostgreSQL Cloud si connecté,
     * ou place automatiquement l'opération en attente de connexion internet sans bloquer.
     */
    fun autoSyncOrSchedule(actionName: String, block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (neonService?.isNetworkAvailable() == true) {
                try {
                    block()
                    Log.d("MicrocreditRepo", "Synchronisation automatique immédiate Neon réussie : $actionName")
                    // Si des éléments étaient précédemment en attente hors-ligne, déclencher le flush automatique immédiat
                    if (neonService?.hasPendingLocalChanges?.value == true || paymentDao.getPendingSyncCountDirect() > 0) {
                        neonService?.autoSyncPendingData(db)
                    }
                } catch (e: Exception) {
                    Log.w("MicrocreditRepo", "Échec sync immédiate ($actionName), mise en attente automatique : ${e.message}")
                    neonService?.schedulePendingSync()
                }
            } else {
                Log.d("MicrocreditRepo", "Hors-ligne : $actionName enregistré en local, en attente de connexion internet.")
                neonService?.schedulePendingSync()
            }
        }
    }

    suspend fun pushNotification(notif: AppNotificationEntity) {
        val activeUser = activeUserProvider?.invoke()
        val effectiveContext = context ?: neonService?.context
        val prefs1 = effectiveContext?.getSharedPreferences("microcredit_prefs", android.content.Context.MODE_PRIVATE)
        val prefs2 = effectiveContext?.getSharedPreferences("remix_microfinance_prefs", android.content.Context.MODE_PRIVATE)

        val activeUserId = activeUser?.id
            ?: prefs1?.getString("active_user_id", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2?.getString("active_user_id", "") ?: ""

        val activeName = activeUser?.fullName
            ?: prefs1?.getString("active_account_name", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2?.getString("active_account_name", "") ?: ""

        val activeEmail = activeUser?.email
            ?: prefs1?.getString("active_account_email", "")?.takeIf { it.isNotEmpty() }
            ?: prefs2?.getString("active_account_email", "") ?: ""

        val activePhone = activeUser?.phone ?: ""

        // Si la notification a été générée localement sans senderId explicite, lui assigner l'utilisateur actif
        val effectiveNotif = if (notif.senderId.isNullOrBlank() && activeUserId.isNotEmpty()) {
            notif.copy(senderId = activeUserId, senderName = activeName)
        } else {
            notif
        }

        // Sauvegarde distante vers Neon et Cloud Firestore pour distribution aux destinataires
        autoSyncOrSchedule("Notification ${effectiveNotif.id}") {
            neonService?.saveNotification(effectiveNotif)?.getOrThrow()
            try {
                firebaseService?.saveNotificationToFirestore(effectiveNotif)
            } catch (_: Exception) {}
        }

        // L'émetteur ne reçoit JAMAIS sa propre notification dans sa boîte de réception locale
        val isMe = (activeUserId.isNotEmpty() && (
            effectiveNotif.senderId?.equals(activeUserId, ignoreCase = true) == true ||
            effectiveNotif.senderId?.removePrefix("USR-")?.equals(activeUserId.removePrefix("USR-"), ignoreCase = true) == true ||
            effectiveNotif.senderId?.equals("USR-$activeUserId", ignoreCase = true) == true
        )) || (!effectiveNotif.senderName.isNullOrBlank() && activeName.isNotBlank() && effectiveNotif.senderName!!.trim().equals(activeName.trim(), ignoreCase = true)) ||
        (activeEmail.isNotEmpty() && (effectiveNotif.senderId?.equals(activeEmail, ignoreCase = true) == true || effectiveNotif.senderName?.equals(activeEmail, ignoreCase = true) == true)) ||
        (activePhone.isNotEmpty() && (effectiveNotif.senderId?.contains(activePhone) == true || effectiveNotif.senderName?.contains(activePhone) == true)) ||
        (activeName.isNotBlank() && (
            effectiveNotif.title.startsWith("Message de $activeName", ignoreCase = true) ||
            effectiveNotif.title.contains("Message de $activeName", ignoreCase = true) ||
            effectiveNotif.title.contains("par $activeName", ignoreCase = true)
        ))

        val allClients = try { clientDao.getAllClientsDirect() } catch (_: Exception) { emptyList() }
        val isConcerned = !isMe && com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(effectiveNotif, activeUser, allClients)

        if (isConcerned) {
            notificationDao.insertNotification(effectiveNotif)
        } else {
            Log.d("MicrocreditRepo", "Notification non insérée localement (non concerné ou auteur) : ${effectiveNotif.title} (${effectiveNotif.id})")
        }
    }

    // --- User & RBAC Management ---
    suspend fun authenticate(identifier: String, pin: String): Result<UserEntity> {
        val trimmed = identifier.trim()
        val user = userDao.findUserByIdentifier(trimmed)
            ?: userDao.findUserByPhone(trimmed)
            ?: return Result.failure(Exception("Identifiant (téléphone, nom d'utilisateur ou email) introuvable."))
        if (!user.isActive) {
            return Result.failure(Exception("Ce compte a été désactivé par l'administration."))
        }
        if (user.pinCode != pin.trim()) {
            return Result.failure(Exception("Mot de passe ou code d'accès erroné. Veuillez réessayer."))
        }
        // Update last login & presence
        userDao.updateUser(user.copy(lastLoginDate = System.currentTimeMillis(), isOnline = true, lastActiveTimestamp = System.currentTimeMillis()))
        logAudit(
            user = user,
            action = "CONNEXION",
            entityType = "USER",
            entityId = user.id,
            details = "Connexion réussie au système (${user.role.label})."
        )
        return Result.success(user)
    }

    suspend fun updateUserPresence(userId: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis()) {
        userDao.updateUserPresence(userId, isOnline, timestamp)
        try {
            neonService?.updateUserOnlineStatus(userId, isOnline, timestamp)
            firebaseService?.updateUserOnlineStatus(userId, isOnline, timestamp)
        } catch (_: Exception) {}
    }

    suspend fun updateClientPresence(clientId: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis()) {
        clientDao.updateClientPresence(clientId, isOnline, timestamp)
        try {
            neonService?.updateClientOnlineStatus(clientId, isOnline, timestamp)
        } catch (_: Exception) {}
    }

    suspend fun updateUserAvatarKey(userId: String, avatarKey: String) {
        userDao.updateUserAvatarKey(userId, avatarKey)
        try {
            neonService?.updateUserAvatar(userId, avatarKey)
            firebaseService?.updateUserAvatar(userId, avatarKey)
        } catch (_: Exception) {}
    }

    suspend fun updateClientAvatarKey(clientId: String, avatarKey: String) {
        clientDao.updateClientAvatarKey(clientId, avatarKey)
        try {
            neonService?.updateClientAvatar(clientId, avatarKey)
            firebaseService?.updateUserAvatar(clientId, avatarKey)
        } catch (_: Exception) {}
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    /**
     * Génère le matricule auto-incrémenté officiel dans l'ordre strict CLI-001, CLI-002, CLI-003, etc.
     * Permet de savoir exactement et sans ambiguïté le quantième client enregistré dans l'institution.
     */
    suspend fun generateNextClientId(): String {
        val existingClients = clientDao.getAllClientsDirect()
        val existingUsers = userDao.getAllUsersDirect()
        val existingApps = loanApplicationDao.getAllApplicationsDirect()
        val existingCycles = creditCycleDao.getAllCyclesDirect()

        val clientNums = existingClients.mapNotNull { extractClientSequenceNumber(it.id) }
        val userNums = existingUsers.mapNotNull { extractClientSequenceNumber(it.id) }
        val appNums = existingApps.mapNotNull { extractClientSequenceNumber(it.clientId) }
        val cycleNums = existingCycles.mapNotNull { extractClientSequenceNumber(it.clientId) }

        val allNumbers = clientNums + userNums + appNums + cycleNums
        val maxNumber = allNumbers.maxOrNull() ?: 0
        return String.format(java.util.Locale.US, "CLI-%03d", maxNumber + 1)
    }

    suspend fun createUserByManager(
        operator: UserEntity,
        fullName: String,
        phone: String,
        username: String,
        email: String = "",
        role: UserRole,
        marketZone: String,
        pinCode: String = "1234"
    ): Result<UserEntity> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire/propriétaire a le droit d'ajouter des utilisateurs."))
        }
        val cleanPhone = phone.trim()
        val cleanUsername = username.trim().lowercase()

        // Check if phone or username already exists
        if (cleanPhone.isNotEmpty()) {
            val existing = userDao.findUserByPhone(cleanPhone)
            if (existing != null) {
                return Result.failure(Exception("Un utilisateur avec ce numéro de téléphone existe déjà."))
            }
        }
        if (cleanUsername.isNotEmpty()) {
            val existingUser = userDao.findUserByUsername(cleanUsername)
            if (existingUser != null) {
                return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
            }
        }

        val clientId = if (role == UserRole.CLIENT) generateNextClientId() else ""
        val prefix = when (role) {
            UserRole.GESTIONNAIRE -> "USR-MGR"
            UserRole.AGENT -> "USR-AGT"
            UserRole.CAISSIER -> "USR-CSH"
            UserRole.CLIENT -> "USR-$clientId"
        }
        val userId = if (role == UserRole.CLIENT) prefix else "$prefix-${(100..999).random()}"
        val newUser = UserEntity(
            id = userId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            email = email.trim(),
            role = role,
            marketZone = marketZone.trim(),
            isActive = true,
            invitedBy = operator.fullName,
            pinCode = pinCode.trim().ifEmpty { "1234" },
            username = cleanUsername
        )
        userDao.insertUser(newUser)
        autoSyncOrSchedule("Création utilisateur ${newUser.fullName}") {
            neonService?.saveUser(newUser)
        }

        // If client, also insert in clients table if not present
        if (role == UserRole.CLIENT) {
            val client = ClientEntity(
                id = clientId,
                fullName = fullName.trim(),
                phone = cleanPhone,
                email = email.trim(),
                marketZone = marketZone.trim(),
                activityType = "Commerce au marché",
                registeredDate = System.currentTimeMillis(),
                isIdVerified = false,
                idCardNumber = "",
                status = "ACTIF"
            )
            clientDao.insertClient(client)
            autoSyncOrSchedule("Création profil client ${client.fullName}") {
                neonService?.saveClient(client)
            }
        }

        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                title = "Nouvel utilisateur ajouté",
                message = "${newUser.fullName} a été créé avec le rôle ${role.label} ($marketZone) par ${operator.fullName}.",
                type = NotificationType.SYSTEM,
                actionRoute = "users"
            )
        )

        logAudit(
            user = operator,
            action = "CREATION_UTILISATEUR",
            entityType = "USER",
            entityId = userId,
            details = "Utilisateur $fullName créé en tant que ${role.label} ($marketZone)."
        )
        return Result.success(newUser)
    }

    suspend fun registerClientAccount(
        fullName: String,
        phone: String,
        username: String,
        marketZone: String,
        quarter: String,
        pinCode: String,
        activityType: String = "Commerce de proximité",
        gender: String = "Femme",
        photoUri: String? = null
    ): Result<UserEntity> {
        val cleanPhone = phone.trim()
        val cleanUsername = username.trim().lowercase()

        val existingPhone = userDao.findUserByPhone(cleanPhone)
        if (existingPhone != null) {
            return Result.failure(Exception("Ce numéro de téléphone est déjà enregistré."))
        }
        if (cleanUsername.isNotEmpty()) {
            val existingUser = userDao.findUserByUsername(cleanUsername)
            if (existingUser != null) {
                return Result.failure(Exception("Ce nom d'utilisateur est déjà utilisé."))
            }
        }

        val clientId = generateNextClientId()
        val userId = "USR-$clientId"
        val newUser = UserEntity(
            id = userId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            email = "",
            role = UserRole.CLIENT,
            marketZone = marketZone.trim(),
            isActive = true,
            pinCode = pinCode.trim().ifEmpty { "1234" },
            username = cleanUsername
        )
        userDao.insertUser(newUser)

        val client = ClientEntity(
            id = clientId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            email = "",
            quarter = quarter.trim(),
            marketZone = marketZone.trim(),
            activityType = activityType.trim(),
            gender = gender.trim(),
            registeredDate = System.currentTimeMillis(),
            isIdVerified = false,
            idCardNumber = "",
            status = "ACTIF",
            avatarUrl = photoUri
        )
        clientDao.insertClient(client)
        autoSyncOrSchedule("Enregistrement client ${client.fullName}") {
            neonService?.saveUser(newUser)
            neonService?.saveClient(client)
        }

        val clientDest = "${client.fullName} • Marché $marketZone (Q. $quarter)"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                targetClientId = client.id,
                destinationAddress = clientDest,
                senderId = newUser.id,
                senderName = newUser.fullName,
                title = "Nouveau compte client enregistré",
                message = "${newUser.fullName} a créé son compte client ($marketZone, Q. $quarter).",
                type = NotificationType.SYSTEM,
                actionRoute = "clients"
            )
        )

        logAudit(
            user = newUser,
            action = "CREATION_COMPTE_CLIENT",
            entityType = "CLIENT",
            entityId = client.id,
            details = "Enregistrement de compte client par ${newUser.fullName} ($marketZone, Q. $quarter).",
            destination = clientDest,
            targetClientId = client.id,
            targetUserId = newUser.id
        )

        return Result.success(newUser)
    }

    suspend fun inviteUser(
        operator: UserEntity,
        fullName: String,
        phone: String,
        email: String,
        role: UserRole,
        marketZone: String
    ): Result<UserInvitationEntity> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire/propriétaire a le droit d'inviter des utilisateurs."))
        }
        val inviteId = "INV-${System.currentTimeMillis() % 100000}"
        val code = "INV-${(1000..9999).random()}"
        val invitation = UserInvitationEntity(
            id = inviteId,
            fullName = fullName,
            phone = phone,
            email = email,
            role = role,
            marketZone = marketZone,
            invitedBy = operator.fullName,
            inviteCode = code,
            status = "ACCEPTEE"
        )
        db.userDao().insertInvitation(invitation)

        // Automatically create user account with default pin
        val userId = "USR-${System.currentTimeMillis() % 100000}"
        val newUser = UserEntity(
            id = userId,
            fullName = fullName,
            phone = phone,
            email = email,
            role = role,
            marketZone = marketZone,
            isActive = true,
            invitedBy = operator.fullName,
            pinCode = "1234"
        )
        db.userDao().insertUser(newUser)
        try {
            neonService?.saveUser(newUser)
        } catch (_: Exception) {}

        val userDest = "$fullName • ${role.label} ($marketZone)"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = userId,
                destinationAddress = userDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Invitation à rejoindre MALISHO NA AKIBA",
                message = "Vous avez été invité avec le rôle ${role.label} ($marketZone) par ${operator.fullName}.",
                type = NotificationType.SYSTEM,
                actionRoute = "users"
            )
        )

        logAudit(
            user = operator,
            action = "INVITATION_UTILISATEUR",
            entityType = "USER",
            entityId = userId,
            details = "Invitation créée pour $fullName en tant que ${role.label} ($marketZone).",
            destination = userDest,
            targetUserId = userId,
            targetRole = role
        )
        return Result.success(invitation)
    }

    suspend fun updateUserRole(operator: UserEntity, targetUserId: String, targetUserName: String, newRole: UserRole): Result<Unit> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire/propriétaire peut modifier les rôles."))
        }
        if (operator.id == targetUserId) {
            return Result.failure(Exception("Un utilisateur ne peut pas modifier son propre rôle."))
        }
        userDao.updateUserRole(targetUserId, newRole)
        val roleDest = "$targetUserName • Nouveau rôle: ${newRole.label}"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = targetUserId,
                destinationAddress = roleDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Mise à jour de votre profil",
                message = "Votre rôle a été modifié en ${newRole.label} par ${operator.fullName}.",
                type = NotificationType.SYSTEM,
                actionRoute = "users"
            )
        )
        logAudit(
            user = operator,
            action = "MODIFICATION_ROLE",
            entityType = "USER",
            entityId = targetUserId,
            details = "Attribution du rôle ${newRole.label} à $targetUserName.",
            destination = roleDest,
            targetUserId = targetUserId,
            targetRole = newRole
        )
        return Result.success(Unit)
    }

    suspend fun updateUserRoleAndMarkets(operator: UserEntity, targetUserId: String, targetUserName: String, newRole: UserRole, newMarkets: String): Result<Unit> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire/propriétaire peut modifier les affectations."))
        }
        userDao.updateUserRoleAndMarkets(targetUserId, newRole, newMarkets.trim())
        val affectDest = "$targetUserName • ${newRole.label} [$newMarkets]"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = targetUserId,
                destinationAddress = affectDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Nouvelle affectation de marchés",
                message = "Vos marchés d'intervention ont été configurés [$newMarkets] avec le rôle ${newRole.label}.",
                type = NotificationType.SYSTEM,
                actionRoute = "users"
            )
        )
        logAudit(
            user = operator,
            action = "MODIFICATION_AFFECTATION",
            entityType = "USER",
            entityId = targetUserId,
            details = "Attribution du rôle ${newRole.label} et des marchés [$newMarkets] à $targetUserName.",
            destination = affectDest,
            targetUserId = targetUserId,
            targetRole = newRole
        )
        return Result.success(Unit)
    }

    suspend fun clearAllTestDataAndReset(operator: UserEntity? = null, keepAdmin: Boolean = true): Result<Unit> {
        clientDao.deleteAllClients()
        loanApplicationDao.deleteAllApplications()
        creditCycleDao.deleteAllCycles()
        paymentDao.deleteAllPayments()
        cashDao.deleteAllMovements()
        chatDao.deleteAllMessages()
        notificationDao.deleteAllNotifications()
        if (keepAdmin) {
            userDao.deleteNonAdminUsers()
            val admin = userDao.getUserByIdDirect("USR-001")
            if (admin == null) {
                userDao.insertUser(
                    UserEntity(
                        id = "USR-001",
                        fullName = "Jacques Masuruku",
                        phone = "+243 970 000 001",
                        email = "jacquesmasuruku2@gmail.com",
                        role = UserRole.GESTIONNAIRE,
                        marketZone = "Direction Générale",
                        isActive = true,
                        createdDate = System.currentTimeMillis(),
                        pinCode = "1234",
                        username = "directeur",
                        isOnline = true,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        val op = operator ?: UserEntity(
            id = "USR-001",
            fullName = "Jacques Masuruku",
            phone = "+243 970 000 001",
            role = UserRole.GESTIONNAIRE
        )
        logAudit(
            user = op,
            action = "RESET_BASE_DE_DONNEES",
            entityType = "SYSTEM",
            entityId = "ALL",
            details = "Réinitialisation complète de toutes les données. Démarrage de l'application à zéro."
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserActive(operator: UserEntity, targetUserId: String, targetUserName: String, isActive: Boolean): Result<Unit> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Permission refusée."))
        }
        if (operator.id == targetUserId) {
            return Result.failure(Exception("Vous ne pouvez pas désactiver votre propre compte gestionnaire."))
        }
        userDao.toggleUserActive(targetUserId, isActive)
        try {
            neonService?.updateUserActiveStatus(targetUserId, isActive)
        } catch (_: Exception) {}
        val statusDest = "$targetUserName • Statut: ${if (isActive) "Actif" else "Désactivé"}"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = targetUserId,
                destinationAddress = statusDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = if (isActive) "Compte utilisateur réactivé" else "Compte utilisateur suspendu",
                message = if (isActive) "Votre compte a été réactivé par la direction." else "Votre compte a été désactivé par la direction.",
                type = NotificationType.SYSTEM,
                actionRoute = "users"
            )
        )
        logAudit(
            user = operator,
            action = if (isActive) "ACTIVATION_UTILISATEUR" else "DESACTIVATION_UTILISATEUR",
            entityType = "USER",
            entityId = targetUserId,
            details = "Statut actif passé à $isActive pour $targetUserName.",
            destination = statusDest,
            targetUserId = targetUserId
        )
        return Result.success(Unit)
    }

    // --- Clients Management ---
    suspend fun registerClient(
        operator: UserEntity,
        fullName: String,
        phone: String,
        email: String,
        quarter: String = "",
        marketZone: String,
        activityType: String,
        gender: String = "Femme",
        idCardNumber: String = "",
        guarantorName: String = "",
        guarantorPhone: String = "",
        notes: String = "",
        avatarUrl: String? = null
    ): Result<ClientEntity> {
        val clientId = generateNextClientId()
        val client = ClientEntity(
            id = clientId,
            fullName = fullName,
            phone = phone,
            email = email,
            quarter = quarter,
            marketZone = marketZone,
            activityType = activityType,
            gender = gender,
            registeredDate = System.currentTimeMillis(),
            isIdVerified = idCardNumber.isNotBlank(),
            idCardNumber = idCardNumber,
            guarantorName = guarantorName,
            guarantorPhone = guarantorPhone,
            status = "ACTIF",
            assignedAgentId = if (operator.role == UserRole.AGENT) operator.id else null,
            notes = notes,
            avatarUrl = avatarUrl
        )
        clientDao.insertClient(client)
        autoSyncOrSchedule("Création client ${client.fullName}") {
            neonService?.saveClient(client)
        }

        // Also ensure a client user account is ready for login
        val existingUser = userDao.findUserByPhone(phone)
        if (existingUser == null) {
            val clientUser = UserEntity(
                id = "USR-${clientId}",
                fullName = fullName,
                phone = phone,
                email = email,
                role = UserRole.CLIENT,
                marketZone = marketZone,
                isActive = true,
                pinCode = "1234",
                username = fullName.lowercase().replace(" ", ".")
            )
            userDao.insertUser(clientUser)
            autoSyncOrSchedule("Compte utilisateur client ${clientUser.fullName}") {
                neonService?.saveUser(clientUser)
            }
        }

        // Notification d'activité avec destination précise
        val clientDest = "${client.fullName} • Marché $marketZone ($quarter)"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                targetClientId = clientId,
                destinationAddress = clientDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Nouvelle cliente enregistrée",
                message = "${client.fullName} a été enregistrée avec succès ($activityType, $marketZone) par ${operator.fullName}.",
                type = NotificationType.SYSTEM,
                actionRoute = "clients"
            )
        )

        logAudit(
            user = operator,
            action = "CREATION_CLIENT",
            entityType = "CLIENT",
            entityId = clientId,
            details = "Nouveau client enregistré: $fullName ($activityType, $marketZone - Quartier $quarter).",
            destination = clientDest,
            targetClientId = clientId,
            targetUserId = "USR-$clientId"
        )
        return Result.success(client)
    }

    // --- Loan Application & Disbursement ---
    suspend fun submitLoanApplication(
        operator: UserEntity,
        clientId: String,
        amount: Double,
        durationDays: Int = 30,
        purpose: String,
        activityType: String,
        marketZone: String,
        quarter: String = "",
        guarantorName: String = "",
        guarantorPhone: String = "",
        idCardNumber: String = "",
        clientPhotoUri: String? = null,
        documentUri: String? = null,
        acceptedTerms: Boolean = true,
        // Nouveaux champs professionnels MALAKI
        lastName: String = "",
        postName: String = "",
        firstName: String = "",
        birthDate: String = "",
        maritalStatus: String = "Mariée",
        dependentsCount: Int = 0,
        commune: String = "Goma",
        avenue: String = "",
        landmark: String = "",
        residenceDuration: String = "Plus de 3 ans",
        businessLocation: String = "",
        businessSeniority: String = "1 à 2 ans",
        estimatedCapital: Double = 0.0,
        dailyRevenue: Double = 0.0,
        currency: String = "CDF",
        repaymentFrequency: String = "Quotidienne",
        // Signature biométrique native & scellement
        signatureId: String? = null,
        signatureMethod: String = "BIOMETRIC",
        signedAt: Long? = null,
        documentHash: String? = null,
        isLocked: Boolean = true,
        version: Int = 1,
        parentApplicationId: String? = null
    ): Result<LoanApplicationEntity> {
        if (amount <= 0) return Result.failure(Exception("Le montant du prêt doit être supérieur à 0."))

        var client = clientDao.getClientByIdDirect(clientId)
        val computedFullName = listOf(lastName, postName, firstName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { client?.fullName ?: "Nouvelle Cliente" }
        val effectivePhone = client?.phone ?: operator.phone

        if (client == null) {
            // Création automatique de la fiche cliente si première demande
            val newClient = ClientEntity(
                id = clientId,
                fullName = computedFullName,
                phone = effectivePhone,
                marketZone = marketZone.ifBlank { "Marché de Kituku" },
                activityType = activityType.ifBlank { "Alimentation" },
                avatarUrl = clientPhotoUri,
                assignedAgentId = if (operator.role == UserRole.AGENT) operator.id else "USR-002",
                gender = "Femme",
                status = "ACTIF"
            )
            clientDao.insertClient(newClient)
            client = newClient
        }

        val dailyRate = 0.035 // 3.5% par jour
        val dailyAmount = amount * dailyRate
        val appId = "APP-${System.currentTimeMillis() % 100000}"
        val now = signedAt ?: System.currentTimeMillis()

        // Calcul du hash document si non fourni
        val resolvedHash = documentHash ?: CryptoHelper.computeLoanDocumentHash(
            applicationId = appId,
            customerId = client.id,
            fullName = computedFullName,
            phone = effectivePhone,
            amount = amount,
            currency = currency,
            purpose = purpose,
            commune = commune,
            quarter = quarter,
            businessType = activityType,
            timestamp = now,
            version = version
        )

        val resolvedSigId = signatureId ?: CryptoHelper.generateSignatureId()

        val application = LoanApplicationEntity(
            id = appId,
            clientId = client.id,
            clientName = computedFullName,
            clientPhone = effectivePhone,
            amountRequested = amount,
            durationDays = durationDays,
            purpose = purpose,
            activityType = activityType,
            gender = client.gender,
            marketZone = marketZone,
            quarter = quarter,
            guarantorName = guarantorName,
            guarantorPhone = guarantorPhone,
            idCardNumber = idCardNumber,
            documentUri = documentUri,
            clientPhotoUri = clientPhotoUri,
            dailyRate = dailyRate,
            dailyPaymentAmount = dailyAmount,
            acceptedTerms = acceptedTerms,
            status = LoanStatus.SOUMISE,
            submissionDate = now,
            // Nouveaux champs
            lastName = lastName,
            postName = postName,
            firstName = firstName,
            birthDate = birthDate,
            maritalStatus = maritalStatus,
            dependentsCount = dependentsCount,
            commune = commune,
            avenue = avenue,
            landmark = landmark,
            residenceDuration = residenceDuration,
            businessType = activityType,
            businessLocation = businessLocation,
            businessSeniority = businessSeniority,
            estimatedCapital = estimatedCapital,
            dailyRevenue = dailyRevenue,
            currency = currency,
            repaymentFrequency = repaymentFrequency,
            // Preuve de signature biométrique
            signatureId = resolvedSigId,
            signatureMethod = signatureMethod,
            signedAt = now,
            documentHash = resolvedHash,
            isLocked = isLocked,
            version = version,
            parentApplicationId = parentApplicationId
        )
        loanApplicationDao.insertApplication(application)
        autoSyncOrSchedule("Demande de crédit ${application.id}") {
            neonService?.saveLoanApplication(application)
        }

        val appDest = "${computedFullName} • ${marketZone} (Q. ${quarter})"
        // Notify managers with clear client destination
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                targetClientId = clientId,
                destinationAddress = appDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Nouvelle demande de crédit (Signée biométriquement)",
                message = "${computedFullName} sollicite un prêt de ${formatAmount(amount)} $currency (${marketZone} / Q. ${quarter}) - Scellé avec signature $resolvedSigId.",
                type = NotificationType.DEMANDE,
                actionRoute = "applications"
            )
        )

        // Notify client applicant exclusively (sender is SYSTEM so client receives confirmation)
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = "USR-$clientId",
                targetClientId = clientId,
                destinationAddress = appDest,
                senderId = "SYSTEM",
                senderName = "Direction MALISHO",
                title = "Demande de crédit transmise",
                message = "Votre dossier de prêt de ${formatAmount(amount)} $currency a été scellé et transmis à la direction. Statut: En attente d'approbation.",
                type = NotificationType.DEMANDE,
                actionRoute = "applications"
            )
        )

        logAudit(
            user = operator,
            action = "DEMANDE_PRET_SIGNEE_BIOMETRIQUE",
            entityType = "LOAN_APPLICATION",
            entityId = appId,
            details = "Demande signée électroniquement par la cliente $computedFullName. Montant: ${formatAmount(amount)} $currency. SignatureId: $resolvedSigId. Hash SHA-256: ${resolvedHash.take(16)}...",
            destination = appDest,
            targetClientId = clientId,
            targetRole = UserRole.GESTIONNAIRE
        )
        return Result.success(application)
    }

    /**
     * Crée une nouvelle version d'une demande de crédit après modification autorisée,
     * garantissant la traçabilité et exigeant une nouvelle signature.
     */
    suspend fun createLoanApplicationVersion(
        operator: UserEntity,
        originalApp: LoanApplicationEntity,
        newAmount: Double,
        newPurpose: String,
        newActivity: String,
        newSignatureId: String,
        newDocumentHash: String
    ): Result<LoanApplicationEntity> {
        val newVersionNumber = originalApp.version + 1
        return submitLoanApplication(
            operator = operator,
            clientId = originalApp.clientId,
            amount = newAmount,
            durationDays = originalApp.durationDays,
            purpose = newPurpose,
            activityType = newActivity,
            marketZone = originalApp.marketZone,
            quarter = originalApp.quarter,
            guarantorName = originalApp.guarantorName,
            guarantorPhone = originalApp.guarantorPhone,
            idCardNumber = originalApp.idCardNumber,
            clientPhotoUri = originalApp.clientPhotoUri,
            documentUri = originalApp.documentUri,
            acceptedTerms = true,
            lastName = originalApp.lastName,
            postName = originalApp.postName,
            firstName = originalApp.firstName,
            birthDate = originalApp.birthDate,
            maritalStatus = originalApp.maritalStatus,
            dependentsCount = originalApp.dependentsCount,
            commune = originalApp.commune,
            avenue = originalApp.avenue,
            landmark = originalApp.landmark,
            residenceDuration = originalApp.residenceDuration,
            businessLocation = originalApp.businessLocation,
            businessSeniority = originalApp.businessSeniority,
            estimatedCapital = originalApp.estimatedCapital,
            dailyRevenue = originalApp.dailyRevenue,
            currency = originalApp.currency,
            repaymentFrequency = originalApp.repaymentFrequency,
            signatureId = newSignatureId,
            signatureMethod = "BIOMETRIC",
            signedAt = System.currentTimeMillis(),
            documentHash = newDocumentHash,
            isLocked = true,
            version = newVersionNumber,
            parentApplicationId = originalApp.id
        )
    }

    suspend fun reviewLoanApplication(
        operator: UserEntity,
        applicationId: String,
        approved: Boolean,
        reviewNotes: String
    ): Result<Unit> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire peut approuver ou refuser les demandes de prêt."))
        }
        val app = loanApplicationDao.getApplicationById(applicationId).firstOrNull()
            ?: return Result.failure(Exception("Demande introuvable."))

        val newStatus = if (approved) LoanStatus.APPROUVEE else LoanStatus.REFUSEE
        loanApplicationDao.updateStatus(
            id = applicationId,
            status = newStatus,
            reviewer = operator.fullName,
            notes = reviewNotes,
            date = System.currentTimeMillis()
        )
        val updatedApp = app.copy(
            status = newStatus,
            reviewedBy = operator.fullName,
            reviewNotes = reviewNotes,
            reviewDate = System.currentTimeMillis()
        )
        autoSyncOrSchedule("Décision demande de prêt ${updatedApp.id}") {
            neonService?.saveLoanApplication(updatedApp)
            firebaseService?.saveLoanApplicationToFirestore(updatedApp)
        }

        val revDest = "${app.clientName} • ${app.marketZone}"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = "USR-${app.clientId}",
                targetClientId = app.clientId,
                destinationAddress = revDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = if (approved) "Demande de prêt approuvée !" else "Décision sur votre demande de prêt",
                message = if (approved)
                    "Félicitations, votre demande de prêt de ${formatAmount(app.amountRequested)} est approuvée. Le décaissement sera effectué prochainement."
                else
                    "Votre demande de prêt a été refusée. Motif: $reviewNotes.",
                type = NotificationType.DEMANDE
            )
        )

        logAudit(
            user = operator,
            action = if (approved) "DEMANDE_APPROUVEE" else "DEMANDE_REFUSEE",
            entityType = "LOAN_APPLICATION",
            entityId = applicationId,
            details = "Demande ${if (approved) "approuvée" else "refusée"} pour ${app.clientName}. Note: $reviewNotes",
            destination = revDest,
            targetClientId = app.clientId,
            targetUserId = "USR-${app.clientId}"
        )
        return Result.success(Unit)
    }

    suspend fun disburseLoan(
        operator: UserEntity,
        applicationId: String,
        paymentMethod: PaymentMethod,
        assignedAgentId: String? = null
    ): Result<CreditCycleEntity> {
        if (operator.role != UserRole.GESTIONNAIRE && operator.role != UserRole.CAISSIER) {
            return Result.failure(Exception("Permission insuffisante pour autoriser le décaissement."))
        }

        val app = loanApplicationDao.getApplicationById(applicationId).firstOrNull()
            ?: return Result.failure(Exception("Demande de prêt introuvable."))

        if (app.status == LoanStatus.DECAISSEE) {
            return Result.failure(Exception("Ce prêt a déjà été décaissé. Les doubles décaissements sont interdits."))
        }

        val settings = settingsDao.getSettingsDirect() ?: BusinessSettingsEntity()
        val principal = app.amountRequested
        val dailyPayment = principal * settings.defaultDailyRate
        val totalContractual = dailyPayment * app.durationDays

        val cycleId = "CY${(1000..9999).random()}"
        val now = System.currentTimeMillis()

        val cycle = CreditCycleEntity(
            id = cycleId,
            clientId = app.clientId,
            clientName = app.clientName,
            clientPhone = app.clientPhone,
            marketZone = app.marketZone,
            agentId = assignedAgentId ?: "USR-AGT-01",
            agentName = "Patrick Kalombo",
            loanApplicationId = app.id,
            disbursementDate = now,
            principalAmount = principal,
            dailyPaymentAmount = dailyPayment,
            durationDays = app.durationDays,
            totalContractual = totalContractual,
            totalCollected = 0.0,
            balanceRemaining = totalContractual,
            recoveryRate = 0.0,
            grossProfit = 0.0,
            enterpriseShare = 0.0,
            clientShare = 0.0,
            charges = 0.0,
            netProfit = 0.0,
            status = CycleStatus.EN_COURS,
            daysElapsed = 0,
            daysLate = 0,
            notes = "Prêt décaissé le ${formatDate(now)}."
        )

        // 1. Insert credit cycle
        creditCycleDao.insertCycle(cycle)
        autoSyncOrSchedule("Cycle de crédit décaissé ${cycle.id}") {
            neonService?.saveCreditCycle(cycle)
            firebaseService?.saveCreditCycleToFirestore(cycle)
        }

        // 2. Mark application disbursed
        loanApplicationDao.updateStatus(
            id = app.id,
            status = LoanStatus.DECAISSEE,
            reviewer = operator.fullName,
            notes = "Décaissé via ${paymentMethod.label} (Cycle $cycleId)",
            date = now
        )
        val disbursedApp = app.copy(
            status = LoanStatus.DECAISSEE,
            reviewedBy = operator.fullName,
            reviewNotes = "Décaissé via ${paymentMethod.label} (Cycle $cycleId)",
            reviewDate = now
        )
        autoSyncOrSchedule("Statut demande décaissée ${disbursedApp.id}") {
            neonService?.saveLoanApplication(disbursedApp)
            firebaseService?.saveLoanApplicationToFirestore(disbursedApp)
        }

        // 3. Automatically record cash outflow in Caisse
        val cashMovement = CashMovementEntity(
            id = "CSH-${System.currentTimeMillis() % 100000}",
            date = now,
            dateString = formatDate(now),
            type = CashMovementType.SORTIE,
            category = CashMovementCategory.DECAISSEMENT_PRET,
            reference = "DISB-$cycleId",
            description = "Décaissement prêt ${app.clientName} ($cycleId)",
            amount = principal,
            paymentMethod = paymentMethod,
            recordedByUserId = operator.id,
            recordedByName = operator.fullName,
            isApproved = true
        )
        cashDao.insertMovement(cashMovement)
        autoSyncOrSchedule("Sortie Caisse Décaissement $cycleId") {
            neonService?.saveCashMovement(cashMovement)
            firebaseService?.saveCashMovementToFirestore(cashMovement)
        }

        val disbDest = "${app.clientName} • ${app.marketZone}"
        // 4. Notify client exclusively
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = "USR-${app.clientId}",
                targetClientId = app.clientId,
                destinationAddress = disbDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Fonds décaissés",
                message = "Votre crédit de ${formatAmount(principal)} ($cycleId) est décaissé. Votre remboursement quotidien est de ${formatAmount(dailyPayment)}.",
                type = NotificationType.DECAISSEMENT
            )
        )

        // 5. Notify cashier
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.CAISSIER,
                destinationAddress = "Caisse Centrale • Décaissement ${app.clientName}",
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Nouveau décaissement enregistré",
                message = "Sortie de caisse de ${formatAmount(principal)} effectuée pour le cycle $cycleId.",
                type = NotificationType.CAISSE
            )
        )

        logAudit(
            user = operator,
            action = "DECAISSEMENT_EFFECTUE",
            entityType = "CREDIT_CYCLE",
            entityId = cycleId,
            details = "Décaissement de ${formatAmount(principal)} pour ${app.clientName} (Réf Caisse: ${cashMovement.id}).",
            destination = disbDest,
            targetClientId = app.clientId,
            targetUserId = "USR-${app.clientId}"
        )

        return Result.success(cycle)
    }

    suspend fun assignCreditToClient(
        operator: UserEntity,
        clientId: String,
        principalAmount: Double,
        durationDays: Int = 30,
        purpose: String = "Crédit commercial",
        activityType: String = "Commerce",
        marketZone: String = "Marché de Kituku",
        paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
        newClientName: String? = null,
        newClientPhone: String? = null
    ): Result<CreditCycleEntity> {
        if (principalAmount <= 0) {
            return Result.failure(Exception("Le montant du crédit doit être supérieur à zéro."))
        }

        val trimmedClientId = clientId.trim()
        var client = clientDao.getClientByIdDirect(trimmedClientId)
            ?: clientDao.getAllClientsDirect().find {
                it.id.trim().equals(trimmedClientId, ignoreCase = true) ||
                it.fullName.trim().equals(trimmedClientId, ignoreCase = true) ||
                it.phone.trim().equals(trimmedClientId, ignoreCase = true)
            }

        if (client == null && neonService != null) {
            try {
                val q = "SELECT * FROM clients WHERE id = '${trimmedClientId.replace("'", "''")}' OR full_name ILIKE '%${trimmedClientId.replace("'", "''")}%' OR phone = '${trimmedClientId.replace("'", "''")}' LIMIT 1;"
                val res = neonService?.executeSql(q)
                val rows = res?.getOrNull()
                if (rows != null && rows.length() > 0) {
                    val row = rows.getJSONObject(0)
                    val c = ClientEntity(
                        id = row.getString("id"),
                        fullName = row.optString("full_name", newClientName ?: trimmedClientId),
                        phone = row.optString("phone", newClientPhone ?: "+243 990 000 000"),
                        email = row.optString("email", ""),
                        quarter = row.optString("quarter", ""),
                        marketZone = row.optString("market_zone", marketZone),
                        activityType = row.optString("activity_type", activityType),
                        gender = row.optString("gender", "Femme"),
                        registeredDate = row.optLong("registered_date", System.currentTimeMillis()),
                        status = "ACTIF"
                    )
                    clientDao.insertClient(c)
                    client = c
                }
            } catch (_: Exception) {}
        }

        // If client record still not found, create it automatically so credit assignment NEVER fails
        if (client == null) {
            val autoClient = ClientEntity(
                id = if (trimmedClientId.startsWith("CLI-")) trimmedClientId else generateNextClientId(),
                fullName = (newClientName?.trim()?.ifEmpty { null } ?: trimmedClientId).ifEmpty { "Client Bénéficiaire" },
                phone = newClientPhone?.trim()?.ifEmpty { null } ?: "+243 990 000 000",
                email = "",
                quarter = "Centre-ville",
                marketZone = marketZone,
                activityType = activityType,
                gender = "Femme",
                registeredDate = System.currentTimeMillis(),
                status = "ACTIF"
            )
            clientDao.insertClient(autoClient)
            try {
                neonService?.saveClient(autoClient)
            } catch (_: Exception) {}
            client = autoClient
        }

        val dailyPayment = principalAmount * 0.035 // 3,5% par jour de la somme reçue
        val totalContractual = dailyPayment * durationDays
        val now = System.currentTimeMillis()
        val appId = "APP-${System.currentTimeMillis() % 100000}-${(100..999).random()}"
        val cycleId = "CY-${System.currentTimeMillis() % 100000}-${(100..999).random()}"

        // 1. Create completed loan application
        val app = LoanApplicationEntity(
            id = appId,
            clientId = client.id,
            clientName = client.fullName,
            clientPhone = client.phone,
            amountRequested = principalAmount,
            durationDays = durationDays,
            purpose = purpose,
            activityType = activityType.ifEmpty { client.activityType },
            gender = client.gender,
            marketZone = marketZone.ifEmpty { client.marketZone },
            quarter = client.quarter,
            guarantorName = client.guarantorName,
            guarantorPhone = client.guarantorPhone,
            idCardNumber = client.idCardNumber,
            dailyRate = 0.035,
            dailyPaymentAmount = dailyPayment,
            acceptedTerms = true,
            status = LoanStatus.DECAISSEE,
            submissionDate = now,
            reviewedBy = operator.fullName,
            reviewDate = now,
            reviewNotes = "Crédit affecté directement par ${operator.fullName}"
        )
        loanApplicationDao.insertApplication(app)
        try {
            firebaseService?.saveLoanApplicationToFirestore(app)
        } catch (_: Exception) {}

        // 2. Create active credit cycle
        val cycle = CreditCycleEntity(
            id = cycleId,
            clientId = client.id,
            clientName = client.fullName,
            clientPhone = client.phone,
            marketZone = marketZone.ifEmpty { client.marketZone },
            agentId = operator.id,
            agentName = operator.fullName,
            loanApplicationId = appId,
            disbursementDate = now,
            principalAmount = principalAmount,
            dailyPaymentAmount = dailyPayment,
            durationDays = durationDays,
            totalContractual = totalContractual,
            totalCollected = 0.0,
            balanceRemaining = totalContractual,
            recoveryRate = 0.0,
            grossProfit = 0.0,
            enterpriseShare = 0.0,
            clientShare = 0.0,
            charges = 0.0,
            netProfit = 0.0,
            status = CycleStatus.EN_COURS,
            daysElapsed = 0,
            daysLate = 0,
            notes = "Crédit affecté avec succès le ${formatDate(now)}. Montant journalier attendu : ${formatAmount(dailyPayment)} (3,5%)."
        )
        creditCycleDao.insertCycle(cycle)
        autoSyncOrSchedule("Affectation Crédit ${cycle.id}") {
            neonService?.saveCreditCycle(cycle)
            firebaseService?.saveCreditCycleToFirestore(cycle)
        }

        // 3. Update client status to ACTIF
        clientDao.updateClient(client.copy(status = "ACTIF"))
        autoSyncOrSchedule("Activation client ${client.fullName}") {
            neonService?.saveClient(client.copy(status = "ACTIF"))
            firebaseService?.saveClientToFirestore(client.copy(status = "ACTIF"))
        }

        // 4. Record cash outflow
        val cashMovement = CashMovementEntity(
            id = "CSH-${UUID.randomUUID().toString().take(8)}",
            date = now,
            dateString = formatDate(now),
            type = CashMovementType.SORTIE,
            category = CashMovementCategory.DECAISSEMENT_PRET,
            reference = "DISB-$cycleId",
            description = "Décaissement crédit affecté à ${client.fullName} ($cycleId)",
            amount = principalAmount,
            paymentMethod = paymentMethod,
            recordedByUserId = operator.id,
            recordedByName = operator.fullName,
            isApproved = true
        )
        cashDao.insertMovement(cashMovement)
        autoSyncOrSchedule("Sortie Caisse Crédit Affecté $cycleId") {
            neonService?.saveCashMovement(cashMovement)
            firebaseService?.saveCashMovementToFirestore(cashMovement)
        }

        val targetAgentId = client.assignedAgentId ?: operator.id
        val assignDest = "${client.fullName} • Marché ${client.marketZone} (Agent: $targetAgentId)"

        // Notification destinée au client
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = "USR-${client.id}",
                targetClientId = client.id,
                destinationAddress = assignDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Crédit accordé avec succès",
                message = "Votre crédit de ${formatAmount(principalAmount)} est actif ($cycleId). Montant journalier (3,5%) : ${formatAmount(dailyPayment)}/j.",
                type = NotificationType.DECAISSEMENT,
                actionRoute = "cycles"
            )
        )

        // Notification destinée à l'agent assigné
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = targetAgentId,
                targetClientId = client.id,
                destinationAddress = "Agent $targetAgentId • Portefeuille ${client.fullName}",
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Nouveau crédit affecté à votre portefeuille",
                message = "Le client ${client.fullName} (${client.marketZone}) vous a été affecté pour suivi ($cycleId - ${formatAmount(principalAmount)}).",
                type = NotificationType.DECAISSEMENT,
                actionRoute = "cycles"
            )
        )

        logAudit(
            user = operator,
            action = "CREDIT_AFFECTE",
            entityType = "CREDIT_CYCLE",
            entityId = cycleId,
            details = "Affectation de crédit de ${formatAmount(principalAmount)} au client ${client.fullName} (Cycle $cycleId).",
            destination = assignDest,
            targetClientId = client.id,
            targetUserId = targetAgentId
        )

        return Result.success(cycle)
    }

    // --- Payments / Recoveries ---
    suspend fun recordPayment(
        operator: UserEntity,
        cycleId: String,
        amountCollected: Double,
        dayNumber: Int,
        paymentMethod: PaymentMethod,
        notes: String,
        isOffline: Boolean = false,
        paymentTimestamp: Long = System.currentTimeMillis()
    ): Result<PaymentEntity> {
        if (amountCollected <= 0) {
            return Result.failure(Exception("Le montant encaissé doit être strictement supérieur à 0."))
        }

        val cleanCycleId = cycleId.trim()
        val cycle = creditCycleDao.getCycleByIdDirect(cleanCycleId)
            ?: creditCycleDao.getAllCyclesDirect().find { it.id.trim().equals(cleanCycleId, ignoreCase = true) }
            ?: creditCycleDao.getAllCyclesDirect().find { it.clientId.trim().equals(cleanCycleId, ignoreCase = true) }
            ?: return Result.failure(Exception("Cycle de crédit $cleanCycleId introuvable."))

        if (cycle.status == CycleStatus.TERMINE || cycle.status == CycleStatus.ANNULE) {
            return Result.failure(Exception("Ce cycle de crédit est déjà clôturé. Impossible d'enregistrer de paiement sans réouverture administrative."))
        }

        val now = System.currentTimeMillis()
        val actualPaymentTime = if (paymentTimestamp > 0) paymentTimestamp else now
        val paymentId = "PAY-${actualPaymentTime % 100000}-${(100..999).random()}"
        val todayStr = formatDate(actualPaymentTime)

        val isNetAvailable = neonService?.isNetworkAvailable() == true
        val effectivelyOffline = isOffline || !isNetAvailable

        val payment = PaymentEntity(
                id = paymentId,
                cycleId = cycle.id,
                clientId = cycle.clientId,
                clientName = cycle.clientName,
                agentId = operator.id,
                agentName = operator.fullName,
                paymentDate = actualPaymentTime,
                dateString = todayStr,
                dayNumber = dayNumber,
                expectedAmount = cycle.dailyPaymentAmount,
                collectedAmount = amountCollected,
                paymentMethod = paymentMethod,
                notes = notes,
                isOfflineSynced = !effectivelyOffline,
                syncPending = effectivelyOffline,
                createdAt = now
            )
            paymentDao.insertPayment(payment)

            // Recompute cycle numbers accurately based on Excel formulas
            val settings = settingsDao.getSettingsDirect() ?: BusinessSettingsEntity()
            val newTotalCollected = cycle.totalCollected + amountCollected
            val newBalance = (cycle.totalContractual - newTotalCollected).coerceAtLeast(0.0)
            val newRecoveryRate = if (cycle.totalContractual > 0) {
                (newTotalCollected / cycle.totalContractual) * 100.0
            } else 0.0

            val grossProfit = (newTotalCollected - cycle.principalAmount).coerceAtLeast(0.0)
            val enterpriseShare = grossProfit * settings.enterpriseShareRate
            val clientShare = grossProfit * settings.clientShareRate
            val charges = cycle.principalAmount * settings.expenseAllowanceRate * (newTotalCollected / cycle.totalContractual)
            val netProfit = (enterpriseShare - charges).coerceAtLeast(0.0)

            val isFinished = newBalance <= 0.0
            val newStatus = if (isFinished) CycleStatus.TERMINE else CycleStatus.EN_COURS

            val updatedCycle = cycle.copy(
                totalCollected = newTotalCollected,
                balanceRemaining = newBalance,
                recoveryRate = newRecoveryRate,
                grossProfit = grossProfit,
                enterpriseShare = enterpriseShare,
                clientShare = clientShare,
                charges = charges,
                netProfit = netProfit,
                status = newStatus,
                daysElapsed = (cycle.daysElapsed + 1).coerceAtMost(cycle.durationDays)
            )
            creditCycleDao.updateCycle(updatedCycle)

            if (effectivelyOffline) {
                neonService?.schedulePendingSync()
            } else {
                autoSyncOrSchedule("Paiement $paymentId & Cycle ${cycle.id}") {
                    neonService?.saveCreditCycle(updatedCycle)
                    val payRes = neonService?.saveTransaction(payment)
                    if (payRes?.isSuccess == true) {
                        paymentDao.markPaymentSynced(paymentId)
                    } else {
                        paymentDao.markPaymentPendingSync(paymentId)
                        neonService?.schedulePendingSync()
                    }
                    firebaseService?.saveCreditCycleToFirestore(updatedCycle)
                    firebaseService?.saveTransactionToFirestore(payment)
                }
            }

            // Automatically record in Caisse as an ENTREE
            val cashMovement = CashMovementEntity(
                id = "CSH-${UUID.randomUUID().toString().take(8)}",
                date = now,
                dateString = todayStr,
                type = CashMovementType.ENTREE,
                category = CashMovementCategory.REMBOURSEMENT,
                reference = paymentId,
                description = "Recouvrement ${cycle.id} - ${cycle.clientName} (Jour $dayNumber)",
                amount = amountCollected,
                paymentMethod = paymentMethod,
                recordedByUserId = operator.id,
                recordedByName = operator.fullName,
                isApproved = true
            )
            cashDao.insertMovement(cashMovement)
            if (effectivelyOffline) {
                neonService?.schedulePendingSync()
            } else {
                autoSyncOrSchedule("Entrée Caisse Recouvrement $paymentId") {
                    neonService?.saveCashMovement(cashMovement)
                    firebaseService?.saveCashMovementToFirestore(cashMovement)
                }
            }

        val payDest = "${cycle.clientName} • Client ${cycle.clientId}"
        // Notify client exclusively
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetUserId = "USR-${cycle.clientId}",
                targetClientId = cycle.clientId,
                destinationAddress = payDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Reçu de paiement $paymentId",
                message = "Paiement de ${formatAmount(amountCollected)} validé pour le jour $dayNumber. Solde restant: ${formatAmount(newBalance)}.",
                type = NotificationType.PAIEMENT,
                actionRoute = "cycles"
            )
        )

        // Notify manager
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                targetClientId = cycle.clientId,
                destinationAddress = "Direction • Encaissement ${cycle.clientName}",
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Encaissement enregistré",
                message = "${operator.fullName} a encaissé ${formatAmount(amountCollected)} auprès de ${cycle.clientName} ($cycleId).",
                type = NotificationType.PAIEMENT,
                actionRoute = "caisse"
            )
        )

        logAudit(
            user = operator,
            action = "PAIEMENT_ENREGISTRE",
            entityType = "PAYMENT",
            entityId = paymentId,
            details = "Recouvrement de ${formatAmount(amountCollected)} (Jour $dayNumber) sur $cycleId par ${operator.fullName}.",
            destination = payDest,
            targetClientId = cycle.clientId,
            targetUserId = "USR-${cycle.clientId}"
        )

        return Result.success(payment)
    }

    // --- Caisse Management ---
    suspend fun recordCashMovement(
        operator: UserEntity,
        type: CashMovementType,
        category: CashMovementCategory,
        description: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        reference: String,
        receiptUri: String? = null
    ): Result<CashMovementEntity> {
        if (operator.role != UserRole.GESTIONNAIRE && operator.role != UserRole.CAISSIER) {
            return Result.failure(Exception("Seuls le caissier et le gestionnaire peuvent enregistrer des mouvements de caisse."))
        }
        if (amount <= 0) {
            return Result.failure(Exception("Le montant du mouvement doit être strictement supérieur à 0."))
        }
        val now = System.currentTimeMillis()
        val cshId = "CSH-${(1000..9999).random()}"
        val movement = CashMovementEntity(
            id = cshId,
            date = now,
            dateString = formatDate(now),
            type = type,
            category = category,
            reference = reference.ifEmpty { "OP-$cshId" },
            description = description,
            amount = amount,
            paymentMethod = paymentMethod,
            recordedByUserId = operator.id,
            recordedByName = operator.fullName,
            receiptUri = receiptUri,
            isApproved = true
        )
        cashDao.insertMovement(movement)
        try {
            neonService?.saveCashMovement(movement)
            firebaseService?.saveCashMovementToFirestore(movement)
        } catch (_: Exception) {}

        val cashDest = "Caisse Principale • ${operator.fullName} (${operator.role.label})"
        // Notification d'activité de caisse enregistrée dans la table app_notifications
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                destinationAddress = cashDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = if (type == CashMovementType.ENTREE) "Entrée en Caisse (+${formatAmount(amount)})" else "Sortie de Caisse (-${formatAmount(amount)})",
                message = "${operator.fullName} a enregistré une ${type.label.lowercase()} de ${formatAmount(amount)}: $description ($category).",
                type = NotificationType.CAISSE,
                actionRoute = "caisse"
            )
        )

        logAudit(
            user = operator,
            action = if (type == CashMovementType.ENTREE) "ENTREE_CAISSE" else "SORTIE_CAISSE",
            entityType = "CAISSE",
            entityId = cshId,
            details = "${type.label} de ${formatAmount(amount)}: $description ($category).",
            destination = cashDest,
            targetRole = UserRole.GESTIONNAIRE
        )
        return Result.success(movement)
    }

    // --- Chat & Messaging ---
    suspend fun sendChatMessage(
        operator: UserEntity,
        clientId: String,
        content: String,
        attachmentUri: String? = null,
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToContentSnippet: String? = null,
        isVoiceMessage: Boolean = false,
        voiceDurationSeconds: Int = 0,
        audioFilePath: String? = null
    ): Result<ChatMessageEntity> {
        if (content.isBlank() && !isVoiceMessage) return Result.failure(Exception("Le message ne peut pas être vide."))
        val msgId = "MSG-${UUID.randomUUID()}"
        val initialReadList = org.json.JSONArray().apply { put(operator.id) }.toString()
        val initialDeliveredList = org.json.JSONArray().apply { put(operator.id) }.toString()
        val msg = ChatMessageEntity(
            id = msgId,
            senderId = operator.id,
            senderName = operator.fullName,
            senderRole = operator.role,
            clientId = clientId,
            content = content.trim(),
            attachmentUri = attachmentUri,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToContentSnippet = replyToContentSnippet,
            isPinned = false,
            isVoiceMessage = isVoiceMessage,
            voiceDurationSeconds = voiceDurationSeconds,
            audioFilePath = audioFilePath,
            deliveredUserIdsJson = initialDeliveredList,
            readUserIdsJson = initialReadList
        )
        chatDao.insertMessage(msg)
        autoSyncOrSchedule("Chat Message ${msg.id}") {
            neonService?.saveChatMessage(msg)?.getOrThrow()
            try {
                firebaseService?.saveChatMessageToFirestore(msg)
            } catch (_: Exception) {}
        }

        // Téléversement discret et transparent en arrière-plan vers le stockage distant Cloudinary
        if (isVoiceMessage && !audioFilePath.isNullOrEmpty() && cloudinaryService != null) {
            val audioFile = java.io.File(audioFilePath)
            if (audioFile.exists()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val remoteUrl = cloudinaryService?.uploadAudioDocument(
                        file = audioFile,
                        displayName = "voice_${operator.id}_$msgId",
                        senderId = operator.id,
                        senderName = operator.fullName,
                        senderRole = operator.role.name,
                        clientId = clientId,
                        messageId = msgId
                    )
                    if (!remoteUrl.isNullOrBlank()) {
                        val updatedMsg = msg.copy(
                            attachmentUri = remoteUrl,
                            audioFilePath = remoteUrl
                        )
                        chatDao.insertMessage(updatedMsg)
                        try {
                            neonService?.saveChatMessage(updatedMsg)?.getOrThrow()
                            firebaseService?.saveChatMessageToFirestore(updatedMsg)
                        } catch (_: Exception) {}
                    }
                }
            }
        } else if (!attachmentUri.isNullOrEmpty() && cloudinaryService != null) {
            val localFile = java.io.File(attachmentUri)
            if (localFile.exists()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val remoteUrl = cloudinaryService?.uploadMedia(
                        file = localFile,
                        resourceType = "auto",
                        folder = "malaki_microfinance/attachments",
                        displayName = "attachment_${operator.id}_$msgId",
                        senderId = operator.id,
                        senderName = operator.fullName,
                        senderRole = operator.role.name,
                        clientId = clientId,
                        messageId = msgId
                    )
                    if (!remoteUrl.isNullOrBlank()) {
                        val updatedMsg = msg.copy(attachmentUri = remoteUrl)
                        chatDao.insertMessage(updatedMsg)
                        try {
                            neonService?.saveChatMessage(updatedMsg)?.getOrThrow()
                            firebaseService?.saveChatMessageToFirestore(updatedMsg)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Résolution précise de la destination et du destinataire exclusif du message
        val allClients = clientDao.getAllClientsDirect()
        val allUsers = userDao.getAllUsersDirect()
        val (targetUserId, targetClientId, destAddress) = com.example.util.ChatSecurityHelper.resolveMessageDestination(
            sender = operator,
            roomId = clientId,
            clients = allClients,
            allUsers = allUsers
        )

        val notifContent = if (isVoiceMessage) "🎤 Message vocal (${voiceDurationSeconds}s)" else content.trim()
        val notifTitle = when {
            clientId == "COMMUNITY_GROUP" -> "Message de ${operator.fullName} (Communauté)"
            operator.role == UserRole.CLIENT -> "Message de ${operator.fullName} (Client)"
            else -> "Message de ${operator.fullName} (${operator.role.label})"
        }

        // Seule la personne à qui le message est adressé doit recevoir la notification
        pushNotification(
            AppNotificationEntity(
                id = "NOTIF-MSG-$msgId",
                targetUserId = targetUserId,
                targetRole = if (targetUserId == null && clientId != "COMMUNITY_GROUP") {
                    if (operator.role == UserRole.CLIENT) UserRole.AGENT else UserRole.CLIENT
                } else null,
                targetClientId = targetClientId ?: (if (clientId.startsWith("CLI-")) clientId else null),
                destinationAddress = destAddress,
                senderId = operator.id,
                senderName = operator.fullName,
                title = notifTitle,
                message = notifContent,
                type = NotificationType.MESSAGE,
                actionRoute = "chat:$clientId"
            )
        )
        return Result.success(msg)
    }

    suspend fun togglePinChatMessage(messageId: String, pin: Boolean): Result<Unit> {
        val now = if (pin) System.currentTimeMillis() else null
        chatDao.updateMessagePinStatus(messageId, pin, now)
        return Result.success(Unit)
    }

    fun getLatestPinnedMessage(clientId: String) = chatDao.getLatestPinnedMessage(clientId)

    suspend fun ensureCommunityWelcomeMessage() {
        val messages = chatDao.getMessagesForClient("COMMUNITY_GROUP").firstOrNull() ?: emptyList()
        if (messages.isEmpty()) {
            val now = System.currentTimeMillis()
            val welcomeYesterday = ChatMessageEntity(
                id = "MSG-COMMUNITY-YESTERDAY",
                senderId = "USR-002",
                senderName = "Erick Mugisho",
                senderRole = UserRole.AGENT,
                clientId = "COMMUNITY_GROUP",
                content = "Rapport de terrain d'hier : Tous les commerçants du Marché de Kituku ont été sensibilisés au remboursement ponctuel.",
                timestamp = now - 86400000L - 3600000L,
                isRead = true
            )
            val welcome = ChatMessageEntity(
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
            val replyToday = ChatMessageEntity(
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
            chatDao.insertMessage(welcomeYesterday)
            chatDao.insertMessage(welcome)
            chatDao.insertMessage(replyToday)
        }
    }

    suspend fun recordDeliveryAndReadReceipt(messageId: String, recipientUserId: String, isReadStatus: Boolean) {
        val existing = chatDao.getMessageByIdDirect(messageId) ?: return
        val deliveredArr = try {
            org.json.JSONArray(existing.deliveredUserIdsJson)
        } catch (_: Exception) { org.json.JSONArray() }
        val readArr = try {
            org.json.JSONArray(existing.readUserIdsJson)
        } catch (_: Exception) { org.json.JSONArray() }

        var deliveredChanged = false
        var hasDelivered = false
        for (i in 0 until deliveredArr.length()) {
            if (deliveredArr.optString(i) == recipientUserId) {
                hasDelivered = true
                break
            }
        }
        if (!hasDelivered) {
            deliveredArr.put(recipientUserId)
            deliveredChanged = true
        }

        var readChanged = false
        if (isReadStatus) {
            var hasRead = false
            for (i in 0 until readArr.length()) {
                if (readArr.optString(i) == recipientUserId) {
                    hasRead = true
                    break
                }
            }
            if (!hasRead) {
                readArr.put(recipientUserId)
                readChanged = true
            }
        }

        if (deliveredChanged || readChanged) {
            val isReadCalculated = existing.isRead || (readArr.length() > 1)
            chatDao.updateDeliveryAndReadReceipts(
                messageId = messageId,
                deliveredJson = deliveredArr.toString(),
                readJson = readArr.toString(),
                isRead = isReadCalculated
            )
            autoSyncOrSchedule("Receipt $messageId") {
                neonService?.updateMessageReceipts(
                    messageId = messageId,
                    deliveredJson = deliveredArr.toString(),
                    readJson = readArr.toString(),
                    isRead = isReadCalculated
                )
            }
        }
    }

    suspend fun markChatMessagesAsRead(clientId: String, currentUserId: String) {
        chatDao.markMessagesAsRead(clientId, currentUserId)
        try {
            notificationDao.markChatNotificationsAsRead(currentUserId, clientId)
            neonService?.markChatMessagesAsRead(clientId, currentUserId)
            firebaseService?.markChatMessagesAsReadInFirestore(clientId, currentUserId)
        } catch (_: Exception) {}
    }

    suspend fun markSingleMessageAsRead(messageId: String) {
        chatDao.markSingleMessageAsRead(messageId)
    }

    suspend fun editChatMessage(
        operator: UserEntity,
        messageId: String,
        newContent: String
    ): Result<ChatMessageEntity> {
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) return Result.failure(Exception("Le message modifié ne peut pas être vide."))
        val existing = chatDao.getMessageByIdDirect(messageId)
            ?: return Result.failure(Exception("Message introuvable."))
        if (existing.senderId != operator.id) {
            return Result.failure(Exception("Seul le propriétaire de ce message peut le modifier."))
        }
        val elapsedMs = System.currentTimeMillis() - existing.timestamp
        if (elapsedMs > 10 * 60 * 1000L) {
            return Result.failure(Exception("La modification est autorisée uniquement avant 10 minutes (délai dépassé)."))
        }
        val now = System.currentTimeMillis()
        chatDao.updateMessageContent(messageId, trimmed, now)
        try {
            neonService?.updateChatMessage(messageId, trimmed, now)
        } catch (_: Exception) {}
        val updated = existing.copy(content = trimmed, isEdited = true, editedAt = now)
        return Result.success(updated)
    }

    suspend fun deleteChatMessage(
        operator: UserEntity,
        messageId: String
    ): Result<Unit> {
        val existing = chatDao.getMessageByIdDirect(messageId)
            ?: return Result.failure(Exception("Message introuvable."))
        if (existing.senderId != operator.id && operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le propriétaire du message ou le gestionnaire peut le supprimer."))
        }
        chatDao.deleteMessage(messageId)
        try {
            neonService?.deleteChatMessage(messageId)
        } catch (_: Exception) {}
        return Result.success(Unit)
    }

    // --- Offline Synchronisation ---
    suspend fun syncOfflinePayments(operator: UserEntity): Int {
        val syncResult = neonService?.autoSyncPendingData(db)
        val count = syncResult?.getOrNull() ?: 0
        if (count > 0) {
            pushNotification(
                AppNotificationEntity(
                    id = UUID.randomUUID().toString(),
                    targetRole = UserRole.GESTIONNAIRE,
                    title = "Synchronisation automatique",
                    message = "$count élément(s) synchronisé(s) avec succès vers Neon Cloud par ${operator.fullName}.",
                    type = NotificationType.SYSTEM,
                    actionRoute = "dashboard"
                )
            )
            logAudit(
                user = operator,
                action = "SYNCHRONISATION_AUTO",
                entityType = "ALL",
                entityId = "SYNC-${System.currentTimeMillis() % 1000}",
                details = "$count élément(s) synchronisé(s) vers Neon PostgreSQL."
            )
        }
        return count
    }

    /**
     * Enregistre explicitement toute activité opérationnelle dans la table app_notifications.
     */
    suspend fun recordNotificationActivity(
        title: String,
        message: String,
        type: NotificationType = NotificationType.SYSTEM,
        targetRole: UserRole? = null,
        targetUserId: String? = null,
        targetClientId: String? = null,
        destinationAddress: String? = null,
        senderId: String? = null,
        senderName: String? = null,
        actionRoute: String? = null
    ): AppNotificationEntity {
        val notif = AppNotificationEntity(
            id = UUID.randomUUID().toString(),
            targetUserId = targetUserId,
            targetRole = targetRole,
            targetClientId = targetClientId,
            destinationAddress = destinationAddress,
            senderId = senderId,
            senderName = senderName,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = actionRoute
        )
        pushNotification(notif)
        return notif
    }

    // --- Settings update ---
    suspend fun updateSettings(
        operator: UserEntity,
        settings: BusinessSettingsEntity
    ): Result<Unit> {
        if (operator.role != UserRole.GESTIONNAIRE) {
            return Result.failure(Exception("Seul le gestionnaire peut modifier les paramètres généraux."))
        }
        settingsDao.insertOrUpdateSettings(settings)
        try {
            neonService?.saveBusinessSettings(settings)
        } catch (_: Exception) {}

        val setDest = "Direction Générale • Paramètres système"
        pushNotification(
            AppNotificationEntity(
                id = UUID.randomUUID().toString(),
                targetRole = UserRole.GESTIONNAIRE,
                destinationAddress = setDest,
                senderId = operator.id,
                senderName = operator.fullName,
                title = "Paramètres d'exploitation modifiés",
                message = "Taux journalier fixé à ${(settings.defaultDailyRate * 100)}%, Durée: ${settings.defaultDurationDays}j par ${operator.fullName}.",
                type = NotificationType.SYSTEM,
                actionRoute = "settings"
            )
        )

        logAudit(
            user = operator,
            action = "PARAMETRES_MODIFIES",
            entityType = "SETTINGS",
            entityId = "1",
            details = "Mise à jour des paramètres: Taux jour ${(settings.defaultDailyRate * 100)}%, Durée ${settings.defaultDurationDays}j.",
            destination = setDest,
            targetRole = UserRole.GESTIONNAIRE
        )
        return Result.success(Unit)
    }

    suspend fun logAudit(
        user: UserEntity,
        action: String,
        entityType: String,
        entityId: String,
        details: String,
        destination: String = "Direction & Système",
        targetUserId: String? = null,
        targetClientId: String? = null,
        targetRole: UserRole? = null
    ) {
        val log = AuditLogEntity(
            id = "AUD-${UUID.randomUUID()}",
            userId = user.id,
            userName = user.fullName,
            userRole = user.role,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details,
            destination = destination,
            targetUserId = targetUserId,
            targetClientId = targetClientId,
            targetRole = targetRole,
            timestamp = System.currentTimeMillis()
        )
        auditDao.insertLog(log)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                neonService?.saveAuditLog(log)
                firebaseService?.saveAuditLogToFirestore(log)
            } catch (_: Exception) {}
        }
    }

    companion object {
        /**
         * Extrait le numéro d'ordre séquentiel d'un matricule client.
         * Ex: "CLI-001" -> 1, "CLI-015" -> 15, "USR-CLI-002" -> 2
         */
        fun extractClientSequenceNumber(clientId: String): Int? {
            val clean = clientId.trim()
            val match = Regex("""^CLI-(\d+)$""", RegexOption.IGNORE_CASE).find(clean)
                ?: Regex("""^USR-CLI-(\d+)$""", RegexOption.IGNORE_CASE).find(clean)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

        /**
         * Retourne le quantième (rang d'ordre ordinal) clair d'un client à partir de son matricule.
         * Permet de savoir précisément et instantanément si c'est le 1er client, 2ème client, 3ème client, etc.
         * Ex: CLI-001 -> "1er client" (Swahili: "Mteja wa 1")
         * Ex: CLI-002 -> "2ème client" (Swahili: "Mteja wa 2")
         */
        fun getClientQuantiemeLabel(clientId: String, lang: com.example.util.AppLanguage = com.example.util.AppLanguage.FRENCH): String {
            val seq = extractClientSequenceNumber(clientId) ?: return ""
            return if (lang == com.example.util.AppLanguage.SWAHILI) {
                "Mteja wa $seq"
            } else {
                if (seq == 1) "1er client" else "${seq}ème client"
            }
        }

        fun formatAmount(amount: Double): String {
            val rounded = amount.toLong()
            val formatted = String.format(Locale.FRENCH, "%,d", rounded).replace(',', ' ')
            return "$formatted FC"
        }

        fun formatPercent(rate: Double): String {
            return String.format(Locale.FRENCH, "%.1f %%", rate)
        }

        fun formatDateTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH)
            return sdf.format(Date(timestamp))
        }

        fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
            return sdf.format(Date(timestamp))
        }

        fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.FRENCH)
            return sdf.format(Date(timestamp))
        }
    }
}
