package com.vexlora.nearpulse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vexlora.nearpulse.ble.*
import com.vexlora.nearpulse.data.local.SafetyCircleEntity
import com.vexlora.nearpulse.data.repository.NearPulseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class NearPulseViewModel(
    private val repository: NearPulseRepository
) : ViewModel() {

    val bleManager = repository.bleManager

    val isScanning = bleManager.isScanning
    val isAdvertising = bleManager.isAdvertising
    val discoveredPeers = bleManager.discoveredPeers
    val activeConnectionPeer = bleManager.activeConnectionPeer
    val activeSosAlert = bleManager.activeSosAlert
    val isSosBroadcasting = bleManager.isSosBroadcasting

    private val _selectedPeerAddress = MutableStateFlow<String?>(null)
    val selectedPeerAddress: StateFlow<String?> = _selectedPeerAddress.asStateFlow()

    val currentMessages: StateFlow<List<BleMessage>> = _selectedPeerAddress.flatMapLatest { address ->
        if (address != null) repository.getMessagesForPeer(address)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val safetyCircle: StateFlow<SafetyCircleEntity?> = repository.getStoredCircle()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _statusNotice = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusNotice = _statusNotice.asSharedFlow()

    init {
        // Observe incoming messages and persist to Room database
        viewModelScope.launch {
            bleManager.incomingMessages.collect { msg ->
                repository.saveMessage(msg)
                repository.saveDiscoveredPeer(msg.peerAddress, msg.senderPseudonym, -65, null)
            }
        }

        // Observe ACKs
        viewModelScope.launch {
            bleManager.messageAckEvents.collect { msgId ->
                repository.updateMessageStatus(msgId, MessageDeliveryStatus.DELIVERED)
            }
        }
    }

    fun toggleDiscovery() {
        if (isScanning.value) {
            bleManager.stopDiscovery()
            bleManager.stopAdvertising()
        } else {
            bleManager.startDiscovery()
            bleManager.startAdvertising()
        }
    }

    fun selectPeerForChat(peer: DiscoveredPeer) {
        _selectedPeerAddress.value = peer.address
        if (activeConnectionPeer.value?.address != peer.address) {
            bleManager.connectToPeer(peer.address)
        }
    }

    fun disconnectPeer() {
        bleManager.disconnectActiveGatt()
    }

    fun sendMessage(text: String) {
        val address = _selectedPeerAddress.value ?: return
        if (text.isBlank()) return

        val message = BleMessage(
            id = UUID.randomUUID().toString(),
            peerAddress = address,
            senderPseudonym = bleManager.localPseudonym,
            content = text.trim(),
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = MessageDeliveryStatus.SENDING
        )

        viewModelScope.launch {
            repository.saveMessage(message)
            bleManager.sendMessage(address, message.content) { success, result ->
                viewModelScope.launch {
                    val newStatus = if (success) MessageDeliveryStatus.DELIVERED else MessageDeliveryStatus.FAILED
                    repository.updateMessageStatus(message.id, newStatus)
                }
            }
        }
    }

    fun startSosBroadcast(details: String = "Emergency assistance needed") {
        bleManager.startAdvertising(isSos = true, sosDetails = details)
        viewModelScope.launch {
            _statusNotice.emit("EMERGENCY SOS BEACON ACTIVE: Broadcasting to all nearby BLE devices.")
        }
    }

    fun stopSosBroadcast() {
        bleManager.stopAdvertising()
        // Resume normal beacon if previously discovering
        if (isScanning.value) {
            bleManager.startAdvertising(isSos = false)
        }
        viewModelScope.launch {
            _statusNotice.emit("SOS Beacon deactivated.")
        }
    }

    fun acknowledgeSos(alert: SosAlert) {
        bleManager.acknowledgeSos(alert)
    }

    fun dismissSosAlert() {
        bleManager.dismissSosAlert()
    }

    fun createSafetyCircle(name: String) {
        val code = "NP-" + (1000..9999).random()
        viewModelScope.launch {
            repository.createOrJoinCircle(code, name.ifBlank { "Squad Alpha" })
            _statusNotice.emit("Safety Circle created with code: $code")
        }
    }

    fun joinSafetyCircle(code: String) {
        val cleanCode = code.trim().uppercase()
        viewModelScope.launch {
            repository.createOrJoinCircle(cleanCode, "Circle $cleanCode")
            _statusNotice.emit("Joined Safety Circle: $cleanCode")
        }
    }

    fun leaveSafetyCircle() {
        viewModelScope.launch {
            repository.leaveCircle()
            _statusNotice.emit("Left Safety Circle.")
        }
    }

    fun rotatePseudonym() {
        val newName = "NP-" + UUID.randomUUID().toString().take(4).uppercase()
        bleManager.localPseudonym = newName
        if (isAdvertising.value) {
            bleManager.startAdvertising(isSos = isSosBroadcasting.value)
        }
        viewModelScope.launch {
            _statusNotice.emit("Privacy pseudonym rotated to: $newName")
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearAllData()
            _statusNotice.emit("All local records and message cache cleared.")
        }
    }

    class Factory(private val repository: NearPulseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NearPulseViewModel(repository) as T
        }
    }
}
