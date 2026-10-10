package com.vexlora.nearpulse.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun SafetyCircleScreen(
    viewModel: NearPulseViewModel,
    onNavigateToChat: () -> Unit
) {
    val currentCircleCode by viewModel.circleCode.collectAsState()
    val allCircles by viewModel.safetyCircles.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCircleCodeInput by remember { mutableStateOf("") }
    var broadcastNotice by remember { mutableStateOf<String?>(null) }

    val circlePeers = remember(peersMap, currentCircleCode) {
        peersMap.values.filter { it.circleCode == currentCircleCode }
    }

    val presetBroadcasts = listOf(
        "ALL MEMBERS SAFE & ACCOUNTED FOR",
        "MEETING AT EXTRACTION POINT ALPHA",
        "RESOURCES DEPLETED (WATER/MEDS)",
        "STANDBY AND MAINTAIN CURRENT POSITION"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAFETY CIRCLES",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = EmeraldGreen
                )
                Text(
                    text = "OFFLINE ENCRYPTED TEAM MESH",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CyberBgDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SWITCH CIRCLE", color = CyberBgDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Circle Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE MESH GROUP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = currentCircleCode,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EmeraldGreenGlow)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${circlePeers.size} team member(s) currently detected in direct BLE proximity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Group Check-In Broadcast
        Text(
            text = "ONE-TAP CIRCLE CHECK-IN",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presetBroadcasts.forEach { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            circlePeers.forEach { peer ->
                                viewModel.selectPeer(peer)
                                viewModel.sendChatMessage("[$currentCircleCode] $message")
                            }
                            broadcastNotice = "Sent to ${circlePeers.size} circle peer(s)"
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        broadcastNotice?.let { notice ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(notice, color = EmeraldGreen, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Members in Circle
        Text(
            text = "DETECTED CIRCLE MEMBERS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (circlePeers.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No other devices in '$currentCircleCode' currently in range.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Set the same Circle Code on peer devices to connect instantly.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(circlePeers, key = { it.address }) { peer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectPeer(peer)
                                onNavigateToChat()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peer.pseudonym, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("RSSI: ${peer.rssi} dBm (${peer.signalCategory.distanceEstimate})", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.selectPeer(peer)
                                    onNavigateToChat()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = EmeraldGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    // Switch / Create Circle Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Set Safety Circle Code", color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-8 character code. Devices with this exact code will form an offline group mesh.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = newCircleCodeInput,
                        onValueChange = { if (it.length <= 10) newCircleCodeInput = it.uppercase() },
                        placeholder = { Text("e.g. OMEGA-7", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCardElevated,
                            unfocusedContainerColor = CyberCardElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCircleCodeInput.isNotBlank()) {
                            viewModel.setCircleCode(newCircleCodeInput)
                            showCreateDialog = false
                            newCircleCodeInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("JOIN CIRCLE", color = CyberBgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = CyberCard
        )
    }
}