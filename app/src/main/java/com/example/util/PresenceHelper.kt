package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PresenceHelper {

    fun formatPresence(
        isOnline: Boolean,
        lastActiveTimestamp: Long,
        now: Long = System.currentTimeMillis(),
        lang: AppLanguage = AppLanguage.FRENCH
    ): String {
        if (isOnline) {
            return if (lang == AppLanguage.SWAHILI) "Yuko mtandaoni" else "En ligne"
        }

        val diffMillis = (now - lastActiveTimestamp).coerceAtLeast(0L)
        val diffMinutes = diffMillis / 60000L

        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calLast = Calendar.getInstance().apply { timeInMillis = lastActiveTimestamp }

        val isToday = calNow.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calLast.get(Calendar.DAY_OF_YEAR)

        val isYesterday = calNow.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) - calLast.get(Calendar.DAY_OF_YEAR) == 1

        val hour = calLast.get(Calendar.HOUR_OF_DAY)
        val minute = String.format(Locale.getDefault(), "%02d", calLast.get(Calendar.MINUTE))

        return when {
            diffMinutes < 1 -> {
                if (lang == AppLanguage.SWAHILI) "Mtandaoni sasa hivi" else "En ligne à l'instant"
            }
            diffMinutes < 60 -> {
                if (lang == AppLanguage.SWAHILI) "Alikuwa mtandaoni dakika $diffMinutes zilizopita"
                else "En ligne il y a $diffMinutes min"
            }
            isToday -> {
                if (lang == AppLanguage.SWAHILI) "Alikuwa mtandaoni leo saa ${hour}h ${minute}Min"
                else "En ligne aujourd'hui à ${hour}h ${minute}Min"
            }
            isYesterday -> {
                if (lang == AppLanguage.SWAHILI) "Alikuwa mtandaoni jana saa ${hour}h ${minute}Min"
                else "En ligne hier à ${hour}h ${minute}Min"
            }
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM", Locale.FRANCE).format(Date(lastActiveTimestamp))
                if (lang == AppLanguage.SWAHILI) "Alikuwa mtandaoni $dateFormat saa ${hour}h ${minute}Min"
                else "En ligne le $dateFormat à ${hour}h ${minute}Min"
            }
        }
    }

    /**
     * Compact presence label for user badges and avatar chips
     */
    fun formatPresenceShort(
        isOnline: Boolean,
        lastActiveTimestamp: Long,
        now: Long = System.currentTimeMillis(),
        lang: AppLanguage = AppLanguage.FRENCH
    ): String {
        if (isOnline) {
            return if (lang == AppLanguage.SWAHILI) "Mtandaoni" else "En ligne"
        }

        val diffMillis = (now - lastActiveTimestamp).coerceAtLeast(0L)
        val diffMinutes = diffMillis / 60000L

        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calLast = Calendar.getInstance().apply { timeInMillis = lastActiveTimestamp }

        val isToday = calNow.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calLast.get(Calendar.DAY_OF_YEAR)

        val isYesterday = calNow.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) - calLast.get(Calendar.DAY_OF_YEAR) == 1

        val hour = calLast.get(Calendar.HOUR_OF_DAY)
        val minute = String.format(Locale.getDefault(), "%02d", calLast.get(Calendar.MINUTE))

        return when {
            diffMinutes < 1 -> if (lang == AppLanguage.SWAHILI) "Sasa" else "À l'instant"
            diffMinutes < 60 -> if (lang == AppLanguage.SWAHILI) "$diffMinutes m" else "Il y a $diffMinutes min"
            isToday -> if (lang == AppLanguage.SWAHILI) "Leo ${hour}h$minute" else "Aujourd'hui à ${hour}h$minute"
            isYesterday -> if (lang == AppLanguage.SWAHILI) "Jana ${hour}h$minute" else "Hier à ${hour}h$minute"
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM", Locale.FRANCE).format(Date(lastActiveTimestamp))
                "$dateFormat à ${hour}h$minute"
            }
        }
    }
}
