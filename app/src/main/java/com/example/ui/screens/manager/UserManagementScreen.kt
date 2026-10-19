package com.example.ui.screens.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.BybitAvatar
import com.example.ui.components.BybitAvatarPickerDialog
import com.example.ui.components.BybitRoleBadge
import com.example.ui.components.UserRoleBadge
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showInviteDialog by remember { mutableStateOf(false) }
    var userToEditRole by remember { mutableStateOf<UserEntity?>(null) }
    var userToChangeAvatar by remember { mutableStateOf<UserEntity?>(null) }
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Gestion des Utilisateurs & Rôles",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Attribution des permissions et création de comptes pour le personnel et les clients",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contrôle des Accès (Direction)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Ajoutez un utilisateur et attribuez son rôle réel officiel (Agent, Client, Caisse, Direction).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showInviteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_add_user_manager")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(users) { user ->
                    UserItemCard(
                        user = user,
                        isCurrentLoggedUser = user.id == currentUser?.id,
                        formattedDate = dateFormat.format(Date(user.createdDate)),
                        onEditRoleClick = { userToEditRole = user },
                        onToggleActiveClick = {
                            viewModel.toggleUserActive(user.id, user.fullName, !user.isActive)
                        },
                        onChangeAvatarClick = {
                            userToChangeAvatar = user
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button to Invite User
        FloatingActionButton(
            onClick = { showInviteDialog = true },
            containerColor = PrimaryNavy,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("invite_user_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Inviter un utilisateur")
        }
    }

    val allClientsList by viewModel.allClients.collectAsStateWithLifecycle()
    if (showInviteDialog) {
        AddUserDialog(
            existingClients = allClientsList,
            onDismiss = { showInviteDialog = false },
            onConfirm = { name, phone, username, password, role, zone, email ->
                viewModel.addUserByManager(
                    fullName = name,
                    phone = phone,
                    username = username,
                    role = role,
                    marketZone = zone,
                    pinCode = password,
                    email = email,
                    onSuccess = {
                        showInviteDialog = false
                    }
                )
            }
        )
    }

    userToEditRole?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { userToEditRole = null },
            onConfirm = { newRole, newMarkets ->
                viewModel.updateUserRoleAndMarkets(user.id, user.fullName, newRole, newMarkets)
                userToEditRole = null
            }
        )
    }

    userToChangeAvatar?.let { user ->
        BybitAvatarPickerDialog(
            currentKey = user.avatarKey,
            role = user.role,
            gender = "Homme",
            onAvatarSelected = { key ->
                if (user.id == currentUser?.id) {
                    viewModel.updateMyAvatarKey(key)
                } else {
                    viewModel.updateUserAvatarKey(user.id, key)
                }
                userToChangeAvatar = null
            },
            onPhotoSelectedFromGallery = { uri ->
                viewModel.uploadAndSetUserAvatarFromUri(uri, context) { success, _ ->
                    if (success) {
                        userToChangeAvatar = null
                    }
                }
            },
            isUploadingPhoto = isUploadingAvatar,
            onDismiss = { userToChangeAvatar = null }
        )
    }
}

