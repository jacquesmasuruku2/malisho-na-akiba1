package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Styles de bulles de messages inspirés des options de personnalisation avancées de Textra
 */
enum class BubbleStyle(
    val id: String,
    val label: String,
    val description: String
) {
    TEXTRA_ROUNDED(
        id = "textra_rounded",
        label = "Bulles Arrondies (Classique)",
        description = "Coins doux et équilibrés avec petit accent inférieur"
    ),
    TEXTRA_PILL(
        id = "textra_pill",
        label = "Capsule Lisse & Moderne",
        description = "Courbures elliptiques modernes ultra-douces"
    ),
    TEXTRA_BALLOON(
        id = "textra_balloon",
        label = "Bulle Dialogue Équilibrée",
        description = "Forme avec encoche de lecture claire"
    ),
    TEXTRA_ANGULAR(
        id = "textra_angular",
        label = "Sobriété Angulaire",
        description = "Coins sobres et nets, style flat épuré"
    ),
    TEXTRA_CUT_CORNER(
        id = "textra_cut",
        label = "Format Biseauté Géométrique",
        description = "Coins coupés structurés, style contemporain"
    ),
    TEXTRA_ASYMMETRIC(
        id = "textra_asymmetric",
        label = "Courbure Asymétrique Épurée",
        description = "Style signature fluide et moderne"
    );

    /**
     * Retourne la forme Compose appropriée pour l'expéditeur ou le destinataire
     */
    fun resolveShape(isMe: Boolean): Shape {
        return when (this) {
            TEXTRA_ROUNDED -> RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            )
            TEXTRA_PILL -> RoundedCornerShape(22.dp)
            TEXTRA_BALLOON -> RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 18.dp
            )
            TEXTRA_ANGULAR -> RoundedCornerShape(
                topStart = 6.dp,
                topEnd = 6.dp,
                bottomStart = if (isMe) 6.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 6.dp
            )
            TEXTRA_CUT_CORNER -> CutCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 12.dp
            )
            TEXTRA_ASYMMETRIC -> RoundedCornerShape(
                topStart = if (isMe) 22.dp else 6.dp,
                topEnd = if (isMe) 6.dp else 22.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        }
    }
}
