package com.example.ui.screens.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.ClientAvatar
import com.example.ui.components.CycleStatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun CreditCyclesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var showAssignCreditDialog by remember { mutableStateOf(false) }

    val filteredCycles = cycles.filter { cycle ->
        val matchesSearch = cycle.clientName.contains(searchQuery, ignoreCase = true) ||
                cycle.id.contains(searchQuery, ignoreCase = true) ||
                cycle.marketZone.contains(searchQuery, ignoreCase = true) ||
                (cycle.agentName ?: "").contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "En cours" -> cycle.status == CycleStatus.EN_COURS
            "En retard" -> cycle.status == CycleStatus.EN_RETARD
            "Terminés" -> cycle.status == CycleStatus.TERMINE
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // En-tête avec bouton Affecter un Crédit direct
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Mikopo & Marejesho" else "Gestion des Crédits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Wateja waliopewa mkopo" else "${filteredCycles.size} client(s) avec crédit",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                Button(
                    onClick = { showAssignCreditDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) AccentSkyBlueLight else PrimaryNavy,
                        contentColor = if (isDark) Color(0xFF032442) else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("assign_credit_header_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (isDark) Color(0xFF032442) else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo" else "Affecter un Crédit",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF032442) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Localization.tr("cycle_search_placeholder", currentLang), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            val filterOptions = if (currentLang == AppLanguage.SWAHILI) {
                listOf("Tous" to "Wote", "En cours" to "Inayoendelea", "En retard" to "Iliyochelewa", "Terminés" to "Zilizoisha")
            } else {
                listOf("Tous" to "Tous", "En cours" to "En cours", "En retard" to "En retard", "Terminés" to "Terminés")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filterOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCycles.isEmpty()) {
                val isDarkEmpty = androidx.compose.foundation.isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Hakuna mkopo unaolingana na utafutaji." else "Aucun cycle de crédit actif trouvé.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Affectez un crédit à un client pour démarrer le suivi à 3,5% / jour et les opérations de recouvrement.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { showAssignCreditDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkEmpty) AccentSkyBlueLight else PrimaryNavy,
                                    contentColor = if (isDarkEmpty) Color(0xFF032442) else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo Sasa" else "Affecter un Crédit",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCycles) { cycle ->
                        val matchingClient = clients.find { it.id == cycle.clientId }
                        CreditCycleCard(
                            cycle = cycle,
                            clientGender = matchingClient?.gender ?: "Femme",
                            photoUrl = matchingClient?.avatarUrl,
                            currentLanguage = currentLang,
                            onClick = { viewModel.openCycleDetail(cycle.id) },
                            onCollectPaymentClick = {
                                viewModel.openPaymentForCycle(cycle.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB to assign credit (visible to Manager and Field Agent)
        if (currentUser?.role == UserRole.GESTIONNAIRE || currentUser?.role == UserRole.AGENT) {
            FloatingActionButton(
                onClick = { showAssignCreditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("new_cycle_fab")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Affecter un crédit",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showAssignCreditDialog) {
        AssignCreditDialog(
            clients = clients,
            onDismiss = { showAssignCreditDialog = false },
            onConfirm = { clientId, amount, durationDays, purpose, paymentMethod, newClientName, newClientPhone, newClientMarket ->
                showAssignCreditDialog = false
                val targetClient = clients.find { it.id == clientId }
                viewModel.assignCreditToClient(
                    clientId = clientId,
                    principalAmount = amount,
                    durationDays = durationDays,
                    purpose = purpose,
                    marketZone = newClientMarket ?: targetClient?.marketZone ?: "Marché de Kituku",
                    activityType = targetClient?.activityType ?: "Commerce",
                    paymentMethod = paymentMethod,
                    newClientName = newClientName,
                    newClientPhone = newClientPhone
                )
            }
        )
    }
}

@Composable
fun CreditCycleCard(
    cycle: CreditCycleEntity,
    clientGender: String = "Femme",
    photoUrl: String? = null,
    onClick: () -> Unit,
    onCollectPaymentClick: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (cycle.status == CycleStatus.EN_RETARD) ErrorRed.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("cycle_card_${cycle.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar + Weighted client name so CycleStatusBadge is NEVER squished on small phones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    ClientAvatar(
                        gender = clientGender,
                        photoUrl = photoUrl,
                        size = 36.dp,
                        isLate = cycle.status == CycleStatus.EN_RETARD,
                        showGenderBadge = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = cycle.clientName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: ${cycle.id} • ${cycle.marketZone}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                CycleStatusBadge(status = cycle.status, lang = currentLanguage)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(if (currentLanguage == AppLanguage.SWAHILI) "Mtaji Uliotolewa" else "Montant prêté", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MicrocreditRepository.formatAmount(cycle.principalAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(if (currentLanguage == AppLanguage.SWAHILI) "Malipo/siku" else "Échéance/jour", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MicrocreditRepository.formatAmount(cycle.dailyPaymentAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentSkyBlue)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (currentLanguage == AppLanguage.SWAHILI) "Deni Inayobaki" else "Solde restant", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = MicrocreditRepository.formatAmount(cycle.balanceRemaining),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cycle.status == CycleStatus.EN_RETARD) ErrorRed else PrimaryNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (currentLanguage == AppLanguage.SWAHILI) "Iliyolipwa" else "Recouvrement"} : ${MicrocreditRepository.formatPercent(cycle.recoveryRate)} (${MicrocreditRepository.formatAmount(cycle.totalCollected)} / ${MicrocreditRepository.formatAmount(cycle.totalContractual)})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (cycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (cycle.status) {
                    CycleStatus.TERMINE -> SuccessGreen
                    CycleStatus.EN_RETARD -> ErrorRed
                    else -> AccentSkyBlue
                },
                trackColor = Color(0xFFE2E8F0)
            )

            if (cycle.status == CycleStatus.EN_RETARD) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.SWAHILI) "Onyo : siku ${cycle.daysLate} hazijalipwa !" else "Alerte : ${cycle.daysLate} jour(s) d'échéances impayées !",
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Responsive Quick Buttons for each cycle card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cycle.status != CycleStatus.TERMINE && cycle.status != CycleStatus.ANNULE) {
                    Button(
                        onClick = onCollectPaymentClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cycle.status == CycleStatus.EN_RETARD) ErrorRed else SuccessGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.SWAHILI) "Kukusanya Rejesho" else "Recouvrement",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.SWAHILI) "Angalia" else "Consulter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
