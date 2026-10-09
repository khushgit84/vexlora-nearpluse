package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onBack: () -> Unit
) {
    val circle by viewModel.safetyCircle.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var circleNameInput by remember { mutableStateOf("") }
    var circleCodeInput by remember { mutableStateOf("") }

    // Filter peers that advertise or match current circle code
    val circleMembers = remember(peersMap, circle) {
        if (circle == null) emptyList()
        else peersMap.values.filter { it.circleCode == circle?.circleCode || it.circleCode != null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "SAFETY CIRCLE", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(text = "Local offline group mesh roster", style = MaterialTheme.typography.labelSmall, color = SignalGreen)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Range & OS Restrictions Disclaimer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceElevated,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SignalYellow, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Safety Circles operate solely over direct peer BLE. Members appear online only when within ~20m range and advertising.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (circle == null) {
            // No Circle Created State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Create or Join a Safety Circle", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Synchronize offline with your hiking party, emergency response team, or family members using peer invitation codes.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SignalGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Create Circle", color = CyberBgDark, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showJoinDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Enter Code", color = SignalGreen)
                        }
                    }
                }
            }
        } else {
            // Active Circle Card
            val activeCircle = circle!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SignalGreen.copy(alpha = 0.5f)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = activeCircle.circleName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(text = "Circle Code: ${activeCircle.circleCode}", style = MaterialTheme.typography.labelSmall, color = SignalGreen, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.leaveSafetyCircle() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = SosRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR / Code Share Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberSurfaceElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "SHARE THIS CODE OFFLINE", fontSize = 10.sp, color = TextMuted)
                                Text(text = activeCircle.circleCode, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = 2.sp)
                            }
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Share", tint = ElectricCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CIRCLE MEMBERS (${circleMembers.size + 1})",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Self Item
                item {
                    MemberCard(
                        name = "${viewModel.bleManager.localPseudonym} (You)",
                        status = "Broadcasting Circle Beacon",
                        isNearby = true,
                        isSelf = true
                    )
                }

                // Discovered Circle Peers
                items(circleMembers) { peer ->
                    MemberCard(
                        name = peer.pseudonym,
                        status = "${peer.signalCategory.label} (${peer.rssi} dBm)",
                        isNearby = peer.rssi >= -90,
                        isSelf = false
                    )
                }
            }
        }

        // Dialog for Create Circle
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Local Safety Circle") },
                text = {
                    Column {
                        Text("Enter a name for your local circle:", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = circleNameInput,
                            onValueChange = { circleNameInput = it },
                            placeholder = { Text("e.g. Expedition Squad") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SignalGreen)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createSafetyCircle(circleNameInput)
                            showCreateDialog = false
                            circleNameInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)
                    ) {
                        Text("Create", color = CyberBgDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Dialog for Join Circle
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("Join Safety Circle") },
                text = {
                    Column {
                        Text("Enter the 6-character circle code from the group creator:", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = circleCodeInput,
                            onValueChange = { circleCodeInput = it },
                            placeholder = { Text("e.g. NP-4921") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SignalGreen)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (circleCodeInput.isNotBlank()) {
                                viewModel.joinSafetyCircle(circleCodeInput)
                                showJoinDialog = false
                                circleCodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)
                    ) {
                        Text("Join", color = CyberBgDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun MemberCard(name: String, status: String, isNearby: Boolean, isSelf: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isNearby) SignalGreen else TextMuted)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleLarge, fontSize = 14.sp, color = TextPrimary)
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = if (isNearby) SignalGreen else TextMuted, fontSize = 11.sp)
            }
            if (isSelf) {
                Text(
                    text = "HOST",
                    color = ElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
