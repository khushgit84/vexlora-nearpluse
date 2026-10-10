package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.nearpulse.data.local.MessageEntity
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: NearPulseViewModel,
    onNavigateToRadar: () -> Unit
) {
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()
    val messages by viewModel.currentChatMessages.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChips = listOf(
        "Are you safe?",
        "Need assistance here",
        "Battery low (15%)",
        "Heading to checkpoint",
        "All clear in my zone"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
    ) {
        // Chat Header
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedPeer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedPeer!!.pseudonym,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Text(
                                text = "RSSI: ${selectedPeer!!.rssi} dBm • Direct BLE Link",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToRadar,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(ElectricCyan, CyberCardBorder)))
                    ) {
                        Text("RADAR", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text(
                        text = "SELECT PEER TO CHAT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )

                    Button(
                        onClick = onNavigateToRadar,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("OPEN RADAR", color = CyberBgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Peer Picker Bar if multiple peers detected
        if (peersMap.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCardElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(peersMap.values.toList(), key = { it.address }) { peer ->
                    val isSelected = selectedPeer?.address == peer.address
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ElectricCyan else CyberCard)
                            .border(1.dp, if (isSelected) ElectricCyan else CyberCardBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectPeer(peer) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = peer.pseudonym,
                            color = if (isSelected) CyberBgDark else TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Messages Thread
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (selectedPeer == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ConnectWithoutContact,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Peer Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Scan or select a peer from the top bar to start offline chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.testMessageNotification(5) },
                        border = BorderStroke(1.dp, ElectricCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST NOTIFICATION (MINIMIZE IN 5s)", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Offline Direct Channel Established",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Messages are sent directly via BLE GATT with delivery ACK",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
            }
        }

        // Quick Preset Message Chips
        if (selectedPeer != null) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCardElevated)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.sendChatMessage(chip)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(chip, color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Message Input Row
        if (selectedPeer != null) {
            Surface(
                color = CyberCard,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 280) inputText = it },
                        placeholder = { Text("Type offline message...", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCardElevated,
                            unfocusedContainerColor = CyberCardElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricCyan,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 100.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ElectricCyan,
                            disabledContainerColor = CyberCardElevated
                        ),
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) CyberBgDark else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: MessageEntity) {
    val isOut = message.isOutgoing
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isOut) 14.dp else 2.dp,
                        bottomEnd = if (isOut) 2.dp else 14.dp
                    )
                )
                .background(
                    if (isOut) Brush.linearGradient(listOf(ElectricCyanDark, ElectricCyan))
                    else Brush.linearGradient(listOf(CyberCardElevated, CyberCard))
                )
                .border(
                    1.dp,
                    if (isOut) Color.Transparent else CyberCardBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!isOut) {
                    Text(
                        text = message.senderPseudonym,
                        color = ElectricCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.text,
                    color = if (isOut) CyberBgDark else TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isOut) FontWeight.Medium else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        color = if (isOut) CyberBgDark.copy(alpha = 0.7f) else TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )

                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isDelivered) "Delivered" else "Sent",
                            tint = if (message.isDelivered) CyberBgDark else CyberBgDark.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}