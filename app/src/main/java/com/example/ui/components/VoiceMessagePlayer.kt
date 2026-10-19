package com.example.ui.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Composant réutilisable pour la lecture de messages vocaux et fichiers audio,
 * spécialement optimisé pour les URL distantes (Cloudinary) et fichiers locaux.
 *
 * Supporte :
 * - Mode autonome (gère son propre MediaPlayer interne si [onTogglePlay] n'est pas fourni)
 * - Mode contrôlé (délègue à un manager audio externe comme [AudioVoiceHelper])
 * - Barre de progression standard fluide et interactive (Seekable)
 * - Indicateur de mise en mémoire tampon / chargement depuis Cloudinary
 * - État Lecture / Pause / Fin de lecture avec reprise
 */
@Composable
fun VoiceMessagePlayer(
    audioUrl: String?,
    modifier: Modifier = Modifier,
    durationSeconds: Int = 0,
    isMe: Boolean = false,
    // Contrôle externe optionnel
    isPlaying: Boolean? = null,
    progress: Float? = null,
    positionSeconds: Int? = null,
    onTogglePlay: (() -> Unit)? = null,
    onSeek: ((Float) -> Unit)? = null
) {
    // Si contrôlé de l'extérieur, on respecte les valeurs injectées
    val isExternallyControlled = isPlaying != null && onTogglePlay != null

    if (isExternallyControlled) {
        VoiceMessagePlayerContent(
            durationSeconds = durationSeconds,
            isPlaying = isPlaying ?: false,
            isLoading = false,
            hasError = false,
            progress = progress ?: 0f,
            currentPositionSeconds = positionSeconds ?: 0,
            isMe = isMe,
            isCloudinary = audioUrl?.contains("cloudinary", ignoreCase = true) == true,
            onTogglePlay = { onTogglePlay?.invoke() },
            onSeek = { onSeek?.invoke(it) },
            modifier = modifier
        )
    } else {
        // Mode autonome avec MediaPlayer interne
        InternalVoiceMessagePlayer(
            audioUrl = audioUrl,
            fallbackDurationSeconds = durationSeconds,
            isMe = isMe,
            modifier = modifier
        )
    }
}

@Composable
private fun InternalVoiceMessagePlayer(
    audioUrl: String?,
    fallbackDurationSeconds: Int,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentPositionSeconds by remember { mutableIntStateOf(0) }
    var totalDurationSeconds by remember(fallbackDurationSeconds) {
        mutableIntStateOf(fallbackDurationSeconds.coerceAtLeast(0))
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // Nettoyage lors de la destruction du composable
    DisposableEffect(audioUrl) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.w("VoiceMessagePlayer", "Error releasing MediaPlayer: ${e.message}")
            }
            mediaPlayer = null
            isPlaying = false
            isLoading = false
        }
    }

    // Boucle de mise à jour de la progression
    LaunchedEffect(isPlaying, isUserSeeking) {
        while (isPlaying && !isUserSeeking) {
            mediaPlayer?.let { player ->
                try {
                    val currentMs = player.currentPosition
                    val durationMs = player.duration
                    if (durationMs > 0) {
                        progress = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        currentPositionSeconds = currentMs / 1000
                    }
                } catch (e: Exception) {
                    Log.w("VoiceMessagePlayer", "Error updating progress: ${e.message}")
                }
            }
            delay(100)
        }
    }

    fun startPlaying() {
        if (audioUrl.isNullOrEmpty()) {
            hasError = true
            return
        }

        hasError = false

        val existingPlayer = mediaPlayer
        if (existingPlayer != null) {
            try {
                existingPlayer.start()
                isPlaying = true
                return
            } catch (e: Exception) {
                Log.w("VoiceMessagePlayer", "Resume error: ${e.message}")
            }
        }

        isLoading = true
        try {
            val player = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    isLoading = false
                    val durationMs = mp.duration
                    if (durationMs > 0) {
                        totalDurationSeconds = durationMs / 1000
                    }
                    mp.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    isPlaying = false
                    progress = 0f
                    currentPositionSeconds = 0
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("VoiceMessagePlayer", "MediaPlayer error: what=$what extra=$extra url=$audioUrl")
                    isLoading = false
                    isPlaying = false
                    hasError = true
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("VoiceMessagePlayer", "Failed to initialize player: ${e.message}")
            isLoading = false
            hasError = true
        }
    }

    fun pausePlaying() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
        } catch (e: Exception) {
            Log.w("VoiceMessagePlayer", "Pause error: ${e.message}")
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            pausePlaying()
        } else {
            startPlaying()
        }
    }

    fun seekTo(fraction: Float) {
        val player = mediaPlayer ?: return
        try {
            val durationMs = player.duration
            if (durationMs > 0) {
                val targetMs = (fraction * durationMs).toInt().coerceIn(0, durationMs)
                player.seekTo(targetMs)
                progress = fraction.coerceIn(0f, 1f)
                currentPositionSeconds = targetMs / 1000
            }
        } catch (e: Exception) {
            Log.w("VoiceMessagePlayer", "Seek error: ${e.message}")
        }
    }

    VoiceMessagePlayerContent(
        durationSeconds = totalDurationSeconds,
        isPlaying = isPlaying,
        isLoading = isLoading,
        hasError = hasError,
        progress = progress,
        currentPositionSeconds = currentPositionSeconds,
        isMe = isMe,
        isCloudinary = audioUrl?.contains("cloudinary", ignoreCase = true) == true,
        onTogglePlay = { togglePlay() },
        onSeek = { fraction ->
            isUserSeeking = true
            seekTo(fraction)
            isUserSeeking = false
        },
        modifier = modifier
    )
}

/**
 * Interface graphique avec barre de progression standard M3 et contrôles audio.
 */
@Composable
private fun VoiceMessagePlayerContent(
    durationSeconds: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    hasError: Boolean,
    progress: Float,
    currentPositionSeconds: Int,
    isMe: Boolean,
    isCloudinary: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSec = durationSeconds.coerceAtLeast(1)
    val displayDurationStr = remember(totalSec) {
        String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)
    }
    val currentPositionStr = remember(currentPositionSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", currentPositionSeconds / 60, currentPositionSeconds % 60)
    }

    val primaryTextColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isMe) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val buttonBackground = if (isMe) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
    val buttonIconColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("voice_message_player")
    ) {
        // Bouton interactif Lecture / Pause / Chargement / Erreur
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(buttonBackground)
                .clickable(enabled = !isLoading, onClick = onTogglePlay)
                .testTag("voice_player_play_pause_btn"),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = buttonIconColor
                    )
                }
                hasError -> {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Réessayer la lecture",
                        tint = buttonIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                isPlaying -> {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Mettre en pause",
                        tint = buttonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lancer la lecture",
                        tint = buttonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Barre de progression standard avec curseur ajustable
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("voice_player_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (isMe) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Informations de durée et indicateur Cloudinary / Vocal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPlaying || currentPositionSeconds > 0) "$currentPositionStr / $displayDurationStr" else displayDurationStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor,
                    modifier = Modifier.testTag("voice_player_duration_text")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCloudinary) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Stocké sur Cloudinary",
                            tint = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Cloud",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Vocal",
                            fontSize = 10.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}
