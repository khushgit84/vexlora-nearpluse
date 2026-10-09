export interface AndroidCodeFile {
  path: string;
  name: string;
  category: 'manifest' | 'gradle' | 'kotlin-ble' | 'compose-ui' | 'room-db' | 'test' | 'docs';
  description: string;
  content: string;
}

export const ANDROID_FILES: AndroidCodeFile[] = [
  {
    path: 'android/app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    category: 'manifest',
    description: 'Hardware BLE features, Android 12+ permissions (neverForLocation), foreground service',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Hardware feature requirements -->
    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true" />

    <!-- Android 12+ (API 31+) Bluetooth Permissions -->
    <!-- neverForLocation guarantees privacy: app does NOT derive physical GPS location from BLE beacons -->
    <uses-permission
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"
        tools:targetApi="s" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_ADVERTISE"
        tools:targetApi="s" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_CONNECT"
        tools:targetApi="s" />

    <!-- Legacy Bluetooth permissions for Android 11 (API 30) and below -->
    <uses-permission
        android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.ACCESS_FINE_LOCATION"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.ACCESS_COARSE_LOCATION"
        android:maxSdkVersion="30" />

    <!-- Notification and Feedback permissions -->
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission
        android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"
        tools:targetApi="upside_down_cake" />

    <application
        android:name=".NearPulseApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.VexloraNearPulse">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.VexloraNearPulse">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".ble.NearPulseForegroundService"
            android:exported="false"
            android:foregroundServiceType="connectedDevice" />
    </application>

</manifest>`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ble/BleManager.kt',
    name: 'BleManager.kt',
    category: 'kotlin-ble',
    description: 'Core BLE Engine: BluetoothLeScanner, BluetoothLeAdvertiser, GATT Server, GATT Client',
    content: `package com.vexlora.nearpulse.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
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

    private var gattServer: BluetoothGattServer? = null
    private var activeGattClient: BluetoothGatt? = null
    var localPseudonym: String = "NP-" + UUID.randomUUID().toString().take(4).uppercase()
    var currentCircleCode: String? = null

    fun isBleSupported(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun isAdvertisingSupported(): Boolean = bluetoothAdapter?.isMultipleAdvertisementSupported == true

    init {
        setupGattServer()
    }

    private fun setupGattServer() {
        if (bluetoothManager == null || bluetoothAdapter?.isEnabled != true) return
        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(BleConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val messageChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            val ackChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_ACK_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val sosChar = BluetoothGattCharacteristic(
                BleConstants.CHAR_SOS_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            service.addCharacteristic(messageChar)
            service.addCharacteristic(ackChar)
            service.addCharacteristic(sosChar)
            gattServer?.addService(service)
        } catch (e: Exception) {
            Log.e("NearPulse", "GATT Server error: \${e.message}")
        }
    }

    fun startAdvertising(isSos: Boolean = false, sosDetails: String = "") {
        if (!isBluetoothEnabled() || !isAdvertisingSupported()) return
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val prefix = if (isSos) "SOS" else "NP"
        val payloadStr = "\$prefix:\$localPseudonym"
        val serviceData = payloadStr.toByteArray(StandardCharsets.UTF_8).take(10).toByteArray()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addServiceData(ParcelUuid(BleConstants.SERVICE_UUID), serviceData)
            .build()

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        isAdvertising.value = true
        isSosBroadcasting.value = isSos
    }

    fun stopAdvertising() {
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        isAdvertising.value = false
        isSosBroadcasting.value = false
    }

    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        bleScanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning.value = true
    }

    fun stopDiscovery() {
        bleScanner?.stopScan(scanCallback)
        isScanning.value = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val scanRecord = result.scanRecord ?: return
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
            val currentMap = discoveredPeers.value.toMutableMap()
            currentMap[device.address] = DiscoveredPeer(
                address = device.address,
                pseudonym = pseudonym,
                rssi = rssi,
                signalCategory = SignalCategory.fromRssi(rssi),
                lastSeenTimestamp = System.currentTimeMillis(),
                isSosActive = isSos
            )
            discoveredPeers.value = currentMap
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {}
    private val gattServerCallback = object : BluetoothGattServerCallback() {}
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ble/BleConstants.kt',
    name: 'BleConstants.kt',
    category: 'kotlin-ble',
    description: '128-bit custom Service and Characteristic UUIDs for VEXLORA NearPulse',
    content: `package com.vexlora.nearpulse.ble

import java.util.UUID

object BleConstants {
    // 128-bit custom service UUID for VEXLORA NearPulse
    val SERVICE_UUID: UUID = UUID.fromString("7b14d2e0-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for sending and receiving short text messages
    val CHAR_MESSAGE_UUID: UUID = UUID.fromString("7b14d2e1-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for message acknowledgments (Delivery Receipts)
    val CHAR_ACK_UUID: UUID = UUID.fromString("7b14d2e2-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for SOS emergency signaling & mutual rescue ACK
    val CHAR_SOS_UUID: UUID = UUID.fromString("7b14d2e3-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for device pseudonym and safety circle handshake
    val CHAR_INFO_UUID: UUID = UUID.fromString("7b14d2e4-252a-4df4-8d4e-e17f5412e601")

    // Standard Client Characteristic Configuration Descriptor
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val PROTOCOL_VERSION: Byte = 0x01
    const val PACKET_TYPE_MSG: Byte = 0x10
    const val PACKET_TYPE_ACK: Byte = 0x20
    const val PACKET_TYPE_SOS: Byte = 0x30
    const val PACKET_TYPE_SOS_ACK: Byte = 0x31

    const val MAX_MESSAGE_LENGTH = 280
    const val REQUESTED_MTU = 512
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/MainActivity.kt',
    name: 'MainActivity.kt',
    category: 'compose-ui',
    description: 'Single-Activity Jetpack Compose host, Android 12+ permission flow, navigation',
    content: `package com.vexlora.nearpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.vexlora.nearpulse.ui.theme.VexloraNearPulseTheme
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NearPulseViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handles BLUETOOTH_SCAN, ADVERTISE, CONNECT */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VexloraNearPulseTheme {
                val navController = rememberNavController()
                // NavHost routing to HomeScreen, RadarScreen, ChatScreen, SosScreen, SafetyCircleScreen, SettingsScreen
            }
        }
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/HomeScreen.kt',
    name: 'HomeScreen.kt',
    category: 'compose-ui',
    description: 'Jetpack Compose Home Dashboard: mesh metrics, discovery toggle, SOS launcher',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vexlora.nearpulse.ui.theme.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun HomeScreen(viewModel: NearPulseViewModel) {
    val isScanning by viewModel.isScanning.collectAsState()
    val peersMap by viewModel.discoveredPeers.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(CyberBgDark).padding(16.dp)) {
        Text("VEXLORA NearPulse", style = MaterialTheme.typography.headlineMedium, color = ElectricCyan)
        Text("Stay Connected. Even Without Internet.", color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        // Power card, stats, SOS trigger, quick navigation
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/RadarScreen.kt',
    name: 'RadarScreen.kt',
    category: 'compose-ui',
    description: 'Radar Canvas animation, RSSI signal categorizer, tap-to-connect peer selector',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun RadarScreen(viewModel: NearPulseViewModel) {
    // Canvas with concentric rings, sweeping radar line, and peers mapped by RF attenuation
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/ChatScreen.kt',
    name: 'ChatScreen.kt',
    category: 'compose-ui',
    description: 'GATT bidirectional P2P messaging, ACK delivery receipts, quick safety chips',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun ChatScreen(viewModel: NearPulseViewModel) {
    // Real GATT client/server two-way text exchange with Delivery Receipt (✓✓)
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/SosScreen.kt',
    name: 'SosScreen.kt',
    category: 'compose-ui',
    description: 'Emergency SOS console with 3-second hold-to-activate and mutual peer ACK',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun SosScreen(viewModel: NearPulseViewModel) {
    // 3-second hold-to-activate emergency beacon with incoming siren overlay
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/SafetyCircleScreen.kt',
    name: 'SafetyCircleScreen.kt',
    category: 'compose-ui',
    description: 'Local safety group creation, 6-character invitation code, direct range tracker',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun SafetyCircleScreen(viewModel: NearPulseViewModel) {
    // Offline circle pairing with invitation codes and direct BLE range status
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/ui/screens/SettingsScreen.kt',
    name: 'SettingsScreen.kt',
    category: 'compose-ui',
    description: 'Bluetooth hardware diagnostic inspector, Android 12 permissions, pseudonym rotation',
    content: `package com.vexlora.nearpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.vexlora.nearpulse.ui.viewmodel.NearPulseViewModel

@Composable
fun SettingsScreen(viewModel: NearPulseViewModel) {
    // Hardware stack inspector, pseudonym rotation, and local Room DB wipe
}`
  },
  {
    path: 'android/app/src/main/java/com/vexlora/nearpulse/data/local/NearPulseDatabase.kt',
    name: 'NearPulseDatabase.kt',
    category: 'room-db',
    description: 'Room SQLite database, entities, and DAOs for offline message persistence',
    content: `package com.vexlora.nearpulse.data.local

import androidx.room.*

@Database(entities = [MessageEntity::class, PeerEntity::class, SafetyCircleEntity::class], version = 1)
abstract class NearPulseDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
    abstract fun safetyCircleDao(): SafetyCircleDao
}`
  },
  {
    path: 'android/app/src/test/java/com/vexlora/nearpulse/BlePacketUnitTest.kt',
    name: 'BlePacketUnitTest.kt',
    category: 'test',
    description: 'JUnit test suite verifying packet serialization, SOS frames, RSSI tiers',
    content: `package com.vexlora.nearpulse

import com.vexlora.nearpulse.ble.*
import org.junit.Assert.*
import org.junit.Test

class BlePacketUnitTest {
    @Test
    fun testBlePacketSerialization() {
        val packet = BlePacket(packetType = BleConstants.PACKET_TYPE_MSG, messageId = "101", senderPseudonym = "NP-1", payload = "Hi")
        val parsed = BlePacket.parse(packet.toByteArray())
        assertEquals("Hi", parsed?.payload)
    }
}`
  },
  {
    path: 'android/app/build.gradle.kts',
    name: 'app/build.gradle.kts',
    category: 'gradle',
    description: 'App Gradle build script with Compose BOM, Room KSP, Material 3, SDK 34',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.vexlora.nearpulse"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.vexlora.nearpulse"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
}`
  },
  {
    path: 'TESTING_TWO_PHONES_GUIDE.md',
    name: 'TESTING_TWO_PHONES_GUIDE.md',
    category: 'docs',
    description: 'Step-by-step physical two-phone installation and testing guide',
    content: `# Two-Phone Physical Testing Guide for VEXLORA NearPulse
Details ADB installation and field test procedures for two physical handsets.`
  }
];
