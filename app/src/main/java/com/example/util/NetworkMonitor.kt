package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observateur de connectivité réseau pour MALISHO NA AKIBA (MALAKI).
 * Détecte en temps réel la disponibilité d'une connexion Internet active (Wi-Fi, 4G/5G, etc.)
 * et déclenche automatiquement la synchronisation avec Neon PostgreSQL Cloud dès le rétablissement du réseau.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(isCurrentlyConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var onNetworkAvailableListener: (() -> Unit)? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Connexion Internet détectée (onAvailable)")
            _isOnline.value = true
            try {
                onNetworkAvailableListener?.invoke()
            } catch (e: Exception) {
                Log.w(TAG, "Erreur callback réseau : ${e.message}")
            }
        }

        override fun onLost(network: Network) {
            val connected = isCurrentlyConnected()
            Log.d(TAG, "Connexion perdue (onLost). Statut actuel : $connected")
            _isOnline.value = connected
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _isOnline.value = hasInternet
            if (hasInternet) {
                try {
                    onNetworkAvailableListener?.invoke()
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur callback capabilities : ${e.message}")
                }
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'enregistrer le NetworkCallback : ${e.message}")
            _isOnline.value = isCurrentlyConnected()
        }
    }

    fun isCurrentlyConnected(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun setOnNetworkAvailableListener(listener: () -> Unit) {
        this.onNetworkAvailableListener = listener
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
