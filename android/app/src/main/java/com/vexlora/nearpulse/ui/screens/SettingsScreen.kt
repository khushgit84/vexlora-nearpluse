package com.vexlora.nearpulse.ui.screens

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
    viewModel: NearPulseViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "SETTINGS & DIAGNOSTICS",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware & Bluetooth Diagnostics Card
        Text(text = "BLUETOOTH HARDWARE STACK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingStatusRow(label = "Bluetooth Hardware Adapter", status = if (viewModel.bleManager.isBleSupported()) "Detected" else "Unavailable", isGood = viewModel.bleManager.isBleSupported())
                Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingStatusRow(label = "Bluetooth State", status = if (viewModel.bleManager.isBluetoothEnabled()) "Enabled" else "Turned Off", isGood = viewModel.bleManager.isBluetoothEnabled())
                Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingStatusRow(label = "BLE Peripheral Advertising", status = if (viewModel.bleManager.isAdvertisingSupported()) "Supported" else "Not Supported", isGood = viewModel.bleManager.isAdvertisingSupported())
                Divider(color = CyberBorder, modifier = Modifier.padding(vertical = 8.dp))
                SettingStatusRow(label = "Target Service UUID", status = "7b14d2e0 (128-bit LE)", isGood = true)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy & Rotating Pseudonyms
        Text(text = "PRIVACY & EPHEMERAL IDENTITY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Active Pseudonym", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = viewModel.bleManager.localPseudonym,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                    Button(
                        onClick = { viewModel.rotatePseudonym() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Rotate ID", color = ElectricCyan, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VEXLORA NearPulse never transmits your phone number, IMEI, MAC address, or contact book in BLE packets.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Android 12+ Permission Declarations Info
        Text(text = "SECURITY & OS PERMISSION MODEL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                PermissionInfoItem(name = "BLUETOOTH_SCAN", note = "neverForLocation flag active: zero GPS tracking")
                PermissionInfoItem(name = "BLUETOOTH_ADVERTISE", note = "Permits broadcasting emergency SOS beacons")
                PermissionInfoItem(name = "BLUETOOTH_CONNECT", note = "Permits P2P GATT encrypted message transfers")
                PermissionInfoItem(name = "POST_NOTIFICATIONS", note = "Emergency SOS acoustic & siren vibration alerts")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Danger Zone: Clear Local Database
        Text(text = "LOCAL DATA MANAGEMENT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "All chat history and peer sightings are stored strictly in local Room SQLite storage on this handset.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SosRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = SosRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Clear All Local Messages & History", color = SosRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Vexlora NearPulse
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "VEXLORA NearPulse v1.0.0 (Build 100)", fontSize = 12.sp, color = TextMuted)
            Text(text = "Engineered for zero-network resilience", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Confirm Dialog
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Erase Local NearPulse Data?") },
                text = { Text("This will permanently remove all cached messages, peer encounter logs, and circle memberships from this phone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearLocalData()
                            showClearConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                    ) {
                        Text("Erase Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun SettingStatusRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextPrimary)
        Text(text = status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isGood) SignalGreen else SosRed)
    }
}

@Composable
fun PermissionInfoItem(name: String, note: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElectricCyan)
        Text(text = note, fontSize = 11.sp, color = TextMuted)
    }
}
