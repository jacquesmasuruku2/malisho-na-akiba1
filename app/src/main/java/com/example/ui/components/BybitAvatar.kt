package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.theme.SuccessGreen
import java.security.MessageDigest

/**
 * Deterministically generates a 5x5 symmetrical pixel grid (GitHub Identicon)
 * and primary color based on the seed string.
 */
fun generateGithubIdenticon(seed: String): Pair<Array<BooleanArray>, Color> {
    val cleanSeed = seed.trim().ifEmpty { "default_identicon" }
    val md5 = try {
        MessageDigest.getInstance("MD5").digest(cleanSeed.toByteArray(Charsets.UTF_8))
    } catch (e: Exception) {
        val h = cleanSeed.hashCode()
        ByteArray(16) { i -> ((h shr (i * 2)) and 0xFF).toByte() }
    }

    val palette = listOf(
        Color(0xFF0969DA), // GitHub Blue
        Color(0xFF1F883D), // GitHub Green
        Color(0xFF8250DF), // GitHub Purple
        Color(0xFFD97706), // GitHub Amber / Orange
        Color(0xFFCF222E), // GitHub Red
        Color(0xFF0E7490), // GitHub Cyan
        Color(0xFFBE185D), // GitHub Magenta / Rose
        Color(0xFF4F46E5), // GitHub Indigo
        Color(0xFF059669)  // GitHub Teal
    )
    val colorIdx = (md5[0].toInt() and 0xFF) % palette.size
    val color = palette[colorIdx]

    // 5x5 symmetrical grid: col 0 = col 4, col 1 = col 3, col 2 = center
    val grid = Array(5) { BooleanArray(5) }
    var byteIdx = 1
    for (row in 0 until 5) {
        for (col in 0..2) {
            val b = md5[byteIdx % md5.size].toInt() and 0xFF
            val isFilled = ((b shr (col + 1)) and 1) == 1 || (b % 3 == 0)
            grid[row][col] = isFilled
            grid[row][4 - col] = isFilled
            byteIdx++
        }
    }
    return Pair(grid, color)
}

/**
 * Authentic, minimalist GitHub Identicon composable.
 * Renders a clean 5x5 symmetrical pixel tile pattern.
 */
