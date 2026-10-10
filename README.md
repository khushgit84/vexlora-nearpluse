# VEXLORA NearPulse 📡

> **100% Offline Decentralized 2.4 GHz Bluetooth Low Energy Radio Mesh for Android & iOS**
> Zero Cellular. Zero Wi-Fi. Zero Cloud. Zero Logins.

---

## ⚡ Overview

**VEXLORA NearPulse** turns standard smartphones into encrypted, decentralized radio nodes. Using native Android BLE and Apple CoreBluetooth APIs, NearPulse forms an ad-hoc local radio mesh for proximity direct chat, tactical RF spectrum radar tracking, and urgent emergency SOS mutual-rescue alerts.

- **Live Web Portal**: [https://khushgit84.github.io/vexlora-nearpluse/](https://khushgit84.github.io/vexlora-nearpluse/)
- **Direct APK Download**: [Download v1.0.0 APK (18 MB)](https://github.com/khushgit84/vexlora-nearpluse/releases/download/v1.0.0/vexlora-nearpulse.apk)
- **Native iOS Project**: [`/ios/VexloraNearPulse.xcodeproj`](ios/VexloraNearPulse.xcodeproj)

---

## 🏗️ Architecture

```
vexlora-nearpulse/
├── android/               # Native Android (Kotlin, Jetpack Compose, Room SQLite, BLE GATT)
├── ios/                   # Native iOS (Swift, SwiftUI, CoreBluetooth Central/Peripheral)
├── web/                   # Cyberpunk Web Portal & Live 2.4 GHz Radar Simulator
└── TESTING_TWO_PHONES_GUIDE.md # Step-by-step physical two-phone test manual
```

### 📡 Shared 128-bit GATT Protocol Profile
| Characteristic | UUID | Description |
| :--- | :--- | :--- |
| **Primary Service** | `7B14D2E0-252A-4DF4-8D4E-E17F5412E601` | Primary NearPulse Mesh Service |
| **Message Char** | `7B14D2E1-252A-4DF4-8D4E-E17F5412E601` | Direct P2P text message transmission |
| **ACK Delivery** | `7B14D2E2-252A-4DF4-8D4E-E17F5412E601` | Delivery receipt acknowledgement |
| **Emergency SOS** | `7B14D2E3-252A-4DF4-8D4E-E17F5412E601` | Emergency distress beacon & cancel trigger |
| **Safety Info** | `7B14D2E4-252A-4DF4-8D4E-E17F5412E601` | Ephemeral node pseudonym handshake |

---

## 📱 Features

1. **RF Spectrum Radar HUD**: Calculates real-time signal strength (RSSI), categorizing nearby nodes into Immediate (<1m), Near (1-5m), and Mid-Range (5-15m).
2. **Smart Phone-Only Filter**: Automatically filters out Bluetooth TVs, set-top boxes, smart bulbs, and peripheral RF noise.
3. **Encrypted Direct Chat**: Low-latency P2P text messaging via BLE GATT with delivery receipts (`Delivered ✓`).
4. **Emergency SOS & Cutoff**: 3-second hold-to-activate distress alarm with high-gain siren and Morse code vibrations. Tapping **"Problem Solved"** immediately silences the siren and cancels the alert across the mesh.

---

## 📄 License
MIT License. Air-gapped and decentralized.
