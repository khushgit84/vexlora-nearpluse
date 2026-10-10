# VEXLORA NearPulse — Native iOS App (Swift & SwiftUI)

## 📡 100% Offline 2.4 GHz BLE Mesh Communication for iPhone & iPad

`VEXLORA NearPulse for iOS` is a native, air-gapped proximity communication client built with **SwiftUI** and **Apple CoreBluetooth**. It is **100% wire-compatible** with the Android Kotlin Jetpack Compose client, enabling seamless cross-platform offline messaging, spectrum radar tracking, and urgent emergency SOS distress alerts.

---

## 🚀 Key Features

1. **Air-Gapped Radio Links**:
   - Zero cellular data. Zero Wi-Fi internet. Zero cloud accounts.
   - Dual-Role BLE: Operates concurrently as `CBCentralManager` (scanner & client) and `CBPeripheralManager` (advertiser & GATT server).
2. **RF Spectrum Radar HUD**:
   - Real-time sweeping visual radar calculating signal RSSI.
   - Proximity categorization: *Immediate (<1m)*, *Near (1-5m)*, *Mid-Range (5-15m)*, *Far (>15m)*.
3. **Smart Phone-Only Heuristic Filter**:
   - Automatically filters out Bluetooth TVs, set-top boxes, smart bulbs, and peripheral noise.
   - Whitelists only genuine smartphones (`iPhone`, `Redmi`, `OnePlus`, `Galaxy`, `Pixel`, `Xiaomi`, etc.).
4. **Encrypted Direct Chat**:
   - Low-latency P2P text messaging via CoreBluetooth `writeValue` with response.
   - Guaranteed delivery receipts (`Delivered ✓` ACK packet).
   - Heads-up system notifications outside the app with sender callsign and text preview.
5. **Tactical Emergency SOS Beacon & Instant Cutoff**:
   - 3-second hold-to-activate emergency distress beacon.
   - High-gain audio siren warble and Morse code vibration pattern (`... --- ...`).
   - **"Problem Solved (Stop Siren)"** action immediately silences the alarm and notifies all mesh peers that the distress has been resolved.

---

## 🔗 Shared Cross-Platform Protocol Specifications

| Protocol Field | 128-bit UUID / Value | Description |
| :--- | :--- | :--- |
| **Service UUID** | `7B14D2E0-252A-4DF4-8D4E-E17F5412E601` | Primary VEXLORA NearPulse GATT Mesh Service |
| **Message Char** | `7B14D2E1-252A-4DF4-8D4E-E17F5412E601` | Direct P2P text message transmission (Write / Notify) |
| **ACK Char** | `7B14D2E2-252A-4DF4-8D4E-E17F5412E601` | Delivery receipt acknowledgement (Write / Notify) |
| **Emergency SOS Char** | `7B14D2E3-252A-4DF4-8D4E-E17F5412E601` | Maximum gain SOS beacon & cancel trigger (Write / Notify) |
| **Safety Circle Char**| `7B14D2E4-252A-4DF4-8D4E-E17F5412E601` | Node metadata & pseudonym handshake (Read / Write) |
| **Protocol Version**| `0x01` | Byte header version |
| **Packet Type MSG** | `0x10` | Standard chat message packet |
| **Packet Type ACK** | `0x20` | Delivery ACK receipt packet |
| **Packet Type SOS** | `0x30` | Emergency distress beacon packet |
| **Packet Type SOS_CANCEL** | `0x32` | Emergency problem solved / silence siren packet |

---

## 🛠 Project Structure

```
ios/
├── VexloraNearPulse.xcodeproj/      # Native Xcode Project configuration
└── VexloraNearPulse/
    ├── App/
    │   └── VexloraNearPulseApp.swift # SwiftUI App lifecycle
    ├── Core/
    │   ├── BleConstants.swift        # Shared 128-bit GATT UUIDs
    │   ├── BlePacket.swift           # Byte-for-byte binary packet wire codec
    │   ├── BleManager.swift          # Dual-role CoreBluetooth engine & phone filter
    │   └── EmergencyAlertManager.swift # High-gain siren, Morse haptics & instant stop
    ├── Models/
    │   ├── PeerNode.swift            # Discovered node model with RSSI tiering
    │   ├── ChatMessage.swift         # Chat message model with delivery status
    │   └── SosAlert.swift            # SOS distress alert model
    ├── Views/
    │   ├── ContentView.swift         # Cyberpunk dark theme bottom tab bar
    │   ├── RadarView.swift           # Sweeping RF spectrum radar canvas
    │   ├── ChatView.swift            # Direct peer-to-peer messaging
    │   ├── SosView.swift             # 3-second hold beacon & "Problem Solved" button
    │   └── SettingsView.swift        # Node pseudonym & RSSI threshold slider
    └── Resources/
        └── Info.plist                # CoreBluetooth background execution entitlements
```

---

## 📲 How To Build & Run on iPhone / iPad

### Method 1: Open in Xcode (macOS)
1. Open the folder in macOS Finder.
2. Double-click `VexloraNearPulse.xcodeproj` to open the project in **Xcode 15 or 16**.
3. Select your connected iPhone from the scheme selector.
4. Go to **Signing & Capabilities** -> Select your **Personal Team** (Free Apple ID works).
5. Press **Cmd + R** to build and run directly on your iPhone.

### Method 2: Sideloading without Mac (AltStore / Sideloadly)
1. Build the `.ipa` archive using Xcode or `xcodebuild`.
2. Sideload onto any non-jailbroken iPhone using **AltStore**, **SideStore**, or **Sideloadly**.
3. Trust your developer profile under **iOS Settings -> General -> VPN & Device Management**.

---

## 🧪 Testing Cross-Platform Android + iPhone Communication
1. **Launch Android App**: Open `VEXLORA NearPulse` on your Android device (e.g., OnePlus or Redmi).
2. **Launch iOS App**: Open `NearPulse` on your iPhone.
3. Both devices will appear on each other's **Radar screen** with real-time RSSI signal strength!
4. Tap the detected node to exchange messages offline over Bluetooth Low Energy.
5. Trigger SOS on either device — the other phone will immediately sound the emergency siren with Morse code vibrations! Tap **"Problem Solved"** to instantly silence the siren.
