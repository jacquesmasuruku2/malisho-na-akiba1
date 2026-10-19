package com.example.ui.screens.caisse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CashMovementCategory
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashMovementType
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.components.CashMovementBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CaisseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val movements by viewModel.allCashMovements.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isBiometricOpsEnabled by viewModel.isBiometricOpsEnabled.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("Toutes") }
    var showAddMovementDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(CashMovementType.ENTREE) }
    var pendingMovementData by remember {
        mutableStateOf<Pair<CashMovementType, Pair<CashMovementCategory, Pair<String, Pair<Double, Pair<PaymentMethod, String>>>>>?>(null)
    }

    val totalEntrees = movements.filter { it.type == CashMovementType.ENTREE }.sumOf { it.amount }
    val totalSorties = movements.filter { it.type == CashMovementType.SORTIE }.sumOf { it.amount }

    val filteredMovements = movements.filter { mov ->
        when (selectedFilter) {
            "Entrées" -> mov.type == CashMovementType.ENTREE
            "Sorties" -> mov.type == CashMovementType.SORTIE
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Solde de caisse Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOLDE DE CAISSE EN DIRECT",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = MicrocreditRepository.formatAmount(cashBalance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Total Entrées", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(MicrocreditRepository.formatAmount(totalEntrees), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Total Sorties", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(MicrocreditRepository.formatAmount(totalSorties), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons (Only Cashier and Manager) - Responsive on all screen sizes
        if (currentUser?.role == UserRole.GESTIONNAIRE || currentUser?.role == UserRole.CAISSIER) {
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isSmallScreen = maxWidth < 340.dp
                    if (isSmallScreen) {
                        // Stacked layout for very compact screens to avoid squishing
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogInitialType = CashMovementType.ENTREE
                                    showAddMovementDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_caisse_entree")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "Kuingiza Pesa (+)" else "Nouvelle Entrée (+)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    dialogInitialType = CashMovementType.SORTIE
                                    showAddMovementDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_caisse_sortie")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "Kutoa Pesa (-)" else "Nouvelle Sortie (-)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // 2-column layout with flexible text and minimum touch target size
                        val isWide = maxWidth >= 420.dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogInitialType = CashMovementType.ENTREE
                                    showAddMovementDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_caisse_entree")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) {
                                            if (isWide) "Kuingiza Pesa (+)" else "Kuingiza (+)"
                                        } else {
                                            if (isWide) "Nouvelle Entrée (+)" else "Entrée (+)"
                                        },
                                        fontSize = if (isWide) 13.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    dialogInitialType = CashMovementType.SORTIE
                                    showAddMovementDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_caisse_sortie")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) {
                                            if (isWide) "Kutoa Pesa (-)" else "Kutoa (-)"
                                        } else {
                                            if (isWide) "Nouvelle Sortie (-)" else "Sortie (-)"
                                        },
                                        fontSize = if (isWide) 13.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Journal des opérations + Barre de filtres adaptative (Toutes, Entrées, Sorties)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // En-tête avec titre et compteur total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Shughuli za Pesa" else "Journal des Opérations",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${filteredMovements.size} / ${movements.size}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Barre de filtre adaptative à TOUS les écrans (Toutes, Entrées, Sorties)
                // Chaque bouton dispose d'un weight(1f), garantissant visibilité et accessibilité sans rognage, en particulier pour le bouton Sorties !
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val countToutes = movements.size
                        val countEntrees = movements.count { it.type == CashMovementType.ENTREE }
                        val countSorties = movements.count { it.type == CashMovementType.SORTIE }

                        // 1. Bouton "Toutes"
                        CaisseFilterTabButton(
                            label = if (currentLang == AppLanguage.SWAHILI) "Zote" else "Toutes",
                            count = countToutes,
                            isSelected = selectedFilter == "Toutes",
                            selectedColor = PrimaryNavy,
                            icon = Icons.Default.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            testTag = "filter_caisse_toutes",
                            onClick = { selectedFilter = "Toutes" }
                        )

                        // 2. Bouton "Entrées"
                        CaisseFilterTabButton(
                            label = if (currentLang == AppLanguage.SWAHILI) "Kuingia" else "Entrées",
                            count = countEntrees,
                            isSelected = selectedFilter == "Entrées",
                            selectedColor = SuccessGreen,
                            icon = Icons.Default.ArrowDownward,
                            modifier = Modifier.weight(1f),
                            testTag = "filter_caisse_entrees",
                            onClick = { selectedFilter = "Entrées" }
                        )

                        // 3. Bouton "Sorties" (adapté à 100% à tous les écrans, jamais masqué ou tronqué)
                        CaisseFilterTabButton(
                            label = if (currentLang == AppLanguage.SWAHILI) "Kutoka" else "Sorties",
                            count = countSorties,
                            isSelected = selectedFilter == "Sorties",
                            selectedColor = ErrorRed,
                            icon = Icons.Default.ArrowUpward,
                            modifier = Modifier.weight(1f),
                            testTag = "filter_caisse_sorties",
                            onClick = { selectedFilter = "Sorties" }
                        )
                    }
                }
            }
        }

        if (filteredMovements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun mouvement enregistré.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredMovements) { mov ->
                CashMovementItemCard(movement = mov)
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showAddMovementDialog) {
        AddCashMovementDialog(
            initialType = dialogInitialType,
            onDismiss = { showAddMovementDialog = false },
            onConfirm = { type, category, desc, amount, method, ref ->
                if (isBiometricOpsEnabled) {
                    showAddMovementDialog = false
                    pendingMovementData = Pair(type, Pair(category, Pair(desc, Pair(amount, Pair(method, ref)))))
                } else {
                    viewModel.recordCashMovement(
                        type = type,
                        category = category,
                        description = desc,
                        amount = amount,
                        paymentMethod = method,
                        reference = ref,
                        onSuccess = { showAddMovementDialog = false }
                    )
                }
            }
        )
    }

    pendingMovementData?.let { pending ->
        val (type, rest1) = pending
        val (category, rest2) = rest1
        val (desc, rest3) = rest2
        val (amount, rest4) = rest3
        val (method, ref) = rest4

        BiometricPromptDialog(
            title = if (type == CashMovementType.SORTIE) {
                if (currentLang == AppLanguage.SWAHILI) "Kuidhinisha Kutoa Pesa kwa Alama ya Kidole" else "Autorisation Biométrique de Sortie de Caisse"
            } else {
                if (currentLang == AppLanguage.SWAHILI) "Kuidhinisha Kuingiza Pesa kwa Alama ya Kidole" else "Validation Biométrique d'Entrée de Caisse"
            },
            subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako ili kuthibitisha muamala" else "Posez votre doigt sur le capteur pour signer l'opération de caisse",
            operationSummary = "${if (type == CashMovementType.SORTIE) "Sortie" else "Entrée"} de ${MicrocreditRepository.formatAmount(amount)} (${category.label}) - $desc",
            onSuccess = {
                viewModel.recordCashMovement(
                    type = type,
                    category = category,
                    description = desc,
                    amount = amount,
                    paymentMethod = method,
                    reference = ref,
                    onSuccess = {
                        pendingMovementData = null
                    }
                )
            },
            onDismiss = {
                pendingMovementData = null
            }
        )
    }
}

