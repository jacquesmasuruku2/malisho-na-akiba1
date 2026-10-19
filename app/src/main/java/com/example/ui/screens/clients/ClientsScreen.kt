package com.example.ui.screens.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClientEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.ClientAvatar
import com.example.ui.screens.loans.AssignCreditDialog
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.components.ClientStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ClientsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAssignCreditDialog by remember { mutableStateOf(false) }
    var clientForCreditAssignment by remember { mutableStateOf<ClientEntity?>(null) }
    var newlyRegisteredClient by remember { mutableStateOf<ClientEntity?>(null) }
    var showEligiblePrompt by remember { mutableStateOf(false) }

    val filteredClients = clients.filter { client ->
        val matchesSearch = client.fullName.contains(searchQuery, ignoreCase = true) ||
                client.phone.contains(searchQuery, ignoreCase = true) ||
                client.marketZone.contains(searchQuery, ignoreCase = true) ||
                client.activityType.contains(searchQuery, ignoreCase = true) ||
                client.id.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Actifs" -> client.status == "ACTIF"
            "En retard" -> client.status == "EN_RETARD"
            "Mes clients" -> client.assignedAgentId == currentUser?.id
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar - Compact & Responsive
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Tafuta mteja, simu, soko, ID..." else "Rechercher client, téléphone, zone, ID...",
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips - Horizontally scrollable for small screens!
            val filterOptions = if (currentLang == AppLanguage.SWAHILI) {
                listOf("Tous" to "Wote", "Actifs" to "Wazuri", "En retard" to "Walio chelewa", "Mes clients" to "Wateja wangu")
            } else {
                listOf("Tous" to "Tous", "Actifs" to "Actifs", "En retard" to "En retard", "Mes clients" to "Mes clients")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                filterOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Responsive Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredClients.size} ${if (currentLang == AppLanguage.SWAHILI) "wateja" else "client(s)"}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val lateCount = filteredClients.count { it.status.contains("RETARD", true) }
                if (lateCount > 0) {
                    Text(
                        text = "$lateCount ${if (currentLang == AppLanguage.SWAHILI) "wamechelewa" else "en retard"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                } else {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Wote wako sawa" else "Tous à jour",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clients List
            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Hakuna mteja anayelingana na utafutaji huu." else "Aucun client trouvé pour cette recherche.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredClients) { client ->
                        val isManager = currentUser?.role == com.example.data.model.UserRole.GESTIONNAIRE
                        ClientItemCard(
                            client = client,
                            onClick = { viewModel.openClientDetail(client.id) },
                            onChatClick = if (isManager) null else { { viewModel.openChat(client.id) } },
                            onAssignCreditClick = {
                                clientForCreditAssignment = client
                                showAssignCreditDialog = true
                            },
                            currentLanguage = currentLang
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Client FAB (visible to Manager and Field Agent)
        if (currentUser?.role == UserRole.GESTIONNAIRE || currentUser?.role == UserRole.AGENT) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryNavy,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_client_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau client")
            }
        }
    }

    if (showAddDialog) {
        val nextMatricule = remember(clients) {
            viewModel.calculateNextClientId(clients)
        }
        AddClientDialog(
            nextMatricule = nextMatricule,
            onDismiss = { showAddDialog = false },
            currentLanguage = currentLang,
            onConfirm = { name, phone, email, zone, activity, gender, idCard, gName, gPhone, notes ->
                viewModel.registerClient(
                    fullName = name,
                    phone = phone,
                    email = email,
                    marketZone = zone,
                    activityType = activity,
                    gender = gender,
                    idCardNumber = idCard,
                    guarantorName = gName,
                    guarantorPhone = gPhone,
                    notes = notes,
                    onSuccess = { client ->
                        showAddDialog = false
                        newlyRegisteredClient = client
                        showEligiblePrompt = true
                    }
                )
            }
        )
    }

    // Dialogue d'éligibilité immédiate après enregistrement du client
    if (showEligiblePrompt && newlyRegisteredClient != null) {
        val client = newlyRegisteredClient!!
        AlertDialog(
            onDismissRequest = {
                showEligiblePrompt = false
                newlyRegisteredClient = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Mteja Amekubaliwa & Anastahili" else "Client Enregistré & Éligible",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Mteja ${client.fullName} amesajiliwa kikamilifu kwenye kanzidata (database)."
                        else
                            "Le client ${client.fullName} a été enregistré avec succès dans la base de données.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI)
                                    "Hali : Anastahili kupokea mkopo (Eligible) !"
                                else
                                    "Statut : Éligible pour l'octroi d'un crédit !",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                        }
                    }
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Ungependa kumkabidhi mkopo sasa hivi ?"
                        else
                            "Souhaitez-vous lui affecter un crédit dès maintenant ?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEligiblePrompt = false
                        clientForCreditAssignment = client
                        showAssignCreditDialog = true
                        newlyRegisteredClient = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text(if (currentLang == AppLanguage.SWAHILI) "Kabidhi mkopo sasa" else "Affecter un crédit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEligiblePrompt = false
                        newlyRegisteredClient = null
                    }
                ) {
                    Text(if (currentLang == AppLanguage.SWAHILI) "Baadaye" else "Plus tard")
                }
            }
        )
    }

    // Boîte de dialogue pour affecter un crédit au client
    if (showAssignCreditDialog) {
        AssignCreditDialog(
            clients = clients,
            preselectedClient = clientForCreditAssignment,
            onDismiss = {
                showAssignCreditDialog = false
                clientForCreditAssignment = null
            },
            onConfirm = { clientId, amount, durationDays, purpose, paymentMethod, newClientName, newClientPhone, newClientMarket ->
                showAssignCreditDialog = false
                val targetClient = clients.find { it.id == clientId } ?: clientForCreditAssignment
                viewModel.assignCreditToClient(
                    clientId = clientId,
                    principalAmount = amount,
                    durationDays = durationDays,
                    purpose = purpose,
                    marketZone = newClientMarket ?: targetClient?.marketZone ?: "Marché de Kituku",
                    activityType = targetClient?.activityType ?: "Commerce",
                    paymentMethod = paymentMethod,
                    newClientName = newClientName,
                    newClientPhone = newClientPhone,
                    onSuccess = {
                        clientForCreditAssignment = null
                    }
                )
            }
        )
    }
}

