package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ui.components.BybitAvatarPickerDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.XenderDrawerContent
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.Localization
import com.example.util.NotificationHelper
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTopBar
import com.example.ui.screens.agent.AgentHomeScreen
import com.example.ui.screens.auth.AppLockScreen
import com.example.ui.screens.auth.GoogleAuthScreen
import com.example.ui.screens.caisse.CaisseScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.client_portal.ClientHomeScreen
import com.example.ui.screens.client_portal.ClientProfileScreen
import com.example.ui.screens.client_portal.ClientScheduleScreen
import com.example.ui.screens.clients.ClientDetailScreen
import com.example.ui.screens.clients.ClientsScreen
import com.example.ui.screens.loans.CreditCycleDetailScreen
import com.example.ui.screens.loans.CreditCyclesScreen
import com.example.ui.screens.loans.LoanApplicationsScreen
import com.example.ui.screens.manager.AuditScreen
import com.example.ui.screens.manager.FinancialProjectionScreen
import com.example.ui.screens.manager.ManagerDashboardScreen
import com.example.ui.screens.manager.MemberCardProductionScreen
import com.example.ui.screens.manager.MoreMenuScreen
import com.example.ui.screens.manager.SettingsScreen
import com.example.ui.screens.manager.UserManagementScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.payments.RecordPaymentScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize sound and vibration notification channels
        NotificationHelper.init(this)
        com.example.receiver.NotificationAlarmReceiver.schedulePeriodicChecks(this)

        // Handle navigation or direct notification click
        handleIncomingIntent(intent)

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (isDarkModePref) {
                null -> systemDark
                true -> true
                false -> false
            }

            // Notification permission request for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle result */ }
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val isGoogleAuthenticated by viewModel.isGoogleAuthenticated.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
            val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val appFontStyle by viewModel.appFontStyle.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = effectiveDark, fontStyle = appFontStyle) {
                CompositionLocalProvider(LocalAppLanguage provides currentLang) {
                    if (!isGoogleAuthenticated) {
                        GoogleAuthScreen(viewModel = viewModel)
                    } else if (isAppLocked) {
                        AppLockScreen(viewModel = viewModel)
                    } else {
                        MainAppContent(viewModel = viewModel, isDarkMode = effectiveDark)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent ?: return

        // Détection de navigation par notification
        val targetRoute = intent.getStringExtra("target_route") ?: intent.getStringExtra("route")
        val clientId = intent.getStringExtra("client_id")
        if (!targetRoute.isNullOrEmpty()) {
            if (targetRoute.startsWith("chat:")) {
                val room = targetRoute.removePrefix("chat:")
                viewModel.openChat(room)
            } else if (targetRoute == "chat") {
                if (!clientId.isNullOrEmpty()) {
                    viewModel.openChat(clientId)
                } else {
                    viewModel.openCommunityChat()
                }
            } else {
                viewModel.navigateTo(targetRoute)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel, isDarkMode: Boolean) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRoute by viewModel.currentRoute.collectAsStateWithLifecycle()
    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val pendingSync by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleAccountEmail.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val operationFeedback by viewModel.operationFeedback.collectAsStateWithLifecycle()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    var showDrawerAvatarPicker by remember { mutableStateOf(false) }

    val unreadNotifs = notifications.count { !it.isRead }

    // Sound and vibration when a new targeted notification arrives exclusively for this user
    var knownNotificationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var activeInAppNotificationPopup by remember { mutableStateOf<com.example.data.model.AppNotificationEntity?>(null) }

    LaunchedEffect(notifications) {
        val currentIds = notifications.map { it.id }.toSet()
        if (isInitialLoad) {
            knownNotificationIds = currentIds
            isInitialLoad = false
            return@LaunchedEffect
        }

        val newNotifications = notifications.filter { it.id !in knownNotificationIds && !it.isRead }
        knownNotificationIds = knownNotificationIds + currentIds

        val user = currentUser
        if (user != null && newNotifications.isNotEmpty()) {
            for (newNotif in newNotifications) {
                // S'assurer de manière absolue que l'émetteur ne reçoit JAMAIS sa propre notification
                val isSender = newNotif.senderId?.equals(user.id, ignoreCase = true) == true ||
                        newNotif.senderId?.removePrefix("USR-")?.equals(user.id.removePrefix("USR-"), ignoreCase = true) == true ||
                        newNotif.senderId?.equals("USR-${user.id}", ignoreCase = true) == true ||
                        (!newNotif.senderName.isNullOrBlank() && user.fullName.isNotBlank() && newNotif.senderName!!.trim().equals(user.fullName.trim(), ignoreCase = true)) ||
                        (user.email.isNotEmpty() && (newNotif.senderId?.equals(user.email, ignoreCase = true) == true || newNotif.senderName?.equals(user.email, ignoreCase = true) == true)) ||
                        (user.phone.isNotEmpty() && (newNotif.senderId?.contains(user.phone) == true || newNotif.senderName?.contains(user.phone) == true)) ||
                        (user.fullName.isNotBlank() && (newNotif.title.contains("Message de ${user.fullName}", ignoreCase = true) || newNotif.title.contains("par ${user.fullName}", ignoreCase = true)))

                if (!isSender && com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(newNotif, user, allClients)) {
                    val isChat = newNotif.type == com.example.data.model.NotificationType.MESSAGE ||
                            newNotif.title.contains("message", ignoreCase = true) ||
                            newNotif.title.contains("ujumbe", ignoreCase = true)

                    val chatRoom = newNotif.actionRoute?.removePrefix("chat:")?.takeIf { it.isNotBlank() && it != "chat" }
                        ?: newNotif.targetClientId
                        ?: if (newNotif.title.contains("Communauté", ignoreCase = true)) "COMMUNITY_GROUP" else null

                    // 1. Notification système Android (avec vibration, son et bref aperçu si long)
                    NotificationHelper.postSystemNotification(
                        context = context,
                        title = newNotif.title,
                        message = newNotif.message,
                        notificationId = newNotif.id.hashCode(),
                        targetRoute = if (isChat) (newNotif.actionRoute ?: "chat") else (newNotif.actionRoute ?: "notifications"),
                        clientId = chatRoom
                    )

                    // 2. Popup de notification dans l'application avec bref aperçu et clic direct
                    activeInAppNotificationPopup = newNotif
                }
            }
        }
    }

    // Role protection: client user should NEVER see caisse, reports, clients management or manager dashboard
    LaunchedEffect(currentUser?.role, currentRoute) {
        if (currentUser?.role == UserRole.CLIENT) {
            val forbiddenRoutes = listOf(
                "dashboard", "clients", "client_detail", "cycles", "cycle_detail",
                "caisse", "caisse_entree", "caisse_sortie", "reports",
                "user_management", "projections", "audit", "member_cards",
                "record_payment", "agent_home", "more", "applications", "settings"
            )
            if (currentRoute in forbiddenRoutes) {
                viewModel.navigateTo("client_portal")
            }
        }
    }

    // Toast event listener
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Determine TopBar Title and whether BackButton should be shown
    val (title, showBack) = when (currentRoute) {
        "dashboard" -> Localization.tr("title_dashboard", currentLang) to false
        "clients" -> Localization.tr("title_clients", currentLang) to false
        "client_detail" -> Localization.tr("title_client_detail", currentLang) to true
        "cycles" -> Localization.tr("title_cycles", currentLang) to false
        "cycle_detail" -> Localization.tr("title_cycle_detail", currentLang) to true
        "applications" -> Localization.tr("title_applications", currentLang) to true
        "record_payment" -> Localization.tr("title_record_payment", currentLang) to false
        "caisse", "caisse_entree", "caisse_sortie" -> Localization.tr("title_caisse", currentLang) to false
        "reports" -> Localization.tr("title_reports", currentLang) to false
        "client_portal" -> Localization.tr("title_client_portal", currentLang) to false
        "client_profile" -> (if (currentLang == AppLanguage.SWAHILI) "Wasifu & Salio Langu" else "Mon Profil & Solde") to true
        "client_schedule" -> Localization.tr("title_client_schedule", currentLang) to false
        "client_loan_apply" -> Localization.tr("title_client_loan_apply", currentLang) to false
        "chat" -> Localization.tr("title_chat", currentLang) to true
        "notifications" -> Localization.tr("title_notifications", currentLang) to true
        "user_management" -> Localization.tr("title_user_management", currentLang) to true
        "projections" -> Localization.tr("title_projections", currentLang) to true
        "settings" -> Localization.tr("title_settings", currentLang) to true
        "audit" -> Localization.tr("title_audit", currentLang) to true
        "more" -> Localization.tr("title_more", currentLang) to false
        "agent_home" -> Localization.tr("title_agent_home", currentLang) to false
        else -> "MALISHO NA AKIBA (MALAKI)" to false
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Close drawer first on back press if open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    // Central BackHandler
    BackHandler(enabled = showBack && drawerState.isClosed) {
        when (currentRoute) {
            "client_detail" -> viewModel.navigateTo("clients")
            "cycle_detail" -> viewModel.navigateTo("cycles")
            "client_profile" -> viewModel.navigateTo("client_portal")
            "application_detail", "applications" -> viewModel.navigateTo(if (currentUser?.role == UserRole.GESTIONNAIRE) "dashboard" else "more")
            "chat" -> viewModel.setRouteForRole(currentUser?.role ?: UserRole.GESTIONNAIRE)
            "notifications" -> viewModel.setRouteForRole(currentUser?.role ?: UserRole.GESTIONNAIRE)
            "user_management", "projections", "settings", "audit" -> viewModel.navigateTo("more")
            else -> viewModel.setRouteForRole(currentUser?.role ?: UserRole.GESTIONNAIRE)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp)
            ) {
                XenderDrawerContent(
                    currentUser = currentUser,
                    googleAccountEmail = googleEmail,
                    currentLang = currentLang,
                    currentRoute = currentRoute,
                    isDarkMode = isDarkMode,
                    unreadNotificationsCount = unreadNotifs,
                    onNavigate = { route ->
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(route)
                    },
                    onSettingsClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo("settings")
                    },
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onLockClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.lockApp()
                    },
                    onLogout = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logoutGoogle()
                    },
                    onAvatarClick = {
                        coroutineScope.launch { drawerState.close() }
                        showDrawerAvatarPicker = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != "chat" && currentRoute != "client_loan_apply" && currentRoute != "notifications") {
                    AppTopBar(
                        title = title,
                        currentUser = currentUser,
                        googleAccountEmail = googleEmail,
                        unreadNotificationsCount = unreadNotifs,
                        pendingSyncCount = pendingSync,
                        isDarkMode = isDarkMode,
                        showBackButton = showBack,
                        currentLanguage = currentLang,
                        onBackClick = {
                            when (currentRoute) {
                                "client_detail" -> viewModel.navigateTo("clients")
                                "cycle_detail" -> viewModel.navigateTo("cycles")
                                "applications" -> viewModel.navigateTo("dashboard")
                                "user_management", "projections", "settings", "audit" -> viewModel.navigateTo("more")
                                else -> viewModel.setRouteForRole(currentUser?.role ?: UserRole.GESTIONNAIRE)
                            }
                        },
                        onHamburgerClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onRecouvrementClick = { viewModel.navigateTo("record_payment") },
                        onNotificationsClick = { viewModel.navigateTo("notifications") },
                        onSyncClick = { viewModel.syncOfflinePayments() },
                        onSettingsClick = { viewModel.navigateTo("settings") },
                        onLockClick = { viewModel.lockApp() },
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onLogoutGoogle = { viewModel.logoutGoogle() },
                        onSwitchRole = { newRole -> viewModel.switchUserRole(newRole) },
                        onAvatarSelected = { key -> viewModel.updateMyAvatarKey(key) },
                        onPhotoSelectedFromGallery = { uri -> viewModel.uploadAndSetUserAvatarFromUri(uri, context) },
                        isUploadingPhoto = isUploadingAvatar
                    )
                }
            },
            bottomBar = {
                val activeRoomId by viewModel.activeChatRoomId.collectAsStateWithLifecycle()
                if ((currentRoute != "chat" || activeRoomId == null) && currentRoute != "client_loan_apply") {
                    AppBottomNavigation(
                        currentRoute = currentRoute,
                        userRole = currentUser?.role,
                        currentLanguage = currentLang,
                        onNavigate = { route -> viewModel.navigateTo(route) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                val screenModifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)

                if (currentUser?.role == UserRole.CLIENT) {
                    when (currentRoute) {
                        "client_portal" -> ClientHomeScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_profile" -> ClientProfileScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_loan_apply" -> com.example.ui.screens.loans.LoanApplicationScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onNavigateBack = { viewModel.navigateTo("client_portal") }
                        )
                        "client_schedule" -> ClientScheduleScreen(viewModel = viewModel, modifier = screenModifier)
                        "chat" -> ChatScreen(viewModel = viewModel, modifier = screenModifier)
                        "notifications" -> NotificationsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding()))
                        else -> ClientHomeScreen(viewModel = viewModel, modifier = screenModifier)
                    }
                } else {
                    when (currentRoute) {
                        "dashboard" -> ManagerDashboardScreen(viewModel = viewModel, modifier = screenModifier)
                        "clients" -> ClientsScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_detail" -> ClientDetailScreen(viewModel = viewModel, modifier = screenModifier)
                        "cycles" -> CreditCyclesScreen(viewModel = viewModel, modifier = screenModifier)
                        "cycle_detail" -> CreditCycleDetailScreen(viewModel = viewModel, modifier = screenModifier)
                        "applications" -> LoanApplicationsScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_loan_apply" -> com.example.ui.screens.loans.LoanApplicationScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onNavigateBack = { viewModel.navigateTo("applications") }
                        )
                        "record_payment" -> RecordPaymentScreen(viewModel = viewModel, modifier = screenModifier)
                        "caisse", "caisse_entree", "caisse_sortie" -> CaisseScreen(viewModel = viewModel, modifier = screenModifier)
                        "reports" -> ReportsScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_portal" -> ClientHomeScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_profile" -> ClientProfileScreen(viewModel = viewModel, modifier = screenModifier)
                        "client_schedule" -> ClientScheduleScreen(viewModel = viewModel, modifier = screenModifier)
                        "chat" -> ChatScreen(viewModel = viewModel, modifier = screenModifier)
                        "notifications" -> NotificationsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding()))
                        "user_management" -> UserManagementScreen(viewModel = viewModel, modifier = screenModifier)
                        "projections" -> FinancialProjectionScreen(viewModel = viewModel, modifier = screenModifier)
                        "settings" -> SettingsScreen(viewModel = viewModel, modifier = screenModifier)
                        "audit" -> AuditScreen(viewModel = viewModel, modifier = screenModifier)
                        "member_cards" -> MemberCardProductionScreen(viewModel = viewModel, modifier = screenModifier)
                        "more" -> MoreMenuScreen(viewModel = viewModel, modifier = screenModifier)
                        "agent_home" -> AgentHomeScreen(viewModel = viewModel, modifier = screenModifier)
                        else -> ManagerDashboardScreen(viewModel = viewModel, modifier = screenModifier)
                    }
                }

                // Popup de notification avec bref aperçu si long et clic direct vers le chat / détails
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeInAppNotificationPopup != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .padding(top = 10.dp, start = 14.dp, end = 14.dp)
                ) {
                    activeInAppNotificationPopup?.let { notif ->
                        InAppNotificationPopupBanner(
                            notification = notif,
                            onDismiss = { activeInAppNotificationPopup = null },
                            onClick = {
                                activeInAppNotificationPopup = null
                                viewModel.markNotificationAsRead(notif.id)
                                val isChat = notif.type == com.example.data.model.NotificationType.MESSAGE ||
                                        notif.actionRoute?.startsWith("chat") == true
                                if (isChat) {
                                    val targetRoomId = com.example.ui.screens.notifications.resolveChatRoomId(notif, viewModel)
                                    viewModel.openChat(targetRoomId)
                                } else {
                                    viewModel.navigateTo("notifications")
                                }
                            }
                        )
                    }
                }
            }
        }

    // Message direct de succès ou d'échec des opérations (Ajouter client, Caisse, Crédit, Recouvrement)
    operationFeedback?.let { feedback ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissOperationFeedback() },
            icon = {
                Icon(
                    imageVector = if (feedback.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (feedback.isSuccess) com.example.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(46.dp)
                )
            },
            title = {
                Text(
                    text = feedback.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (feedback.isSuccess) com.example.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        color = if (feedback.isSuccess) androidx.compose.ui.graphics.Color(0xFFDCFCE7) else androidx.compose.ui.graphics.Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedback.message,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (feedback.isSuccess) androidx.compose.ui.graphics.Color(0xFF14532D) else androidx.compose.ui.graphics.Color(0xFF7F1D1D),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    feedback.details?.let { details ->
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = details,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isDirectRecovery = feedback.actionLabel.contains("Recouvrement", ignoreCase = true)
                        viewModel.dismissOperationFeedback()
                        if (isDirectRecovery) {
                            val cycleId = viewModel.selectedCycleId.value
                            if (!cycleId.isNullOrEmpty()) {
                                viewModel.openPaymentForCycle(cycleId)
                            } else {
                                viewModel.navigateTo("record_payment")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (feedback.isSuccess) com.example.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(feedback.actionLabel)
                }
            }
        )
    }

    if (showDrawerAvatarPicker) {
        BybitAvatarPickerDialog(
            currentKey = currentUser?.avatarKey,
            role = currentUser?.role,
            gender = "Homme",
            onAvatarSelected = { key ->
                viewModel.updateMyAvatarKey(key)
                showDrawerAvatarPicker = false
            },
            onPhotoSelectedFromGallery = { uri ->
                viewModel.uploadAndSetUserAvatarFromUri(uri, context) { success, _ ->
                    if (success) {
                        showDrawerAvatarPicker = false
                    }
                }
            },
            isUploadingPhoto = isUploadingAvatar,
            onDismiss = { showDrawerAvatarPicker = false }
        )
    }
}
}

