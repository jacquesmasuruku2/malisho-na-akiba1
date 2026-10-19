package com.example.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.AppLanguage
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricAuthResult
import com.example.util.BiometricHardwareStatus
import com.example.util.Localization
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val hardwareStatus = remember { BiometricAuthHelper.checkBiometricStatus(context) }

    var isVerifying by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinMode by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "lockPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lockPulseScale"
    )

    fun executeUnlock() {
        isSuccess = true
        isVerifying = false
        coroutineScope.launch {
            delay(400)
            viewModel.unlockApp()
        }
    }

    fun triggerBiometrics() {
        val activity = context as? Activity
        if (activity != null && hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
            isVerifying = true
            errorMessage = null
            BiometricAuthHelper.authenticateWithBiometrics(
                activity = activity,
                title = if (currentLang == AppLanguage.SWAHILI) "Kufungua MALAKI" else "Déverrouillage MALAKI",
                subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako kwenye kitambuzi" else "Posez votre doigt sur le capteur d'empreinte",
                description = "Authentification de sécurité requise"
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        executeUnlock()
                    }
                    is BiometricAuthResult.Error -> {
                        isVerifying = false
                        errorMessage = result.message
                    }
                    is BiometricAuthResult.Failed -> {
                        isVerifying = false
                        errorMessage = if (currentLang == AppLanguage.SWAHILI)
                            "Alama ya kidole haikutambuliwa. Jaribu tena au weka PIN."
                        else
                            "Empreinte non reconnue. Réessayez ou utilisez votre code PIN."
                    }
                    is BiometricAuthResult.Cancelled -> {
                        isVerifying = false
                    }
                }
            }
        } else {
            // Virtual touch simulation
            isVerifying = true
            errorMessage = null
            coroutineScope.launch {
                delay(350)
                executeUnlock()
            }
        }
    }

    // Auto prompt if hardware available
    LaunchedEffect(Unit) {
        if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
            triggerBiometrics()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(76.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_malaki_logo),
                        contentDescription = "Logo MALAKI",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (currentLang == AppLanguage.SWAHILI) "MALISHO NA AKIBA" else "MALISHO NA AKIBA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Programu Imefungwa kwa Usalama" else "Application Verrouillée",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Info Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.fullName?.take(1) ?: "M").uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Utilisateur",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Localization.roleLabel(currentUser?.role, currentLang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!showPinMode) {
                // FINGERPRINT UNLOCK MODE
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI)
                        "Weka kidole chako kwenye kitambuzi ili kuendelea"
                    else
                        "Posez votre doigt sur le capteur d'empreinte pour déverrouiller",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Pulsing Touch Button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(if (isVerifying) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isSuccess) SuccessGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 2.5.dp,
                            color = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable(enabled = !isVerifying && !isSuccess) {
                            triggerBiometrics()
                        }
                        .testTag("app_lock_fingerprint_touch"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Déverrouillé",
                            tint = SuccessGreen,
                            modifier = Modifier.size(54.dp)
                        )
                    } else if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Empreinte",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sensor State Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (hardwareStatus) {
                        BiometricHardwareStatus.AVAILABLE -> SuccessGreen.copy(alpha = 0.12f)
                        else -> AccentSkyBlue.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when (hardwareStatus) {
                            BiometricHardwareStatus.AVAILABLE -> "● Capteur d'empreinte actif"
                            else -> "● Capteur interactif (Touchez l'icône)"
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
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { triggerBiometrics() },
                    enabled = !isVerifying && !isSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_unlock_biometric")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kufungua na Alama ya Kidole" else "Déverrouiller avec l'empreinte",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        showPinMode = true
                        errorMessage = null
                    }
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Tumia PIN ya siri" else "Utiliser le code PIN",
                        fontSize = 12.sp
                    )
                }
            } else {
                // PIN UNLOCK MODE
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Weka nambari ya PIN ya siri :" else "Entrez votre code secret PIN :",
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
                        .testTag("input_lock_pin")
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (pinInput.isBlank()) {
                            errorMessage = "Veuillez entrer votre code PIN."
                            return@Button
                        }
                        if (BiometricAuthHelper.verifyPin(context, pinInput)) {
                            executeUnlock()
                        } else {
                            errorMessage = "Code PIN incorrect (par défaut: 1234)."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_unlock_pin")
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kufungua" else "Déverrouiller",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = { showPinMode = false }) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Rudi kwa alama ya kidole" else "Retour à l'empreinte",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Option
            OutlinedButton(
                onClick = {
                    viewModel.unlockApp()
                    viewModel.logoutGoogle()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_lock_logout")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentLang == AppLanguage.SWAHILI) "Toka kwenye akaunti" else "Changer de compte / Se déconnecter",
                    fontSize = 12.sp
                )
            }
        }
    }
}
