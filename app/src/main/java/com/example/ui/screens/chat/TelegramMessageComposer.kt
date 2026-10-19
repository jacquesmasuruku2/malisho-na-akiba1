package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.util.Locale

@Composable
fun TelegramMessageComposer(
    messageText: String,
    onMessageChange: (String) -> Unit,
    pendingAttachment: PendingAttachment?,
    onRemoveAttachment: () -> Unit,
    isRecordingAudio: Boolean,
    recordingTimerSeconds: Int,
    recordingAmplitudes: List<Float>,
    onOpenAttachmentPicker: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onSendVoiceRecording: () -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureMessageComposer(
        messageText = messageText,
        onMessageChange = onMessageChange,
        pendingAttachment = pendingAttachment,
        onRemoveAttachment = onRemoveAttachment,
        isRecordingAudio = isRecordingAudio,
        recordingTimerSeconds = recordingTimerSeconds,
        recordingAmplitudes = recordingAmplitudes,
        onOpenAttachmentPicker = onOpenAttachmentPicker,
        onStartVoiceRecording = onStartVoiceRecording,
        onCancelVoiceRecording = onCancelVoiceRecording,
        onSendVoiceRecording = onSendVoiceRecording,
        onSendMessage = onSendMessage,
        modifier = modifier
    )
}

@Composable
fun SecureMessageComposer(
    messageText: String,
    onMessageChange: (String) -> Unit,
    pendingAttachment: PendingAttachment?,
    onRemoveAttachment: () -> Unit,
    isRecordingAudio: Boolean,
    recordingTimerSeconds: Int,
    recordingAmplitudes: List<Float>,
    onOpenAttachmentPicker: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onSendVoiceRecording: () -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // --- ATTACHMENT PREVIEW BEFORE SEND ---
            AnimatedVisibility(
                visible = pendingAttachment != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (pendingAttachment != null) {
                    AttachmentPreviewCard(
                        attachment = pendingAttachment,
                        onRemove = onRemoveAttachment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }
            }

            if (isRecordingAudio) {
                // --- ACTIVE VOICE RECORDING BAR ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Cancel Recording Button [🗑️ Annuler]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onCancelVoiceRecording)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("composer_cancel_voice")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Annuler l'enregistrement",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Annuler",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 2. Pulsing Recording Dot & Live Waveform & Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        val pulseTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "red_dot_pulse"
                        )

                        // Red recording dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Timer text (00:08)
                        val timerStr = remember(recordingTimerSeconds) {
                            String.format(Locale.getDefault(), "%02d:%02d", recordingTimerSeconds / 60, recordingTimerSeconds % 60)
                        }
                        Text(
                            text = timerStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Live audio waveform amplitude visualization
                        LiveWaveformIndicator(
                            amplitudes = recordingAmplitudes,
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        )
                    }

                    // 3. Send Voice Recording Button [➤ Envoyer]
                    IconButton(
                        onClick = onSendVoiceRecording,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer le message vocal",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // --- COMPOSITION BAR ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Integrated Attachment Button [📎]
                    IconButton(
                        onClick = onOpenAttachmentPicker,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (pendingAttachment != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("chat_attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Joindre un fichier",
                            tint = if (pendingAttachment != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Text Input Field in the center
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 110.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Écrire un message...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        BasicTextField(
                            value = messageText,
                            onValueChange = onMessageChange,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSendTextOrAttachment = messageText.isNotBlank() || pendingAttachment != null

                    if (canSendTextOrAttachment) {
                        // SEND BUTTON [➤] when text is typed or attachment is ready
                        IconButton(
                            onClick = onSendMessage,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // MICROPHONE BUTTON [🎙️] when field is empty
                        IconButton(
                            onClick = onStartVoiceRecording,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("chat_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Enregistrer un message vocal",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview card showing the selected attachment before sending.
 */
@Composable
private fun AttachmentPreviewCard(
    attachment: PendingAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or Icon
        when (attachment.type) {
            AttachmentType.PHOTO -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                ) {
                    val model = if (attachment.localFilePath.isNotEmpty()) File(attachment.localFilePath) else attachment.uri
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Aperçu de l'image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }

            AttachmentType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFAB47BC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AttachmentType.DOCUMENT -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFA726)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AttachmentType.LOCATION -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF26A69A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name and size
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${attachment.type.label} • ${attachment.fileSizeFormatted}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Cancel / Discard attachment button [✕]
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(32.dp)
                .testTag("remove_attachment_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Retirer la pièce jointe",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Animated live waveform for voice recording.
 */
@Composable
private fun LiveWaveformIndicator(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val totalBars = 16
        val barWidth = 3.dp.toPx()
        val spacing = ((size.width - (totalBars * barWidth)) / (totalBars - 1)).coerceAtLeast(1.5.dp.toPx())
        val maxHeight = size.height

        val amps = if (amplitudes.isEmpty()) List(totalBars) { 0.2f } else amplitudes.takeLast(totalBars)

        for (i in 0 until totalBars) {
            val amp = if (i < amps.size) amps[i] else 0.2f
            val barHeight = (amp * maxHeight).coerceIn(3.dp.toPx(), maxHeight)
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}