/**
 * Popup de notification élégant en tête d'écran :
 * - Affiche un bref aperçu si le message est long
 * - Redirige immédiatement vers le chat ou le détail complet au clic
 * - Couleurs originales de l'application (PrimaryNavy & AccentSkyBlue)
 */
@Composable
fun InAppNotificationPopupBanner(
    notification: com.example.data.model.AppNotificationEntity,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChat = notification.type == com.example.data.model.NotificationType.MESSAGE ||
            notification.actionRoute?.startsWith("chat") == true
    val isLong = notification.message.length > 70

    // Auto-fermeture après 5 secondes si non touché
    LaunchedEffect(notification.id) {
        kotlinx.coroutines.delay(5000L)
        onDismiss()
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = com.example.ui.theme.PrimaryNavy,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("in_app_notification_popup")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.AccentSkyBlue),
                modifier = Modifier.size(38.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(
                        imageVector = if (isChat) Icons.Default.Chat else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = com.example.ui.theme.AccentSkyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "À l'instant",
                        fontSize = 10.sp,
                        color = com.example.ui.theme.AccentSkyBlue
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bref aperçu si le message est long
                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = if (isLong) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                if (isLong) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isChat) "💬 Message direct • Toucher pour ouvrir la discussion" else "📖 Bref aperçu • Toucher pour lire le détail",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.ui.theme.AccentSkyBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
