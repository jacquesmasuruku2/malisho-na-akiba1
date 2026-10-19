package com.example.ui.screens.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.util.AppLanguage
import com.example.util.BiometricAuthHelper
import com.example.util.BiometricAuthResult
import com.example.util.BiometricHardwareStatus
import com.example.util.Localization
import com.example.util.SavedCredential
import com.example.util.SavedCredentialsHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Écran d'Authentification Sécurisé pour MALISHO NA AKIBA (MALAKI).
 *
 * RÈGLE FONDAMENTALE :
 * - Aucun compte n'est cliquable pour obtenir un accès immédiat sans preuve d'identité.
 * - Authentification réelle via Google Auth (Credential Manager) & Firebase Auth :
 *   1. Continuer avec Google (GetGoogleIdOption / CredentialManager + Firebase Auth)
 *   2. Adresse E-mail + Mot de passe (Neon PostgreSQL & Room)
 *   3. Création de compte sécurisée
 *   4. Mot de passe oublié
 *   5. Empreinte digitale biométrique matérielle (uniquement après enregistrement d'une clé locale valide)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var authSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Identifiants sauvegardés sur l'appareil pour faciliter la reconnexion
    var savedCredentials by remember { mutableStateOf(SavedCredentialsHelper.getSavedCredentials(context)) }
    var lastSavedCredential by remember { mutableStateOf(SavedCredentialsHelper.getLastSavedCredential(context)) }
    var rememberCredentialsOnSignup by remember { mutableStateOf(true) }
    var rememberCredentialsOnLogin by remember { mutableStateOf(true) }
    var showSavedPasswordDialog by remember { mutableStateOf<SavedCredential?>(null) }

    // État d'affichage du formulaire E-mail et Mot de passe (caché par défaut)
    var isEmailPasswordFormVisible by remember { mutableStateOf(false) }

    // Navigation entre les modes du formulaire : 0 = Connexion, 1 = Créer un compte, 2 = Mot de passe oublié
    var selectedAuthMode by remember { mutableIntStateOf(0) }

    // Champs de connexion pré-remplis avec le compte sauvegardé si disponible
    var loginEmail by remember { mutableStateOf(lastSavedCredential?.email ?: "") }
    var loginPassword by remember { mutableStateOf(lastSavedCredential?.password ?: "") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Champs d'inscription
    var signupFullName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("+243 ") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    // Champ mot de passe oublié
    var forgotEmail by remember { mutableStateOf("") }

    // État d'attente de confirmation de courriel
    var pendingEmailConfirmation by remember { mutableStateOf<String?>(null) }

    // Authentification biométrique matérielle
    var showBiometricModal by remember { mutableStateOf(false) }



    fun triggerBiometricPrompt() {
        val activity = context as? Activity
        val status = BiometricAuthHelper.checkBiometricStatus(context)
        val lastId = BiometricAuthHelper.getLastBiometricUserId(context)

        if (lastId.isNullOrEmpty()) {
            Toast.makeText(
                context,
                if (currentLang == AppLanguage.SWAHILI)
                    "Tafadhali ingia mara ya kwanza kwa Google ao neno la siri ili kuweka alama ya kidole."
                else
                    "Veuillez d'abord vous connecter par Google ou e-mail pour associer votre empreinte.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (activity != null && status == BiometricHardwareStatus.AVAILABLE) {
            BiometricAuthHelper.authenticateWithBiometrics(
                activity = activity,
                title = if (currentLang == AppLanguage.SWAHILI) "Kuingia na Alama ya Kidole" else "Connexion par Empreinte Digitale",
                subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako kwenye kitambuzi" else "Posez votre doigt sur le capteur pour déverrouiller MALAKI"
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        val targetUser = allUsers.find { it.id == lastId }
                        if (targetUser != null && targetUser.isActive) {
                            viewModel.loginWithBiometrics(targetUser)
                        } else {
                            authErrorMessage = "Compte associé à l'empreinte introuvable ou inactif."
                        }
                    }
                    is BiometricAuthResult.Error -> {
                        authErrorMessage = result.message
                    }
                    is BiometricAuthResult.Failed -> {
                        authErrorMessage = if (currentLang == AppLanguage.SWAHILI)
                            "Alama ya kidole haikutambuliwa. Jaribu tena."
                        else
                            "Empreinte digitale non reconnue. Réessayez."
                    }
                    is BiometricAuthResult.Cancelled -> {}
                }
            }
        } else {
            showBiometricModal = true
        }
    }

    /**
     * Authentification Google Sign-In officielle via CredentialManager & Firebase Auth :
     * 1. Utilise GetGoogleIdOption configuré avec GOOGLE_WEB_CLIENT_ID
     * 2. Récupère le jeton ID (idToken)
     * 3. Authentifie auprès de Firebase Auth (GoogleAuthProvider.getCredential)
     * 4. Gère les erreurs : annulation, aucun compte, problème réseau/jeton
     */
    fun startGoogleSignIn() {
        val activity = context as? Activity ?: return
        isGoogleLoading = true
        authErrorMessage = null
        authSuccessMessage = null

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                val webClientId = try {
                    context.getString(R.string.default_web_client_id)
                } catch (_: Throwable) {
                    try {
                        com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID
                    } catch (_: Throwable) {
                        ""
                    }
                }

                if (webClientId.isBlank()) {
                    isGoogleLoading = false
                    authErrorMessage = "Configuration Google Sign-In introuvable (default_web_client_id manquant)."
                    Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 1. Interactive Sign-In via GetSignInWithGoogleOption
                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                val idToken = when {
                    credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    }
                    credential is GoogleIdTokenCredential -> {
                        credential.idToken
                    }
                    else -> null
                }

                if (!idToken.isNullOrEmpty()) {
                    viewModel.authenticateWithGoogleFirebase(idToken) { success, error ->
                        isGoogleLoading = false
                        if (success) {
                            authSuccessMessage = "Connexion Google réussie !"
                            Toast.makeText(context, "Bienvenue sur MALISHO NA AKIBA !", Toast.LENGTH_SHORT).show()
                        } else {
                            authErrorMessage = error ?: "Échec de l'authentification Firebase."
                            Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    isGoogleLoading = false
                    authErrorMessage = "Type d'identifiant Google inattendu ou jeton introuvable."
                    Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: GetCredentialCancellationException) {
                isGoogleLoading = false
                Log.d("GoogleAuth", "Connexion Google annulée par l'utilisateur: ${e.message}")
                Toast.makeText(context, "Connexion Google annulée.", Toast.LENGTH_SHORT).show()
            } catch (e: NoCredentialException) {
                // Si GetSignInWithGoogleOption rencontre NoCredentialException, tentons GetGoogleIdOption
                try {
                    val fallbackWebClientId = try {
                        context.getString(R.string.default_web_client_id)
                    } catch (_: Throwable) {
                        com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID
                    }
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(fallbackWebClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val fallbackResult = CredentialManager.create(activity).getCredential(activity, fallbackRequest)
                    val fallbackCred = fallbackResult.credential
                    val fallbackToken = when {
                        fallbackCred is CustomCredential &&
                            fallbackCred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                            GoogleIdTokenCredential.createFrom(fallbackCred.data).idToken
                        }
                        fallbackCred is GoogleIdTokenCredential -> {
                            fallbackCred.idToken
                        }
                        else -> null
                    }
                    if (!fallbackToken.isNullOrEmpty()) {
                        viewModel.authenticateWithGoogleFirebase(fallbackToken) { success, error ->
                            isGoogleLoading = false
                            if (success) {
                                authSuccessMessage = "Connexion Google réussie !"
                                Toast.makeText(context, "Bienvenue sur MALISHO NA AKIBA !", Toast.LENGTH_SHORT).show()
                            } else {
                                authErrorMessage = error ?: "Échec de l'authentification Firebase."
                                Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                        return@launch
                    }
                } catch (fallbackEx: Exception) {
                    Log.w("GoogleAuth", "Fallback GetGoogleIdOption exception: ${fallbackEx.message}")
                }

                isGoogleLoading = false
                authErrorMessage = "Aucun compte Google sélectionné. Veuillez sélectionner un compte Google dans la boîte de dialogue."
                Log.w("GoogleAuth", "No credential available", e)
                Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) {
                isGoogleLoading = false
                authErrorMessage = "Erreur Google Sign-In : ${e.localizedMessage ?: e.message}"
                Log.e("GoogleAuth", "Credential error", e)
                Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isGoogleLoading = false
                authErrorMessage = "Erreur de connexion : ${e.localizedMessage ?: e.message}"
                Log.e("GoogleAuth", "General auth error", e)
                Toast.makeText(context, authErrorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .animateContentSize(animationSpec = spring()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Sélecteur de langue (Français / Swahili)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Français",
                    fontSize = 11.sp,
                    fontWeight = if (currentLang == AppLanguage.FRENCH) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentLang == AppLanguage.FRENCH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setLanguage(AppLanguage.FRENCH) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Kiswahili (RDC)",
                    fontSize = 11.sp,
                    fontWeight = if (currentLang == AppLanguage.SWAHILI) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentLang == AppLanguage.SWAHILI) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setLanguage(AppLanguage.SWAHILI) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Logo officiel MALAKI
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(86.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_malaki_logo),
                        contentDescription = "Logo MALISHO NA AKIBA (MALAKI)",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = Localization.tr("app_title", currentLang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = Localization.tr("app_acronym", currentLang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Authentification Sécurisée • Google Auth & Neon PostgreSQL",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bannière de confirmation d'e-mail en attente
            if (pendingEmailConfirmation != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vérifiez votre adresse e-mail",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nous avons envoyé un lien de confirmation à $pendingEmailConfirmation via Brevo. Consultez votre boîte de réception pour continuer.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.resendEmailConfirmation(pendingEmailConfirmation!!) { success, err ->
                                        if (success) {
                                            Toast.makeText(context, "Lien renvoyé avec succès !", Toast.LENGTH_SHORT).show()
                                        } else {
                                            authErrorMessage = err
                                        }
                                    }
                                }
                            ) {
                                Text("Renvoyer l'e-mail", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Message d'erreur
            if (authErrorMessage != null) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authErrorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { authErrorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Message de succès
            if (authSuccessMessage != null) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authSuccessMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ====================================================
            // 0. COMPTE SAUVEGARDÉ SUR CET APPAREIL (CONNEXION RAPIDE SANS OUBLI DE MOT DE PASSE)
            // ====================================================
            if (lastSavedCredential != null) {
                val cred = lastSavedCredential!!
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("card_saved_credential")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cred.fullName.ifEmpty { "Compte Mémorisé" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreen.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "Sauvegardé",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = cred.email,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    SavedCredentialsHelper.removeCredential(context, cred.email)
                                    savedCredentials = SavedCredentialsHelper.getSavedCredentials(context)
                                    lastSavedCredential = SavedCredentialsHelper.getLastSavedCredential(context)
                                    if (lastSavedCredential != null) {
                                        loginEmail = lastSavedCredential!!.email
                                        loginPassword = lastSavedCredential!!.password
                                    } else {
                                        loginEmail = ""
                                        loginPassword = ""
                                    }
                                    Toast.makeText(context, "Identifiants retirés de cet appareil", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Oublier ce compte",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                loginEmail = cred.email
                                loginPassword = cred.password
                                isAuthLoading = true
                                authErrorMessage = null
                                viewModel.loginWithEmailPassword(
                                    email = cred.email,
                                    password = cred.password,
                                    onResult = { success, err ->
                                        isAuthLoading = false
                                        if (!success) {
                                            authErrorMessage = err ?: "Identifiant ou mot de passe incorrect."
                                        }
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("btn_quick_login_saved_account")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connexion directe • ${cred.fullName.substringBefore(" ")}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    loginEmail = cred.email
                                    loginPassword = cred.password
                                    isEmailPasswordFormVisible = true
                                    selectedAuthMode = 0
                                }
                            ) {
                                Text(
                                    text = "Remplir les champs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = { showSavedPasswordDialog = cred }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voir mon mot de passe",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ====================================================
            // 1. BOUTON GOOGLE OAUTH OFFICIEL (VÉRITABLE AUTHENTIFICATION)
            // ====================================================
            if (isGoogleLoading) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Vérification Google OAuth en cours...", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = { startGoogleSignIn() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .testTag("btn_google_signin")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Logo Google",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.tr("auth_signin_google", currentLang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ====================================================
            // 2. BOUTON EMPREINTE DIGITALE (VRAIE BIOMÉTRIE MATÉRIELLE)
            // ====================================================
            OutlinedButton(
                onClick = { triggerBiometricPrompt() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_biometric_signin")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Empreinte Digitale",
                        tint = PrimaryNavy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLang == AppLanguage.SWAHILI) "Kuingia na Alama ya Kidole" else "Connexion par Empreinte Digitale",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Séparateur Visuel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(
                    text = "  ou  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ====================================================
            // 3. BOUTON D'ACCÈS « EMAIL ET MOT DE PASSE » (CACHÉ PAR DÉFAUT)
            // ====================================================
            OutlinedButton(
                onClick = {
                    isEmailPasswordFormVisible = !isEmailPasswordFormVisible
                    authErrorMessage = null
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isEmailPasswordFormVisible) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_toggle_email_password_form")
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEmailPasswordFormVisible) {
                        if (currentLang == AppLanguage.SWAHILI) "Ficha fomu ya Barua pepe na Neno la siri" else "Masquer E-mail et Mot de passe"
                    } else {
                        if (currentLang == AppLanguage.SWAHILI) "Barua pepe na Neno la siri" else "E-mail et Mot de passe"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isEmailPasswordFormVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // FORMULAIRE DÉPLIABLE EMAIL & MOT DE PASSE (ANIMÉ & ACCESSIBLE SUR CLIC)
            AnimatedVisibility(
                visible = isEmailPasswordFormVisible,
                enter = fadeIn() + expandVertically(animationSpec = spring()),
                exit = fadeOut() + shrinkVertically(animationSpec = spring())
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Onglets de navigation
                        TabRow(
                            selectedTabIndex = selectedAuthMode.coerceAtMost(1),
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedAuthMode == 0,
                                onClick = {
                                    selectedAuthMode = 0
                                    authErrorMessage = null
                                    authSuccessMessage = null
                                },
                                text = { Text("Se connecter", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                            )
                            Tab(
                                selected = selectedAuthMode == 1,
                                onClick = {
                                    selectedAuthMode = 1
                                    authErrorMessage = null
                                    authSuccessMessage = null
                                },
                                text = { Text("Créer un compte", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedAuthMode) {
                            0 -> {
                                // --- SOUS-ÉCRAN : CONNEXION E-MAIL + MOT DE PASSE ---
                                Text(
                                    text = "Adresse e-mail :",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = { loginEmail = it },
                                    placeholder = { Text("nom@exemple.com", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().testTag("input_login_email")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Mot de passe :",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    placeholder = { Text("••••••••", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (loginPasswordVisible) "Masquer" else "Afficher"
                                            )
                                        }
                                    },
                                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_login_password")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedAuthMode = 2
                                            forgotEmail = loginEmail
                                            authErrorMessage = null
                                        }
                                    ) {
                                        Text("Mot de passe oublié ?", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // Option pour mémoriser les identifiants lors de la connexion
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { rememberCredentialsOnLogin = !rememberCredentialsOnLogin }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = rememberCredentialsOnLogin,
                                        onCheckedChange = { rememberCredentialsOnLogin = it },
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryNavy)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mémoriser mes identifiants sur cet appareil",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isAuthLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PrimaryNavy)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Connexion à Neon PostgreSQL...", fontSize = 12.sp, color = PrimaryNavy)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                                authErrorMessage = "Veuillez renseigner votre e-mail et votre mot de passe."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            authErrorMessage = null
                                            viewModel.loginWithEmailPassword(
                                                email = loginEmail,
                                                password = loginPassword,
                                                onResult = { success, err ->
                                                    isAuthLoading = false
                                                    if (success) {
                                                        if (rememberCredentialsOnLogin) {
                                                            SavedCredentialsHelper.saveCredential(
                                                                context = context,
                                                                email = loginEmail,
                                                                password = loginPassword,
                                                                fullName = loginEmail.substringBefore("@"),
                                                                phone = "",
                                                                role = UserRole.CLIENT
                                                            )
                                                            savedCredentials = SavedCredentialsHelper.getSavedCredentials(context)
                                                            lastSavedCredential = SavedCredentialsHelper.getLastSavedCredential(context)
                                                        }
                                                    } else {
                                                        authErrorMessage = err ?: "Identifiant ou mot de passe incorrect."
                                                    }
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_submit_login")
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Se connecter", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }

                            1 -> {
                                // --- SOUS-ÉCRAN : CRÉER UN COMPTE ---
                                Text("Nom complet * :", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupFullName,
                                    onValueChange = { signupFullName = it },
                                    placeholder = { Text("Ex: Jacques Masuruku", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Adresse e-mail * :", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupEmail,
                                    onValueChange = { signupEmail = it },
                                    placeholder = { Text("nom@exemple.com", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Numéro de téléphone :", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupPhone,
                                    onValueChange = { signupPhone = it },
                                    placeholder = { Text("+243 ...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Mot de passe * (min. 6 caractères) :", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupPassword,
                                    onValueChange = { signupPassword = it },
                                    placeholder = { Text("••••••••", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                            Icon(
                                                imageVector = if (signupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (signupPasswordVisible) "Masquer" else "Afficher"
                                            )
                                        }
                                    },
                                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Confirmer le mot de passe * :", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupConfirmPassword,
                                    onValueChange = { signupConfirmPassword = it },
                                    placeholder = { Text("••••••••", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // OPTION ESSENTIELLE : Sauvegarde des identifiants lors de la création
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { rememberCredentialsOnSignup = !rememberCredentialsOnSignup }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = rememberCredentialsOnSignup,
                                            onCheckedChange = { rememberCredentialsOnSignup = it },
                                            colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Sauvegarder mes identifiants sur cet appareil",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Ne perdez jamais votre mot de passe et reconnectez-vous en 1 clic.",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isAuthLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SuccessGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Création du compte dans Neon PostgreSQL...", fontSize = 12.sp, color = SuccessGreen)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (signupFullName.isBlank() || signupEmail.isBlank() || signupPassword.isBlank()) {
                                                authErrorMessage = "Veuillez remplir tous les champs obligatoires."
                                                return@Button
                                            }
                                            if (signupPassword != signupConfirmPassword) {
                                                authErrorMessage = "Les deux mots de passe ne correspondent pas."
                                                return@Button
                                            }
                                            if (signupPassword.length < 4) {
                                                authErrorMessage = "Le mot de passe doit comporter au moins 4 caractères."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            authErrorMessage = null
                                            viewModel.signUpWithEmailPassword(
                                                email = signupEmail,
                                                password = signupPassword,
                                                fullName = signupFullName,
                                                phone = signupPhone,
                                                role = UserRole.CLIENT,
                                                onResult = { success, err ->
                                                    isAuthLoading = false
                                                    if (success) {
                                                        if (rememberCredentialsOnSignup) {
                                                            SavedCredentialsHelper.saveCredential(
                                                                context = context,
                                                                email = signupEmail,
                                                                password = signupPassword,
                                                                fullName = signupFullName,
                                                                phone = signupPhone,
                                                                role = UserRole.CLIENT
                                                            )
                                                            savedCredentials = SavedCredentialsHelper.getSavedCredentials(context)
                                                            lastSavedCredential = SavedCredentialsHelper.getLastSavedCredential(context)
                                                            loginEmail = signupEmail
                                                            loginPassword = signupPassword
                                                        }
                                                        authSuccessMessage = if (rememberCredentialsOnSignup) {
                                                            "Compte créé et identifiants sauvegardés avec succès sur cet appareil !"
                                                        } else {
                                                            "Compte créé avec succès dans la base de données !"
                                                        }
                                                        selectedAuthMode = 0
                                                    } else {
                                                        authErrorMessage = err ?: "Erreur lors de la création du compte."
                                                    }
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_submit_signup")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Créer mon compte", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }

                            2 -> {
                                // --- SOUS-ÉCRAN : MOT DE PASSE OUBLIÉ ---
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Réinitialisation du mot de passe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Saisissez votre e-mail pour recevoir un lien sécurisé via Brevo SMTP :",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = forgotEmail,
                                    onValueChange = { forgotEmail = it },
                                    placeholder = { Text("nom@exemple.com", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isAuthLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Envoi de l'e-mail via Brevo...", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (forgotEmail.isBlank()) {
                                                authErrorMessage = "Veuillez renseigner votre adresse e-mail."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            authErrorMessage = null
                                            viewModel.resetPassword(forgotEmail) { success, msg ->
                                                isAuthLoading = false
                                                if (success) {
                                                    authSuccessMessage = msg
                                                    selectedAuthMode = 0
                                                } else {
                                                    authErrorMessage = msg ?: "Échec de l'envoi."
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Envoyer le lien de réinitialisation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { selectedAuthMode = 0 },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retour à la connexion", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mentions de sécurité et SSL
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MALISHO NA AKIBA • Sécurité SSL & Neon PostgreSQL Cloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp
                )
            }
        }
    }

    // Dialogue d'empreinte digitale si le matériel système n'est pas prêt
    if (showBiometricModal) {
        val lastId = BiometricAuthHelper.getLastBiometricUserId(context)
        val targetUser = allUsers.find { it.id == lastId }

        if (targetUser != null && targetUser.isActive) {
            BiometricPromptDialog(
                title = if (currentLang == AppLanguage.SWAHILI) "Kuingia na Alama ya Kidole" else "Connexion par Empreinte Digitale",
                subtitle = if (currentLang == AppLanguage.SWAHILI) "Weka kidole chako kwenye kitambuzi" else "Posez votre doigt sur le capteur pour déverrouiller MALAKI",
                operationSummary = "Capteur matériel : ${targetUser.fullName}",
                onSuccess = {
                    showBiometricModal = false
                    viewModel.loginWithBiometrics(targetUser)
                },
                onDismiss = {
                    showBiometricModal = false
                }
            )
        } else {
            showBiometricModal = false
            Toast.makeText(context, "Aucune empreinte active enregistrée.", Toast.LENGTH_SHORT).show()
        }
    }

    // Dialogue d'affichage sécurisé du mot de passe mémorisé (pour ne jamais l'oublier)
    if (showSavedPasswordDialog != null) {
        val cred = showSavedPasswordDialog!!
        AlertDialog(
            onDismissRequest = { showSavedPasswordDialog = null },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Identifiants Mémorisés",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accès sauvegardés sur cet appareil pour vous reconnecter facilement :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Nom : ${cred.fullName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "E-mail : ${cred.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Mot de passe enregistré :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = cred.password,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Mot de passe", cred.password)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Mot de passe copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("Copier", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavedPasswordDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}
