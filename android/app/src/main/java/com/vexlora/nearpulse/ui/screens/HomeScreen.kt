package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ble.DiscoveredPeer
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun HomeScreen(
    viewModel: NearPulseViewModel,
    onNavigateToRadar: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToCircle: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val isSosActive by viewModel.isSosBroadcasting.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()
    val activeSosAlert by viewModel.activeSosAlert.collectAsState()
    val localPseudonym by viewModel.localPseudonym.collectAsState()
    val circleCode by viewModel.circleCode.collectAsState()
    val allMessages by viewModel.allDbMessages.collectAsState()

    val peersList = remember(peersMap) { peersMap.values.toList() }

    // Pulse animation for active broadcast
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VEXLORA NEARPULSE",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = ElectricCyan
                        )
                        Text(
                            text = "OFFLINE BLE DISCOVERY MESH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Identity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCardElevated)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = localPseudonym,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Active Emergency SOS Alert Banner if detected nearby
        item {
            AnimatedVisibility(visible = activeSosAlert != null) {
                activeSosAlert?.let { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, CrimsonRed, RoundedCornerShape(12.dp))
                            .clickable { onNavigateToSos() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency",
                                tint = CrimsonRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SOS DISTRESS DETECTED!",
                                    color = CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "From ${alert.senderPseudonym} (${alert.rssi} dBm)",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.dismissIncomingSos() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCardElevated),
                                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("SOLVED", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { onNavigateToSos() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("VIEW", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BLE Radio Controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RADIO CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Toggle Button
                        Button(
                            onClick = { viewModel.toggleScanning() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) EmeraldGreen else CyberCardElevated
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isScanning) Color.White else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isScanning) "SCANNING" else "START SCAN",
                                    color = if (isScanning) CyberBgDark else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Broadcast Toggle Button
                        Button(
                            onClick = { viewModel.toggleAdvertising() },
                            modifier = Modifier
                                .weight(1f)
                                .then(if (isAdvertising) Modifier.scale(pulseScale) else Modifier),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvertising) ElectricCyan else CyberCardElevated
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdvertising) CyberBgDark else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAdvertising) "BEACON ON" else "BROADCAST",
                                    color = if (isAdvertising) CyberBgDark else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Metric Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nearby Peers Counter
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToRadar() },
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElectricCyan.copy(alpha = 0.3f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PEERS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Icon(Icons.Default.Radar, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${peersList.size}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                        Text("in radio range", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                // Safety Circle Status
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToCircle() },
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EmeraldGreen.copy(alpha = 0.3f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CIRCLE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = circleCode,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen
                        )
                        Text("Active Group", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                // Stored Messages Counter
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToChat() },
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NeonAmber.copy(alpha = 0.3f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LOGS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${allMessages.size}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = NeonAmber
                        )
                        Text("saved offline", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        // Rapid Emergency SOS Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSosActive) CrimsonRed else CrimsonRed.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CrimsonRed, RoundedCornerShape(14.dp))
                    .clickable { onNavigateToSos() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "SOS",
                        tint = if (isSosActive) Color.White else CrimsonRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSosActive) "SOS BROADCAST TRANSMITTING" else "EMERGENCY RESCUE BEACON",
                            color = if (isSosActive) Color.White else CrimsonRed,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isSosActive) "High-gain BLE distress pulses firing" else "Tap to trigger 100% offline emergency distress signal",
                            color = if (isSosActive) Color.White.copy(alpha = 0.8f) else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = if (isSosActive) Color.White else CrimsonRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Live Discovered Peers Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED PEERS NEARBY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToRadar) {
                    Text("OPEN RADAR", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Peer items list
        if (peersList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isScanning) "Scanning 2.4 GHz spectrum..." else "Scanner idle. Tap 'START SCAN' above.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(peersList, key = { it.address }) { peer ->
                PeerListCard(
                    peer = peer,
                    onChatClick = {
                        viewModel.selectPeer(peer)
                        onNavigateToChat()
                    }
                )
            }
        }
    }
}

@Composable
fun PeerListCard(
    peer: DiscoveredPeer,
    onChatClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .clickable { onChatClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal Indicator Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            peer.isSosActive -> CrimsonRed
                            peer.rssi >= -65 -> EmeraldGreen
                            peer.rssi >= -80 -> ElectricCyan
                            else -> NeonAmber
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peer.pseudonym,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (peer.isSosActive) CrimsonRed else TextPrimary
                    )
                    if (peer.circleCode != null && peer.circleCode != "GENERAL") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldGreenGlow)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = peer.circleCode,
                                color = EmeraldGreen,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text(
                    text = "${peer.signalCategory.label} • ${peer.rssi} dBm (${peer.signalCategory.distanceEstimate})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onChatClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = CyberCardElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Chat",
                    tint = ElectricCyan
                )
            }
        }
    }
}