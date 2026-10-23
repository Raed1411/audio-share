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
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtil {

    /**
     * Validates whether an IPv4 address is a real local address and not a dummy/loopback address.
     */
    private fun isValidLocalIp(ip: String): Boolean {
        if (ip.isBlank()) return false
        if (ip.startsWith("127.")) return false
        if (ip.startsWith("192.0.0.")) return false // Dummy IANA address on some Android carriers
        if (ip.startsWith("169.254.")) return false // Link-local
        if (ip == "0.0.0.0" || ip == "255.255.255.255") return false
        return true
    }

    /**
     * Retrieves all valid IPv4 addresses from all active network interfaces.
     */
    fun getAllLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
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
                        if (hostAddress != null && isValidLocalIp(hostAddress)) {
                            ips.add(hostAddress)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ips
    }

    /**
     * Retrieves the primary local device IPv4 address.
     */
    fun getLocalWifiIpAddress(context: Context): String? {
        val allIps = getAllLocalIpAddresses()
        // Prefer private IPv4 address ranges: 192.168.x.x, 10.x.x.x, 172.16-31.x.x
        val privateIp = allIps.firstOrNull { ip ->
            ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")
        }
        if (privateIp != null) return privateIp

        return allIps.firstOrNull()
    }
}
