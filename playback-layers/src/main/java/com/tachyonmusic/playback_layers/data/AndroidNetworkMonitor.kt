package com.tachyonmusic.playback_layers.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.tachyonmusic.playback_layers.domain.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


class AndroidNetworkMonitor(
    @ApplicationContext context: Context
) : NetworkMonitor {
    override val networkConnectionState = callbackFlow {
        var lastNetworkInfo =
            NetworkMonitor.NetworkInfo(connectionStatus = NetworkMonitor.ConnectionStatus.Disconnected)

        val callback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                lastNetworkInfo =
                    lastNetworkInfo.copy(connectionStatus = NetworkMonitor.ConnectionStatus.Available)
                trySend(lastNetworkInfo)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                lastNetworkInfo =
                    lastNetworkInfo.copy(connectionStatus = NetworkMonitor.ConnectionStatus.Disconnecting)
                trySend(lastNetworkInfo)
            }

            override fun onLost(network: Network) {
                lastNetworkInfo =
                    lastNetworkInfo.copy(connectionStatus = NetworkMonitor.ConnectionStatus.Disconnected)
                trySend(lastNetworkInfo)
            }

            override fun onUnavailable() {
                lastNetworkInfo =
                    lastNetworkInfo.copy(connectionStatus = NetworkMonitor.ConnectionStatus.Disconnected)
                trySend(lastNetworkInfo)
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isMetered =
                    !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                val isConnected =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                lastNetworkInfo = lastNetworkInfo.copy(
                    connectionStatus = if (isConnected) NetworkMonitor.ConnectionStatus.Connected else NetworkMonitor.ConnectionStatus.Disconnected,
                    isMetered = isMetered
                )
                trySend(lastNetworkInfo)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            // TODO: What to do here?
            val networkInfo = connectivityManager.activeNetworkInfo

            lastNetworkInfo = NetworkMonitor.NetworkInfo(
                connectionStatus = if (networkInfo != null && networkInfo.isConnectedOrConnecting)
                    NetworkMonitor.ConnectionStatus.Connected
                else NetworkMonitor.ConnectionStatus.Disconnected,
                isMetered = networkInfo?.isRoaming == true
            )

            trySend(lastNetworkInfo)
        }

        awaitClose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}