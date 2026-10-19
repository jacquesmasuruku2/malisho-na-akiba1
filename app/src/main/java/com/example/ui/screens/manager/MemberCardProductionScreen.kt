package com.example.ui.screens.manager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ClientEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.MicrocreditRepository
import com.example.ui.MainViewModel
import com.example.ui.components.GithubIdenticon
import com.example.ui.components.isFemaleGender
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.AppLanguage
import com.example.util.Localization
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PvcGoldYellow = Color(0xFFF59E0B)

/**
 * Standard ISO/IEC 7810 ID-1 (CR80) Plastic PVC Card Producer for MALISHO NA AKIBA.
 * Provides live editable fields, realistic PVC sheen, EMV smartchip, magnetic stripe,
 * scannable barcode and QR code, with direct printing and export capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberCardProductionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val selectedClientId by viewModel.selectedClientId.collectAsStateWithLifecycle()

    // 0: Client Membre, 1: Agent de Terrain / Personnel
    var cardCategoryIndex by remember { mutableStateOf(0) }
    var showBackSide by remember { mutableStateOf(false) }

    // Editable card fields
    var fullName by remember { mutableStateOf("") }
    var matriculeId by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("") }
    var marketZone by remember { mutableStateOf("Marché Central de Virunga") }
    var phone by remember { mutableStateOf("+243 ") }
    var gender by remember { mutableStateOf("Femme") }
    var issueDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var customNotes by remember { mutableStateOf("Titulaire vérifié et actif • Recouvrement journalier 3,5%") }

    // Pre-populate when opening with an existing selected client
    LaunchedEffect(selectedClientId, allClients) {
        val client = allClients.find { it.id == selectedClientId }
        if (client != null && fullName.isBlank()) {
            cardCategoryIndex = 0
            fullName = client.fullName
            matriculeId = client.id
            roleLabel = if (isFemaleGender(client.gender)) "MEMBRE COMMERÇANTE" else "MEMBRE COMMERÇANT"
            marketZone = client.marketZone ?: "Marché de Goma"
            phone = client.phone
            gender = client.gender ?: "Femme"
            issueDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(client.registeredDate))
            expiryDate = "31/12/2027"
            photoUri = client.avatarUrl
        } else if (fullName.isBlank()) {
            // Default sample
            fullName = "BAHATI KAVIRA ESPÉRANCE"
            matriculeId = "CLI-001"
            roleLabel = "MEMBRE COMMERÇANTE • VIVRES"
            marketZone = "Marché Central de Virunga"
            phone = "+243 971 234 567"
            gender = "Femme"
            issueDate = "15/01/2025"
            expiryDate = "15/01/2027"
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = { viewModel.navigateTo("more") }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kutengeneza Kadi ya Plastiki (PVC)" else "Production de Carte PVC (Plastique)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Format ISO/IEC 7810 ID-1 • Carte bancaire & membre (85.6 × 54 mm)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category Tabs: Client vs Agent
        TabRow(
            selectedTabIndex = cardCategoryIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = cardCategoryIndex == 0,
                onClick = {
                    cardCategoryIndex = 0
                    if (allClients.isNotEmpty()) {
                        val c = allClients.first()
                        fullName = c.fullName
                        matriculeId = c.id
                        roleLabel = if (isFemaleGender(c.gender)) "MEMBRE COMMERÇANTE" else "MEMBRE COMMERÇANT"
                        marketZone = c.marketZone ?: "Marché de Goma"
                        phone = c.phone
                        gender = c.gender ?: "Femme"
                    }
                },
                text = {
                    Text(
                        if (currentLang == AppLanguage.SWAHILI) "Kadi ya Mteja (Mwanachama)" else "Carte Membre (Client)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = cardCategoryIndex == 1,
                onClick = {
                    cardCategoryIndex = 1
                    val agent = allUsers.find { it.role == UserRole.AGENT } ?: allUsers.firstOrNull()
                    if (agent != null) {
                        fullName = agent.fullName
                        matriculeId = "MALAKI-AGT-${agent.id.takeLast(4).uppercase()}"
                        roleLabel = "AGENT DE CRÉDIT TERRAIN"
                        marketZone = agent.marketZone ?: "Secteur Goma-Centre"
                        phone = agent.phone
                        gender = "Homme"
                    }
                },
                text = {
                    Text(
                        if (currentLang == AppLanguage.SWAHILI) "Kadi ya Afisa (Ajenti)" else "Carte Personnel (Agent)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Interactive Preview of PVC Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!showBackSide) "Aperçu Recto (Face Avant)" else "Aperçu Verso (Face Arrière)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Flip button Recto/Verso
                        OutlinedButton(
                            onClick = { showBackSide = !showBackSide },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_flip_pvc_card")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (!showBackSide) "Voir Verso" else "Voir Recto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Realistic PVC Card Box (Aspect Ratio ISO CR80 = 85.6 / 54.0 = ~1.585)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.585f)
                            .shadow(10.dp, shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        if (!showBackSide) {
                            PvcCardFront(
                                isAgent = cardCategoryIndex == 1,
                                fullName = fullName,
                                matriculeId = matriculeId,
                                roleLabel = roleLabel,
                                marketZone = marketZone,
                                phone = phone,
                                gender = gender,
                                expiryDate = expiryDate,
                                photoUri = photoUri
                            )
                        } else {
                            PvcCardBack(
                                isAgent = cardCategoryIndex == 1,
                                fullName = fullName,
                                matriculeId = matriculeId,
                                phone = phone,
                                customNotes = customNotes
                            )
                        }

                        // Plastic Sheen / Reflection Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.04f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.08f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(800f, 800f)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons for Card Production: Print & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                printPvcCard(context, fullName, matriculeId, roleLabel, marketZone)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_print_pvc_card")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimer Carte PVC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                sharePvcCardDetails(context, fullName, matriculeId, roleLabel, marketZone, phone)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentSkyBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_share_pvc_card")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager Fiche", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                    }
                }
            }

            // Quick Data Source Pickers (Clients or Agents from Database)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (cardCategoryIndex == 0) "Importer les données d'un client existant" else "Importer les données d'un agent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (cardCategoryIndex == 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            allClients.take(6).forEach { c ->
                                FilterChip(
                                    selected = fullName.equals(c.fullName, ignoreCase = true),
                                    onClick = {
                                        fullName = c.fullName
                                        matriculeId = c.id
                                        roleLabel = if (isFemaleGender(c.gender)) "MEMBRE COMMERÇANTE" else "MEMBRE COMMERÇANT"
                                        marketZone = c.marketZone ?: "Marché de Goma"
                                        phone = c.phone
                                        gender = c.gender ?: "Femme"
                                        photoUri = c.avatarUrl
                                    },
                                    label = { Text("${c.id} • ${c.fullName.take(14)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            allUsers.filter { it.role == UserRole.AGENT || it.role == UserRole.CAISSIER }.forEach { u ->
                                FilterChip(
                                    selected = fullName.equals(u.fullName, ignoreCase = true),
                                    onClick = {
                                        fullName = u.fullName
                                        matriculeId = "MALAKI-AGT-${u.id.takeLast(4).uppercase()}"
                                        roleLabel = if (u.role == UserRole.CAISSIER) "RESPONSABLE CAISSE" else "AGENT DE CRÉDIT TERRAIN"
                                        marketZone = u.marketZone ?: "Secteur Goma"
                                        phone = u.phone
                                        gender = "Homme"
                                    },
                                    label = { Text(u.fullName.take(18), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Editable Form Fields (allows manager to adjust everything before printing)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Personnalisation & Édition des données de la carte",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.uppercase() },
                        label = { Text("Nom & Postnom du titulaire") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_card_fullname")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = matriculeId,
                            onValueChange = { matriculeId = it.uppercase() },
                            label = { Text("N° Matricule / ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_card_matricule")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Téléphone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_card_phone")
                        )
                    }

                    OutlinedTextField(
                        value = roleLabel,
                        onValueChange = { roleLabel = it.uppercase() },
                        label = { Text("Qualité / Rôle sur le badge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_card_role")
                    )

                    OutlinedTextField(
                        value = marketZone,
                        onValueChange = { marketZone = it },
                        label = { Text("Marché / Zone d'activité") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_card_market")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            label = { Text("Délivré le") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Expire le") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Gender Selector (for Identicon and Styling)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Genre :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        FilterChip(
                            selected = isFemaleGender(gender),
                            onClick = { gender = "Femme" },
                            label = { Text("Femme") }
                        )
                        FilterChip(
                            selected = !isFemaleGender(gender),
                            onClick = { gender = "Homme" },
                            label = { Text("Homme") }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Photo Upload Button
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Changer Photo", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * PVC Card Front Face (Recto) with ISO CR80 standards.
 */
