package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.util.AppLanguage
import com.example.util.Localization

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    userRole: UserRole?,
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Fallback to GESTIONNAIRE so navigation menus are NEVER empty on refresh or load
    val resolvedRole = userRole ?: UserRole.GESTIONNAIRE

    val items = when (resolvedRole) {
        UserRole.GESTIONNAIRE -> listOf(
            NavItem("dashboard", Localization.tr("nav_dashboard", currentLanguage), Icons.Default.Dashboard, "nav_dashboard"),
            NavItem("clients", Localization.tr("nav_clients", currentLanguage), Icons.Default.Group, "nav_clients"),
            NavItem("cycles", Localization.tr("nav_cycles", currentLanguage), Icons.Default.AccountBalance, "nav_cycles"),
            NavItem("record_payment", if (currentLanguage == AppLanguage.SWAHILI) "Kukusanya" else "Recouvrement", Icons.Default.Payments, "nav_record_payment"),
            NavItem("caisse", Localization.tr("nav_caisse", currentLanguage), Icons.Default.AccountBalanceWallet, "nav_caisse")
        )
        UserRole.AGENT -> listOf(
            NavItem("agent_home", Localization.tr("nav_clients", currentLanguage), Icons.Default.Group, "nav_agent_home"),
            NavItem("cycles", Localization.tr("nav_cycles", currentLanguage), Icons.Default.AccountBalance, "nav_agent_cycles"),
            NavItem("record_payment", Localization.tr("nav_collect", currentLanguage), Icons.Default.Payments, "nav_record_payment"),
            NavItem("reports", Localization.tr("nav_reports", currentLanguage), Icons.Default.Assessment, "nav_reports"),
            NavItem("chat", Localization.tr("nav_chat", currentLanguage), Icons.Default.Chat, "nav_agent_chat")
        )
        UserRole.CAISSIER -> listOf(
            NavItem("caisse", Localization.tr("nav_caisse", currentLanguage), Icons.Default.AccountBalanceWallet, "nav_caisse"),
            NavItem("caisse_entree", if (currentLanguage == AppLanguage.SWAHILI) "Kuingiza" else "Entrée", Icons.Default.PriceCheck, "nav_caisse_entree"),
            NavItem("caisse_sortie", if (currentLanguage == AppLanguage.SWAHILI) "Kutoa" else "Sortie", Icons.Default.MonetizationOn, "nav_caisse_sortie"),
            NavItem("chat", Localization.tr("nav_chat", currentLanguage), Icons.Default.Chat, "nav_caissier_chat")
        )
        UserRole.CLIENT -> listOf(
            NavItem("client_portal", Localization.tr("nav_home", currentLanguage), Icons.Default.Home, "nav_client_portal"),
            NavItem("client_profile", if (currentLanguage == AppLanguage.SWAHILI) "Wasifu" else "Profil", Icons.Default.Person, "nav_client_profile"),
            NavItem("client_loan_apply", Localization.tr("nav_apply", currentLanguage), Icons.Default.NoteAdd, "nav_client_apply"),
            NavItem("client_schedule", Localization.tr("nav_schedule", currentLanguage), Icons.Default.FormatListNumbered, "nav_client_schedule"),
            NavItem("chat", Localization.tr("nav_advisor", currentLanguage), Icons.Default.Chat, "nav_client_chat")
        )
    }

    if (items.isNotEmpty()) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}
