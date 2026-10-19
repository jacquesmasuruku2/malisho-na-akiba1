package com.example.ui.screens.loans

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.components.ClientAvatar
import com.example.ui.components.LoanStatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun LoanApplicationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val applications by viewModel.allLoanApplications.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isBiometricOpsEnabled by viewModel.isBiometricOpsEnabled.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("Toutes") }
    var applicationToReview by remember { mutableStateOf<LoanApplicationEntity?>(null) }
    var applicationToDisburse by remember { mutableStateOf<LoanApplicationEntity?>(null) }
    var showCreateApplicationDialog by remember { mutableStateOf(false) }
    var pendingDisbursement by remember { mutableStateOf<Pair<LoanApplicationEntity, PaymentMethod>?>(null) }

    val filteredApps = applications.filter { app ->
        when (selectedFilter) {
            "Soumises" -> app.status == LoanStatus.SOUMISE || app.status == LoanStatus.EN_ETUDE
            "Approuvées" -> app.status == LoanStatus.APPROUVEE
            "Décaissées" -> app.status == LoanStatus.DECAISSEE
            "Refusées" -> app.status == LoanStatus.REFUSEE
            else -> true
        }
    }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Demandes de Prêt",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Marchés de Goma • Collecte journalière 3,5%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo("client_loan_apply") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+ Nouveau Prêt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Toutes", "Soumises", "Approuvées", "Décaissées", "Refusées").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune demande de prêt trouvée.", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps) { app ->
                        LoanApplicationCard(
                            application = app,
                            userRole = currentUser?.role,
                            onReviewClick = { applicationToReview = app },
                            onDisburseClick = { applicationToDisburse = app }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    if (showCreateApplicationDialog) {
        FullLoanApplicationDialog(
            clientId = "CLI-${(100..999).random()}",
            initialClientName = "",
            initialClientPhone = "",
            initialQuarter = "",
            initialMarketZone = "Marché de Kituku",
            onDismiss = { showCreateApplicationDialog = false },
            onSubmit = { amount, duration, purp, activ, zone, quart, idCard, photoUri, gName, gPhone, acceptedTerms ->
                viewModel.submitLoanApplication(
                    clientId = "CLI-${(100..999).random()}",
                    amount = amount,
                    durationDays = duration,
                    purpose = purp,
                    activityType = activ,
                    marketZone = zone,
                    quarter = quart,
                    guarantorName = gName,
                    guarantorPhone = gPhone,
                    idCardNumber = idCard,
                    clientPhotoUri = photoUri,
                    acceptedTerms = acceptedTerms,
                    onSuccess = {
                        showCreateApplicationDialog = false
                    }
                )
            }
        )
    }

    // Review Dialog (Approve / Reject)
    applicationToReview?.let { app ->
        ReviewApplicationDialog(
            application = app,
            onDismiss = { applicationToReview = null },
            onConfirm = { approved, notes ->
                viewModel.reviewLoanApplication(
                    applicationId = app.id,
                    approved = approved,
                    reviewNotes = notes,
                    onSuccess = { applicationToReview = null }
                )
            }
        )
    }

    // Disbursement Dialog (Create cycle + Cash outflow)
    applicationToDisburse?.let { app ->
        DisburseLoanDialog(
            application = app,
            onDismiss = { applicationToDisburse = null },
            onConfirm = { method ->
                if (isBiometricOpsEnabled) {
                    applicationToDisburse = null
                    pendingDisbursement = Pair(app, method)
                } else {
                    viewModel.disburseLoan(
                        applicationId = app.id,
                        paymentMethod = method,
                        onSuccess = { applicationToDisburse = null }
                    )
                }
            }
        )
    }

    pendingDisbursement?.let { (app, method) ->
        BiometricPromptDialog(
            title = if (currentLang == AppLanguage.SWAHILI) "Kuidhinisha Kutoa Mkopo kwa Alama ya Kidole" else "Autorisation Biométrique de Décaissement",
            subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako kwenye kitambuzi ili kutoa pesa" else "Validation de la Direction pour libérer les fonds au client",
            operationSummary = "Décaissement de ${MicrocreditRepository.formatAmount(app.amountRequested)} pour ${app.clientName} (${method.label})",
            onSuccess = {
                viewModel.disburseLoan(
                    applicationId = app.id,
                    paymentMethod = method,
                    onSuccess = {
                        pendingDisbursement = null
                    }
                )
            },
            onDismiss = {
                pendingDisbursement = null
            }
        )
    }
}

