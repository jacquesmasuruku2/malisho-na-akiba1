package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricAuthResult
import com.example.util.BiometricHardwareStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricPromptDialog(
    title: String = "Validation Biométrique",
    subtitle: String = "Posez votre doigt sur le capteur d'empreinte",
    operationSummary: String? = null,
    negativeButtonText: String = "Annuler",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hardwareStatus = remember { BiometricAuthHelper.checkBiometricStatus(context) }

    var isVerifying by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinFallback by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    // Pulse animation for fingerprint icon
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprintPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun executeSuccess() {
        isSuccess = true
        isVerifying = false
        coroutineScope.launch {
            delay(500)
            onSuccess()
        }
    }

    fun triggerNativeOrVirtualScan() {
        val activity = context as? Activity
        if (activity != null && hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
            isVerifying = true
            errorMessage = null
            BiometricAuthHelper.authenticateWithBiometrics(
                activity = activity,
                title = title,
                subtitle = subtitle,
                description = operationSummary ?: "Validation par empreinte de l'agent MALAKI",
                negativeButtonText = negativeButtonText
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        executeSuccess()
                    }
                    is BiometricAuthResult.Error -> {
                        isVerifying = false
                        errorMessage = result.message
                    }
                    is BiometricAuthResult.Failed -> {
                        isVerifying = false
                        errorMessage = "Empreinte non reconnue. Veuillez réessayer ou entrer le code PIN."
                    }
                    is BiometricAuthResult.Cancelled -> {
                        isVerifying = false
                    }
                }
            }
        } else {
            // Emulation / Simulator mode: simulates instant biometric touch
            isVerifying = true
            errorMessage = null
            coroutineScope.launch {
                delay(400)
                executeSuccess()
            }
        }
    }

    // Auto-trigger native prompt if hardware is available
    LaunchedEffect(Unit) {
        if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
            triggerNativeOrVirtualScan()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isVerifying && !isSuccess) onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sécurité MALISHO NA AKIBA",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (operationSummary != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = operationSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!showPinFallback) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Fingerprint Touch Target
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(if (isVerifying) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isSuccess) SuccessGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable(enabled = !isVerifying && !isSuccess) {
                                triggerNativeOrVirtualScan()
                            }
                            .testTag("biometric_touch_sensor"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSuccess) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Validé",
                                tint = SuccessGreen,
                                modifier = Modifier.size(50.dp)
                            )
                        } else if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(46.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Capteur d'empreinte",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (hardwareStatus) {
                            BiometricHardwareStatus.AVAILABLE -> SuccessGreen.copy(alpha = 0.12f)
                            else -> AccentSkyBlue.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = when (hardwareStatus) {
                                BiometricHardwareStatus.AVAILABLE -> "● Capteur d'empreinte matériel actif"
                                else -> "● Capteur interactif (Appuyez sur l'icône)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (hardwareStatus) {
                                BiometricHardwareStatus.AVAILABLE -> SuccessGreen
                                else -> PrimaryNavy
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Virtual Scan Button (Very convenient for emulator / test)
                    Button(
                        onClick = { triggerNativeOrVirtualScan() },
                        enabled = !isVerifying && !isSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_validate_biometrics")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Toucher pour valider avec l'empreinte", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN Code Toggle Button
                    TextButton(
                        onClick = {
                            showPinFallback = true
                            errorMessage = null
                        }
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Utiliser le Code PIN de secours", fontSize = 12.sp)
                    }
                } else {
                    // PIN CODE FALLBACK UI
                    Text(
                        text = "Entrez votre code secret PIN de validation :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6) pinInput = it
                            errorMessage = null
                        },
                        label = { Text("Code PIN (ex: 1234)") },
                        placeholder = { Text("1234") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_biometric_pin")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (pinInput.isBlank()) {
                                errorMessage = "Veuillez entrer votre code PIN."
                                return@Button
                            }
                            if (BiometricAuthHelper.verifyPin(context, pinInput)) {
                                executeSuccess()
                            } else {
                                errorMessage = "Code PIN incorrect (par défaut: 1234)."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_submit_pin")
                    ) {
                        Text("Valider le code PIN", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { showPinFallback = false }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retour au capteur d'empreinte", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying && !isSuccess
            ) {
                Text(negativeButtonText)
            }
        }
    )
}
