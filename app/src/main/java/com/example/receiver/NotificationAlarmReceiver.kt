package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récepteur système pour assurer le fonctionnement des notifications MALAKI
 * même avant l'ouverture de l'application (Démarrage du téléphone, alarmes périodiques en arrière-plan).
 */
class NotificationAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_CHECK_NOTIFICATIONS = "com.example.malaki.CHECK_NOTIFICATIONS"
        private const val REQUEST_CODE = 44001
        private const val INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        fun schedulePeriodicChecks(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                    action = ACTION_CHECK_NOTIFICATIONS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Planification inexacte périodique économe en batterie
                val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    INTERVAL_MS,
                    pendingIntent
                )
                Log.d(TAG, "Vérifications de notifications périodiques planifiées.")
            } catch (e: Exception) {
                Log.w(TAG, "Impossible de planifier l'alarme: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "NotificationAlarmReceiver reçu action: $action")

        // S'assurer que le canal de notification est initialisé
        NotificationHelper.init(context)

        // Replanifier si redémarrage ou mise à jour
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            schedulePeriodicChecks(context)
        }

        // Vérification en arrière-plan des messages et alertes financières en attente
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs1 = context.getSharedPreferences("microcredit_prefs", Context.MODE_PRIVATE)
                val prefs2 = context.getSharedPreferences("remix_microfinance_prefs", Context.MODE_PRIVATE)
                val activeUserId = prefs1.getString("active_user_id", null)?.takeIf { it.isNotEmpty() }
                    ?: prefs2.getString("active_user_id", null)
                if (activeUserId.isNullOrEmpty()) {
                    // Aucun utilisateur connecté sur cet appareil, aucune notification à afficher
                    return@launch
                }

                val db = AppDatabase.getInstance(context)
                val currentUser = db.userDao().getUserByIdDirect(activeUserId) ?: return@launch
                val allClients = db.clientDao().getAllClientsDirect()

                // Notifications non lues strictement destinées à cet utilisateur (jamais les notifications de ses propres actions)
                val unreadNotifs = db.appNotificationDao().getRecentUnreadDirect().filter { notif ->
                    val isSender = notif.senderId?.equals(currentUser.id, ignoreCase = true) == true ||
                            notif.senderId?.removePrefix("USR-")?.equals(currentUser.id.removePrefix("USR-"), ignoreCase = true) == true ||
                            notif.senderId?.equals("USR-${currentUser.id}", ignoreCase = true) == true ||
                            (!notif.senderName.isNullOrBlank() && currentUser.fullName.isNotBlank() && notif.senderName!!.trim().equals(currentUser.fullName.trim(), ignoreCase = true)) ||
                            (currentUser.email.isNotEmpty() && (notif.senderId?.equals(currentUser.email, ignoreCase = true) == true || notif.senderName?.equals(currentUser.email, ignoreCase = true) == true)) ||
                            (currentUser.phone.isNotEmpty() && (notif.senderId?.contains(currentUser.phone) == true || notif.senderName?.contains(currentUser.phone) == true)) ||
                            (currentUser.fullName.isNotBlank() && (notif.title.contains("Message de ${currentUser.fullName}", ignoreCase = true) || notif.title.contains("par ${currentUser.fullName}", ignoreCase = true)))

                    !isSender && com.example.util.ChatSecurityHelper.isNotificationConcernedForUser(notif, currentUser, allClients)
                }

                for (notif in unreadNotifs.take(3)) {
                    NotificationHelper.postSystemNotification(
                        context = context,
                        title = notif.title,
                        message = notif.message,
                        notificationId = notif.id.hashCode(),
                        targetRoute = notif.actionRoute ?: "notifications"
                    )
                }

                // Vérifier s'il y a des messages de chat entrants non lus (jamais ceux envoyés par l'utilisateur connecté)
                val allMessages = db.chatMessageDao().getAllMessagesDirect()
                val incomingUnreadChat = allMessages.count { msg ->
                    msg.senderId != currentUser.id &&
                            msg.senderId.removePrefix("USR-") != currentUser.id.removePrefix("USR-") &&
                            !msg.isRead &&
                            com.example.util.ChatSecurityHelper.canUserAccessChatRoom(currentUser, msg.clientId, allClients)
                }
                if (incomingUnreadChat > 0) {
                    NotificationHelper.postSystemNotification(
                        context = context,
                        title = "Nouveau message Chat MALAKI",
                        message = "Vous avez $incomingUnreadChat nouveau(x) message(s) reçu(s).",
                        notificationId = 99881,
                        targetRoute = "chat"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la vérification des notifications en arrière-plan", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
