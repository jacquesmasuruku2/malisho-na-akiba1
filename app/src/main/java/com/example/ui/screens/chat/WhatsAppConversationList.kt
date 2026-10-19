package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ClientEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.BybitAvatar
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modèle de synthèse de conversation sécurisée
 */
data class SecureConversationSummary(
    val roomId: String,
    val title: String,
    val subtitle: String,
    val isCommunityGroup: Boolean,
    val interlocutorUser: UserEntity? = null,
    val interlocutorClient: ClientEntity? = null,
    val isOnline: Boolean = false,
    val lastActiveTimestamp: Long = 0L,
    val lastMessage: ChatMessageEntity? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
)

typealias WhatsAppConversationSummary = SecureConversationSummary

private val SecureTeal = Color(0xFF0F766E)
private val SecureDarkTeal = Color(0xFF115E59)
private val SecureAccentBlue = Color(0xFF0284C7)
private val SecureBadgeColor = Color(0xFF0D9488)

/**
 * Écran d'accueil de la messagerie sécurisée :
 * - En-tête professionnel avec recherche rapide
 * - Filtres : Toutes, Communauté, Directs / Privées, Non lues
 * - Discussion Communautaire épinglée pour toute l'équipe et les membres
 * - Discussions privées 1-à-1 avec agents ou collègues
 * - Sécurité stricte : les messages privés des clients sont inaccessibles au gestionnaire
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureConversationList(
    currentUser: UserEntity?,
    conversations: List<SecureConversationSummary>,
    allUsers: List<UserEntity>,
    allClients: List<ClientEntity>,
    onOpenConversation: (String) -> Unit,
    onOpenCommunityChat: () -> Unit,
    onOpenBubbleStylePicker: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, COMMUNITY, PRIVATE, UNREAD
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val isClient = currentUser?.role == UserRole.CLIENT
    val isManager = currentUser?.role == UserRole.GESTIONNAIRE

    val filteredConversations = remember(conversations, selectedFilter, searchQuery) {
        conversations.filter { conv ->
            val matchesFilter = when (selectedFilter) {
                "COMMUNITY" -> conv.isCommunityGroup
                "PRIVATE" -> !conv.isCommunityGroup
                "UNREAD" -> conv.unreadCount > 0
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                conv.title.contains(searchQuery, ignoreCase = true) ||
                        conv.subtitle.contains(searchQuery, ignoreCase = true) ||
                        (conv.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true)
            }

            matchesFilter && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- TOP BAR SÉCURISÉE (COMPACTE & ÉLÉGANTE) ---
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Rechercher un échange ou message...", fontSize = 12.5.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("secure_search_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecureTeal,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        isSearching = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Fermer recherche", modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Discussions",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SecureTeal.copy(alpha = 0.14f),
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = SecureTeal,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Protégé",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecureTeal
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${currentUser?.fullName ?: "Utilisateur"} • ${currentUser?.role?.label ?: ""}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { isSearching = true },
                                modifier = Modifier.size(34.dp).testTag("secure_btn_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Rechercher",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box {
                                IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Nouvelle discussion privée") },
                                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = SecureTeal) },
                                        onClick = {
                                            showMoreMenu = false
                                            showNewChatSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Canal Communautaire") },
                                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = PrimaryNavy) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenCommunityChat()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Personnaliser l'affichage") },
                                        leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenBubbleStylePicker()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tout marquer comme lu") },
                                        leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null, tint = SuccessGreen) },
                                        onClick = {
                                            showMoreMenu = false
                                            onMarkAllAsRead()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // --- BANNIÈRE DISCRÈTE DE CONFIDENTIALITÉ SI GESTIONNAIRE ---
                    if (isManager) {
                        Surface(
                            color = PrimaryNavy.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Confidentialité : Les échanges privés des clients sont confidentiels.",
                                    fontSize = 10.sp,
                                    color = PrimaryNavy,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // --- FILTRES DE MESSAGERIE (COMPACTS) ---
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            SecureFilterChip(
                                label = "Toutes",
                                count = conversations.size,
                                isSelected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                testTag = "filter_all"
                            )
                        }
                        item {
                            SecureFilterChip(
                                label = "Communauté",
                                count = conversations.count { it.isCommunityGroup },
                                isSelected = selectedFilter == "COMMUNITY",
                                onClick = { selectedFilter = "COMMUNITY" },
                                testTag = "filter_community"
                            )
                        }
                        item {
                            SecureFilterChip(
                                label = "Directs",
                                count = conversations.count { !it.isCommunityGroup },
                                isSelected = selectedFilter == "PRIVATE",
                                onClick = { selectedFilter = "PRIVATE" },
                                testTag = "filter_private"
                            )
                        }
                        val unreadTotal = conversations.sumOf { it.unreadCount }
                        item {
                            SecureFilterChip(
                                label = "Non lues",
                                count = unreadTotal,
                                isSelected = selectedFilter == "UNREAD",
                                isBadgeAccent = unreadTotal > 0,
                                onClick = { selectedFilter = "UNREAD" },
                                testTag = "filter_unread"
                            )
                        }
                    }
                }
            }

            // --- LISTE DES DISCUSSIONS SÉCURISÉES ---
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SecureTeal.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = SecureTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Aucun échange trouvé pour « $searchQuery »" else "Aucune discussion dans cette catégorie",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isClient) "Vous pouvez échanger sur le canal de la communauté ou écrire directement à votre conseiller de crédit."
                            else "Sélectionnez le bouton (+) pour initier une discussion sécurisée.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("secure_conversation_list")
                ) {
                    items(filteredConversations, key = { it.roomId }) { item ->
                        SecureConversationRow(
                            summary = item,
                            currentUserId = currentUser?.id ?: "",
                            onClick = {
                                if (item.isCommunityGroup) {
                                    onOpenCommunityChat()
                                } else {
                                    onOpenConversation(item.roomId)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp, end = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // --- BOUTON FLOTTANT : NOUVELLE DISCUSSION SÉCURISÉE ---
        FloatingActionButton(
            onClick = { showNewChatSheet = true },
            containerColor = SecureTeal,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("secure_fab_new_chat")
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Nouvelle discussion sécurisée",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // --- BOTTOM SHEET POUR DÉMARRER UNE DISCUSSION SÉCURISÉE ---
    if (showNewChatSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNewChatSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            NewChatBottomSheetContent(
                currentUser = currentUser,
                allUsers = allUsers,
                allClients = allClients,
                onSelectUser = { targetUser ->
                    showNewChatSheet = false
                    val myId = currentUser?.id ?: "USR-001"
                    val directId = "DIRECT_" + listOf(myId, targetUser.id).sorted().joinToString("_")
                    onOpenConversation(directId)
                },
                onSelectClient = { targetClient ->
                    showNewChatSheet = false
                    val myId = currentUser?.id ?: "USR-001"
                    if (currentUser?.role == UserRole.AGENT && (targetClient.assignedAgentId == null || targetClient.assignedAgentId == myId || targetClient.marketZone == currentUser?.marketZone)) {
                        onOpenConversation(targetClient.id)
                    } else {
                        val targetId = targetClient.id
                        val directId = "DIRECT_" + listOf(myId, targetId).sorted().joinToString("_")
                        onOpenConversation(directId)
                    }
                },
                onSelectCommunity = {
                    showNewChatSheet = false
                    onOpenCommunityChat()
                }
            )
        }
    }
}

/**
 * Backward compatibility alias
 */
