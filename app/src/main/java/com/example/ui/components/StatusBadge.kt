package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashMovementType
import com.example.data.model.CycleStatus
import com.example.data.model.LoanStatus
import com.example.data.model.UserRole
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.Localization

@Composable
fun StatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    dotColor: Color? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LoanStatusBadge(
    status: LoanStatus,
    lang: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val label = Localization.statusLabel(status.name, lang)
    val (bg, text, dot) = when (status) {
        LoanStatus.BROUILLON -> Triple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            Color(0xFF94A3B8)
        )
        LoanStatus.SOUMISE -> Triple(
            if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            Color(0xFF0284C7)
        )
        LoanStatus.EN_ETUDE -> Triple(
            if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            Color(0xFFD97706)
        )
        LoanStatus.APPROUVEE -> Triple(
            if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Color(0xFF16A34A)
        )
        LoanStatus.REFUSEE -> Triple(
            if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Color(0xFFDC2626)
        )
        LoanStatus.DECAISSEE, LoanStatus.EN_COURS -> Triple(
            if (isDark) Color(0xFF172554) else Color(0xFFDBEAFE),
            if (isDark) Color(0xFF60A5FA) else Color(0xFF1E40AF),
            Color(0xFF2563EB)
        )
        LoanStatus.TERMINEE -> Triple(
            if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Color(0xFF16A34A)
        )
    }
    StatusBadge(
        text = label,
        backgroundColor = bg,
        textColor = text,
        dotColor = dot,
        modifier = modifier
    )
}

@Composable
fun CycleStatusBadge(
    status: CycleStatus,
    lang: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val label = Localization.statusLabel(status.name, lang)
    val (bg, text, dot) = when (status) {
        CycleStatus.PREVU -> Triple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            Color(0xFF94A3B8)
        )
        CycleStatus.APPROUVE, CycleStatus.EN_COURS, CycleStatus.DECAISSE -> Triple(
            if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            Color(0xFF0284C7)
        )
        CycleStatus.EN_RETARD, CycleStatus.EN_DEFAUT -> Triple(
            if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Color(0xFFDC2626)
        )
        CycleStatus.TERMINE -> Triple(
            if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Color(0xFF16A34A)
        )
        CycleStatus.ANNULE -> Triple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
            Color(0xFF64748B)
        )
    }
    StatusBadge(
        text = label,
        backgroundColor = bg,
        textColor = text,
        dotColor = dot,
        modifier = modifier
    )
}

@Composable
fun ClientStatusBadge(
    status: String,
    lang: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val normalized = status.trim().uppercase()
    val label = Localization.statusLabel(status, lang)
    val (bg, text, dot) = when {
        normalized.contains("RETARD") -> Triple(
            if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Color(0xFFDC2626)
        )
        normalized.contains("ACTIF") -> Triple(
            if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Color(0xFF16A34A)
        )
        else -> Triple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            Color(0xFF94A3B8)
        )
    }
    StatusBadge(
        text = label,
        backgroundColor = bg,
        textColor = text,
        dotColor = dot,
        modifier = modifier
    )
}

@Composable
fun UserRoleBadge(
    role: UserRole,
    lang: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val label = Localization.roleLabel(role, lang)
    val (bg, text) = when (role) {
        UserRole.GESTIONNAIRE -> if (isDark) Color(0xFF172554) to Color(0xFF60A5FA) else Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        UserRole.AGENT -> if (isDark) Color(0xFF0C4A6E) to Color(0xFF38BDF8) else Color(0xFFE0F2FE) to Color(0xFF0284C7)
        UserRole.CAISSIER -> if (isDark) Color(0xFF451A03) to Color(0xFFFBBF24) else Color(0xFFFEF3C7) to Color(0xFFD97706)
        UserRole.CLIENT -> if (isDark) Color(0xFF052E16) to Color(0xFF4ADE80) else Color(0xFFDCFCE7) to Color(0xFF16A34A)
    }
    StatusBadge(text = label, backgroundColor = bg, textColor = text, modifier = modifier)
}

@Composable
fun CashMovementBadge(
    type: CashMovementType,
    lang: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val label = when (type) {
        CashMovementType.ENTREE -> if (lang == AppLanguage.SWAHILI) "Kuingiza" else "Entrée"
        CashMovementType.SORTIE -> if (lang == AppLanguage.SWAHILI) "Kutoa" else "Sortie"
    }
    val (bg, text, dot) = when (type) {
        CashMovementType.ENTREE -> Triple(
            if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Color(0xFF16A34A)
        )
        CashMovementType.SORTIE -> Triple(
            if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Color(0xFFDC2626)
        )
    }
    StatusBadge(text = label, backgroundColor = bg, textColor = text, dotColor = dot, modifier = modifier)
}
