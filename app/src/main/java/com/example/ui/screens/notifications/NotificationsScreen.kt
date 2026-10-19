package com.example.ui.screens.notifications

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotificationEntity
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueContainer
import com.example.ui.theme.AccentSkyBlueLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightBackground
import com.example.ui.theme.OnPrimaryNavy
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PrimaryNavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Couleur discrète pour le connecteur en pointillés
private val DottedLineColor = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    var selectedNotification by remember { mutableStateOf<AppNotificationEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // Groupement des notifications par date calendaire (ordre antéchronologique)
    val groupedNotifications = remember(notifications) {
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        notifications
            .sortedByDescending { it.timestamp }
            .groupBy { notif -> dayKeyFormat.format(Date(notif.timestamp)) }
            .toList()
    }

    val fullDateFormat = remember { SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm", Locale.FRENCH) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // =========================================================================
        // 1. BANNIÈRE ORIGINALE DE L'APPLICATION (BLEU MARINE / PRIMARY NAVY)
        //    (Remplacement complet du rouge par les couleurs authentiques de l'app)
        // =========================================================================
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_back_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notifications",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = AccentSkyBlue,
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount non lue${if (unreadCount > 1) "s" else ""}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Option sonnerie / vibration
                    IconButton(
                        onClick = {
                            NotificationHelper.vibrateAndPlaySound(context)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Test Alerte Sonore",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bouton CLEAR ALL avec les couleurs originales
                    TextButton(
                        onClick = {
                            if (notifications.isNotEmpty()) {
                                showClearAllConfirmDialog = true
                            }
                        },
                        enabled = notifications.isNotEmpty(),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("btn_clear_all_notifications")
                    ) {
                        Text(
                            text = "CLEAR ALL",
                            color = if (notifications.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. CORPS : TIMELINE ARRANGÉE PAR DATE AVEC CONNECTEURS EN POINTILLÉS
        // =========================================================================
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentSkyBlueContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Aucune notification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )

                        Text(
                            text = "Toutes vos notifications apparaîtront ici arrangées par date avec le suivi chronologique.",
                            fontSize = 12.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                seedSampleNotifications(viewModel)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Générer des alertes d'exemple", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("notifications_timeline_list")
            ) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                groupedNotifications.forEach { (_, notifsUnderDate) ->
                    val firstTimestamp = notifsUnderDate.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                    val dateHeaderLabel = formatHeaderDate(firstTimestamp)

                    // En-tête de date avec badge pill discret aux couleurs de l'application
                    item {
                        DateBadgeHeader(dateText = dateHeaderLabel)
                    }

                    // Ligne pointillée reliant la date à la première carte
                    item {
                        DottedLineConnector()
                    }

                    // Cartes de notification pour cette date
                    notifsUnderDate.forEachIndexed { index, notif ->
                        item {
                            NotificationTimelineCard(
                                notification = notif,
                                onClick = {
                                    // Marquer immédiatement comme lue
                                    viewModel.markNotificationAsRead(notif.id)

                                    // RÈGLE CLEF : Si c'est une notification de message,
                                    // amener DIRECTEMENT sur le chat où le message a été envoyé !
                                    if (notif.type == NotificationType.MESSAGE || notif.actionRoute?.startsWith("chat") == true) {
                                        val targetRoomId = resolveChatRoomId(notif, viewModel)
                                        viewModel.openChat(targetRoomId)
                                    } else {
                                        // Pour les autres notifications, ouvrir le bel espace de lecture
                                        selectedNotification = notif
                                    }
                                }
                            )
                        }

                        // Ligne pointillée entre deux cartes successives sous la même date
                        if (index < notifsUnderDate.size - 1) {
                            item {
                                DottedLineConnector()
                            }
                        }
                    }

                    // Espace avant la prochaine date
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // =========================================================================
    // 3. DIALOGUE DE CONFIRMATION "CLEAR ALL"
    // =========================================================================
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = {
                Text("Effacer toutes les notifications ?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Cette action va supprimer l'ensemble de votre historique de notifications. Voulez-vous continuer ?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmDialog = false
                        viewModel.clearAllNotifications()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Effacer tout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Annuler", color = TextMuted)
                }
            }
        )
    }

    // =========================================================================
    // 4. BEL ESPACE DE LECTURE DE NOTIFICATIONS AVEC LES COULEURS DE L'APPLICATION
    //    (ModalBottomSheet élégante, spacieuse, haute lisibilité et actions directes)
    // =========================================================================
    selectedNotification?.let { notif ->
        val formattedFullDate = fullDateFormat.format(Date(notif.timestamp))
        NotificationReadingBottomSheet(
            notification = notif,
            formattedDate = formattedFullDate,
            onDismiss = { selectedNotification = null },
            onDelete = {
                viewModel.deleteNotification(notif.id)
                selectedNotification = null
            },
            onNavigateAction = { route ->
                selectedNotification = null
                if (route.startsWith("chat:")) {
                    val roomId = route.removePrefix("chat:")
                    viewModel.openChat(roomId)
                } else if (route == "chat") {
                    val roomId = resolveChatRoomId(notif, viewModel)
                    viewModel.openChat(roomId)
                } else {
                    viewModel.navigateTo(route)
                }
            },
            onTestAlert = {
                NotificationHelper.vibrateAndPlaySound(context)
            }
        )
    }
}

/**
 * En-tête de date encapsulé dans un badge arrondi discret (ex: 17 OCT. 2026).
 * Stylé aux couleurs de l'application (PrimaryNavy discret).
 */
@Composable
fun DateBadgeHeader(dateText: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PrimaryNavy.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 2.dp)
    ) {
        Text(
            text = dateText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryNavy,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * Ligne de connecteur vertical en 3 points (⋮) reliant les dates et les cartes.
 */
@Composable
fun DottedLineConnector(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(start = 18.dp, top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(DottedLineColor)
            )
        }
    }
}

/**
 * Carte de notification blanche épurée, arrangée avec la timeline et les couleurs originales.
 */
@Composable
fun NotificationTimelineCard(
    notification: AppNotificationEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMessage = notification.type == NotificationType.MESSAGE || notification.actionRoute?.startsWith("chat") == true

    // Palette des couleurs originales de l'application selon la nature de la notification
    val (icon, badgeBorderColor) = when (notification.type) {
        NotificationType.PAIEMENT -> Icons.Default.Payment to SuccessGreen
        NotificationType.DECAISSEMENT -> Icons.Default.MonetizationOn to AccentSkyBlue
        NotificationType.DEMANDE -> Icons.Default.AccountBalanceWallet to PrimaryNavy
        NotificationType.RETARD -> Icons.Default.Warning to WarningAmber
        NotificationType.CAISSE -> Icons.Default.AccountBalanceWallet to WarningAmber
        NotificationType.MESSAGE -> Icons.Default.Chat to AccentSkyBlue
        NotificationType.SYSTEM -> Icons.Default.Notifications to PrimaryNavy
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.8.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône circulaire avec contour coloré selon les teintes originales
            Surface(
                shape = CircleShape,
                color = badgeBorderColor.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, badgeBorderColor),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeBorderColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Texte de la notification : bref aperçu si long, détail complet accessible au clic
            Column(modifier = Modifier.weight(1f)) {
                val isLong = notification.message.length > 80
                Text(
                    text = notification.message,
                    fontSize = 13.5.sp,
                    fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    color = TextPrimary,
                    lineHeight = 18.5.sp,
                    maxLines = if (isLong) 2 else 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (isLong) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isMessage) "💬 Message direct • Toucher pour ouvrir la discussion" else "📖 Bref aperçu • Toucher pour lire le détail complet",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentSkyBlue
                    )
                } else if (isMessage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Message direct • Appuyez pour ouvrir la discussion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentSkyBlue
                        )
                    }
                }
            }

            // Indicateur non lu discret aux couleurs de l'application (bleu ciel)
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(AccentSkyBlue)
                )
            }
        }
    }
}

