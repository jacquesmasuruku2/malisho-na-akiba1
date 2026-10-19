package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Polices Google Fonts installées localement
 */
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
)

val PlayfairDisplayFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal)
)

val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal)
)

/**
 * Styles de police applicables à toute l'interface de l'application
 */
enum class AppFontStyle(
    val id: String,
    val label: String,
    val subtitle: String,
    val fontFamily: FontFamily
) {
    POPPINS(
        id = "poppins",
        label = "Poppins (Moderne & Géométrique)",
        subtitle = "Design moderne, chaleureux, ouvert et hautement lisible",
        fontFamily = PoppinsFontFamily
    ),
    DEFAULT(
        id = "default",
        label = "Standard Système (Roboto)",
        subtitle = "Lisibilité nette, standard Android équilibrée",
        fontFamily = FontFamily.Default
    ),
    PLAYFAIR(
        id = "playfair",
        label = "Playfair Display (Élégant Serif)",
        subtitle = "Style institutionnel distingué et prestigieux",
        fontFamily = PlayfairDisplayFontFamily
    ),
    ROBOTO_MONO(
        id = "roboto_mono",
        label = "Roboto Mono (Finance & Technique)",
        subtitle = "Idéal pour les chiffres, l'audit et la précision financière",
        fontFamily = RobotoMonoFontFamily
    ),
    SANS_SERIF(
        id = "sans_serif",
        label = "Moderne Sans-Serif",
        subtitle = "Design épuré, sobre et contemporain",
        fontFamily = FontFamily.SansSerif
    ),
    SERIF(
        id = "serif",
        label = "Classique Serif",
        subtitle = "Style typographique classique et formel",
        fontFamily = FontFamily.Serif
    )
}

/**
 * Construit un ensemble typographique Material 3 complet basé sur la famille de police choisie
 */
fun createAppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp
        )
    )
}
