package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CycleStatus
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        if (currentLang == AppLanguage.SWAHILI) "Kila Siku" else "Journalier",
        if (currentLang == AppLanguage.SWAHILI) "Kila Juma" else "Hebdomadaire",
        if (currentLang == AppLanguage.SWAHILI) "Kila Mwezi" else "Mensuel"
    )

    val todayDateString = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())

    // Daily Metrics
    val todayPayments = payments.filter { it.dateString == todayDateString }
    val activeCycles = cycles.filter { it.status == CycleStatus.EN_COURS || it.status == CycleStatus.EN_RETARD }
    val todayExpected = activeCycles.sumOf { it.dailyPaymentAmount }
    val todayCollected = todayPayments.sumOf { it.collectedAmount }
    val todayVariance = todayCollected - todayExpected
    val todayRecoveryRate = if (todayExpected > 0) (todayCollected / todayExpected) * 100.0 else 0.0

    // Weekly Metrics
    val weekExpected = todayExpected * 7
    val weekCollected = payments.sumOf { it.collectedAmount }
    val weekVariance = weekCollected - weekExpected
    val weekRecoveryRate = if (weekExpected > 0) (weekCollected / weekExpected) * 100.0 else 0.0

    // Monthly Metrics
    val monthExpected = cycles.sumOf { it.totalContractual }
    val monthCollected = cycles.sumOf { it.totalCollected }
    val monthVariance = monthCollected - monthExpected
    val monthRecoveryRate = if (monthExpected > 0) (monthCollected / monthExpected) * 100.0 else 0.0

    val context = LocalContext.current

    val generateDailyPdfAction: () -> Unit = {
        val reportData = com.example.util.DailyPdfReportGenerator.DailyReportData(
            dateString = todayDateString,
            totalExpected = todayExpected,
            totalCollected = todayCollected,
            totalVariance = todayVariance,
            recoveryRate = todayRecoveryRate,
            activeLoansCount = activeCycles.size,
            payments = todayPayments
        )
        viewModel.shareDailyReportPdf(context, reportData)
    }

    val exportReportAction: () -> Unit = {
        if (selectedTab == 0) {
            generateDailyPdfAction()
        } else {
            val reportText = buildString {
                appendLine("=== MALISHO NA AKIBA (MALAKI) - RAPPORT ${tabs[selectedTab].uppercase()} ===")
                appendLine("Date: $todayDateString")
                when (selectedTab) {
                    1 -> {
                        appendLine("Période: Hebdomadaire (7 jours)")
                        appendLine("Attendu: ${MicrocreditRepository.formatAmount(weekExpected)}")
                        appendLine("Encaissé: ${MicrocreditRepository.formatAmount(weekCollected)}")
                        appendLine("Écart: ${MicrocreditRepository.formatAmount(weekVariance)}")
                        appendLine("Taux: ${MicrocreditRepository.formatPercent(weekRecoveryRate)}")
                    }
                    2 -> {
                        appendLine("Période: Mensuel")
                        appendLine("Attendu: ${MicrocreditRepository.formatAmount(monthExpected)}")
                        appendLine("Encaissé: ${MicrocreditRepository.formatAmount(monthCollected)}")
                        appendLine("Écart: ${MicrocreditRepository.formatAmount(monthVariance)}")
                        appendLine("Taux: ${MicrocreditRepository.formatPercent(monthRecoveryRate)}")
                    }
                }
            }
            viewModel.shareReport(reportText)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Ripoti za Recouvrement" else "Rapports de Recouvrement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Ufuatiliaji wa fedha na makusanyo" else "Suivi des performances et encaissements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Share / Export Button - fully adapted to all screen sizes
                Button(
                    onClick = exportReportAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("reports_btn_export")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Exporter le rapport",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kutuma" else "Exporter",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }

        // Quick Export Action Card (Guarantees prominent, responsive export across any screen)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { exportReportAction() }
                    .testTag("reports_card_export_action")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Tuma Ripoti ya ${tabs[selectedTab]}" else "Exporter le Rapport ${tabs[selectedTab]}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Sambaza kupitia Chat, SMS ao Mail" else "Partager via Chat, SMS, Email",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Button(
                        onClick = exportReportAction,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kutuma" else "Exporter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // RAPPORT JOURNALIER
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Rapport du $todayDateString",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${todayPayments.size} paiements",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentSkyBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Montant Attendu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        MicrocreditRepository.formatAmount(todayExpected),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column {
                                    Text("Montant Encaissé", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        MicrocreditRepository.formatAmount(todayCollected),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Écart", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${if (todayVariance >= 0) "+" else ""}${MicrocreditRepository.formatAmount(todayVariance)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (todayVariance >= 0) SuccessGreen else ErrorRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Taux de recouvrement du jour",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    MicrocreditRepository.formatPercent(todayRecoveryRate),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentSkyBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (todayRecoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (todayRecoveryRate >= 90) SuccessGreen else WarningAmber
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bouton d'action direct : Exporter & Partager le Rapport Journalier en PDF
                            Button(
                                onClick = generateDailyPdfAction,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("reports_btn_generate_daily_pdf")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Pakua & Sambaza Ripoti ya PDF" else "Générer & Partager le Rapport PDF",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Détail des Encaisses de la Journée",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (todayPayments.isEmpty()) {
                    item {
                        Text(
                            "Aucun paiement enregistré pour aujourd'hui.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    items(todayPayments) { pay ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
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
                                        pay.clientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${pay.cycleId} • Jour ${pay.dayNumber} • Agent : ${pay.agentName}",
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
            }

            1 -> {
                // RAPPORT HEBDOMADAIRE
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Semaine en cours (Semaine 40)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailReportRow("Paiements enregistrés", "${payments.size} opérations")
                            DetailReportRow("Montant attendu sur 7 jours", MicrocreditRepository.formatAmount(weekExpected))
                            DetailReportRow("Montant encaissé réel", MicrocreditRepository.formatAmount(weekCollected), color = SuccessGreen, isBold = true)
                            DetailReportRow("Écart de recouvrement", "${if (weekVariance >= 0) "+" else ""}${MicrocreditRepository.formatAmount(weekVariance)}", color = if (weekVariance >= 0) SuccessGreen else ErrorRed, isBold = true)
                            DetailReportRow("Taux de recouvrement hebdomadaire", MicrocreditRepository.formatPercent(weekRecoveryRate), color = AccentSkyBlue, isBold = true)
                        }
                    }
                }
            }

            2 -> {
                // RAPPORT MENSUEL
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Bilan Mensuel Global (Cycle en cours)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailReportRow("Volume contractuel total", MicrocreditRepository.formatAmount(monthExpected))
                            DetailReportRow("Volume total encaissé", MicrocreditRepository.formatAmount(monthCollected), color = SuccessGreen, isBold = true)
                            DetailReportRow("Solde restant en portefeuille", MicrocreditRepository.formatAmount(monthExpected - monthCollected), color = WarningAmber, isBold = true)
                            DetailReportRow("Taux global de recouvrement", MicrocreditRepository.formatPercent(monthRecoveryRate), color = AccentSkyBlue, isBold = true)
                        }
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
fun DetailReportRow(
    label: String,
    value: String,
    color: Color? = null,
    isBold: Boolean = false
) {
    val resolvedColor = color ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
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
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = resolvedColor
        )
    }
}
