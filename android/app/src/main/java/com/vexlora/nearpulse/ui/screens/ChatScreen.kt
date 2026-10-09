package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.vexlora.nearpulse.ble.BleMessage
import com.vexlora.nearpulse.ble.ConnectionStatus
import com.vexlora.nearpulse.ble.MessageDeliveryStatus
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: NearPulseViewModel,
    onBack: () -> Unit
) {
    val activePeer by viewModel.activeConnectionPeer.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickSafetyMessages = listOf(
        "I'm safe",
        "Need help",
        "Meet me here",
        "Battery low",
        "Waiting for you"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
    ) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceDark,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (activePeer?.connectionStatus) {
                                ConnectionStatus.CONNECTED -> SignalGreen
                                ConnectionStatus.CONNECTING -> SignalYellow
                                else -> TextMuted
                            }
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePeer?.pseudonym ?: "Offline Peer",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (activePeer?.connectionStatus) {
                            ConnectionStatus.CONNECTED -> "GATT LE Connected (MTU 512)"
                            ConnectionStatus.CONNECTING -> "Establishing Bluetooth GATT handshake..."
                            else -> "Disconnected from peer"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (activePeer?.connectionStatus) {
                            ConnectionStatus.CONNECTED -> SignalGreen
                            ConnectionStatus.CONNECTING -> SignalYellow
                            else -> TextMuted
                        },
                        fontSize = 11.sp
                    )
                }

                if (activePeer?.connectionStatus == ConnectionStatus.CONNECTED) {
                    IconButton(onClick = { viewModel.disconnectPeer() }) {
                        Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = TextMuted)
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No direct BLE messages yet", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "Exchange encrypted short packets offline", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(messages) { msg ->
                    MessageBubble(message = msg)
                }
            }
        }

        // Quick Safety Preset Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickSafetyMessages) { chip ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyberSurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder)),
                    modifier = Modifier.clickable {
                        viewModel.sendMessage(chip)
                    }
                ) {
                    Text(
                        text = chip,
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Message Input Field
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { if (it.length <= 280) textInput = it },
                    placeholder = { Text("Type offline message...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (textInput.isNotBlank()) ElectricCyan else CyberSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) CyberBgDark else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: BleMessage) {
    val isOut = message.isOutgoing
    val alignment = if (isOut) Alignment.End else Alignment.Start
    val bubbleColor = if (isOut) ElectricCyan.copy(alpha = 0.2f) else CyberSurfaceElevated
    val borderColor = if (isOut) ElectricCyan.copy(alpha = 0.5f) else CyberBorder
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOut) 14.dp else 2.dp,
                bottomEnd = if (isOut) 2.dp else 14.dp
            ),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isOut) {
                    Text(
                        text = message.senderPseudonym,
                        color = NeonViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageDeliveryStatus.PENDING -> Text("⏳", fontSize = 10.sp)
                            MessageDeliveryStatus.SENDING -> Text("↗", fontSize = 10.sp, color = SignalYellow)
                            MessageDeliveryStatus.DELIVERED -> Text("✓✓", fontSize = 10.sp, color = SignalGreen)
                            MessageDeliveryStatus.FAILED -> Text("!", fontSize = 10.sp, color = SosRed)
                        }
                    }
                }
            }
        }
    }
}