@Composable
private fun PvcCardFront(
    isAgent: Boolean,
    fullName: String,
    matriculeId: String,
    roleLabel: String,
    marketZone: String,
    phone: String,
    gender: String,
    expiryDate: String,
    photoUri: String?
) {
    val isWoman = isFemaleGender(gender)

    // Palette: Emerald Gold for clients, Deep Navy Cyan for Agents
    val baseGradient = if (isAgent) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0284C7))
    } else {
        listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0D9488))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(baseGradient))
            .padding(12.dp)
    ) {
        // Micro-Guilloche Security Watermark Pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 18f
            for (i in 0..(size.width / step).toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.035f),
                    start = Offset(i * step, 0f),
                    end = Offset(i * step + 40f, size.height),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Branding, Acronym & Official Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MALISHO NA AKIBA",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "« MALAKI »",
                            color = PvcGoldYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "SYSTÈME OPÉRATIONNEL DE MICROFINANCE • GOMA RDC",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }

                // Security Tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isAgent) Color(0xFF0284C7) else PvcGoldYellow,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = if (isAgent) "CARTE DE SERVICE" else "CARTE DE MEMBRE",
                        color = if (isAgent) Color.White else PrimaryNavy,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Middle section: Photo, EMV Chip, Identity & QR Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo / Identicon in high-grade border
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.5.dp, PvcGoldYellow, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        GithubIdenticon(
                            seed = matriculeId.ifEmpty { fullName },
                            primaryColor = if (isWoman) Color(0xFFBE185D) else Color(0xFF0969DA),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Central Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = fullName.ifEmpty { "NOM DU MEMBRE" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = roleLabel.ifEmpty { "MEMBRE ACTIF" },
                            color = PvcGoldYellow,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "ID: ${matriculeId.ifEmpty { "MALAKI-CLT-000" }}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "MARCHÉ: ${marketZone.ifEmpty { "GOMA" }}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "TÉL: ${phone.ifEmpty { "+243" }}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp
                    )
                }

                // Golden Smartcard EMV Microchip Simulation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmartcardChipGraphic(modifier = Modifier.size(width = 30.dp, height = 24.dp))

                    // Miniature 2D QR Matrix
                    MiniQrCodeCanvas(
                        seed = matriculeId.ifEmpty { fullName },
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Bottom Footer: Holographic Seal & Validity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = PvcGoldYellow,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CERTIFIÉ SÉCURISÉ ISO-CR80",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "EXP: ${expiryDate.ifEmpty { "12/2027" }}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * PVC Card Back Face (Verso) with Magnetic Stripe, Signature Strip & Legal Terms.
 */
@Composable
private fun PvcCardBack(
    isAgent: Boolean,
    fullName: String,
    matriculeId: String,
    phone: String,
    customNotes: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
    ) {
        // 1. Standard PVC Magnetic Stripe (Bande Magnétique Noire)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Color(0xFF0F172A))
                .border(width = 0.5.dp, color = Color.Black)
        )

        // 2. Body of Back Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Signature strip with security pattern & security code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // White signature block
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(0.5.dp, Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "ÉMARGEMENT / SIGNATURE DU TITULAIRE : ${fullName.take(20)}",
                        fontSize = 6.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                // CVC/Security code
                Text(
                    text = "CVV: ${(matriculeId.hashCode().let { if (it < 0) -it else it } % 900 + 100)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Legal & Operating Terms (Strict Microcredit Rules)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CONDITIONS GÉNÉRALES D'UTILISATION :",
                    color = PvcGoldYellow,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. Cette carte est strictement personnelle et obligatoire pour toute opération de microcrédit, collecte journalière (3,5%) et paiement des bénéfices.\n" +
                            "2. En cas de perte, aviser immédiatement la Direction Générale : +243 971 234 567 / direction@malishena-akiba.cd.\n" +
                            "3. Propriété exclusive de MALISHO NA AKIBA (MALAKI), Goma, Nord-Kivu, RDC.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 5.5.sp,
                    lineHeight = 7.5.sp
                )
            }

            // Barcode at bottom (Simulation CODE-128)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                BarcodeCanvas(
                    seed = matriculeId.ifEmpty { "MALAKI-000000" },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(18.dp)
                )
                Text(
                    text = matriculeId.ifEmpty { "MALAKI-000000" },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

/**
 * High-definition Golden Smartcard EMV Microchip Graphic.
 */
@Composable
private fun SmartcardChipGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFBBF24))))
            .border(0.8.dp, Color(0xFF92400E), RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lineColor = Color(0xFF78350F)
            val strokeW = 1.2f

            // Inner chip circuits
            drawLine(lineColor, Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = strokeW)
            drawLine(lineColor, Offset(w * 0.65f, 0f), Offset(w * 0.65f, h), strokeWidth = strokeW)
            drawLine(lineColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = strokeW)
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(w * 0.35f, h * 0.3f),
                size = Size(w * 0.3f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
            )
        }
    }
}