@Composable
fun GithubIdenticon(
    seed: String,
    primaryColor: Color? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF6F8FA)
) {
    val (grid, derivedColor) = remember(seed) {
        generateGithubIdenticon(seed)
    }
    val activeColor = primaryColor ?: derivedColor

    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(1.dp, activeColor.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            val cellWidth = size.width / 5f
            val cellHeight = size.height / 5f
            for (row in 0 until 5) {
                for (col in 0 until 5) {
                    if (grid[row][col]) {
                        drawRect(
                            color = activeColor,
                            topLeft = Offset(col * cellWidth, row * cellHeight),
                            size = Size(cellWidth + 0.5f, cellHeight + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Avatar styles metadata for simple GitHub identicons.
 */
enum class BybitAvatarType(
    val key: String,
    val title: String,
    val subtitle: String,
    val seed: String,
    val primaryColor: Color
) {
    VIP_DIRECTOR(
        key = "bybit_vip",
        title = "Direction Générale",
        subtitle = "Identicon GitHub Bleu Océan",
        seed = "direction_generale_vip",
        primaryColor = Color(0xFF0969DA)
    ),
    TECH_AGENT(
        key = "bybit_agent",
        title = "Agent de Crédit",
        subtitle = "Identicon GitHub Vert Émeraude",
        seed = "agent_credit_terrain",
        primaryColor = Color(0xFF1F883D)
    ),
    MALE_TRADER(
        key = "bybit_male",
        title = "Client Commerçant",
        subtitle = "Identicon GitHub Cyan Céleste",
        seed = "client_homme_kituku",
        primaryColor = Color(0xFF0E7490)
    ),
    FEMALE_TRADER(
        key = "bybit_female",
        title = "Cliente Commerçante",
        subtitle = "Identicon GitHub Rose Fuchsia",
        seed = "cliente_femme_virunga",
        primaryColor = Color(0xFFBE185D)
    ),
    PURPLE_LEADER(
        key = "identicon_purple",
        title = "Direction Adjointe",
        subtitle = "Identicon GitHub Violet Royal",
        seed = "direction_adjointe_purple",
        primaryColor = Color(0xFF8250DF)
    ),
    AMBER_TREASURY(
        key = "identicon_amber",
        title = "Responsable Caisse",
        subtitle = "Identicon GitHub Ambre Doré",
        seed = "caisse_centrale_amber",
        primaryColor = Color(0xFFD97706)
    );

    companion object {
        fun fromKey(key: String?): BybitAvatarType? {
            if (key.isNullOrBlank()) return null
            return entries.find { it.key.equals(key.trim(), ignoreCase = true) }
        }

        fun resolve(avatarKey: String?, role: UserRole?, gender: String?): BybitAvatarType {
            val fromKey = fromKey(avatarKey)
            if (fromKey != null) return fromKey

            return when (role) {
                UserRole.GESTIONNAIRE -> VIP_DIRECTOR
                UserRole.AGENT -> TECH_AGENT
                UserRole.CAISSIER -> AMBER_TREASURY
                UserRole.CLIENT -> {
                    if (isFemaleGender(gender)) FEMALE_TRADER else MALE_TRADER
                }
                null -> {
                    if (isFemaleGender(gender)) FEMALE_TRADER else MALE_TRADER
                }
            }
        }
    }
}

/**
 * Clean, modern profile avatar using GitHub default Identicons.
 * Simple, minimalist, fast, and elegant.
 */
@Composable
fun BybitAvatar(
    avatarKey: String? = null,
    role: UserRole? = null,
    gender: String? = null,
    photoUrl: String? = null,
    isOnline: Boolean? = null,
    size: Dp = 48.dp,
    showGlow: Boolean = false,
    showPresenceDot: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val avatarType = BybitAvatarType.resolve(avatarKey, role, gender)
    val effectiveSeed = avatarKey ?: role?.name ?: gender ?: "user_default"

    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle / Rounded
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFF6F8FA))
                .border(
                    width = 1.2.dp,
                    color = avatarType.primaryColor.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val effectiveUrl = if (!photoUrl.isNullOrBlank()) {
                photoUrl
            } else if (!avatarKey.isNullOrBlank() && (avatarKey.startsWith("http://") || avatarKey.startsWith("https://") || avatarKey.startsWith("content://") || avatarKey.startsWith("file://"))) {
                avatarKey
            } else null

            if (!effectiveUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(effectiveUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = avatarType.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GithubIdenticon(
                    seed = effectiveSeed,
                    primaryColor = avatarType.primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Presence Status Beacon Indicator
        if (showPresenceDot && isOnline != null) {
            val dotSize = (size * 0.28f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isOnline) SuccessGreen else Color(0xFF94A3B8))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Compact clean badge attached to user cards
 */
@Composable
fun BybitRoleBadge(
    role: UserRole,
    gender: String? = null,
    modifier: Modifier = Modifier
) {
    val isWoman = isFemaleGender(gender)

    val (bgGradient, borderColor, label, icon) = when (role) {
        UserRole.GESTIONNAIRE -> Quadruple(
            Color(0xFF0969DA).copy(alpha = 0.12f),
            Color(0xFF0969DA),
            "DIRECTION",
            Icons.Default.WorkspacePremium
        )
        UserRole.AGENT -> Quadruple(
            Color(0xFF1F883D).copy(alpha = 0.12f),
            Color(0xFF1F883D),
            "AGENT DE CRÉDIT",
            Icons.Default.Star
        )
        UserRole.CAISSIER -> Quadruple(
            Color(0xFFD97706).copy(alpha = 0.12f),
            Color(0xFFD97706),
            "CAISSE CENTRALE",
            Icons.Default.Star
        )
        UserRole.CLIENT -> Quadruple(
            if (isWoman) Color(0xFFBE185D).copy(alpha = 0.12f) else Color(0xFF0E7490).copy(alpha = 0.12f),
            if (isWoman) Color(0xFFBE185D) else Color(0xFF0E7490),
            if (isWoman) "CLIENT • FEMME" else "CLIENT • HOMME",
            if (isWoman) Icons.Default.Female else Icons.Default.Male
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        color = bgGradient,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                letterSpacing = 0.4.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Interactive Dialog to pick a clean GitHub Identicon Avatar
 */
@Composable
fun BybitAvatarPickerDialog(
    currentKey: String?,
    role: UserRole?,
    gender: String?,
    onAvatarSelected: (String) -> Unit,
    onPhotoSelectedFromGallery: ((Uri) -> Unit)? = null,
    isUploadingPhoto: Boolean = false,
    onDismiss: () -> Unit
) {
    val activeType = BybitAvatarType.resolve(currentKey, role, gender)
    val isCustomCloudinaryPhoto = currentKey?.startsWith("http://") == true || currentKey?.startsWith("https://") == true

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoSelectedFromGallery?.invoke(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Photo de Profil & Avatar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choisissez une photo sur votre téléphone ou un identicon",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // SECTION 1 : CHOISIR UNE PHOTO SUR LE TÉLÉPHONE (CLOUDINARY)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Photo personnelle du téléphone",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Téléversée et sauvegardée sur Cloudinary",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isCustomCloudinaryPhoto) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, SuccessGreen, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(currentKey)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Photo actuelle",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "✓ Photo Cloudinary active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Votre photo est synchronisée",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isUploadingPhoto,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Envoi vers Cloudinary...", fontSize = 12.5.sp, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCustomCloudinaryPhoto) "Remplacer ma photo (Galerie)" else "Choisir une photo sur mon téléphone",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ou sélectionner un identicon :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // SECTION 2 : IDENTICONS DISPONIBLES
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(BybitAvatarType.entries) { item ->
                        val isSelected = !isCustomCloudinaryPhoto && item == activeType
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    item.primaryColor.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) item.primaryColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAvatarSelected(item.key) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    GithubIdenticon(
                                        seed = item.seed,
                                        primaryColor = item.primaryColor,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, item.primaryColor, CircleShape)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sélectionné",
                                            tint = item.primaryColor,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        }
    )
}
