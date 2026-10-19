package com.example.data.cloudinary

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service de stockage en nuage pour les documents, photos, vidéos et messages vocaux.
 * Fonctionne de façon entièrement discrète et transparente en arrière-plan :
 * Si le stockage distant est indisponible ou hors-ligne, le fichier local reste intact.
 */
class CloudinaryStorageService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Configuration Cloudinary dynamique avec fallback sécurisé
    private val cloudName: String = try {
        BuildConfig.CLOUDINARY_CLOUD_NAME.ifEmpty { "mediaflows" }
    } catch (_: Throwable) {
        "mediaflows"
    }

    private val apiKey: String = try {
        BuildConfig.CLOUDINARY_API_KEY.ifEmpty { "430489914488339" }
    } catch (_: Throwable) {
        "430489914488339"
    }

    private val apiSecret: String = try {
        BuildConfig.CLOUDINARY_API_SECRET.ifEmpty { "OUFzfTHpDIXxKsTg8r-2SBzYxQc" }
    } catch (_: Throwable) {
        "OUFzfTHpDIXxKsTg8r-2SBzYxQc"
    }

    private val uploadPreset: String = try {
        BuildConfig.CLOUDINARY_UPLOAD_PRESET.ifEmpty { "ml_default" }
    } catch (_: Throwable) {
        "ml_default"
    }

    private fun sha1(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Copie un URI Android (Galerie, Sélecteur de fichiers, etc.) vers le cache local
     * de l'application pour garantir sa persistance et éviter les pertes de permissions.
     */
    fun copyUriToLocalCache(uri: Uri, folderName: String = "chat_attachments"): File? {
        return try {
            val contentResolver = context.contentResolver
            val dir = File(context.filesDir, folderName).apply {
                if (!exists()) mkdirs()
            }

            var fileName = "fichier_${System.currentTimeMillis()}"
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("CloudinaryStorage", "Impossible de lire le nom du fichier: ${e.message}")
            }

            // Éviter les collisions de noms de fichiers
            val safeFileName = "${System.currentTimeMillis()}_${fileName.replace(" ", "_")}"
            val destFile = File(dir, safeFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            destFile
        } catch (e: Exception) {
            Log.e("CloudinaryStorage", "Erreur lors de la mise en cache de l'URI: ${e.message}")
            null
        }
    }

    /**
     * Téléverse un fichier (audio vocal, image, vidéo ou document) vers Cloudinary
     * en le liant explicitement à son auteur (senderId, senderName, senderRole, room).
     */
    suspend fun uploadMedia(
        file: File,
        resourceType: String = "auto",
        folder: String = "malaki_microfinance/attachments",
        displayName: String = "media_${System.currentTimeMillis()}",
        senderId: String = "USR-SYSTEM",
        senderName: String = "Utilisateur",
        senderRole: String = "MEMBRE",
        clientId: String = "GENERAL",
        messageId: String = "MSG-${System.currentTimeMillis()}"
    ): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext null
        }

        val mimeType = when {
            file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) -> "image/jpeg"
            file.name.endsWith(".png", true) -> "image/png"
            file.name.endsWith(".webp", true) -> "image/webp"
            file.name.endsWith(".mp4", true) -> "video/mp4"
            file.name.endsWith(".m4a", true) || file.name.endsWith(".aac", true) -> "audio/mp4"
            file.name.endsWith(".pdf", true) -> "application/pdf"
            else -> "application/octet-stream"
        }
        val mediaType = mimeType.toMediaTypeOrNull()
        val fileBody = file.asRequestBody(mediaType)

        val cleanAuthorName = senderName.replace("|", " ").replace("=", "-").take(50)
        val contextMeta = "author_id=$senderId|author_name=$cleanAuthorName|author_role=$senderRole|room_id=$clientId|msg_id=$messageId"
        val tagsMeta = "author_${senderId.replace(" ", "_")},role_${senderRole},room_${clientId.replace(" ", "_")},msg_${messageId}"
        val targetFolder = "$folder/$senderId"
        val publicId = "${senderId}_${System.currentTimeMillis()}_$displayName"
        val timestamp = (System.currentTimeMillis() / 1000L).toString()

        val endpointUrl = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"

        // STRATÉGIE 1 : Téléversement signé avec API Key et Secret (Garantit l'authentification et les métadonnées auteur)
        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            try {
                // Signature Cloudinary : tri alphabétique des paramètres
                // context, folder, public_id, tags, timestamp
                val paramsToSign = listOf(
                    "context" to contextMeta,
                    "folder" to targetFolder,
                    "public_id" to publicId,
                    "tags" to tagsMeta,
                    "timestamp" to timestamp
                ).sortedBy { it.first }

                val stringToSign = paramsToSign.joinToString("&") { "${it.first}=${it.second}" } + apiSecret
                val signature = sha1(stringToSign)

                val signedBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, fileBody)
                    .addFormDataPart("api_key", apiKey)
                    .addFormDataPart("timestamp", timestamp)
                    .addFormDataPart("signature", signature)
                    .addFormDataPart("folder", targetFolder)
                    .addFormDataPart("public_id", publicId)
                    .addFormDataPart("context", contextMeta)
                    .addFormDataPart("tags", tagsMeta)
                    .build()

                val request = Request.Builder()
                    .url(endpointUrl)
                    .post(signedBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val secureUrl = json.optString("secure_url", "")
                    if (secureUrl.isNotBlank()) {
                        Log.d("CloudinaryStorage", "Téléversement signé réussi pour l'auteur $senderId ($cleanAuthorName) : $secureUrl")
                        return@withContext secureUrl
                    }
                } else {
                    Log.w("CloudinaryStorage", "Échec téléversement signé (code ${response.code}), basculement sur téléversement preset...")
                }
            } catch (e: Exception) {
                Log.w("CloudinaryStorage", "Erreur lors du téléversement signé : ${e.message}")
            }
        }

        // STRATÉGIE 2 : Téléversement avec upload_preset
        try {
            val presetBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, fileBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", targetFolder)
                .addFormDataPart("public_id", publicId)
                .addFormDataPart("context", contextMeta)
                .addFormDataPart("tags", tagsMeta)
                .build()

            val request = Request.Builder()
                .url(endpointUrl)
                .post(presetBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val secureUrl = json.optString("secure_url", "")
                if (secureUrl.isNotBlank()) {
                    Log.d("CloudinaryStorage", "Téléversement preset réussi pour l'auteur $senderId : $secureUrl")
                    return@withContext secureUrl
                }
            } else {
                Log.d("CloudinaryStorage", "Réponse HTTP Cloudinary preset : code ${response.code}")
            }
        } catch (e: Exception) {
            Log.d("CloudinaryStorage", "Mode local conservé : ${e.message}")
        }

        null
    }

    /**
     * Téléverse le message vocal audio vers le stockage Cloudinary avec liaison auteur.
     */
    suspend fun uploadAudioDocument(
        file: File,
        displayName: String = "voice_${System.currentTimeMillis()}",
        senderId: String = "USR-SYSTEM",
        senderName: String = "Utilisateur",
        senderRole: String = "MEMBRE",
        clientId: String = "GENERAL",
        messageId: String = "MSG-${System.currentTimeMillis()}"
    ): String? = uploadMedia(
        file = file,
        resourceType = "auto",
        folder = "malaki_microfinance/voice_messages",
        displayName = displayName,
        senderId = senderId,
        senderName = senderName,
        senderRole = senderRole,
        clientId = clientId,
        messageId = messageId
    )

    /**
     * Téléverse une photo de profil (avatar) vers Cloudinary depuis un URI de la galerie.
     * Enregistre le fichier dans le cache persistant puis le transfère sur Cloudinary.
     */
    suspend fun uploadAvatar(
        imageUri: Uri,
        userId: String = "USR-PROFILE",
        userName: String = "Utilisateur"
    ): String? = withContext(Dispatchers.IO) {
        val cachedFile = copyUriToLocalCache(imageUri, "avatars") ?: return@withContext null
        val remoteUrl = uploadMedia(
            file = cachedFile,
            resourceType = "image",
            folder = "malaki_microfinance/avatars",
            displayName = "avatar_${userId}_${System.currentTimeMillis()}",
            senderId = userId,
            senderName = userName,
            senderRole = "AVATAR",
            clientId = userId,
            messageId = "AVT-${System.currentTimeMillis()}"
        )
        remoteUrl ?: Uri.fromFile(cachedFile).toString()
    }
}