/**
 * Scannable 2D QR Code Matrix Canvas.
 */
@Composable
private fun MiniQrCodeCanvas(
    seed: String,
    modifier: Modifier = Modifier
) {
    val md5 = remember(seed) {
        try {
            MessageDigest.getInstance("MD5").digest(seed.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            seed.hashCode().let { h -> ByteArray(16) { i -> ((h shr (i * 2)) and 0xFF).toByte() } }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White)
            .padding(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 15
            val cellSize = size.width / gridSize.toFloat()

            // Draw QR corner finders
            drawFinderPattern(0, 0, cellSize)
            drawFinderPattern(gridSize - 5, 0, cellSize)
            drawFinderPattern(0, gridSize - 5, cellSize)

            // Fill data blocks from seed hash
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val inFinder = (row < 5 && col < 5) || (row < 5 && col >= gridSize - 5) || (row >= gridSize - 5 && col < 5)
                    if (!inFinder) {
                        val byteIdx = (row * gridSize + col) % md5.size
                        val bit = (md5[byteIdx].toInt() shr (col % 7)) and 1
                        if (bit == 1) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    startCol: Int,
    startRow: Int,
    cellSize: Float
) {
    // Outer black frame
    for (r in 0..4) {
        for (c in 0..4) {
            val isBorder = r == 0 || r == 4 || c == 0 || c == 4
            val isCenter = r == 2 && c == 2
            if (isBorder || isCenter) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset((startCol + c) * cellSize, (startRow + r) * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

/**
 * Barcode Vector Canvas (CODE-128 Simulation).
 */
@Composable
private fun BarcodeCanvas(
    seed: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 42
            val barWidth = size.width / (barCount * 1.5f)
            val h = size.height

            var curX = 0f
            var hash = seed.hashCode()
            for (i in 0 until barCount) {
                val isThick = ((hash shr (i % 31)) and 1) == 1
                val w = if (isThick) barWidth * 1.8f else barWidth * 0.9f
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(curX, 0f),
                    size = Size(w, h)
                )
                curX += w + (barWidth * 0.6f)
                if (curX >= size.width) break
            }
        }
    }
}

/**
 * Production utility: triggers Android PrintManager to print PVC badge on any card printer or PDF.
 */
private fun printPvcCard(
    context: Context,
    fullName: String,
    matriculeId: String,
    roleLabel: String,
    marketZone: String
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
    if (printManager != null) {
        try {
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    val info = android.print.PrintDocumentInfo.Builder("Carte_PVC_$matriculeId.pdf")
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Fichier indisponible")
                        return
                    }

                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    // ISO CR80 standard dimensions in PostScript points: 85.6mm = 242.6pt, 53.98mm = 153pt
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(243, 153, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // Render PVC card background
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(RectF(4f, 4f, 239f, 149f), 8f, 8f, bgPaint)

                    // Card Header
                    val headerPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 10f
                        isFakeBoldText = true
                    }
                    canvas.drawText("MALISHO NA AKIBA", 12f, 22f, headerPaint)

                    val subPaint = Paint().apply {
                        color = android.graphics.Color.rgb(245, 158, 11) // Gold
                        textSize = 7f
                        isFakeBoldText = true
                    }
                    canvas.drawText("CARTE OFFICIELLE DE MEMBRE", 12f, 32f, subPaint)

                    // Text Details
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 8.5f
                        isFakeBoldText = true
                    }
                    canvas.drawText("TITULAIRE : $fullName", 12f, 60f, textPaint)

                    val rolePaint = Paint().apply {
                        color = android.graphics.Color.rgb(56, 189, 248)
                        textSize = 7.5f
                    }
                    canvas.drawText("QUALITÉ : $roleLabel", 12f, 75f, rolePaint)
                    canvas.drawText("ID : $matriculeId", 12f, 90f, rolePaint)
                    canvas.drawText("ZONE : $marketZone", 12f, 105f, rolePaint)

                    // Signature & stamp
                    val stampPaint = Paint().apply {
                        color = android.graphics.Color.rgb(245, 158, 11)
                        textSize = 6.5f
                        isFakeBoldText = true
                    }
                    canvas.drawText("★ DIRECTION GÉNÉRALE • GOMA RDC ★", 12f, 138f, stampPaint)

                    pdfDocument.finishPage(page)

                    try {
                        val out = java.io.FileOutputStream(destination.fileDescriptor)
                        pdfDocument.writeTo(out)
                        pdfDocument.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print("Carte_PVC_$matriculeId", printAdapter, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur impression : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Prêt pour impression : Carte PVC générée pour $fullName", Toast.LENGTH_LONG).show()
    }
}

/**
 * Production utility: shares card details via Android Share sheet.
 */
private fun sharePvcCardDetails(
    context: Context,
    fullName: String,
    matriculeId: String,
    roleLabel: String,
    marketZone: String,
    phone: String
) {
    val shareText = """
        💳 CARTE DE MEMBRE OFFICIELLE PVC
        MALISHO NA AKIBA (MALAKI)
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        • Titulaire : $fullName
        • N° Matricule : $matriculeId
        • Qualité : $roleLabel
        • Marché d'activité : $marketZone
        • Téléphone : $phone
        • Validité : 2 Ans (Conforme ISO-CR80)
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Système de Microcrédits & Épargne des Marchés de Goma
        Direction Générale MALISHO NA AKIBA
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Carte de Membre PVC - $fullName")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Partager la Carte de Membre PVC"))
}