@Composable
fun CashMovementItemCard(
    movement: CashMovementEntity,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CashMovementBadge(type = movement.type)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = movement.category.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(movement.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${movement.dateString} • Réf: ${movement.reference} • Par: ${movement.recordedByName} (${movement.paymentMethod.label})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (movement.type == CashMovementType.ENTREE) "+" else "-"} ${MicrocreditRepository.formatAmount(movement.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (movement.type == CashMovementType.ENTREE) SuccessGreen else ErrorRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashMovementDialog(
    initialType: CashMovementType,
    onDismiss: () -> Unit,
    onConfirm: (CashMovementType, CashMovementCategory, String, Double, PaymentMethod, String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var category by remember {
        mutableStateOf(if (initialType == CashMovementType.ENTREE) CashMovementCategory.REMBOURSEMENT else CashMovementCategory.CHARGE)
    }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (type == CashMovementType.ENTREE) "Enregistrer une Entrée en Caisse" else "Enregistrer une Sortie de Caisse", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector : Entrée (+) et Sortie (-) adaptées et équilibrées à 100% de la largeur
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == CashMovementType.ENTREE,
                        onClick = {
                            type = CashMovementType.ENTREE
                            category = CashMovementCategory.REMBOURSEMENT
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Entrée (+)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("dialog_chip_entree")
                    )
                    FilterChip(
                        selected = type == CashMovementType.SORTIE,
                        onClick = {
                            type = CashMovementType.SORTIE
                            category = CashMovementCategory.CHARGE
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sortie (-)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ErrorRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("dialog_chip_sortie")
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Montant (FC) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Libellé de l'opération *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("N° Pièce / Réf Reçu (ex: FOU-01)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mode de règlement :", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.ESPECES, PaymentMethod.MOBILE_MONEY, PaymentMethod.BANQUE).forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = {
                                Text(
                                    text = m.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && description.isNotBlank()) {
                        onConfirm(type, category, description, amount, paymentMethod, reference)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (type == CashMovementType.ENTREE) SuccessGreen else ErrorRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(if (type == CashMovementType.ENTREE) "Enregistrer l'Entrée" else "Enregistrer la Sortie", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Bouton de filtre adaptatif avec support de tous les ratios d'écran,
 * icônes d'orientation, badges de comptage et états sélectionnés distinctifs.
 */
@Composable
private fun CaisseFilterTabButton(
    label: String,
    count: Int,
    isSelected: Boolean,
    selectedColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val badgeBg = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = badgeBg,
                shape = CircleShape,
                modifier = Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}