@Composable
fun WhatsAppConversationList(
    currentUser: UserEntity?,
    conversations: List<SecureConversationSummary>,
    allUsers: List<UserEntity>,
    allClients: List<ClientEntity>,
    onOpenConversation: (String) -> Unit,
    onOpenCommunityChat: () -> Unit,
    onOpenBubbleStylePicker: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureConversationList(
        currentUser = currentUser,
        conversations = conversations,
        allUsers = allUsers,
        allClients = allClients,
        onOpenConversation = onOpenConversation,
        onOpenCommunityChat = onOpenCommunityChat,
        onOpenBubbleStylePicker = onOpenBubbleStylePicker,
        onMarkAllAsRead = onMarkAllAsRead,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun SecureFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    isBadgeAccent: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) SecureTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SecureTeal) else null,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SecureDarkTeal else MaterialTheme.colorScheme.onSurface
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (isBadgeAccent) SecureBadgeColor else if (isSelected) SecureDarkTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = count.toString(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ligne d'une discussion sécurisée
 */
@Composable
fun SecureConversationRow(
    summary: SecureConversationSummary,
    currentUserId: String,
    onClick: () -> Unit
) {
    val lastMsg = summary.lastMessage
    val isMeSender = lastMsg?.senderId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("conversation_item_${summary.roomId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(52.dp)) {
            if (summary.isCommunityGroup) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryNavy,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Communauté",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                BybitAvatar(
                    avatarKey = summary.interlocutorUser?.avatarKey ?: summary.interlocutorClient?.avatarKey,
                    role = summary.interlocutorUser?.role ?: UserRole.CLIENT,
                    gender = summary.interlocutorClient?.gender,
                    photoUrl = summary.interlocutorClient?.avatarUrl,
                    isOnline = summary.isOnline,
                    size = 52.dp,
                    showGlow = false
                )
            }

            // Épinglé badge
            if (summary.isPinned) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Épinglé",
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Corps : Titre et aperçu dernier message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.title,
                    fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Date/Heure
                if (lastMsg != null) {
                    Text(
                        text = formatConversationTimestamp(lastMsg.timestamp),
                        fontSize = 11.sp,
                        fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (summary.unreadCount > 0) SecureTeal else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coche d'envoi si envoyé par moi
                    if (isMeSender && lastMsg != null) {
                        if (lastMsg.isRead) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Lu",
                                tint = SecureAccentBlue,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 3.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Envoyé",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 3.dp)
                            )
                        }
                    }

                    // Aperçu : Vocal, Fichier ou Texte
                    if (lastMsg?.isVoiceMessage == true) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = SecureTeal,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = "Message vocal (${lastMsg.voiceDurationSeconds}s)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (!lastMsg?.attachmentUri.isNullOrEmpty()) {
                        Text(
                            text = "📎 " + (lastMsg?.content?.ifBlank { "Pièce jointe" } ?: "Pièce jointe"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val senderPrefix = if (summary.isCommunityGroup && lastMsg != null && !isMeSender) {
                            "${lastMsg.senderName.substringBefore(' ')}: "
                        } else ""
                        Text(
                            text = senderPrefix + (lastMsg?.content ?: summary.subtitle),
                            fontSize = 13.sp,
                            color = if (summary.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (summary.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Badge non lus
                if (summary.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = SecureBadgeColor,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (summary.unreadCount > 99) "99+" else summary.unreadCount.toString(),
                                fontSize = 10.sp,
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

/**
 * Contenu du BottomSheet pour démarrer une nouvelle conversation sécurisée :
 * - Mode WhatsApp / Telegram : Recherche par Numéro de téléphone ou Email avec vérification dans la base de données
 * - Recherche parmi TOUS les utilisateurs de l'application (Staff, Conseillers, Caissiers et Clients)
 * - Démarrage instantané de la discussion dès qu'un numéro ou email correspondant est trouvé
 */
@Composable
fun NewChatBottomSheetContent(
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    allClients: List<ClientEntity>,
    onSelectUser: (UserEntity) -> Unit,
    onSelectClient: (ClientEntity) -> Unit,
    onSelectCommunity: () -> Unit
) {
    var searchContactQuery by remember { mutableStateOf("") }
    var phoneOrEmailInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("PHONE_EMAIL") } // PHONE_EMAIL, ALL, TEAM, CLIENTS, COMMUNITY

    val myUserId = currentUser?.id ?: ""
    val myPhone = currentUser?.phone ?: ""

    // Normalisation pour la recherche par téléphone ou e-mail
    val cleanDirectQuery = phoneOrEmailInput.trim()
    val directDigits = cleanDirectQuery.filter { it.isDigit() }
    val isDirectEmail = cleanDirectQuery.contains("@")

    // Recherche directe par téléphone ou email dans la base de données
    val directMatchedUsers = remember(phoneOrEmailInput, allUsers, myUserId) {
        if (cleanDirectQuery.length < 3) emptyList()
        else allUsers.filter { user ->
            user.id != myUserId && (
                (directDigits.length >= 4 && user.phone.filter { it.isDigit() }.contains(directDigits)) ||
                (user.email.isNotEmpty() && user.email.contains(cleanDirectQuery, ignoreCase = true)) ||
                user.phone.contains(cleanDirectQuery) ||
                (user.username.isNotEmpty() && user.username.contains(cleanDirectQuery, ignoreCase = true)) ||
                user.fullName.contains(cleanDirectQuery, ignoreCase = true)
            )
        }
    }

    val directMatchedClients = remember(phoneOrEmailInput, allClients, myPhone) {
        if (cleanDirectQuery.length < 3) emptyList()
        else allClients.filter { client ->
            client.phone != myPhone && (
                (directDigits.length >= 4 && client.phone.filter { it.isDigit() }.contains(directDigits)) ||
                (client.email.isNotEmpty() && client.email.contains(cleanDirectQuery, ignoreCase = true)) ||
                client.phone.contains(cleanDirectQuery) ||
                client.fullName.contains(cleanDirectQuery, ignoreCase = true)
            )
        }
    }

    val hasDirectMatches = directMatchedUsers.isNotEmpty() || directMatchedClients.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(max = 620.dp)
            .testTag("new_chat_sheet")
    ) {
        // En-tête de la modale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SecureTeal.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = SecureTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nouvelle discussion sécurisée",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Recherchez un utilisateur ou entrez son numéro / e-mail",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Onglets de navigation
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "PHONE_EMAIL" to "📱 N° Tél ou Email",
                "ALL" to "👥 Tous les contacts (${allUsers.size + allClients.size})",
                "TEAM" to "💼 Équipe & Staff (${allUsers.filter { it.role != UserRole.CLIENT }.size})",
                "CLIENTS" to "🤝 Clients (${allClients.size})",
                "COMMUNITY" to "🌍 Communauté"
            )

            items(tabs) { (tabKey, tabLabel) ->
                val isSelected = selectedTab == tabKey
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SecureTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { selectedTab = tabKey }
                ) {
                    Text(
                        text = tabLabel,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CONTENU SELON L'ONGLET SÉLECTIONNÉ
        when (selectedTab) {
            "PHONE_EMAIL" -> {
                // =========================================================================
                // MODE WHATSAPP / TELEGRAM : RECHERCHE PAR NUMÉRO DE TÉLÉPHONE OU EMAIL
                // =========================================================================
                OutlinedTextField(
                    value = phoneOrEmailInput,
                    onValueChange = { phoneOrEmailInput = it },
                    placeholder = { Text("Ex: +243 999 123 456 ou nom@malaki.cd", fontSize = 13.sp) },
                    label = { Text("Numéro de téléphone ou adresse e-mail") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isDirectEmail) Icons.Default.Email else Icons.Default.Phone,
                            contentDescription = null,
                            tint = SecureTeal
                        )
                    },
                    trailingIcon = {
                        if (phoneOrEmailInput.isNotEmpty()) {
                            IconButton(onClick = { phoneOrEmailInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_phone_email_search_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecureTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Si des correspondances sont trouvées dans la base de données
                    if (hasDirectMatches) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SuccessGreen.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Utilisateur trouvé dans la base de données de Malaki !",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }

                        // Utilisateurs staff / comptes trouvés
                        items(directMatchedUsers) { user ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, SecureTeal.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BybitAvatar(
                                            avatarKey = user.avatarKey,
                                            role = user.role,
                                            isOnline = user.isOnline,
                                            size = 44.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.fullName,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${user.role.label} • ${user.marketZone.ifEmpty { "Direction" }}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (user.phone.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SecureTeal, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(user.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                if (user.email.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Email, contentDescription = null, tint = SecureTeal, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(user.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { onSelectUser(user) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecureTeal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("start_chat_with_user_${user.id}")
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Démarrer la discussion avec ${user.fullName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Clients trouvés
                        items(directMatchedClients) { client ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, SecureTeal.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BybitAvatar(
                                            avatarKey = client.avatarKey,
                                            role = UserRole.CLIENT,
                                            gender = client.gender,
                                            photoUrl = client.avatarUrl,
                                            isOnline = client.isOnline,
                                            size = 44.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = client.fullName,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Client • ${client.marketZone} • ${client.activityType}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (client.phone.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = SecureTeal, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(client.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                if (client.email.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Email, contentDescription = null, tint = SecureTeal, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(client.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { onSelectClient(client) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecureTeal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("start_chat_with_client_${client.id}")
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Démarrer la discussion avec ${client.fullName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else if (cleanDirectQuery.length >= 4) {
                        // 2. Aucun contact correspondant trouvé dans la base de données
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Aucun utilisateur trouvé",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Le numéro ou l'e-mail \"$cleanDirectQuery\" n'est pas encore enregistré dans la base de données de Malaki. Assurez-vous d'avoir saisi le bon numéro ou vérifiez dans l'annuaire complet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // 3. Invite d'explication WhatsApp / Télégram & contacts récents
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SecureTeal.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = SecureTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Comme sur WhatsApp et Telegram, tapez le numéro de téléphone (+243...) ou l'adresse email d'un utilisateur pour vérifier sa présence dans la base et débuter immédiatement l'échange.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.5.sp
                                    )
                                }
                            }
                        }

                        // Suggestions rapides parmi les contacts de l'équipe
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SUGGESTIONS RAPIDES DANS L'ANNUAIRE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        val quickSuggestions = allUsers.filter { it.id != myUserId }.take(4)
                        items(quickSuggestions) { u ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(u) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BybitAvatar(avatarKey = u.avatarKey, role = u.role, isOnline = u.isOnline, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.fullName, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${u.role.label} • ${u.phone.ifEmpty { u.email }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "Chater",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureTeal
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }

            else -> {
                // =========================================================================
                // ANNUAIRE COMPLET : TOUS LES UTILISATEURS, ÉQUIPE OU CLIENTS
                // =========================================================================
                OutlinedTextField(
                    value = searchContactQuery,
                    onValueChange = { searchContactQuery = it },
                    placeholder = { Text("Rechercher par nom, téléphone, e-mail ou zone...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SecureTeal) },
                    trailingIcon = {
                        if (searchContactQuery.isNotEmpty()) {
                            IconButton(onClick = { searchContactQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("directory_search_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecureTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option Communauté
                    if (selectedTab == "ALL" || selectedTab == "COMMUNITY") {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryNavy.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onSelectCommunity)
                                    .testTag("contact_community_group")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryNavy,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🌍 Discussion Communautaire",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNavy
                                        )
                                        Text(
                                            text = "Canal général de l'équipe et annonces",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PrimaryNavy.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Ouvrir",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Équipe & Utilisateurs
                    if (selectedTab == "ALL" || selectedTab == "TEAM") {
                        val filteredUsers = allUsers.filter { user ->
                            user.id != myUserId &&
                                    (searchContactQuery.isBlank() ||
                                            user.fullName.contains(searchContactQuery, ignoreCase = true) ||
                                            user.phone.contains(searchContactQuery) ||
                                            user.email.contains(searchContactQuery, ignoreCase = true) ||
                                            user.marketZone.contains(searchContactQuery, ignoreCase = true))
                        }

                        if (filteredUsers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ÉQUIPE & STAFF (${filteredUsers.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                )
                            }

                            items(filteredUsers) { user ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectUser(user) }
                                        .testTag("contact_user_${user.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BybitAvatar(
                                            avatarKey = user.avatarKey,
                                            role = user.role,
                                            isOnline = user.isOnline,
                                            size = 40.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.fullName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${user.role.label} • ${user.marketZone.ifEmpty { "Direction" }} • ${user.phone.ifEmpty { user.email }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SecureTeal.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Discuter",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecureTeal,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Clients
                    if (selectedTab == "ALL" || selectedTab == "CLIENTS") {
                        val filteredClients = allClients.filter { client ->
                            client.phone != myPhone &&
                                    (searchContactQuery.isBlank() ||
                                            client.fullName.contains(searchContactQuery, ignoreCase = true) ||
                                            client.phone.contains(searchContactQuery) ||
                                            client.email.contains(searchContactQuery, ignoreCase = true) ||
                                            client.marketZone.contains(searchContactQuery, ignoreCase = true))
                        }

                        if (filteredClients.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "CLIENTS DU RÉSEAU (${filteredClients.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                )
                            }

                            items(filteredClients) { client ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectClient(client) }
                                        .testTag("contact_client_${client.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BybitAvatar(
                                            avatarKey = client.avatarKey,
                                            role = UserRole.CLIENT,
                                            gender = client.gender,
                                            photoUrl = client.avatarUrl,
                                            isOnline = client.isOnline,
                                            size = 40.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = client.fullName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Client • ${client.marketZone} (${client.phone})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SecureTeal.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Discuter",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecureTeal,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Formatage des heures/dates selon les conventions standard de messagerie
 */
private fun formatConversationTimestamp(timestamp: Long): String {
    val calMsg = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calNow = Calendar.getInstance()

    return when {
        calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) - 1 -> {
            "Hier"
        }
        else -> {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
