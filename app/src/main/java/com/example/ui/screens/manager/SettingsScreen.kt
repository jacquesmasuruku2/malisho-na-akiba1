package com.example.ui.screens.manager

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BusinessSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.components.BybitAvatar
import com.example.ui.components.BybitAvatarPickerDialog
import com.example.ui.components.BybitRoleBadge
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricHardwareStatus
import com.example.util.SavedCredential
import com.example.util.SavedCredentialsHelper
import com.example.util.Localization

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.businessSettings.collectAsStateWithLifecycle()
    val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleAccountEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleAccountName.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isBiometricOpsEnabled by viewModel.isBiometricOpsEnabled.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    val isNeonSyncing by viewModel.isNeonSyncing.collectAsStateWithLifecycle()
    val neonSyncStatus by viewModel.neonSyncStatus.collectAsStateWithLifecycle()
    val neonLastError by viewModel.neonLastError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hardwareStatus = remember { BiometricAuthHelper.checkBiometricStatus(context) }

    val directPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadAndSetUserAvatarFromUri(uri, context)
        }
    }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showBiometricTestDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    var orgName by remember(currentSettings) { mutableStateOf(currentSettings?.organizationName ?: "MALISHO NA AKIBA") }
    var initialCap by remember(currentSettings) { mutableStateOf(currentSettings?.initialCapital?.toLong()?.toString() ?: "5000000") }
    var targetCap by remember(currentSettings) { mutableStateOf(currentSettings?.targetCapital?.toLong()?.toString() ?: "15000000") }
    var dailyRate by remember(currentSettings) { mutableStateOf(((currentSettings?.defaultDailyRate ?: 0.035) * 100).toString()) }
    var durationDays by remember(currentSettings) { mutableStateOf(currentSettings?.defaultDurationDays?.toString() ?: "30") }
    var enterpriseShare by remember(currentSettings) { mutableStateOf(((currentSettings?.enterpriseShareRate ?: 0.70) * 100).toString()) }
    var clientShare by remember(currentSettings) { mutableStateOf(((currentSettings?.clientShareRate ?: 0.30) * 100).toString()) }
    var expenseRate by remember(currentSettings) { mutableStateOf(((currentSettings?.expenseAllowanceRate ?: 0.05) * 100).toString()) }
    var lossRate by remember(currentSettings) { mutableStateOf(((currentSettings?.lossProvisionRate ?: 0.02) * 100).toString()) }

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
                        contentDescription = Localization.tr("action_back", currentLang),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = Localization.tr("title_settings", currentLang),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Lugha, mwonekano wa kioo, Google na barua za mikopo" else "Langue, apparence, compte Google & barèmes financiers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // BYBIT PROFILE AVATAR
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("settings_bybit_avatar_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BybitAvatar(
                        avatarKey = currentUser?.avatarKey,
                        role = currentUser?.role,
                        isOnline = true,
                        size = 64.dp,
                        showGlow = true,
                        onClick = { showAvatarPicker = true }
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AVATAR DE PROFIL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = currentUser?.fullName ?: "Jacques Masuruku",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        currentUser?.let { BybitRoleBadge(role = it.role) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    directPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isUploadingAvatar,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Envoi Cloudinary...", fontSize = 11.sp, color = Color.White)
                                } else {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Photo du téléphone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showAvatarPicker = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Identicons", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 1. LANGUAGE SELECTION (Français & Kiswahili facile de la RDC)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .testTag("settings_language_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.tr("settings_language_section", currentLang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentLang.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = Localization.tr("settings_language_desc", currentLang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = currentLang == langOption
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setLanguage(langOption) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("lang_option_${langOption.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(langOption.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = langOption.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = langOption.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sélectionné",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. APPEARANCE / THEME TOGGLE (Requirement 3: Mode Clair & Mode Sombre)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr("settings_theme_section", currentLang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = Localization.tr("settings_theme_desc", currentLang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Light Mode
                        FilterChip(
                            selected = isDarkModePref == false,
                            onClick = { viewModel.setDarkModeExplicit(false) },
                            leadingIcon = {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(Localization.tr("settings_theme_light", currentLang), fontSize = 12.sp, fontWeight = if (isDarkModePref == false) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_theme_light")
                        )

                        // Dark Mode
                        FilterChip(
                            selected = isDarkModePref == true,
                            onClick = { viewModel.setDarkModeExplicit(true) },
                            leadingIcon = {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(Localization.tr("settings_theme_dark", currentLang), fontSize = 12.sp, fontWeight = if (isDarkModePref == true) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_theme_dark")
                        )

                        // Auto / System Mode
                        FilterChip(
                            selected = isDarkModePref == null,
                            onClick = { viewModel.toggleDarkMode() },
                            leadingIcon = {
                                Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(Localization.tr("settings_theme_auto", currentLang), fontSize = 12.sp, fontWeight = if (isDarkModePref == null) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_theme_auto")
                        )
                    }
                }
            }
        }

        // 2. GOOGLE AUTHENTICATION ACCOUNT INFO (Requirement 3: Authentification Google)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.tr("settings_google_section", currentLang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = googleName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = googleEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.logoutGoogle() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr("settings_google_logout", currentLang),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ====================================================
        // SYNCHRONISATION CLOUD NEON POSTGRESQL (BASE DE DONNÉES)
        // ====================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("settings_firebase_sync_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 340.dp
                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "USAWAZISHAJI WA NEON" else "BASE DE DONNÉES NEON",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isNeonSyncing) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isNeonSyncing) WarningAmber.copy(alpha = 0.4f) else SuccessGreen.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isNeonSyncing) WarningAmber else SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isNeonSyncing) {
                                                if (currentLang == AppLanguage.SWAHILI) "Inasawazisha..." else "Synchronisation..."
                                            } else {
                                                if (currentLang == AppLanguage.SWAHILI) "Imeunganishwa na Neon" else "Connecté à Neon Postgres"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNeonSyncing) WarningAmber else SuccessGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
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
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "USAWAZISHAJI WA NEON" else "BASE DE DONNÉES NEON",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isNeonSyncing) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isNeonSyncing) WarningAmber.copy(alpha = 0.4f) else SuccessGreen.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isNeonSyncing) WarningAmber else SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isNeonSyncing) {
                                                if (currentLang == AppLanguage.SWAHILI) "Inasawazisha..." else "Synchronisation..."
                                            } else {
                                                if (currentLang == AppLanguage.SWAHILI) "Imeunganishwa na Neon" else "Connecté à Neon Postgres"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNeonSyncing) WarningAmber else SuccessGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI)
                                    "Usawazishaji wa Moja kwa Moja : Kila shughuli inahifadhiwa kwenye Neon punde simu inapokuwa na intaneti. Nje ya mtandao, inangoja mtandao urejee bila kuhitaji kubofya."
                                else
                                    "Synchronisation 100% Automatique : Toute activité effectuée est directement enregistrée dans Neon Cloud dès que la connexion internet est active. Hors-ligne, les données restent en attente sécurisée et se synchronisent d'elles-mêmes dès le retour du réseau, sans attendre de clic manuel.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Base Neon : malisho-na-akiba (PostgreSQL)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Hôte : ep-sweet-cake-b5zubedo-pooler.c-7.us-east-2.aws.neon.tech",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "État : $neonSyncStatus",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!neonLastError.isNullOrEmpty()) {
                                Text(
                                    text = "Détail : $neonLastError",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncWithNeon() },
                            enabled = !isNeonSyncing,
                            modifier = Modifier.weight(1f).testTag("btn_sync_from_cloud")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Pakua toka Neon" else "Recevoir de Neon",
                                fontSize = 11.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.uploadLocalDataToNeon() },
                            enabled = !isNeonSyncing,
                            modifier = Modifier.weight(1f).testTag("btn_push_to_cloud")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Sawazisha sasa" else "Forcer l'envoi",
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // ====================================================
        // PERSONNALISATION DE LA POLICE ET STYLE DE BULLES (TEXTRA & TYPOGRAPHIE)
        // ====================================================
        item {
            val appFontStyle by viewModel.appFontStyle.collectAsStateWithLifecycle()
            val chatBubbleStyle by viewModel.chatBubbleStyle.collectAsStateWithLifecycle()

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("settings_typography_bubble_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Section 1: Police de l'application
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 340.dp
                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "MTINDO WA MAANDISHI" else "STYLE DE POLICE D'ÉCRITURE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE0F2FE),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(
                                        text = appFontStyle.label.substringBefore(" ("),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        } else {
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
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "MTINDO WA MAANDISHI" else "STYLE DE POLICE D'ÉCRITURE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE0F2FE),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = appFontStyle.label.substringBefore(" ("),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Chagua mtindo wa herufi utakaoonekana kwenye kurasa zote za programu :"
                        else
                            "Choisissez la police qui s'applique à toute l'interface (menus, formulaires, chiffres, chat) :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    com.example.ui.theme.AppFontStyle.entries.forEach { fontOption ->
                        val isSelected = appFontStyle == fontOption
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAppFontStyle(fontOption) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontOption.label,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontFamily = fontOption.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = fontOption.subtitle,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Actif",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Section 2: Forme des bulles de discussion (inspiré Textra)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 340.dp
                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "MTINDO WA VIPUPWE" else "FORME DES BULLES DU CHAT",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f)),
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(
                                        text = chatBubbleStyle.label.substringBefore(" ("),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        } else {
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
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "MTINDO WA VIPUPWE" else "FORME DES BULLES DU CHAT",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = chatBubbleStyle.label.substringBefore(" ("),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Chagua umbo la viputo vya ujumbe kwenye orodha inayoshuka :"
                        else
                            "Choisissez la forme des bulles de messages via la liste déroulante ci-dessous :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sélecteur compact sous forme de liste déroulante (DropdownMenu)
                    var showBubbleDropdown by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7).copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showBubbleDropdown = true }
                                .testTag("bubble_style_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chatBubbleStyle.label,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = chatBubbleStyle.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Text(
                                            text = "Choisir",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Ouvrir la liste déroulante",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showBubbleDropdown,
                            onDismissRequest = { showBubbleDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            com.example.ui.theme.BubbleStyle.entries.forEach { styleOption ->
                                val isSelected = chatBubbleStyle == styleOption
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = styleOption.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = "Actif",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF15803D),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = styleOption.description,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setChatBubbleStyle(styleOption)
                                        showBubbleDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Style,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Sélectionné",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // SÉCURITÉ & BIOMÉTRIE (PROTECTION DES FONDS & ACCÈS)
        // ====================================================
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PrimaryNavy.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("settings_biometric_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 340.dp
                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "USALAMA WA KIDOLE" else "SÉCURITÉ & BIOMÉTRIE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryNavy,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (hardwareStatus) {
                                        BiometricHardwareStatus.AVAILABLE -> SuccessGreen.copy(alpha = 0.15f)
                                        BiometricHardwareStatus.NOT_ENROLLED -> WarningAmber.copy(alpha = 0.15f)
                                        else -> AccentSkyBlue.copy(alpha = 0.15f)
                                    },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(
                                        text = when (hardwareStatus) {
                                            BiometricHardwareStatus.AVAILABLE -> if (currentLang == AppLanguage.SWAHILI) "Kitambuzi kipo tayari" else "Capteur Actif"
                                            BiometricHardwareStatus.NOT_ENROLLED -> if (currentLang == AppLanguage.SWAHILI) "Bila alama" else "Non enrôlé"
                                            else -> if (currentLang == AppLanguage.SWAHILI) "Njia ya Kompyuta" else "Virtuel / Émulateur"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (hardwareStatus) {
                                            BiometricHardwareStatus.AVAILABLE -> SuccessGreen
                                            BiometricHardwareStatus.NOT_ENROLLED -> WarningAmber
                                            else -> PrimaryNavy
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        } else {
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
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.SWAHILI) "USALAMA WA KIDOLE" else "SÉCURITÉ & BIOMÉTRIE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryNavy,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (hardwareStatus) {
                                        BiometricHardwareStatus.AVAILABLE -> SuccessGreen.copy(alpha = 0.15f)
                                        BiometricHardwareStatus.NOT_ENROLLED -> WarningAmber.copy(alpha = 0.15f)
                                        else -> AccentSkyBlue.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = when (hardwareStatus) {
                                            BiometricHardwareStatus.AVAILABLE -> if (currentLang == AppLanguage.SWAHILI) "Kitambuzi kipo tayari" else "Capteur Actif"
                                            BiometricHardwareStatus.NOT_ENROLLED -> if (currentLang == AppLanguage.SWAHILI) "Bila alama" else "Non enrôlé"
                                            else -> if (currentLang == AppLanguage.SWAHILI) "Njia ya Kompyuta" else "Virtuel / Émulateur"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (hardwareStatus) {
                                            BiometricHardwareStatus.AVAILABLE -> SuccessGreen
                                            BiometricHardwareStatus.NOT_ENROLLED -> WarningAmber
                                            else -> PrimaryNavy
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Linda mfumo wa mikopo na pesa za uwanjani kwa kutumia alama ya kidole na PIN ya usalama :"
                        else
                            "Protégez les opérations de microcrédit et les liquidités par authentification biométrique et code PIN :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 1. App Lock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Kufunga programu kwa alama ya kidole" else "Verrouillage de l'application",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI)
                                    "Lazimisha kuweka kidole au PIN wakati wa kufungua MALAKI"
                                else
                                    "Exiger l'empreinte digitale ou le code PIN pour déverrouiller l'application",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { viewModel.setAppLockEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryNavy,
                                checkedTrackColor = PrimaryNavy.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.testTag("switch_app_lock")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 2. Financial Operations Biometric Requirement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Uthibitisho wa kutoa na kupokea pesa" else "Validation biométrique des transactions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI)
                                    "Lazimisha alama ya kidole kabla ya kurekodi malipo au kutoa mkopo"
                                else
                                    "Exiger l'empreinte avant chaque encaissement, décaissement ou sortie de caisse",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = isBiometricOpsEnabled,
                            onCheckedChange = { viewModel.setBiometricOpsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SuccessGreen,
                                checkedTrackColor = SuccessGreen.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.testTag("switch_biometric_ops")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Action buttons: Lock Now, Test Sensor & Change PIN
                    Button(
                        onClick = { viewModel.lockApp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_lock_app_now")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Funga Programu Sasa Hivi" else "Verrouiller l'application maintenant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBiometricTestDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_biometric_sensor")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Jaribu Kidole" else "Tester Capteur",
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                newPinInput = BiometricAuthHelper.getSecurityPin(context)
                                showPinChangeDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_change_security_pin")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Badilisha PIN" else "Modifier PIN",
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 4. Identifiants sauvegardés sur l'appareil (Reconnexion sans mot de passe oublié)
                    var savedCreds by remember { mutableStateOf(SavedCredentialsHelper.getSavedCredentials(context)) }
                    var showSavedCredsDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Identifiants Mémorisés",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (savedCreds.isEmpty())
                                    "Aucun compte sauvegardé sur cet appareil."
                                else
                                    "${savedCreds.size} compte(s) enregistré(s) pour une connexion rapide sans oubli de mot de passe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (savedCreds.isNotEmpty()) {
                            TextButton(onClick = { showSavedCredsDialog = true }) {
                                Text("Gérer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            }
                        }
                    }

                    if (showSavedCredsDialog) {
                        AlertDialog(
                            onDismissRequest = { showSavedCredsDialog = false },
                            icon = {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(28.dp))
                            },
                            title = {
                                Text("Comptes Mémorisés sur l'Appareil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    savedCreds.forEach { cred ->
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(cred.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(cred.email, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        SavedCredentialsHelper.removeCredential(context, cred.email)
                                                        savedCreds = SavedCredentialsHelper.getSavedCredentials(context)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteForever, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        SavedCredentialsHelper.clearAll(context)
                                        savedCreds = emptyList()
                                        showSavedCredsDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Tout effacer", fontSize = 12.sp)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSavedCredsDialog = false }) {
                                    Text("Fermer")
                                }
                            }
                        )
                    }
                }
            }
        }

        // 3. FINANCIAL BUSINESS LOGIC & PARAMETERS
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        Localization.tr("settings_business_section", currentLang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text(Localization.tr("settings_org_name", currentLang)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = initialCap,
                            onValueChange = { initialCap = it },
                            label = { Text(Localization.tr("settings_initial_cap", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetCap,
                            onValueChange = { targetCap = it },
                            label = { Text(Localization.tr("settings_target_cap", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Localization.tr("settings_rates_section", currentLang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dailyRate,
                            onValueChange = { dailyRate = it },
                            label = { Text(Localization.tr("settings_daily_rate", currentLang)) },
                            placeholder = { Text("ex: 3.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            label = { Text(Localization.tr("settings_duration_days", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Localization.tr("settings_profit_split", currentLang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = enterpriseShare,
                            onValueChange = { enterpriseShare = it },
                            label = { Text(Localization.tr("settings_share_company", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = clientShare,
                            onValueChange = { clientShare = it },
                            label = { Text(Localization.tr("settings_share_client", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = expenseRate,
                            onValueChange = { expenseRate = it },
                            label = { Text(Localization.tr("settings_expense_rate", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lossRate,
                            onValueChange = { lossRate = it },
                            label = { Text(Localization.tr("settings_loss_rate", currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val newSettings = BusinessSettingsEntity(
                                id = 1,
                                organizationName = orgName,
                                initialCapital = initialCap.toDoubleOrNull() ?: 5000000.0,
                                targetCapital = targetCap.toDoubleOrNull() ?: 15000000.0,
                                defaultDailyRate = (dailyRate.toDoubleOrNull() ?: 3.5) / 100.0,
                                minInitialLoan = 50000.0,
                                maxInitialLoan = 400000.0,
                                defaultDurationDays = durationDays.toIntOrNull() ?: 30,
                                enterpriseShareRate = (enterpriseShare.toDoubleOrNull() ?: 50.0) / 100.0,
                                clientShareRate = (clientShare.toDoubleOrNull() ?: 50.0) / 100.0,
                                expenseAllowanceRate = (expenseRate.toDoubleOrNull() ?: 5.0) / 100.0,
                                lossProvisionRate = (lossRate.toDoubleOrNull() ?: 2.0) / 100.0
                            )
                            viewModel.updateSettings(newSettings)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.tr("settings_save", currentLang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. MODULES D'ADMINISTRATION RAPIDE
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Localization.tr("settings_modules_section", currentLang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Accédez rapidement aux fonctions avancées de gestion de l'institution :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo("applications") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Demandes", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { viewModel.navigateTo("user_management") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Personnel", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo("projections") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Projections", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { viewModel.navigateTo("audit") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Audit", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // 5. RÉINITIALISATION & DÉMARRAGE À ZÉRO (PURGE DES DONNÉES DE TEST)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Kuanza Upya / Kufuta Data Zote" else "Démarrage à zéro / Purge des données",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Futa data zote za majaribio (wateja, mikopo, malipo, pesa za caisse) ili kuanza upya na mfumo safi kabisa. Akaunti yako ya Msimamizi itabaki salama."
                        else
                            "Supprimez toutes les données de test (clients, crédits, remboursements, écritures de caisse) pour commencer avec une base totalement vierge. Votre compte Direction sera préservé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_purge_data")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.SWAHILI) "Anza upya (Futa data za majaribio)" else "Commencer à zéro (Vider les données de test)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CopyRight | Développé par Jacques M.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "MALISHO NA AKIBA (MALAKI) • Version 1.0.0",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Kuanza upya kutoka mwanzo?" else "Commencer à zéro ?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI)
                        "Hatua hii itafuta kabisa wateja wote, mikopo yote, na miamala yote ya caisse ili uanze kuweka data zako halisi pekee. Je, unathibitisha?"
                    else
                        "Cette action va supprimer tous les clients, prêts et opérations de test enregistrés pour vous permettre de démarrer uniquement avec vos données réelles. Confirmez-vous la réinitialisation ?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetAllDataToZero()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (currentLang == AppLanguage.SWAHILI) "Ndio, Futa Zote" else "Oui, Tout Réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(Localization.tr("action_cancel", currentLang))
                }
            }
        )
    }

    if (showBiometricTestDialog) {
        BiometricPromptDialog(
            title = if (currentLang == AppLanguage.SWAHILI) "Kujaribu Kitambuzi cha Kidole" else "Test du Capteur Biométrique",
            subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako kwenye kitambuzi" else "Posez votre doigt sur le capteur pour tester l'authentification",
            operationSummary = "Test de bon fonctionnement du module de sécurité MALAKI",
            onSuccess = {
                showBiometricTestDialog = false
            },
            onDismiss = {
                showBiometricTestDialog = false
            }
        )
    }

    if (showPinChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = {
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Badilisha Nambari ya Siri (PIN)" else "Modifier le Code PIN de Secours",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI)
                            "Weka nambari ya siri ya tarakimu 4 inayotumika kama mbadala wa alama ya kidole :"
                        else
                            "Entrez un code secret à 4 chiffres utilisé en cas d'indisponibilité du capteur d'empreinte :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6) newPinInput = it },
                        label = { Text("Nouveau Code PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.isNotBlank()) {
                            viewModel.setSecurityPin(newPinInput)
                            showPinChangeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text(if (currentLang == AppLanguage.SWAHILI) "Kuchunga PIN" else "Enregistrer le PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text(if (currentLang == AppLanguage.SWAHILI) "Acha" else "Annuler")
                }
            }
        )
    }

    if (showAvatarPicker) {
        BybitAvatarPickerDialog(
            currentKey = currentUser?.avatarKey,
            role = currentUser?.role,
            gender = "Homme",
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
