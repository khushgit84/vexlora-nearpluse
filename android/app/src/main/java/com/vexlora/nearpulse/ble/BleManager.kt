package com.vexlora.nearpulse.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    private val tag = "BleManager"

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    val isScanning = MutableStateFlow(false)
    val isAdvertising = MutableStateFlow(false)
    val discoveredPeers = MutableStateFlow<Map<String, DiscoveredPeer>>(emptyMap())
    val activeConnectionPeer = MutableStateFlow<DiscoveredPeer?>(null)
    val incomingMessages = MutableSharedFlow<BleMessage>(extraBufferCapacity = 64)
    val messageAckEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val activeSosAlert = MutableStateFlow<SosAlert?>(null)
    val isSosBroadcasting = MutableStateFlow(false)
    private val resolvedSosAddresses = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private var gattServer: BluetoothGattServer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var localPseudonym: String = "NP-" + UUID.randomUUID().toString().take(4).uppercase()
    var currentCircleCode: String = "GENERAL"

    fun isBleSupported(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun isAdvertisingSupported(): Boolean = bluetoothAdapter?.isMultipleAdvertisementSupported == true

    init {
        setupGattServer()
    }

    fun setupGattServer() {
        if (bluetoothManager == null || bluetoothAdapter?.isEnabled != true) return
        try {
            gattServer?.close()
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(BleConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val messageChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            messageChar.addDescriptor(BluetoothGattDescriptor(BleConstants.CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

            val ackChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_ACK_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            ackChar.addDescriptor(BluetoothGattDescriptor(BleConstants.CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

            val sosChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_SOS_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            sosChar.addDescriptor(BluetoothGattDescriptor(BleConstants.CCCD_UUID, BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

            service.addCharacteristic(messageChar)
            service.addCharacteristic(ackChar)
            service.addCharacteristic(sosChar)

            gattServer?.addService(service)
            Log.d(tag, "GATT Server configured successfully")
        } catch (e: Exception) {
            Log.e(tag, "GATT Server error: ${e.message}")
        }
    }

    fun startAdvertising(isSos: Boolean = false, sosDetails: String = "") {
        if (!isBluetoothEnabled()) {
            Log.w(tag, "Cannot advertise: Bluetooth disabled")
            return
        }
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            Log.w(tag, "Device does not support BLE Peripheral advertising")
            return
        }

        try {
            stopAdvertising()

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            // 1. Primary Advertisement Packet (Guaranteed <= 31 bytes)
            // 128-bit Service UUID is 16 bytes + 2 header bytes = 18 bytes
            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build()

            // 2. Scan Response Packet (Separate 31 bytes)
            // Custom Vexlora Metadata: PREFIX:CALLSIGN
            val prefix = if (isSos) "SOS" else "NP"
            val metaStr = "$prefix:$localPseudonym"
            val metaBytes = metaStr.toByteArray(StandardCharsets.UTF_8).take(12).toByteArray()

            val scanResponseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false) // MUST be false: "OnePlus Nord 3 5G" is 19 bytes and exceeds 31-byte limit!
                .addManufacturerData(0xFFFF, metaBytes)
                .build()

            bleAdvertiser?.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
            isSosBroadcasting.value = isSos
            if (isSos) {
                EmergencyAlertManager.triggerLocalSosBroadcastFeedback(context)
            }
            Log.d(tag, "BLE startAdvertising triggered for $metaStr, isSos=$isSos")
        } catch (e: Exception) {
            Log.e(tag, "Error starting advertising: ${e.message}", e)
        }
    }

    fun stopAdvertising() {
        try {
            bleAdvertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.w(tag, "Error stopping advertising: ${e.message}")
        }
        isAdvertising.value = false
        isSosBroadcasting.value = false
        EmergencyAlertManager.dismissSosNotification(context)
    }

    fun startDiscovery() {
        if (!isBluetoothEnabled()) {
            Log.w(tag, "Cannot scan: Bluetooth disabled")
            return
        }
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            Log.w(tag, "BluetoothLeScanner unavailable")
            return
        }

        try {
            stopDiscovery()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            // Start unfiltered scan so that all nearby BLE devices and NearPulse nodes appear
            bleScanner?.startScan(null, settings, scanCallback)
            isScanning.value = true
            Log.d(tag, "Started unrestricted BLE discovery")
        } catch (e: Exception) {
            Log.e(tag, "Error starting unfiltered scan: ${e.message}, falling back to filtered scan", e)
            try {
                val filters = listOf(
                    ScanFilter.Builder().setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID)).build()
                )
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bleScanner?.startScan(filters, settings, scanCallback)
                isScanning.value = true
            } catch (e2: Exception) {
                Log.e(tag, "Fallback scan failed: ${e2.message}", e2)
            }
        }
    }

    fun stopDiscovery() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(tag, "Error stopping discovery: ${e.message}")
        }
        isScanning.value = false
    }

    fun sendMessage(targetAddress: String, text: String, messageId: String = UUID.randomUUID().toString().take(8)): BleMessage {
        val message = BleMessage(
            id = messageId,
            peerAddress = targetAddress,
            senderPseudonym = localPseudonym,
            text = text,
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            isDelivered = false
        )

        coroutineScope.launch {
            val packet = BlePacket(
                packetType = BleConstants.PACKET_TYPE_MSG,
                messageId = messageId,
                senderPseudonym = localPseudonym,
                payload = text
            )
            writePacketToPeer(targetAddress, packet)
        }

        return message
    }

    fun sendSosMutualAck(sosAlert: SosAlert) {
        coroutineScope.launch {
            val packet = BlePacket(
                packetType = BleConstants.PACKET_TYPE_SOS_ACK,
                messageId = sosAlert.id,
                senderPseudonym = localPseudonym,
                payload = "HELP_DISPATCHED_FROM_$localPseudonym"
            )
            writePacketToPeer(sosAlert.senderAddress, packet)
        }
    }

    fun dismissSosAlert() {
        val current = activeSosAlert.value
        if (current != null) {
            resolvedSosAddresses.add(current.senderAddress)
        }
        activeSosAlert.value = null
        EmergencyAlertManager.dismissSosNotification(context)
    }

    private fun writePacketToPeer(targetAddress: String, packet: BlePacket) {
        val device = bluetoothAdapter?.getRemoteDevice(targetAddress) ?: return
        val packetBytes = packet.toByteArray()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(tag, "GATT link to ${device.address}: status=$status, newState=$newState")
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.requestMtu(BleConstants.REQUESTED_MTU)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(BleConstants.SERVICE_UUID)
                    val characteristicUuid = when (packet.packetType) {
                        BleConstants.PACKET_TYPE_ACK, BleConstants.PACKET_TYPE_SOS_ACK -> BleConstants.CHAR_ACK_UUID
                        BleConstants.PACKET_TYPE_SOS -> BleConstants.CHAR_SOS_UUID
                        else -> BleConstants.CHAR_MESSAGE_UUID
                    }

                    val char = service?.getCharacteristic(characteristicUuid)
                    if (char != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(char, packetBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            char.value = packetBytes
                            @Suppress("DEPRECATION")
                            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(char)
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(tag, "Packet delivered to $targetAddress: ${packet.messageId}")
                    if (packet.packetType == BleConstants.PACKET_TYPE_MSG) {
                        coroutineScope.launch {
                            messageAckEvents.emit(packet.messageId)
                        }
                    }
                }
                gatt.disconnect()
            }
        }

        // Modern TRANSPORT_LE connection on Android 6+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, callback)
        }
    }

    private fun isPhoneDevice(
        device: BluetoothDevice,
        devName: String,
        isNearPulse: Boolean
    ): Boolean {
        val lower = devName.lowercase()

        // 1. Reject non-phone Bluetooth peripherals FIRST (TVs, media boxes, set-top boxes, speakers, earphones, smart watches, wearables)
        val nonPhoneKeywords = listOf(
            "tv", "stb", "box", "soundbar", "speaker", "audio", "echo", "alexa",
            "buds", "airpods", "headphone", "earphone", "headset", "tws", "wireless stereo", "neckband",
            "watch", "band", "fit", "tracker", "amazfit", "garmin",
            "beacon", "tag", "sensor", "bulb", "light", "plug", "dongle", "printer", "remote",
            "cast", "roku", "jio", "firestick", "bravia", "webos"
        )
        for (kw in nonPhoneKeywords) {
            if (lower.contains(kw)) {
                return false
            }
        }

        // 2. If running VEXLORA NearPulse protocol, it is an active phone mesh node
        if (isNearPulse) return true

        // 3. Reject unnamed BLE devices (beacons, smart plugs, light bulbs, random RF noise)
        if (devName.isBlank()) return false

        // 4. Check BluetoothClass if available
        try {
            val btClass = device.bluetoothClass
            if (btClass != null) {
                val major = btClass.majorDeviceClass
                if (major == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO ||
                    major == android.bluetooth.BluetoothClass.Device.Major.WEARABLE ||
                    major == android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL ||
                    major == android.bluetooth.BluetoothClass.Device.Major.IMAGING ||
                    major == android.bluetooth.BluetoothClass.Device.Major.TOY ||
                    major == android.bluetooth.BluetoothClass.Device.Major.HEALTH) {
                    return false
                }
                if (major == android.bluetooth.BluetoothClass.Device.Major.PHONE) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // 5. Whitelist known smartphone brands and patterns
        val phoneKeywords = listOf(
            "phone", "mobile", "redmi", "xiaomi", "oneplus", "nord", "iphone",
            "galaxy", "pixel", "realme", "oppo", "vivo", "motorola", "moto",
            "nothing", "poco", "iqoo", "honor", "huawei", "infinix", "tecno",
            "nokia", "xperia", "asus", "rog"
        )
        for (kw in phoneKeywords) {
            if (lower.contains(kw)) {
                return true
            }
        }

        return false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val scanRecord = result.scanRecord
            val rssi = result.rssi

            val serviceUuids = scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
            val isNearPulseService = serviceUuids.contains(BleConstants.SERVICE_UUID)
            val manufacturerData = scanRecord?.getManufacturerSpecificData(0xFFFF)

            var pseudonym = ""
            var isSos = false
            var isNearPulse = isNearPulseService

            if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                val metaStr = try { String(manufacturerData, StandardCharsets.UTF_8) } catch (_: Exception) { "" }
                if (metaStr.startsWith("NP:") || metaStr.startsWith("SOS:")) {
                    isNearPulse = true
                    val parts = metaStr.split(":")
                    if (parts.isNotEmpty()) {
                        isSos = parts[0] == "SOS"
                        if (parts.size >= 2) {
                            pseudonym = parts[1]
                        }
                    }
                }
            }

            val devName = try {
                scanRecord?.deviceName ?: device.name ?: ""
            } catch (_: Exception) {
                ""
            }

            if (devName.startsWith("NP-", ignoreCase = true) || devName.startsWith("SOS-", ignoreCase = true)) {
                isNearPulse = true
                if (pseudonym.isEmpty()) {
                    pseudonym = devName
                }
                if (devName.startsWith("SOS-", ignoreCase = true)) {
                    isSos = true
                }
            }

            // FILTER: Show ONLY nearby phones, ignore all TVs, STBs, and non-phone Bluetooth devices!
            if (!isPhoneDevice(device, devName, isNearPulse)) {
                return
            }

            // Assign clear pseudonym: either custom callsign, real phone name, or readable callsign
            if (pseudonym.isEmpty()) {
                pseudonym = devName
            }

            Log.d(tag, "Discovered Phone: $address, name='$devName', rssi=$rssi, isNearPulse=$isNearPulse, pseudo=$pseudonym")

            val currentMap = discoveredPeers.value.toMutableMap()
            val peer = DiscoveredPeer(
                address = address,
                pseudonym = pseudonym,
                rssi = rssi,
                signalCategory = SignalCategory.fromRssi(rssi),
                lastSeenTimestamp = System.currentTimeMillis(),
                isSosActive = isSos,
                circleCode = if (isNearPulse) currentCircleCode else null
            )
            currentMap[address] = peer
            discoveredPeers.value = currentMap

            if (isSos) {
                if (!resolvedSosAddresses.contains(address)) {
                    val alert = SosAlert(
                        id = "SOS-${address.takeLast(4)}",
                        senderAddress = address,
                        senderPseudonym = pseudonym,
                        message = "DISTRESS SIGNAL DETECTED NEARBY! RSSI: $rssi dBm",
                        timestamp = System.currentTimeMillis(),
                        rssi = rssi
                    )
                    val prev = activeSosAlert.value
                    if (prev == null || prev.senderAddress != address) {
                        activeSosAlert.value = alert
                        EmergencyAlertManager.triggerSosAlarm(context, alert)
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "BLE Scan Failed: error $errorCode")
            isScanning.value = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(tag, "BLE Broadcast Active and transmitting")
            isAdvertising.value = true
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(tag, "BLE Broadcast Failed: error $errorCode")
            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                try {
                    val minimalData = AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                        .build()
                    val settings = AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .build()
                    bleAdvertiser?.startAdvertising(settings, minimalData, object : AdvertiseCallback() {
                        override fun onStartSuccess(s: AdvertiseSettings?) {
                            Log.d(tag, "Minimal fallback BLE Broadcast active")
                            isAdvertising.value = true
                        }
                        override fun onStartFailure(err: Int) {
                            Log.e(tag, "Minimal fallback also failed: $err")
                            isAdvertising.value = false
                        }
                    })
                    return
                } catch (e: Exception) {
                    Log.e(tag, "Fallback error: ${e.message}")
                }
            }
            isAdvertising.value = false
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d(tag, "GATT Server incoming connection from ${device?.address}, state: $newState")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }

            val data = value ?: return
            val senderAddr = device?.address ?: "UNKNOWN"
            val parsedPacket = BlePacket.parse(data)

            if (parsedPacket != null) {
                when (parsedPacket.packetType) {
                    BleConstants.PACKET_TYPE_MSG -> {
                        val msg = BleMessage(
                            id = parsedPacket.messageId,
                            peerAddress = senderAddr,
                            senderPseudonym = parsedPacket.senderPseudonym,
                            text = parsedPacket.payload,
                            timestamp = System.currentTimeMillis(),
                            isOutgoing = false,
                            isDelivered = true
                        )
                        coroutineScope.launch {
                            incomingMessages.emit(msg)
                            // Post rich notification outside the app
                            EmergencyAlertManager.postIncomingMessageNotification(context, msg)

                            val ackPacket = BlePacket(
                                packetType = BleConstants.PACKET_TYPE_ACK,
                                messageId = parsedPacket.messageId,
                                senderPseudonym = localPseudonym,
                                payload = "ACK"
                            )
                            writePacketToPeer(senderAddr, ackPacket)
                        }
                    }
                    BleConstants.PACKET_TYPE_ACK -> {
                        coroutineScope.launch {
                            messageAckEvents.emit(parsedPacket.messageId)
                        }
                    }
                    BleConstants.PACKET_TYPE_SOS -> {
                        val alert = SosAlert(
                            id = parsedPacket.messageId,
                            senderAddress = senderAddr,
                            senderPseudonym = parsedPacket.senderPseudonym,
                            message = parsedPacket.payload,
                            timestamp = System.currentTimeMillis()
                        )
                        activeSosAlert.value = alert
                        EmergencyAlertManager.triggerSosAlarm(context, alert)
                    }
                    BleConstants.PACKET_TYPE_SOS_ACK -> {
                        Log.d(tag, "Mutual SOS Rescue ACK received from ${parsedPacket.senderPseudonym}")
                    }
                }
            }
        }
    }

    fun cleanUp() {
        stopDiscovery()
        stopAdvertising()
        try {
            gattServer?.close()
        } catch (e: Exception) {
            Log.w(tag, "Error closing GATT server: ${e.message}")
        }
        coroutineScope.cancel()
    }
}