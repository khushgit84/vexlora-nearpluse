package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ble.DiscoveredPeer
import com.vexlora.nearpulse.ble.SignalCategory
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    viewModel: NearPulseViewModel,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onBack: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()
    val peersList = remember(peersMap) { peersMap.values.toList() }

    // Radar beam sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "BLUETOOTH RADAR", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(text = "Filtered to NearPulse 128-bit BLE service", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
            IconButton(onClick = { viewModel.toggleDiscovery() }) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Toggle Scan",
                    tint = if (isScanning) ElectricCyan else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Radar Canvas Visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceDark)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) / 2f - 10.dp.toPx()

                // Concentric Range Rings
                val ringCount = 3
                for (i in 1..ringCount) {
                    val r = maxRadius * (i.toFloat() / ringCount)
                    drawCircle(
                        color = CyberBorder,
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Crosshairs
                drawLine(
                    color = CyberBorder.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = CyberBorder.copy(alpha = 0.5f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )

                // Rotating Scanner Sweep Beam
                if (isScanning) {
                    val angleRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        x = center.x + (maxRadius * cos(angleRad)).toFloat(),
                        y = center.y + (maxRadius * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricCyan, ElectricCyan.copy(alpha = 0f)),
                            center = center,
                            radius = maxRadius
                        ),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Origin Point
                drawCircle(color = ElectricCyan, radius = 4.dp.toPx(), center = center)

                // Plot detected peers
                peersList.forEachIndexed { idx, peer ->
                    val angle = (idx * (360f / maxOf(peersList.size, 1))) + 30f
                    val angleR = Math.toRadians(angle.toDouble())
                    // RSSI mapping to relative radar radius: -40dBm close to center, -100dBm near outer edge
                    val normalizedRssi = ((peer.rssi + 105).coerceIn(10, 75)) / 75f
                    val peerRadius = maxRadius * (1f - normalizedRssi * 0.7f)
                    val pX = center.x + (peerRadius * cos(angleR)).toFloat()
                    val pY = center.y + (peerRadius * sin(angleR)).toFloat()

                    val dotColor = if (peer.isSosActive) SosRed else ElectricCyan
                    drawCircle(
                        color = dotColor.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = Offset(pX, pY)
                    )
                    drawCircle(
                        color = dotColor,
                        radius = 4.dp.toPx(),
                        center = Offset(pX, pY)
                    )
                }
            }

            // Radar Status Chip
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBgDark.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isScanning) "SCANNING (BALANCED LE)" else "RADAR PAUSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isScanning) ElectricCyan else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RSSI Scientific Disclaimer Notice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceElevated,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RSSI reflects received RF signal attenuation, not metric distance. Walls, body absorption, and phone antennas affect readings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Discovered Peers List
        Text(
            text = "DISCOVERED PEERS (${peersList.size})",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (peersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No NearPulse peers advertising nearby",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Ensure other device has NearPulse open & Bluetooth turned ON",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(peersList) { peer ->
                    PeerRadarCard(peer = peer, onClick = { onPeerSelected(peer) })
                }
            }
        }
    }
}

@Composable
fun PeerRadarCard(peer: DiscoveredPeer, onClick: () -> Unit) {
    val signalColor = when (peer.signalCategory) {
        SignalCategory.EXCELLENT -> SignalGreen
        SignalCategory.STRONG -> ElectricCyan
        SignalCategory.MODERATE -> SignalYellow
        SignalCategory.WEAK -> SignalOrange
        SignalCategory.MARGINAL -> SosRed
    }

    val secondsAgo = ((System.currentTimeMillis() - peer.lastSeenTimestamp) / 1000).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (peer.isSosActive) SosRed.copy(alpha = 0.15f) else CyberSurfaceDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(if (peer.isSosActive) SosRed else CyberBorder, CyberBorder)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (peer.isSosActive) SosRed.copy(alpha = 0.2f) else CyberSurfaceElevated)
                    .border(1.dp, if (peer.isSosActive) SosRed else CyberBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (peer.isSosActive) Icons.Default.Warning else Icons.Default.PermDeviceInformation,
                    contentDescription = null,
                    tint = if (peer.isSosActive) SosRed else ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peer.pseudonym,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (peer.isSosActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SOS",
                            color = SosRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(SosRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${peer.signalCategory.label} (${peer.rssi} dBm)",
                        style = MaterialTheme.typography.labelSmall,
                        color = signalColor,
                        fontSize = 11.sp
                    )
                    Text(text = " • ", color = TextMuted)
                    Text(
                        text = "🔋 ${peer.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (peer.batteryLevel <= 20) SosRed else SignalGreen,
                        fontSize = 11.sp
                    )
                    Text(text = " • ", color = TextMuted)
                    Text(
                        text = if (secondsAgo == 0L) "Just now" else "${secondsAgo}s ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Connect", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
