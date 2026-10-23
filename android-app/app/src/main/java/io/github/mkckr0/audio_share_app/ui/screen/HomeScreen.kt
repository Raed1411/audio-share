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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.mkckr0.audio_share_app.R
import io.github.mkckr0.audio_share_app.service.AudioPlayer
import io.github.mkckr0.audio_share_app.ui.MainActivity
import io.github.mkckr0.audio_share_app.ui.screen.HomeScreenViewModel.UiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()

    when (val uiState = viewModel.uiState.collectAsStateWithLifecycle().value) {
        UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Success -> {
            var host by remember(uiState.host) { mutableStateOf(uiState.host) }
            var port by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
            var started by remember { mutableStateOf(false) }

            val isHostError by remember { derivedStateOf { host.isEmpty() } }
            val isPortError by remember { derivedStateOf { port.isEmpty() } }

            val mainScrollState = rememberScrollState()

            // Auto-fill host with detected device IP when host is empty or default prefix
            LaunchedEffect(uiState.deviceIp) {
                val devIp = uiState.deviceIp
                if (devIp != null && (host.isEmpty() || host == "192.168." || host.endsWith("."))) {
                    host = devIp
                    val currentPort = port.toIntOrNull() ?: 65530
                    viewModel.saveNetWorkSettings(devIp, currentPort)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(mainScrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Connection Status Hero Header Card
                StatusHeroCard(started = started)

                // 2. Wi-Fi & Local Network Auto-Discovery Card
                NetworkDiscoveryCard(
                    deviceIp = uiState.deviceIp,
                    isScanning = uiState.isScanning,
                    discoveredServers = uiState.discoveredServers,
                    onScanClick = {
                        val currentPort = port.toIntOrNull() ?: 65530
                        viewModel.scanNetwork(currentPort, host)
                    },
                    onServerSelected = { serverIp ->
                        host = serverIp
                        val currentPort = port.toIntOrNull() ?: 65530
                        viewModel.saveNetWorkSettings(serverIp, currentPort)
                    },
                    onAutoDetectIpClick = {
                        val devIp = uiState.deviceIp
                        if (devIp != null) {
                            host = devIp
                            val currentPort = port.toIntOrNull() ?: 65530
                            viewModel.saveNetWorkSettings(devIp, currentPort)
                        } else {
                            viewModel.autoDetectLocalIp()
                        }
                    }
                )

                // 3. Server Host & Port Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_host),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (uiState.deviceIp != null && !started) {
                                FilterChip(
                                    selected = host == uiState.deviceIp,
                                    onClick = {
                                        host = uiState.deviceIp
                                        val currentPort = port.toIntOrNull() ?: 65530
                                        viewModel.saveNetWorkSettings(uiState.deviceIp, currentPort)
                                    },
                                    label = { Text(stringResource(R.string.label_auto_detect_ip)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = {
                                    if (it.isEmpty() || !it.contains(Regex("\\s"))) {
                                        host = it
                                    }
                                },
                                enabled = !started,
                                isError = isHostError,
                                label = { Text(stringResource(R.string.label_host)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Row {
                                        if (uiState.deviceIp != null && host != uiState.deviceIp && !started) {
                                            IconButton(onClick = {
                                                host = uiState.deviceIp
                                                val currentPort = port.toIntOrNull() ?: 65530
                                                viewModel.saveNetWorkSettings(uiState.deviceIp, currentPort)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoFixHigh,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (host.isNotEmpty() && !started) {
                                            IconButton(onClick = { host = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(0.68f),
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = port,
                                onValueChange = {
                                    if (it.isEmpty() || (it.isDigitsOnly() && it.toInt() in 1..65535)) {
                                        port = it
                                    }
                                },
                                enabled = !started,
                                isError = isPortError,
                                label = { Text(stringResource(R.string.label_port)) },
                                modifier = Modifier.weight(0.32f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }

                // 4. Primary Streaming Action Button
                val buttonContainerColor by animateColorAsState(
                    targetValue = if (started) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    label = "buttonContainerColor"
                )
                val buttonContentColor by animateColorAsState(
                    targetValue = if (started) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    label = "buttonContentColor"
                )

                Button(
                    onClick = {
                        if (isHostError || isPortError) return@Button
                        scope.launch {
                            if (started) {
                                activity.awaitMediaController().stop()
                            } else {
                                try {
                                    viewModel.saveNetWorkSettings(host, port.toInt()).join()
                                    activity.awaitMediaController().play()
                                } catch (_: NumberFormatException) {
                                    return@launch
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonContentColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (started) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (started) stringResource(R.string.label_stop_streaming)
                            else stringResource(R.string.label_start_streaming),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. Activity Logs Card
                ActivityLogsCard()
            }

            LifecycleStartEffect(true) {
                val listener = object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        started = playWhenReady
                    }
                }

                scope.launch {
                    activity.awaitMediaController().run {
                        started = playWhenReady
                        addListener(listener)
                    }
                }

                onStopOrDispose {
                    scope.launch {
                        activity.awaitMediaController().removeListener(listener)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeroCard(started: Boolean) {
    val cardContainerColor by animateColorAsState(
        targetValue = if (started) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "heroCardColor"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardContainerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (started) stringResource(R.string.label_status_streaming)
                    else stringResource(R.string.label_status_disconnected),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (started) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AudioPlayer.message.ifEmpty {
                        if (started) stringResource(R.string.label_started)
                        else stringResource(R.string.label_stopped)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (started) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (started) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (started) Icons.Default.Wifi else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (started) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkDiscoveryCard(
    deviceIp: String?,
    isScanning: Boolean,
    discoveredServers: List<String>,
    onScanClick: () -> Unit,
    onServerSelected: (String) -> Unit,
    onAutoDetectIpClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onAutoDetectIpClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (deviceIp != null) stringResource(R.string.label_device_ip, deviceIp)
                            else stringResource(R.string.label_auto_detect_ip),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onScanClick,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.label_scanning), fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.label_scan_network), fontSize = 12.sp)
                    }
                }
            }

            AnimatedVisibility(
                visible = isScanning || discoveredServers.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_discovered_servers),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (discoveredServers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            discoveredServers.forEach { serverIp ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onServerSelected(serverIp) },
                                    label = { Text(serverIp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Computer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else if (isScanning) {
                        Text(
                            text = stringResource(R.string.label_scanning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogsCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_logs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = AudioPlayer.message.ifEmpty { "Ready..." },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
