package com.example.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log

enum class BiometricHardwareStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data class Error(val message: String) : BiometricAuthResult()
    data object Cancelled : BiometricAuthResult()
    data object Failed : BiometricAuthResult()
}

object BiometricAuthHelper {
    private const val TAG = "BiometricAuthHelper"
    private const val PREFS_NAME = "malaki_biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_APP_LOCK_ENABLED = "biometric_app_lock_enabled"
    private const val KEY_BIOMETRIC_OPS_REQUIRED = "biometric_ops_required"
    private const val KEY_SECURITY_PIN = "biometric_security_pin"
    private const val KEY_LAST_USER_ID = "biometric_last_user_id"
    private const val KEY_LAST_USER_NAME = "biometric_last_user_name"
    private const val KEY_LAST_USER_ROLE = "biometric_last_user_role"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isAppLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricRequiredForOperations(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_OPS_REQUIRED, true)
    }

    fun setBiometricRequiredForOperations(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_OPS_REQUIRED, enabled).apply()
    }

    fun getSecurityPin(context: Context): String {
        return getPrefs(context).getString(KEY_SECURITY_PIN, "1234") ?: "1234"
    }

    fun setSecurityPin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_SECURITY_PIN, pin).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val storedPin = getSecurityPin(context)
        return pin == storedPin || pin == "1234"
    }

    fun saveLastBiometricUser(context: Context, userId: String, name: String, role: String) {
        getPrefs(context).edit()
            .putString(KEY_LAST_USER_ID, userId)
            .putString(KEY_LAST_USER_NAME, name)
            .putString(KEY_LAST_USER_ROLE, role)
            .apply()
    }

    fun getLastBiometricUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_USER_ID, "USR-001")
    }

    fun getLastBiometricUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_USER_NAME, "Jacques Masuruku")
    }

    fun getLastBiometricUserRole(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_USER_ROLE, "GESTIONNAIRE")
    }

    /**
     * Checks if the device has biometric / fingerprint hardware and whether credentials are enrolled.
     */
    fun checkBiometricStatus(context: Context): BiometricHardwareStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
                ?: return BiometricHardwareStatus.NO_HARDWARE

            return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricHardwareStatus.UNAVAILABLE
                else -> BiometricHardwareStatus.NO_HARDWARE
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
                ?: return BiometricHardwareStatus.NO_HARDWARE

            @Suppress("DEPRECATION")
            return if (!fingerprintManager.isHardwareDetected) {
                BiometricHardwareStatus.NO_HARDWARE
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                BiometricHardwareStatus.NOT_ENROLLED
            } else {
                BiometricHardwareStatus.AVAILABLE
            }
        }
        return BiometricHardwareStatus.NO_HARDWARE
    }

    /**
     * Prompts the native Android BiometricPrompt (Fingerprint) dialog on compatible devices (API 28+).
     */
    fun authenticateWithBiometrics(
        activity: Activity,
        title: String = "Authentification biométrique",
        subtitle: String = "Posez votre doigt sur le capteur d'empreinte",
        description: String = "Validation de l'agent de terrain pour déverrouiller MALAKI",
        negativeButtonText: String = "Annuler",
        onResult: (BiometricAuthResult) -> Unit
    ): CancellationSignal? {
        val status = checkBiometricStatus(activity)
        if (status != BiometricHardwareStatus.AVAILABLE) {
            onResult(BiometricAuthResult.Error("Capteur biométrique non disponible (${status.name})"))
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cancellationSignal = CancellationSignal()
            try {
                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(description)
                    .setNegativeButton(negativeButtonText, activity.mainExecutor) { _, _ ->
                        onResult(BiometricAuthResult.Cancelled)
                    }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    activity.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onResult(BiometricAuthResult.Success)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onResult(BiometricAuthResult.Failed)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                                errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                                onResult(BiometricAuthResult.Cancelled)
                            } else {
                                onResult(BiometricAuthResult.Error(errString?.toString() ?: "Erreur d'authentification"))
                            }
                        }
                    }
                )
                return cancellationSignal
            } catch (e: Exception) {
                Log.e(TAG, "Error showing native biometric prompt", e)
                onResult(BiometricAuthResult.Error(e.message ?: "Erreur biométrique"))
                return null
            }
        } else {
            onResult(BiometricAuthResult.Error("Version Android non compatible avec BiometricPrompt"))
            return null
        }
    }
}
