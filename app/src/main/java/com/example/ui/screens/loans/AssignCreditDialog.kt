package com.example.ui.screens.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientEntity
import com.example.data.model.PaymentMethod
import com.example.data.repository.MicrocreditRepository
import com.example.ui.components.ClientAvatar
import com.example.ui.theme.SuccessGreen

@Composable
fun AssignCreditDialog(
    clients: List<ClientEntity>,
    preselectedClient: ClientEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        clientId: String,
        amount: Double,
        durationDays: Int,
        purpose: String,
        paymentMethod: PaymentMethod,
        newClientName: String?,
        newClientPhone: String?,
        newClientMarket: String?
    ) -> Unit
) {
    var isNewClientMode by remember { mutableStateOf(clients.isEmpty()) }
    var selectedClient by remember(preselectedClient, clients) {
        mutableStateOf(preselectedClient ?: clients.firstOrNull())
    }

    LaunchedEffect(preselectedClient, clients) {
        if (selectedClient == null && clients.isNotEmpty()) {
            selectedClient = preselectedClient ?: clients.first()
        }
    }

    var newClientName by remember { mutableStateOf("") }
    var newClientPhone by remember { mutableStateOf("+243 ") }
    var newClientMarket by remember { mutableStateOf("Marché de Kituku") }

    val nextMatricule = remember(clients) {
        val maxNumber = clients.mapNotNull {
            com.example.data.repository.MicrocreditRepository.extractClientSequenceNumber(it.id)
        }.maxOrNull() ?: 0
        String.format(java.util.Locale.US, "CLI-%03d", maxNumber + 1)
    }
    val nextQuantieme = remember(nextMatricule) {
        com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(nextMatricule)
    }

    var showClientPickerModal by remember { mutableStateOf(false) }
    var clientSearchFilter by remember { mutableStateOf("") }

    var amountInput by remember { mutableStateOf("100000") }
    var durationDaysInput by remember { mutableStateOf("30") }
    var purposeInput by remember { mutableStateOf("Activité commerciale") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }

    val cleanAmountStr = amountInput.replace(" ", "").replace(",", ".")
    val amount = cleanAmountStr.toDoubleOrNull()
        ?: amountInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        ?: 0.0

    val durationDays = durationDaysInput.filter { it.isDigit() }.toIntOrNull() ?: 30
    val dailyExpected = amount * 0.035 // 3,5% par jour
    val totalContractual = dailyExpected * durationDays

    val canSubmit = amount > 0 && ((!isNewClientMode && selectedClient != null) || (isNewClientMode && newClientName.isNotBlank()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Affecter un Crédit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Octroi officiel avec calcul standard à 3,5 % / jour",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. SÉLECTION OU CRÉATION DU CLIENT BÉNÉFICIAIRE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Client bénéficiaire :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (clients.isNotEmpty()) {
                            TextButton(
                                onClick = { isNewClientMode = !isNewClientMode },
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNewClientMode) Icons.Default.Person else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isNewClientMode) "Client existant" else "+ Nouveau", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (!isNewClientMode && selectedClient != null) {
                    val client = selectedClient!!
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClientPickerModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClientAvatar(
                                gender = client.gender,
                                photoUrl = client.avatarUrl,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val quantieme = com.example.data.repository.MicrocreditRepository.getClientQuantiemeLabel(client.id)
                                Text(
                                    text = "${client.id}${if (quantieme.isNotEmpty()) " • $quantieme" else ""} • ${client.phone}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Secteur : ${client.marketZone} • ${client.activityType.ifEmpty { "Commerce" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { showClientPickerModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Changer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else if (!isNewClientMode && clients.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClientPickerModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Choisir parmi les clients existants",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${clients.size} client(s) répertorié(s)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showClientPickerModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Choisir", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // Saisie directe d'un nouveau client bénéficiaire
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Matricule attribué automatiquement : $nextMatricule • $nextQuantieme",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newClientName,
                            onValueChange = { newClientName = it },
                            label = { Text("Nom complet du client *") },
                            placeholder = { Text("Ex: Bernadette Kavira") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newClientPhone,
                            onValueChange = { newClientPhone = it },
                            label = { Text("Numéro de téléphone") },
                            placeholder = { Text("Ex: +243 990 000 123") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newClientMarket,
                            onValueChange = { newClientMarket = it },
                            label = { Text("Zone / Marché d'activité") },
                            placeholder = { Text("Ex: Marché de Kituku, Virunga...") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 2. MONTANT DU CRÉDIT (SOMME REÇUE)
                Text(
                    text = "2. Somme reçue en crédit (FC) :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Montant du prêt (FC) *") },
                    placeholder = { Text("Ex: 100000 ou 200000") },
                    leadingIcon = {
                        Icon(Icons.Default.PriceCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assign_credit_amount_input")
                )

                // Raccourcis montants fréquents 100% responsifs sur 2 lignes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Première ligne : 50 000 FC et 100 000 FC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50000", "100000").forEach { preset ->
                            FilterChip(
                                selected = amountInput == preset,
                                onClick = { amountInput = preset },
                                label = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = MicrocreditRepository.formatAmount(preset.toDouble()),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                                    .testTag("preset_${preset}_button")
                            )
                        }
                    }

                    // Deuxième ligne : 300 000 FC (placé sous 50 000 FC) et 200 000 FC (placé sous 100 000 FC) - 100% responsive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("300000", "200000").forEach { preset ->
                            FilterChip(
                                selected = amountInput == preset,
                                onClick = { amountInput = preset },
                                label = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = MicrocreditRepository.formatAmount(preset.toDouble()),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                                    .testTag("preset_${preset}_button")
                            )
                        }
                    }
                }

                // 3. APERÇU FINANCIER CONTRACTUEL (3,5%)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Maquette financière officielle (3,5% / jour) :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Montant attendu / jour (3,5%) :", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = MicrocreditRepository.formatAmount(dailyExpected),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Durée prévue du cycle :", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$durationDays jours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total contractuel à rembourser :", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = MicrocreditRepository.formatAmount(totalContractual),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 4. OBJET DU CRÉDIT & MODE DE DÉCAISSEMENT
                OutlinedTextField(
                    value = purposeInput,
                    onValueChange = { purposeInput = it },
                    label = { Text("Objet du crédit / Activité") },
                    placeholder = { Text("Ex: Commerce friperie, denrées alimentaires...") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mode de décaissement :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.label, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        val finalClientId = if (!isNewClientMode && selectedClient != null) {
                            selectedClient!!.id
                        } else {
                            nextMatricule
                        }
                        onConfirm(
                            finalClientId,
                            amount,
                            durationDays,
                            purposeInput,
                            selectedMethod,
                            if (isNewClientMode) newClientName.trim() else null,
                            if (isNewClientMode) newClientPhone.trim() else null,
                            if (isNewClientMode) newClientMarket.trim() else null
                        )
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_assign_credit_button")
            ) {
                Text(
                    text = if (amount > 0)
                        "Affecter le Crédit (${MicrocreditRepository.formatAmount(amount)})"
                    else
                        "Affecter le Crédit",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    // Modal pour sélectionner parmi tous les clients existants
    if (showClientPickerModal) {
        AlertDialog(
            onDismissRequest = { showClientPickerModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Choisir un Client Existant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { showClientPickerModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            },
            text = {
                val filteredClients = clients.filter {
                    clientSearchFilter.isBlank() ||
                            it.fullName.contains(clientSearchFilter, ignoreCase = true) ||
                            it.phone.contains(clientSearchFilter, ignoreCase = true) ||
                            it.marketZone.contains(clientSearchFilter, ignoreCase = true)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = clientSearchFilter,
                        onValueChange = { clientSearchFilter = it },
                        placeholder = { Text("Rechercher nom, tél ou marché...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

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
                                val isSelected = selectedClient?.id == client.id
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
                                            selectedClient = client
                                            isNewClientMode = false
                                            showClientPickerModal = false
                                        }
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
                                            Text(client.fullName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${client.phone} • ${client.marketZone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showClientPickerModal = false }) {
                    Text("Valider la sélection")
                }
            }
        )
    }
}
