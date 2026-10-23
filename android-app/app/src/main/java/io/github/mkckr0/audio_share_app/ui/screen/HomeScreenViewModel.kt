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

package io.github.mkckr0.audio_share_app.ui.screen

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mkckr0.audio_share_app.R
import io.github.mkckr0.audio_share_app.model.NetworkConfigKeys
import io.github.mkckr0.audio_share_app.model.NetworkUtil
import io.github.mkckr0.audio_share_app.model.getInteger
import io.github.mkckr0.audio_share_app.model.networkConfigDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val host: String,
            val port: Int,
            val deviceIp: String?,
            val isScanning: Boolean,
            val discoveredServers: List<String>
        ) : UiState
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<String>>(emptyList())
    val discoveredServers = _discoveredServers.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        application.networkConfigDataStore.data,
        _isScanning,
        _discoveredServers
    ) { prefs, scanning, servers ->
        val defaultHost = application.getString(R.string.default_host)
        val savedHost = prefs[stringPreferencesKey(NetworkConfigKeys.HOST)]
        val deviceIp = NetworkUtil.getLocalWifiIpAddress(application)

        // Auto-detect IP prefix or full device IP if host is untouched
        val effectiveHost = if (savedHost.isNullOrEmpty() || savedHost == defaultHost) {
            if (deviceIp != null) {
                NetworkUtil.getSubnetPrefix(deviceIp)
            } else {
                defaultHost
            }
        } else {
            savedHost
        }

        val port = prefs[intPreferencesKey(NetworkConfigKeys.PORT)]
            ?: application.getInteger(R.integer.default_port)

        UiState.Success(
            host = effectiveHost,
            port = port,
            deviceIp = deviceIp,
            isScanning = scanning,
            discoveredServers = servers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiState.Loading,
    )

    fun saveNetWorkSettings(host: String, port: Int): Job {
        return viewModelScope.launch {
            getApplication<Application>().networkConfigDataStore.edit {
                it[stringPreferencesKey(NetworkConfigKeys.HOST)] = host
                it[intPreferencesKey(NetworkConfigKeys.PORT)] = port
            }
        }
    }

    fun scanNetwork(port: Int) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _discoveredServers.value = emptyList()

            val app = getApplication<Application>()
            val deviceIp = NetworkUtil.getLocalWifiIpAddress(app)
            val subnetPrefix = if (deviceIp != null) {
                NetworkUtil.getSubnetPrefix(deviceIp)
            } else {
                "192.168.1."
            }

            val serversList = mutableListOf<String>()
            NetworkUtil.scanLocalSubnetForServers(
                subnetPrefix = subnetPrefix,
                port = port,
                timeoutMs = 250,
                onServerFound = { newServerIp ->
                    if (!serversList.contains(newServerIp)) {
                        serversList.add(newServerIp)
                        _discoveredServers.value = serversList.toList()
                    }
                }
            )

            // Auto-select first discovered server if found and current host is prefix/empty
            if (serversList.isNotEmpty()) {
                val firstServer = serversList.first()
                saveNetWorkSettings(firstServer, port)
            }

            _isScanning.value = false
        }
    }

    fun autoDetectLocalIp() {
        val app = getApplication<Application>()
        val deviceIp = NetworkUtil.getLocalWifiIpAddress(app)
        if (deviceIp != null) {
            val prefix = NetworkUtil.getSubnetPrefix(deviceIp)
            saveNetWorkSettings(prefix, app.getInteger(R.integer.default_port))
        }
    }
}
