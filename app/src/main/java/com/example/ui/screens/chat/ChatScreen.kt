package com.example.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ClientEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.BybitAvatar
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.BubbleStyle
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.AudioVoiceHelper
import com.example.util.PresenceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val SecureTealColor = Color(0xFF0F766E)
private val SecureAccentTeal = SecureTealColor

sealed class ChatListItem(val key: String) {
    data class DateHeader(val dateText: String, val dateKey: String) : ChatListItem("header_$dateKey")
    data class MessageItem(val message: ChatMessageEntity) : ChatListItem("msg_${message.id}")
}

@Composable
fun ChatDateHeaderChip(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = SecureAccentTeal,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = dateText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioHelper = remember { AudioVoiceHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioHelper.release()
        }
    }

    val activeChatRoomId by viewModel.activeChatRoomId.collectAsStateWithLifecycle()
    val chatClientId by viewModel.chatClientId.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allChatMessages by viewModel.allChatMessages.collectAsStateWithLifecycle()
    val currentChatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
    val pinnedMessage by viewModel.currentPinnedMessage.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentClock by viewModel.currentClock.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val bubbleStyle by viewModel.chatBubbleStyle.collectAsStateWithLifecycle()

    var showBubbleStyleSheet by remember { mutableStateOf(false) }

    // Construction réactive et sécurisée de la liste des conversations selon les règles de confidentialité
    val myUserId = currentUser?.id ?: "USR-001"
    val conversationSummaries = remember(allChatMessages, allUsers, allClients, myUserId, currentClock, currentUser) {
        val list = mutableListOf<SecureConversationSummary>()
        val userRole = currentUser?.role ?: UserRole.GESTIONNAIRE

        // 1. DISCUSSION COMMUNAUTAIRE (Épinglée en tête pour tous les utilisateurs)
        val communityMsgs = allChatMessages.filter { it.clientId == "COMMUNITY_GROUP" }
        val lastCommunityMsg = communityMsgs.maxByOrNull { it.timestamp }
        val communityUnread = communityMsgs.count { it.senderId != myUserId && !it.isRead }
        list.add(
            SecureConversationSummary(
                roomId = "COMMUNITY_GROUP",
                title = "🌍 Communauté & Équipe Malaki",
                subtitle = "Canal d'équipe & Annonces générales",
                isCommunityGroup = true,
                isOnline = true,
                lastActiveTimestamp = currentClock,
                lastMessage = lastCommunityMsg,
                unreadCount = communityUnread,
                isPinned = true
            )
        )

        when (userRole) {
            UserRole.CLIENT -> {
                // Client voit sa discussion avec son conseiller de crédit + les directs qui le concernent
                val myClient = allClients.find { client ->
                    (currentUser != null && currentUser?.email?.isNotBlank() == true && client.email.equals(currentUser?.email, ignoreCase = true)) ||
                    (currentUser != null && currentUser?.phone?.isNotBlank() == true && client.phone == currentUser?.phone) ||
                    (currentUser != null && (client.id == currentUser?.id || client.id == "CLI-${currentUser?.id?.removePrefix("USR-")}")) ||
                    (currentUser != null && currentUser?.fullName?.isNotBlank() == true && client.fullName.equals(currentUser?.fullName, ignoreCase = true))
                }
                val myClientId = myClient?.id ?: "CLI-${myUserId.removePrefix("USR-")}"
                val advisorId = myClient?.assignedAgentId
                val advisorUser = allUsers.find { it.id == advisorId } ?: allUsers.find { it.role == UserRole.AGENT }
                val clientMsgs = allChatMessages.filter { it.clientId == myClientId }
                val lastClientMsg = clientMsgs.maxByOrNull { it.timestamp }
                val unreadClient = clientMsgs.count { it.senderId != myUserId && !it.isRead }
                val isOnline = (advisorUser?.isOnline == true && currentClock - advisorUser.lastActiveTimestamp < 120000L)

                list.add(
                    SecureConversationSummary(
                        roomId = myClientId,
                        title = "Conseiller • ${advisorUser?.fullName ?: "David Baraka"}",
                        subtitle = "🔒 Discussion privée avec votre conseiller de crédit",
                        isCommunityGroup = false,
                        interlocutorUser = advisorUser,
                        interlocutorClient = myClient,
                        isOnline = isOnline,
                        lastActiveTimestamp = advisorUser?.lastActiveTimestamp ?: currentClock,
                        lastMessage = lastClientMsg,
                        unreadCount = unreadClient,
                        isPinned = false
                    )
                )

                // Discussions directes 1-à-1 impliquant le client
                val clientPossibleIds = com.example.util.ChatSecurityHelper.getUserPossibleIds(currentUser, allClients)
                val clientDirectMsgs = allChatMessages.filter { msg ->
                    com.example.util.ChatSecurityHelper.isUserParticipantOfDirectRoom(currentUser, msg.clientId, allClients)
                }.groupBy { it.clientId }

                clientDirectMsgs.forEach { (roomId, msgs) ->
                    val parts = roomId.removePrefix("DIRECT_").split("_")
                    val otherUserId = parts.find { p ->
                        clientPossibleIds.none { myId ->
                            p.equals(myId, ignoreCase = true) || p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
                        }
                    } ?: parts.firstOrNull()
                    val otherUser = allUsers.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || it.id.removePrefix("USR-") == otherUserId }
                    val otherClient = allClients.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || (otherUser != null && it.phone == otherUser.phone) }
                    val lastMsg = msgs.maxByOrNull { it.timestamp }
                    val unread = msgs.count { it.senderId != myUserId && !it.isRead }
                    val title = otherUser?.fullName ?: otherClient?.fullName ?: otherUserId ?: "Discussion privée"
                    val subtitle = otherUser?.let { "🔒 ${it.role.label} • Discussion directe" } ?: otherClient?.let { "🔒 Client • ${it.marketZone}" } ?: "🔒 Discussion directe"
                    list.add(
                        SecureConversationSummary(
                            roomId = roomId,
                            title = title,
                            subtitle = subtitle,
                            isCommunityGroup = false,
                            interlocutorUser = otherUser,
                            interlocutorClient = otherClient,
                            isOnline = (otherUser?.isOnline == true && (currentClock - otherUser.lastActiveTimestamp < 120000L)) ||
                                    (otherClient?.isOnline == true && (currentClock - otherClient.lastActiveTimestamp < 120000L)),
                            lastActiveTimestamp = otherUser?.lastActiveTimestamp ?: otherClient?.lastActiveTimestamp ?: currentClock,
                            lastMessage = lastMsg,
                            unreadCount = unread,
                            isPinned = false
                        )
                    )
                }
            }

            UserRole.GESTIONNAIRE -> {
                // Le gestionnaire voit toutes les discussions directes où il est participant
                val managerPossibleIds = com.example.util.ChatSecurityHelper.getUserPossibleIds(currentUser, allClients)
                val staffDirectMsgs = allChatMessages.filter { msg ->
                    com.example.util.ChatSecurityHelper.isUserParticipantOfDirectRoom(currentUser, msg.clientId, allClients)
                }.groupBy { it.clientId }

                val handledUserIds = mutableSetOf<String>()
                staffDirectMsgs.forEach { (roomId, msgs) ->
                    val parts = roomId.removePrefix("DIRECT_").split("_")
                    val otherUserId = parts.find { p ->
                        managerPossibleIds.none { myId ->
                            p.equals(myId, ignoreCase = true) || p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
                        }
                    } ?: parts.firstOrNull()
                    val otherUser = allUsers.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || it.id.removePrefix("USR-") == otherUserId }
                    val otherClient = allClients.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || (otherUser != null && it.phone == otherUser.phone) }

                    otherUser?.let { handledUserIds.add(it.id) }
                    otherClient?.let { handledUserIds.add(it.id) }
                    val lastMsg = msgs.maxByOrNull { it.timestamp }
                    val unread = msgs.count { it.senderId != myUserId && !it.isRead }
                    val title = otherUser?.fullName ?: otherClient?.fullName ?: otherUserId ?: "Discussion directe"
                    val subtitle = otherUser?.let { "${it.role.label} • ${it.marketZone.ifEmpty { "Direction" }}" } ?: otherClient?.let { "Client • ${it.marketZone}" } ?: "Discussion directe"
                    list.add(
                        SecureConversationSummary(
                            roomId = roomId,
                            title = title,
                            subtitle = subtitle,
                            isCommunityGroup = false,
                            interlocutorUser = otherUser,
                            interlocutorClient = otherClient,
                            isOnline = (otherUser?.isOnline == true && (currentClock - otherUser.lastActiveTimestamp < 120000L)) ||
                                    (otherClient?.isOnline == true && (currentClock - otherClient.lastActiveTimestamp < 120000L)),
                            lastActiveTimestamp = otherUser?.lastActiveTimestamp ?: otherClient?.lastActiveTimestamp ?: currentClock,
                            lastMessage = lastMsg,
                            unreadCount = unread,
                            isPinned = false
                        )
                    )
                }

                // Suggestions de collègues de travail staff (Agents et Caissiers)
                allUsers.filter { it.id != myUserId && it.role != UserRole.CLIENT && it.id !in handledUserIds }.forEach { user ->
                    val directRoomId = "DIRECT_" + listOf(myUserId, user.id).sorted().joinToString("_")
                    list.add(
                        SecureConversationSummary(
                            roomId = directRoomId,
                            title = user.fullName,
                            subtitle = "${user.role.label} • Discussion directe",
                            isCommunityGroup = false,
                            interlocutorUser = user,
                            isOnline = user.isOnline && (currentClock - user.lastActiveTimestamp < 120000L),
                            lastActiveTimestamp = user.lastActiveTimestamp,
                            lastMessage = null,
                            unreadCount = 0,
                            isPinned = false
                        )
                    )
                }
            }

            UserRole.AGENT -> {
                // Agent voit :
                // 1) Ses clients attribués ou les clients avec qui il y a eu échange
                val handledClients = mutableSetOf<String>()
                val clientMsgsGrouped = allChatMessages.filter { it.clientId.startsWith("CLI-") }.groupBy { it.clientId }

                clientMsgsGrouped.forEach { (cId, msgs) ->
                    val client = allClients.find { it.id == cId }
                    if (client != null && (client.assignedAgentId == null || client.assignedAgentId == myUserId || client.marketZone == currentUser?.marketZone || msgs.isNotEmpty())) {
                        handledClients.add(client.id)
                        val lastMsg = msgs.maxByOrNull { it.timestamp }
                        val unread = msgs.count { it.senderId != myUserId && !it.isRead }
                        val isOnline = client.isOnline && (currentClock - client.lastActiveTimestamp < 120000L)
                        list.add(
                            SecureConversationSummary(
                                roomId = client.id,
                                title = client.fullName,
                                subtitle = "Client • ${client.marketZone} (${client.phone})",
                                isCommunityGroup = false,
                                interlocutorClient = client,
                                isOnline = isOnline,
                                lastActiveTimestamp = client.lastActiveTimestamp,
                                lastMessage = lastMsg,
                                unreadCount = unread,
                                isPinned = false
                            )
                        )
                    }
                }

                // Clients assignés n'ayant pas encore de message
                allClients.filter { (it.assignedAgentId == myUserId || it.marketZone == currentUser?.marketZone) && it.id !in handledClients }.forEach { client ->
                    val isOnline = client.isOnline && (currentClock - client.lastActiveTimestamp < 120000L)
                    list.add(
                        SecureConversationSummary(
                            roomId = client.id,
                            title = client.fullName,
                            subtitle = "Client • ${client.marketZone}",
                            isCommunityGroup = false,
                            interlocutorClient = client,
                            isOnline = isOnline,
                            lastActiveTimestamp = client.lastActiveTimestamp,
                            lastMessage = null,
                            unreadCount = 0,
                            isPinned = false
                        )
                    )
                }

                // 2) Discuter avec les collègues staff et contacts directs
                val handledStaff = mutableSetOf<String>()
                val agentPossibleIds = com.example.util.ChatSecurityHelper.getUserPossibleIds(currentUser, allClients)
                val staffDirectMsgs = allChatMessages.filter { msg ->
                    com.example.util.ChatSecurityHelper.isUserParticipantOfDirectRoom(currentUser, msg.clientId, allClients)
                }.groupBy { it.clientId }

                staffDirectMsgs.forEach { (roomId, msgs) ->
                    val parts = roomId.removePrefix("DIRECT_").split("_")
                    val otherUserId = parts.find { p ->
                        agentPossibleIds.none { myId ->
                            p.equals(myId, ignoreCase = true) || p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
                        }
                    } ?: parts.firstOrNull()
                    val otherUser = allUsers.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || it.id.removePrefix("USR-") == otherUserId }
                    val otherClient = allClients.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || (otherUser != null && it.phone == otherUser.phone) }

                    otherUser?.let { handledStaff.add(it.id) }
                    val lastMsg = msgs.maxByOrNull { it.timestamp }
                    val unread = msgs.count { it.senderId != myUserId && !it.isRead }
                    val title = otherUser?.fullName ?: otherClient?.fullName ?: otherUserId ?: "Discussion directe"
                    val subtitle = otherUser?.let { "${it.role.label} • Discussion directe" } ?: otherClient?.let { "Client • ${it.marketZone}" } ?: "Discussion directe"
                    list.add(
                        SecureConversationSummary(
                            roomId = roomId,
                            title = title,
                            subtitle = subtitle,
                            isCommunityGroup = false,
                            interlocutorUser = otherUser,
                            interlocutorClient = otherClient,
                            isOnline = (otherUser?.isOnline == true && (currentClock - otherUser.lastActiveTimestamp < 120000L)) ||
                                    (otherClient?.isOnline == true && (currentClock - otherClient.lastActiveTimestamp < 120000L)),
                            lastActiveTimestamp = otherUser?.lastActiveTimestamp ?: otherClient?.lastActiveTimestamp ?: currentClock,
                            lastMessage = lastMsg,
                            unreadCount = unread,
                            isPinned = false
                        )
                    )
                }
            }

            UserRole.CAISSIER -> {
                // Caissier voit communauté et ses discussions directes
                val cashierPossibleIds = com.example.util.ChatSecurityHelper.getUserPossibleIds(currentUser, allClients)
                val staffDirectMsgs = allChatMessages.filter { msg ->
                    com.example.util.ChatSecurityHelper.isUserParticipantOfDirectRoom(currentUser, msg.clientId, allClients)
                }.groupBy { it.clientId }

                staffDirectMsgs.forEach { (roomId, msgs) ->
                    val parts = roomId.removePrefix("DIRECT_").split("_")
                    val otherUserId = parts.find { p ->
                        cashierPossibleIds.none { myId ->
                            p.equals(myId, ignoreCase = true) || p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
                        }
                    } ?: parts.firstOrNull()
                    val otherUser = allUsers.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || it.id.removePrefix("USR-") == otherUserId }
                    val otherClient = allClients.find { it.id == otherUserId || "USR-${it.id}" == otherUserId || (otherUser != null && it.phone == otherUser.phone) }

                    val lastMsg = msgs.maxByOrNull { it.timestamp }
                    val unread = msgs.count { it.senderId != myUserId && !it.isRead }
                    val title = otherUser?.fullName ?: otherClient?.fullName ?: otherUserId ?: "Discussion directe"
                    val subtitle = otherUser?.let { "${it.role.label} • Discussion directe" } ?: otherClient?.let { "Client • ${it.marketZone}" } ?: "Discussion directe"
                    list.add(
                        SecureConversationSummary(
                            roomId = roomId,
                            title = title,
                            subtitle = subtitle,
                            isCommunityGroup = false,
                            interlocutorUser = otherUser,
                            interlocutorClient = otherClient,
                            isOnline = (otherUser?.isOnline == true && (currentClock - otherUser.lastActiveTimestamp < 120000L)) ||
                                    (otherClient?.isOnline == true && (currentClock - otherClient.lastActiveTimestamp < 120000L)),
                            lastActiveTimestamp = otherUser?.lastActiveTimestamp ?: otherClient?.lastActiveTimestamp ?: currentClock,
                            lastMessage = lastMsg,
                            unreadCount = unread,
                            isPinned = false
                        )
                    )
                }
            }
        }

        // Tri : épinglé en premier, puis le message le plus récent
        list.sortedWith(
            compareByDescending<SecureConversationSummary> { it.isPinned }
                .thenByDescending { it.lastMessage?.timestamp ?: 0L }
        )
    }

    // --- GESTION DE LA NAVIGATION DANS LA MESSAGERIE SÉCURISÉE ---
    if (activeChatRoomId == null) {
        // Vue 1 : LISTE DES DISCUSSIONS SÉCURISÉES
        SecureConversationList(
            currentUser = currentUser,
            conversations = conversationSummaries,
            allUsers = allUsers,
            allClients = allClients,
            onOpenConversation = { roomId ->
                viewModel.openChat(roomId)
            },
            onOpenCommunityChat = {
                viewModel.openCommunityChat()
            },
            onOpenBubbleStylePicker = {
                showBubbleStyleSheet = true
            },
            onMarkAllAsRead = {
                viewModel.markAllNotificationsAsRead()
                viewModel.markCurrentChatAsRead()
                Toast.makeText(context, "Toutes les discussions sont marquées comme lues.", Toast.LENGTH_SHORT).show()
            },
            onNavigateBack = {
                viewModel.navigateBack()
            },
            modifier = modifier
        )
    } else {
        // Interception du retour système pour revenir à la liste des discussions
        BackHandler(enabled = true) {
            viewModel.closeActiveChatRoom()
        }

        // Vue 2 : SALON DE DISCUSSION SÉCURISÉ AVEC BALAYAGE POUR RÉPONDRE
        SecureChatRoomView(
            viewModel = viewModel,
            audioHelper = audioHelper,
            roomId = activeChatRoomId ?: chatClientId,
            messages = currentChatMessages,
            pinnedMessage = pinnedMessage,
            currentUser = currentUser,
            allUsers = allUsers,
            allClients = allClients,
            currentClock = currentClock,
            currentLang = currentLang,
            bubbleStyle = bubbleStyle,
            onBackToConversationList = {
                viewModel.closeActiveChatRoom()
            },
            onOpenBubbleStylePicker = {
                showBubbleStyleSheet = true
            },
            modifier = modifier
        )
    }

    // --- MODAL DE SÉLECTION DU STYLE DES BULLES ---
    if (showBubbleStyleSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBubbleStyleSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("bubble_style_picker_sheet")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = SecureAccentTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Personnaliser le style des bulles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                BubbleStyle.values().forEach { style ->
                    val isSelected = style == bubbleStyle
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SecureAccentTeal.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SecureAccentTeal) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setChatBubbleStyle(style)
                                showBubbleStyleSheet = false
                            }
                            .testTag("style_${style.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = style.description,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = SecureAccentTeal,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Salon de discussion structuré avec :
 * - Balayage de gauche à droite sur n'importe quel message pour répondre instantanément
 * - Bannière de prévisualisation de réponse au-dessus du compositeur
 * - Barre supérieure avec statut de connexion en direct et indicateurs de sécurité
 * - Compositeur multimédia, vocal et pièces jointes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureChatRoomView(
    viewModel: MainViewModel,
    audioHelper: AudioVoiceHelper,
    roomId: String,
    messages: List<ChatMessageEntity>,
    pinnedMessage: ChatMessageEntity?,
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allClients: List<ClientEntity>,
    currentClock: Long,
    currentLang: com.example.util.AppLanguage,
    bubbleStyle: BubbleStyle,
    onBackToConversationList: () -> Unit,
    onOpenBubbleStylePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAccessAllowed = viewModel.canUserAccessChatRoom(currentUser, roomId, allClients)
    if (!isAccessAllowed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Text(
                    text = "Accès Restreint & Données Protégées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (currentUser?.role == UserRole.GESTIONNAIRE) {
                        "Conformément à la politique stricte de confidentialité de Malaki, les discussions privées entre les clients et leurs agents de crédit sont confidentielles. La direction générale n'y a pas accès."
                    } else {
                        "Vous n'avez pas l'autorisation d'accéder à cette conversation privée."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = onBackToConversationList,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retour aux discussions")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault()) }

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var selectedActionMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var viewingInfoMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var deletingMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId != null) {
            delay(2200L)
            highlightedMessageId = null
        }
    }

    // Regroupement des messages par jour avec en-têtes de dates distinctes (Aujourd'hui, Hier, Date complète)
    val chatListItems = remember(messages) {
        val items = mutableListOf<ChatListItem>()
        var lastDateKey = ""
        val calMsg = Calendar.getInstance()
        val calNow = Calendar.getInstance()
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        messages.forEach { msg ->
            val dateKey = dayKeyFormat.format(Date(msg.timestamp))
            if (dateKey != lastDateKey) {
                lastDateKey = dateKey
                calMsg.timeInMillis = msg.timestamp

                val isToday = calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                        calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                val isYesterday = calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                        calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) - 1

                val dateText = when {
                    isToday -> "Aujourd'hui"
                    isYesterday -> "Hier"
                    calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) -> {
                        SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(Date(msg.timestamp))
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
                    }
                    else -> {
                        SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(Date(msg.timestamp))
                    }
                }
                items.add(ChatListItem.DateHeader(dateText = dateText, dateKey = dateKey))
            }
            items.add(ChatListItem.MessageItem(msg))
        }
        items
    }

    // Pièces jointes & vocal
    var showAttachmentPickerSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var customLocationText by remember { mutableStateOf("Marché de Kituku, Goma") }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }

    // Sélecteur de photos et vidéos (Android Photo Picker moderne sans permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val attachment = resolveUriToPendingAttachment(context, uri, AttachmentType.PHOTO)
            if (attachment != null) {
                pendingAttachment = attachment
                showAttachmentPickerSheet = false
            } else {
                Toast.makeText(context, "Impossible de charger la photo sélectionnée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sélecteur de documents & fichiers (PDF, bordereaux, reçus, etc.)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val attachment = resolveUriToPendingAttachment(context, uri, AttachmentType.DOCUMENT)
            if (attachment != null) {
                pendingAttachment = attachment
                showAttachmentPickerSheet = false
            } else {
                Toast.makeText(context, "Impossible de charger le document sélectionné", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingTimerSeconds by remember { mutableIntStateOf(0) }
    val recordingAmplitudes by audioHelper.recordingAmplitudes.collectAsStateWithLifecycle()
    val isPlayingMessageId by audioHelper.playingMessageId.collectAsStateWithLifecycle()
    val playbackProgress by audioHelper.playbackProgress.collectAsStateWithLifecycle()
    val playbackPositionSeconds by audioHelper.playbackPositionSeconds.collectAsStateWithLifecycle()

    // Permission microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val res = audioHelper.startRecording()
            if (res.isSuccess) {
                isRecordingAudio = true
                recordingTimerSeconds = 0
            } else {
                Toast.makeText(context, "Impossible de démarrer l'enregistrement", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permission microphone requise", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingTimerSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingTimerSeconds++
            }
        }
    }

    // Scroll au dernier message lors de la réception
    LaunchedEffect(roomId, messages.size) {
        viewModel.markCurrentChatAsRead()
        if (chatListItems.isNotEmpty()) {
            listState.animateScrollToItem(chatListItems.size - 1)
        }
    }

    // Détermination de l'interlocuteur selon le type de salon
    val isCommunity = roomId == "COMMUNITY_GROUP"
    val isDirectUserChat = roomId.startsWith("DIRECT_")
    val directOtherUserId = if (isDirectUserChat) {
        val parts = roomId.removePrefix("DIRECT_").split("_")
        val myIds = listOf(currentUser?.id ?: "", "USR-${currentUser?.id ?: ""}", (currentUser?.id ?: "").removePrefix("USR-"))
        parts.find { it !in myIds }
    } else null
    val directOtherUser = if (directOtherUserId != null) {
        allUsers.find { it.id == directOtherUserId || "USR-${it.id}" == directOtherUserId || it.id.removePrefix("USR-") == directOtherUserId }
    } else null
    val directOtherClient = if (directOtherUserId != null) {
        allClients.find { it.id == directOtherUserId || "USR-${it.id}" == directOtherUserId || (directOtherUser != null && it.phone == directOtherUser.phone) }
    } else null

    val currentClient = allClients.find { it.id == roomId }

    val roomTitle = when {
        isCommunity -> "🌍 Communauté & Équipe Malaki"
        currentUser?.role == UserRole.CLIENT && !isDirectUserChat -> {
            val assignedAgent = allUsers.find { it.id == currentClient?.assignedAgentId } ?: allUsers.find { it.role == UserRole.AGENT }
            "Conseiller • ${assignedAgent?.fullName ?: directOtherUser?.fullName ?: "David Baraka"}"
        }
        isDirectUserChat -> directOtherUser?.fullName ?: directOtherClient?.fullName ?: "Discussion directe"
        currentClient != null -> currentClient.fullName
        else -> "Discussion ($roomId)"
    }

    val roomSubtitle = when {
        isCommunity -> "Canal collectif • ${allUsers.size + allClients.size} membres"
        currentUser?.role == UserRole.CLIENT && !isDirectUserChat -> "🔒 Discussion privée avec votre conseiller"
        isDirectUserChat -> if (directOtherClient != null) "🔒 Client • ${directOtherClient.marketZone}" else "🔒 ${directOtherUser?.role?.label ?: "Équipe"} • Discussion privée"
        currentClient != null -> "🔒 Client • ${currentClient.marketZone}"
        else -> "Discussion privée"
    }

    val isOtherOnline = when {
        isCommunity -> true
        isDirectUserChat -> (directOtherUser?.isOnline == true && currentClock - directOtherUser.lastActiveTimestamp < 120000L) ||
                (directOtherClient?.isOnline == true && currentClock - directOtherClient.lastActiveTimestamp < 120000L)
        currentClient != null -> (currentClient.isOnline && currentClock - currentClient.lastActiveTimestamp < 120000L)
        else -> false
    }

    val lastActiveTime = when {
        isDirectUserChat -> directOtherUser?.lastActiveTimestamp ?: currentClock
        currentClient != null -> currentClient.lastActiveTimestamp
        else -> currentClock
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- BARRE SUPÉRIEURE DU SALON SÉCURISÉ (COMPACTE & ÉLÉGANTE) ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToConversationList,
                    modifier = Modifier.size(36.dp).testTag("chat_room_btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour aux discussions",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Avatar du salon compact
                if (isCommunity) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryNavy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    BybitAvatar(
                        avatarKey = directOtherUser?.avatarKey ?: currentClient?.avatarKey,
                        role = directOtherUser?.role ?: UserRole.CLIENT,
                        gender = currentClient?.gender,
                        photoUrl = currentClient?.avatarUrl,
                        isOnline = isOtherOnline,
                        size = 36.dp,
                        showGlow = true
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = roomTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!isCommunity) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SecureTealColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = SecureTealColor,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Protégé",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureTealColor
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        if (!isCommunity) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isOtherOnline) SuccessGreen else Color(0xFF9E9E9E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = PresenceHelper.formatPresence(
                                    isOnline = isOtherOnline,
                                    lastActiveTimestamp = lastActiveTime,
                                    now = currentClock,
                                    lang = currentLang
                                ),
                                fontSize = 10.5.sp,
                                color = if (isOtherOnline) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = roomSubtitle,
                                fontSize = 10.5.sp,
                                color = SecureTealColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenBubbleStylePicker,
                    modifier = Modifier.size(34.dp).testTag("chat_room_btn_style")
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = "Style des bulles",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- BANNIÈRE MESSAGE ÉPINGLÉ ---
        if (pinnedMessage != null) {
            val pinned = pinnedMessage
            Surface(
                color = SecureAccentTeal.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Épinglé",
                        tint = SecureAccentTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Message épinglé • ${pinned.senderName}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecureAccentTeal
                        )
                        Text(
                            text = if (pinned.isVoiceMessage) "🎤 Message vocal (${pinned.voiceDurationSeconds}s)" else pinned.content,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel.togglePinMessage(pinned) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Désépingler", modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // --- CONTENU DU SALON DE DISCUSSION ---
        if (chatListItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SecureAccentTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SecureAccentTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = "Discussion Sécurisée & Chiffrée",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Les messages et pièces jointes échangés dans ce canal sont strictement confidentiels.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("👋 Bonjour !", "Suivi de crédit", "Note vocale 🎤").forEach { promptText ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SecureAccentTeal.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { messageInput = promptText }
                                ) {
                                    Text(
                                        text = promptText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SecureAccentTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- LISTE DES MESSAGES (AVEC BALAYAGE DE GAUCHE À DROITE POUR RÉPONDRE) ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatListItems, key = { it.key }) { item ->
                    when (item) {
                        is ChatListItem.DateHeader -> {
                            ChatDateHeaderChip(dateText = item.dateText)
                        }
                    is ChatListItem.MessageItem -> {
                        val msg = item.message
                        val isMe = msg.senderId == currentUser?.id || (currentUser?.role == UserRole.CLIENT && msg.senderRole == UserRole.CLIENT)
                        val isPlayingThis = isPlayingMessageId == msg.id
                        val isHighlighted = highlightedMessageId == msg.id

                        ChatBubble(
                            message = msg,
                            isMe = isMe,
                            isHighlighted = isHighlighted,
                            formattedTime = timeFormat.format(Date(msg.timestamp)),
                            isPlayingAudio = isPlayingThis,
                            playbackProgress = if (isPlayingThis) playbackProgress else 0f,
                            playbackPositionSeconds = if (isPlayingThis) playbackPositionSeconds else 0,
                            bubbleStyle = bubbleStyle,
                            onTogglePlayAudio = {
                                audioHelper.playVoiceMessage(msg.id, msg.audioFilePath ?: msg.attachmentUri)
                            },
                            onSeekAudio = { fraction ->
                                if (isPlayingThis) audioHelper.seekTo(fraction)
                            },
                            onLongClick = {
                                selectedActionMessage = msg
                            },
                            onSwipeToReply = {
                                replyingToMessage = msg
                                Toast.makeText(context, "Réponse à ${msg.senderName}", Toast.LENGTH_SHORT).show()
                            },
                            onReplySnippetClick = { targetReplyId ->
                                val targetIdx = chatListItems.indexOfFirst {
                                    it is ChatListItem.MessageItem && it.message.id == targetReplyId
                                }
                                if (targetIdx >= 0) {
                                    highlightedMessageId = targetReplyId
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIdx)
                                    }
                                } else {
                                    Toast.makeText(context, "Message d'origine plus ancien ou introuvable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

        // --- BANNIÈRE DE PRÉVISUALISATION DE RÉPONSE (AU-DESSUS DU COMPOSITEUR) ---
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            replyingToMessage?.let { replyTarget ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("reply_preview_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SecureAccentTeal)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = SecureAccentTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Réponse à ${replyTarget.senderName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecureAccentTeal
                                )
                            }
                            Text(
                                text = if (replyTarget.isVoiceMessage) "🎤 Message vocal (${replyTarget.voiceDurationSeconds}s)"
                                else if (!replyTarget.attachmentUri.isNullOrEmpty()) "📎 Pièce jointe : ${replyTarget.content}"
                                else replyTarget.content,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { replyingToMessage = null },
                            modifier = Modifier.size(30.dp).testTag("cancel_reply_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Annuler la réponse",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- COMPOSITEUR MULTIMÉDIA & AUDIO ---
        TelegramMessageComposer(
            messageText = messageInput,
            onMessageChange = { messageInput = it },
            pendingAttachment = pendingAttachment,
            onRemoveAttachment = { pendingAttachment = null },
            isRecordingAudio = isRecordingAudio,
            recordingTimerSeconds = recordingTimerSeconds,
            recordingAmplitudes = recordingAmplitudes,
            onOpenAttachmentPicker = { showAttachmentPickerSheet = true },
            onStartVoiceRecording = {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onCancelVoiceRecording = {
                audioHelper.cancelRecording()
                isRecordingAudio = false
                recordingTimerSeconds = 0
            },
            onSendVoiceRecording = {
                val file = audioHelper.stopRecording()
                isRecordingAudio = false
                if (file != null && file.exists()) {
                    viewModel.sendChatMessage(
                        content = "Message vocal",
                        replyToMessage = replyingToMessage,
                        isVoiceMessage = true,
                        voiceDurationSeconds = recordingTimerSeconds.coerceAtLeast(1),
                        audioFilePath = file.absolutePath
                    )
                    replyingToMessage = null
                }
                recordingTimerSeconds = 0
            },
            onSendMessage = {
                if (messageInput.isNotBlank() || pendingAttachment != null) {
                    viewModel.sendChatMessage(
                        content = messageInput.ifBlank { pendingAttachment?.displayName ?: "Pièce jointe" },
                        attachmentUri = pendingAttachment?.localFilePath ?: pendingAttachment?.uri?.toString(),
                        replyToMessage = replyingToMessage
                    )
                    messageInput = ""
                    pendingAttachment = null
                    replyingToMessage = null
                }
            }
        )
    }

    // --- CONTEXTUAL BOTTOM SHEET (APPUI LONG 2 SECONDES) ---
    if (selectedActionMessage != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val targetMsg = selectedActionMessage!!
        val isMe = targetMsg.senderId == currentUser?.id || (currentUser?.role == UserRole.CLIENT && targetMsg.senderRole == UserRole.CLIENT)
        val elapsed = (currentClock - targetMsg.timestamp).coerceAtLeast(0L)
        val isWithin10Min = elapsed <= 10 * 60 * 1000L

        ModalBottomSheet(
            onDismissRequest = { selectedActionMessage = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chat_context_menu_sheet")
            ) {
                // En-tête aperçu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SecureAccentTeal)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetMsg.senderName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecureAccentTeal
                        )
                        Text(
                            text = if (targetMsg.isVoiceMessage) "🎤 Message vocal (${targetMsg.voiceDurationSeconds}s)" else targetMsg.content,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Répondre à ce message
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    title = "Répondre à ce message",
                    subtitle = "Citer et répondre directement dans le salon",
                    tag = "action_reply",
                    tint = SecureAccentTeal,
                    onClick = {
                        replyingToMessage = targetMsg
                        selectedActionMessage = null
                    }
                )

                // 2. Répondre en privé
                if (isCommunity && targetMsg.senderId != currentUser?.id) {
                    ContextMenuItem(
                        icon = Icons.Default.Lock,
                        title = "Répondre en privé",
                        subtitle = "Ouvrir une discussion 100% privée et confidentielle avec ${targetMsg.senderName}",
                        tag = "action_reply_private",
                        onClick = {
                            selectedActionMessage = null
                            val myId = currentUser?.id ?: "USR-001"
                            val directRoomId = "DIRECT_" + listOf(myId, targetMsg.senderId).sorted().joinToString("_")
                            viewModel.openChat(directRoomId)
                            replyingToMessage = targetMsg
                        }
                    )
                }

                // 3. Copier
                ContextMenuItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copier",
                    subtitle = "Copier le texte dans le presse-papiers",
                    tag = "action_copy",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", targetMsg.content))
                        selectedActionMessage = null
                        Toast.makeText(context, "Message copié !", Toast.LENGTH_SHORT).show()
                    }
                )

                // 4. Épingler
                ContextMenuItem(
                    icon = Icons.Default.PushPin,
                    title = if (targetMsg.isPinned) "Désépingler" else "Épingler ce message",
                    subtitle = "Garder visible en haut de la discussion",
                    tag = "action_pin",
                    onClick = {
                        viewModel.togglePinMessage(targetMsg)
                        selectedActionMessage = null
                    }
                )

                // 5. Infos délivrabilité
                ContextMenuItem(
                    icon = Icons.Default.Info,
                    title = "Infos",
                    subtitle = "Délivrabilité : liste des membres ayant lu",
                    tag = "action_info",
                    tint = AccentSkyBlue,
                    onClick = {
                        viewingInfoMessage = targetMsg
                        selectedActionMessage = null
                    }
                )

                // 6. Modifier (< 10 min)
                if (isMe && !targetMsg.isVoiceMessage) {
                    ContextMenuItem(
                        icon = Icons.Default.Edit,
                        title = "Modifier",
                        subtitle = if (isWithin10Min) "Modifier le message (autorisé < 10 min)" else "Délai de modification dépassé (> 10 min)",
                        enabled = isWithin10Min,
                        tag = "action_edit",
                        onClick = {
                            editingMessage = targetMsg
                            editTextValue = targetMsg.content
                            selectedActionMessage = null
                        }
                    )
                }

                // 7. Supprimer
                if (isMe || currentUser?.role == UserRole.GESTIONNAIRE) {
                    ContextMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Supprimer",
                        subtitle = "Supprimer définitivement ce message",
                        tag = "action_delete",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            deletingMessage = targetMsg
                            selectedActionMessage = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialogues complémentaires (Infos, Modifier, Supprimer)
    if (viewingInfoMessage != null) {
        val msg = viewingInfoMessage!!
        AlertDialog(
            onDismissRequest = { viewingInfoMessage = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = SecureAccentTeal, modifier = Modifier.size(32.dp)) },
            title = { Text("Délivrabilité du message", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Envoyé : ${fullDateFormat.format(Date(msg.timestamp))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = msg.content, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = if (msg.isRead) Color(0xFF34B7F1) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (msg.isRead) "Lu par le destinataire (2v bleus)" else "Envoyé / Remis au serveur (1v)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.isRead) Color(0xFF00796B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingInfoMessage = null }) { Text("OK") }
            }
        )
    }

    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Modifier le message", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nouveau texte") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingMessage!!
                        viewModel.editChatMessage(target.id, editTextValue)
                        editingMessage = null
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("Annuler") }
            }
        )
    }

    if (deletingMessage != null) {
        val target = deletingMessage!!
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            title = { Text("Supprimer le message ?", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous vraiment supprimer ce message de la discussion ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatMessage(target.id)
                        deletingMessage = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) { Text("Annuler") }
            }
        )
    }

    // --- FEUILLE DE SÉLECTION DES PIÈCES JOINTES (PHOTOS, DOCUMENTS, LOCALISATION) ---
    if (showAttachmentPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAttachmentPickerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("attachment_picker_sheet")
            ) {
                Text(
                    text = "Partager un fichier dans le chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sélectionnez une photo, un document ou votre localisation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1 : Photos & Vidéos
                    AttachmentOptionCard(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Photo / Vidéo",
                        sublabel = "Galerie",
                        tint = Color(0xFF2563EB),
                        backgroundColor = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f),
                        testTag = "btn_attach_photo",
                        onClick = {
                            try {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erreur sélecteur photo: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Option 2 : Documents & Reçus
                    AttachmentOptionCard(
                        icon = Icons.Default.Description,
                        label = "Document",
                        sublabel = "PDF, Reçu",
                        tint = Color(0xFFD97706),
                        backgroundColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f),
                        testTag = "btn_attach_document",
                        onClick = {
                            try {
                                documentPickerLauncher.launch("*/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erreur sélecteur document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Option 3 : Localisation
                    AttachmentOptionCard(
                        icon = Icons.Default.LocationOn,
                        label = "Position",
                        sublabel = "Point de vente",
                        tint = Color(0xFF0F5132),
                        backgroundColor = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f),
                        testTag = "btn_attach_location",
                        onClick = {
                            showAttachmentPickerSheet = false
                            showLocationDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialogue de partage de localisation / zone commerciale
    if (showLocationDialog) {
        val predefinedMarkets = listOf(
            "Marché de Kituku, Goma",
            "Marché de Birere, Quartier Mapendo",
            "Marché Central de Virunga",
            "Point de vente Quartier Katindo",
            "Zone commerciale Majengo"
        )
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0F5132), modifier = Modifier.size(32.dp)) },
            title = { Text("Partager une localisation", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Choisissez votre zone de commerce ou saisissez l'adresse :", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    predefinedMarkets.take(3).forEach { market ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (customLocationText == market) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (customLocationText == market) BorderStroke(1.dp, Color(0xFF0F5132)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { customLocationText = market }
                        ) {
                            Text(
                                text = market,
                                fontSize = 12.5.sp,
                                fontWeight = if (customLocationText == market) FontWeight.Bold else FontWeight.Normal,
                                color = if (customLocationText == market) Color(0xFF0F5132) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLocationText,
                        onValueChange = { customLocationText = it },
                        label = { Text("Adresse personnalisée") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val loc = customLocationText.ifBlank { "Goma, Nord-Kivu" }
                        pendingAttachment = PendingAttachment(
                            type = AttachmentType.LOCATION,
                            localFilePath = "geo:0,0?q=${Uri.encode(loc)}",
                            displayName = "📍 $loc",
                            fileSizeFormatted = "Position partagée",
                            mimeType = "text/plain",
                            locationTitle = loc,
                            locationCoordinates = loc
                        )
                        showLocationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                ) { Text("Partager cette position") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun AttachmentOptionCard(
    icon: ImageVector,
    label: String,
    sublabel: String,
    tint: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = sublabel,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun resolveUriToPendingAttachment(
    context: Context,
    uri: Uri,
    forcedType: AttachmentType? = null
): PendingAttachment? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        var displayName = if (forcedType == AttachmentType.PHOTO) "Photo_${System.currentTimeMillis()}.jpg" else "Document_${System.currentTimeMillis()}"
        var fileSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        // Enregistrement local sécurisé et durable dans le stockage interne privé de l'application
        val durableDir = File(context.filesDir, "chat_attachments").apply { if (!exists()) mkdirs() }
        val safeName = "${System.currentTimeMillis()}_${displayName.replace(" ", "_")}"
        val targetFile = File(durableDir, safeName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        val resolvedType = forcedType ?: when {
            mimeType.startsWith("image/") -> AttachmentType.PHOTO
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            else -> AttachmentType.DOCUMENT
        }

        val actualSize = if (fileSize > 0) fileSize else targetFile.length()
        val sizeFormatted = when {
            actualSize > 1024 * 1024 -> "${"%.1f".format(Locale.US, actualSize / (1024f * 1024f))} Mo"
            actualSize > 1024 -> "${actualSize / 1024} Ko"
            actualSize > 0 -> "$actualSize o"
            else -> "Document joint"
        }

        PendingAttachment(
            type = resolvedType,
            localFilePath = targetFile.absolutePath,
            displayName = displayName,
            fileSizeFormatted = sizeFormatted,
            mimeType = mimeType,
            uri = uri
        )
    } catch (e: Exception) {
        Log.e("ChatScreen", "Erreur lors du traitement de la pièce jointe: ${e.message}", e)
        null
    }
}

/**
 * Bulle de message style WhatsApp avec :
 * - Geste de balayage de gauche à droite pour "Répondre rapidement"
 * - Indicateur visuel circulaire animé (flèche WhatsApp) lors du balayage
 * - Retour haptique au franchissement du seuil
 * - Aperçu de citation cliquable au-dessus du contenu
 * - Statut d'envoi 1V / 2V
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    isHighlighted: Boolean = false,
    formattedTime: String,
    isPlayingAudio: Boolean,
    playbackProgress: Float = 0f,
    playbackPositionSeconds: Int = 0,
    bubbleStyle: BubbleStyle = BubbleStyle.TEXTRA_ROUNDED,
    onTogglePlayAudio: () -> Unit,
    onSeekAudio: (Float) -> Unit = {},
    onLongClick: () -> Unit,
    onSwipeToReply: () -> Unit = {},
    onReplySnippetClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current

    val triggerThresholdPx = remember(density) { with(density) { 44.dp.toPx() } }
    val maxSwipePx = remember(density) { with(density) { 76.dp.toPx() } }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val resolvedShape = bubbleStyle.resolveShape(isMe)
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) SecureAccentTeal.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(durationMillis = 350),
        label = "highlightColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        hasTriggeredHaptic = false
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value >= triggerThresholdPx) {
                                onSwipeToReply()
                            }
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        // Balayage exclusif de gauche à droite
                        if (dragAmount > 0 || offsetX.value > 0f) {
                            val target = (offsetX.value + dragAmount).coerceIn(0f, maxSwipePx)
                            if (target != offsetX.value) {
                                change.consume()
                                if (target >= triggerThresholdPx && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasTriggeredHaptic = true
                                }
                                coroutineScope.launch {
                                    offsetX.snapTo(target)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // --- INDICATEUR VISUEL CIRCULAIRE WHATSAPP (FLÈCHE DE RÉPONSE) ---
        val currentOffset = offsetX.value
        if (currentOffset > 4f) {
            val progress = (currentOffset / triggerThresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .graphicsLayer {
                        scaleX = 0.5f + (0.5f * progress)
                        scaleY = 0.5f + (0.5f * progress)
                        alpha = progress
                    }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (progress >= 1f) SecureAccentTeal else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Répondre rapidement",
                            tint = if (progress >= 1f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- BULLE DÉPLACÉE AVEC LE BALAYAGE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) },
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (!isMe) {
                Text(
                    text = "${message.senderName} (${message.senderRole.label})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecureAccentTeal,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Card(
                shape = resolvedShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .combinedClickable(
                        onClick = {
                            if (message.isVoiceMessage) {
                                onTogglePlayAudio()
                            }
                        },
                        onLongClick = onLongClick
                    )
                    .then(
                        if (isHighlighted) {
                            Modifier
                                .border(2.5.dp, SecureAccentTeal, resolvedShape)
                                .background(highlightColor, resolvedShape)
                        } else if (!isMe) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                resolvedShape
                            )
                        } else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    // Badge épinglé
                    if (message.isPinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isMe) Color(0xFFFFD54F) else SecureAccentTeal,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Épinglé",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color(0xFFFFD54F) else SecureAccentTeal
                            )
                        }
                    }

                    // CITATION DE RÉPONSE CLIQUEABLE DANS LA BULLE
                    if (!message.replyToMessageId.isNullOrEmpty() && !message.replyToSenderName.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    message.replyToMessageId?.let(onReplySnippetClick)
                                }
                                .padding(bottom = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isMe) Color(0xFFFFD54F) else SecureAccentTeal)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color(0xFFFFD54F) else SecureAccentTeal
                                    )
                                    Text(
                                        text = message.replyToContentSnippet ?: "Message",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Contenu : Vocal, Média ou Texte
                    if (message.isVoiceMessage) {
                        VoiceMessageBubble(
                            durationSeconds = message.voiceDurationSeconds,
                            isPlaying = isPlayingAudio,
                            playbackProgress = playbackProgress,
                            playbackPositionSeconds = playbackPositionSeconds,
                            isMe = isMe,
                            onTogglePlay = onTogglePlayAudio,
                            onSeek = onSeekAudio
                        )
                    } else if (!message.attachmentUri.isNullOrEmpty()) {
                        MediaAttachmentBubble(
                            attachmentUri = message.attachmentUri,
                            contentDescription = message.content,
                            isMe = isMe
                        )
                        if (message.content.isNotBlank() &&
                            !message.content.startsWith("Photo_") &&
                            !message.content.startsWith("Video_") &&
                            !message.content.startsWith("Document_")
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.content,
                                fontSize = 13.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = message.content,
                            fontSize = 13.sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Heure et statut WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isEdited) {
                            Text(
                                text = "(modifié)",
                                fontSize = 9.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 10.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isMe) {
                                if (message.isRead) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Message lu",
                                        tint = Color(0xFF64FFDA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Message distribué",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) tint else tint.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
