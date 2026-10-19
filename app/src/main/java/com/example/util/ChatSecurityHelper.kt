package com.example.util

import com.example.data.model.AppNotificationEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.ClientEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole

/**
 * Règle de sécurité, de confidentialité et de ciblage strict pour :
 * 1. La messagerie (accès aux salons et aux messages).
 * 2. L'adressage des notifications (seule la personne à qui le message/l'opération est adressé(e) doit recevoir la notification).
 * 3. L'attribution d'une adresse/destination aux activités (journal d'activité / audit) et visibilité exclusive aux personnes concernées.
 */
object ChatSecurityHelper {

    /**
     * Retourne l'ensemble de tous les identifiants possibles rattachés à un utilisateur.
     */
    fun getUserPossibleIds(user: UserEntity?, clients: List<ClientEntity>): Set<String> {
        if (user == null) return emptySet()
        val ids = mutableSetOf(
            user.id,
            "USR-${user.id}",
            user.id.removePrefix("USR-")
        )
        if (user.username.isNotEmpty()) ids.add(user.username)
        if (user.email.isNotEmpty()) {
            ids.add(user.email.lowercase())
            ids.add(user.email.substringBefore("@"))
        }
        if (user.phone.isNotEmpty()) ids.add(user.phone)

        val myClient = clients.find {
            (user.phone.isNotEmpty() && it.phone == user.phone) ||
            it.id == user.id ||
            user.id.endsWith(it.id) ||
            (it.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true))
        }
        if (myClient != null) {
            ids.add(myClient.id)
            ids.add("USR-${myClient.id}")
            ids.add(myClient.id.removePrefix("USR-"))
        }
        return ids
    }

    /**
     * Vérifie de manière robuste si un utilisateur est participant d'une discussion directe 1-à-1.
     */
    fun isUserParticipantOfDirectRoom(user: UserEntity?, roomId: String, clients: List<ClientEntity>): Boolean {
        if (user == null || !roomId.startsWith("DIRECT_")) return false
        val participants = roomId.removePrefix("DIRECT_").split("_")
        val myIds = getUserPossibleIds(user, clients)
        return participants.any { p ->
            myIds.any { myId ->
                p.equals(myId, ignoreCase = true) ||
                p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
            }
        }
    }

    /**
     * Vérifie si un utilisateur a le droit d'accéder à un salon de discussion.
     */
    fun canUserAccessChatRoom(
        user: UserEntity?,
        roomId: String,
        clients: List<ClientEntity>
    ): Boolean {
        if (user == null) return false
        if (roomId == "COMMUNITY_GROUP") return true

        // Discussions directes 1-à-1 (créées par recherche de contact, téléphone ou email style WhatsApp/Telegram)
        if (roomId.startsWith("DIRECT_")) {
            if (!isUserParticipantOfDirectRoom(user, roomId, clients)) return false
            // Règle de confidentialité stricte : La direction (Gestionnaire) ne doit pas avoir accès aux conversations directes impliquant un client
            if (user.role == UserRole.GESTIONNAIRE) {
                val participants = roomId.removePrefix("DIRECT_").split("_")
                val hasClientParticipant = participants.any { p ->
                    p.startsWith("CLI-") || p.startsWith("USR-CLI-") || clients.any { c ->
                        c.id.equals(p, ignoreCase = true) || "USR-${c.id}".equals(p, ignoreCase = true)
                    }
                }
                if (hasClientParticipant) return false
            }
            return true
        }

        return when (user.role) {
            UserRole.CLIENT -> {
                // Le client a accès à son salon privé attitré (ex: CLI-001)
                val myClient = clients.find {
                    it.phone == user.phone || it.id == user.id || user.id.endsWith(it.id) ||
                            (it.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true))
                }
                val myClientId = myClient?.id ?: if (user.id.startsWith("USR-CLI-")) user.id.removePrefix("USR-") else null
                (myClientId != null && roomId == myClientId) || (roomId == user.id)
            }

            UserRole.GESTIONNAIRE -> {
                // Règle de confidentialité : les salons clients de terrain CLI-... restent assignés aux agents
                !roomId.startsWith("CLI-")
            }

            UserRole.AGENT -> {
                if (roomId.startsWith("CLI-")) {
                    val client = clients.find { it.id == roomId }
                    client == null || client.assignedAgentId == null || client.assignedAgentId == user.id || client.marketZone == user.marketZone
                } else true
            }

            UserRole.CAISSIER -> {
                !roomId.startsWith("CLI-")
            }
        }
    }

    /**
     * Détermine l'adresse et le destinataire cible d'un message selon le salon.
     * Retourne Triple(targetUserId, targetClientId, destinationAddress)
     */
    fun resolveMessageDestination(
        sender: UserEntity,
        roomId: String,
        clients: List<ClientEntity>,
        allUsers: List<UserEntity>
    ): Triple<String?, String?, String> {
        return when {
            roomId == "COMMUNITY_GROUP" -> {
                Triple(
                    null, // Diffusé à toute la communauté sauf l'expéditeur
                    null,
                    "Salon Communautaire MALISHO NA AKIBA"
                )
            }
            roomId.startsWith("CLI-") -> {
                val client = clients.find { it.id == roomId }
                if (sender.role == UserRole.CLIENT) {
                    // Le client envoie un message à son agent de crédit assigné
                    val agentId = client?.assignedAgentId
                    val assignedAgent = agentId?.let { aid -> allUsers.find { it.id == aid } }
                    val destAddress = if (assignedAgent != null) {
                        "Conseiller ${assignedAgent.fullName} (${assignedAgent.marketZone})"
                    } else {
                        "Conseiller de zone (${client?.marketZone ?: "Marché"})"
                    }
                    Triple(agentId, roomId, destAddress)
                } else {
                    // L'agent/staff envoie un message privé au client
                    val clientUser = client?.let { c ->
                        allUsers.find { it.phone == c.phone || (c.email.isNotEmpty() && it.email.equals(c.email, ignoreCase = true)) }
                    }
                    val targetUserId = clientUser?.id ?: "USR-$roomId"
                    val destAddress = "Client : ${client?.fullName ?: roomId} • ${client?.marketZone ?: "Zone marchande"}"
                    Triple(targetUserId, roomId, destAddress)
                }
            }
            roomId.startsWith("DIRECT_") -> {
                val participants = roomId.removePrefix("DIRECT_").split("_")
                val myPossibleIds = getUserPossibleIds(sender, clients)
                val recipientId = participants.firstOrNull { p ->
                    myPossibleIds.none { myId ->
                        p.equals(myId, ignoreCase = true) ||
                        p.removePrefix("USR-").equals(myId.removePrefix("USR-"), ignoreCase = true)
                    }
                } ?: participants.lastOrNull()
                val recipientUser = recipientId?.let { rid ->
                    allUsers.find { it.id == rid || "USR-${it.id}" == rid || it.id.removePrefix("USR-") == rid }
                }
                val recipientClient = recipientId?.let { rid ->
                    clients.find { it.id == rid || "USR-${it.id}" == rid || (recipientUser != null && it.phone == recipientUser.phone) }
                }
                val destAddress = recipientUser?.let {
                    "${it.fullName} (${it.role.label})"
                } ?: recipientClient?.let {
                    "${it.fullName} (Client)"
                } ?: "Contact ${recipientId ?: "privé"}"
                Triple(recipientUser?.id ?: recipientId, recipientClient?.id, destAddress)
            }
            else -> {
                Triple(null, null, "Discussion $roomId")
            }
        }
    }

    /**
     * Règle stricte de réception des notifications :
     * La notification doit venir UNIQUEMENT à la personne à qui le message ou l'opération a été adressé(e).
     * Tous les utilisateurs n'ont pas besoin de voir cette notification si elle ne leur est pas adressée.
     * L'expéditeur ne reçoit JAMAIS de notification pour sa propre action ou son propre message.
     */
    fun isNotificationConcernedForUser(
        notification: AppNotificationEntity,
        user: UserEntity?,
        clients: List<ClientEntity>
    ): Boolean {
        if (user == null) return false

        // 1. L'émetteur ne reçoit JAMAIS sa propre notification (contrôle absolu par ID, préfixe USR-, compte client, email ou nom)
        val sId = notification.senderId
        if (!sId.isNullOrBlank()) {
            if (sId.equals(user.id, ignoreCase = true)) return false
            if (sId.removePrefix("USR-").equals(user.id.removePrefix("USR-"), ignoreCase = true)) return false
            if (sId.equals("USR-${user.id}", ignoreCase = true) || user.id.equals("USR-$sId", ignoreCase = true)) return false
            if (user.username.isNotEmpty() && sId.equals(user.username, ignoreCase = true)) return false
            if (user.email.isNotEmpty() && (sId.equals(user.email, ignoreCase = true) || sId.equals(user.email.substringBefore("@"), ignoreCase = true))) return false
            if (user.phone.isNotEmpty() && (sId.contains(user.phone) || user.phone.contains(sId))) return false

            val myClient = clients.find {
                (user.phone.isNotEmpty() && it.phone == user.phone) ||
                it.id == user.id ||
                user.id.endsWith(it.id) ||
                (it.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true))
            }
            if (myClient != null && (sId.equals(myClient.id, ignoreCase = true) || sId.equals("USR-${myClient.id}", ignoreCase = true))) {
                return false
            }
        }

        val sName = notification.senderName
        if (!sName.isNullOrBlank()) {
            if (user.fullName.isNotBlank() && sName.trim().equals(user.fullName.trim(), ignoreCase = true)) return false
            if (user.email.isNotEmpty() && sName.equals(user.email, ignoreCase = true)) return false
            if (user.phone.isNotEmpty() && sName.contains(user.phone)) return false
        }

        // Vérification sur le titre et le contenu : un utilisateur ne reçoit jamais une notification dont il est l'auteur
        if (user.fullName.isNotBlank()) {
            val trimmedName = user.fullName.trim()
            if (notification.title.startsWith("Message de $trimmedName", ignoreCase = true) ||
                notification.title.contains("Message de $trimmedName", ignoreCase = true) ||
                notification.title.contains("par $trimmedName", ignoreCase = true)
            ) {
                return false
            }
        }

        // 2. Si un utilisateur précis est expressément ciblé (targetUserId)
        if (notification.targetUserId != null) {
            val targetId = notification.targetUserId
            val myPossibleIds = getUserPossibleIds(user, clients)
            val isTargeted = myPossibleIds.any {
                it.equals(targetId, ignoreCase = true) ||
                it.removePrefix("USR-").equals(targetId.removePrefix("USR-"), ignoreCase = true)
            }
            if (isTargeted) return true
            // Ciblé pour un autre utilisateur particulier -> Masqué !
            return false
        }

        // 3. Si un rôle spécifique est ciblé et ne correspond pas au rôle de l'utilisateur
        if (notification.targetRole != null && notification.targetRole != user.role) {
            return false
        }

        // 4. Si un client particulier est concerné (targetClientId)
        if (notification.targetClientId != null) {
            val cId = notification.targetClientId
            val client = clients.find { it.id == cId }

            return when (user.role) {
                UserRole.CLIENT -> {
                    // Seul CE client particulier a le droit de voir cette notification
                    val isMe = (client != null && (client.phone == user.phone || client.id == user.id || user.id.endsWith(client.id) || (client.email.isNotEmpty() && client.email.equals(user.email, ignoreCase = true))))
                    isMe || user.id == cId || user.id.removePrefix("USR-") == cId
                }
                UserRole.AGENT -> {
                    // L'agent voit la notification seulement s'il gère ce client ou si ce client est dans son marché
                    if (client == null) true
                    else client.assignedAgentId == user.id || client.marketZone == user.marketZone
                }
                UserRole.GESTIONNAIRE -> {
                    // Le gestionnaire ne voit que les alertes de gestion (pas les messages privés de clients)
                    notification.type != com.example.data.model.NotificationType.MESSAGE
                }
                UserRole.CAISSIER -> {
                    notification.type == com.example.data.model.NotificationType.PAIEMENT ||
                            notification.type == com.example.data.model.NotificationType.CAISSE
                }
            }
        }

        // 5. Si un rôle spécifique est ciblé et correspond au rôle de l'utilisateur
        if (notification.targetRole != null && notification.targetRole == user.role) {
            return true
        }

        // 6. Notification générale de groupe ou de diffusion (targetUserId == null && targetRole == null && targetClientId == null)
        // Les messages privés ne doivent JAMAIS apparaître en diffusion générale
        if (notification.type == com.example.data.model.NotificationType.MESSAGE) {
            val isCommunity = notification.title.contains("Communauté", ignoreCase = true) ||
                    notification.destinationAddress?.contains("Communaut", ignoreCase = true) == true ||
                    notification.actionRoute?.contains("COMMUNITY_GROUP") == true
            return isCommunity
        }

        // Accessible à tous les membres connectés pour les alertes système générales
        return true
    }

    /**
     * Règle stricte de visibilité des activités (Journal d'audit & sécurité) :
     * Chaque activité a une adresse ou une destination (bénéficiaire, marché, caisse, etc.).
     * L'activité ne doit apparaître qu'aux personnes concernées.
     */
    fun isActivityConcernedForUser(
        log: AuditLogEntity,
        user: UserEntity?,
        clients: List<ClientEntity>
    ): Boolean {
        if (user == null) return false

        // 1. La Direction (Gestionnaire) a la supervision de contrôle et d'audit intégral
        if (user.role == UserRole.GESTIONNAIRE) return true

        // 2. L'auteur de l'action est toujours concerné
        if (log.userId == user.id) return true

        // 3. Si un utilisateur précis est expressément ciblé par l'activité
        if (log.targetUserId != null) {
            if (log.targetUserId == user.id) return true
            if (user.role == UserRole.CLIENT) {
                if (log.targetUserId == "USR-${user.id}" || log.targetUserId.removePrefix("USR-") == user.id) return true
                val myClient = clients.find {
                    it.phone == user.phone || it.id == user.id || user.id.endsWith(it.id) ||
                            (it.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true))
                }
                if (myClient != null && (log.targetUserId == myClient.id || log.targetUserId == "USR-${myClient.id}")) {
                    return true
                }
            }
        }

        // 4. Si un rôle spécifique est expressément ciblé
        if (log.targetRole != null && log.targetRole == user.role) {
            return true
        }

        // 5. Si l'utilisateur est un Client
        if (user.role == UserRole.CLIENT) {
            val myClient = clients.find {
                it.phone == user.phone || it.id == user.id || user.id.endsWith(it.id) ||
                        (it.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true))
            }
            val myClientId = myClient?.id ?: if (user.id.startsWith("USR-CLI-")) user.id.removePrefix("USR-") else null
            if (myClientId != null && log.targetClientId == myClientId) return true
            if (myClient != null && log.destination.contains(myClient.fullName, ignoreCase = true)) return true
            return false
        }

        // 6. Si l'utilisateur est un Agent
        if (user.role == UserRole.AGENT) {
            if (log.targetClientId != null) {
                val client = clients.find { it.id == log.targetClientId }
                if (client != null && (client.assignedAgentId == user.id || client.marketZone == user.marketZone)) {
                    return true
                }
            }
            if (log.destination.contains(user.marketZone, ignoreCase = true) || log.destination.contains(user.fullName, ignoreCase = true)) {
                return true
            }
            return false
        }

        // 7. Si l'utilisateur est un Caissier
        if (user.role == UserRole.CAISSIER) {
            if (log.entityType == "CAISSE" || log.entityType == "PAYMENT" || log.action.contains("CAISSE") || log.action.contains("PAIEMENT")) {
                return true
            }
            return false
        }

        return false
    }
}
