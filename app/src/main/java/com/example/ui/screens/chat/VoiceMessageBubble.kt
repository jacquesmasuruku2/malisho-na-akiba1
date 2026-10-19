package com.example.ui.screens.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun VoiceMessageBubble(
    durationSeconds: Int,
    isPlaying: Boolean,
    playbackProgress: Float,
    playbackPositionSeconds: Int,
    isMe: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSeconds = durationSeconds.coerceAtLeast(1)
    val displayDurationStr = remember(totalSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
    val currentPositionStr = remember(playbackPositionSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", playbackPositionSeconds / 60, playbackPositionSeconds % 60)
    }

    val activeColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary
    val inactiveColor = if (isMe) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val playButtonBg = if (isMe) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("voice_message_bubble_content")
    ) {
        // Modern Play / Pause Circular Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(playButtonBg)
                .clickable(onClick = onTogglePlay)
                .testTag("voice_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Lecture du message vocal",
                tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Stylized Interactive Waveform Track
            AudioWaveformTrack(
                progress = playbackProgress,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Time and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPlaying || playbackPositionSeconds > 0) "$currentPositionStr / $displayDurationStr" else displayDurationStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Vocal",
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Draws audio waveform bars that react to playback progression.
 */
@Composable
private fun AudioWaveformTrack(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Deterministic pseudo-waveform heights
    val waveformBarHeights = remember {
        listOf(
            0.35f, 0.65f, 0.9f, 0.45f, 0.75f, 1.0f, 0.55f, 0.85f,
            0.4f, 0.7f, 0.95f, 0.5f, 0.8f, 0.6f, 0.85f, 0.45f,
            0.7f, 1.0f, 0.65f, 0.5f, 0.85f, 0.4f, 0.6f, 0.35f
        )
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val tappedFraction = (offset.x / size.width).coerceIn(0f, 1f)
                onSeek(tappedFraction)
            }
        }
    ) {
        val totalBars = waveformBarHeights.size
        val barWidth = 3.5.dp.toPx()
        val spacing = ((size.width - (totalBars * barWidth)) / (totalBars - 1)).coerceAtLeast(1.5.dp.toPx())
        val maxHeight = size.height

        for (i in 0 until totalBars) {
            val barFraction = (i.toFloat() / (totalBars - 1)).coerceIn(0f, 1f)
            val isPassed = barFraction <= progress
            val barHeight = (waveformBarHeights[i] * maxHeight).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = if (isPassed) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
