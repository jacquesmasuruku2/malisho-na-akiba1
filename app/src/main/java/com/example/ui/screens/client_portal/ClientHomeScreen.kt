package com.example.ui.screens.client_portal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.model.UserRole
import com.example.ui.components.BybitAvatarPickerDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CycleStatus
import com.example.data.model.LoanStatus
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.ClientAvatar
import com.example.ui.components.CycleStatusBadge
import com.example.ui.components.LoanStatusBadge
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ClientHomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val applications by viewModel.allLoanApplications.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()

    // Filter strictly for this authenticated client - NEVER fall back to another client's data
    val myClient = clients.find { client ->
        (currentUser != null && currentUser?.email?.isNotBlank() == true && client.email.equals(currentUser?.email, ignoreCase = true)) ||
        (currentUser != null && currentUser?.phone?.isNotBlank() == true && client.phone == currentUser?.phone) ||
        (currentUser != null && (client.id == currentUser?.id || client.id == "CLI-${currentUser?.id?.removePrefix("USR-")}")) ||
        (currentUser != null && currentUser?.fullName?.isNotBlank() == true && client.fullName.equals(currentUser?.fullName, ignoreCase = true))
    }
    val clientTargetId = myClient?.id ?: (if (currentUser?.role == UserRole.CLIENT) "CLI-${currentUser?.id?.removePrefix("USR-")}" else "")
    val clientGender = myClient?.gender ?: "Autre"
    val myCycles = if (clientTargetId.isNotBlank()) cycles.filter { it.clientId == clientTargetId } else emptyList()
    val activeCycle = myCycles.find { it.status == CycleStatus.EN_COURS || it.status == CycleStatus.EN_RETARD }
    val myApplications = if (clientTargetId.isNotBlank()) applications.filter { it.clientId == clientTargetId } else emptyList()
    val myPayments = if (clientTargetId.isNotBlank()) payments.filter { it.clientId == clientTargetId } else emptyList()

    val context = androidx.compose.ui.platform.LocalContext.current
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    var showAvatarPicker by remember { mutableStateOf(false) }

    var showApplyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Card
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            ClientAvatar(
                                gender = clientGender,
                                photoUrl = myClient?.avatarUrl ?: currentUser?.avatarKey?.takeIf { it.startsWith("http") },
                                size = 56.dp,
                                showGenderBadge = false
                            )
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Changer la photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ESPACE CLIENT",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = currentUser?.fullName?.ifBlank { null } ?: "Espace Client",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MALISHO NA AKIBA • ${currentUser?.marketZone ?: "Marché de Kituku"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo("client_profile") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.22f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("client_btn_profile")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Mon Profil & Solde", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.navigateTo("client_loan_apply") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentSkyBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("client_btn_apply")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demander", fontSize = 11.5.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.openChat(clientTargetId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("client_btn_chat")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Conseiller", fontSize = 11.5.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Active Loan Status Card (Requirement 4)
        item {
            if (activeCycle != null) {
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
                            Column {
                                Text("Votre Crédit en Cours", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Cycle ${activeCycle.id} • Décaissé le ${MicrocreditRepository.formatAmount(activeCycle.principalAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            CycleStatusBadge(status = activeCycle.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Paiement / Jour", fontSize = 11.sp, color = TextMuted)
                                Text(MicrocreditRepository.formatAmount(activeCycle.dailyPaymentAmount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentSkyBlue)
                            }
                            Column {
                                Text("Total Déjà Payé", fontSize = 11.sp, color = TextMuted)
                                Text(MicrocreditRepository.formatAmount(activeCycle.totalCollected), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Solde Restant", fontSize = 11.sp, color = TextMuted)
                                Text(MicrocreditRepository.formatAmount(activeCycle.balanceRemaining), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progression du remboursement", fontSize = 11.sp, color = TextSecondary)
                            Text(MicrocreditRepository.formatPercent(activeCycle.recoveryRate), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (activeCycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SuccessGreen
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.navigateTo("client_profile") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_see_balance_and_profile")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consulter le Solde Restant & Profil Complet", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Aucun prêt actif pour le moment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Vous êtes éligible à un crédit de développement commercial.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.navigateTo("client_profile") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_check_account_status")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vérifier le Statut de mon Compte & Éligibilité", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Suivi des Demandes de Prêt (Requirement 4)
        item {
            Text("Suivi de vos Demandes de Prêt (${myApplications.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        if (myApplications.isEmpty()) {
            item {
                Text("Vous n'avez pas encore soumis de demande de prêt.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(myApplications) { app ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Demande ${app.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            LoanStatusBadge(status = app.status)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Montant : ${MicrocreditRepository.formatAmount(app.amountRequested)} • Durée : ${app.durationDays} jours", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Objet : ${app.purpose}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (app.reviewNotes != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Note de l'institution : ${app.reviewNotes}", fontSize = 11.sp, color = AccentSkyBlue)
                        }
                    }
                }
            }
        }

        // Recent Receipts
        item {
            Text("Vos Derniers Reçus de Paiement (${myPayments.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        if (myPayments.isEmpty()) {
            item {
                Text("Aucun reçu disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(myPayments.take(4)) { pay ->
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
                            Text("Reçu ${pay.id} • Jour ${pay.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${pay.dateString} • Validé par l'agent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showApplyDialog) {
        com.example.ui.screens.loans.FullLoanApplicationDialog(
            clientId = clientTargetId,
            initialClientName = currentUser?.fullName ?: "",
            initialClientPhone = currentUser?.phone ?: "",
            initialQuarter = "Birere",
            initialMarketZone = "Marché de Kituku",
            onDismiss = { showApplyDialog = false },
            onSubmit = { amount, duration, purpose, activity, zone, quarter, idCard, photoUri, gName, gPhone, acceptedTerms ->
                viewModel.submitLoanApplication(
                    clientId = clientTargetId,
                    amount = amount,
                    durationDays = duration,
                    purpose = purpose,
                    activityType = activity,
                    marketZone = zone,
                    quarter = quarter,
                    guarantorName = gName,
                    guarantorPhone = gPhone,
                    idCardNumber = idCard,
                    clientPhotoUri = photoUri,
                    acceptedTerms = acceptedTerms,
                    onSuccess = { showApplyDialog = false }
                )
            }
        )
    }

    if (showAvatarPicker) {
        BybitAvatarPickerDialog(
            currentKey = myClient?.avatarUrl ?: currentUser?.avatarKey,
            role = currentUser?.role ?: UserRole.CLIENT,
            gender = clientGender,
            onAvatarSelected = { key ->
                viewModel.updateMyAvatarKey(key)
                showAvatarPicker = false
            },
            onPhotoSelectedFromGallery = { uri ->
                viewModel.uploadAndSetUserAvatarFromUri(uri, context) { success, _ ->
                    if (success) {
                        showAvatarPicker = false
                    }
                }
            },
            isUploadingPhoto = isUploadingAvatar,
            onDismiss = { showAvatarPicker = false }
        )
    }
}
