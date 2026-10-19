package com.example.ui.screens.client_portal

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CycleStatus
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BybitAvatarPickerDialog
import com.example.ui.components.ClientAvatar
import com.example.ui.components.ClientStatusBadge
import com.example.ui.components.CycleStatusBadge
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran officiel de Profil Membre pour MALISHO NA AKIBA (MALAKI).
 * Permet aux membres de consulter :
 * 1. Leur identité officielle, matricule séquentiel auto-incrémenté et quantième (CLI-001 • 1er client...)
 * 2. Le solde actuel de crédit en temps réel (solde restant, montant payé, échéance journalière)
 * 3. Le statut complet du compte (Éligibilité, statut KYC, score de ponctualité)
 * 4. Les coordonnées de leur agent de terrain et les options de sécurité (changement de PIN)
 */
@Composable
fun ClientProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.navigateTo("client_portal")
    }

    // Recherche stricte du profil client rattaché à cet utilisateur connecté
    val myClient = clients.find { client ->
        (currentUser != null && currentUser?.email?.isNotBlank() == true && client.email.equals(currentUser?.email, ignoreCase = true)) ||
        (currentUser != null && currentUser?.phone?.isNotBlank() == true && client.phone == currentUser?.phone) ||
        (currentUser != null && (client.id == currentUser?.id || client.id == "CLI-${currentUser?.id?.removePrefix("USR-")}")) ||
        (currentUser != null && currentUser?.fullName?.isNotBlank() == true && client.fullName.equals(currentUser?.fullName, ignoreCase = true))
    }

    val clientTargetId = myClient?.id ?: if (currentUser?.role == UserRole.CLIENT) "CLI-${currentUser?.id?.removePrefix("USR-")}" else "CLI-001"
    val clientGender = myClient?.gender ?: "Femme"
    val quantiemeLabel = MicrocreditRepository.getClientQuantiemeLabel(clientTargetId, currentLang)

    val myCycles = cycles.filter { it.clientId == clientTargetId }
    val activeCycle = myCycles.find { it.status == CycleStatus.EN_COURS || it.status == CycleStatus.EN_RETARD }
    val completedCycles = myCycles.filter { it.status == CycleStatus.TERMINE }
    val myPayments = payments.filter { it.clientId == clientTargetId }.sortedByDescending { it.paymentDate }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val clientPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAndSetUserAvatarFromUri(uri, context) { success, error ->
                if (success) {
                    Toast.makeText(context, if (currentLang == AppLanguage.SWAHILI) "Picha imesasishwa kikamilifu" else "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, error ?: "Échec du téléversement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barre de navigation supérieure retour
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("client_portal") },
                    modifier = Modifier.testTag("btn_back_from_profile")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Wasifu & Salio Langu" else "Mon Profil & Situation de Crédit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Taarifa kamili za mteja na mkopo" else "Détails complets du compte membre",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. CARTE D'IDENTITÉ OFFICIELLE DU MEMBRE
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().testTag("client_profile_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar avec bouton de modification photo
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            ClientAvatar(
                                gender = clientGender,
                                photoUrl = myClient?.avatarUrl ?: currentUser?.avatarKey?.takeIf { it.startsWith("http") },
                                size = 68.dp,
                                isLate = activeCycle?.status == CycleStatus.EN_RETARD,
                                showGenderBadge = true
                            )
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Changer photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.fullName?.ifBlank { myClient?.fullName } ?: "Membre Malaki",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Badge Matricule auto-incrémenté + Quantième officiel
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = clientTargetId,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (quantiemeLabel.isNotEmpty()) {
                                        Text(
                                            text = " • $quantiemeLabel",
                                            color = Color(0xFFFEF08A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Secteur : ${myClient?.marketZone ?: currentUser?.marketZone ?: "Marché de Kituku"}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Ligne Récapitulative du Statut Général du Compte
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isAccountGood = activeCycle?.status != CycleStatus.EN_RETARD
                            Icon(
                                imageVector = if (isAccountGood) Icons.Default.Verified else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAccountGood) SuccessGreen else Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeCycle?.status == CycleStatus.EN_RETARD)
                                    (if (currentLang == AppLanguage.SWAHILI) "Akaunti : Imechelewa kulipa" else "Statut : Retard de remboursement")
                                else
                                    (if (currentLang == AppLanguage.SWAHILI) "Akaunti : Nzuri & Inastahili" else "Statut : Compte Actif & Éligible"),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val memberSince = myClient?.registeredDate?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Récent"
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Mwanachama tangu $memberSince" else "Membre depuis le $memberSince",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // 2. SITUATION DE CRÉDIT ACTUELLE & SOLDE EN TEMPS RÉEL (CURRENT CREDIT BALANCE)
        item {
            Text(
                text = if (currentLang == AppLanguage.SWAHILI) "Hali ya Mkopo & Salio la Sasa" else "Situation du Crédit & Solde Restant",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            if (activeCycle != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (activeCycle.status == CycleStatus.EN_RETARD) ErrorRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("current_credit_balance_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Cycle Actif : ${activeCycle.id}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Octroyé pour : ${activeCycle.marketZone}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            CycleStatusBadge(status = activeCycle.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ENCADRÉ PRINCIPAL DU SOLDE RESTANT (CURRENT BALANCE)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "SALIO LILILOBAKI KULIPWA" else "SOLDE RESTANT À REMBOURSER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = MicrocreditRepository.formatAmount(activeCycle.balanceRemaining),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeCycle.status == CycleStatus.EN_RETARD) ErrorRed else PrimaryNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI)
                                        "Kiwango cha kila siku (3.5%) : ${MicrocreditRepository.formatAmount(activeCycle.dailyPaymentAmount)} / siku"
                                    else
                                        "Échéance journalière (3,5%) : ${MicrocreditRepository.formatAmount(activeCycle.dailyPaymentAmount)} / jour",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // GRILLE DES MÉTRIQUES DU CYCLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Kiasi Kilichotolewa" else "Prêt Initial Reçu",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = MicrocreditRepository.formatAmount(activeCycle.principalAmount),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Jumla Iliyolipwa" else "Total Déjà Payé",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = MicrocreditRepository.formatAmount(activeCycle.totalCollected),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Jumla ya Mkataba" else "Total Contractuel",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = MicrocreditRepository.formatAmount(activeCycle.totalContractual),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BARRE DE PROGRESSION DU RECOUVREMENT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Maendeleo ya marejesho :" else "Progression du remboursement :",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = MicrocreditRepository.formatPercent(activeCycle.recoveryRate),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCycle.recoveryRate >= 80.0) SuccessGreen else AccentSkyBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (activeCycle.recoveryRate / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (activeCycle.status == CycleStatus.EN_RETARD) ErrorRed else SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bouton d'action pour voir l'échéancier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.navigateTo("client_schedule") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_view_full_schedule")
                            ) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Ratiba Kamili" else "Voir Échéancier",
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.openChat(clientTargetId) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_contact_agent_from_balance")
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.SWAHILI) "Mshauri Wangu" else "Mon Agent",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // AUCUN PRÊT ACTIF (ÉTAT FINANCIER SOLDE NUL & ÉLIGIBILITÉ MAXIMALE)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Salio Lako la Mkopo : FC 0 (Hakuna Deni)" else "Solde Actuel : 0 FC (Aucune Dette Active)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI)
                                "Akaunti yako iko safi. Unastahili kupokea mkopo mpya wa kukuza biashara yako hadi 400 000 FC."
                            else
                                "Votre compte est entièrement en règle. Vous êtes pleinement éligible pour souscrire un prêt commercial de développement.",
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.navigateTo("client_loan_apply") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_apply_loan_from_profile")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Omba Mkopo Mpya Sasa" else "Demander un Nouveau Crédit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. INFORMATIONS DU COMPTE & STATUT DE CONFORMITÉ (ACCOUNT STATUS & KYC)
        item {
            Text(
                text = if (currentLang == AppLanguage.SWAHILI) "Maelezo ya Akaunti & Uthibitisho" else "Informations du Compte & Conformité",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ligne 1 : Téléphone & Identifiant
                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        label = if (currentLang == AppLanguage.SWAHILI) "Nambari ya Simu" else "Téléphone principal",
                        value = myClient?.phone ?: currentUser?.phone ?: "Non renseigné"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Ligne 2 : Email
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "Adresse E-mail",
                        value = (myClient?.email?.ifBlank { null } ?: currentUser?.email?.ifBlank { null }) ?: "Non renseignée"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Ligne 3 : Marché & Zone d'activité
                    ProfileDetailRow(
                        icon = Icons.Default.Storefront,
                        label = if (currentLang == AppLanguage.SWAHILI) "Soko la Biashara" else "Marché & Zone d'activité",
                        value = "${myClient?.marketZone ?: currentUser?.marketZone ?: "Marché de Kituku"} (${myClient?.activityType ?: "Commerce"})"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Ligne 4 : Quartier de résidence
                    ProfileDetailRow(
                        icon = Icons.Default.Place,
                        label = if (currentLang == AppLanguage.SWAHILI) "Mtaa wa Makazi" else "Quartier de résidence",
                        value = myClient?.quarter?.ifBlank { "Goma Centre" } ?: "Goma Centre"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Ligne 5 : Pièce d'identité KYC
                    val hasIdCard = myClient?.idCardNumber?.isNotBlank() == true
                    ProfileDetailRow(
                        icon = Icons.Default.CreditCard,
                        label = if (currentLang == AppLanguage.SWAHILI) "Kitambulisho (KYC)" else "Pièce d'Identité (KYC)",
                        value = if (hasIdCard) "${myClient?.idCardNumber} (Vérifié ✓)" else "À compléter auprès de l'agent",
                        valueColor = if (hasIdCard) SuccessGreen else WarningAmber
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Ligne 6 : Garant enregistré
                    val hasGuarantor = myClient?.guarantorName?.isNotBlank() == true
                    ProfileDetailRow(
                        icon = Icons.Default.Shield,
                        label = if (currentLang == AppLanguage.SWAHILI) "Mtu wa Dhamana" else "Garant enregistré",
                        value = if (hasGuarantor) "${myClient?.guarantorName} (${myClient?.guarantorPhone})" else "Non spécifié"
                    )
                }
            }
        }

        // 4. HISTORIQUE DES PAIEMENTS & REÇUS DU MEMBRE
        if (myPayments.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Malipo ya Hivi Karibuni (${myPayments.size})" else "Derniers Remboursements (${myPayments.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { viewModel.navigateTo("client_schedule") }) {
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Tazama yote" else "Voir tout",
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            items(myPayments.take(3)) { payment ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reçu N° ${payment.receiptNumber}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val payDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(payment.paymentDate))
                            Text(
                                text = "$payDate • ${payment.paymentMethod.label}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "+ ${MicrocreditRepository.formatAmount(payment.amountPaid)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        // 5. GESTION DU COMPTE & SÉCURITÉ
        item {
            Text(
                text = if (currentLang == AppLanguage.SWAHILI) "Usalama na Mipangilio" else "Sécurité & Paramètres du Compte",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 1 : Modifier le code PIN
                    ProfileOptionItem(
                        icon = Icons.Default.Key,
                        title = if (currentLang == AppLanguage.SWAHILI) "Badilisha Nenosiri (PIN)" else "Changer mon code PIN secret",
                        subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka PIN mpya ya tarakimu 4" else "Protéger l'accès à mon espace client",
                        onClick = { showChangePinDialog = true },
                        testTag = "btn_change_pin"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Option 2 : Mon conseiller / Chat d'assistance
                    ProfileOptionItem(
                        icon = Icons.Default.Chat,
                        title = if (currentLang == AppLanguage.SWAHILI) "Zungumza na Mshauri Wangu" else "Contacter mon agent collecteur",
                        subtitle = "Assistance directe pour mes paiements au marché",
                        onClick = { viewModel.openChat(clientTargetId) },
                        testTag = "btn_profile_chat"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Option 3 : Déconnexion sécurisée
                    ProfileOptionItem(
                        icon = Icons.Default.Logout,
                        title = if (currentLang == AppLanguage.SWAHILI) "Ondoka (Logout)" else "Déconnexion de mon compte",
                        subtitle = "Quitter la session sur cet appareil",
                        iconTint = ErrorRed,
                        titleColor = ErrorRed,
                        onClick = { viewModel.logout() },
                        testTag = "btn_profile_logout"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Modal de sélection d'avatar Bybit ou photo de galerie
    if (showAvatarPicker) {
        BybitAvatarPickerDialog(
            currentKey = currentUser?.avatarKey ?: "bybit_female",
            role = UserRole.CLIENT,
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

    // Dialogue de modification du code PIN
    if (showChangePinDialog) {
        ChangePinDialog(
            currentLanguage = currentLang,
            onDismiss = { showChangePinDialog = false },
            onConfirm = { newPin ->
                viewModel.updateClientPinCode(newPin) { success, error ->
                    if (success) {
                        showChangePinDialog = false
                        Toast.makeText(context, if (currentLang == AppLanguage.SWAHILI) "Nenosiri jipya limewekwa !" else "Code PIN modifié avec succès !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, error ?: "Erreur", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentSkyBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ProfileOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun ChangePinDialog(
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.SWAHILI) "Badilisha PIN ya Akaunti" else "Modifier mon code PIN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (currentLanguage == AppLanguage.SWAHILI)
                        "Weka PIN mpya yenye angalau tarakimu 4 kwa ajili ya kufungua akaunti yako."
                    else
                        "Saisissez un nouveau code PIN à 4 chiffres minimum pour sécuriser votre compte membre.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                            newPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text(if (currentLanguage == AppLanguage.SWAHILI) "PIN Mpya (Tarakimu 4)" else "Nouveau code PIN (4 chiffres)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text(if (currentLanguage == AppLanguage.SWAHILI) "Thibitisha PIN Mpya" else "Confirmer le nouveau code PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length < 4) {
                        errorMessage = if (currentLanguage == AppLanguage.SWAHILI) "PIN lazima iwe na tarakimu 4 au zaidi." else "Le code PIN doit comporter au moins 4 chiffres."
                    } else if (newPin != confirmPin) {
                        errorMessage = if (currentLanguage == AppLanguage.SWAHILI) "PIN hazilingani." else "Les deux codes PIN ne correspondent pas."
                    } else {
                        onConfirm(newPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text(if (currentLanguage == AppLanguage.SWAHILI) "Hifadhi PIN" else "Enregistrer le PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (currentLanguage == AppLanguage.SWAHILI) "Ghairi" else "Annuler")
            }
        }
    )
}
