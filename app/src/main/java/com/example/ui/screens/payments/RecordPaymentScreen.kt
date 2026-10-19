package com.example.ui.screens.payments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar
import com.example.data.model.ClientEntity
import com.example.data.model.CycleStatus
import com.example.data.model.PaymentMethod
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.components.ClientAvatar
import com.example.ui.screens.loans.AssignCreditDialog
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.util.AppLanguage

@Composable
fun RecordPaymentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val adaptiveSuccess = if (isDark) SuccessGreenLight else SuccessGreen
    val adaptiveWarning = if (isDark) WarningAmberLight else WarningAmber
    val adaptiveAccent = if (isDark) AccentSkyBlueLight else AccentSkyBlue
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val activeCycles = cycles.filter {
        it.status == CycleStatus.EN_COURS ||
        it.status == CycleStatus.EN_RETARD ||
        (it.balanceRemaining > 0 && it.status != CycleStatus.TERMINE && it.status != CycleStatus.ANNULE)
    }
    val selectedCycleId by viewModel.selectedCycleId.collectAsStateWithLifecycle()

    var showCyclePickerModal by remember { mutableStateOf(false) }
    var clientSearchQuery by remember { mutableStateOf("") }
    var showAssignCreditDialog by remember { mutableStateOf(false) }
    var preselectedClientForCredit by remember { mutableStateOf<ClientEntity?>(null) }
    var cyclePickerTab by remember { mutableIntStateOf(0) } // 0 = Crédits actifs, 1 = Tous les clients

    var selectedCycle by remember(selectedCycleId, activeCycles) {
        mutableStateOf(activeCycles.find { it.id == selectedCycleId } ?: activeCycles.firstOrNull())
    }

    LaunchedEffect(selectedCycleId, activeCycles) {
        if (!selectedCycleId.isNullOrEmpty()) {
            val matching = activeCycles.find { it.id.trim().equals(selectedCycleId!!.trim(), ignoreCase = true) }
            if (matching != null) {
                selectedCycle = matching
            }
        } else if (selectedCycle == null && activeCycles.isNotEmpty()) {
            selectedCycle = activeCycles.first()
        }
    }

    val principalReceived = selectedCycle?.principalAmount ?: 0.0
    val expectedAmount = principalReceived * 0.035

    var dayNumber by remember(selectedCycle) {
        mutableIntStateOf((selectedCycle?.daysElapsed ?: 0) + 1)
    }

    var actuallyCollectedInput by remember(selectedCycle) {
        mutableStateOf(if (expectedAmount > 0) expectedAmount.toLong().toString() else "3500")
    }

    LaunchedEffect(selectedCycle) {
        selectedCycle?.let { cycle ->
            dayNumber = cycle.daysElapsed + 1
            val expected = cycle.principalAmount * 0.035
            if (expected > 0) {
                actuallyCollectedInput = expected.toLong().toString()
            }
        }
    }

    val context = LocalContext.current
    var paymentDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastRecordedDateFormatted by remember { mutableStateOf("") }

    fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = paymentDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = paymentDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                paymentDateMillis = updatedCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = paymentDateMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = paymentDateMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                paymentDateMillis = updatedCal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    var paymentMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var agentNotes by remember { mutableStateOf("") }
    var isOfflineSimulation by remember { mutableStateOf(false) }

    var showSuccessReceipt by remember { mutableStateOf(false) }
    var lastRecordedAmount by remember { mutableStateOf("") }
    var lastExpectedAmount by remember { mutableStateOf("") }
    var lastClientName by remember { mutableStateOf("") }

    val isBiometricOpsEnabled by viewModel.isBiometricOpsEnabled.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    var showBiometricValidationDialog by remember { mutableStateOf(false) }

    val isOnline by viewModel.neonService.isOnline.collectAsStateWithLifecycle()
    val isEffectiveOffline = isOfflineSimulation || !isOnline

    fun processPaymentExecution() {
        val cycle = selectedCycle ?: return
        val actuallyCollected = actuallyCollectedInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            ?: actuallyCollectedInput.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        if (actuallyCollected > 0) {
            viewModel.recordPayment(
                cycleId = cycle.id,
                amount = actuallyCollected,
                dayNumber = dayNumber,
                paymentMethod = paymentMethod,
                notes = agentNotes.ifEmpty {
                    val diff = actuallyCollected - expectedAmount
                    when {
                        diff == 0.0 -> "Paiement conforme au montant attendu (3,5%)"
                        diff < 0.0 -> "Paiement partiel (reliquat de ${MicrocreditRepository.formatAmount(-diff)})"
                        else -> "Paiement avec surplus / avance (+${MicrocreditRepository.formatAmount(diff)})"
                    }
                },
                isOffline = isEffectiveOffline,
                paymentTimestamp = paymentDateMillis,
                onSuccess = {
                    lastRecordedAmount = MicrocreditRepository.formatAmount(actuallyCollected)
                    lastExpectedAmount = MicrocreditRepository.formatAmount(expectedAmount)
                    lastClientName = cycle.clientName
                    lastRecordedDateFormatted = MicrocreditRepository.formatDateTime(paymentDateMillis)
                    showSuccessReceipt = true
                    agentNotes = ""
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // En-tête de la page
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kukusanya Rejesho (Recouvrement)" else "Formulaire de Recouvrement",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Ukusanyaji wa fedha za mkopo uwanjani" else "Encaissement journalier (Taux standard 3,5 %)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mode Hors-ligne / En ligne toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEffectiveOffline) "Hors-ligne" else "En ligne",
                            fontSize = 11.sp,
                            color = if (isEffectiveOffline) adaptiveWarning else adaptiveSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isOfflineSimulation,
                            onCheckedChange = { isOfflineSimulation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = adaptiveWarning,
                                checkedTrackColor = adaptiveWarning.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Bannière explicative sur la synchronisation automatique
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEffectiveOffline) adaptiveWarning.copy(alpha = 0.12f) else adaptiveSuccess.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isEffectiveOffline) adaptiveWarning.copy(alpha = 0.35f) else adaptiveSuccess.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isEffectiveOffline) adaptiveWarning else adaptiveSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEffectiveOffline) {
                                if (currentLang == AppLanguage.SWAHILI)
                                    "Hali ya Nje ya Mtandao : Data inahifadhiwa kwenye simu na itasawazishwa moja kwa moja punde mtandao utakaporejea."
                                else
                                    "Mode Hors-Ligne : Enregistré en local • Synchronisation 100% automatique dès que la connexion internet est active."
                            } else {
                                if (currentLang == AppLanguage.SWAHILI)
                                    "Imeunganishwa : Usawazishaji wa moja kwa moja kwenye hifadhidata ya Neon PostgreSQL Cloud."
                                else
                                    "Connecté : Synchronisation automatique en direct vers la base Neon PostgreSQL Cloud."
                            },
                            fontSize = 11.sp,
                            color = if (isEffectiveOffline) adaptiveWarning else adaptiveSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Reçu électronique validé avec haute lisibilité en mode sombre
        if (showSuccessReceipt) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, adaptiveSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = adaptiveSuccess,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recouvrement Validé avec Succès",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = adaptiveSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$lastClientName • $lastRecordedAmount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Montant attendu (3,5%) : $lastExpectedAmount",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lastRecordedDateFormatted.isNotBlank()) {
                            Text(
                                text = "Encaissé le : $lastRecordedDateFormatted",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = adaptiveSuccess
                            )
                        }
                        Text(
                            text = if (isOfflineSimulation) "Enregistré localement (synchro automatique dès connexion)" else "Synchronisé en temps réel avec la caisse centrale",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showSuccessReceipt = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveSuccess,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enregistrer un autre recouvrement", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Formulaire principal
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. SÉLECTION DU CLIENT AYANT DÉJÀ REÇU UN CRÉDIT
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1. CLIENT AYANT REÇU UN CRÉDIT *",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            // Bouton direct pour affecter un nouveau crédit
                            OutlinedButton(
                                onClick = { showAssignCreditDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 32.dp).testTag("assign_credit_top_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Affecter Crédit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeCycles.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Aucun crédit actif trouvé. Affectez un crédit à un client pour pouvoir effectuer son recouvrement journalier.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp
                                    )
                                    Button(
                                        onClick = { showAssignCreditDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Affecter un Crédit Maintenant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Sélecteur visuel du client ayant reçu un crédit
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCyclePickerModal = true }
                                    .testTag("client_credit_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (selectedCycle != null) {
                                            val c = selectedCycle!!
                                            Text(
                                                text = c.clientName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Reçu : ${MicrocreditRepository.formatAmount(c.principalAmount)} • Attendu (3,5%) : ${MicrocreditRepository.formatAmount(c.principalAmount * 0.035)}/j",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Solde restant : ${MicrocreditRepository.formatAmount(c.balanceRemaining)} • Dossier : ${c.id}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "Sélectionner un client dans la liste (${activeCycles.size} avec crédit)",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showCyclePickerModal = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 32.dp)
                                    ) {
                                        Text("Changer", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // FICHE RÉCAPITULATIVE DU CRÉDIT SÉLECTIONNÉ
                    selectedCycle?.let { cycle ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Somme reçue en crédit :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = MicrocreditRepository.formatAmount(cycle.principalAmount),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Solde restant :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = MicrocreditRepository.formatAmount(cycle.balanceRemaining),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = adaptiveAccent
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Taux journalier :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF0C4A6E) else MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "3,5 % / jour",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // 2. CASE « MONTANT ATTENDU » (3,5 % DE LA SOMME REÇUE EN CRÉDIT)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = adaptiveWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "2. MONTANT ATTENDU (3,5 % DU CRÉDIT REÇU)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveWarning
                            )
                        }

                        OutlinedTextField(
                            value = if (selectedCycle != null) {
                                "${MicrocreditRepository.formatAmount(expectedAmount)}  (3,5 % de ${MicrocreditRepository.formatAmount(principalReceived)})"
                            } else {
                                "Sélectionnez un client ci-dessus"
                            },
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(Icons.Default.PriceCheck, contentDescription = null, tint = adaptiveWarning)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expected_amount_field"),
                            supportingText = {
                                Text(
                                    text = if (selectedCycle != null)
                                        "Calcul contractuel officiel : ${MicrocreditRepository.formatAmount(principalReceived)} × 3,5 % = ${MicrocreditRepository.formatAmount(expectedAmount)}"
                                    else
                                        "Veuillez choisir un client pour calculer les 3,5 %",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    // 3. CASE « MONTANT RÉELLEMENT RECOUVRÉ » (SOMME EFFECTIVEMENT VERSÉE)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = adaptiveSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3. MONTANT RÉELLEMENT RECOUVRÉ *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveSuccess
                            )
                        }

                        val actuallyCollected = actuallyCollectedInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                            ?: actuallyCollectedInput.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                        val difference = actuallyCollected - expectedAmount

                        OutlinedTextField(
                            value = actuallyCollectedInput,
                            onValueChange = { actuallyCollectedInput = it },
                            label = { Text("Somme effectivement versée par le client (FC) *") },
                            placeholder = { Text("Ex: ${expectedAmount.toLong()}") },
                            leadingIcon = {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = adaptiveSuccess)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("amount_actually_collected_input"),
                            supportingText = {
                                when {
                                    actuallyCollected <= 0.0 -> {
                                        Text("Veuillez saisir le montant effectivement remis par le client.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }
                                    difference == 0.0 -> {
                                        Text("✓ Versement complet conforme au montant attendu de 3,5 % (${MicrocreditRepository.formatAmount(expectedAmount)}).", color = adaptiveSuccess, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                    difference < 0.0 -> {
                                        Text("⚠ Versement partiel : reliquat de ${MicrocreditRepository.formatAmount(-diffToPos(difference))} reporté.", color = adaptiveWarning, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                    else -> {
                                        Text("★ Versement anticipé : surplus de +${MicrocreditRepository.formatAmount(difference)} déduit du solde.", color = adaptiveAccent, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }
                            }
                        )

                        // BADGE RÉCAPITULATIF DE L'ÉCART
                        if (actuallyCollected > 0 && selectedCycle != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = when {
                                            difference == 0.0 -> "Statut : Paiement exact (3,5%)"
                                            difference < 0.0 -> "Statut : Paiement partiel (-${MicrocreditRepository.formatAmount(-difference)})"
                                            else -> "Statut : Avance (+${MicrocreditRepository.formatAmount(difference)})"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            difference == 0.0 -> adaptiveSuccess
                                            difference < 0.0 -> adaptiveWarning
                                            else -> adaptiveAccent
                                        }
                                    )

                                    val newEstimatedBalance = (selectedCycle?.balanceRemaining ?: 0.0) - actuallyCollected
                                    Text(
                                        text = "Nouveau solde : ${MicrocreditRepository.formatAmount(if (newEstimatedBalance > 0) newEstimatedBalance else 0.0)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 4. DATE ET HEURE D'ENCAISSEMENT AUPRÈS DU CLIENT *
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "4. DATE ET HEURE D'ENCAISSEMENT AUPRÈS DU CLIENT *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_datetime_picker_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val isNarrow = maxWidth < 340.dp
                                    if (isNarrow) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Reçu auprès du client le :",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = MicrocreditRepository.formatDateTime(paymentDateMillis),
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showDatePicker() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 34.dp)
                                                        .testTag("change_payment_date_button")
                                                ) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }

                                                OutlinedButton(
                                                    onClick = { showTimePicker() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 34.dp)
                                                        .testTag("change_payment_time_button")
                                                ) {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Heure", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Reçu auprès du client le :",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = MicrocreditRepository.formatDateTime(paymentDateMillis),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = { showDatePicker() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 34.dp)
                                                        .testTag("change_payment_date_button")
                                                ) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }

                                                OutlinedButton(
                                                    onClick = { showTimePicker() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 34.dp)
                                                        .testTag("change_payment_time_button")
                                                ) {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Heure", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { paymentDateMillis = System.currentTimeMillis() },
                                        label = { Text("Maintenant", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                                        icon = { Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(13.dp)) },
                                        modifier = Modifier.heightIn(min = 30.dp)
                                    )
                                    Text(
                                        text = "Horodatage de la remise des fonds par le client.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 5. PARAMÈTRES SECONDAIRES DU RECOUVREMENT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dayNumber.toString(),
                            onValueChange = { dayNumber = it.toIntOrNull() ?: 1 },
                            label = { Text("Jour du cycle") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Column(modifier = Modifier.weight(2f)) {
                            Text("Mode de règlement :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PaymentMethod.entries.forEach { method ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { paymentMethod = method },
                                        label = { Text(method.label, fontSize = 10.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = agentNotes,
                        onValueChange = { agentNotes = it },
                        label = { Text("Observations de l'agent (Optionnel)") },
                        placeholder = { Text("Ex: Versement partiel, promis le complément demain...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // BOUTON DE VALIDATION DU RECOUVREMENT
                    val cleanValStr = actuallyCollectedInput.replace(" ", "").replace(",", ".")
                    val actuallyCollectedVal = cleanValStr.toDoubleOrNull()
                        ?: actuallyCollectedInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                        ?: 0.0
                    Button(
                        onClick = {
                            if (selectedCycle != null && actuallyCollectedVal > 0) {
                                if (isBiometricOpsEnabled) {
                                    showBiometricValidationDialog = true
                                } else {
                                    processPaymentExecution()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedCycle != null && actuallyCollectedVal > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_recovery_button")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (actuallyCollectedVal > 0)
                                "Valider le Recouvrement (${MicrocreditRepository.formatAmount(actuallyCollectedVal)})"
                            else
                                "Valider le Recouvrement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BOUTON "AFFECTER CRÉDIT" DIRECTEMENT AU BAS DU FORMULAIRE (100% RESPONSIVE À TOUS LES ÉCRANS)
                    OutlinedButton(
                        onClick = { showAssignCreditDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("record_payment_assign_credit_bottom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo (Affecter Crédit)" else "Affecter un Crédit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Action additionnelle en bas de page pour affecter un nouveau crédit
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    val isNarrow = maxWidth < 340.dp
                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Unahitaji kutoa mkopo mpya?" else "Nouveau prêt à octroyer ?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi mkopo kwa mteja kwa haraka" else "Affecter un crédit à un client en 1 clic",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showAssignCreditDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                                    .testTag("quick_assign_credit_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo (Affecter)" else "Affecter un Crédit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Unahitaji kutoa mkopo mpya?" else "Nouveau prêt à octroyer ?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi mkopo kwa mteja kwa haraka" else "Affecter un crédit à un client en 1 clic",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showAssignCreditDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("quick_assign_credit_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kabidhi Mkopo" else "Affecter Crédit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal pour sélectionner le client à recouvrer parmi tous les crédits actifs
    if (showCyclePickerModal) {
        val filteredCycles = activeCycles.filter {
            clientSearchQuery.isBlank() ||
                    it.clientName.contains(clientSearchQuery, ignoreCase = true) ||
                    it.id.contains(clientSearchQuery, ignoreCase = true) ||
                    it.marketZone.contains(clientSearchQuery, ignoreCase = true)
        }

        val filteredClients = clients.filter {
            clientSearchQuery.isBlank() ||
                    it.fullName.contains(clientSearchQuery, ignoreCase = true) ||
                    it.phone.contains(clientSearchQuery, ignoreCase = true) ||
                    it.marketZone.contains(clientSearchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showCyclePickerModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Choisir le Client à Recouvrer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { showCyclePickerModal = false }) {
                        Icon(Icons.Default.Check, contentDescription = "Fermer")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Onglets : Crédits en cours VS Tous les clients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = cyclePickerTab == 0,
                            onClick = { cyclePickerTab = 0 },
                            label = { Text("Crédits actifs (${activeCycles.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = cyclePickerTab == 1,
                            onClick = { cyclePickerTab = 1 },
                            label = { Text("Tous clients (${clients.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = clientSearchQuery,
                        onValueChange = { clientSearchQuery = it },
                        placeholder = { Text("Rechercher nom, tél, dossier...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (cyclePickerTab == 0) {
                        // LISTE DES CRÉDITS ACTIFS
                        if (filteredCycles.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Aucun crédit actif trouvé.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        showCyclePickerModal = false
                                        showAssignCreditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Affecter un crédit maintenant", fontSize = 11.5.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(filteredCycles) { cycle ->
                                    val isSelected = selectedCycle?.id == cycle.id
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else
                                                MaterialTheme.colorScheme.surface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCycle = cycle
                                                dayNumber = cycle.daysElapsed + 1
                                                val expected = cycle.principalAmount * 0.035
                                                actuallyCollectedInput = expected.toLong().toString()
                                                showCyclePickerModal = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(cycle.clientName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = "Reçu : ${MicrocreditRepository.formatAmount(cycle.principalAmount)} • Attendu (3,5%) : ${MicrocreditRepository.formatAmount(cycle.principalAmount * 0.035)}/j",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Solde restant : ${MicrocreditRepository.formatAmount(cycle.balanceRemaining)} • Zone : ${cycle.marketZone}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // LISTE DE TOUS LES CLIENTS EXISTANTS
                        if (filteredClients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun client trouvé.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(filteredClients) { client ->
                                    val clientCycle = activeCycles.find { it.clientId == client.id }
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ClientAvatar(
                                                gender = client.gender,
                                                photoUrl = client.avatarUrl,
                                                size = 36.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(client.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text("${client.phone} • ${client.marketZone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (clientCycle != null) {
                                                    Text(
                                                        text = "Crédit actif : ${MicrocreditRepository.formatAmount(clientCycle.principalAmount)} (Attendu 3,5% : ${MicrocreditRepository.formatAmount(clientCycle.principalAmount * 0.035)}/j)",
                                                        fontSize = 10.5.sp,
                                                        color = SuccessGreen,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Aucun crédit actif",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (clientCycle != null) {
                                                Button(
                                                    onClick = {
                                                        selectedCycle = clientCycle
                                                        dayNumber = clientCycle.daysElapsed + 1
                                                        val expected = clientCycle.principalAmount * 0.035
                                                        actuallyCollectedInput = expected.toLong().toString()
                                                        showCyclePickerModal = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.heightIn(min = 30.dp)
                                                ) {
                                                    Text("Recouvrer", fontSize = 11.sp)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        preselectedClientForCredit = client
                                                        showCyclePickerModal = false
                                                        showAssignCreditDialog = true
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.heightIn(min = 30.dp)
                                                ) {
                                                    Text("+ Crédit", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCyclePickerModal = false }) {
                    Text("Valider")
                }
            }
        )
    }

    // Dialogue d'affectation de crédit directement depuis l'écran de recouvrement
    if (showAssignCreditDialog) {
        AssignCreditDialog(
            clients = clients,
            preselectedClient = preselectedClientForCredit,
            onDismiss = {
                showAssignCreditDialog = false
                preselectedClientForCredit = null
            },
            onConfirm = { clientId, amount, durationDays, purpose, pMethod, newClientName, newClientPhone, newClientMarket ->
                showAssignCreditDialog = false
                val targetClient = clients.find { it.id == clientId } ?: preselectedClientForCredit
                viewModel.assignCreditToClient(
                    clientId = clientId,
                    principalAmount = amount,
                    durationDays = durationDays,
                    purpose = purpose,
                    marketZone = newClientMarket ?: targetClient?.marketZone ?: "Marché de Kituku",
                    activityType = targetClient?.activityType ?: "Commerce",
                    paymentMethod = pMethod,
                    newClientName = newClientName,
                    newClientPhone = newClientPhone,
                    onSuccess = { createdCycle ->
                        preselectedClientForCredit = null
                        selectedCycle = createdCycle
                        dayNumber = 1
                        val expected = createdCycle.principalAmount * 0.035
                        actuallyCollectedInput = expected.toLong().toString()
                    }
                )
            }
        )
    }

    if (showBiometricValidationDialog) {
        BiometricPromptDialog(
            title = "Validation du Recouvrement",
            subtitle = "${selectedCycle?.clientName} • ${MicrocreditRepository.formatAmount(actuallyCollectedInput.toDoubleOrNull() ?: 0.0)}",
            onSuccess = {
                showBiometricValidationDialog = false
                processPaymentExecution()
            },
            onDismiss = { showBiometricValidationDialog = false }
        )
    }
}

private fun diffToPos(diff: Double): Double = if (diff < 0) -diff else diff
