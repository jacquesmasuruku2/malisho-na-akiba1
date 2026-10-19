package com.example.util

import android.content.Context
import android.util.Base64
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modèle de données pour les identifiants de connexion sauvegardés sur l'appareil.
 */
data class SavedCredential(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String = "",
    val role: UserRole = UserRole.CLIENT,
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * Gestionnaire sécurisé de sauvegarde locale des identifiants de connexion (MALISHO NA AKIBA).
 *
 * Permet aux utilisateurs (particulièrement les commerçants et clients) de :
 * 1. Sauvegarder automatiquement ou manuellement leurs identifiants lors de la création du compte.
 * 2. Se reconnecter facilement sans oublier leur mot de passe.
 * 3. Pré-remplir les champs de connexion ou se connecter en 1 clic.
 * 4. Consulter leur mot de passe mémorisé ou supprimer les accès locaux.
 */
object SavedCredentialsHelper {
    private const val PREFS_NAME = "malaki_saved_credentials_prefs"
    private const val KEY_CREDENTIALS_LIST = "saved_credentials_json"
    private const val KEY_LAST_SAVED_EMAIL = "last_saved_email"
    private const val KEY_REMEMBER_CREDENTIALS = "remember_credentials_enabled"

    // Clé de masquage réversible (XOR + Base64) pour éviter le stockage en texte brut lisible
    private const val OBFUSCATION_MASK = 0x5C

    private fun obfuscate(text: String): String {
        if (text.isEmpty()) return ""
        val xored = text.map { (it.code xor OBFUSCATION_MASK).toChar() }.joinToString("")
        return Base64.encodeToString(xored.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun deobfuscate(encoded: String): String {
        if (encoded.isEmpty()) return ""
        return try {
            val decoded = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
            decoded.map { (it.code xor OBFUSCATION_MASK).toChar() }.joinToString("")
        } catch (_: Exception) {
            encoded
        }
    }

    /**
     * Enregistre les identifiants d'un utilisateur sur cet appareil.
     */
    fun saveCredential(
        context: Context,
        email: String,
        password: String,
        fullName: String,
        phone: String = "",
        role: UserRole = UserRole.CLIENT
    ) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanName = fullName.trim()

        if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = getSavedCredentials(context).toMutableList()

        // Supprime l'ancienne entrée pour ce même e-mail ou téléphone s'il existait
        list.removeAll { it.email.equals(cleanEmail, ignoreCase = true) || (phone.isNotBlank() && it.phone == phone.trim()) }

        // Ajoute en tête de liste
        list.add(
            0,
            SavedCredential(
                email = cleanEmail,
                password = cleanPassword,
                fullName = cleanName,
                phone = phone.trim(),
                role = role,
                savedAt = System.currentTimeMillis()
            )
        )

        // Limite aux 5 comptes les plus récents
        val trimmedList = list.take(5)

        val jsonArray = JSONArray()
        trimmedList.forEach { cred ->
            val obj = JSONObject()
            obj.put("email", cred.email)
            obj.put("pwd", obfuscate(cred.password))
            obj.put("name", cred.fullName)
            obj.put("phone", cred.phone)
            obj.put("role", cred.role.name)
            obj.put("time", cred.savedAt)
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_CREDENTIALS_LIST, jsonArray.toString())
            .putString(KEY_LAST_SAVED_EMAIL, cleanEmail)
            .putBoolean(KEY_REMEMBER_CREDENTIALS, true)
            .apply()
    }

    /**
     * Récupère la liste de tous les identifiants mémorisés sur l'appareil.
     */
    fun getSavedCredentials(context: Context): List<SavedCredential> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CREDENTIALS_LIST, null) ?: return emptyList()

        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<SavedCredential>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val email = obj.optString("email")
                val pwdObfuscated = obj.optString("pwd")
                val name = obj.optString("name")
                val phone = obj.optString("phone", "")
                val roleStr = obj.optString("role", UserRole.CLIENT.name)
                val role = try {
                    UserRole.valueOf(roleStr)
                } catch (_: Exception) {
                    UserRole.CLIENT
                }
                val time = obj.optLong("time", System.currentTimeMillis())

                if (email.isNotEmpty()) {
                    result.add(
                        SavedCredential(
                            email = email,
                            password = deobfuscate(pwdObfuscated),
                            fullName = name,
                            phone = phone,
                            role = role,
                            savedAt = time
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Récupère le dernier compte sauvegardé pour pré-remplir l'écran de connexion.
     */
    fun getLastSavedCredential(context: Context): SavedCredential? {
        val list = getSavedCredentials(context)
        if (list.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastEmail = prefs.getString(KEY_LAST_SAVED_EMAIL, null)
        return list.find { it.email.equals(lastEmail, ignoreCase = true) } ?: list.firstOrNull()
    }

    /**
     * Supprime les identifiants mémorisés d'un compte spécifique.
     */
    fun removeCredential(context: Context, email: String) {
        val list = getSavedCredentials(context).toMutableList()
        list.removeAll { it.email.equals(email.trim(), ignoreCase = true) }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { cred ->
            val obj = JSONObject()
            obj.put("email", cred.email)
            obj.put("pwd", obfuscate(cred.password))
            obj.put("name", cred.fullName)
            obj.put("phone", cred.phone)
            obj.put("role", cred.role.name)
            obj.put("time", cred.savedAt)
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_CREDENTIALS_LIST, jsonArray.toString())
            .apply()
    }

    /**
     * Supprime tous les identifiants mémorisés sur l'appareil.
     */
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
