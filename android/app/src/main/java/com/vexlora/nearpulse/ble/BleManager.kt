package com.vexlora.nearpulse.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "NearPulseBleManager"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // UI States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<Map<String, DiscoveredPeer>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _activeConnectionPeer = MutableStateFlow<DiscoveredPeer?>(null)
    val activeConnectionPeer: StateFlow<DiscoveredPeer?> = _activeConnectionPeer.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<BleMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<BleMessage> = _incomingMessages.asSharedFlow()

    private val _messageAckEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val messageAckEvents: SharedFlow<String> = _messageAckEvents.asSharedFlow()

    private val _activeSosAlert = MutableStateFlow<SosAlert?>(null)
    val activeSosAlert: StateFlow<SosAlert?> = _activeSosAlert.asStateFlow()

    private val _isSosBroadcasting = MutableStateFlow(false)
    val isSosBroadcasting: StateFlow<Boolean> = _isSosBroadcasting.asStateFlow()

    // GATT Server and Client references
    private var gattServer: BluetoothGattServer? = null
    private var activeGattClient: BluetoothGatt? = null
    private val connectedServerDevices = ConcurrentHashMap.newKeySet<BluetoothDevice>()

    // Local configuration
    var localPseudonym: String = "NP-" + UUID.randomUUID().toString().take(4).uppercase()
    var currentCircleCode: String? = null

    // Check hardware support
    fun isBleSupported(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun isAdvertisingSupported(): Boolean = bluetoothAdapter?.isMultipleAdvertisementSupported == true

    init {
        setupGattServer()
    }

    // -------------------------------------------------------------
    // GATT SERVER (Acts as Peripheral to accept incoming connections)
    // -------------------------------------------------------------
    private fun setupGattServer() {
        if (bluetoothManager == null || bluetoothAdapter?.isEnabled != true) return

        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(
                BleConstants.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            // Characteristic for writing messages
            val messageChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE or
                        BluetoothGattCharacteristic.PERMISSION_READ
            )
            val msgCccd = BluetoothGattDescriptor(
                BleConstants.CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
            )
            messageChar.addDescriptor(msgCccd)

            // Characteristic for ACKs
            val ackChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_ACK_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            // Characteristic for SOS alerts
            val sosChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_SOS_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or
                        BluetoothGattCharacteristic.PERMISSION_READ
            )

            // Characteristic for Peer Info
            val infoChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_INFO_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )

            service.addCharacteristic(messageChar)
            service.addCharacteristic(ackChar)
            service.addCharacteristic(sosChar)
            service.addCharacteristic(infoChar)

            gattServer?.addService(service)
            Log.d(TAG, "GATT Server configured and service added.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup GATT Server: ${e.message}")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(TAG, "Server connection state changed: ${device.address} -> $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedServerDevices.add(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedServerDevices.remove(device)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }

            value?.let { data ->
                val packet = BlePacket.parse(data)
                if (packet != null) {
                    when (packet.packetType) {
                        BleConstants.PACKET_TYPE_MSG -> {
                            handleIncomingMessage(device.address, packet)
                            // Send delivery acknowledgment back immediately
                            sendAckToDevice(device, packet.messageId)
                        }
                        BleConstants.PACKET_TYPE_ACK -> {
                            handleIncomingAck(packet.messageId)
                        }
                        BleConstants.PACKET_TYPE_SOS -> {
                            handleIncomingSos(device.address, packet)
                        }
                        BleConstants.PACKET_TYPE_SOS_ACK -> {
                            Log.d(TAG, "Peer acknowledged SOS: ${packet.senderPseudonym}")
                        }
                    }
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val responseValue = when (characteristic.uuid) {
                BleConstants.CHAR_INFO_UUID -> {
                    "${localPseudonym}|${currentCircleCode ?: "NONE"}".toByteArray(StandardCharsets.UTF_8)
                }
                BleConstants.CHAR_SOS_UUID -> {
                    if (_isSosBroadcasting.value) "ACTIVE_SOS".toByteArray() else "CLEAR".toByteArray()
                }
                else -> byteArrayOf()
            }
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responseValue)
        }
    }

    // -------------------------------------------------------------
    // ADVERTISING (Broadcast Presence & SOS)
    // -------------------------------------------------------------
    fun startAdvertising(isSos: Boolean = false, sosDetails: String = "") {
        if (!isBluetoothEnabled() || !isAdvertisingSupported()) {
            Log.w(TAG, "Bluetooth not enabled or advertising not supported")
            return
        }

        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bleAdvertiser == null) return

        stopAdvertising()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        // Service Data contains flags: [0] = SOS flag (0x01 if active), [1..] = short pseudonym
        val prefix = if (isSos) "SOS" else "NP"
        val payloadStr = "$prefix:$localPseudonym"
        val serviceDataBytes = payloadStr.toByteArray(StandardCharsets.UTF_8).take(10).toByteArray()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addServiceData(ParcelUuid(BleConstants.SERVICE_UUID), serviceDataBytes)
            .build()

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        _isAdvertising.value = true
        _isSosBroadcasting.value = isSos
    }

    fun stopAdvertising() {
        try {
            bleAdvertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertiser: ${e.message}")
        }
        _isAdvertising.value = false
        _isSosBroadcasting.value = false
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE Advertising started successfully.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Advertising failed with error code: $errorCode")
            _isAdvertising.value = false
            _isSosBroadcasting.value = false
        }
    }

    // -------------------------------------------------------------
    // SCANNING (Radar Discovery)
    // -------------------------------------------------------------
    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) return

        stopDiscovery()

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        _isScanning.value = true
        Log.d(TAG, "BLE Scanning started for NearPulse peers.")
    }

    fun stopDiscovery() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scanner: ${e.message}")
        }
        _isScanning.value = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val scanRecord = result.scanRecord ?: return

            // Parse service data
            val serviceData = scanRecord.getServiceData(ParcelUuid(BleConstants.SERVICE_UUID))
            var pseudonym = "Peer-" + device.address.takeLast(4).replace(":", "")
            var isSos = false

            if (serviceData != null && serviceData.isNotEmpty()) {
                val dataStr = String(serviceData, StandardCharsets.UTF_8)
                val parts = dataStr.split(":")
                if (parts.size >= 2) {
                    isSos = parts[0] == "SOS"
                    pseudonym = parts[1]
                }
            }

            val rssi = result.rssi
            val currentMap = _discoveredPeers.value.toMutableMap()
            val existing = currentMap[device.address]

            val updatedPeer = DiscoveredPeer(
                address = device.address,
                pseudonym = pseudonym,
                rssi = rssi,
                signalCategory = SignalCategory.fromRssi(rssi),
                lastSeenTimestamp = System.currentTimeMillis(),
                isSosActive = isSos,
                connectionStatus = existing?.connectionStatus ?: ConnectionStatus.DISCONNECTED
            )

            currentMap[device.address] = updatedPeer
            _discoveredPeers.value = currentMap

            // If SOS is broadcasted by peer, register emergency alert
            if (isSos) {
                _activeSosAlert.value = SosAlert(
                    alertId = UUID.randomUUID().toString(),
                    broadcasterAddress = device.address,
                    broadcasterPseudonym = pseudonym,
                    timestamp = System.currentTimeMillis(),
                    details = "Emergency beacon detected within Bluetooth range"
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    // -------------------------------------------------------------
    // GATT CLIENT (Connect to peer to send messages/SOS ACK)
    // -------------------------------------------------------------
    fun connectToPeer(peerAddress: String) {
        val bluetoothAdapter = this.bluetoothAdapter ?: return
        val device = bluetoothAdapter.getRemoteDevice(peerAddress) ?: return

        disconnectActiveGatt()

        updatePeerConnectionStatus(peerAddress, ConnectionStatus.CONNECTING)
        activeGattClient = device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnectActiveGatt() {
        activeGattClient?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GATT client: ${e.message}")
            }
        }
        activeGattClient = null
        _activeConnectionPeer.value?.let { peer ->
            updatePeerConnectionStatus(peer.address, ConnectionStatus.DISCONNECTED)
        }
        _activeConnectionPeer.value = null
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server on $address. Discovering services...")
                updatePeerConnectionStatus(address, ConnectionStatus.CONNECTED)
                val peer = _discoveredPeers.value[address]
                _activeConnectionPeer.value = peer?.copy(connectionStatus = ConnectionStatus.CONNECTED)
                gatt.requestMtu(BleConstants.REQUESTED_MTU)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from $address")
                updatePeerConnectionStatus(address, ConnectionStatus.DISCONNECTED)
                if (_activeConnectionPeer.value?.address == address) {
                    _activeConnectionPeer.value = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU configured to $mtu. Discovering services.")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered on ${gatt.device.address}")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write succeeded: ${characteristic.uuid}")
            } else {
                Log.e(TAG, "Characteristic write failed: status $status")
            }
        }
    }

    fun sendMessage(targetAddress: String, content: String, onResult: (Boolean, String) -> Unit) {
        val gatt = activeGattClient
        if (gatt == null || _activeConnectionPeer.value?.address != targetAddress) {
            onResult(false, "No active GATT connection to peer")
            return
        }

        val service = gatt.getService(BleConstants.SERVICE_UUID)
        val char = service?.getCharacteristic(BleConstants.CHAR_MESSAGE_UUID)
        if (char == null) {
            onResult(false, "NearPulse GATT service not ready")
            return
        }

        val messageId = UUID.randomUUID().toString()
        val packet = BlePacket(
            packetType = BleConstants.PACKET_TYPE_MSG,
            messageId = messageId,
            senderPseudonym = localPseudonym,
            payload = content
        )

        char.value = packet.toByteArray()
        val written = gatt.writeCharacteristic(char)
        if (written) {
            onResult(true, messageId)
        } else {
            onResult(false, "Write command rejected by Bluetooth stack")
        }
    }

    fun acknowledgeSos(alert: SosAlert) {
        val gatt = activeGattClient
        if (gatt != null && gatt.device.address == alert.broadcasterAddress) {
            val service = gatt.getService(BleConstants.SERVICE_UUID)
            val char = service?.getCharacteristic(BleConstants.CHAR_ACK_UUID)
            if (char != null) {
                val packet = BlePacket(
                    packetType = BleConstants.PACKET_TYPE_SOS_ACK,
                    messageId = alert.alertId,
                    senderPseudonym = localPseudonym,
                    payload = "RESCUE_ACK"
                )
                char.value = packet.toByteArray()
                gatt.writeCharacteristic(char)
            }
        }
        _activeSosAlert.value = alert.copy(isAcknowledgedByLocalUser = true)
    }

    fun dismissSosAlert() {
        _activeSosAlert.value = null
    }

    private fun sendAckToDevice(device: BluetoothDevice, messageId: String) {
        // If connected as server or client, write ACK packet
        coroutineScope.launch {
            _messageAckEvents.emit(messageId)
        }
    }

    private fun handleIncomingMessage(senderAddress: String, packet: BlePacket) {
        coroutineScope.launch {
            val msg = BleMessage(
                id = packet.messageId,
                peerAddress = senderAddress,
                senderPseudonym = packet.senderPseudonym,
                content = packet.payload,
                timestamp = System.currentTimeMillis(),
                isOutgoing = false,
                status = MessageDeliveryStatus.DELIVERED
            )
            _incomingMessages.emit(msg)
        }
    }

    private fun handleIncomingAck(messageId: String) {
        coroutineScope.launch {
            _messageAckEvents.emit(messageId)
        }
    }

    private fun handleIncomingSos(senderAddress: String, packet: BlePacket) {
        _activeSosAlert.value = SosAlert(
            alertId = packet.messageId,
            broadcasterAddress = senderAddress,
            broadcasterPseudonym = packet.senderPseudonym,
            timestamp = System.currentTimeMillis(),
            details = packet.payload.ifBlank { "Emergency SOS Signal" }
        )
    }

    private fun updatePeerConnectionStatus(address: String, status: ConnectionStatus) {
        val current = _discoveredPeers.value.toMutableMap()
        current[address]?.let { peer ->
            current[address] = peer.copy(connectionStatus = status)
            _discoveredPeers.value = current
        }
    }

    fun clearDiscoveredPeers() {
        _discoveredPeers.value = emptyMap()
    }

    fun cleanup() {
        stopDiscovery()
        stopAdvertising()
        disconnectActiveGatt()
        try {
            gattServer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT server: ${e.message}")
        }
        gattServer = null
        coroutineScope.cancel()
    }
}
