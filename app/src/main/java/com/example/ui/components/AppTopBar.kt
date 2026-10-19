package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.BybitAvatar
import com.example.ui.components.BybitAvatarPickerDialog
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.Localization

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    currentUser: UserEntity?,
    googleAccountEmail: String = "jacquesmasuruku2@gmail.com",
    unreadNotificationsCount: Int = 0,
    pendingSyncCount: Int = 0,
    isDarkMode: Boolean? = null,
    showBackButton: Boolean = false,
    currentLanguage: AppLanguage = LocalAppLanguage.current,
    onBackClick: () -> Unit = {},
    onHamburgerClick: () -> Unit = {},
    onRecouvrementClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onLogoutGoogle: () -> Unit = {},
    onSwitchRole: (UserRole) -> Unit = {},
    onAvatarSelected: (String) -> Unit = {},
    onPhotoSelectedFromGallery: ((Uri) -> Unit)? = null,
    isUploadingPhoto: Boolean = false
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(44.dp).testTag("top_bar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // MENU HAMBURGER XENDER
                IconButton(
                    onClick = onHamburgerClick,
                    modifier = Modifier.size(44.dp).testTag("top_bar_hamburger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Principal Hamburger",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(3.dp))

                // Logo officiel MALAKI dans la barre supérieure
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable(onClick = onHamburgerClick),
                    shadowElevation = 1.5.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.malaki_official_logo),
                        contentDescription = "Logo MALAKI",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle ?: (currentUser?.let { "${it.fullName} • ${Localization.roleLabel(it.role, currentLanguage)}" } ?: "MALISHO NA AKIBA (MALAKI)"),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Sync status indicator
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.size(48.dp).testTag("sync_button")
                ) {
                    if (pendingSyncCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = WarningAmber) {
                                    Text(pendingSyncCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "En attente de synchronisation",
                                tint = WarningAmber
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synchronisé",
                            tint = SuccessGreen
                        )
                    }
                }

                // Notifications icon
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(48.dp).testTag("notifications_button")
                ) {
                    if (unreadNotificationsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(unreadNotificationsCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Profile Avatar (Clicking the avatar displays role & accesses Espace Gestionnaire / Sécurité)
                BybitAvatar(
                    avatarKey = currentUser?.avatarKey,
                    role = currentUser?.role,
                    isOnline = true,
                    size = 38.dp,
                    showPresenceDot = true,
                    onClick = { showRoleDialog = true },
                    modifier = Modifier.testTag("topbar_user_avatar")
                )
            }
        }
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = {
                Text(
                    text = if (currentUser?.role == UserRole.GESTIONNAIRE)
                        Localization.tr("topbar_role_title", currentLanguage)
                    else
                        (if (currentLanguage == AppLanguage.SWAHILI) "Akaunti & Usalama" else "Compte & Sécurité"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    // Authenticated user card with Avatar and Edit button
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BybitAvatar(
                                avatarKey = currentUser?.avatarKey,
                                role = currentUser?.role,
                                isOnline = true,
                                size = 48.dp,
                                showPresenceDot = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.fullName ?: googleAccountEmail,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Localization.roleLabel(currentUser?.role, currentLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentUser?.marketZone != null) {
                                    Text(
                                        text = "Zone : ${currentUser.marketZone}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                showRoleDialog = false
                                showAvatarPicker = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Changer l'avatar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Biometric notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.SWAHILI) "Usalama wa alama ya kidole umewashwa" else "Sécurité par empreinte digitale active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser?.role == UserRole.GESTIONNAIRE) {
                        Text(
                            Localization.tr("topbar_switch_role_desc", currentLanguage),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        UserRole.entries.forEach { role ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSwitchRole(role)
                                        showRoleDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserRoleBadge(role = role, lang = currentLanguage)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Localization.roleLabel(role, currentLanguage),
                                    fontWeight = if (currentUser?.role == role) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            showRoleDialog = false
                            onLogoutGoogle()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_topbar_dialog_logout")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentLanguage == AppLanguage.SWAHILI) "Kutoka / Kufunga Usalama" else "Se déconnecter (Verrouiller)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CopyRight | Développé par Jacques M. • Version 1.0.0",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text(Localization.tr("action_close", currentLanguage))
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
                onAvatarSelected(key)
                showAvatarPicker = false
            },
            onPhotoSelectedFromGallery = { uri ->
                onPhotoSelectedFromGallery?.invoke(uri)
                showAvatarPicker = false
            },
            isUploadingPhoto = isUploadingPhoto,
            onDismiss = { showAvatarPicker = false }
        )
    }
}