@Composable
fun UserItemCard(
    user: UserEntity,
    isCurrentLoggedUser: Boolean,
    formattedDate: String,
    onEditRoleClick: () -> Unit,
    onToggleActiveClick: () -> Unit,
    onChangeAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BybitAvatar(
                avatarKey = user.avatarKey,
                role = user.role,
                isOnline = user.isOnline,
                size = 46.dp,
                showGlow = true,
                onClick = onChangeAvatarClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentLoggedUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(Vous)", fontSize = 11.sp, color = AccentSkyBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.phone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val assignedMarkets = user.marketZone.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (assignedMarkets.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (assignedMarkets.size > 1) "Affecté à ${assignedMarkets.size} marchés : ${user.marketZone}" else "Marché : ${user.marketZone}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "Créé le $formattedDate • Statut : ${if (user.isActive) "Actif" else "Désactivé"}",
                    fontSize = 11.sp,
                    color = if (user.isActive) SuccessGreen else ErrorRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BybitRoleBadge(role = user.role)
                    if (user.role == UserRole.CLIENT) {
                        val clientId = if (user.id.startsWith("USR-CLI-")) user.id.removePrefix("USR-") else user.id
                        val quantieme = com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(clientId)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "$clientId${if (quantieme.isNotEmpty()) " • $quantieme" else ""}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!isCurrentLoggedUser) {
                    IconButton(onClick = onEditRoleClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier rôle", tint = AccentSkyBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleActiveClick) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = if (user.isActive) "Désactiver" else "Activer",
                            tint = if (user.isActive) ErrorRed else SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(Icons.Default.Lock, contentDescription = "Protégé", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    existingClients: List<com.example.data.model.ClientEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, phone: String, username: String, password: String, role: UserRole, marketZone: String, email: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+243 ") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("1234") }
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.AGENT) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMarkets by remember { mutableStateOf(setOf("Marché de Kituku")) }
    var customMarket by remember { mutableStateOf("") }

    val roleOptions = listOf(
        Triple(UserRole.AGENT, "Agent de terrain", "Visite des marchés, collecte journalière, encaissements et suivi retards"),
        Triple(UserRole.CLIENT, "Client", "Accès à l'espace client, demande de prêt, échéancier et messagerie"),
        Triple(UserRole.CAISSIER, "Caissier (Caisse Centrale)", "Mouvements de caisse, décaissements des prêts approuvés"),
        Triple(UserRole.GESTIONNAIRE, "Gestionnaire (Direction)", "Supervision générale, validation des crédits, clôture et rapports")
    )
    val currentRoleOption = roleOptions.find { it.first == selectedRole } ?: roleOptions.first()

    val marketsList = listOf(
        "Marché de Kituku",
        "Marché Alanine",
        "Marché Virunga",
        "Marché de Nyabushongo",
        "Marché de Rwasama",
        "Marché La Vision",
        "Marché de Mubi",
        "Marché de Birere",
        "Guichet Central",
        "Direction Générale",
        "Autres..."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajouter un Utilisateur (Personnel ou Client)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (username.isBlank() || username.startsWith("user.")) {
                            username = it.lowercase().replace(" ", ".")
                        }
                    },
                    label = { Text("Nom complet *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro de téléphone *") },
                    placeholder = { Text("+243 ...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nom d'utilisateur (pour connexion) *") },
                    placeholder = { Text("Ex: agent.kalombo ou caissier.marie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe ou Code PIN *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email (Optionnelle)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Rôle officiel à attribuer :", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentRoleOption.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sélectionner un rôle dans la liste *") },
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedRole) {
                                    UserRole.AGENT -> Icons.Default.DirectionsWalk
                                    UserRole.CLIENT -> Icons.Default.Person
                                    UserRole.CAISSIER -> Icons.Default.AccountBalanceWallet
                                    UserRole.GESTIONNAIRE -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { roleDropdownExpanded = !roleDropdownExpanded }) {
                                Icon(
                                    imageVector = if (roleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Dérouler la liste des rôles"
                                )
                            }
                        },
                        supportingText = {
                            Text(currentRoleOption.third, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                            .testTag("role_dropdown_field")
                    )

                    // Overlay to capture clicks anywhere on the OutlinedTextField
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        roleOptions.forEach { (role, roleName, roleDesc) ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = roleName,
                                            fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedRole == role) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = roleDesc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.AGENT -> Icons.Default.DirectionsWalk
                                            UserRole.CLIENT -> Icons.Default.Person
                                            UserRole.CAISSIER -> Icons.Default.AccountBalanceWallet
                                            UserRole.GESTIONNAIRE -> Icons.Default.AdminPanelSettings
                                        },
                                        contentDescription = null,
                                        tint = if (selectedRole == role) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (selectedRole == role) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRole = role
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedRole == UserRole.CLIENT) {
                    val nextMatricule = remember(existingClients) {
                        val maxNumber = existingClients.mapNotNull {
                            com.example.data.repository.MicrocreditRepository.extractClientSequenceNumber(it.id)
                        }.maxOrNull() ?: 0
                        String.format(java.util.Locale.US, "CLI-%03d", maxNumber + 1)
                    }
                    val nextQuantieme = remember(nextMatricule) {
                        com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(nextMatricule)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Matricule client attribué automatiquement : $nextMatricule • $nextQuantieme",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    Text(
                        text = "Marchés d'affectation (${selectedMarkets.size} sélectionné(s)) :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Possibilité d'affecter un personnel dans plusieurs marchés simultanément :",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedMarkets = marketsList.filter { it != "Autres..." }.toSet()
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Tous les marchés", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            selectedMarkets = setOf("Marché de Kituku")
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Réinitialiser", fontSize = 11.sp)
                    }
                }

                marketsList.chunked(2).forEach { rowMarkets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowMarkets.forEach { m ->
                            val isSelected = selectedMarkets.contains(m)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedMarkets = if (isSelected) {
                                        if (selectedMarkets.size > 1) selectedMarkets - m else selectedMarkets
                                    } else {
                                        selectedMarkets + m
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(m, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedMarkets.contains("Autres...")) {
                    OutlinedTextField(
                        value = customMarket,
                        onValueChange = { customMarket = it },
                        label = { Text("Précisez le marché ou secteur spécifique *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && phone.isNotBlank()) {
                        val listWithoutOther = selectedMarkets.filter { it != "Autres..." }.toMutableList()
                        if (selectedMarkets.contains("Autres...") && customMarket.isNotBlank()) {
                            listWithoutOther.add(customMarket.trim())
                        }
                        val effectiveZone = if (listWithoutOther.isEmpty()) "Direction Générale" else listWithoutOther.joinToString(", ")
                        val effectiveU = if (username.isBlank()) fullName.lowercase().replace(" ", ".") else username
                        onConfirm(fullName, phone, effectiveU, password.ifEmpty { "1234" }, selectedRole, effectiveZone, email)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Créer le compte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditUserDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (newRole: UserRole, newMarkets: String) -> Unit
) {
    val marketsList = listOf(
        "Marché de Kituku",
        "Marché Alanine",
        "Marché Virunga",
        "Marché de Nyabushongo",
        "Marché de Rwasama",
        "Marché La Vision",
        "Marché de Mubi",
        "Marché de Birere",
        "Guichet Central",
        "Direction Générale",
        "Autres..."
    )

    var selectedRole by remember { mutableStateOf(user.role) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    val roleOptions = listOf(
        Triple(UserRole.AGENT, "Agent de terrain", "Visite des marchés, collecte journalière, encaissements et suivi retards"),
        Triple(UserRole.CLIENT, "Client", "Accès à l'espace client, demande de prêt, échéancier et messagerie"),
        Triple(UserRole.CAISSIER, "Caissier (Caisse Centrale)", "Mouvements de caisse, décaissements des prêts approuvés"),
        Triple(UserRole.GESTIONNAIRE, "Gestionnaire (Direction)", "Supervision générale, validation des crédits, clôture et rapports")
    )
    val currentRoleOption = roleOptions.find { it.first == selectedRole } ?: roleOptions.first()

    val initialMarkets = remember(user.marketZone) {
        user.marketZone.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    var selectedMarkets by remember { mutableStateOf(if (initialMarkets.isEmpty()) setOf("Marché de Kituku") else initialMarkets) }
    var customMarket by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Modifier Rôle & Affectations de ${user.fullName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Rôle dans l'organisation :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentRoleOption.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sélectionner un rôle *") },
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedRole) {
                                    UserRole.AGENT -> Icons.Default.DirectionsWalk
                                    UserRole.CLIENT -> Icons.Default.Person
                                    UserRole.CAISSIER -> Icons.Default.AccountBalanceWallet
                                    UserRole.GESTIONNAIRE -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { roleDropdownExpanded = !roleDropdownExpanded }) {
                                Icon(
                                    imageVector = if (roleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Dérouler la liste des rôles"
                                )
                            }
                        },
                        supportingText = {
                            Text(currentRoleOption.third, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                            .testTag("edit_role_dropdown_field")
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { roleDropdownExpanded = !roleDropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        roleOptions.forEach { (role, roleName, roleDesc) ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = roleName,
                                            fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedRole == role) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = roleDesc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.AGENT -> Icons.Default.DirectionsWalk
                                            UserRole.CLIENT -> Icons.Default.Person
                                            UserRole.CAISSIER -> Icons.Default.AccountBalanceWallet
                                            UserRole.GESTIONNAIRE -> Icons.Default.AdminPanelSettings
                                        },
                                        contentDescription = null,
                                        tint = if (selectedRole == role) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (selectedRole == role) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRole = role
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    Text(
                        text = "Marchés d'affectation (${selectedMarkets.size} sélectionnés) :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Affectez ce personnel à un ou plusieurs marchés d'intervention :",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedMarkets = marketsList.filter { it != "Autres..." }.toSet()
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Tous les marchés", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            selectedMarkets = setOf("Marché de Kituku")
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Par défaut", fontSize = 11.sp)
                    }
                }

                marketsList.chunked(2).forEach { rowMarkets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowMarkets.forEach { m ->
                            val isSelected = selectedMarkets.contains(m)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedMarkets = if (isSelected) {
                                        if (selectedMarkets.size > 1) selectedMarkets - m else selectedMarkets
                                    } else {
                                        selectedMarkets + m
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(m, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedMarkets.contains("Autres...")) {
                    OutlinedTextField(
                        value = customMarket,
                        onValueChange = { customMarket = it },
                        label = { Text("Précisez le marché supplémentaire") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val listWithoutOther = selectedMarkets.filter { it != "Autres..." }.toMutableList()
                    if (selectedMarkets.contains("Autres...") && customMarket.isNotBlank()) {
                        listWithoutOther.add(customMarket.trim())
                    }
                    val effectiveZone = if (listWithoutOther.isEmpty()) "Direction Générale" else listWithoutOther.joinToString(", ")
                    onConfirm(selectedRole, effectiveZone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Mettre à jour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
