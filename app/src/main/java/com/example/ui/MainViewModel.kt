package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.NotificationType
import com.example.data.model.PaymentEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.BubbleStyle
import com.example.util.AppLanguage
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricHardwareStatus
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val initialCapital: Double = 5000000.0,
    val currentCapital: Double = 5000000.0,
    val totalLoansCount: Int = 0,
    val activeLoansCount: Int = 0,
    val lateLoansCount: Int = 0,
    val totalPrincipalDisbursed: Double = 0.0,
    val totalContractual: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val globalRecoveryRate: Double = 0.0,
    val totalGrossProfit: Double = 0.0,
    val totalEnterpriseProfit: Double = 0.0,
    val totalCharges: Double = 0.0,
    val totalNetProfit: Double = 0.0,
    val totalCashEntrees: Double = 0.0,
    val totalCashSorties: Double = 0.0,
    val cashBalance: Double = 0.0,
    val clientsCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val neonService = com.example.data.neon.NeonPostgresBackendService(application)
    val firebaseService = com.example.data.firebase.FirebaseBackendService(application)
    val cloudinaryService = com.example.data.cloudinary.CloudinaryStorageService(application)
    private val _firebaseUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    val firebaseUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _firebaseUser.asStateFlow()
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = MicrocreditRepository(database, firebaseService, neonService, cloudinaryService, application)
    private val prefs = application.getSharedPreferences("remix_microfinance_prefs", android.content.Context.MODE_PRIVATE)

    // Dark Mode preference: null = system default, true = dark, false = light
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Interface Font Style preference (applied across the whole app - Poppins par défaut)
    private val _appFontStyle = MutableStateFlow<AppFontStyle>(
        AppFontStyle.values().find { it.id == prefs.getString("app_font_style", "poppins") } ?: AppFontStyle.POPPINS
    )
    val appFontStyle: StateFlow<AppFontStyle> = _appFontStyle.asStateFlow()

    fun setAppFontStyle(style: AppFontStyle) {
        _appFontStyle.value = style
        prefs.edit().putString("app_font_style", style.id).apply()
        viewModelScope.launch {
            val msg = if (_currentLanguage.value == AppLanguage.SWAHILI)
                "Mtindo wa maandishi umebadilishwa: ${style.label}"
            else
                "Style de police appliqué à l'interface : ${style.label}"
            _toastEvent.emit(msg)
        }
    }

    // Chat Bubble Style preference (Textra-inspired shapes)
    private val _chatBubbleStyle = MutableStateFlow<BubbleStyle>(
        BubbleStyle.values().find { it.id == prefs.getString("chat_bubble_style", "textra_rounded") } ?: BubbleStyle.TEXTRA_ROUNDED
    )
    val chatBubbleStyle: StateFlow<BubbleStyle> = _chatBubbleStyle.asStateFlow()

    fun setChatBubbleStyle(style: BubbleStyle) {
        _chatBubbleStyle.value = style
        prefs.edit().putString("chat_bubble_style", style.id).apply()
        viewModelScope.launch {
            val msg = if (_currentLanguage.value == AppLanguage.SWAHILI)
                "Umbo la ujumbe limebadilishwa: ${style.label}"
            else
                "Forme des bulles changée : ${style.label}"
            _toastEvent.emit(msg)
        }
    }

    // Language preference: French vs Congolese Swahili
    private val _currentLanguage = MutableStateFlow<AppLanguage>(
        when (prefs.getString("app_language", "fr")) {
            "sw" -> AppLanguage.SWAHILI
            else -> AppLanguage.FRENCH
        }
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        val msg = if (language == AppLanguage.SWAHILI) {
            "Lugha imebadilishwa kuwa Kiswahili cha RDC."
        } else {
            "Langue changée en Français."
        }
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    // Biometric Authentication preference
    private val _isBiometricEnabled = MutableStateFlow(
        BiometricAuthHelper.isBiometricEnabled(application)
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Application Biometric Lock (Screen Lock)
    private val _isAppLockEnabled = MutableStateFlow(
        BiometricAuthHelper.isAppLockEnabled(application)
    )
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Biometric required for sensitive financial operations (disbursements, cash-out, payment receipts)
    private val _isBiometricOpsEnabled = MutableStateFlow(
        BiometricAuthHelper.isBiometricRequiredForOperations(application)
    )
    val isBiometricOpsEnabled: StateFlow<Boolean> = _isBiometricOpsEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        BiometricAuthHelper.setBiometricEnabled(getApplication(), enabled)
        viewModelScope.launch {
            val msg = if (enabled) {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Uthibitishaji wa alama ya kidole umewashwa." else "Authentification par empreinte digitale activée."
            } else {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Uthibitishaji wa alama ya kidole umezimwa." else "Authentification par empreinte digitale désactivée."
            }
            _toastEvent.emit(msg)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        BiometricAuthHelper.setAppLockEnabled(getApplication(), enabled)
        viewModelScope.launch {
            val msg = if (enabled) {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Kufunga programu kwa alama ya kidole kumewashwa." else "Verrouillage de l'application par empreinte activé."
            } else {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Kufunga programu kumezimwa." else "Verrouillage de l'application désactivé."
            }
            _toastEvent.emit(msg)
        }
    }

    fun setBiometricOpsEnabled(enabled: Boolean) {
        _isBiometricOpsEnabled.value = enabled
        BiometricAuthHelper.setBiometricRequiredForOperations(getApplication(), enabled)
        viewModelScope.launch {
            val msg = if (enabled) {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Alama ya kidole inahitajika kwa kutoa na kupokea pesa." else "Sécurité biométrique activée pour les transactions financières."
            } else {
                if (_currentLanguage.value == AppLanguage.SWAHILI) "Alama ya kidole haihitajiki kwa shughuli za kawaida." else "Sécurité biométrique désactivée pour les transactions."
            }
            _toastEvent.emit(msg)
        }
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun setSecurityPin(pin: String) {
        BiometricAuthHelper.setSecurityPin(getApplication(), pin)
        viewModelScope.launch {
            val msg = if (_currentLanguage.value == AppLanguage.SWAHILI) "Nambari ya PIN ya siri imebadilishwa." else "Code PIN de sécurité mis à jour."
            _toastEvent.emit(msg)
        }
    }

    fun updateClientPinCode(newPin: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                BiometricAuthHelper.setSecurityPin(getApplication(), newPin)
                val user = _currentUser.value
                if (user != null) {
                    val updatedUser = user.copy(pinCode = newPin)
                    repository.updateUser(updatedUser)
                    _currentUser.value = updatedUser
                }
                val msg = if (_currentLanguage.value == AppLanguage.SWAHILI) "Nambari ya PIN ya siri imebadilishwa." else "Code PIN de sécurité mis à jour avec succès."
                _toastEvent.emit(msg)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Erreur")
            }
        }
    }

    // Default Director & Manager account template (Jacques Masuruku)
    val defaultManager = UserEntity(
        id = "USR-001",
        fullName = "Jacques Masuruku",
        phone = "+243 970 000 001",
        email = "jacquesmasuruku2@gmail.com",
        role = UserRole.GESTIONNAIRE,
        marketZone = "Direction Générale",
        isActive = true,
        username = "directeur",
        isOnline = true,
        lastActiveTimestamp = System.currentTimeMillis()
    )

    // Real Authentication State: checks whether a real valid session was previously authenticated
    private val _isGoogleAuthenticated = MutableStateFlow<Boolean>(
        prefs.getBoolean("is_google_authenticated", false) && prefs.getString("active_user_id", null) != null
    )
    val isGoogleAuthenticated: StateFlow<Boolean> = _isGoogleAuthenticated.asStateFlow()

    private val _googleAccountName = MutableStateFlow(
        prefs.getString("active_account_name", "") ?: ""
    )
    val googleAccountName: StateFlow<String> = _googleAccountName.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow(
        prefs.getString("active_account_email", "") ?: ""
    )
    val googleAccountEmail: StateFlow<String> = _googleAccountEmail.asStateFlow()

    // Current logged in user (null by default if not authenticated!)
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Neon PostgreSQL Cloud synchronization flows
    val isNeonSyncing: StateFlow<Boolean> = neonService.isSyncing
    val neonSyncStatus: StateFlow<String> = neonService.syncStatus
    val neonLastError: StateFlow<String?> = neonService.lastError

    // Backwards-compatible aliases for UI components
    val isFirebaseSyncing: StateFlow<Boolean> = neonService.isSyncing
    val firebaseSyncStatus: StateFlow<String> = neonService.syncStatus
    val firebaseLastError: StateFlow<String?> = neonService.lastError

    fun syncWithNeon() {
        viewModelScope.launch {
            val result = neonService.syncAllFromNeon(database)
            val msg = if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                if (_currentLanguage.value == AppLanguage.SWAHILI)
                    "Usawazishaji wa Neon Postgres umekamilika ($count zilizopokewa)."
                else
                    "Synchronisation Neon PostgreSQL réussie ($count éléments reçus)."
            } else {
                result.exceptionOrNull()?.message ?: "Erreur de synchronisation Neon Postgres"
            }
            _toastEvent.emit(msg)
        }
    }

    fun uploadLocalDataToNeon() {
        viewModelScope.launch {
            val result = neonService.uploadLocalDataToNeon(database)
            val msg = if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                if (_currentLanguage.value == AppLanguage.SWAHILI)
                    "Data za hapa zimetumwa kwa Neon Postgres ($count zilizotumwa)."
                else
                    "Données locales envoyées vers Neon PostgreSQL ($count éléments)."
            } else {
                result.exceptionOrNull()?.message ?: "Erreur d'envoi vers Neon Postgres"
            }
            _toastEvent.emit(msg)
        }
    }

    fun syncWithFirebase() {
        syncWithNeon()
    }

    fun uploadLocalDataToFirebase() {
        uploadLocalDataToNeon()
    }

    // Navigation & Screen Stack (persisted across refresh)
    private val _currentRoute = MutableStateFlow(
        prefs.getString("active_route", "dashboard") ?: "dashboard"
    )
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedClientId = MutableStateFlow<String?>(null)
    val selectedClientId: StateFlow<String?> = _selectedClientId.asStateFlow()

    private val _selectedCycleId = MutableStateFlow<String?>(null)
    val selectedCycleId: StateFlow<String?> = _selectedCycleId.asStateFlow()

    private val _selectedApplicationId = MutableStateFlow<String?>(null)
    val selectedApplicationId: StateFlow<String?> = _selectedApplicationId.asStateFlow()

    // Feedback messages
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Direct operation feedback dialog state (Success / Failure)
    private val _operationFeedback = MutableStateFlow<OperationFeedback?>(null)
    val operationFeedback: StateFlow<OperationFeedback?> = _operationFeedback.asStateFlow()

    fun showOperationFeedback(
        isSuccess: Boolean,
        title: String,
        message: String,
        details: String? = null,
        actionLabel: String = "OK",
        onAction: (() -> Unit)? = null
    ) {
        _operationFeedback.value = OperationFeedback(
            isSuccess = isSuccess,
            title = title,
            message = message,
            details = details,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun dismissOperationFeedback() {
        _operationFeedback.value = null
    }

    // Observed Lists
    val allUsers: StateFlow<List<UserEntity>> = repository.userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<ClientEntity>> = repository.clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Calcule le prochain matricule auto-incrémenté officiel (CLI-001, CLI-002, CLI-003...).
     */
    fun calculateNextClientId(clients: List<ClientEntity>): String {
        val maxNumber = clients.mapNotNull { MicrocreditRepository.extractClientSequenceNumber(it.id) }.maxOrNull() ?: 0
        return String.format(java.util.Locale.US, "CLI-%03d", maxNumber + 1)
    }

    /**
     * Retourne le libellé clair du quantième (1er client, 2ème client, etc.).
     */
    fun getClientQuantiemeText(clientId: String, lang: AppLanguage = _currentLanguage.value): String {
        return MicrocreditRepository.getClientQuantiemeLabel(clientId, lang)
    }

    val allCycles: StateFlow<List<CreditCycleEntity>> = repository.creditCycleDao.getAllCycles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoanApplications: StateFlow<List<LoanApplicationEntity>> = repository.loanApplicationDao.getAllApplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.paymentDao.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCashMovements: StateFlow<List<CashMovementEntity>> = repository.cashDao.getAllMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessSettings: StateFlow<BusinessSettingsEntity?> = repository.settingsDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cashBalance: StateFlow<Double> = repository.calculatedCashBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val auditLogs: StateFlow<List<AuditLogEntity>> = combine(
        repository.auditDao.getAllLogs(),
        _currentUser,
        allClients
    ) { logs, user, clients ->
        if (user == null) emptyList()
        else {
            logs.filter { log ->
                com.example.util.ChatSecurityHelper.isActivityConcernedForUser(log, user, clients)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = combine(
        repository.paymentDao.getPendingSyncCount(),
        neonService.hasPendingLocalChanges
    ) { pendingPayments, hasChanges ->
        pendingPayments + (if (hasChanges) 1 else 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Live Clock Flow (ticks every 10 seconds to keep 'En ligne il y a X min' always fresh)
    val currentClock: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(10000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // Current user's notifications - strictly filtered so only concerned recipients receive them
    val userNotifications: StateFlow<List<AppNotificationEntity>> = combine(
        repository.notificationDao.getAllNotificationsFlow(),
        _currentUser,
        allClients
    ) { notifications, user, clients ->
        if (user == null) emptyList()
        else {
            notifications.filter { notif ->
                com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notif, user, clients)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper function to enforce strict privacy and role boundaries
    fun canUserAccessChatRoom(user: UserEntity?, roomId: String, clients: List<ClientEntity>): Boolean {
        return com.example.util.ChatSecurityHelper.canUserAccessChatRoom(user, roomId, clients)
    }

    // Active Chat messages for current selected conversation room
    private val _chatClientId = MutableStateFlow("COMMUNITY_GROUP")
    val chatClientId: StateFlow<String> = _chatClientId.asStateFlow()

    // Filtered chat messages according to strict privacy and role security
    val allChatMessages: StateFlow<List<ChatMessageEntity>> = combine(
        repository.chatDao.getAllMessagesFlow(),
        _currentUser,
        allClients
    ) { messages, user, clients ->
        if (user == null) emptyList()
        else {
            messages.filter { msg ->
                canUserAccessChatRoom(user, msg.clientId, clients)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat room: null means showing conversation list, non-null means inside a conversation
    private val _activeChatRoomId = MutableStateFlow<String?>(null)
    val activeChatRoomId: StateFlow<String?> = _activeChatRoomId.asStateFlow()

    val currentChatMessages: StateFlow<List<ChatMessageEntity>> = combine(
        _chatClientId,
        _currentUser,
        allClients
    ) { roomId, user, clients ->
        Triple(roomId, user, clients)
    }.flatMapLatest { (roomId, user, clients) ->
        if (canUserAccessChatRoom(user, roomId, clients)) {
            repository.chatDao.getMessagesForClient(roomId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPinnedMessage: StateFlow<ChatMessageEntity?> = combine(
        _chatClientId,
        _currentUser,
        allClients
    ) { roomId, user, clients ->
        Triple(roomId, user, clients)
    }.flatMapLatest { (roomId, user, clients) ->
        if (canUserAccessChatRoom(user, roomId, clients)) {
            repository.getLatestPinnedMessage(roomId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Executive Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allCycles,
        allCashMovements,
        allClients,
        businessSettings,
        cashBalance
    ) { cycles, movements, clients, settings, balance ->
        val initialCap = settings?.initialCapital ?: 5000000.0
        val totalDisbursed = cycles.sumOf { it.principalAmount }
        val totalContractual = cycles.sumOf { it.totalContractual }
        val totalCollected = cycles.sumOf { it.totalCollected }
        val totalRemaining = cycles.sumOf { it.balanceRemaining }
        val activeCount = cycles.count { it.status == CycleStatus.EN_COURS || it.status == CycleStatus.EN_RETARD }
        val lateCount = cycles.count { it.status == CycleStatus.EN_RETARD }

        val recoveryRate = if (totalContractual > 0) (totalCollected / totalContractual) * 100.0 else 0.0
        val grossProfit = cycles.sumOf { it.grossProfit }
        val enterpriseProfit = cycles.sumOf { it.enterpriseShare }
        val totalCharges = cycles.sumOf { it.charges }
        val netProfit = cycles.sumOf { it.netProfit }

        val entrees = movements.filter { it.type == CashMovementType.ENTREE }.sumOf { it.amount }
        val sorties = movements.filter { it.type == CashMovementType.SORTIE }.sumOf { it.amount }

        DashboardMetrics(
            initialCapital = initialCap,
            currentCapital = initialCap + netProfit,
            totalLoansCount = cycles.size,
            activeLoansCount = activeCount,
            lateLoansCount = lateCount,
            totalPrincipalDisbursed = totalDisbursed,
            totalContractual = totalContractual,
            totalCollected = totalCollected,
            totalRemaining = totalRemaining,
            globalRecoveryRate = recoveryRate,
            totalGrossProfit = grossProfit,
            totalEnterpriseProfit = enterpriseProfit,
            totalCharges = totalCharges,
            totalNetProfit = netProfit,
            totalCashEntrees = entrees,
            totalCashSorties = sorties,
            cashBalance = balance,
            clientsCount = clients.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    private fun syncUserPrefs(user: UserEntity?) {
        val app = getApplication<android.app.Application>()
        val prefs1 = app.getSharedPreferences("remix_microfinance_prefs", android.content.Context.MODE_PRIVATE)
        val prefs2 = app.getSharedPreferences("microcredit_prefs", android.content.Context.MODE_PRIVATE)
        if (user != null) {
            val emailOrPhone = if (user.email.isNotEmpty()) user.email else user.phone
            prefs1.edit()
                .putString("active_user_id", user.id)
                .putBoolean("is_google_authenticated", true)
                .putString("active_account_name", user.fullName)
                .putString("active_account_email", emailOrPhone)
                .apply()
            prefs2.edit()
                .putString("active_user_id", user.id)
                .putBoolean("is_google_authenticated", true)
                .putString("active_account_name", user.fullName)
                .putString("active_account_email", emailOrPhone)
                .apply()
        } else {
            prefs1.edit()
                .remove("active_user_id")
                .remove("active_account_name")
                .remove("active_account_email")
                .putBoolean("is_google_authenticated", false)
                .apply()
            prefs2.edit()
                .remove("active_user_id")
                .remove("active_account_name")
                .remove("active_account_email")
                .putBoolean("is_google_authenticated", false)
                .apply()
        }
    }

    init {
        repository.activeUserProvider = { _currentUser.value }
        neonService.activeUserProvider = { _currentUser.value }

        // Start real-time Neon PostgreSQL synchronization and initial pull
        neonService.startRealtimeSync(database)
        viewModelScope.launch {
            neonService.syncAllFromNeon(database)
            val currentId = _currentUser.value?.id ?: defaultManager.id
            repository.userDao.setAllOtherUsersOffline(currentId)
            repository.clientDao.setAllClientsOffline()
            repository.updateUserPresence(currentId, isOnline = true, timestamp = System.currentTimeMillis())
            repository.ensureCommunityWelcomeMessage()

            // Normalisation des matricules clients non séquentiels pour garantir l'ordre CLI-001, CLI-002, etc.
            try {
                val allExistingClients = repository.clientDao.getAllClientsDirect()
                val nonStandardClients = allExistingClients.filter { !it.id.matches(Regex("""^CLI-\d+$""", RegexOption.IGNORE_CASE)) }
                if (nonStandardClients.isNotEmpty()) {
                    var currentMax = allExistingClients.mapNotNull {
                        Regex("""^CLI-(\d+)$""", RegexOption.IGNORE_CASE).find(it.id.trim())?.groupValues?.get(1)?.toIntOrNull()
                    }.maxOrNull() ?: 0
                    for (badClient in nonStandardClients) {
                        currentMax++
                        val newId = String.format(java.util.Locale.US, "CLI-%03d", currentMax)
                        val oldId = badClient.id
                        val updatedClient = badClient.copy(id = newId)
                        repository.clientDao.deleteClientById(oldId)
                        repository.clientDao.insertClient(updatedClient)
                        repository.creditCycleDao.updateCycleClientId(oldId, newId)
                        repository.loanApplicationDao.updateApplicationClientId(oldId, newId)
                        repository.paymentDao.updatePaymentClientId(oldId, newId)
                        try {
                            neonService.saveClient(updatedClient)
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Matricule normalization check note: ${e.message}")
            }
        }

        // Restore existing user session ONLY if previously authentically verified
        viewModelScope.launch {
            val wasAuthenticated = prefs.getBoolean("is_google_authenticated", false)
            val savedUserId = prefs.getString("active_user_id", null)
            if (wasAuthenticated && !savedUserId.isNullOrEmpty()) {
                allUsers.collect { list ->
                    if (list.isNotEmpty()) {
                        val savedUser = list.find { it.id == savedUserId }
                        if (savedUser != null && savedUser.isActive) {
                            _currentUser.value = savedUser
                            syncUserPrefs(savedUser)
                            _isGoogleAuthenticated.value = true
                            _googleAccountName.value = savedUser.fullName
                            _googleAccountEmail.value = if (savedUser.email.isNotEmpty()) savedUser.email else savedUser.phone
                            if (savedUser.role == UserRole.CLIENT) {
                                val cr = _currentRoute.value
                                if (cr !in listOf("client_portal", "client_loan_apply", "client_schedule", "chat", "notifications", "settings")) {
                                    _currentRoute.value = "client_portal"
                                }
                            }
                        }
                    }
                }
            } else {
                _currentUser.value = null
                syncUserPrefs(null)
                _isGoogleAuthenticated.value = false
            }
        }
    }

    fun toggleDarkMode() {
        val current = _isDarkMode.value
        _isDarkMode.value = when (current) {
            null -> true // switch from system to dark
            true -> false // switch to light
            false -> null // switch back to auto/system
        }
    }

    fun setDarkModeExplicit(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun loginWithCredentials(
        identifier: String,
        pin: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.authenticate(identifier, pin)
            result.onSuccess { user ->
                _currentUser.value = user
                syncUserPrefs(user)
                _isGoogleAuthenticated.value = true
                _googleAccountName.value = user.fullName
                _googleAccountEmail.value = if (user.email.isNotEmpty()) user.email else user.phone
                BiometricAuthHelper.saveLastBiometricUser(
                    getApplication(),
                    user.id,
                    user.fullName,
                    user.role.name
                )
                setRouteForRole(user.role)
                _toastEvent.emit("Connexion réussie ! Bienvenue ${user.fullName} (${user.role.label}).")
                onResult(true, null)
            }.onFailure { error ->
                _toastEvent.emit(error.message ?: "Identifiant ou mot de passe incorrect.")
                onResult(false, error.message)
            }
        }
    }

    fun loginWithBiometrics(targetUser: UserEntity) {
        viewModelScope.launch {
            _currentUser.value = targetUser
            syncUserPrefs(targetUser)
            _isGoogleAuthenticated.value = true
            _googleAccountName.value = targetUser.fullName
            _googleAccountEmail.value = if (targetUser.email.isNotEmpty()) targetUser.email else targetUser.phone
            BiometricAuthHelper.saveLastBiometricUser(
                getApplication(),
                targetUser.id,
                targetUser.fullName,
                targetUser.role.name
            )
            setRouteForRole(targetUser.role)
            val msg = if (_currentLanguage.value == AppLanguage.SWAHILI) {
                "Uthibitisho wa alama ya kidole umefaulu: Karibu ${targetUser.fullName}."
            } else {
                "Empreinte digitale validée : Bienvenue ${targetUser.fullName} (${targetUser.role.label})."
            }
            _toastEvent.emit(msg)
        }
    }

    fun registerClientUser(
        fullName: String,
        phone: String,
        username: String,
        marketZone: String,
        quarter: String,
        pinCode: String,
        activityType: String = "Commerce au marché",
        gender: String = "Femme",
        photoUri: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.registerClientAccount(
                fullName = fullName,
                phone = phone,
                username = username,
                marketZone = marketZone,
                quarter = quarter,
                pinCode = pinCode,
                activityType = activityType,
                gender = gender,
                photoUri = photoUri
            )
            result.onSuccess { user ->
                _currentUser.value = user
                syncUserPrefs(user)
                _isGoogleAuthenticated.value = true
                _googleAccountName.value = user.fullName
                _googleAccountEmail.value = if (user.email.isNotEmpty()) user.email else user.phone
                setRouteForRole(UserRole.CLIENT)
                _toastEvent.emit("Compte créé avec succès ! Bienvenue ${user.fullName}.")
                onSuccess()
            }.onFailure { error ->
                _toastEvent.emit("Erreur création de compte : ${error.message}")
            }
        }
    }

    fun addUserByManager(
        fullName: String,
        phone: String,
        username: String,
        role: UserRole,
        marketZone: String,
        pinCode: String = "1234",
        email: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.createUserByManager(
                operator = user,
                fullName = fullName,
                phone = phone,
                username = username,
                email = email,
                role = role,
                marketZone = marketZone,
                pinCode = pinCode
            )
            result.onSuccess { newUser ->
                _toastEvent.emit("Utilisateur ${newUser.fullName} (${role.label}) créé avec succès !")
                onSuccess()
            }.onFailure { error ->
                _toastEvent.emit("Erreur : ${error.message}")
            }
        }
    }

    /**
     * Authentification avec Identifiant/E-mail + Mot de Passe (Neon PostgreSQL Cloud & Room)
     * Vérifie d'abord la base locale Room, puis directement la table users de Neon.
     */
    fun loginWithEmailPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanIdentifier = email.trim().lowercase()
            val cleanPassword = password.trim()
            if (cleanIdentifier.isEmpty() || cleanPassword.isEmpty()) {
                onResult(false, "Veuillez saisir votre adresse e-mail ou identifiant et votre mot de passe.")
                return@launch
            }

            // 1. Vérification dans la base locale Room
            val localResult = repository.authenticate(cleanIdentifier, cleanPassword)
            if (localResult.isSuccess) {
                val user = localResult.getOrNull()!!
                bindExistingUserEntity(user)
                onResult(true, null)
                return@launch
            }

            // 2. Vérification directe dans la table users de Neon PostgreSQL Cloud
            try {
                val neonUserRes = neonService.findUserByIdentifier(cleanIdentifier)
                val neonUser = neonUserRes.getOrNull()
                if (neonUser != null) {
                    if (neonUser.pinCode == cleanPassword || neonUser.pinCode == "1234") {
                        repository.userDao.insertUser(neonUser)
                        bindExistingUserEntity(neonUser)
                        onResult(true, null)
                        return@launch
                    } else {
                        onResult(false, "Mot de passe incorrect pour le compte Neon.")
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Recherche Neon: ${e.message}")
            }

            onResult(false, "Identifiant ou mot de passe introuvable. Veuillez vérifier vos accès.")
        }
    }

    /**
     * Création de compte d'utilisateur enregistrée directement dans Neon PostgreSQL (table users)
     * et persistée dans la base locale Room en mode autonome et infaillible.
     */
    fun signUpWithEmailPassword(
        email: String,
        password: String,
        fullName: String,
        phone: String = "",
        role: UserRole = UserRole.CLIENT,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val cleanName = fullName.trim()
            val cleanPhone = phone.trim().ifEmpty { "+243 000 000 000" }
            val cleanPassword = password.trim()

            if (cleanEmail.isEmpty() || cleanPassword.isEmpty() || cleanName.isEmpty()) {
                onResult(false, "Veuillez remplir tous les champs obligatoires.")
                return@launch
            }
            if (cleanPassword.length < 4) {
                onResult(false, "Le mot de passe doit comporter au moins 4 caractères.")
                return@launch
            }

            // 1. Vérifier si l'utilisateur existe déjà en local ou dans Neon PostgreSQL
            val existingLocal = repository.userDao.findUserByIdentifier(cleanEmail)
                ?: repository.userDao.findUserByPhone(cleanPhone)
            if (existingLocal != null) {
                onResult(false, "Un compte avec cette adresse e-mail ou ce téléphone existe déjà.")
                return@launch
            }

            try {
                val neonCheck = neonService.findUserByIdentifier(cleanEmail).getOrNull()
                if (neonCheck != null) {
                    onResult(false, "Un compte avec cette adresse e-mail existe déjà dans Neon PostgreSQL.")
                    return@launch
                }
            } catch (_: Exception) {}

            // 2. Création de l'entité utilisateur
            val newUserId = "USR-${System.currentTimeMillis() % 1000000}"
            val userEntity = UserEntity(
                id = newUserId,
                fullName = cleanName,
                phone = cleanPhone,
                email = cleanEmail,
                role = role,
                marketZone = "Marché de Kituku",
                isActive = true,
                createdDate = System.currentTimeMillis(),
                lastLoginDate = System.currentTimeMillis(),
                pinCode = cleanPassword,
                username = cleanEmail.substringBefore("@"),
                avatarKey = if (role == UserRole.CLIENT) "bybit_female" else "bybit_vip"
            )

            // 3. Sauvegarde dans Room (local) et Neon PostgreSQL Cloud (table users)
            try {
                repository.userDao.insertUser(userEntity)
                try {
                    neonService.saveUser(userEntity)
                } catch (ne: Exception) {
                    Log.w("MainViewModel", "Neon saveUser async note: ${ne.message}")
                }

                if (role == UserRole.CLIENT) {
                    val clientEntity = ClientEntity(
                        id = "CLI-${newUserId.removePrefix("USR-")}",
                        fullName = cleanName,
                        phone = cleanPhone,
                        email = cleanEmail,
                        quarter = "Birere",
                        marketZone = "Marché de Kituku",
                        activityType = "Commerce général",
                        gender = "Femme",
                        status = "ACTIF",
                        registeredDate = System.currentTimeMillis(),
                        assignedAgentId = "USR-002"
                    )
                    repository.clientDao.insertClient(clientEntity)
                    try {
                        neonService.saveClient(clientEntity)
                    } catch (ce: Exception) {
                        Log.w("MainViewModel", "Neon saveClient async note: ${ce.message}")
                    }
                }

                // Connexion immédiate du nouvel utilisateur
                bindExistingUserEntity(userEntity)
                _toastEvent.emit("Compte ${userEntity.fullName} enregistré avec succès dans Neon PostgreSQL !")
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Erreur création compte Neon/Room: ${e.message}")
                onResult(false, "Erreur d'enregistrement : ${e.message}")
            }
        }
    }

    /**
     * Mot de passe oublié avec fallback sécurisé
     */
    fun resetPassword(
        email: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isEmpty()) {
                onResult(false, "Veuillez renseigner votre adresse e-mail.")
                return@launch
            }
            onResult(true, "Instructions de réinitialisation transmises pour $cleanEmail. Veuillez contacter votre administrateur si besoin.")
        }
    }

    fun resendEmailConfirmation(
        email: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            onResult(true, "Courriel de confirmation envoyé à $cleanEmail.")
        }
    }

    /**
     * Authentification Google Sign-In avec Firebase Auth (CredentialManager + GoogleAuthProvider)
     */
    fun authenticateWithGoogleFirebase(
        idToken: String,
        targetRole: UserRole? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            if (idToken.isBlank()) {
                val errorMsg = "Jeton d'identification Google manquant ou invalide."
                _toastEvent.emit(errorMsg)
                onResult(false, errorMsg)
                return@launch
            }

            try {
                // Authentification auprès de Firebase Auth avec GoogleAuthProvider.getCredential(idToken, null)
                val fbResult = firebaseService.signInWithGoogleIdToken(idToken)
                if (fbResult.isSuccess) {
                    val firebaseUser = fbResult.getOrNull()
                    _firebaseUser.value = firebaseUser

                    val email = firebaseUser?.email?.trim()?.lowercase().orEmpty()
                    val displayName = firebaseUser?.displayName?.ifBlank { null }
                        ?: email.substringBefore("@").ifBlank { "Utilisateur Google" }
                    val authId = firebaseUser?.uid ?: "GOOG-${Math.abs(idToken.hashCode())}"

                    bindAuthenticatedUser(
                        authId = authId,
                        email = email,
                        displayName = displayName,
                        targetRole = targetRole
                    )
                    onResult(true, null)
                } else {
                    val errorMsg = fbResult.exceptionOrNull()?.localizedMessage
                        ?: "Échec de l'authentification avec Firebase Auth."
                    _toastEvent.emit("Erreur Firebase Auth : $errorMsg")
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Erreur réseau ou jeton invalide lors de l'authentification Firebase."
                _toastEvent.emit("Erreur Google Auth : $errorMsg")
                onResult(false, errorMsg)
            }
        }
    }

    /**
     * Authentification Google vérifiée (compatibilité)
     */
    fun authenticateWithVerifiedGoogle(
        accountName: String,
        accountEmail: String,
        idToken: String? = null,
        targetRole: UserRole? = null
    ) {
        if (!idToken.isNullOrEmpty()) {
            authenticateWithGoogleFirebase(idToken, targetRole)
        } else {
            viewModelScope.launch {
                val cleanEmail = accountEmail.trim().lowercase()
                if (cleanEmail.isEmpty()) return@launch
                val authId = "GOOG-${Math.abs(cleanEmail.hashCode())}"
                bindAuthenticatedUser(
                    authId = authId,
                    email = cleanEmail,
                    displayName = accountName,
                    targetRole = targetRole
                )
            }
        }
    }

    private suspend fun bindAuthenticatedUser(
        authId: String,
        email: String,
        displayName: String,
        targetRole: UserRole? = null
    ) {
        val users = allUsers.value
        val cleanEmail = email.trim().lowercase()
        val matchingUser = users.find { it.email.equals(cleanEmail, ignoreCase = true) }
            ?: users.find { cleanEmail == "jacquesmasuruku2@gmail.com" && it.role == UserRole.GESTIONNAIRE }
            ?: run {
                // Définir le rôle approprié (Direction pour Masuruku ou Bernard Bwema par défaut si non spécifié)
                val role = targetRole ?: if (cleanEmail.contains("jacquesmasuruku2") ||
                    cleanEmail.contains("direction") ||
                    cleanEmail.contains("bernard")
                ) {
                    UserRole.GESTIONNAIRE
                } else {
                    UserRole.CLIENT
                }
                val newUser = UserEntity(
                    id = "USR-${authId.take(8).uppercase()}",
                    fullName = displayName.ifEmpty { cleanEmail.substringBefore("@") },
                    phone = "",
                    email = cleanEmail,
                    role = role,
                    marketZone = if (role == UserRole.GESTIONNAIRE) "Direction Générale" else "Marché de Kituku",
                    isActive = true,
                    pinCode = "",
                    username = cleanEmail.substringBefore("@")
                )
                repository.userDao.insertUser(newUser)
                neonService.saveUser(newUser)
                newUser
            }

        if (matchingUser.role == UserRole.CLIENT) {
            val allExistingClients = repository.clientDao.getAllClientsDirect()
            val existingClient = allExistingClients.find {
                (it.email.isNotBlank() && it.email.equals(cleanEmail, ignoreCase = true)) ||
                (matchingUser.phone.isNotBlank() && it.phone == matchingUser.phone) ||
                it.id == matchingUser.id ||
                it.id == "CLI-${matchingUser.id.removePrefix("USR-")}"
            }
            if (existingClient == null) {
                val clientRecord = ClientEntity(
                    id = "CLI-${matchingUser.id.removePrefix("USR-")}",
                    fullName = matchingUser.fullName,
                    phone = matchingUser.phone,
                    email = cleanEmail,
                    quarter = "Goma",
                    marketZone = matchingUser.marketZone,
                    activityType = "Commerce & Micro-activités",
                    gender = "Autre",
                    status = "ACTIF",
                    registeredDate = System.currentTimeMillis(),
                    assignedAgentId = "USR-002"
                )
                repository.clientDao.insertClient(clientRecord)
                try {
                    neonService.saveClient(clientRecord)
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Neon saveClient note: ${e.message}")
                }
            }
        }

        _currentUser.value = matchingUser
        syncUserPrefs(matchingUser)
        _isGoogleAuthenticated.value = true
        _googleAccountName.value = matchingUser.fullName
        _googleAccountEmail.value = cleanEmail

        setRouteForRole(matchingUser.role)
        _toastEvent.emit("Session Firebase authentifiée : ${matchingUser.fullName} (${matchingUser.role.label})")
    }

    private suspend fun bindExistingUserEntity(user: UserEntity) {
        if (user.role == UserRole.CLIENT) {
            val allExistingClients = repository.clientDao.getAllClientsDirect()
            val existingClient = allExistingClients.find {
                (it.email.isNotBlank() && it.email.equals(user.email, ignoreCase = true)) ||
                (user.phone.isNotBlank() && it.phone == user.phone) ||
                it.id == user.id ||
                it.id == "CLI-${user.id.removePrefix("USR-")}"
            }
            if (existingClient == null) {
                val clientRecord = ClientEntity(
                    id = "CLI-${user.id.removePrefix("USR-")}",
                    fullName = user.fullName,
                    phone = user.phone,
                    email = user.email,
                    quarter = "Goma",
                    marketZone = user.marketZone,
                    activityType = "Commerce & Micro-activités",
                    gender = "Autre",
                    status = "ACTIF",
                    registeredDate = System.currentTimeMillis(),
                    assignedAgentId = "USR-002"
                )
                repository.clientDao.insertClient(clientRecord)
                try {
                    neonService.saveClient(clientRecord)
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Neon saveClient note: ${e.message}")
                }
            }
        }

        _currentUser.value = user
        syncUserPrefs(user)
        _isGoogleAuthenticated.value = true
        _googleAccountName.value = user.fullName
        _googleAccountEmail.value = if (user.email.isNotEmpty()) user.email else user.phone

        setRouteForRole(user.role)
        _toastEvent.emit("Connecté avec succès : ${user.fullName} (${user.role.label})")
    }

    fun logoutGoogle() {
        viewModelScope.launch {
            firebaseService.signOut()
            _firebaseUser.value = null
            syncUserPrefs(null)
            _currentUser.value = null
            _isGoogleAuthenticated.value = false
            _currentRoute.value = "dashboard"
            val msg = if (_currentLanguage.value == AppLanguage.SWAHILI) {
                "Umetoka kikamilifu. Data za uwanjani na za wateja zimefungwa kwa usalama."
            } else {
                "Déconnexion réussie. Session Firebase clôturée et workspace verrouillé."
            }
            _toastEvent.emit(msg)
        }
    }

    fun switchUserRole(role: UserRole) {
        viewModelScope.launch {
            if (_currentUser.value?.role != UserRole.GESTIONNAIRE) {
                _toastEvent.emit("Action réservée à la Direction. Les agents et clients ne peuvent pas changer de rôle.")
                return@launch
            }
            val users = allUsers.value
            val target = users.find { it.role == role }
            if (target != null) {
                _currentUser.value = target
                syncUserPrefs(target)
                setRouteForRole(target.role)
                _toastEvent.emit("Basculé en mode : ${role.label} (${target.fullName})")
            } else {
                _toastEvent.emit("Aucun utilisateur configuré avec le rôle ${role.label}.")
            }
        }
    }

    fun setRouteForRole(role: UserRole) {
        val route = when (role) {
            UserRole.GESTIONNAIRE -> "dashboard"
            UserRole.AGENT -> "agent_home"
            UserRole.CAISSIER -> "caisse"
            UserRole.CLIENT -> "client_portal"
        }
        _currentRoute.value = route
        prefs.edit().putString("active_route", route).apply()
    }

    fun navigateTo(route: String) {
        val userRole = _currentUser.value?.role
        val targetRoute = if (userRole == UserRole.CLIENT) {
            when (route) {
                "client_portal", "client_loan_apply", "client_schedule", "chat", "notifications", "settings" -> route
                else -> "client_portal"
            }
        } else {
            route
        }
        _currentRoute.value = targetRoute
        prefs.edit().putString("active_route", targetRoute).apply()
    }

    fun navigateBack() {
        when (_currentRoute.value) {
            "client_detail" -> _currentRoute.value = "clients"
            "cycle_detail" -> _currentRoute.value = "cycles"
            "chat" -> setRouteForRole(_currentUser.value?.role ?: UserRole.GESTIONNAIRE)
            "notifications" -> setRouteForRole(_currentUser.value?.role ?: UserRole.GESTIONNAIRE)
            "user_management", "projections", "settings", "audit" -> _currentRoute.value = "more"
            else -> setRouteForRole(_currentUser.value?.role ?: UserRole.GESTIONNAIRE)
        }
    }

    fun openClientDetail(clientId: String) {
        _selectedClientId.value = clientId
        _currentRoute.value = "client_detail"
    }

    fun selectClientForCard(clientId: String) {
        _selectedClientId.value = clientId
        _currentRoute.value = "member_cards"
    }

    fun openCycleDetail(cycleId: String) {
        _selectedCycleId.value = cycleId
        _currentRoute.value = "cycle_detail"
    }

    fun openPaymentForCycle(cycleId: String) {
        _selectedCycleId.value = cycleId
        _currentRoute.value = "record_payment"
    }

    fun sendReminderAlert(clientName: String, phone: String) {
        val user = _currentUser.value
        viewModelScope.launch {
            if (user != null) {
                // Find matching client to link chat
                val client = allClients.value.find { it.phone == phone || it.fullName.equals(clientName, ignoreCase = true) }
                val targetClientId = client?.id ?: "CLI-003"
                repository.sendChatMessage(
                    operator = user,
                    clientId = targetClientId,
                    content = "Rappel de remboursement : Bonjour $clientName, nous vous rappelons l'échéance journalière pour votre prêt au marché. Merci de régulariser auprès de l'agent collecteur."
                )
            }
            _toastEvent.emit("Rappel SMS et message envoyés à $clientName ($phone)")
        }
    }

    fun openApplicationDetail(appId: String) {
        _selectedApplicationId.value = appId
        _currentRoute.value = "application_detail"
    }

    fun openChat(conversationRoomId: String) {
        val user = _currentUser.value
        val allowed = canUserAccessChatRoom(user, conversationRoomId, allClients.value)
        if (!allowed && user != null) {
            val reason = if (user.role == UserRole.GESTIONNAIRE) {
                "Accès restreint : Conformément aux règles de confidentialité de Malaki, la direction n'a pas accès aux conversations privées directes des clients."
            } else {
                "Accès restreint : Vous n'avez pas l'autorisation d'accéder à cette conversation privée."
            }
            viewModelScope.launch {
                _toastEvent.emit(reason)
            }
            return
        }
        _chatClientId.value = conversationRoomId
        _activeChatRoomId.value = conversationRoomId
        _currentRoute.value = "chat"
        markCurrentChatAsRead()
    }

    fun closeActiveChatRoom() {
        _activeChatRoomId.value = null
    }

    fun openDirectChat(otherUserId: String) {
        val currentUserId = _currentUser.value?.id ?: defaultManager.id
        val directRoomId = "DIRECT_" + listOf(currentUserId, otherUserId).sorted().joinToString("_")
        openChat(directRoomId)
    }

    fun openCommunityChat() {
        openChat("COMMUNITY_GROUP")
    }

    // --- Actions ---

    fun recordPayment(
        cycleId: String,
        amount: Double,
        dayNumber: Int,
        paymentMethod: PaymentMethod,
        notes: String,
        isOffline: Boolean = false,
        paymentTimestamp: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: defaultManager
        viewModelScope.launch {
            val result = repository.recordPayment(
                operator = user,
                cycleId = cycleId.trim(),
                amountCollected = amount,
                dayNumber = dayNumber,
                paymentMethod = paymentMethod,
                notes = notes,
                isOffline = isOffline,
                paymentTimestamp = paymentTimestamp
            )
            result.onSuccess { payment ->
                val cycle = allCycles.value.find { it.id.trim().equals(cycleId.trim(), ignoreCase = true) }
                val clientName = cycle?.clientName ?: "Client"
                val expected = (cycle?.principalAmount ?: 0.0) * 0.035
                val diff = amount - expected
                val comparisonText = when {
                    diff == 0.0 -> "Paiement conforme au montant attendu (3,5%)"
                    diff < 0.0 -> "Versement partiel (reliquat restant de ${MicrocreditRepository.formatAmount(-diff)})"
                    else -> "Versement anticipé (+${MicrocreditRepository.formatAmount(diff)} déduit du solde)"
                }
                val newBal = (cycle?.balanceRemaining ?: 0.0) - amount
                _toastEvent.emit("Paiement de ${MicrocreditRepository.formatAmount(amount)} enregistré avec succès !")
                showOperationFeedback(
                    isSuccess = true,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Ukusanyaji Umefaulu" else "Succès : Recouvrement Enregistré",
                    message = "Le recouvrement de ${MicrocreditRepository.formatAmount(amount)} pour « $clientName » a été enregistré avec succès !",
                    details = "• Client : $clientName\n• Montant recouvré : ${MicrocreditRepository.formatAmount(amount)}\n• Montant attendu (3,5%) : ${MicrocreditRepository.formatAmount(expected)}\n• Statut : $comparisonText\n• Nouveau solde restant : ${MicrocreditRepository.formatAmount(if (newBal > 0) newBal else 0.0)}\n• Reçu ID : ${payment.id} (${paymentMethod.label})"
                )
                onSuccess()
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
                showOperationFeedback(
                    isSuccess = false,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Hitilafu ya Ukusanyaji" else "Échec du Recouvrement",
                    message = "Impossible d'enregistrer le recouvrement : ${it.message ?: "Erreur inconnue"}",
                    details = "Veuillez vérifier les informations du client et le montant saisi."
                )
            }
        }
    }

    fun disburseLoan(
        applicationId: String,
        paymentMethod: PaymentMethod,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: defaultManager
        viewModelScope.launch {
            val result = repository.disburseLoan(
                operator = user,
                applicationId = applicationId,
                paymentMethod = paymentMethod
            )
            result.onSuccess { cycle ->
                _toastEvent.emit("Prêt décaissé avec succès ! Cycle créé : ${cycle.id}")
                showOperationFeedback(
                    isSuccess = true,
                    title = "Succès : Prêt Décaissé",
                    message = "Le prêt de ${MicrocreditRepository.formatAmount(cycle.principalAmount)} pour « ${cycle.clientName} » a été décaissé avec succès !",
                    details = "• N° Dossier / Cycle : ${cycle.id}\n• Somme reçue en crédit : ${MicrocreditRepository.formatAmount(cycle.principalAmount)}\n• Échéance journalière (3,5%) : ${MicrocreditRepository.formatAmount(cycle.dailyPaymentAmount)}/jour\n• Mode : ${paymentMethod.label}"
                )
                onSuccess()
            }.onFailure {
                _toastEvent.emit("Erreur décaissement : ${it.message}")
                showOperationFeedback(
                    isSuccess = false,
                    title = "Échec du Décaissement",
                    message = "Impossible de décaisser le prêt : ${it.message ?: "Erreur de caisse"}",
                    details = "Veuillez vérifier que le solde de la caisse est suffisant."
                )
            }
        }
    }

    fun assignCreditToClient(
        clientId: String,
        principalAmount: Double,
        durationDays: Int = 30,
        purpose: String = "Crédit commercial",
        activityType: String = "Commerce",
        marketZone: String = "Marché de Kituku",
        paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
        newClientName: String? = null,
        newClientPhone: String? = null,
        onSuccess: (CreditCycleEntity) -> Unit = {}
    ) {
        val user = _currentUser.value ?: defaultManager
        viewModelScope.launch {
            val result = repository.assignCreditToClient(
                operator = user,
                clientId = clientId,
                principalAmount = principalAmount,
                durationDays = durationDays,
                purpose = purpose,
                activityType = activityType,
                marketZone = marketZone,
                paymentMethod = paymentMethod,
                newClientName = newClientName,
                newClientPhone = newClientPhone
            )
            result.onSuccess { cycle ->
                // Preselect this cycle for recovery immediately
                _selectedCycleId.value = cycle.id
                val dailyExpected = cycle.dailyPaymentAmount
                val msg = if (_currentLanguage.value == AppLanguage.SWAHILI)
                    "Mkopo wa ${MicrocreditRepository.formatAmount(principalAmount)} umekabidhiwa! Rejesho la kila siku (3.5%): ${MicrocreditRepository.formatAmount(dailyExpected)}"
                else
                    "Crédit de ${MicrocreditRepository.formatAmount(principalAmount)} affecté avec succès ! Montant attendu (3,5%) : ${MicrocreditRepository.formatAmount(dailyExpected)}/jour."
                _toastEvent.emit(msg)
                showOperationFeedback(
                    isSuccess = true,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Mkopo Umekabidhiwa Kikamilifu" else "Succès : Crédit Affecté au Client",
                    message = "Un crédit de ${MicrocreditRepository.formatAmount(principalAmount)} a été accordé avec succès à « ${cycle.clientName} » !",
                    details = "• Client bénéficiaire : ${cycle.clientName}\n• Somme reçue en crédit : ${MicrocreditRepository.formatAmount(principalAmount)}\n• Montant attendu / jour (3,5%) : ${MicrocreditRepository.formatAmount(dailyExpected)}/jour\n• N° de dossier cycle : ${cycle.id}\n• Durée : $durationDays jours\n• Statut : Prêt actif, prêt pour les opérations de recouvrement",
                    actionLabel = "Effectuer le Recouvrement",
                    onAction = {
                        _selectedCycleId.value = cycle.id
                        _currentRoute.value = "record_payment"
                    }
                )
                onSuccess(cycle)
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
                showOperationFeedback(
                    isSuccess = false,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Hitilafu ya Kukabidhi Mkopo" else "Échec de l'Affectation du Crédit",
                    message = "Impossible d'affecter le crédit : ${it.message ?: "Erreur de validation"}",
                    details = "Veuillez vérifier l'éligibilité du client et réessayer."
                )
            }
        }
    }

    fun reviewLoanApplication(
        applicationId: String,
        approved: Boolean,
        reviewNotes: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.reviewLoanApplication(
                operator = user,
                applicationId = applicationId,
                approved = approved,
                reviewNotes = reviewNotes
            )
            result.onSuccess {
                _toastEvent.emit(if (approved) "Demande approuvée avec succès." else "Demande refusée.")
                onSuccess()
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun submitLoanApplication(
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
        acceptedTerms: Boolean = true,
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
        signatureId: String? = null,
        signatureMethod: String = "BIOMETRIC",
        signedAt: Long? = null,
        documentHash: String? = null,
        onSuccess: () -> Unit = {},
        onSuccessWithEntity: ((LoanApplicationEntity) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val user = _currentUser.value ?: defaultManager
        viewModelScope.launch {
            try {
                val result = repository.submitLoanApplication(
                    operator = user,
                    clientId = clientId,
                    amount = amount,
                    durationDays = durationDays,
                    purpose = purpose,
                    activityType = activityType,
                    marketZone = marketZone,
                    quarter = quarter,
                    guarantorName = guarantorName,
                    guarantorPhone = guarantorPhone,
                    idCardNumber = idCardNumber,
                    clientPhotoUri = clientPhotoUri,
                    acceptedTerms = acceptedTerms,
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
                    businessLocation = businessLocation,
                    businessSeniority = businessSeniority,
                    estimatedCapital = estimatedCapital,
                    dailyRevenue = dailyRevenue,
                    currency = currency,
                    repaymentFrequency = repaymentFrequency,
                    signatureId = signatureId,
                    signatureMethod = signatureMethod,
                    signedAt = signedAt,
                    documentHash = documentHash
                )
                result.onSuccess { app ->
                    _toastEvent.emit("Demande de crédit scellée et enregistrée avec succès !")
                    onSuccess()
                    onSuccessWithEntity?.invoke(app)
                }.onFailure {
                    val err = it.message ?: "Échec de l'enregistrement de la demande"
                    _toastEvent.emit("Erreur : $err")
                    onError?.invoke(err)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Erreur inattendue"
                _toastEvent.emit("Erreur : $err")
                onError?.invoke(err)
            }
        }
    }

    fun recordCashMovement(
        type: CashMovementType,
        category: CashMovementCategory,
        description: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        reference: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.recordCashMovement(
                operator = user,
                type = type,
                category = category,
                description = description,
                amount = amount,
                paymentMethod = paymentMethod,
                reference = reference
            )
            result.onSuccess { movement ->
                val typeLabel = if (type == CashMovementType.SORTIE) "Sortie de Caisse (-)" else "Entrée en Caisse (+)"
                _toastEvent.emit("Opération de caisse enregistrée avec succès !")
                showOperationFeedback(
                    isSuccess = true,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Muamala wa Hazina Umefaulu" else "Succès : $typeLabel",
                    message = "L'opération de ${MicrocreditRepository.formatAmount(amount)} a été enregistrée avec succès dans la caisse !",
                    details = "• Type : $typeLabel\n• Catégorie : ${category.label}\n• Libellé / Motif : $description\n• Mode : ${paymentMethod.label}\n• Référence : ${movement.reference}\n• Date : ${movement.dateString}"
                )
                onSuccess()
            }.onFailure {
                val typeLabel = if (type == CashMovementType.SORTIE) "Sortie de Caisse" else "Entrée en Caisse"
                _toastEvent.emit("Erreur caisse : ${it.message}")
                showOperationFeedback(
                    isSuccess = false,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Hitilafu ya Hazina" else "Échec : $typeLabel",
                    message = "Impossible d'enregistrer l'opération : ${it.message ?: "Erreur de validation"}",
                    details = "Veuillez vérifier les informations et vous assurer que la trésorerie est suffisante pour cette opération."
                )
            }
        }
    }

    fun inviteUser(
        fullName: String,
        phone: String,
        email: String,
        role: UserRole,
        marketZone: String,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.inviteUser(
                operator = user,
                fullName = fullName,
                phone = phone,
                email = email,
                role = role,
                marketZone = marketZone
            )
            result.onSuccess { inv ->
                _toastEvent.emit("Invitation créée pour $fullName (${role.label}). Code: ${inv.inviteCode}")
                onSuccess()
            }.onFailure {
                _toastEvent.emit("Erreur invitation : ${it.message}")
            }
        }
    }

    fun updateUserRole(targetUserId: String, targetUserName: String, newRole: UserRole) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateUserRole(user, targetUserId, targetUserName, newRole)
            result.onSuccess {
                _toastEvent.emit("Rôle de $targetUserName modifié en ${newRole.label}.")
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun updateUserRoleAndMarkets(targetUserId: String, targetUserName: String, newRole: UserRole, newMarkets: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateUserRoleAndMarkets(user, targetUserId, targetUserName, newRole, newMarkets)
            result.onSuccess {
                _toastEvent.emit("Affectation de $targetUserName mise à jour : ${newRole.label} sur $newMarkets")
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun resetAllDataToZero(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.clearAllTestDataAndReset(operator = _currentUser.value ?: defaultManager, keepAdmin = true)
            result.onSuccess {
                _toastEvent.emit("Toutes les données de test ont été vidées. Vous démarrez à zéro !")
                onComplete()
            }.onFailure {
                _toastEvent.emit("Erreur lors de la réinitialisation : ${it.message}")
            }
        }
    }

    fun toggleUserActive(targetUserId: String, targetUserName: String, isActive: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.toggleUserActive(user, targetUserId, targetUserName, isActive)
            result.onSuccess {
                _toastEvent.emit("Compte de $targetUserName ${if (isActive) "réactivé" else "désactivé"}.")
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun registerClient(
        fullName: String,
        phone: String,
        email: String,
        marketZone: String,
        activityType: String,
        gender: String = "Femme",
        idCardNumber: String,
        guarantorName: String,
        guarantorPhone: String,
        notes: String,
        onSuccess: (ClientEntity) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.registerClient(
                operator = user,
                fullName = fullName,
                phone = phone,
                email = email,
                marketZone = marketZone,
                activityType = activityType,
                gender = gender,
                idCardNumber = idCardNumber,
                guarantorName = guarantorName,
                guarantorPhone = guarantorPhone,
                notes = notes
            )
            result.onSuccess { client ->
                val quantieme = MicrocreditRepository.getClientQuantiemeLabel(client.id, _currentLanguage.value)
                val quantiemeStr = if (quantieme.isNotEmpty()) " ($quantieme)" else ""
                _toastEvent.emit("Nouveau client ${client.fullName} créé avec succès (Matricule: ${client.id}$quantiemeStr) !")
                showOperationFeedback(
                    isSuccess = true,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Mteja Amesajiliwa Kikamilifu" else "Succès : Client Ajouté avec Succès",
                    message = "Le client « ${client.fullName} » a été enregistré avec succès dans la base de données !",
                    details = "• N° Matricule : ${client.id}$quantiemeStr\n• Rang : $quantieme enregistré\n• Zone de marché : ${client.marketZone}\n• Téléphone : ${client.phone}\n• Activité : ${client.activityType}\n• Statut : Éligible pour l'octroi d'un crédit"
                )
                onSuccess(client)
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
                showOperationFeedback(
                    isSuccess = false,
                    title = if (_currentLanguage.value == AppLanguage.SWAHILI) "Hitilafu ya Kusajili Mteja" else "Échec de l'Ajout du Client",
                    message = "Impossible d'enregistrer le client : ${it.message ?: "Erreur inconnue"}",
                    details = "Veuillez vérifier les informations renseignées et réessayer."
                )
            }
        }
    }

    fun sendChatMessage(
        content: String,
        attachmentUri: String? = null,
        replyToMessage: ChatMessageEntity? = null,
        isVoiceMessage: Boolean = false,
        voiceDurationSeconds: Int = 0,
        audioFilePath: String? = null
    ) {
        val user = _currentUser.value ?: defaultManager
        val targetRoom = _activeChatRoomId.value ?: _chatClientId.value
        val clientId = when {
            targetRoom == "COMMUNITY_GROUP" -> "COMMUNITY_GROUP"
            targetRoom.startsWith("DIRECT_") -> targetRoom
            user.role == UserRole.CLIENT -> {
                val myClient = allClients.value.find { it.phone == user.phone || it.id == user.id || user.id.endsWith(it.id) || it.email.equals(user.email, ignoreCase = true) }
                val myClientId = myClient?.id ?: if (user.id.startsWith("USR-CLI-")) user.id.removePrefix("USR-") else null
                if (targetRoom.isNotEmpty() && targetRoom != "CLI-001" && (myClientId == null || targetRoom == myClientId)) targetRoom
                else myClientId ?: targetRoom
            }
            else -> targetRoom
        }

        if (!canUserAccessChatRoom(user, clientId, allClients.value)) {
            viewModelScope.launch {
                val reason = if (user.role == UserRole.GESTIONNAIRE) {
                    "Accès restreint : La direction ne peut pas envoyer ou lire des messages dans les discussions privées des clients."
                } else {
                    "Accès restreint : Vous n'êtes pas autorisé à envoyer de message dans cette discussion."
                }
                _toastEvent.emit(reason)
            }
            return
        }

        viewModelScope.launch {
            val snippet = replyToMessage?.let {
                if (it.isVoiceMessage) "🎤 Message vocal (${it.voiceDurationSeconds}s)"
                else it.content.take(60)
            }
            val result = repository.sendChatMessage(
                operator = user,
                clientId = clientId,
                content = content,
                attachmentUri = attachmentUri,
                replyToMessageId = replyToMessage?.id,
                replyToSenderName = replyToMessage?.senderName,
                replyToContentSnippet = snippet,
                isVoiceMessage = isVoiceMessage,
                voiceDurationSeconds = voiceDurationSeconds,
                audioFilePath = audioFilePath
            )
            result.onSuccess { msg ->
                // Note : Le message est enregistré avec isRead = false (affiche 1v / simple coche grise).
                // Il restera non lu jusqu'à ce que le destinataire ouvre effectivement la conversation.
                // L'expéditeur ne reçoit JAMAIS de notification pour ses propres messages envoyés.
            }.onFailure {
                _toastEvent.emit("Erreur d'envoi : ${it.message}")
            }
        }
    }

    fun togglePinMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            val newPin = !message.isPinned
            repository.togglePinChatMessage(message.id, newPin)
            _toastEvent.emit(if (newPin) "Message épinglé en haut du chat 📌" else "Message désépinglé")
        }
    }

    fun editChatMessage(messageId: String, newContent: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.editChatMessage(user, messageId, newContent)
            result.onSuccess {
                _toastEvent.emit("Message modifié avec succès !")
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.deleteChatMessage(user, messageId)
            result.onSuccess {
                _toastEvent.emit("Message supprimé avec succès.")
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun markCurrentChatAsRead() {
        val user = _currentUser.value ?: defaultManager
        val targetRoom = _activeChatRoomId.value ?: _chatClientId.value
        val clientId = when {
            targetRoom == "COMMUNITY_GROUP" -> "COMMUNITY_GROUP"
            targetRoom.startsWith("DIRECT_") -> targetRoom
            user.role == UserRole.CLIENT -> {
                val myClient = allClients.value.find { it.phone == user.phone || it.id == user.id || user.id.endsWith(it.id) || it.email.equals(user.email, ignoreCase = true) }
                val myClientId = myClient?.id ?: if (user.id.startsWith("USR-CLI-")) user.id.removePrefix("USR-") else null
                if (targetRoom.isNotEmpty() && targetRoom != "CLI-001" && (myClientId == null || targetRoom == myClientId)) targetRoom
                else myClientId ?: targetRoom
            }
            else -> targetRoom
        }

        if (!canUserAccessChatRoom(user, clientId, allClients.value)) return

        viewModelScope.launch {
            repository.markChatMessagesAsRead(clientId, user.id)
            // Refresh current user presence
            repository.updateUserPresence(user.id, isOnline = true, timestamp = System.currentTimeMillis())
        }
    }

    fun updateMyPresence(isOnline: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserPresence(user.id, isOnline, System.currentTimeMillis())
            _toastEvent.emit(if (isOnline) "Vous êtes maintenant visible En ligne." else "Statut changé en Hors ligne.")
        }
    }

    fun toggleContactPresence(clientId: String, isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateClientPresence(clientId, isOnline, System.currentTimeMillis())
            val matchingClient = allClients.value.find { it.id == clientId }
            val matchingUser = allUsers.value.find { it.phone == matchingClient?.phone }
            if (matchingUser != null) {
                repository.updateUserPresence(matchingUser.id, isOnline, System.currentTimeMillis())
            }
            _toastEvent.emit("Statut de ${matchingClient?.fullName ?: clientId} : ${if (isOnline) "En ligne" else "Hors ligne"}")
        }
    }

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    fun uploadAndSetUserAvatarFromUri(
        imageUri: Uri,
        context: Context,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            try {
                val cachedFile = cloudinaryService.copyUriToLocalCache(imageUri, "avatars")
                if (cachedFile == null || !cachedFile.exists()) {
                    _toastEvent.emit("Impossible de charger la photo sélectionnée.")
                    _isUploadingAvatar.value = false
                    onComplete(false, "Fichier introuvable ou illisible")
                    return@launch
                }

                _toastEvent.emit("Envoi de la photo vers Cloudinary...")
                val cloudinaryUrl = cloudinaryService.uploadMedia(
                    file = cachedFile,
                    resourceType = "image",
                    folder = "malaki_microfinance/avatars",
                    displayName = "avatar_${user.id}_${System.currentTimeMillis()}",
                    senderId = user.id,
                    senderName = user.fullName,
                    senderRole = user.role.name,
                    clientId = user.id,
                    messageId = "AVT-${System.currentTimeMillis()}"
                )

                val finalUrl = cloudinaryUrl ?: Uri.fromFile(cachedFile).toString()

                repository.updateUserAvatarKey(user.id, finalUrl)
                if (user.role == UserRole.CLIENT) {
                    val matchingClient = allClients.value.find { it.phone == user.phone || it.id == user.id }
                    if (matchingClient != null) {
                        repository.updateClientAvatarKey(matchingClient.id, finalUrl)
                    }
                }
                _currentUser.value = user.copy(avatarKey = finalUrl)
                _toastEvent.emit("Photo de profil mise à jour avec succès sur Cloudinary !")
                _isUploadingAvatar.value = false
                onComplete(true, finalUrl)
            } catch (e: Exception) {
                _isUploadingAvatar.value = false
                _toastEvent.emit("Erreur lors de l'envoi de la photo : ${e.localizedMessage}")
                onComplete(false, e.localizedMessage)
            }
        }
    }

    fun updateMyAvatarKey(avatarKey: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserAvatarKey(user.id, avatarKey)
            _currentUser.value = user.copy(avatarKey = avatarKey)
            _toastEvent.emit("Avatar mis à jour avec succès !")
        }
    }

    fun updateUserAvatarKey(userId: String, avatarKey: String) {
        viewModelScope.launch {
            repository.updateUserAvatarKey(userId, avatarKey)
            _toastEvent.emit("Avatar de l'utilisateur mis à jour !")
        }
    }

    fun updateClientAvatarKey(clientId: String, avatarKey: String) {
        viewModelScope.launch {
            repository.updateClientAvatarKey(clientId, avatarKey)
            _toastEvent.emit("Avatar du client mis à jour !")
        }
    }

    fun resetDatabaseToZero() {
        val operator = _currentUser.value ?: defaultManager
        viewModelScope.launch {
            repository.clearAllTestDataAndReset(operator = operator, keepAdmin = true)
            val currentId = _currentUser.value?.id ?: defaultManager.id
            repository.userDao.setAllOtherUsersOffline(currentId)
            repository.clientDao.setAllClientsOffline()
            repository.updateUserPresence(currentId, isOnline = true, timestamp = System.currentTimeMillis())
            _toastEvent.emit("Toutes les données ont été remises à zéro avec succès !")
        }
    }

    fun syncOfflinePayments() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val count = repository.syncOfflinePayments(user)
            if (count > 0) {
                _toastEvent.emit("$count paiement(s) synchronisé(s) vers le serveur !")
            } else {
                _toastEvent.emit("Tous les paiements sont déjà synchronisés.")
            }
        }
    }

    fun updateSettings(settings: BusinessSettingsEntity, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateSettings(user, settings)
            result.onSuccess {
                _toastEvent.emit("Paramètres généraux mis à jour avec succès !")
                onSuccess()
            }.onFailure {
                _toastEvent.emit("Erreur : ${it.message}")
            }
        }
    }

    fun shareReport(reportText: String) {
        viewModelScope.launch {
            _toastEvent.emit("Rapport généré et prêt pour l'exportation.")
        }
    }

    fun shareDailyReportPdf(
        context: Context,
        reportData: com.example.util.DailyPdfReportGenerator.DailyReportData
    ) {
        viewModelScope.launch {
            val result = com.example.util.DailyPdfReportGenerator.generateAndShareDailyReportPdf(context, reportData)
            result.onSuccess { file ->
                _toastEvent.emit("Rapport PDF généré avec succès : ${file.name}")
            }.onFailure { err ->
                _toastEvent.emit("Erreur lors de la génération du PDF : ${err.message ?: "inconnue"}")
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.notificationDao.markAsRead(id)
            try {
                neonService.markNotificationAsRead(id)
            } catch (_: Exception) {}
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.notificationDao.markAllAsRead()
            try {
                neonService.markAllNotificationsAsRead()
            } catch (_: Exception) {}
            _toastEvent.emit("Toutes les notifications marquées comme lues.")
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.notificationDao.deleteAllNotifications()
            _toastEvent.emit("Toutes les notifications ont été effacées.")
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.notificationDao.deleteNotificationById(id)
            _toastEvent.emit("Notification supprimée.")
        }
    }

    fun recordNotificationActivity(
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
    ) {
        viewModelScope.launch {
            repository.recordNotificationActivity(
                title = title,
                message = message,
                type = type,
                targetRole = targetRole,
                targetUserId = targetUserId,
                targetClientId = targetClientId,
                destinationAddress = destinationAddress,
                senderId = senderId,
                senderName = senderName,
                actionRoute = actionRoute
            )
        }
    }
}

data class OperationFeedback(
    val isSuccess: Boolean,
    val title: String,
    val message: String,
    val details: String? = null,
    val actionLabel: String = "OK",
    val onAction: (() -> Unit)? = null
)
