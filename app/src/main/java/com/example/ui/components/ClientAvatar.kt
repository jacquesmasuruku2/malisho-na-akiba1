package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.ErrorRed
import com.example.util.AppLanguage

/**
 * Returns true if gender string corresponds to Female/Femme.
 */
fun isFemaleGender(gender: String?): Boolean {
    if (gender == null) return false
    val g = gender.trim().lowercase()
    return g.startsWith("f") || g.contains("femme") || g.contains("mwanamke")
}

/**
 * Highly polished client profile avatar customized specifically for Men and Women.
 * Never uses generic dull placeholders. Automatically displays the appropriate male
 * or female stylized merchant portrait with status ring and gender indicator.
 */
@Composable
fun ClientAvatar(
    gender: String,
    photoUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    isLate: Boolean = false,
    showGenderBadge: Boolean = true,
    contentDescription: String? = null
) {
    val isWoman = isFemaleGender(gender)

    // Palette per gender
    val malePrimary = Color(0xFF0284C7)
    val maleBg = Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD)))
    val femalePrimary = Color(0xFFDB2777)
    val femaleBg = Brush.linearGradient(listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8)))

    val activePrimary = if (isWoman) femalePrimary else malePrimary
    val activeBg = if (isWoman) femaleBg else maleBg
    val defaultDrawableRes = if (isWoman) R.drawable.img_bybit_avatar_female else R.drawable.img_bybit_avatar_male

    val borderColor = when {
        isLate -> ErrorRed
        isWoman -> Color(0xFFF472B6)
        else -> Color(0xFF38BDF8)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFF6F8FA))
                .border(
                    width = if (isLate) 2.dp else 1.2.dp,
                    color = borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription ?: if (isWoman) "Photo Client Femme" else "Photo Client Homme",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GithubIdenticon(
                    seed = contentDescription ?: if (isWoman) "client_female_$gender" else "client_male_$gender",
                    primaryColor = activePrimary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Gender Micro-Badge in the lower corner (for sizes >= 32.dp)
        if (showGenderBadge && size >= 32.dp) {
            val badgeSize = (size * 0.36f).coerceIn(14.dp, 22.dp)
            val badgeIconSize = (badgeSize * 0.7f).coerceIn(10.dp, 16.dp)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(if (isWoman) Color(0xFFBE185D) else Color(0xFF0369A1))
                    .border(1.2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isWoman) Icons.Default.Female else Icons.Default.Male,
                    contentDescription = if (isWoman) "Femme" else "Homme",
                    tint = Color.White,
                    modifier = Modifier.size(badgeIconSize)
                )
            }
        }
    }
}

/**
 * Clean, modern badge that displays "Client" (never "Emprunteur")
 * along with the client's gender.
 */
@Composable
fun ClientRoleGenderBadge(
    gender: String,
    lang: AppLanguage = AppLanguage.FRENCH,
    modifier: Modifier = Modifier
) {
    val isWoman = isFemaleGender(gender)
    val genderLabel = if (lang == AppLanguage.SWAHILI) {
        if (isWoman) "Mwanamke" else "Mwanaume"
    } else {
        if (isWoman) "Femme" else "Homme"
    }

    val roleLabel = if (lang == AppLanguage.SWAHILI) "Mteja" else "Client"

    val badgeBg = if (isWoman) Color(0xFFFDF2F8) else Color(0xFFF0F9FF)
    val badgeContentColor = if (isWoman) Color(0xFFBE185D) else Color(0xFF0369A1)
    val badgeBorder = if (isWoman) Color(0xFFFBCFE8) else Color(0xFFBAE6FD)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isWoman) Icons.Default.Female else Icons.Default.Male,
                contentDescription = null,
                tint = badgeContentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$roleLabel • $genderLabel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeContentColor
            )
        }
    }
}
