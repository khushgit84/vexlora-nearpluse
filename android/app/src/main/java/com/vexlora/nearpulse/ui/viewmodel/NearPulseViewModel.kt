package com.vexlora.nearpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vexlora.nearpulse.NearPulseApplication
import com.vexlora.nearpulse.ble.*
import com.vexlora.nearpulse.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NearPulseViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NearPulseApplication
    private val bleManager = app.bleManager
    private val db = app.database

    val isScanning = bleManager.isScanning
    val isAdvertising = bleManager.isAdvertising
    val discoveredPeers = bleManager.discoveredPeers
    val activeSosAlert = bleManager.activeSosAlert
    val isSosBroadcasting = bleManager.isSosBroadcasting

    private val _selectedPeer = MutableStateFlow<DiscoveredPeer?>(null)
    val selectedPeer: StateFlow<DiscoveredPeer?> = _selectedPeer.asStateFlow()

    private val _localPseudonym = MutableStateFlow(bleManager.localPseudonym)
    val localPseudonym: StateFlow<String> = _localPseudonym.asStateFlow()

    private val _circleCode = MutableStateFlow(bleManager.currentCircleCode)
    val circleCode: StateFlow<String> = _circleCode.asStateFlow()

    val safetyCircles = db.safetyCircleDao().getAllCircles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDbPeers = db.peerDao().getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDbMessages = db.messageDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentChatMessages = _selectedPeer.flatMapLatest { peer ->
        if (peer == null) flowOf(emptyList())
        else db.messageDao().getMessagesForPeer(peer.address)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically persist discovered peers into Room
        viewModelScope.launch {
            discoveredPeers.collect { peersMap ->
                peersMap.values.forEach { peer ->
                    db.peerDao().insertOrUpdatePeer(
                        PeerEntity(
                            address = peer.address,
                            pseudonym = peer.pseudonym,
                            lastSeenTimestamp = peer.lastSeenTimestamp,
                            lastRssi = peer.rssi,
                            circleCode = peer.circleCode ?: "GENERAL"
                        )
                    )
                }
            }
        }

        // Automatically persist incoming BLE messages
        viewModelScope.launch {
            bleManager.incomingMessages.collect { msg ->
                db.messageDao().insertMessage(
                    MessageEntity(
                        id = msg.id,
                        peerAddress = msg.peerAddress,
                        senderPseudonym = msg.senderPseudonym,
                        text = msg.text,
                        timestamp = msg.timestamp,
                        isOutgoing = false,
                        isDelivered = true
                    )
                )
            }
        }

        // Automatically update delivery ACKs
        viewModelScope.launch {
            bleManager.messageAckEvents.collect { messageId ->
                db.messageDao().markDelivered(messageId)
            }
        }

        // Seed default emergency and general circles
        viewModelScope.launch {
            db.safetyCircleDao().insertCircle(
                SafetyCircleEntity(
                    circleCode = "GENERAL",
                    circleName = "General Broadcast Mesh",
                    memberCount = 1
                )
            )
            db.safetyCircleDao().insertCircle(
                SafetyCircleEntity(
                    circleCode = "RESCUE",
                    circleName = "Emergency Rescue Response",
                    memberCount = 1
                )
            )
        }
    }

    fun startAutoPulse() {
        bleManager.setupGattServer()
        if (!isScanning.value) {
            bleManager.startDiscovery()
            NearPulseForegroundService.start(getApplication())
        }
        if (!isAdvertising.value) {
            bleManager.startAdvertising()
        }
    }

    fun toggleScanning() {
        if (isScanning.value) {
            bleManager.stopDiscovery()
            NearPulseForegroundService.stop(getApplication())
        } else {
            bleManager.startDiscovery()
            NearPulseForegroundService.start(getApplication())
        }
    }

    fun toggleAdvertising() {
        if (isAdvertising.value) {
            bleManager.stopAdvertising()
        } else {
            bleManager.startAdvertising()
        }
    }

    fun selectPeer(peer: DiscoveredPeer?) {
        _selectedPeer.value = peer
    }

    fun selectPeerByAddress(address: String, pseudonym: String? = null) {
        val found = discoveredPeers.value[address] ?: DiscoveredPeer(
            address = address,
            pseudonym = pseudonym ?: ("Peer-" + address.takeLast(4)),
            rssi = -70,
            signalCategory = SignalCategory.NEAR,
            lastSeenTimestamp = System.currentTimeMillis()
        )
        _selectedPeer.value = found
    }

    fun testMessageNotification(delaySeconds: Long = 0) {
        viewModelScope.launch {
            if (delaySeconds > 0) {
                kotlinx.coroutines.delay(delaySeconds * 1000)
            }
            val testMsg = BleMessage(
                id = "TEST-" + System.currentTimeMillis().toString().takeLast(4),
                peerAddress = "11:22:33:44:55:66",
                senderPseudonym = "NP-ALPHA",
                text = "Hey! Offline BLE direct mesh message received outside the app.",
                timestamp = System.currentTimeMillis(),
                isOutgoing = false,
                isDelivered = true
            )
            // Persist peer and message so tapping notification immediately opens chat with this peer
            db.peerDao().insertOrUpdatePeer(
                PeerEntity(
                    address = testMsg.peerAddress,
                    pseudonym = testMsg.senderPseudonym,
                    lastSeenTimestamp = testMsg.timestamp,
                    lastRssi = -55,
                    circleCode = "GENERAL"
                )
            )
            db.messageDao().insertMessage(
                MessageEntity(
                    id = testMsg.id,
                    peerAddress = testMsg.peerAddress,
                    senderPseudonym = testMsg.senderPseudonym,
                    text = testMsg.text,
                    timestamp = testMsg.timestamp,
                    isOutgoing = false,
                    isDelivered = true
                )
            )
            EmergencyAlertManager.postIncomingMessageNotification(
                getApplication(),
                testMsg
            )
        }
    }

    fun sendChatMessage(text: String) {
        val peer = _selectedPeer.value ?: return
        if (text.isBlank()) return

        val msg = bleManager.sendMessage(peer.address, text.trim())
        viewModelScope.launch {
            db.messageDao().insertMessage(
                MessageEntity(
                    id = msg.id,
                    peerAddress = msg.peerAddress,
                    senderPseudonym = msg.senderPseudonym,
                    text = msg.text,
                    timestamp = msg.timestamp,
                    isOutgoing = true,
                    isDelivered = false
                )
            )
        }
    }

    fun startSosBroadcast(emergencyType: String = "EMERGENCY DISTRESS") {
        bleManager.startAdvertising(isSos = true, sosDetails = emergencyType)
    }

    fun stopSosBroadcast() {
        bleManager.stopAdvertising()
    }

    fun dismissIncomingSos() {
        bleManager.dismissSosAlert()
    }

    fun testEmergencyAlarm(delaySeconds: Long = 0) {
        viewModelScope.launch {
            if (delaySeconds > 0) {
                kotlinx.coroutines.delay(delaySeconds * 1000)
            }
            EmergencyAlertManager.triggerSosAlarm(
                getApplication(),
                SosAlert(
                    id = "DRILL-" + System.currentTimeMillis().toString().takeLast(4),
                    senderAddress = "AA:BB:CC:DD:EE:FF",
                    senderPseudonym = "DRILL-TEST",
                    message = "RESCUE DRILL: High-priority background siren & SOS vibration active",
                    timestamp = System.currentTimeMillis(),
                    rssi = -45
                )
            )
        }
    }

    fun respondToSos(alert: SosAlert) {
        bleManager.sendSosMutualAck(alert)
        bleManager.dismissSosAlert()
    }

    fun updatePseudonym(newName: String) {
        if (newName.isNotBlank()) {
            bleManager.localPseudonym = newName.trim().uppercase()
            _localPseudonym.value = bleManager.localPseudonym
            if (isAdvertising.value) {
                bleManager.startAdvertising(isSos = isSosBroadcasting.value)
            }
        }
    }

    fun setCircleCode(newCode: String) {
        if (newCode.isNotBlank()) {
            val upper = newCode.trim().uppercase()
            bleManager.currentCircleCode = upper
            _circleCode.value = upper
            viewModelScope.launch {
                db.safetyCircleDao().insertCircle(
                    SafetyCircleEntity(
                        circleCode = upper,
                        circleName = "Circle $upper",
                        memberCount = 1
                    )
                )
            }
            if (isAdvertising.value) {
                bleManager.startAdvertising(isSos = isSosBroadcasting.value)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.messageDao().deleteAllMessages()
            db.peerDao().deleteAllPeers()
        }
    }

    fun isBleSupported(): Boolean = bleManager.isBleSupported()
    fun isBluetoothEnabled(): Boolean = bleManager.isBluetoothEnabled()
    fun isAdvertisingSupported(): Boolean = bleManager.isAdvertisingSupported()
}
