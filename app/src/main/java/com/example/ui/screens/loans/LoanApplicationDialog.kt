package com.example.ui.screens.loans

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.ClientAvatar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.repository.MicrocreditRepository
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

// List of Markets in Goma as requested
val PROPOSED_MARKETS = listOf(
    "Marché de Kituku",
    "Marché Alanine",
    "Marché Virunga",
    "Marché de Nyabushongo",
    "Marché de Rwasama",
    "Marché La Vision",
    "Marché de Mubi",
    "Marché de Birere",
    "Autres..."
)

@Composable
fun FullLoanApplicationDialog(
    clientId: String,
    initialClientName: String,
    initialClientPhone: String,
    initialQuarter: String = "",
    initialMarketZone: String = "Marché de Kituku",
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        durationDays: Int,
        purpose: String,
        activityType: String,
        marketZone: String,
        quarter: String,
        idCardNumber: String,
        clientPhotoUri: String?,
        guarantorName: String,
        guarantorPhone: String,
        acceptedTerms: Boolean
    ) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var clientName by remember { mutableStateOf(initialClientName) }
    var clientPhone by remember { mutableStateOf(initialClientPhone) }
    var quarter by remember { mutableStateOf(initialQuarter) }
    var idCardNumber by remember { mutableStateOf("") }
    
    // Market selection
    var selectedMarketOption by remember {
        mutableStateOf(
            if (PROPOSED_MARKETS.contains(initialMarketZone)) initialMarketZone else "Marché de Kituku"
        )
    }
    var customMarketText by remember { mutableStateOf("") }
    
    val effectiveMarket by remember {
        derivedStateOf {
            if (selectedMarketOption == "Autres...") {
                customMarketText.trim().ifEmpty { "Autre marché" }
            } else {
                selectedMarketOption
            }
        }
    }

    var activityType by remember { mutableStateOf("Commerce de vivres et alimentation") }
    var purpose by remember { mutableStateOf("Achat de stock et renforcement fonds de roulement") }
    
    // Loan amount: begins at 50 000 FC up to 400 000 FC (accessible at 50 000 FC, 60 000 FC...), daily rate = 3.5%
    var loanAmount by remember { mutableDoubleStateOf(50000.0) }
    var durationDays by remember { mutableStateOf(30) }
    
    // Daily calculation: 3.5% of borrowed amount
    val dailyPaymentAmount by remember {
        derivedStateOf { loanAmount * 0.035 }
    }
    val totalContractualAmount by remember {
        derivedStateOf { dailyPaymentAmount * durationDays }
    }
    val grossProfitGenerated by remember {
        derivedStateOf { (totalContractualAmount - loanAmount).coerceAtLeast(0.0) }
    }
    val clientProfitShare50Percent by remember {
        derivedStateOf { grossProfitGenerated * 0.50 }
    }

    // Guarantor (optional or recommended)
    var guarantorName by remember { mutableStateOf("Référence ou Garant solidaire") }
    var guarantorPhone by remember { mutableStateOf("+243 ") }

    // Client Photo & Gender
    var selectedGender by remember { mutableStateOf("Femme") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
        }
    }

    // Contract terms agreement
    var acceptedTerms by remember { mutableStateOf(false) }
    var showContractModal by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Demande de Crédit Client Solidaire",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MALISHO NA AKIBA • Collecte journalière 3,5%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Photo & Genre du client
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedPhotoUri != null) {
                                        AsyncImage(
                                            model = selectedPhotoUri,
                                            contentDescription = "Photo du client",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        ClientAvatar(
                                            gender = selectedGender,
                                            size = 68.dp,
                                            showGenderBadge = true
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Photo du profil client",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Avatar personnalisé automatique selon le sexe",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (selectedPhotoUri != null) "Changer" else "Photo perso", fontSize = 11.sp)
                                        }
                                        if (selectedPhotoUri != null) {
                                            OutlinedButton(
                                                onClick = { selectedPhotoUri = null },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Effacer", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Sexe / Genre du client * :", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedGender == "Femme") (if (isDark) Color(0xFF831843) else Color(0xFFFDF2F8)) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (selectedGender == "Femme") 1.8.dp else 1.dp,
                                        color = if (selectedGender == "Femme") (if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedGender = "Femme" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        ClientAvatar(gender = "Femme", size = 26.dp, showGenderBadge = false)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Femme",
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedGender == "Femme") FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedGender == "Femme") (if (isDark) Color(0xFFFBCFE8) else Color(0xFFBE185D)) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedGender == "Homme") (if (isDark) Color(0xFF0C4A6E) else Color(0xFFF0F9FF)) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (selectedGender == "Homme") 1.8.dp else 1.dp,
                                        color = if (selectedGender == "Homme") (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedGender = "Homme" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        ClientAvatar(gender = "Homme", size = 26.dp, showGenderBadge = false)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Homme",
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedGender == "Homme") FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedGender == "Homme") (if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Montant du prêt (Max 40 000 FC selon la confiance) & Collecte journalière 3.5%
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MONTANT SOLLICITÉ",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "De 100 000 FC à 400 000 FC",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = MicrocreditRepository.formatAmount(loanAmount),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            loanAmount = (loanAmount - 10000.0).coerceAtLeast(50000.0)
                                        },
                                        enabled = loanAmount > 50000.0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                    ) {
                                        Text("-10k", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            loanAmount = (loanAmount + 10000.0).coerceAtMost(400000.0)
                                        },
                                        enabled = loanAmount < 400000.0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                    ) {
                                        Text("+10k", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = "Montant minimum : 50 000 FC (choix direct de 50 000 FC, 60 000 FC... jusqu'à 400 000 FC)",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Slider to choose between 50 000 FC and 400 000 FC (step: 10 000 FC)
                            Slider(
                                value = loanAmount.toFloat(),
                                onValueChange = { loanAmount = (Math.round(it / 10000.0) * 10000).toDouble().coerceIn(50000.0, 400000.0) },
                                valueRange = 50000f..400000f,
                                steps = 34,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentSkyBlue,
                                    activeTrackColor = AccentSkyBlue,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            // Quick Amount Chips (featuring 50 000 FC and 60 000 FC)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(50000.0, 60000.0, 100000.0, 200000.0).forEach { amt ->
                                    FilterChip(
                                        selected = loanAmount == amt,
                                        onClick = { loanAmount = amt },
                                        label = { Text("${(amt / 1000).toInt()}k FC", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentSkyBlue,
                                            selectedLabelColor = PrimaryNavy,
                                            containerColor = Color.White.copy(alpha = 0.15f),
                                            labelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Daily 3.5% collection summary card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("COLLECTE JOURNALIÈRE (3,5%)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${MicrocreditRepository.formatAmount(dailyPaymentAmount)} / jour",
                                        color = SuccessGreen,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("50% BÉNÉFICES POUR VOUS", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "+ ${MicrocreditRepository.formatAmount(clientProfitShare50Percent)}",
                                        color = AccentSkyBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⏰ Chaque soir, notre agent de terrain passe directement à votre lieu de commerce au marché pour recouvrer les 3,5% (${MicrocreditRepository.formatAmount(dailyPaymentAmount)}).",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Section 3: Informations Personnelles (Quartier + Pièce d'identité facultatif)
                item {
                    Text(
                        text = "1. Identification du commerçant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom complet du demandeur *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Numéro de téléphone (Appel & SMS) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = quarter,
                        onValueChange = { quarter = it },
                        label = { Text("Quartier de résidence *") },
                        placeholder = { Text("Ex: Birere, Virunga, Majengo, Katindo, Ndosho...") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_quarter")
                    )
                }

                item {
                    OutlinedTextField(
                        value = idCardNumber,
                        onValueChange = { idCardNumber = it },
                        label = { Text("Numéro de la pièce d'identité (Facultatif)") },
                        placeholder = { Text("Numéro de carte d'électeur ou passeport") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_id_card")
                    )
                }

                // Section 4: Marché où il exerce ses activités (Requirement)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Marché d'activité professionnelle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dans quel marché exercez-vous vos activités commerciales ?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PROPOSED_MARKETS.chunked(2).forEach { rowMarkets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMarkets.forEach { market ->
                                    FilterChip(
                                        selected = selectedMarketOption == market,
                                        onClick = { selectedMarketOption = market },
                                        label = {
                                            Text(
                                                text = market,
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedMarketOption == market) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowMarkets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // If "Autres..." selected, text field appears to type custom market
                item {
                    AnimatedVisibility(visible = selectedMarketOption == "Autres...") {
                        OutlinedTextField(
                            value = customMarketText,
                            onValueChange = { customMarketText = it },
                            label = { Text("Précisez votre marché ou lieu de commerce *") },
                            placeholder = { Text("Ex: Marché Kahembe, Kilijiwe, etc.") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("input_custom_market")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = activityType,
                        onValueChange = { activityType = it },
                        label = { Text("Type d'activité / Marchandise vendue *") },
                        placeholder = { Text("Ex: Vente de farine, poissons, légumes, pagnes...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 5: Bouton de validation des règles & contrat (Requirement)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (acceptedTerms) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (acceptedTerms) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (acceptedTerms) SuccessGreen else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Règles, Modalités & Contrat d'Engagement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notre modèle solidaire garantit la collecte chaque soir de 3,5% au marché, une rétrocession de 50% des bénéfices générés pour vous, et la liberté de rembourser le capital à la fin.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bouton cliquable qui reprend comment nous travaillons sous forme de contrat
                            Button(
                                onClick = { showContractModal = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (acceptedTerms) SuccessGreen else PrimaryNavy
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_read_contract")
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (acceptedTerms) "Contrat Validé (Cliquer pour relire)" else "📜 Lire le Contrat & Règles (50% de Bénéfices)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { acceptedTerms = !acceptedTerms }
                            ) {
                                Checkbox(
                                    checked = acceptedTerms,
                                    onCheckedChange = { acceptedTerms = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "J'accepte formellement les règles et le contrat de remboursement journalier à 3,5% et partage de 50% de bénéfices.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        errorMessage = "Veuillez renseigner votre nom complet."
                        return@Button
                    }
                    if (quarter.isBlank()) {
                        errorMessage = "Veuillez renseigner votre quartier."
                        return@Button
                    }
                    if (selectedMarketOption == "Autres..." && customMarketText.isBlank()) {
                        errorMessage = "Veuillez préciser le nom de votre marché."
                        return@Button
                    }
                    if (!acceptedTerms) {
                        errorMessage = "Vous devez lire et valider les règles et modalités du contrat ci-dessus."
                        return@Button
                    }
                    errorMessage = null
                    onSubmit(
                        loanAmount,
                        durationDays,
                        purpose,
                        activityType,
                        effectiveMarket,
                        quarter,
                        idCardNumber,
                        selectedPhotoUri,
                        guarantorName,
                        guarantorPhone,
                        acceptedTerms
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                enabled = acceptedTerms,
                modifier = Modifier.testTag("btn_submit_loan_application")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Valider & Soumettre ma Demande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    // Contract text modal (Requirement: un bouton cliquable qui reprend comment nous travaillons et comment nous donnerons 50% de bénéfices réalisés et à la fin si client veut rembourser le capital; sous forme de contrat)
    if (showContractModal) {
        ContractTermsModalDialog(
            clientName = clientName.ifEmpty { "Le Client" },
            marketZone = effectiveMarket,
            quarter = quarter.ifEmpty { "Non spécifié" },
            loanAmount = loanAmount,
            dailyPayment = dailyPaymentAmount,
            clientProfitShare = clientProfitShare50Percent,
            durationDays = durationDays,
            onDismiss = { showContractModal = false },
            onAccept = {
                acceptedTerms = true
                showContractModal = false
            }
        )
    }
}

@Composable
fun ContractTermsModalDialog(
    clientName: String,
    marketZone: String,
    quarter: String,
    loanAmount: Double,
    dailyPayment: Double,
    clientProfitShare: Double,
    durationDays: Int,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("CONTRAT D'ENGAGEMENT MUTUEL", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("MALISHO NA AKIBA (MALAKI) • Microcrédit Solidaire de Proximité", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ENTRE LES SOUSSIGNÉS :", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryNavy)
                        Text("1. L'Institution MALISHO NA AKIBA (MALAKI), œuvrant pour l'inclusion financière des commerçants.", fontSize = 11.sp)
                        Text("2. M./Mme $clientName (Client), exerçant au $marketZone (Quartier : $quarter).", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Text("IL A ÉTÉ CONVENU ET ARRÊTÉ CE QUI SUIT :", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // Article 1
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ARTICLE 1 : OBJET DU PRÊT & CONFIANCE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "L'institution accorde au client un prêt de développement commercial de ${MicrocreditRepository.formatAmount(loanAmount)}. Les montants prêtés commencent à 100 000 FC et vont jusqu'à 400 000 FC selon la confiance acquise, destinés à financer directement les activités du commerçant.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Article 2
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ARTICLE 2 : MODALITÉ DE RECOUVREMENT JOURNALIER (3,5%)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Pour chaque prêt, une collecte journalière de 3,5% du capital prêté est effectuée, soit un montant exact de ${MicrocreditRepository.formatAmount(dailyPayment)} par jour. Chaque soir, un agent de terrain attitré passe directement sur le lieu du commerce / étalage au marché pour encaisser la tranche et remettre un reçu certifié.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Article 3
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ARTICLE 3 : PARTAGE ÉQUITABLE DE 50% DES BÉNÉFICES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Notre principe fondamental est la solidarité et le partenariat gagnant-gagnant. À l'issue du cycle de $durationDays jours, cinquante pour cent (50%) de l'ensemble des bénéfices nets réalisés sur l'opération (soit une projection de ${MicrocreditRepository.formatAmount(clientProfitShare)}) sont reversés et restitués directement au client !",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Article 4
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ARTICLE 4 : CLÔTURE DU CYCLE ET CAPITAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "À la fin de la période convenue, le client est totalement libre : s'il le désire, il peut solder et rembourser le capital initial, ou bien reconduire un nouveau cycle de crédit avec un montant accru grâce à la confiance établie.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Article 5
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ARTICLE 5 : ENGAGEMENT & PÉNALITÉS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Le client s'engage à être présent à son stand lors du passage de l'agent chaque soir et à verser régulièrement la tranche journalière de 3,5%. Tout retard non motivé suspend le partage des bénéfices.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "En cliquant sur 'J'accepte le Contrat', vous confirmez avoir pris connaissance de l'ensemble de ces clauses et vous vous engagez à respecter les modalités de collecte journalière.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.testTag("btn_accept_contract_modal")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("J'accepte le Contrat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
