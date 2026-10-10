package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun SettingsScreen(
    viewModel: NearPulseViewModel
) {
    val currentPseudonym by viewModel.localPseudonym.collectAsState()
    val allPeers by viewModel.allDbPeers.collectAsState()
    val allMessages by viewModel.allDbMessages.collectAsState()

    var pseudonymInput by remember { mutableStateOf(currentPseudonym) }
    var showSavedToast by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var testNotifStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "SETTINGS & HARDWARE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = ElectricCyan
            )
            Text(
                text = "BLE STACK & LOCAL STORAGE DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        // Identity Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LOCAL CALLSIGN / PSEUDONYM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Visible to nearby peers in the BLE broadcast beacon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = pseudonymInput,
                        onValueChange = { if (it.length <= 12) pseudonymInput = it.uppercase() },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCardElevated,
                            unfocusedContainerColor = CyberCardElevated,
                            focusedTextColor = ElectricCyan,
                            unfocusedTextColor = ElectricCyan,
                            cursorColor = ElectricCyan
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            viewModel.updatePseudonym(pseudonymInput)
                            showSavedToast = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE", color = CyberBgDark, fontWeight = FontWeight.Bold)
                    }
                }

                if (showSavedToast) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Callsign updated successfully!", color = EmeraldGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Hardware Diagnostics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BLE HARDWARE CAPABILITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                DiagnosticRow(label = "Bluetooth Hardware Adapter", value = if (viewModel.isBleSupported()) "SUPPORTED" else "UNAVAILABLE", isGood = viewModel.isBleSupported())
                DiagnosticRow(label = "Bluetooth Radio Status", value = if (viewModel.isBluetoothEnabled()) "ENABLED" else "DISABLED", isGood = viewModel.isBluetoothEnabled())
                DiagnosticRow(label = "Multiple BLE Advertisements", value = if (viewModel.isAdvertisingSupported()) "SUPPORTED" else "LIMITED", isGood = viewModel.isAdvertisingSupported())
                DiagnosticRow(label = "GATT Protocol MTU", value = "512 BYTES", isGood = true)
                DiagnosticRow(label = "Operating Frequency", value = "2.402 - 2.480 GHz ISM", isGood = true)
                DiagnosticRow(label = "Privacy Flags", value = "neverForLocation (Zero GPS)", isGood = true)
            }
        }

        // Offline Database Storage Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LOCAL ROOM PERSISTENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discovered Peers Cached", color = TextSecondary)
                    Text("${allPeers.size}", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Offline Message Logs", color = TextSecondary)
                    Text("${allMessages.size}", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showWipeConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CrimsonRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PURGE LOCAL DATABASE", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Notification & Alert Drill Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTIFICATIONS & ALERT DRILL",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verify heads-up system notifications, sender name, message preview, sound, and vibration when outside the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Test now
                Button(
                    onClick = {
                        viewModel.testMessageNotification(0)
                        testNotifStatus = "Direct message notification posted! Check status bar."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = CyberBgDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST CHAT NOTIFICATION NOW", color = CyberBgDark, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test in 5s (minimize app)
                OutlinedButton(
                    onClick = {
                        viewModel.testMessageNotification(5)
                        testNotifStatus = "Triggering in 5s! Minimize or lock phone now to test outside the app!"
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST IN 5 SECONDS (MINIMIZE APP)", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test SOS
                OutlinedButton(
                    onClick = {
                        viewModel.testEmergencyAlarm(0)
                        testNotifStatus = "Emergency SOS siren & Morse vibration triggered!"
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CrimsonRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST EMERGENCY SOS ALARM", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }

                if (testNotifStatus != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testNotifStatus!!,
                        color = EmeraldGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Decentralization Notice
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AIR-GAPPED ARCHITECTURE", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VEXLORA NearPulse requires zero internet connectivity, zero user logins, and zero cloud backends. All communication happens exclusively between peer device Bluetooth Low Energy radios within 2.4 GHz physical proximity.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version 1.0.0 • VEXLORA Mesh Network",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Purge Local Database?", color = CrimsonRed, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently clear all cached peer encounters and message logs stored in local SQLite Room storage on this phone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("WIPE DATA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = CyberCard
        )
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = if (isGood) EmeraldGreen else CrimsonRed,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}