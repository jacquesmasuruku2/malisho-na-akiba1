package com.example.ui.screens.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

data class CycleProjection(
    val cycleNumber: Int,
    val startingCapital: Double,
    val loansCount: Int,
    val totalPrincipal: Double,
    val totalContractual: Double,
    val estimatedLoss: Double,
    val estimatedExpenses: Double,
    val netProfit: Double,
    val endingCapital: Double
)

@Composable
fun FinancialProjectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var startingCapitalInput by remember { mutableStateOf("5000000") }
    var loansCountInput by remember { mutableStateOf("25") }
    var avgTicketInput by remember { mutableStateOf("200000") }
    var dailyRateInput by remember { mutableStateOf("3.5") }
    var lossRateInput by remember { mutableStateOf("2.0") }
    var expenseRateInput by remember { mutableStateOf("5.0") }

    val startCap = startingCapitalInput.toDoubleOrNull() ?: 5000000.0
    val loans = loansCountInput.toIntOrNull() ?: 25
    val avgTicket = avgTicketInput.toDoubleOrNull() ?: 200000.0
    val dailyRate = (dailyRateInput.toDoubleOrNull() ?: 3.5) / 100.0
    val lossRate = (lossRateInput.toDoubleOrNull() ?: 2.0) / 100.0
    val expRate = (expenseRateInput.toDoubleOrNull() ?: 5.0) / 100.0

    // Compute 6-Cycle Projections iteratively
    val projections = remember(startCap, loans, avgTicket, dailyRate, lossRate, expRate) {
        var currentCap = startCap
        val list = mutableListOf<CycleProjection>()
        for (i in 1..6) {
            val totalPrincipal = (loans * avgTicket).coerceAtMost(currentCap)
            val contractual = totalPrincipal * (1 + (dailyRate * 30 - 1)) // principal * 3.5% * 30 = principal * 1.05
            val grossProfit = contractual - totalPrincipal
            val loss = totalPrincipal * lossRate
            val exp = totalPrincipal * expRate
            val net = (grossProfit * 0.70 - exp - loss).coerceAtLeast(0.0)
            val endingCap = currentCap + net
            list.add(
                CycleProjection(
                    cycleNumber = i,
                    startingCapital = currentCap,
                    loansCount = loans,
                    totalPrincipal = totalPrincipal,
                    totalContractual = contractual,
                    estimatedLoss = loss,
                    estimatedExpenses = exp,
                    netProfit = net,
                    endingCapital = endingCap
                )
            )
            currentCap = endingCap
        }
        list
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
                IconButton(onClick = { viewModel.navigateTo("more") }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = "Modèle de Simulation sur 6 Cycles",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Projection de croissance & rentabilité (Modèle Excel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Disclaimer Banner (Requirement 29)
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cette fonctionnalité est un outil prévisionnel de simulation mathématique et ne constitue pas un résultat comptable réel.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        // Simulation Parameters Inputs
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PARAMÈTRES DE LA SIMULATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startingCapitalInput,
                            onValueChange = { startingCapitalInput = it },
                            label = { Text("Capital départ (FC)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = loansCountInput,
                            onValueChange = { loansCountInput = it },
                            label = { Text("Prêts / Cycle") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = avgTicketInput,
                            onValueChange = { avgTicketInput = it },
                            label = { Text("Ticket moyen (FC)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dailyRateInput,
                            onValueChange = { dailyRateInput = it },
                            label = { Text("Taux/jour (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Summary Card at Cycle 6
        item {
            val finalCap = projections.lastOrNull()?.endingCapital ?: startCap
            val totalProfit6Cycles = finalCap - startCap

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RÉSULTAT PROJETÉ AU CYCLE 6 (M+6)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(MicrocreditRepository.formatAmount(finalCap), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gain net estimé : +${MicrocreditRepository.formatAmount(totalProfit6Cycles)} (${String.format("%.1f", (totalProfit6Cycles / startCap) * 100)} % de ROI)",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Detailed Cycle by Cycle projection cards
        item {
            Text("Évolution Cycle par Cycle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(projections) { p ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cycle ${p.cycleNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Capital fin : ${MicrocreditRepository.formatAmount(p.endingCapital)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Capital engagé : ${MicrocreditRepository.formatAmount(p.totalPrincipal)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Bénéfice net cycle : +${MicrocreditRepository.formatAmount(p.netProfit)}", fontSize = 11.sp, color = AccentSkyBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
