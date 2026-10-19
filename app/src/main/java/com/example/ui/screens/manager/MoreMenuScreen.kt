package com.example.ui.screens.manager

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentSkyBlue
import com.example.util.AppLanguage
import com.example.util.Localization

@Composable
fun MoreMenuScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = Localization.tr("more_title", currentLang),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = Localization.tr("more_subtitle", currentLang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            MoreMenuItem(
                title = Localization.tr("more_item_settings_title", currentLang),
                subtitle = Localization.tr("more_item_settings_desc", currentLang),
                icon = Icons.Default.Settings,
                onClick = { viewModel.navigateTo("settings") }
            )
        }

        item {
            MoreMenuItem(
                title = Localization.tr("more_item_apps_title", currentLang),
                subtitle = Localization.tr("more_item_apps_desc", currentLang),
                icon = Icons.Default.NoteAdd,
                onClick = { viewModel.navigateTo("applications") }
            )
        }

        item {
            MoreMenuItem(
                title = Localization.tr("more_item_users_title", currentLang),
                subtitle = Localization.tr("more_item_users_desc", currentLang),
                icon = Icons.Default.Group,
                onClick = { viewModel.navigateTo("user_management") }
            )
        }

        item {
            MoreMenuItem(
                title = if (currentLang == AppLanguage.SWAHILI) "Kadi za Wanachama PVC (Plastiki)" else "Cartes de Membre PVC (Plastique)",
                subtitle = if (currentLang == AppLanguage.SWAHILI) "Tengeneza na kuhariri kadi za plastiki za wateja na maajenti" else "Générer, éditer et imprimer les cartes plastiques pour clients et agents",
                icon = Icons.Default.CreditCard,
                onClick = { viewModel.navigateTo("member_cards") }
            )
        }

        item {
            MoreMenuItem(
                title = Localization.tr("more_item_proj_title", currentLang),
                subtitle = Localization.tr("more_item_proj_desc", currentLang),
                icon = Icons.Default.TrendingUp,
                onClick = { viewModel.navigateTo("projections") }
            )
        }

        item {
            MoreMenuItem(
                title = Localization.tr("more_item_audit_title", currentLang),
                subtitle = Localization.tr("more_item_audit_desc", currentLang),
                icon = Icons.Default.Security,
                onClick = { viewModel.navigateTo("audit") }
            )
        }

        item {
            MoreMenuItem(
                title = if (currentLang == AppLanguage.SWAHILI) "Kutuma Data Nje ya Mtandao" else "Synchronisation Hors-Ligne",
                subtitle = if (pendingSyncCount > 0) (if (currentLang == AppLanguage.SWAHILI) "Malipo $pendingSyncCount yanangoja kutumwa" else "$pendingSyncCount paiement(s) en attente de synchronisation") else (if (currentLang == AppLanguage.SWAHILI) "Malipo yote yametumwa vizuri" else "Tous les paiements sont synchronisés"),
                icon = Icons.Default.CloudSync,
                badgeText = if (pendingSyncCount > 0) "$pendingSyncCount" else null,
                onClick = { viewModel.syncOfflinePayments() }
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MALISHO NA AKIBA (MALAKI) • Système Mobile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Toleo la 1.0.0 • Mfumo wa kiteknolojia wa mikopo ya masoko, ulinzi mkali wa data na mahesabu halisi ya fedha za DRC." else "Version 1.0.0 • Architecture conforme au modèle opérationnel Excel avec base SQLite/Room chiffrée, RBAC strict et calculs centralisés.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version 1.0.0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSkyBlue
                        )
                        Text(
                            text = "CopyRight | Développé par Jacques M.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.logoutGoogle() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Déconnexion",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Kutoka mu Programu" else "Se Déconnecter de la Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CopyRight | Développé par Jacques M.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 1.0.0 • MALISHO NA AKIBA (MALAKI)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentSkyBlue)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(badgeText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