/**
 * Bel espace de lecture dédié pour afficher l'intégralité d'une notification
 * avec les couleurs de l'application (PrimaryNavy, AccentSkyBlue, surfaces raffinées).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationReadingBottomSheet(
    notification: AppNotificationEntity,
    formattedDate: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onNavigateAction: (String) -> Unit,
    onTestAlert: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val (icon, badgeColor, typeLabel) = when (notification.type) {
        NotificationType.DEMANDE -> Triple(Icons.Default.MonetizationOn, PrimaryNavy, "Demande de Crédit")
        NotificationType.PAIEMENT -> Triple(Icons.Default.Payment, SuccessGreen, "Paiement Recouvré")
        NotificationType.RETARD -> Triple(Icons.Default.Warning, WarningAmber, "Alerte de Retard")
        NotificationType.DECAISSEMENT -> Triple(Icons.Default.AccountBalanceWallet, AccentSkyBlue, "Décaissement de Prêt")
        NotificationType.CAISSE -> Triple(Icons.Default.AccountBalanceWallet, WarningAmber, "Mouvement de Caisse")
        NotificationType.MESSAGE -> Triple(Icons.Default.Chat, AccentSkyBlue, "Message Privé")
        NotificationType.SYSTEM -> Triple(Icons.Default.Notifications, PrimaryNavy, "Notification Système")
    }

    val actionRoute = when (notification.type) {
        NotificationType.DEMANDE -> "applications"
        NotificationType.PAIEMENT -> "record_payment"
        NotificationType.RETARD -> "cycles"
        NotificationType.DECAISSEMENT -> "cycles"
        NotificationType.CAISSE -> "caisse"
        NotificationType.MESSAGE -> notification.actionRoute ?: "chat"
        NotificationType.SYSTEM -> notification.actionRoute
    }

    val actionButtonTitle = when (notification.type) {
        NotificationType.DEMANDE -> "Examiner les demandes"
        NotificationType.PAIEMENT -> "Voir les remboursements"
        NotificationType.RETARD -> "Voir le dossier de prêt"
        NotificationType.DECAISSEMENT -> "Voir le cycle de crédit"
        NotificationType.CAISSE -> "Consulter la caisse"
        NotificationType.MESSAGE -> "Ouvrir la discussion"
        NotificationType.SYSTEM -> if (actionRoute != null) "Accéder à la section" else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // =========================================================================
            // EN-TÊTE SUPÉRIEUR DU BEL ESPACE DE LECTURE (COULEURS DE L'APPLICATION)
            // =========================================================================
            Surface(
                color = PrimaryNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentSkyBlueContainer,
                            border = BorderStroke(1.5.dp, AccentSkyBlueLight),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = notification.title.ifBlank { typeLabel },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentSkyBlue.copy(alpha = 0.25f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 11.sp,
                                    color = AccentSkyBlueLight,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }
            }

            // =========================================================================
            // CORPS DU BEL ESPACE DE LECTURE (ESPACE SPACIEUX, TYPOGRAPHIE CONFORTABLE)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Ligne Date & Statut de lecture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreenContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Consultée",
                                fontSize = 11.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // =====================================================================
                // CARTE PRINCIPALE DE LECTURE DU MESSAGE (Grand espace épuré & agréable)
                // =====================================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LightBackground,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = notification.message,
                            fontSize = 15.5.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Normal,
                            color = PrimaryNavyDark
                        )

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                        // Bouton de copie rapide du texte du message
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(notification.message))
                                    Toast.makeText(context, "Texte copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copier le texte",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                            }
                        }
                    }
                }

                // Carte Métadonnées (Destinataire / Émetteur / Concerne)
                if (!notification.destinationAddress.isNullOrEmpty() || !notification.senderName.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentSkyBlueContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, AccentSkyBlueLight.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!notification.destinationAddress.isNullOrEmpty()) {
                                Text(
                                    text = "🎯 Concerne : ${notification.destinationAddress}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryNavy
                                )
                            }
                            if (!notification.senderName.isNullOrEmpty()) {
                                Text(
                                    text = "👤 Source : ${notification.senderName}",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // =====================================================================
                // BARRE D'ACTIONS INFÉRIEURE (COULEURS DE L'APPLICATION)
                // =====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton de suppression
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDelete()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer cette notification",
                            tint = ErrorRed
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Bouton tester sonnerie
                        OutlinedButton(
                            onClick = onTestAlert,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryNavy.copy(alpha = 0.4f)),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alerte", fontSize = 12.sp, color = PrimaryNavy, fontWeight = FontWeight.Bold)
                        }

                        // Bouton principal d'action aux couleurs de l'application
                        if (actionRoute != null && actionButtonTitle != null) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onNavigateAction(actionRoute)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Icon(
                                    imageVector = if (notification.type == NotificationType.MESSAGE) Icons.Default.Chat else Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = actionButtonTitle,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Résout de façon sûre l'identifiant exact du salon de messagerie correspondant à une notification.
 */