@Composable
fun ClientItemCard(
    client: ClientEntity,
    onClick: () -> Unit,
    onChatClick: (() -> Unit)? = null,
    onAssignCreditClick: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    modifier: Modifier = Modifier
) {
    val isLate = client.status.contains("RETARD", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isLate) ErrorRed.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("client_card_${client.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Main Top Row: Avatar + Name + StatusBadge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customized Gender Avatar for Men and Women
                ClientAvatar(
                    gender = client.gender,
                    photoUrl = client.avatarUrl,
                    size = 38.dp,
                    isLate = isLate,
                    showGenderBadge = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Name takes all available width smoothly up to StatusBadge
                Text(
                    text = client.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Status Badge aligned cleanly at top right
                ClientStatusBadge(status = client.status, lang = currentLanguage)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Second Row: Distinct ID badge (e.g. CLI-005) + Market & Activity Info
            // Always rock-solid positioning, never displaced by long client names!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp)
            ) {
                val quantiemeLabel = com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(client.id, currentLanguage)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = client.id,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (quantiemeLabel.isNotEmpty()) {
                            Text(
                                text = " • $quantiemeLabel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${client.marketZone} • ${client.activityType}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom metadata & Action row - compact & responsive with ZERO wasted empty space!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Phone with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = client.phone,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentSkyBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Actions: Chat, Affecter Crédit & View Dossier
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bouton Affecter un Crédit direct
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = SuccessGreen.copy(alpha = 0.15f),
                        modifier = Modifier.clickable(onClick = onAssignCreditClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.SWAHILI) "Mkopo" else "Crédit",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }

                    if (onChatClick != null) {
                        IconButton(
                            onClick = onChatClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = Localization.tr("action_contact", currentLanguage),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable(onClick = onClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = Localization.tr("action_dossier", currentLanguage),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddClientDialog(
    nextMatricule: String = "CLI-001",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String, String, String) -> Unit,
    currentLanguage: AppLanguage = AppLanguage.FRENCH
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+243 ") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Femme") }
    var marketZone by remember { mutableStateOf("Marché Central") }
    var activityType by remember { mutableStateOf("Commerce de vivres") }
    var idCardNumber by remember { mutableStateOf("") }
    var guarantorName by remember { mutableStateOf("") }
    var guarantorPhone by remember { mutableStateOf("+243 ") }
    var notes by remember { mutableStateOf("") }

    val quantiemeLabel = remember(nextMatricule, currentLanguage) {
        com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(nextMatricule, currentLanguage)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(Localization.tr("clients_dialog_add_title", currentLanguage), fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bannière officielle d'auto-incrémentation du matricule
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.SWAHILI) "Nambari itakayopewa kiotomatiki :" else "Matricule attribué automatiquement :",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = nextMatricule,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (quantiemeLabel.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                text = quantiemeLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(Localization.tr("clients_field_fullname", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(Localization.tr("clients_field_phone", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Gender Selection (Homme / Femme) with stylized profile avatars
                item {
                    Column {
                        Text(
                            text = "${Localization.tr("clients_field_gender", currentLanguage)} :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Femme Option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (gender == "Femme") Color(0xFFFDF2F8) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (gender == "Femme") 1.8.dp else 1.dp,
                                    color = if (gender == "Femme") Color(0xFFDB2777) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { gender = "Femme" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    ClientAvatar(gender = "Femme", size = 28.dp, showGenderBadge = false)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.SWAHILI) "Mwanamke" else "Femme",
                                        fontSize = 12.sp,
                                        fontWeight = if (gender == "Femme") FontWeight.Bold else FontWeight.Medium,
                                        color = if (gender == "Femme") Color(0xFFBE185D) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Homme Option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (gender == "Homme") Color(0xFFF0F9FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (gender == "Homme") 1.8.dp else 1.dp,
                                    color = if (gender == "Homme") Color(0xFF0284C7) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { gender = "Homme" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    ClientAvatar(gender = "Homme", size = 28.dp, showGenderBadge = false)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.SWAHILI) "Mwanaume" else "Homme",
                                        fontSize = 12.sp,
                                        fontWeight = if (gender == "Homme") FontWeight.Bold else FontWeight.Medium,
                                        color = if (gender == "Homme") Color(0xFF0369A1) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    val quickMarkets = listOf("Marché de Kituku", "Marché Alanine", "Marché Virunga", "Marché de Nyabushongo", "Marché de Rwasama", "Marché de Birere")
                    Column {
                        Text(Localization.tr("clients_field_market", currentLanguage) + " :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            quickMarkets.forEach { qm ->
                                FilterChip(
                                    selected = marketZone == qm,
                                    onClick = { marketZone = qm },
                                    label = { Text(qm, fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = marketZone,
                        onValueChange = { marketZone = it },
                        label = { Text(Localization.tr("clients_field_market", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = activityType,
                        onValueChange = { activityType = it },
                        label = { Text(Localization.tr("clients_field_activity", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = idCardNumber,
                        onValueChange = { idCardNumber = it },
                        label = { Text(Localization.tr("clients_field_idcard", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = guarantorName,
                        onValueChange = { guarantorName = it },
                        label = { Text(Localization.tr("clients_field_guarantor", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = guarantorPhone,
                        onValueChange = { guarantorPhone = it },
                        label = { Text(Localization.tr("clients_field_guarantor_phone", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(Localization.tr("clients_field_notes", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(fullName, phone, email, marketZone, activityType, gender, idCardNumber, guarantorName, guarantorPhone, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text(Localization.tr("action_save", currentLanguage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.tr("action_cancel", currentLanguage))
            }
        }
    )
}
