# VEXLORA NearPulse 📡
> **"Stay Connected. Even Without Internet."**

VEXLORA NearPulse is a modern, native Android offline-first safety and peer-to-peer communication application built with Kotlin, Jetpack Compose, Material 3, and native Android Bluetooth Low Energy (BLE) APIs.

---

## ⚡ Core Architecture

- **Runtime & UI**: Native Android (Kotlin), Jetpack Compose, Material 3 Cyberpunk Dark Theme (`#070A0F`, Electric Cyan `#00F0FF`, Neon Violet `#A855F7`, SOS Red `#FF1744`).
- **Bluetooth Subsystem**:
  - `BluetoothLeScanner`: Scans for devices advertising the custom 128-bit NearPulse Service UUID (`7b14d2e0-252a-4df4-8d4e-e17f5412e601`).
  - `BluetoothLeAdvertiser`: Broadcasts the device's ephemeral pseudonym and SOS distress state in BLE Service Data.
  - `BluetoothGattServer`: Hosts GATT primary service with Characteristics for Message, Delivery ACK, Emergency Beacon, and Info Handshake.
  - `BluetoothGatt` (Client): Connects to remote peer, negotiates MTU 512, and performs bidirectional packet exchange with acknowledgments.
- **Local Persistence**: Room SQLite database (`NearPulseDatabase`) with entities for `MessageEntity`, `PeerEntity`, and `SafetyCircleEntity`.
- **State Management**: Kotlin Coroutines + `StateFlow` + `SharedFlow` with lifecycle-aware ViewModels.
- **Foreground Safety Service**: `NearPulseForegroundService` maintains BLE peripheral discovery and distress beacon listeners with Android 14 `connectedDevice` foreground service type.

---

## 🛡️ Permissions & Android 12+ Compatibility

The app adheres strictly to the Android 12 (API 31+) Bluetooth permissions:

```xml
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

- **`neverForLocation`**: Guarantees user privacy; the app does not access or derive physical GPS coordinates from Bluetooth beacons.
- **Legacy Compatibility**: On Android 6 to 11, requests `ACCESS_FINE_LOCATION` (with `maxSdkVersion="30"`) solely because older Android OS versions required location to scan BLE beacons.
- **Notification & Vibration**: `POST_NOTIFICATIONS` and `VIBRATE` for emergency SOS siren alerts.

---

## 📱 Hardware Requirements & Two-Phone Testing

For detailed instructions on flashing and testing between two physical Android devices, consult `TESTING_TWO_PHONES_GUIDE.md`.

*Note: Android Virtual Devices (emulators) do not support Bluetooth LE Peripheral Advertising or physical RF signal attenuation. Testing bidirectional messaging requires two physical Android handsets with Bluetooth 4.2+ (Bluetooth 5.0+ recommended).*
