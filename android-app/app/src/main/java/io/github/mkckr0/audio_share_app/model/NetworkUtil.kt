/*
 *    Copyright 2022-2024 mkckr0 <https://github.com/mkckr0>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.mkckr0.audio_share_app.model

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Locale

object NetworkUtil {

    /**
     * Retrieves the current device local IPv4 address on Wi-Fi or active network interface.
     */
    fun getLocalWifiIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo
            if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
                val ip = dhcpInfo.ipAddress
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
            }
        } catch (_: Exception) {
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        if (hostAddress != null && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    /**
     * Extracts the subnet prefix from an IP address (e.g. "192.168.1.15" -> "192.168.1.")
     */
    fun getSubnetPrefix(ipAddress: String): String {
        val lastDotIndex = ipAddress.lastIndexOf('.')
        return if (lastDotIndex != -1) {
            ipAddress.substring(0, lastDotIndex + 1)
        } else {
            "192.168.1."
        }
    }

    /**
     * Scans the local subnet for active Audio Share servers concurrently.
     */
    suspend fun scanLocalSubnetForServers(
        subnetPrefix: String,
        port: Int = 65530,
        timeoutMs: Int = 300,
        onServerFound: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        val prefix = if (subnetPrefix.endsWith(".")) subnetPrefix else "$subnetPrefix."
        val foundServers = mutableListOf<String>()

        coroutineScope {
            val deferreds = (1..254).map { hostSuffix ->
                async {
                    val candidateIp = "$prefix$hostSuffix"
                    if (isServerActiveAt(candidateIp, port, timeoutMs)) {
                        synchronized(foundServers) {
                            foundServers.add(candidateIp)
                        }
                        onServerFound(candidateIp)
                        candidateIp
                    } else {
                        null
                    }
                }
            }
            deferreds.awaitAll()
        }

        foundServers.sorted()
    }

    private fun isServerActiveAt(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
