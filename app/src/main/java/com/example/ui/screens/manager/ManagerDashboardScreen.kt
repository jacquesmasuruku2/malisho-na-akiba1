package com.example.ui.screens.manager

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.CreditCycleEntity
import com.example.data.model.CycleStatus
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.CycleStatusBadge
import com.example.ui.components.MetricCard
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.AppLanguage
import com.example.util.Localization

@Composable
fun ManagerDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val lateCycles = cycles.filter { it.status == CycleStatus.EN_RETARD }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Quick Action Bar with premium iconography adapted to single-line cards on all screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo("applications") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("dashboard_btn_loans")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Maombi" else "Demandes",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo("record_payment") },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("dashboard_btn_payment")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kukusanya" else "Recouvrement",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo("caisse") },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("dashboard_btn_caisse")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kechi" else "Caisse",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { viewModel.navigateTo("user_management") },
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("dashboard_btn_users")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Wafanyakazi na Majukumu (Ongeza Mfanyakazi)" else "Personnel & Rôles (Ajouter un utilisateur)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section: Capital & Caisse Overview
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "MAKUTA YALIYO MU KECHI SASA" else "SOLDE DE CAISSE DISPONIBLE",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MicrocreditRepository.formatAmount(metrics.cashBalance),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(if (currentLang == AppLanguage.SWAHILI) "Mtaji wa Mwanzo" else "Capital Initial", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(MicrocreditRepository.formatAmount(metrics.initialCapital), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text(if (currentLang == AppLanguage.SWAHILI) "Mtaji wa Sasa" else "Capital Actuel", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(MicrocreditRepository.formatAmount(metrics.currentCapital), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text(if (currentLang == AppLanguage.SWAHILI) "Faida Safi" else "Profit Net", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(MicrocreditRepository.formatAmount(metrics.totalNetProfit), color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: KPI Grid (Excel Model KPIs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = Localization.tr("dash_portfolio_title", currentLang),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = Localization.tr("metric_disbursed", currentLang),
                        value = MicrocreditRepository.formatAmount(metrics.totalPrincipalDisbursed),
                        subtitle = "${metrics.totalLoansCount} ${if (currentLang == AppLanguage.SWAHILI) "mizunguko" else "cycles"}",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = AccentSkyBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = Localization.tr("metric_collected", currentLang),
                        value = MicrocreditRepository.formatAmount(metrics.totalCollected),
                        subtitle = "${if (currentLang == AppLanguage.SWAHILI) "Kipimo" else "Taux"}: ${MicrocreditRepository.formatPercent(metrics.globalRecoveryRate)}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = Localization.tr("metric_remaining", currentLang),
                        value = MicrocreditRepository.formatAmount(metrics.totalRemaining),
                        subtitle = "${metrics.activeLoansCount} ${if (currentLang == AppLanguage.SWAHILI) "inayoendelea" else "crédits actifs"}",
                        icon = Icons.Default.AccountBalance,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (currentLang == AppLanguage.SWAHILI) "Faida ya Jumla" else "Profit Brut Réalisé",
                        value = MicrocreditRepository.formatAmount(metrics.totalGrossProfit),
                        subtitle = "${if (currentLang == AppLanguage.SWAHILI) "Gharama" else "Charges"}: ${MicrocreditRepository.formatAmount(metrics.totalCharges)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section: Taux de Recouvrement Global Progress
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.tr("metric_recovery_rate", currentLang),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = MicrocreditRepository.formatPercent(metrics.globalRecoveryRate),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (metrics.globalRecoveryRate >= 85) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (metrics.globalRecoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (metrics.globalRecoveryRate >= 85) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${if (currentLang == AppLanguage.SWAHILI) "Iliyokusanywa" else "Encaissé"}: ${MicrocreditRepository.formatAmount(metrics.totalCollected)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${if (currentLang == AppLanguage.SWAHILI) "Mkataba" else "Contractuel"}: ${MicrocreditRepository.formatAmount(metrics.totalContractual)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Section: Alerte Crédits en Retard (Excel Risk Monitoring)
        if (lateCycles.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Onyo : Mikopo ${lateCycles.size} Iliyochelewa" else "Alerte : ${lateCycles.size} Crédit(s) en Retard",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    lateCycles.forEach { cycle ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.openCycleDetail(cycle.id) }
                                .testTag("late_cycle_card_${cycle.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top row: Client name and Status Badge with safe space
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = cycle.clientName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Cycle ${cycle.id} • ${cycle.marketZone}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CycleStatusBadge(status = cycle.status, lang = currentLang)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Metrics info row with responsive alignment
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ErrorRed.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Kuchelewa" else "Retard constaté",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "siku ${cycle.daysLate} hazijalipwa" else "${cycle.daysLate} jour(s) impayé(s)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ErrorRed
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Malipo/siku" else "Échéance/jour",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = MicrocreditRepository.formatAmount(cycle.dailyPaymentAmount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentSkyBlue
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Deni inayobaki" else "Solde restant",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = MicrocreditRepository.formatAmount(cycle.balanceRemaining),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ErrorRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Responsive Action Buttons for Thérèse Tshala & Late cycles
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.openPaymentForCycle(cycle.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .heightIn(min = 40.dp)
                                            .testTag("late_btn_collect_${cycle.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Kukusanya" else "Encaisser",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.sendReminderAlert(cycle.clientName, cycle.clientPhone) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .testTag("late_btn_remind_${cycle.id}")
                                    ) {
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Kukumbusha" else "Relancer",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.openCycleDetail(cycle.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .testTag("late_btn_detail_${cycle.id}")
                                    ) {
                                        Text(
                                            text = if (currentLang == AppLanguage.SWAHILI) "Habari" else "Détails",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Cycles récents
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Mizunguko ya Mikopo ya Hivi Karibuni" else "Cycles de Crédit Récents",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Ona yote (${cycles.size})" else "Voir tout (${cycles.size})",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.navigateTo("cycles") }
                    )
                }

                cycles.take(3).forEach { cycle ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openCycleDetail(cycle.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cycle.clientName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                CycleStatusBadge(status = cycle.status, lang = currentLang)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${if (currentLang == AppLanguage.SWAHILI) "Mkopo" else "Prêt"}: ${MicrocreditRepository.formatAmount(cycle.principalAmount)} • ${if (currentLang == AppLanguage.SWAHILI) "Kila siku" else "Journalier"}: ${MicrocreditRepository.formatAmount(cycle.dailyPaymentAmount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (cycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (cycle.status == CycleStatus.TERMINE) SuccessGreen else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
