package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ble.DiscoveredPeer
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    viewModel: NearPulseViewModel,
    onNavigateToChat: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val peersList = remember(peersMap) { peersMap.values.toList() }

    // Continuous 360-degree radar sweep rotation
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Pulse wave for blips
    val blipPulse by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blipPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Radar Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RF SPECTRUM RADAR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = ElectricCyan
                )
                Text(
                    text = if (isScanning) "ACTIVE SCAN • 2.4 GHz BLE" else "SCANNER PAUSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isScanning) EmeraldGreen else TextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.toggleScanning() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isScanning) EmeraldGreen.copy(alpha = 0.2f) else CyberCardElevated
                )
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Scan",
                    tint = if (isScanning) EmeraldGreen else ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Radar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(peersList) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = (size.width.coerceAtMost(size.height) / 2f) - 20f

                            // Find tapped peer within 30dp radius
                            val tappedPeer = peersList.firstOrNull { peer ->
                                val angleDeg = (peer.address.hashCode() % 360).toFloat()
                                val angleRad = angleDeg * (PI / 180f).toFloat()
                                val normalizedDist = ((peer.rssi + 40).coerceIn(-60, 0) / -60f).coerceIn(0.15f, 0.95f)
                                val r = normalizedDist * maxRadius
                                val x = center.x + r * cos(angleRad)
                                val y = center.y + r * sin(angleRad)
                                val peerPos = Offset(x, y)
                                (tapOffset - peerPos).getDistance() <= 60f
                            }
                            if (tappedPeer != null) {
                                viewModel.selectPeer(tappedPeer)
                            }
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.width.coerceAtMost(size.height) / 2f) - 20f

                // Concentric Range Rings
                val ringFractions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                ringFractions.forEach { frac ->
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.15f),
                        radius = maxRadius * frac,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Crosshair axes
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1f
                )

                // Diagonal guide lines
                val diagOffset = maxRadius * 0.7071f
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.08f),
                    start = Offset(center.x - diagOffset, center.y - diagOffset),
                    end = Offset(center.x + diagOffset, center.y + diagOffset),
                    strokeWidth = 1f
                )
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.08f),
                    start = Offset(center.x - diagOffset, center.y + diagOffset),
                    end = Offset(center.x + diagOffset, center.y - diagOffset),
                    strokeWidth = 1f
                )

                // Sweeping Radar Line with gradient cone
                if (isScanning) {
                    val sweepRad = sweepAngle * (PI / 180f).toFloat()
                    val sweepEnd = Offset(
                        center.x + maxRadius * cos(sweepRad),
                        center.y + maxRadius * sin(sweepRad)
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                ElectricCyan.copy(alpha = 0.12f),
                                ElectricCyan.copy(alpha = 0.35f)
                            ),
                            center = center
                        ),
                        startAngle = sweepAngle - 45f,
                        sweepAngle = 45f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                    )

                    drawLine(
                        color = ElectricCyan,
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2f
                    )
                }

                // Center Origin Point (You)
                drawCircle(color = ElectricCyan, radius = 6f, center = center)
                drawCircle(color = ElectricCyan.copy(alpha = 0.3f), radius = 12f, center = center)

                // Plot Discovered Peer Blips
                peersList.forEach { peer ->
                    val angleDeg = (peer.address.hashCode() % 360).toFloat()
                    val angleRad = angleDeg * (PI / 180f).toFloat()
                    // -40 dBm (close) to -100 dBm (far)
                    val normalizedDist = ((peer.rssi + 40).coerceIn(-60, 0) / -60f).coerceIn(0.18f, 0.95f)
                    val r = normalizedDist * maxRadius
                    val blipPos = Offset(
                        center.x + r * cos(angleRad),
                        center.y + r * sin(angleRad)
                    )

                    val blipColor = when {
                        peer.isSosActive -> CrimsonRed
                        peer.circleCode != null && peer.circleCode != "GENERAL" -> EmeraldGreen
                        else -> ElectricCyan
                    }

                    // Halo ring
                    drawCircle(
                        color = blipColor.copy(alpha = (1f - (blipPulse / 14f)).coerceIn(0f, 0.6f)),
                        radius = blipPulse,
                        center = blipPos,
                        style = Stroke(width = 2f)
                    )

                    // Core Blip
                    val isSelected = selectedPeer?.address == peer.address
                    drawCircle(
                        color = if (isSelected) Color.White else blipColor,
                        radius = if (isSelected) 8f else 6f,
                        center = blipPos
                    )
                }
            }
        }

        // Radar Distance Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("< 1m Immediate", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("1-5m Near", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("5-15m Mid", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("> 15m Far", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Peer Detail / Action Panel
        selectedPeer?.let { peer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.pseudonym,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (peer.isSosActive) CrimsonRed else ElectricCyan
                        )
                        Text(
                            text = "RSSI: ${peer.rssi} dBm • Est: ${peer.signalCategory.distanceEstimate} • Circle: ${peer.circleCode ?: "GENERAL"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = onNavigateToChat,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OPEN CHAT", color = CyberBgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Horizontal Quick-Picker of all detected peers
        if (peersList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(peersList, key = { it.address }) { peer ->
                    val isSelected = selectedPeer?.address == peer.address
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricCyan else CyberCard)
                            .border(1.dp, if (isSelected) ElectricCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectPeer(peer) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${peer.pseudonym} (${peer.rssi}dBm)",
                            color = if (isSelected) CyberBgDark else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}