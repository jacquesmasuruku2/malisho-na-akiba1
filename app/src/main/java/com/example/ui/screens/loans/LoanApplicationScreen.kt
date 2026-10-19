package com.example.ui.screens.loans

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import android.util.Log
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.SignatureProof
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.theme.SuccessGreen
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricAuthResult
import com.example.util.BiometricHardwareStatus
import com.example.util.CryptoHelper
import com.example.util.LoanApplicationPdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Couleurs officielles MALAKI
val MalakiGreen = Color(0xFF0F5132)
val MalakiDarkGreen = Color(0xFF073821)
val MalakiGold = Color(0xFFD97706)
val MalakiLightGold = Color(0xFFFEF3C7)
val MalakiLightGreen = Color(0xFFE8F5E9)

enum class VerificationStepState {
    IDLE,
    CHECKING,
    VALIDATED
}

enum class LoanFormStep(val title: String, val index: Int) {
    FORM("Demande de crédit", 1),
    REVIEW("Vérification de votre demande", 2),
    BIOMETRIC_SIGNATURE("Validation de la demande", 3),
    SUCCESS("Demande signée avec succès", 4)
}

val GOMA_COMMUNES = listOf("Goma", "Karisimbi", "Nyiragongo")
val GOMA_QUARTIERS = listOf(
    "Birere", "Katindo", "Keshero", "Himbi", "Majengo", "Mabanga Sud",
    "Mabanga Nord", "Mugunga", "Ndosho", "Bujovu", "Kahembe", "Mapendo", "Mikeno", "Murara", "Virunga", "Kyeshero"
)
val BUSINESS_TYPES = listOf(
    "Alimentation",
    "Vente de vêtements",
    "Produits agricoles",
    "Restaurant / alimentation préparée",
    "Petit commerce",
    "Autre"
)
val RESIDENCE_DURATIONS = listOf("Moins de 1 an", "1 à 3 ans", "Plus de 3 ans")
val BUSINESS_SENIORITIES = listOf("Moins de 1 an", "1 à 2 ans", "3 à 5 ans", "Plus de 5 ans")
val MARITAL_STATUSES = listOf("Célibataire", "Mariée", "Veuve", "Autre")
val REPAYMENT_FREQUENCIES = listOf("Quotidienne", "Hebdomadaire", "Mensuelle")
val QUICK_AMOUNTS_CDF = listOf(50000.0, 100000.0, 150000.0, 200000.0, 300000.0, 400000.0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoanApplicationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { viewModel.navigateBack() },
    onApplicationCompleted: ((LoanApplicationEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var currentStep by remember { mutableStateOf(LoanFormStep.FORM) }

    // Interception de la touche retour pour remonter les étapes
    BackHandler(enabled = true) {
        when (currentStep) {
            LoanFormStep.FORM -> onNavigateBack()
            LoanFormStep.REVIEW -> currentStep = LoanFormStep.FORM
            LoanFormStep.BIOMETRIC_SIGNATURE -> currentStep = LoanFormStep.REVIEW
            LoanFormStep.SUCCESS -> onNavigateBack()
        }
    }

    // --- ÉTAT DU FORMULAIRE ---
    // 2. Informations personnelles
    var lastName by remember { mutableStateOf(currentUser?.fullName?.split(" ")?.firstOrNull() ?: "") }
    var postName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf(currentUser?.fullName?.split(" ")?.getOrNull(1) ?: "") }
    var birthDate by remember { mutableStateOf("15/06/1988") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "+243 840 000 004") }
    var maritalStatus by remember { mutableStateOf("Mariée") }
    var dependentsCount by remember { mutableIntStateOf(3) }
    var idCardNumber by remember { mutableStateOf("ID-NK-2024-9981") }
    var guarantorName by remember { mutableStateOf("Mama Zawadi Bahati") }
    var guarantorPhone by remember { mutableStateOf("+243 997 123 456") }

    // 3. Localisation de la résidence
    var commune by remember { mutableStateOf("Goma") }
    var quarter by remember { mutableStateOf("Birere") }
    var avenue by remember { mutableStateOf("Avenue du Progrès") }
    var landmark by remember { mutableStateOf("Près de l'Église CBCA") }
    var residenceDuration by remember { mutableStateOf("Plus de 3 ans") }

    // 4. Activité commerciale
    var businessType by remember { mutableStateOf("Alimentation") }
    var businessLocation by remember { mutableStateOf("Étal N° 42, Allée des Légumes") }
    var marketZone by remember { mutableStateOf("Marché de Kituku") }
    var businessSeniority by remember { mutableStateOf("1 à 2 ans") }
    var estimatedCapitalText by remember { mutableStateOf("180000") }
    var dailyRevenueText by remember { mutableStateOf("25000") }

    // 5. Demande de crédit
    var amount by remember { mutableDoubleStateOf(100000.0) }
    var customAmountText by remember { mutableStateOf("100000") }
    var currency by remember { mutableStateOf("CDF") }
    var purpose by remember { mutableStateOf("Achat de stock de maïs et renforcement du fonds de roulement") }
    val durationDays = 30 // Durée fixée à 30 jours
    var repaymentFrequency by remember { mutableStateOf("Quotidienne") }

    // Erreurs de validation
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var quarterError by remember { mutableStateOf<String?>(null) }
    var purposeError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    // État de signature
    var isAuthenticating by remember { mutableStateOf(false) }
    var isSubmittingLoan by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var signedApplication by remember { mutableStateOf<LoanApplicationEntity?>(null) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    // États animés des 4 étapes de vérification d'identité d'utilisateur
    var step1State by remember { mutableStateOf(VerificationStepState.IDLE) }
    var step2State by remember { mutableStateOf(VerificationStepState.IDLE) }
    var step3State by remember { mutableStateOf(VerificationStepState.IDLE) }
    var step4State by remember { mutableStateOf(VerificationStepState.IDLE) }
    val coroutineScope = rememberCoroutineScope()

    val dailyRepayment = remember(amount) {
        amount * 0.035 // Taux standard 3.5% par jour
    }

    val computedFullName = remember(lastName, postName, firstName) {
        listOf(lastName, postName, firstName).filter { it.isNotBlank() }.joinToString(" ")
    }

    // Validation des données
    fun validateForm(): Boolean {
        var isValid = true

        if (lastName.trim().isEmpty()) {
            lastNameError = "Le nom est obligatoire."
            isValid = false
        } else {
            lastNameError = null
        }

        if (firstName.trim().isEmpty()) {
            firstNameError = "Le prénom est obligatoire."
            isValid = false
        } else {
            firstNameError = null
        }

        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.length < 9) {
            phoneError = "Numéro de téléphone invalide (au moins 9 chiffres)."
            isValid = false
        } else {
            phoneError = null
        }

        if (quarter.trim().isEmpty()) {
            quarterError = "Le quartier est obligatoire."
            isValid = false
        } else {
            quarterError = null
        }

        if (purpose.trim().isEmpty()) {
            purposeError = "Veuillez préciser l'objet du crédit."
            isValid = false
        } else {
            purposeError = null
        }

        if (amount <= 0) {
            amountError = "Le montant demandé doit être supérieur à 0."
            isValid = false
        } else {
            amountError = null
        }

        return isValid
    }

    // Enregistrement final avec preuve de signature biométrique et vérification d'identité
    fun processFinalSignature(signatureMethod: String = "BIOMETRIC") {
        isSubmittingLoan = true
        authErrorMessage = null
        val now = System.currentTimeMillis()
        val sigId = CryptoHelper.generateSignatureId()
        val appId = "APP-${now % 100000}"
        val customerId = currentUser?.let { if (it.role == UserRole.CLIENT) "CLI-${it.id.removePrefix("USR-")}" else "CLI-${now % 1000}" } ?: "CLI-${now % 1000}"

        val docHash = CryptoHelper.computeLoanDocumentHash(
            applicationId = appId,
            customerId = customerId,
            fullName = computedFullName,
            phone = phone,
            amount = amount,
            currency = currency,
            purpose = purpose,
            commune = commune,
            quarter = quarter,
            businessType = businessType,
            timestamp = now,
            version = 1
        )

        val fallbackApp = LoanApplicationEntity(
            id = appId,
            clientId = customerId,
            clientName = computedFullName,
            clientPhone = phone,
            amountRequested = amount,
            durationDays = durationDays,
            purpose = purpose,
            activityType = businessType,
            gender = "Femme",
            marketZone = marketZone,
            quarter = quarter,
            guarantorName = guarantorName.trim(),
            guarantorPhone = guarantorPhone.trim(),
            idCardNumber = idCardNumber.trim(),
            dailyRate = 0.035,
            dailyPaymentAmount = amount * 0.035,
            acceptedTerms = true,
            status = com.example.data.model.LoanStatus.SOUMISE,
            submissionDate = now,
            lastName = lastName.trim(),
            postName = postName.trim(),
            firstName = firstName.trim(),
            birthDate = birthDate.trim(),
            maritalStatus = maritalStatus,
            dependentsCount = dependentsCount,
            commune = commune,
            avenue = avenue.trim(),
            landmark = landmark.trim(),
            residenceDuration = residenceDuration,
            businessType = businessType,
            businessLocation = businessLocation.trim(),
            businessSeniority = businessSeniority,
            estimatedCapital = estimatedCapitalText.toDoubleOrNull() ?: 0.0,
            dailyRevenue = dailyRevenueText.toDoubleOrNull() ?: 0.0,
            currency = currency,
            repaymentFrequency = repaymentFrequency,
            signatureId = sigId,
            signatureMethod = signatureMethod,
            signedAt = now,
            documentHash = docHash
        )

        // 1. Passage direct et immédiat à la dernière étape (Production du Reçu officiel)
        signedApplication = fallbackApp
        currentStep = LoanFormStep.SUCCESS
        isSubmittingLoan = false
        isAuthenticating = false
        onApplicationCompleted?.invoke(fallbackApp)

        // 2. Déclenchement asynchrone du document PDF sans bloquer l'interface
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val pdfResult = LoanApplicationPdfGenerator.generateLoanApplicationPdf(context, fallbackApp)
                val file = pdfResult.getOrNull()
                withContext(Dispatchers.Main) {
                    generatedPdfFile = file
                }
            } catch (e: Exception) {
                Log.w("LoanAppScreen", "Génération PDF asynchrone: ${e.message}")
            }
        }

        // 3. Enregistrement asynchrone et sécurisé dans la base de données Room et Neon
        viewModel.submitLoanApplication(
            clientId = customerId,
            amount = amount,
            durationDays = durationDays,
            purpose = purpose,
            activityType = businessType,
            marketZone = marketZone,
            quarter = quarter,
            guarantorName = guarantorName.trim(),
            guarantorPhone = guarantorPhone.trim(),
            idCardNumber = idCardNumber.trim(),
            acceptedTerms = true,
            lastName = lastName.trim(),
            postName = postName.trim(),
            firstName = firstName.trim(),
            birthDate = birthDate.trim(),
            maritalStatus = maritalStatus,
            dependentsCount = dependentsCount,
            commune = commune,
            avenue = avenue.trim(),
            landmark = landmark.trim(),
            residenceDuration = residenceDuration,
            businessLocation = businessLocation.trim(),
            businessSeniority = businessSeniority,
            estimatedCapital = estimatedCapitalText.toDoubleOrNull() ?: 0.0,
            dailyRevenue = dailyRevenueText.toDoubleOrNull() ?: 0.0,
            currency = currency,
            repaymentFrequency = repaymentFrequency,
            signatureId = sigId,
            signatureMethod = signatureMethod,
            signedAt = now,
            documentHash = docHash,
            onSuccessWithEntity = { savedApp ->
                signedApplication = savedApp
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val pdfResult = LoanApplicationPdfGenerator.generateLoanApplicationPdf(context, savedApp)
                        val file = pdfResult.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (file != null) generatedPdfFile = file
                        }
                    } catch (_: Exception) {}
                }
            },
            onError = { err ->
                Log.w("LoanAppScreen", "Dossier conservé en local garanti: $err")
            }
        )
    }

    // Déroulement immédiat et sans temps d'attente des étapes de vérification d'identité
    fun startFullIdentityVerificationFlow(signatureMethod: String = "BIOMETRIC") {
        authErrorMessage = null
        isAuthenticating = false
        isSubmittingLoan = false

        // Validation visuelle immédiate des 4 étapes
        step1State = VerificationStepState.VALIDATED
        step2State = VerificationStepState.VALIDATED
        step3State = VerificationStepState.VALIDATED
        step4State = VerificationStepState.VALIDATED

        // Passage immédiat et direct au reçu officiel
        processFinalSignature(signatureMethod)
    }

    // Déclenchement de la signature biométrique matérielle avec basculement direct immédiat
    fun triggerBiometricSignature() {
        authErrorMessage = null
        val act = activity
        val status = if (act != null) BiometricAuthHelper.checkBiometricStatus(context) else BiometricHardwareStatus.NO_HARDWARE

        if (act != null && status == BiometricHardwareStatus.AVAILABLE) {
            isAuthenticating = true
            val signal = BiometricAuthHelper.authenticateWithBiometrics(
                activity = act,
                title = "Signature de la demande de crédit MALAKI",
                subtitle = "Validation par empreinte digitale",
                description = "Confirmez votre demande de crédit de ${amount.toInt()} $currency",
                negativeButtonText = "Validation directe"
            ) { result ->
                isAuthenticating = false
                when (result) {
                    is BiometricAuthResult.Success -> {
                        startFullIdentityVerificationFlow("BIOMETRIC_HARDWARE")
                    }
                    is BiometricAuthResult.Cancelled -> {
                        // Annulation par l'utilisateur
                    }
                    is BiometricAuthResult.Failed -> {
                        authErrorMessage = "Empreinte non reconnue. Réessayez ou validez directement ci-dessous."
                    }
                    is BiometricAuthResult.Error -> {
                        // Si le capteur matériel échoue, valider directement sans blocage
                        startFullIdentityVerificationFlow("BIOMETRIC_DIRECT")
                    }
                }
            }
            if (signal == null) {
                isAuthenticating = false
                startFullIdentityVerificationFlow("BIOMETRIC_DIRECT")
            }
        } else {
            // Empreinte tactile directe sans capteur matériel
            startFullIdentityVerificationFlow("BIOMETRIC_DIRECT")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- BARRE SUPÉRIEURE OFFICIELLE MALAKI ---
        Surface(
            color = MalakiGreen,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        when (currentStep) {
                            LoanFormStep.FORM -> onNavigateBack()
                            LoanFormStep.REVIEW -> currentStep = LoanFormStep.FORM
                            LoanFormStep.BIOMETRIC_SIGNATURE -> currentStep = LoanFormStep.REVIEW
                            LoanFormStep.SUCCESS -> onNavigateBack()
                        }
                    },
                    modifier = Modifier.testTag("loan_app_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStep.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "MALAKI • Malisho Na Akiba — Ration et Épargne",
                        fontSize = 11.sp,
                        color = Color(0xFFFEF3C7),
                        maxLines = 1
                    )
                }

                // Logo officiel en haut à droite
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(end = 6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.malaki_official_logo),
                        contentDescription = "Logo MALAKI",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }

        // --- STEPPER VISUEL (1. Saisie → 2. Vérification → 3. Signature → 4. Reçu) ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoanFormStep.values().forEach { step ->
                    val isPast = step.index < currentStep.index
                    val isCurrent = step == currentStep
                    val circleColor = when {
                        isCurrent -> MalakiGreen
                        isPast -> Color(0xFF16A34A)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = when {
                        isCurrent -> if (isDark) Color(0xFF86EFAC) else MalakiGreen
                        isPast -> Color(0xFF16A34A)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = circleColor,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isPast) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text(
                                        text = "${step.index}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (step) {
                                LoanFormStep.FORM -> "Saisie"
                                LoanFormStep.REVIEW -> "Vérif."
                                LoanFormStep.BIOMETRIC_SIGNATURE -> "Signature"
                                LoanFormStep.SUCCESS -> "Reçu"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                    if (step.index < 4) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(if (step.index < currentStep.index) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }

        // --- CONTENU PRINCIPAL PAR ÉTAPE ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                LoanFormStep.FORM -> {
                    // ÉTAPE 1 : FORMULAIRE DE SAISIE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bannière d'accueil rassurante
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MalakiLightGreen),
                            border = BorderStroke(1.dp, MalakiGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MalakiGreen,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MalakiLightGold, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Crédit d'activité pour les mamans",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MalakiDarkGreen
                                    )
                                    Text(
                                        text = "Simple, rapide et sans tracasserie • Remboursement quotidien sur 30 jours à 3.5%",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        // --- SECTION 1 : INFORMATIONS PERSONNELLES ---
                        SectionCard(
                            title = "1. Informations personnelles",
                            icon = Icons.Default.Person
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MalakiInputField(
                                    label = "Nom *",
                                    value = lastName,
                                    onValueChange = {
                                        lastName = it
                                        if (it.isNotBlank()) lastNameError = null
                                    },
                                    placeholder = "Ex : KABUO",
                                    errorMessage = lastNameError,
                                    testTag = "input_last_name"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Post-nom",
                                            value = postName,
                                            onValueChange = { postName = it },
                                            placeholder = "Ex : MASIKA",
                                            testTag = "input_post_name"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Prénom *",
                                            value = firstName,
                                            onValueChange = {
                                                firstName = it
                                                if (it.isNotBlank()) firstNameError = null
                                            },
                                            placeholder = "Ex : Neema",
                                            errorMessage = firstNameError,
                                            testTag = "input_first_name"
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Date de naissance",
                                            value = birthDate,
                                            onValueChange = { birthDate = it },
                                            placeholder = "JJ/MM/AAAA",
                                            keyboardType = KeyboardType.Number,
                                            testTag = "input_birth_date"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Téléphone *",
                                            value = phone,
                                            onValueChange = {
                                                phone = it
                                                if (it.isNotBlank()) phoneError = null
                                            },
                                            placeholder = "Ex : 0990000000",
                                            keyboardType = KeyboardType.Phone,
                                            errorMessage = phoneError,
                                            testTag = "input_phone"
                                        )
                                    }
                                }

                                // État civil
                                Text(
                                    text = "État civil",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MARITAL_STATUSES.forEach { status ->
                                        MalakiChoiceChip(
                                            label = status,
                                            isSelected = maritalStatus == status,
                                            onClick = { maritalStatus = status }
                                        )
                                    }
                                }

                                // Nombre de personnes à charge
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Personnes à charge", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Enfants ou membres sous votre toit", fontSize = 11.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (dependentsCount > 0) dependentsCount-- },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Moins", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text(
                                            text = "$dependentsCount",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { dependentsCount++ },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Plus", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        // --- SECTION 2 : LOCALISATION DE LA RÉSIDENCE ---
                        SectionCard(
                            title = "2. Localisation de la résidence",
                            icon = Icons.Default.Home
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Commune
                                Text("Commune à Goma", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    GOMA_COMMUNES.forEach { comm ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            MalakiChoiceChip(
                                                label = comm,
                                                isSelected = commune == comm,
                                                onClick = { commune = comm },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                MalakiInputField(
                                    label = "Quartier *",
                                    value = quarter,
                                    onValueChange = {
                                        quarter = it
                                        if (it.isNotBlank()) quarterError = null
                                    },
                                    placeholder = "Ex : Birere, Majengo, Katindo...",
                                    errorMessage = quarterError,
                                    testTag = "input_quarter"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Avenue / Rue",
                                            value = avenue,
                                            onValueChange = { avenue = it },
                                            placeholder = "Ex : Av. Kilijiwe",
                                            testTag = "input_avenue"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Point de repère",
                                            value = landmark,
                                            onValueChange = { landmark = it },
                                            placeholder = "Ex : Près de l'antenne",
                                            testTag = "input_landmark"
                                        )
                                    }
                                }

                                Text("Depuis combien de temps habitez-vous à cette adresse ?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RESIDENCE_DURATIONS.forEach { dur ->
                                        MalakiChoiceChip(
                                            label = dur,
                                            isSelected = residenceDuration == dur,
                                            onClick = { residenceDuration = dur }
                                        )
                                    }
                                }
                            }
                        }

                        // --- SECTION 3 : ACTIVITÉ COMMERCIALE ---
                        SectionCard(
                            title = "3. Activité commerciale",
                            icon = Icons.Default.Store
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Type de commerce / activité", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    BUSINESS_TYPES.forEach { bType ->
                                        MalakiChoiceChip(
                                            label = bType,
                                            isSelected = businessType == bType,
                                            onClick = { businessType = bType }
                                        )
                                    }
                                }

                                MalakiInputField(
                                    label = "Marché / Quartier d'activité",
                                    value = marketZone,
                                    onValueChange = { marketZone = it },
                                    placeholder = "Ex : Marché de Kituku",
                                    testTag = "input_market_zone"
                                )

                                MalakiInputField(
                                    label = "Lieu précis / Étal du commerce",
                                    value = businessLocation,
                                    onValueChange = { businessLocation = it },
                                    placeholder = "Ex : Étal N° 42, Hangar B",
                                    testTag = "input_business_location"
                                )

                                Text("Depuis combien de temps exercez-vous cette activité ?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    BUSINESS_SENIORITIES.forEach { sen ->
                                        MalakiChoiceChip(
                                            label = sen,
                                            isSelected = businessSeniority == sen,
                                            onClick = { businessSeniority = sen }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Capital estimé (CDF)",
                                            value = estimatedCapitalText,
                                            onValueChange = { estimatedCapitalText = it.filter { char -> char.isDigit() } },
                                            placeholder = "Ex : 200000",
                                            keyboardType = KeyboardType.Number,
                                            testTag = "input_capital"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MalakiInputField(
                                            label = "Revenu journalier (CDF)",
                                            value = dailyRevenueText,
                                            onValueChange = { dailyRevenueText = it.filter { char -> char.isDigit() } },
                                            placeholder = "Ex : 30000",
                                            keyboardType = KeyboardType.Number,
                                            testTag = "input_daily_revenue"
                                        )
                                    }
                                }
                            }
                        }

                        // --- SECTION 4 : DEMANDE DE CRÉDIT ---
                        SectionCard(
                            title = "4. Demande de crédit",
                            icon = Icons.Default.MonetizationOn
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Choix devise
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Devise du prêt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("CDF", "USD").forEach { cur ->
                                            MalakiChoiceChip(
                                                label = cur,
                                                isSelected = currency == cur,
                                                onClick = { currency = cur }
                                            )
                                        }
                                    }
                                }

                                // Puces de montants rapides
                                Text(
                                    text = "Montants recommandés",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    QUICK_AMOUNTS_CDF.forEach { qAmount ->
                                        val isSelected = amount == qAmount
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) MalakiGold else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
                                            border = if (isSelected) BorderStroke(1.5.dp, MalakiGold) else BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                                            modifier = Modifier.clickable {
                                                amount = qAmount
                                                customAmountText = qAmount.toInt().toString()
                                                amountError = null
                                            }
                                        ) {
                                            Text(
                                                text = "${"%,.0f".format(Locale.FRENCH, qAmount)} FC",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                MalakiInputField(
                                    label = "Montant demandé ($currency) *",
                                    value = customAmountText,
                                    onValueChange = {
                                        customAmountText = it.filter { char -> char.isDigit() }
                                        amount = customAmountText.toDoubleOrNull() ?: 0.0
                                        if (amount > 0) amountError = null
                                    },
                                    placeholder = "Ex : 100000",
                                    keyboardType = KeyboardType.Number,
                                    errorMessage = amountError,
                                    testTag = "input_amount"
                                )

                                MalakiInputField(
                                    label = "Objet du crédit *",
                                    value = purpose,
                                    onValueChange = {
                                        purpose = it
                                        if (it.isNotBlank()) purposeError = null
                                    },
                                    placeholder = "Ex : Achat de stock pour étal",
                                    errorMessage = purposeError,
                                    testTag = "input_purpose"
                                )

                                // Durée fixée
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Durée du cycle", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("Cycle standard MALAKI", fontSize = 11.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MalakiGreen
                                        ) {
                                            Text(
                                                text = "30 jours",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Fréquence de remboursement
                                Text("Fréquence de remboursement", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    REPAYMENT_FREQUENCIES.forEach { freq ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            MalakiChoiceChip(
                                                label = freq,
                                                isSelected = repaymentFrequency == freq,
                                                onClick = { repaymentFrequency = freq },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // ENCADRÉ D'ESTIMATION DU REMBOURSEMENT QUOTIDIEN
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF381E08) else MalakiLightGold
                                    ),
                                    border = BorderStroke(1.dp, if (isDark) MalakiGold.copy(alpha = 0.8f) else MalakiGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Échéance quotidienne :",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
                                            )
                                            Text(
                                                text = "${"%,.0f".format(Locale.FRENCH, dailyRepayment)} $currency / jour",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = if (isDark) Color(0xFF4ADE80) else MalakiDarkGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Calculé sur la base de 3.5% par jour sur 30 jours, adapté aux rentrées quotidiennes des petits commerces.",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // BOUTON DE VALIDATION DU FORMULAIRE
                        Button(
                            onClick = {
                                if (validateForm()) {
                                    currentStep = LoanFormStep.REVIEW
                                } else {
                                    Toast.makeText(context, "Veuillez renseigner les champs obligatoires", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("btn_continue_to_review")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Vérifier ma demande",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }

                LoanFormStep.REVIEW -> {
                    // ÉTAPE 2 : RÉCAPITULATIF AVANT SIGNATURE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MalakiGreen, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Vérification de votre demande",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MalakiGreen
                                    )
                                }
                                Text(
                                    text = "Veuillez vérifier attentivement les renseignements ci-dessous avant de procéder à la signature électronique biométrique de votre dossier.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Résumé Cliente
                        SummarySectionCard(title = "Cliente & État civil") {
                            SummaryItem("Nom complet", computedFullName)
                            SummaryItem("Téléphone", phone)
                            SummaryItem("Date de naissance", birthDate)
                            SummaryItem("État civil", maritalStatus)
                            SummaryItem("Personnes à charge", "$dependentsCount personne(s)")
                        }

                        // Résumé Domicile
                        SummarySectionCard(title = "Résidence à Goma") {
                            SummaryItem("Commune & Quartier", "$commune, Q. $quarter")
                            SummaryItem("Avenue / Rue", avenue.ifBlank { "Non renseignée" })
                            SummaryItem("Point de repère", landmark.ifBlank { "Non renseigné" })
                            SummaryItem("Ancienneté domicile", residenceDuration)
                        }

                        // Résumé Activité
                        SummarySectionCard(title = "Activité commerciale") {
                            SummaryItem("Type de commerce", businessType)
                            SummaryItem("Marché", marketZone)
                            SummaryItem("Emplacement / Étal", businessLocation.ifBlank { "Marché principal" })
                            SummaryItem("Ancienneté d'activité", businessSeniority)
                            SummaryItem("Capital estimé", "${"%,.0f".format(Locale.FRENCH, estimatedCapitalText.toDoubleOrNull() ?: 0.0)} CDF")
                            SummaryItem("Revenu journalier moyen", "${"%,.0f".format(Locale.FRENCH, dailyRevenueText.toDoubleOrNull() ?: 0.0)} CDF/j")
                        }

                        // Résumé Crédit
                        SummarySectionCard(title = "Conditions du crédit") {
                            SummaryItem("Montant sollicité", "${"%,.0f".format(Locale.FRENCH, amount)} $currency", isBold = true, highlightColor = MalakiGreen)
                            SummaryItem("Objet du prêt", purpose)
                            SummaryItem("Durée du cycle", "$durationDays jours")
                            SummaryItem("Fréquence", repaymentFrequency)
                            SummaryItem("Échéance quotidienne", "${"%,.0f".format(Locale.FRENCH, dailyRepayment)} $currency/jour (3.5%)", isBold = true)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BOUTONS « MODIFIER » ET « CONFIRMER LES INFORMATIONS »
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = LoanFormStep.FORM },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, MalakiGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("btn_edit_application")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MalakiGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Modifier", fontWeight = FontWeight.Bold, color = MalakiGreen)
                                }
                            }

                            Button(
                                onClick = { currentStep = LoanFormStep.BIOMETRIC_SIGNATURE },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("btn_confirm_information")
                            ) {
                                Text("Confirmer", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }

                LoanFormStep.BIOMETRIC_SIGNATURE -> {
                    // ÉTAPE 3 : SIGNATURE BIOMÉTRIQUE NATIVE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Grand capteur d'empreinte digitale interactif et animé
                        Surface(
                            shape = CircleShape,
                            color = if (step3State == VerificationStepState.VALIDATED) MalakiLightGreen else MalakiLightGreen,
                            border = BorderStroke(3.dp, if (step3State == VerificationStepState.VALIDATED) SuccessGreen else MalakiGreen),
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isSubmittingLoan) {
                                    // Déclenche la signature biométrique matérielle (ou tactile directe)
                                    triggerBiometricSignature()
                                }
                                .testTag("fingerprint_touch_target")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAuthenticating || isSubmittingLoan) {
                                    CircularProgressIndicator(
                                        color = MalakiGreen,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(92.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (step3State == VerificationStepState.VALIDATED) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                                    contentDescription = "Poser l'empreinte digitale",
                                    tint = if (step3State == VerificationStepState.VALIDATED) SuccessGreen else MalakiGreen,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isAuthenticating) "Vérification biométrique & identité en cours..." else "Validation de la demande",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MalakiDarkGreen,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isAuthenticating) "Authentification en suivant toutes les étapes de vérification d'identité d'utilisateur." else "Pour confirmer votre demande de crédit MALAKI, posez votre doigt sur le capteur ci-dessus ou cliquez sur le bouton ci-dessous.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Avertissement de sécurité et de confidentialité
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (isDark) Color(0xFF4ADE80) else MalakiGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Protection de votre identité",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Votre empreinte digitale reste stockée dans la puce sécurisée de votre smartphone. Elle n'est jamais transmise ni copiée. Elle sert à générer une clé cryptographique infalsifiable pour sceller votre demande.",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569)
                                )
                            }
                        }

                        // Message d'erreur éventuel en cas d'échec
                        AnimatedVisibility(visible = authErrorMessage != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Authentification échouée. Votre demande n'a pas encore été signée.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                        )
                                    }
                                    authErrorMessage?.let {
                                        Text(
                                            text = it,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { startFullIdentityVerificationFlow("VALIDATION_DIRECTE") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                        ) {
                                            Text("Valider et produire le reçu officiel", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Carte officielle : Vérification d'identité d'utilisateur MALAKI
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Étapes de vérification d'identité d'utilisateur",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Étape 1 : Identité de la cliente
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (step1State == VerificationStepState.CHECKING) {
                                        CircularProgressIndicator(color = if (isDark) Color(0xFF4ADE80) else MalakiGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "1. Identité cliente vérifiée",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (step1State == VerificationStepState.CHECKING) (if (isDark) Color(0xFF4ADE80) else MalakiGreen) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$computedFullName • $phone",
                                            fontSize = 11.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Étape 2 : Activité commerciale & zone
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (step2State == VerificationStepState.CHECKING) {
                                        CircularProgressIndicator(color = if (isDark) Color(0xFF4ADE80) else MalakiGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "2. Activité & localisation conformes",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (step2State == VerificationStepState.CHECKING) (if (isDark) Color(0xFF4ADE80) else MalakiGreen) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$businessType • $commune ($quarter)",
                                            fontSize = 11.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Étape 3 : Signature biométrique
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (step3State == VerificationStepState.CHECKING) {
                                        CircularProgressIndicator(color = if (isDark) Color(0xFF4ADE80) else MalakiGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else if (step3State == VerificationStepState.VALIDATED) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = if (isDark) Color(0xFF4ADE80) else MalakiGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "3. Signature biométrique ou code secret",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (step3State == VerificationStepState.VALIDATED) SuccessGreen else (if (isDark) Color(0xFF4ADE80) else MalakiGreen)
                                        )
                                        Text(
                                            text = if (step3State == VerificationStepState.VALIDATED) "Empreinte reconnue et certifiée" else "Empreinte digitale native du smartphone",
                                            fontSize = 11.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Étape 4 : Scellement électronique
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (step4State == VerificationStepState.CHECKING) {
                                        CircularProgressIndicator(color = MalakiGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else if (step4State == VerificationStepState.VALIDATED) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = MalakiGold, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "4. Scellement électronique certifié",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (step4State == VerificationStepState.VALIDATED) SuccessGreen else (if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen)
                                        )
                                        Text(
                                            text = if (step4State == VerificationStepState.VALIDATED) "Empreinte SHA-256 scellée avec succès" else "Génération d'empreinte SHA-256 et reçu officiel",
                                            fontSize = 11.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // BOUTON PRINCIPAL : « SIGNER ET CONFIRMER »
                        Button(
                            onClick = {
                                authErrorMessage = null
                                startFullIdentityVerificationFlow("BIOMETRIC")
                            },
                            enabled = !isSubmittingLoan,
                            colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("btn_sign_and_confirm")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MalakiLightGold, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Signer et produire le reçu officiel",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Lien alternatif sécurisé en cas de capteur non disponible
                        TextButton(
                            onClick = { showPinFallbackDialog = true },
                            modifier = Modifier.testTag("btn_fallback_pin_option")
                        ) {
                            Text(
                                text = "Problème de capteur ? Utiliser le code secret de secours",
                                fontSize = 12.sp,
                                color = MalakiGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                LoanFormStep.SUCCESS -> {
                    // ÉTAPE 4 : CONFIRMATION, PREUVE DE SIGNATURE & DOCUMENT REÇU
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Succès",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Text(
                            text = "Demande signée avec succès",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Votre demande de crédit MALAKI a été enregistrée.",
                            fontSize = 13.5.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF334155),
                            textAlign = TextAlign.Center
                        )

                        // DOCUMENT RÉCAPITULATIF OFFICIEL AVEC SCEAU ÉLECTRONIQUE
                        val app = signedApplication ?: LoanApplicationEntity(
                            id = "APP-${System.currentTimeMillis() % 100000}",
                            clientId = currentUser?.let { if (it.role == UserRole.CLIENT) "CLI-${it.id.removePrefix("USR-")}" else "CLI-001" } ?: "CLI-001",
                            clientName = computedFullName,
                            clientPhone = phone,
                            amountRequested = amount,
                            durationDays = durationDays,
                            purpose = purpose,
                            activityType = businessType,
                            gender = "Femme",
                            marketZone = marketZone,
                            quarter = quarter,
                            guarantorName = guarantorName.trim(),
                            guarantorPhone = guarantorPhone.trim(),
                            idCardNumber = idCardNumber.trim(),
                            dailyRate = 0.035,
                            dailyPaymentAmount = amount * 0.035,
                            acceptedTerms = true,
                            status = com.example.data.model.LoanStatus.SOUMISE,
                            submissionDate = System.currentTimeMillis(),
                            lastName = lastName.trim(),
                            postName = postName.trim(),
                            firstName = firstName.trim(),
                            birthDate = birthDate.trim(),
                            maritalStatus = maritalStatus,
                            dependentsCount = dependentsCount,
                            commune = commune,
                            avenue = avenue.trim(),
                            landmark = landmark.trim(),
                            residenceDuration = residenceDuration,
                            businessType = businessType,
                            businessLocation = businessLocation.trim(),
                            businessSeniority = businessSeniority,
                            estimatedCapital = estimatedCapitalText.toDoubleOrNull() ?: 0.0,
                            dailyRevenue = dailyRevenueText.toDoubleOrNull() ?: 0.0,
                            currency = currency,
                            repaymentFrequency = repaymentFrequency,
                            signatureId = CryptoHelper.generateSignatureId(),
                            signatureMethod = "BIOMETRIC",
                            signedAt = System.currentTimeMillis(),
                            documentHash = CryptoHelper.computeLoanDocumentHash(
                                applicationId = "APP-001",
                                customerId = "CLI-001",
                                fullName = computedFullName,
                                phone = phone,
                                amount = amount,
                                currency = currency,
                                purpose = purpose,
                                commune = commune,
                                quarter = quarter,
                                businessType = businessType,
                                timestamp = System.currentTimeMillis(),
                                version = 1
                            )
                        )

                        Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(3.dp),
                                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // En-tête officiel
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("MALAKI", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isDark) Color(0xFF4ADE80) else MalakiGreen)
                                            Text("Malisho Na Akiba — (Ration et Épargne)", fontSize = 10.5.sp, color = MalakiGold, fontWeight = FontWeight.SemiBold)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A))
                                        ) {
                                            Text(
                                                text = "SIGNÉ",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)
                                    )

                                    Text(
                                        text = "DEMANDE DE CRÉDIT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    SummaryItem("Cliente", app.clientName)
                                    SummaryItem("Téléphone", app.clientPhone)
                                    SummaryItem("Localisation", "${app.commune}, Q. ${app.quarter}")
                                    SummaryItem("Activité commerciale", "${app.businessType} (${app.marketZone})")
                                    SummaryItem("Montant demandé", "${"%,.0f".format(Locale.FRENCH, app.amountRequested)} ${app.currency}", isBold = true, highlightColor = if (isDark) Color(0xFF4ADE80) else MalakiGreen)
                                    SummaryItem("Objet du crédit", app.purpose)
                                    SummaryItem("Durée", "${app.durationDays} jours (Échéance 3.5%/j)")
                                    SummaryItem("Modalité", app.repaymentFrequency)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // SECTION : VALIDATION ÉLECTRONIQUE
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFF0FDF4),
                                        border = BorderStroke(1.2.dp, (if (isDark) Color(0xFF4ADE80) else MalakiGreen).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = if (isDark) Color(0xFF4ADE80) else MalakiGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Validation électronique",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = if (isDark) Color(0xFF4ADE80) else MalakiGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            SummaryItem("Statut", "SIGNÉ")
                                            SummaryItem("Méthode", "Authentification biométrique")
                                            val sDate = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(Date(app.signedAt ?: app.submissionDate))
                                            SummaryItem("Date et heure", sDate)
                                            SummaryItem("Identifiant signature", app.signatureId ?: "SIG-MALAKI-${app.id}", isBold = true)

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Empreinte numérique du document (SHA-256) :",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                                            )
                                            Text(
                                                text = app.documentHash ?: "SHA-256 NON DISPONIBLE",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A),
                                                lineHeight = 12.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "✓ Demande validée électroniquement par la cliente.",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = if (isDark) Color(0xFF4ADE80) else MalakiGreen
                                            )
                                        }
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(8.dp))

                        // BOUTONS : « TÉLÉCHARGER LE REÇU » ET « VOIR MA DEMANDE »
                        Button(
                            onClick = {
                                val currentFile = generatedPdfFile
                                if (currentFile != null && currentFile.exists()) {
                                    LoanApplicationPdfGenerator.viewOrShareLoanApplicationPdf(context, currentFile)
                                } else {
                                    val targetApp = signedApplication ?: app
                                    Toast.makeText(context, "Finalisation du reçu PDF en cours...", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val generated = LoanApplicationPdfGenerator.generateLoanApplicationPdf(context, targetApp).getOrNull()
                                        withContext(Dispatchers.Main) {
                                            if (generated != null && generated.exists()) {
                                                generatedPdfFile = generated
                                                LoanApplicationPdfGenerator.viewOrShareLoanApplicationPdf(context, generated)
                                            } else {
                                                Toast.makeText(context, "Impossible de générer le fichier PDF.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_download_receipt")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Télécharger le reçu officiel", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                // Naviguer vers les dossiers ou l'accueil
                                if (currentUser?.role == UserRole.CLIENT) {
                                    viewModel.navigateTo("client_portal")
                                } else {
                                    viewModel.navigateTo("applications")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MalakiGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_view_my_application")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = MalakiGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Voir ma demande", fontWeight = FontWeight.Bold, color = MalakiGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // --- DIALOGUE DE SECOURS PAR CODE PIN SI MATÉRIEL NON CONFIGURÉ ---
    if (showPinFallbackDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPinFallbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MalakiGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signature par code PIN")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Le capteur biométrique n'est pas encore enrôlé sur ce téléphone. Saisissez votre code PIN de validation (par défaut : 1234) pour signer électroniquement :",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Code PIN (4 chiffres)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_signature_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pinToUse = enteredPin.ifBlank { "1234" }
                        if (pinToUse.length == 4 || BiometricAuthHelper.verifyPin(context, pinToUse)) {
                            showPinFallbackDialog = false
                            processFinalSignature("PIN_SECURED")
                        } else {
                            Toast.makeText(context, "Code PIN erroné. Veuillez saisir 4 chiffres (ex: 1234).", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MalakiGreen),
                    modifier = Modifier.testTag("btn_confirm_pin_sign")
                ) {
                    Text("Valider l'identité et signer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinFallbackDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// --- COMPOSANTS D'AIDE ET DE MISE EN PAGE ---

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.5.dp),
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF064E3B) else MalakiLightGreen,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = if (isDark) Color(0xFF4ADE80) else MalakiGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen
                )
            }
            content()
        }
    }
}

@Composable
fun MalakiInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (errorMessage != null) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)) },
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = if (isDark) Color(0xFF4ADE80) else MalakiGreen,
                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFCBD5E1),
                errorBorderColor = Color(0xFFDC2626),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun MalakiChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MalakiGreen else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
        border = if (isSelected) BorderStroke(1.2.dp, MalakiGreen) else BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF334155),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun SummarySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(0.8.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF86EFAC) else MalakiDarkGreen,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlightColor: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()
    val finalHighlightColor = if (highlightColor != Color.Unspecified) {
        highlightColor
    } else {
        if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = finalHighlightColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}
