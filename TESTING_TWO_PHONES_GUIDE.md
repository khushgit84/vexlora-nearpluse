# Two-Phone Physical Testing Guide for VEXLORA NearPulse

This guide details how to install and test **VEXLORA NearPulse** across two physical Android devices completely offline without Wi-Fi, cellular, or internet connection.

---

## 1. Quick ADB Installation

The compiled debug APK is located at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device 1 (Primary):
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <DEVICE_1_SERIAL> install -r "d:\vscode\vexlora nearpluse\android\app\build\outputs\apk\debug\app-debug.apk"
```

### Install on Device 2 (Secondary):
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <DEVICE_2_SERIAL> install -r "d:\vscode\vexlora nearpluse\android\app\build\outputs\apk\debug\app-debug.apk"
```

### Launch on both handsets:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.vexlora.nearpulse/.MainActivity
```

---

## 2. Test Procedure 1: Peer Discovery & 360° Radar

1. **Grant Permissions**: Upon first launch on both phones, accept the Bluetooth Scan, Advertise, and Nearby Devices runtime permissions.
2. **Turn On Radio on Phone 1**:
   - On the **Home** tab, tap **START SCAN** (turns green).
   - Tap **BROADCAST** (turns cyan).
3. **Turn On Radio on Phone 2**:
   - Tap **START SCAN** and **BROADCAST**.
4. **Inspect Radar**:
   - Switch to the **Radar** tab on Phone 1.
   - Observe the 360° rotating radar sweep.
   - The second phone will appear as a pulsing blip with its pseudonym (e.g. `NP-XXXX`).
   - Notice the radial distance ring according to physical separation:
     - **< 1m**: Immediate range (-40 to -60 dBm)
     - **1m - 5m**: Near range (-60 to -75 dBm)
     - **5m - 15m**: Mid range (-75 to -90 dBm)
     - **> 15m**: Far range (-90+ dBm)
   - Tap the blip to view signal telemetry (RSSI in dBm) and tap **OPEN CHAT**.

---

## 3. Test Procedure 2: Direct 100% Offline P2P Chat

1. In the **Chat** tab, select the detected peer from the top bar or radar.
2. Send a custom text message (or tap preset chips like *"Are you safe?"*).
3. **Delivery Acknowledgment (✓✓)**:
   - Initial transmission shows a single checkmark (`✓`).
   - When the receiver handset processes the packet over BLE GATT, it automatically returns an ACK packet, updating the status to a double checkmark (`✓✓`).
4. All messages are stored permanently in local SQLite **Room** persistence on both devices.

---

## 4. Test Procedure 3: Emergency SOS Distress Beacon & Background Alarms

1. **Background Siren & Heads-Up Notification**:
   - When any device running NearPulse receives an incoming SOS distress packet (even while the app is minimized in the background or device is locked):
     - **Audible Siren**: Plays high-priority system alarm sound (`TYPE_ALARM`).
     - **SOS Morse Code Vibration**: Phone hardware vibrator fires the international SOS sequence (`... --- ...` : 3 short pulses, 3 long pulses, 3 short pulses).
     - **Heads-Up Banner**: High-priority notification pops up over any active app with the sender's callsign, signal strength (in dBm), and distance estimate.
     - **Direct Rescue Action**: Tapping the notification immediately wakes the device and opens the SOS radar.

2. **Triggering from Phone 1**:
   - Navigate to the **SOS** tab.
   - Select an emergency category (*"MEDICAL EMERGENCY"* or *"TRAPPED / PHYSICAL HAZARD"*).
   - **Hold the circular SOS button for 3 seconds** (animated progress ring fills to prevent accidental activation).
   - Phone 1 provides tactile vibration feedback confirming high-gain distress pulses are broadcasting.

3. **Self-Drill Simulator (Immediate or Background)**:
   - On the **SOS** screen, scroll down to the **EMERGENCY DRILL SIMULATOR** card.
   - Tap **TEST NOW** to immediately trigger the siren, vibration, and heads-up banner.
   - Or tap **TEST IN BG (5s)**, minimize the app or lock the screen, and observe the emergency alarm trigger after 5 seconds!

---

## 5. Test Procedure 4: Mesh Safety Circle Grouping

1. On both phones, go to the **Circle** tab.
2. Tap **SWITCH CIRCLE** and enter the same code on both devices (e.g., `ALPHA-7`).
3. Both devices will now recognize each other as official circle team members (highlighted with an Emerald Green glow on radar).
4. Tap any of the **ONE-TAP CIRCLE CHECK-IN** buttons (e.g., *"ALL MEMBERS SAFE & ACCOUNTED FOR"* or *"MEETING AT EXTRACTION POINT ALPHA"*) to broadcast a group status ping to all circle members in range.