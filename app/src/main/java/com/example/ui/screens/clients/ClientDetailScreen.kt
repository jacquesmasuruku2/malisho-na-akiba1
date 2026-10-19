package com.example.ui.screens.clients

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.data.model.CycleStatus
import com.example.ui.components.ClientAvatar
import com.example.ui.components.ClientRoleGenderBadge
import com.example.ui.components.ClientStatusBadge
import com.example.ui.components.CycleStatusBadge
import com.example.ui.screens.loans.AssignCreditDialog
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClientDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedId by viewModel.selectedClientId.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()

    val client = clients.find { it.id == selectedId }
    val clientCycles = cycles.filter { it.clientId == selectedId }
    val clientPayments = payments.filter { it.clientId == selectedId }

    var showAssignCreditDialog by remember { mutableStateOf(false) }

    val clientPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetClient = client
        if (uri != null && targetClient != null) {
            val key = if (targetClient.gender == "F") "female_client" else "male_client"
            viewModel.updateClientAvatarKey(targetClient.id, key)
        }
    }

    BackHandler {
        viewModel.navigateTo("clients")
    }

    if (client == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (currentLang == AppLanguage.SWAHILI) "Mteja hajapatikana." else "Client introuvable.")
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("clients") }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Localization.tr("action_back", currentLang),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = Localization.tr("client_detail_title", currentLang),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Client Identity Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    clientPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("btn_change_client_photo")
                        ) {
                            ClientAvatar(
                                gender = client.gender,
                                photoUrl = client.avatarUrl,
                                size = 56.dp,
                                isLate = client.status.contains("RETARD", true),
                                showGenderBadge = true
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Changer la photo Cloudinary",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client.fullName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val quantieme = MicrocreditRepository.getClientQuantiemeLabel(client.id, currentLang)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "${client.id}${if (quantieme.isNotEmpty()) " • $quantieme" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                ClientRoleGenderBadge(gender = client.gender, lang = currentLang)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ClientStatusBadge(status = client.status, lang = currentLang)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(Localization.tr("client_detail_info_section", currentLang), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))

                    val quantieme = MicrocreditRepository.getClientQuantiemeLabel(client.id, currentLang)
                    if (quantieme.isNotEmpty()) {
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Cheo rasmi : $quantieme aliyesajiliwa" else "Rang officiel : $quantieme enregistré dans l'institution",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val genderText = if (client.gender.startsWith("H", ignoreCase = true) || client.gender.startsWith("M", ignoreCase = true)) {
                        if (currentLang == AppLanguage.SWAHILI) "Mwanaume (♂)" else "Homme (Masculin ♂)"
                    } else {
                        if (currentLang == AppLanguage.SWAHILI) "Mwanamke (♀)" else "Femme (Féminin ♀)"
                    }
                    Text("${if (currentLang == AppLanguage.SWAHILI) "Jinsia" else "Sexe"} : $genderText", fontSize = 12.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text("${Localization.tr("client_detail_activity", currentLang)} : ${client.activityType}", fontSize = 12.5.sp, color = TextSecondary)
                    Text("${Localization.tr("client_detail_zone", currentLang)} : ${client.marketZone}", fontSize = 12.5.sp, color = TextSecondary)
                    Text("${Localization.tr("client_detail_phone", currentLang)} : ${client.phone}", fontSize = 12.5.sp, color = AccentSkyBlue, fontWeight = FontWeight.Medium)
                    if (client.idCardNumber.isNotBlank()) {
                        Text("${Localization.tr("client_detail_idcard", currentLang)} : ${client.idCardNumber}", fontSize = 12.5.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Localization.tr("client_detail_guarantor_section", currentLang), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    val emptyLabel = if (currentLang == AppLanguage.SWAHILI) "Haijaandikwa" else "Non renseigné"
                    Text("${if (currentLang == AppLanguage.SWAHILI) "Jina" else "Nom"} : ${client.guarantorName.ifEmpty { emptyLabel }}", fontSize = 12.5.sp, color = TextSecondary)
                    Text("${if (currentLang == AppLanguage.SWAHILI) "Simu" else "Contact"} : ${client.guarantorPhone.ifEmpty { emptyLabel }}", fontSize = 12.5.sp, color = TextSecondary)

                    if (client.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Localization.tr("client_detail_notes_section", currentLang), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(client.notes, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Action Buttons
        item {
            val lateCycle = clientCycles.find { it.status == CycleStatus.EN_RETARD }
            val activeCycle = clientCycles.find { it.status == CycleStatus.EN_COURS }
            val payableCycle = lateCycle ?: activeCycle

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (payableCycle != null) {
                    Button(
                        onClick = { viewModel.openPaymentForCycle(payableCycle.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lateCycle != null) ErrorRed else SuccessGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lateCycle != null)
                                "Effectuer le Recouvrement en retard (${MicrocreditRepository.formatAmount(lateCycle.dailyPaymentAmount)}/j)"
                            else
                                "Effectuer le Recouvrement (3,5%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAssignCreditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo" else "Affecter un crédit", fontSize = 12.sp)
                    }

                    if (currentUser?.role != com.example.data.model.UserRole.GESTIONNAIRE) {
                        OutlinedButton(
                            onClick = { viewModel.openChat(client.id) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (currentLang == AppLanguage.SWAHILI) "Ujumbe" else "Message", fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Chat Client Protégé",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.selectClientForCard(client.id)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (currentLang == AppLanguage.SWAHILI) "Kadi ya Mwanachama (PVC Plastiki)" else "Produire Carte de Membre PVC (Plastique)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (lateCycle != null) {
                    OutlinedButton(
                        onClick = { viewModel.sendReminderAlert(client.fullName, client.phone) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Tuma ukumbusho kwa mteja & mdhamini (${client.guarantorName.ifEmpty { "Garant" }})" else "Envoyer rappel au client & garant (${client.guarantorName.ifEmpty { "Garant" }})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed
                        )
                    }
                }
            }
        }

        // Credit Cycles Section
        item {
            Text(if (currentLang == AppLanguage.SWAHILI) "Historia ya Mizunguko ya Mikopo (${clientCycles.size})" else "Historique des Cycles de Crédit (${clientCycles.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        if (clientCycles.isEmpty()) {
            item {
                Text(if (currentLang == AppLanguage.SWAHILI) "Hakuna mkopo wowote kwa sasa." else "Aucun cycle de crédit actif ou passé pour ce client.", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            items(clientCycles) { cycle ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { viewModel.openCycleDetail(cycle.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cycle ${cycle.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            CycleStatusBadge(status = cycle.status, lang = currentLang)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${if (currentLang == AppLanguage.SWAHILI) "Mkopo" else "Prêt"} : ${MicrocreditRepository.formatAmount(cycle.principalAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${if (currentLang == AppLanguage.SWAHILI) "Jumla" else "Total dû"} : ${MicrocreditRepository.formatAmount(cycle.totalContractual)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${if (currentLang == AppLanguage.SWAHILI) "Imelipwa" else "Encaissé"} : ${MicrocreditRepository.formatAmount(cycle.totalCollected)}", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                            Text("${if (currentLang == AppLanguage.SWAHILI) "Inayobaki" else "Solde"} : ${MicrocreditRepository.formatAmount(cycle.balanceRemaining)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (cycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentSkyBlue
                        )
                    }
                }
            }
        }

        // Payment Receipts Section
        item {
            Text(if (currentLang == AppLanguage.SWAHILI) "Historia ya Malipo Yaliyopokelewa (${clientPayments.size})" else "Historique des Paiements Encaissés (${clientPayments.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        if (clientPayments.isEmpty()) {
            item {
                Text(if (currentLang == AppLanguage.SWAHILI) "Hakuna malipo yoyote yaliyorekodiwa." else "Aucun paiement enregistré pour l'instant.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(clientPayments) { pay ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${pay.id} • ${if (currentLang == AppLanguage.SWAHILI) "Siku" else "Jour"} ${pay.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${pay.dateString} • Mode : ${pay.paymentMethod.label}", fontSize = 11.sp, color = TextMuted)
                        }
                        Text(
                            text = MicrocreditRepository.formatAmount(pay.collectedAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showAssignCreditDialog) {
        AssignCreditDialog(
            clients = listOf(client),
            preselectedClient = client,
            onDismiss = { showAssignCreditDialog = false },
            onConfirm = { clientId, amount, durationDays, purpose, paymentMethod, newClientName, newClientPhone, newClientMarket ->
                showAssignCreditDialog = false
                viewModel.assignCreditToClient(
                    clientId = clientId,
                    principalAmount = amount,
                    durationDays = durationDays,
                    purpose = purpose,
                    marketZone = newClientMarket ?: client.marketZone,
                    activityType = client.activityType,
                    paymentMethod = paymentMethod,
                    newClientName = newClientName,
                    newClientPhone = newClientPhone
                )
            }
        )
    }
}
