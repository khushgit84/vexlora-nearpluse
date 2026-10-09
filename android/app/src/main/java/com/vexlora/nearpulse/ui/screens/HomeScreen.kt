package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun HomeScreen(
    viewModel: NearPulseViewModel,
    onNavigateToRadar: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToCircle: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()
    val activeSos by viewModel.isSosBroadcasting.collectAsState()
    val incomingSosAlert by viewModel.activeSosAlert.collectAsState()

    // Pulse animation for radar badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VEXLORA",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NearPulse",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Pseudonym Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceDark)
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAdvertising) SignalGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.bleManager.localPseudonym,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        Text(
            text = "Stay Connected. Even Without Internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Incoming Emergency SOS Alert Notification Banner
        AnimatedVisibility(visible = incomingSosAlert != null) {
            incomingSosAlert?.let { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onNavigateToSos() },
                    colors = CardDefaults.cardColors(containerColor = SosRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SosRed, NeonViolet)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SosRed, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "EMERGENCY SOS DETECTED", color = SosRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${alert.broadcasterPseudonym} needs assistance within BLE range!", color = TextPrimary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.acknowledgeSos(alert) },
                            colors = ButtonDefaults.buttonColors(containerColor = SosRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "ACK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Discovery Power Center Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberBorder, if (isScanning) ElectricCyan else CyberBorder)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BLE MESH STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = if (isScanning) "DISCOVERY ACTIVE" else "STANDBY MODE",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isScanning) ElectricCyan else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .scale(if (isScanning) pulseScale else 1f)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) ElectricCyan.copy(alpha = 0.15f) else CyberSurfaceElevated)
                            .border(1.dp, if (isScanning) ElectricCyan else CyberBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = if (isScanning) ElectricCyan else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(title = "Nearby Peers", value = "${peersMap.size}", color = ElectricCyan)
                    MetricBox(title = "GATT Service", value = "128-bit LE", color = NeonViolet)
                    MetricBox(title = "Hardware", value = if (viewModel.bleManager.isAdvertisingSupported()) "ADV OK" else "LEGACY", color = SignalGreen)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { viewModel.toggleDiscovery() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) CyberSurfaceElevated else ElectricCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isScanning) TextPrimary else CyberBgDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Stop Discovery" else "Start Nearby Discovery",
                        color = if (isScanning) TextPrimary else CyberBgDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SOS Panic Button Callout
        Button(
            onClick = onNavigateToSos,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, if (activeSos) SosRed else SosRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activeSos) SosRed else SosRed.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sos,
                    contentDescription = null,
                    tint = if (activeSos) Color.White else SosRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (activeSos) "EMERGENCY SOS IS ACTIVE" else "EMERGENCY SOS CONSOLE",
                    color = if (activeSos) Color.White else SosRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Navigation Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavCard(
                title = "Bluetooth Radar",
                desc = "Real-time RF RSSI scan",
                icon = Icons.Default.Radar,
                tint = ElectricCyan,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToRadar
            )
            NavCard(
                title = "Nearby Chat",
                desc = "Offline GATT P2P text",
                icon = Icons.Default.Chat,
                tint = NeonViolet,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToChat
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavCard(
                title = "Safety Circle",
                desc = "Offline squad heartbeat",
                icon = Icons.Default.GroupWork,
                tint = SignalGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCircle
            )
            NavCard(
                title = "Hardware & Settings",
                desc = "Permissions & privacy",
                icon = Icons.Default.Settings,
                tint = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
fun MetricBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun NavCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(96.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberBorder, CyberBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = desc, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}
