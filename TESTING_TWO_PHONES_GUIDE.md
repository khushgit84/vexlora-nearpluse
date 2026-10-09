# Two-Phone Physical Testing Guide for VEXLORA NearPulse

This guide details how to build, install, and verify the offline BLE mesh features between two physical Android devices.

---

## 🛠️ Step 1: Open and Build in Android Studio

1. **Prerequisites**:
   - Android Studio (Flamingo, Hedgehog, Iguana, Jellyfish, or newer).
   - JDK 17 (set in Android Studio -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK).
   - Android SDK 34 installed.
   - Two physical Android phones running Android 8.0 (API 26) through Android 15.

2. **Open Project**:
   - Open Android Studio and select **Open** -> choose the `android` directory.
   - Wait for Gradle sync to complete.

3. **Build APK via Gradle Command Line**:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   The debug APK will be generated at:
   `android/app/build/outputs/apk/debug/app-debug.apk`

---

## 📲 Step 2: Install on Two Physical Handsets

Connect Phone 1 via USB with USB Debugging enabled:
```bash
adb -s <DEVICE_1_SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
```

Connect Phone 2 via USB with USB Debugging enabled:
```bash
adb -s <DEVICE_2_SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Step 3: Step-by-Step Test Scenarios

### Test 1: Nearby Radar Discovery
1. Launch **VEXLORA NearPulse** on both Phone 1 and Phone 2.
2. Grant Bluetooth scan, advertise, and connect permissions on both phones.
3. On Phone 1, tap **Start Nearby Discovery**. Phone 1 begins broadcasting its NearPulse UUID (`7b14d2e0`) and pseudonym (e.g. `NP-8A2F`).
4. On Phone 2, tap **Bluetooth Radar**.
5. **Expected Result**: Phone 2's radar sweep detects Phone 1 within 1–3 seconds, plotting it on the radar with its real RSSI category (e.g. "Excellent Signal (-55 dBm)") and "Just now" timestamp.

### Test 2: Two-Way Offline P2P GATT Messaging
1. On Phone 2's Radar screen, tap on Phone 1 (`NP-8A2F`) and select **Connect**.
2. Phone 2 opens a GATT client connection to Phone 1.
3. Phone 2 sends a message: *"Offline test: all clear"*.
4. **Expected Result**:
   - Phone 1's GATT server receives the write packet on `CHAR_MESSAGE_UUID`.
   - Phone 1 automatically transmits a confirmation receipt on `CHAR_ACK_UUID`.
   - Phone 2 updates the message status to `DELIVERED (✓✓)`.
   - Phone 1 replies with quick preset: *"I'm safe"*. Phone 2 receives the reply without cellular or Wi-Fi data.

### Test 3: Emergency SOS Beacon Broadcast & Mutual Acknowledgement
1. On Phone 1, open **SOS Console**.
2. Press and hold the SOS activation circle for **3 continuous seconds** (progress bar reaches 100%).
3. Phone 1 begins transmitting high-priority emergency BLE advertisement packets.
4. On Phone 2, a prominent red emergency overlay immediately pops up:
   `"EMERGENCY SOS DETECTED: NP-8A2F needs assistance within BLE range!"`
5. On Phone 2, tap **Acknowledge Help**.
6. Phone 2 connects over GATT and writes a rescue confirmation packet.
7. Phone 1 confirms that assistance has been acknowledged by a nearby peer.
8. On Phone 1, tap **Cancel Emergency SOS** to terminate the emergency broadcast.

### Test 4: Safety Circle Offline Handshake
1. On Phone 1, navigate to **Safety Circle** and tap **Create Circle** (e.g., name: "Alpha Squad").
2. Phone 1 generates a 6-character code (e.g. `NP-4921`).
3. On Phone 2, tap **Enter Code** and input `NP-4921`.
4. Both devices register the circle ID in local Room storage and advertise the circle hash.
5. In the member list, Phone 1 and Phone 2 appear as detected circle members.

---

## ⚠️ Known Hardware & OS Limitations

1. **Android Virtual Device (Emulator)**: Emulators do NOT have real Bluetooth chips and cannot advertise or measure physical RSSI attenuation. Testing MUST be done on real phones.
2. **Line of Sight & Physical Range**: Direct Bluetooth LE range is typically 10 to 30 meters. Thick concrete walls or metal barriers reduce signal strength.
3. **OS Background Restrictions**: Android aggressively throttles BLE scanning when the screen is locked unless running in foreground with the included `NearPulseForegroundService`.