fun resolveChatRoomId(notification: AppNotificationEntity, viewModel: MainViewModel): String {
    // 1. Depuis actionRoute explicite ("chat:CLI-001", "chat:COMMUNITY_GROUP", etc.)
    val routeRoom = notification.actionRoute?.removePrefix("chat:")?.takeIf { it.isNotBlank() && it != "chat" }
    if (routeRoom != null) return routeRoom

    // 2. Depuis targetClientId (ex: "CLI-001")
    if (!notification.targetClientId.isNullOrBlank()) {
        return notification.targetClientId
    }

    // 3. Détection dans destinationAddress
    val dest = notification.destinationAddress.orEmpty()
    val cliMatch = Regex("""(CLI-\d+)""").find(dest)?.value
    if (cliMatch != null) return cliMatch

    if (dest.contains("Communautaire", ignoreCase = true) || dest.contains("COMMUNITY", ignoreCase = true)) {
        return "COMMUNITY_GROUP"
    }

    // 4. Détection dans le titre
    if (notification.title.contains("Communauté", ignoreCase = true)) {
        return "COMMUNITY_GROUP"
    }

    // 5. Cas d'un utilisateur Client recevant un message
    val currentUser = viewModel.currentUser.value
    if (currentUser != null && currentUser.role == UserRole.CLIENT) {
        val myClients = viewModel.allClients.value
        val myClient = myClients.find { it.phone == currentUser.phone || it.id == currentUser.id || currentUser.id.endsWith(it.id) }
        val myClientId = myClient?.id ?: if (currentUser.id.startsWith("USR-CLI-")) currentUser.id.removePrefix("USR-") else null
        if (myClientId != null) return myClientId
    }

    // 6. Cas de messages directs entre deux utilisateurs (ex: DIRECT_USR-001_USR-002)
    if (currentUser != null) {
        val otherId = notification.senderId ?: notification.targetUserId
        if (!otherId.isNullOrBlank() && otherId != currentUser.id) {
            val participants = listOf(currentUser.id, otherId).sorted()
            return "DIRECT_" + participants.joinToString("_")
        }
    }

    return "COMMUNITY_GROUP"
}

