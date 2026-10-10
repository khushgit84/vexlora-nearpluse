package com.vexlora.nearpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SosScreen(
    viewModel: NearPulseViewModel
) {
    val isSosActive by viewModel.isSosBroadcasting.collectAsState()
    val activeSosAlert by viewModel.activeSosAlert.collectAsState()

    var isPressing by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var selectedEmergencyType by remember { mutableStateOf("MEDICAL EMERGENCY") }

    val emergencyOptions = listOf(
        "MEDICAL EMERGENCY",
        "TRAPPED / PHYSICAL HAZARD",
        "OFF-GRID / LOST",
        "SEVERE WEATHER CRISIS"
    )

    // Pulsing animation for active alarm
    val infiniteTransition = rememberInfiniteTransition(label = "sosAlarm")
    val alarmPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmPulse"
    )

    // Coroutine for hold-to-activate timer
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(isPressing) {
        if (isPressing && !isSosActive) {
            val startTime = System.currentTimeMillis()
            val holdDuration = 3000L
            while (isPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    viewModel.startSosBroadcast(selectedEmergencyType)
                    isPressing = false
                    holdProgress = 0f
                    break
                }
                delay(30)
            }
        } else {
            holdProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSosActive) CrimsonRed.copy(alpha = alarmPulse * 0.25f) else CyberBgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SOS Station Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EMERGENCY SOS BEACON",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = CrimsonRed
                )
                Text(
                    text = "100% OFFLINE DISTRESS PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CrimsonRed,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Incoming Emergency Alert Card (if detected from someone else nearby)
        activeSosAlert?.let { alert ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CrimsonRed, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DISTRESS SIGNAL DETECTED!",
                                color = CrimsonRed,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "From: ${alert.senderPseudonym} (${alert.rssi} dBm)",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = alert.message,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.respondToSos(alert) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SEND RESCUE ACK", color = CyberBgDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.dismissIncomingSos() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed)
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PROBLEM SOLVED (STOP SOUND)", color = CrimsonRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Emergency Type Selector Chips
        if (!isSosActive) {
            Text(
                text = "SELECT EMERGENCY TYPE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                emergencyOptions.forEach { type ->
                    val isSelected = selectedEmergencyType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CrimsonRed.copy(alpha = 0.2f) else CyberCard)
                            .border(1.dp, if (isSelected) CrimsonRed else CyberCardBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedEmergencyType = type }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedEmergencyType = type },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CrimsonRed,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type,
                                color = if (isSelected) CrimsonRed else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Circular Hold-To-Activate / Active Beacon Center
        Box(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(isSosActive) {
                    if (!isSosActive) {
                        detectTapGestures(
                            onPress = {
                                isPressing = true
                                tryAwaitRelease()
                                isPressing = false
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 16f

                // Outer Track
                drawCircle(
                    color = if (isSosActive) CrimsonRed.copy(alpha = 0.3f) else CyberCardElevated,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 12f)
                )

                // Hold Progress Arc
                if (!isSosActive && holdProgress > 0f) {
                    drawArc(
                        color = CrimsonRed,
                        startAngle = -90f,
                        sweepAngle = 360f * holdProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }

                // Active SOS strobe ring
                if (isSosActive) {
                    drawCircle(
                        color = CrimsonRed.copy(alpha = alarmPulse),
                        radius = radius + 8f,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                }
            }

            // Inner Button Content
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(if (isSosActive) CrimsonRed else CrimsonRed.copy(alpha = 0.15f))
                    .border(2.dp, CrimsonRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isSosActive) Icons.Default.Campaign else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isSosActive) Color.White else CrimsonRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSosActive) "SOS ACTIVE" else "HOLD 3s",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isSosActive) Color.White else CrimsonRed
                    )
                    Text(
                        text = if (isSosActive) "TRANSMITTING" else "TO BROADCAST",
                        fontSize = 11.sp,
                        color = if (isSosActive) Color.White.copy(alpha = 0.8f) else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SOS Active Stop Action or Instructions
        if (isSosActive) {
            Button(
                onClick = { viewModel.stopSosBroadcast() },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldGreen, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberBgDark, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROBLEM SOLVED / STOP ALARM",
                    color = CyberBgDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SAFETY INSTRUCTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Transmits maximum-power BLE emergency advertisements up to 100 meters.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Operates entirely without cellular towers, SIM card, or Wi-Fi.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Any device running NearPulse within range will trigger an audible and visual alarm.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "EMERGENCY DRILL SIMULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test the siren audio, SOS Morse code vibration, and background heads-up alert:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testEmergencyAlarm(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, CrimsonRed, RoundedCornerShape(8.dp))
                        ) {
                            Text("TEST NOW", color = CrimsonRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { viewModel.testEmergencyAlarm(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .border(1.dp, TextMuted, RoundedCornerShape(8.dp))
                        ) {
                            Text("TEST IN BG (5s)", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}