@Composable
fun LoanApplicationCard(
    application: LoanApplicationEntity,
    userRole: UserRole?,
    onReviewClick: () -> Unit,
    onDisburseClick: () -> Unit,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClientAvatar(
                        gender = application.gender,
                        photoUrl = application.clientPhotoUri,
                        size = 42.dp,
                        showGenderBadge = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(application.clientName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${application.marketZone} • Q. ${application.quarter.ifEmpty { "Goma" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                LoanStatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Montant demandé", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MicrocreditRepository.formatAmount(application.amountRequested), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Collecte journalière (3,5%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${MicrocreditRepository.formatAmount(application.amountRequested * 0.035)} / jour",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Activité", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(application.activityType, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Objet : ${application.purpose}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Téléphone : ${application.clientPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (application.idCardNumber.isNotBlank()) {
                Text("Pièce d'identité : ${application.idCardNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Pièce d'identité : Non renseignée (facultatif)", fontSize = 11.sp, color = TextMuted)
            }

            Text("Modalités : Contrat 50% bénéfices validé • Passage agent chaque soir au marché", fontSize = 11.sp, color = AccentSkyBlue)

            val context = androidx.compose.ui.platform.LocalContext.current
            if (!application.signatureId.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDCFCE7))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Signé biométriquement (${application.signatureId})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                    IconButton(
                        onClick = {
                            val res = com.example.util.LoanApplicationPdfGenerator.generateLoanApplicationPdf(context, application)
                            res.getOrNull()?.let { file ->
                                com.example.util.LoanApplicationPdfGenerator.viewOrShareLoanApplicationPdf(context, file)
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Télécharger reçu",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (application.reviewNotes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Avis gestionnaire : ${application.reviewNotes}",
                    fontSize = 11.sp,
                    color = AccentSkyBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            // Actions according to role and status
            if (userRole == UserRole.GESTIONNAIRE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (application.status == LoanStatus.SOUMISE || application.status == LoanStatus.EN_ETUDE) {
                        Button(
                            onClick = onReviewClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Étudier & Décider", fontSize = 12.sp)
                        }
                    }

                    if (application.status == LoanStatus.APPROUVEE) {
                        Button(
                            onClick = onDisburseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Autoriser Décaissement", fontSize = 12.sp)
                        }
                    }
                }
            } else if (userRole == UserRole.CAISSIER && application.status == LoanStatus.APPROUVEE) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDisburseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exécuter Décaissement Caisse", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ReviewApplicationDialog(
    application: LoanApplicationEntity,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Décision sur la demande ${application.id}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Client : ${application.clientName}", fontWeight = FontWeight.SemiBold)
                Text("Montant : ${MicrocreditRepository.formatAmount(application.amountRequested)}")
                Text("Objet : ${application.purpose}")

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Commentaire ou motif de la décision *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(true, notes.ifEmpty { "Dossier conforme et validé." }) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approuver")
                }
                Button(
                    onClick = { onConfirm(false, notes.ifEmpty { "Capacité de remboursement insuffisante." }) },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refuser")
                }
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
fun DisburseLoanDialog(
    application: LoanApplicationEntity,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirmation de Décaissement", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Client : ${application.clientName}", fontWeight = FontWeight.Bold)
                Text("Montant à décaisser : ${MicrocreditRepository.formatAmount(application.amountRequested)}", color = PrimaryNavy, fontWeight = FontWeight.Bold)

                Text(
                    text = "Attention : Cette opération enregistre automatiquement une SORTIE de caisse et génère le cycle de crédit contractuel. Un prêt ne peut pas être décaissé deux fois.",
                    fontSize = 12.sp,
                    color = WarningAmber
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Mode de décaissement :", fontWeight = FontWeight.Medium, fontSize = 13.sp)

                PaymentMethod.values().forEach { method ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text("Valider le Décaissement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