/**
 * Formate un timestamp sous la forme "17 OCT. 2026" comme sur la capture d'écran.
 */
private fun formatHeaderDate(timestamp: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
    val month = when (cal.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "JANV."
        Calendar.FEBRUARY -> "FÉVR."
        Calendar.MARCH -> "MARS"
        Calendar.APRIL -> "AVR."
        Calendar.MAY -> "MAI"
        Calendar.JUNE -> "JUIN"
        Calendar.JULY -> "JUIL."
        Calendar.AUGUST -> "AOÛT"
        Calendar.SEPTEMBER -> "SEPT."
        Calendar.OCTOBER -> "OCT."
        Calendar.NOVEMBER -> "NOV."
        Calendar.DECEMBER -> "DÉC."
        else -> "OCT."
    }
    val year = cal.get(Calendar.YEAR)
    return "$day $month $year"
}

/**
 * Génère des exemples de notifications réalistes sur plusieurs dates pour illustrer immédiatement la disposition demandée.
 */
private fun seedSampleNotifications(viewModel: MainViewModel) {
    val now = System.currentTimeMillis()

    // Notification de message pour tester la navigation directe vers le chat
    viewModel.recordNotificationActivity(
        title = "Message Privé • Agent Solidaire",
        message = "Bonjour ! Votre dossier de renouvellement de crédit pour le stand au marché a été approuvé. Passez à la caisse pour le décaissement.",
        type = NotificationType.MESSAGE,
        destinationAddress = "Salon Privé Client • CLI-001",
        targetClientId = "CLI-001",
        actionRoute = "chat:CLI-001"
    )

    // Autres notifications d'opérations courantes
    viewModel.recordNotificationActivity(
        title = "Achat de Forfait Validé",
        message = "Félicitations ! Vous avez acheté ce forfait Internet 3GB a 120U avec succès !",
        type = NotificationType.SYSTEM,
        destinationAddress = "Ligne Principale • Compte Malaki"
    )
    viewModel.recordNotificationActivity(
        title = "Paiement Recouvré Validé",
        message = "Remboursement de 15 000 FC bien enregistré et transmis avec succès à la caisse centrale.",
        type = NotificationType.PAIEMENT,
        destinationAddress = "Caisse Principale • Agent Collecteur"
    )
    viewModel.recordNotificationActivity(
        title = "Forfait Journalier",
        message = "Félicitations ! Vous avez acheté ce forfait Internet 1GB a 50U valide jsq 23h59' avec succès !",
        type = NotificationType.SYSTEM,
        destinationAddress = "Ligne Principale"
    )
}
