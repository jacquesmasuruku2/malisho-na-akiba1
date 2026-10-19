package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import com.example.util.Localization

@Composable
fun XenderDrawerContent(
    currentUser: UserEntity?,
    googleAccountEmail: String = "",
    currentLang: AppLanguage = AppLanguage.FRENCH,
    currentRoute: String = "dashboard",
    isDarkMode: Boolean = false,
    unreadNotificationsCount: Int = 0,
    onNavigate: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onAvatarClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
    ) {
        // ====================================================
        // EN-TÊTE PROFIL XENDER (NOM, FONCTION, INFOS PERTINENTES)
        // ====================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryNavy,
                            PrimaryNavy.copy(alpha = 0.92f),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column {
                // EN-TÊTE LOGO OFFICIEL MALAKI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(38.dp),
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.malaki_official_logo),
                            contentDescription = "Logo Officiel MALAKI",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MALAKI",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFFFEF3C7)
                        )
                        Text(
                            text = "Malisho Na Akiba — Ration et Épargne",
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar grand format avec halo Xender
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, Color(0xFFF59E0B), CircleShape)
                            .clickable(onClick = onAvatarClick)
                            .testTag("drawer_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        BybitAvatar(
                            avatarKey = currentUser?.avatarKey,
                            role = currentUser?.role,
                            isOnline = true,
                            size = 66.dp,
                            showGlow = true
                        )
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Changer la photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // NOM COMPLET DU PROFIL CONNECTÉ
                        Text(
                            text = currentUser?.fullName ?: "Utilisateur MALAKI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // FONCTION / RÔLE DU PROFIL
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (currentUser?.role) {
                                UserRole.GESTIONNAIRE -> Color(0xFFF59E0B)
                                UserRole.AGENT -> AccentSkyBlue
                                UserRole.CAISSIER -> SuccessGreen
                                else -> Color(0xFF94A3B8)
                            }.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (currentUser?.role) {
                                    UserRole.GESTIONNAIRE -> Color(0xFFF59E0B)
                                    UserRole.AGENT -> AccentSkyBlue
                                    UserRole.CAISSIER -> SuccessGreen
                                    else -> Color(0xFF94A3B8)
                                }
                            )
                        ) {
                            Text(
                                text = currentUser?.let { Localization.roleLabel(it.role, currentLang) } ?: "Membre du Personnel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // STATUT EN LIGNE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Upo Hewani (Mtandaoni)" else "En ligne & Connecté",
                                fontSize = 10.5.sp,
                                color = Color(0xFF86EFAC)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AUTRES INFORMATIONS PERTINENTES (TÉLÉPHONE, ZONE, IDENTIFIANT)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentUser?.phone?.takeIf { it.isNotEmpty() }?.let { phone ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(phone, fontSize = 11.5.sp, color = Color.White)
                            }
                        }

                        val email = currentUser?.email?.ifEmpty { googleAccountEmail } ?: googleAccountEmail
                        if (email.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(email, fontSize = 11.sp, color = Color(0xFFE2E8F0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        currentUser?.marketZone?.takeIf { it.isNotEmpty() }?.let { zone ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Affectation : $zone",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFEF08A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BOUTON DIRECT ACCÉDER AUX PARAMÈTRES DANS L'EN-TÊTE DU PROFIL
                if (currentUser?.role != UserRole.CLIENT) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSettingsClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Fungua Mipangilio ya Wasifu" else "Accéder aux Paramètres & Profil",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("client_profile") }
                            .testTag("drawer_btn_client_profile_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.SWAHILI) "Tazama Wasifu & Salio Langu" else "Voir Mon Profil & Solde de Crédit",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isClient = currentUser?.role == UserRole.CLIENT

        if (isClient) {
            // ====================================================
            // MENUS STRICTEMENT RÉSERVÉS AU CLIENT
            // (La caisse, les rapports, la gestion des clients et le tableau de bord sont exclus !)
            // ====================================================
            DrawerMenuItem(
                icon = Icons.Default.Home,
                label = if (currentLang == AppLanguage.SWAHILI) "Ukurasa Wangu wa Mteja" else "Mon Espace Client",
                isSelected = currentRoute == "client_portal",
                onClick = { onNavigate("client_portal") },
                testTag = "drawer_item_client_portal"
            )

            DrawerMenuItem(
                icon = Icons.Default.Person,
                label = if (currentLang == AppLanguage.SWAHILI) "Wasifu & Salio Langu" else "Mon Profil & Solde de Crédit",
                isSelected = currentRoute == "client_profile",
                onClick = { onNavigate("client_profile") },
                testTag = "drawer_item_client_profile"
            )

            DrawerMenuItem(
                icon = Icons.Default.NoteAdd,
                label = if (currentLang == AppLanguage.SWAHILI) "Kuomba Mkopo Mpya" else "Demander un Prêt / Crédit",
                isSelected = currentRoute == "client_loan_apply",
                onClick = { onNavigate("client_loan_apply") },
                testTag = "drawer_item_client_apply"
            )

            DrawerMenuItem(
                icon = Icons.Default.FormatListNumbered,
                label = if (currentLang == AppLanguage.SWAHILI) "Ratiba ya Marejesho Yangu" else "Mon Échéancier de Remboursement",
                isSelected = currentRoute == "client_schedule",
                onClick = { onNavigate("client_schedule") },
                testTag = "drawer_item_client_schedule"
            )

            DrawerMenuItem(
                icon = Icons.Default.Chat,
                label = "Chat",
                isSelected = currentRoute == "chat",
                onClick = { onNavigate("chat") },
                testTag = "drawer_item_chat"
            )

            DrawerMenuItem(
                icon = Icons.Default.Notifications,
                label = if (currentLang == AppLanguage.SWAHILI) "Taarifa Zangu" else "Mes Notifications",
                isSelected = currentRoute == "notifications",
                onClick = { onNavigate("notifications") },
                badgeContent = if (unreadNotificationsCount > 0) unreadNotificationsCount.toString() else null,
                testTag = "drawer_item_notifications"
            )
        } else {
            // ====================================================
            // MENUS POUR LES GESTIONNAIRES, AGENTS ET CAISSIERS
            // ====================================================
            if (currentUser?.role == UserRole.GESTIONNAIRE) {
                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    label = if (currentLang == AppLanguage.SWAHILI) "Mipangilio ya Mfumo" else "Paramètres",
                    isSelected = currentRoute == "settings",
                    onClick = onSettingsClick,
                    badgeContent = null,
                    tint = PrimaryNavy,
                    testTag = "drawer_item_settings"
                )
            }

            DrawerMenuItem(
                icon = Icons.Default.Payments,
                label = if (currentLang == AppLanguage.SWAHILI) "Kukusanya Rejesho (Recouvrement)" else "Recouvrement Rapide",
                isSelected = currentRoute == "record_payment",
                onClick = { onNavigate("record_payment") },
                badgeContent = "3.5%",
                tint = SuccessGreen,
                testTag = "drawer_item_record_payment"
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DrawerMenuItem(
                icon = Icons.Default.Dashboard,
                label = Localization.tr("nav_dashboard", currentLang),
                isSelected = currentRoute == "dashboard" || currentRoute == "agent_home",
                onClick = { onNavigate(if (currentUser?.role == UserRole.AGENT) "agent_home" else "dashboard") },
                testTag = "drawer_item_dashboard"
            )

            DrawerMenuItem(
                icon = Icons.Default.Group,
                label = if (currentLang == AppLanguage.SWAHILI) "Wateja & Usajili" else "Gestion des Clients",
                isSelected = currentRoute == "clients",
                onClick = { onNavigate("clients") },
                testTag = "drawer_item_clients"
            )

            DrawerMenuItem(
                icon = Icons.Default.AccountBalance,
                label = if (currentLang == AppLanguage.SWAHILI) "Mikopo & Mizunguko" else "Crédits & Cycles",
                isSelected = currentRoute == "cycles",
                onClick = { onNavigate("cycles") },
                testTag = "drawer_item_cycles"
            )

            DrawerMenuItem(
                icon = Icons.Default.NoteAdd,
                label = if (currentLang == AppLanguage.SWAHILI) "Ombi la Mkopo (MALAKI)" else "Demande de Crédit",
                isSelected = currentRoute == "client_loan_apply",
                onClick = { onNavigate("client_loan_apply") },
                testTag = "drawer_item_loan_apply"
            )

            DrawerMenuItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = Localization.tr("nav_caisse", currentLang),
                isSelected = currentRoute == "caisse" || currentRoute == "caisse_entree" || currentRoute == "caisse_sortie",
                onClick = { onNavigate("caisse") },
                testTag = "drawer_item_caisse"
            )

            if (currentUser?.role == UserRole.GESTIONNAIRE) {
                DrawerMenuItem(
                    icon = Icons.Default.ManageAccounts,
                    label = if (currentLang == AppLanguage.SWAHILI) "Wafanyakazi & Akaunti" else "Gestion des Utilisateurs",
                    isSelected = currentRoute == "user_management",
                    onClick = { onNavigate("user_management") },
                    testTag = "drawer_item_users"
                )
            }

            DrawerMenuItem(
                icon = Icons.Default.Assessment,
                label = Localization.tr("nav_reports", currentLang),
                isSelected = currentRoute == "reports",
                onClick = { onNavigate("reports") },
                testTag = "drawer_item_reports"
            )

            DrawerMenuItem(
                icon = Icons.Default.Chat,
                label = Localization.tr("nav_chat", currentLang),
                isSelected = currentRoute == "chat",
                onClick = { onNavigate("chat") },
                testTag = "drawer_item_chat"
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ====================================================
        // CONTRÔLES RAPIDES (MODE SOMBRE, CLOUD, VERROUILLAGE, DÉCONNEXION)
        // ====================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = if (isDarkMode) Color(0xFFFBBF24) else PrimaryNavy,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Mwonekano wa Usiku" else "Mode Sombre",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = isDarkMode,
                onCheckedChange = { onToggleDarkMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFBBF24),
                    checkedTrackColor = Color(0xFFFBBF24).copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("drawer_switch_dark_mode")
            )
        }

        DrawerMenuItem(
            icon = Icons.Default.Lock,
            label = if (currentLang == AppLanguage.SWAHILI) "Kufunga Programu (PIN)" else "Verrouiller l'application",
            onClick = onLockClick,
            testTag = "drawer_item_lock"
        )

        DrawerMenuItem(
            icon = Icons.Default.Logout,
            label = if (currentLang == AppLanguage.SWAHILI) "Toka kwenye Mfumo" else "Déconnexion",
            onClick = onLogout,
            tint = MaterialTheme.colorScheme.error,
            testTag = "drawer_item_logout"
        )

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    badgeContent: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = ""
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else tint
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.5.sp
            )
        },
        badge = {
            badgeContent?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .testTag(testTag)
    )
}
