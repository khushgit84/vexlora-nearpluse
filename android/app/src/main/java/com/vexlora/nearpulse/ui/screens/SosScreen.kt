package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SosScreen(
    viewModel: NearPulseViewModel,
    onBack: () -> Unit
) {
    val isSosActive by viewModel.isSosBroadcasting.collectAsState()
    val incomingSosAlert by viewModel.activeSosAlert.collectAsState()

    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Flashing red pulse animation when SOS is active
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val sosPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = "EMERGENCY SOS MODE",
                style = MaterialTheme.typography.titleLarge,
                color = if (isSosActive) SosRed else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Range & Operating System Limitations Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceDark,
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SensorsOff, contentDescription = null, tint = SignalYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BLUETOOTH RANGE BOUNDARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SignalYellow
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NearPulse SOS broadcasts over Bluetooth LE (approx. 10-30 meters direct line of sight). Does NOT replace emergency 911 / 112 services. Delivery is only confirmed when a recipient phone sends a cryptographic ACK packet.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Action Circle
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(if (isSosActive) sosPulseScale else 1f)
                .clip(CircleShape)
                .background(
                    if (isSosActive) SosRed.copy(alpha = 0.2f)
                    else CyberSurfaceDark
                )
                .border(
                    width = if (isSosActive) 3.dp else 2.dp,
                    color = if (isSosActive) SosRed else CyberBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSosActive) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = SosRed, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BROADCASTING SOS",
                        color = SosRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "LE Beacon Pulse Active",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                // Deliberate Press-and-Hold Trigger to prevent accidental clicks
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(if (isHolding) SosRed.copy(alpha = 0.3f) else CyberSurfaceElevated)
                        .border(2.dp, if (isHolding) SosRed else CyberBorder, CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    val job = coroutineScope.launch {
                                        for (i in 1..100) {
                                            holdProgress = i / 100f
                                            delay(30) // 3 seconds total hold
                                        }
                                        viewModel.startSosBroadcast("Distress Beacon Triggered")
                                        isHolding = false
                                        holdProgress = 0f
                                    }
                                    tryAwaitRelease()
                                    job.cancel()
                                    isHolding = false
                                    holdProgress = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = SosRed, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHolding) "HOLD (${(holdProgress * 100).toInt()}%)" else "HOLD 3 SECONDS",
                            color = SosRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "To Activate Emergency",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SOS Controls
        if (isSosActive) {
            Button(
                onClick = { viewModel.stopSosBroadcast() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SosRed))
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null, tint = SosRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "CANCEL EMERGENCY SOS", color = SosRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Incoming Alert Card (If another nearby phone is broadcasting SOS)
        incomingSosAlert?.let { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SosRed.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SosRed, NeonViolet)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = SosRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PEER SOS RECEIVED",
                            color = SosRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Device ${alert.broadcasterPseudonym} (${alert.broadcasterAddress}) is signaling distress.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.acknowledgeSos(alert) },
                            colors = ButtonDefaults.buttonColors(containerColor = SosRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = if (alert.isAcknowledgedByLocalUser) "Acknowledged ✓" else "Acknowledge Help", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.dismissSosAlert() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Dismiss", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
