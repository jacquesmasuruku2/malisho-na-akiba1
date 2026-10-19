package com.example.ui.screens.chat

import android.net.Uri

enum class AttachmentType(val label: String) {
    PHOTO("Photo"),
    VIDEO("Vidéo"),
    DOCUMENT("Document"),
    LOCATION("Localisation")
}

data class PendingAttachment(
    val type: AttachmentType,
    val localFilePath: String,
    val displayName: String,
    val fileSizeFormatted: String,
    val mimeType: String,
    val uri: Uri? = null,
    val locationTitle: String? = null,
    val locationCoordinates: String? = null
)
