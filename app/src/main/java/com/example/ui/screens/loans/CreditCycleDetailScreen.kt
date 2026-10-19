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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.ClientAvatar
import com.example.ui.components.CycleStatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun CreditCycleDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCycleId by viewModel.selectedCycleId.collectAsStateWithLifecycle()
    val allCycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val cycle = allCycles.find { it.id == selectedCycleId }
    val targetClient = allClients.find { it.id == cycle?.clientId }
    val cyclePayments = allPayments.filter { it.cycleId == selectedCycleId }

    if (cycle == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (currentLang == AppLanguage.SWAHILI) "Mkopo haujapatikana." else "Cycle de crédit introuvable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("cycles") }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Localization.tr("action_back", currentLang),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Habari za Mkopo ${cycle.id}" else "Détail du Prêt ${cycle.id}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Header Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClientAvatar(
                                gender = targetClient?.gender ?: "Femme",
                                photoUrl = targetClient?.avatarUrl,
                                size = 46.dp,
                                isLate = cycle.status == CycleStatus.EN_RETARD,
                                showGenderBadge = true
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    cycle.clientName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${if (currentLang == AppLanguage.SWAHILI) "Afisa" else "Agent"} : ${cycle.agentName ?: (if (currentLang == AppLanguage.SWAHILI) "Hajawekwa" else "Non assigné")} • Soko : ${cycle.marketZone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        CycleStatusBadge(status = cycle.status, lang = currentLang)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (cycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentSkyBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${cycle.daysElapsed} ${if (currentLang == AppLanguage.SWAHILI) "kati ya siku" else "sur"} ${cycle.durationDays} ${if (currentLang == AppLanguage.SWAHILI) "" else "jours"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${if (currentLang == AppLanguage.SWAHILI) "Kipimo" else "Taux"}: ${MicrocreditRepository.formatPercent(cycle.recoveryRate)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSkyBlue
                        )
                    }
                }
            }
        }

        // Financial Breakdown Table (Excel Fields)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (currentLang == AppLanguage.SWAHILI) "MCHANGANUO WA MAKUTA YA MKOPO" else "STRUCTURE FINANCIÈRE DU PRÊT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Mtaji uliotolewa" else "Montant du prêt décaissé", value = MicrocreditRepository.formatAmount(cycle.principalAmount))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Malipo ya kila siku" else "Paiement quotidien contractuel", value = MicrocreditRepository.formatAmount(cycle.dailyPaymentAmount))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Idadi ya siku zilizopangwa" else "Nombre de jours prévus", value = "${cycle.durationDays} ${if (currentLang == AppLanguage.SWAHILI) "siku" else "jours"}")
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Jumla ya kulipa mkataba" else "Total contractuel", value = MicrocreditRepository.formatAmount(cycle.totalContractual), isBold = true)
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Jumla iliyokusanywa" else "Total encaissé réel", value = MicrocreditRepository.formatAmount(cycle.totalCollected), color = SuccessGreen, isBold = true)
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Deni inayobaki" else "Solde restant à recouvrer", value = MicrocreditRepository.formatAmount(cycle.balanceRemaining), color = if (cycle.balanceRemaining > 0) ErrorRed else SuccessGreen, isBold = true)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (currentLang == AppLanguage.SWAHILI) "MGAWANYO WA FAIDA NA AKIBA" else "RÉPARTITION & RENTABILITÉ (LOGIQUE EXCEL)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Faida ya jumla" else "Profit brut réalisé", value = MicrocreditRepository.formatAmount(cycle.grossProfit))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Sehemu ya Shirika (70%)" else "Part Entreprise (70%)", value = MicrocreditRepository.formatAmount(cycle.enterpriseShare))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Sehemu ya Mteja (Akiba 30%)" else "Part Client (30%)", value = MicrocreditRepository.formatAmount(cycle.clientShare))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Gharama za kazi" else "Charges d'exploitation allouées", value = MicrocreditRepository.formatAmount(cycle.charges))
                    DetailRow(label = if (currentLang == AppLanguage.SWAHILI) "Faida Safi ya Shirika" else "Profit Net Entreprise", value = MicrocreditRepository.formatAmount(cycle.netProfit), color = SuccessGreen, isBold = true)
                }
            }
        }

        // Action button
        if (cycle.status != CycleStatus.TERMINE && cycle.status != CycleStatus.ANNULE) {
            item {
                Button(
                    onClick = { viewModel.navigateTo("record_payment") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLang == AppLanguage.SWAHILI) "Kupokea malipo kwa mkopo huu" else "Enregistrer un encaissement pour ce prêt", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Payments History for this Cycle
        item {
            Text(
                if (currentLang == AppLanguage.SWAHILI) "Malipo Yaliyopokelewa (${cyclePayments.size})" else "Paiements Encaissés (${cyclePayments.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (cyclePayments.isEmpty()) {
            item {
                Text(
                    if (currentLang == AppLanguage.SWAHILI) "Hakuna malipo yoyote yaliyopokelewa bado." else "Aucun encaissement pour ce prêt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            items(cyclePayments) { pay ->
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
                            Text(
                                "${if (currentLang == AppLanguage.SWAHILI) "Siku" else "Jour"} ${pay.dayNumber} • ${pay.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${pay.dateString} • Agent: ${pay.agentName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = MicrocreditRepository.formatAmount(pay.collectedAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
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
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    color: Color? = null,
    isBold: Boolean = false
) {
    val resolvedColor = color ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = resolvedColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
