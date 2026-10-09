import React from 'react';
import { Smartphone, Terminal, Shield, AlertTriangle, CheckCircle, WifiOff, Cpu, Radio } from 'lucide-react';

export const TwoPhoneGuide: React.FC = () => {
  return (
    <div className="bg-[#0A0E17] border border-slate-800 rounded-2xl p-6 space-y-6 text-slate-200 shadow-2xl">
      {/* Title & Introduction */}
      <div className="flex items-start justify-between border-b border-slate-800 pb-4">
        <div>
          <div className="flex items-center gap-2 text-cyan-400 font-mono text-xs font-bold uppercase tracking-wider">
            <Radio className="w-4 h-4" />
            Field Testing Specification
          </div>
          <h2 className="text-xl font-black text-white mt-1">Two Physical Android Phones Testing Protocol</h2>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl">
            VEXLORA NearPulse uses real hardware Bluetooth Low Energy (BLE) peripheral advertising and GATT servers.
            Because Android emulators (AVDs) lack physical BLE peripheral advertising chips, testing must be conducted on two real Android smartphones.
          </p>
        </div>
        <div className="px-3 py-1 bg-amber-950/40 border border-amber-800/80 rounded-xl text-amber-300 text-xs font-mono flex items-center gap-1.5">
          <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
          <span>Real Hardware Only</span>
        </div>
      </div>

      {/* 4 Steps Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Step 1 */}
        <div className="bg-[#0E1422] border border-slate-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-cyan-300 font-bold text-sm">
            <span className="w-6 h-6 rounded-full bg-cyan-950 border border-cyan-800 flex items-center justify-center text-xs text-cyan-400 font-mono">1</span>
            Import Into Android Studio
          </div>
          <p className="text-xs text-slate-400">
            Open the <code className="text-cyan-300 bg-slate-900 px-1 py-0.5 rounded">android/</code> directory in Android Studio Hedgehog or newer. Ensure Gradle uses JDK 17 and Android SDK 34 (UpsideDownCake) is installed.
          </p>
          <div className="bg-[#070A0F] border border-slate-800/80 rounded-lg p-2.5 font-mono text-[11px] text-slate-300 flex items-center justify-between">
            <span>cd android && ./gradlew assembleDebug</span>
          </div>
        </div>

        {/* Step 2 */}
        <div className="bg-[#0E1422] border border-slate-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-purple-300 font-bold text-sm">
            <span className="w-6 h-6 rounded-full bg-purple-950 border border-purple-800 flex items-center justify-center text-xs text-purple-400 font-mono">2</span>
            Deploy via ADB to Handset 1 & 2
          </div>
          <p className="text-xs text-slate-400">
            Enable USB Debugging on both phones. Flash the debug APK to both devices using ADB:
          </p>
          <div className="bg-[#070A0F] border border-slate-800/80 rounded-lg p-2.5 font-mono text-[11px] text-slate-300">
            adb -s DEVICE_1 install -r app-debug.apk<br/>
            adb -s DEVICE_2 install -r app-debug.apk
          </div>
        </div>

        {/* Step 3 */}
        <div className="bg-[#0E1422] border border-slate-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-emerald-300 font-bold text-sm">
            <span className="w-6 h-6 rounded-full bg-emerald-950 border border-emerald-800 flex items-center justify-center text-xs text-emerald-400 font-mono">3</span>
            Permit Android 12+ BLE Permissions
          </div>
          <p className="text-xs text-slate-400">
            When prompted, grant <strong className="text-slate-200">Nearby Devices</strong> permissions. NearPulse uses <code className="text-cyan-300">neverForLocation</code> flag so no GPS tracking occurs.
          </p>
          <div className="flex flex-wrap gap-1.5 pt-1">
            <span className="text-[10px] font-mono px-2 py-0.5 bg-slate-900 border border-slate-700 rounded text-slate-300">BLUETOOTH_SCAN</span>
            <span className="text-[10px] font-mono px-2 py-0.5 bg-slate-900 border border-slate-700 rounded text-slate-300">BLUETOOTH_ADVERTISE</span>
            <span className="text-[10px] font-mono px-2 py-0.5 bg-slate-900 border border-slate-700 rounded text-slate-300">BLUETOOTH_CONNECT</span>
          </div>
        </div>

        {/* Step 4 */}
        <div className="bg-[#0E1422] border border-slate-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-rose-300 font-bold text-sm">
            <span className="w-6 h-6 rounded-full bg-rose-950 border border-rose-800 flex items-center justify-center text-xs text-rose-400 font-mono">4</span>
            Verify Offline P2P GATT Messaging
          </div>
          <p className="text-xs text-slate-400">
            Turn Airplane mode ON with Bluetooth ON. Handset 1 discovers Handset 2 on Radar. Tap Connect, send a text, and observe instantaneous delivery ACK <code className="text-emerald-400">✓✓</code>.
          </p>
          <div className="text-[11px] text-emerald-400 font-mono flex items-center gap-1">
            <CheckCircle className="w-3.5 h-3.5" />
            <span>Fully functional without internet or SIM card</span>
          </div>
        </div>
      </div>

      {/* Detailed Testing Scenarios */}
      <div className="bg-[#0E1422] border border-slate-800 rounded-xl p-4 space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider font-mono">
          Interactive Test Verification Checklist
        </h3>

        <div className="space-y-2 text-xs">
          <div className="p-2.5 bg-[#070A0F] border border-slate-800 rounded-lg flex items-start gap-2.5">
            <CheckCircle className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-slate-200">Radar Discovery & RSSI Attenuation</div>
              <div className="text-slate-400 text-[11px]">
                Bring Phone 1 close (0.5m) to Phone 2: RSSI reads ~ -45 dBm (Excellent). Move 15m away: RSSI falls to ~ -85 dBm (Moderate/Weak). Proves genuine RF signal reading without distance hallucination.
              </div>
            </div>
          </div>

          <div className="p-2.5 bg-[#070A0F] border border-slate-800 rounded-lg flex items-start gap-2.5">
            <CheckCircle className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-slate-200">Bidirectional GATT Characteristics</div>
              <div className="text-slate-400 text-[11px]">
                Phone 1 acts as GATT Client writing to Phone 2's GATT Server on characteristic <code className="text-cyan-300">7b14d2e1</code>. Phone 2's server acknowledges receipt on characteristic <code className="text-cyan-300">7b14d2e2</code>.
              </div>
            </div>
          </div>

          <div className="p-2.5 bg-[#070A0F] border border-slate-800 rounded-lg flex items-start gap-2.5">
            <CheckCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-slate-200">Emergency SOS 3-Second Hold & Mutual Rescue ACK</div>
              <div className="text-slate-400 text-[11px]">
                Hold SOS button for 3 continuous seconds on Phone 1. Phone 2 immediately triggers audio-visual emergency banner. Tap "Acknowledge Help" on Phone 2 to send confirmation receipt back to Phone 1.
              </div>
            </div>
          </div>

          <div className="p-2.5 bg-[#070A0F] border border-slate-800 rounded-lg flex items-start gap-2.5">
            <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-slate-200">Local Safety Circle Offline Key Exchange</div>
              <div className="text-slate-400 text-[11px]">
                Generate circle code on Phone 1 (e.g. <code className="text-cyan-300">NP-4921</code>). Enter code on Phone 2. Both devices bind the circle key in local Room database storage.
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Known Limitations */}
      <div className="bg-amber-950/20 border border-amber-900/60 rounded-xl p-4 text-xs space-y-1.5">
        <div className="text-amber-400 font-bold flex items-center gap-1.5">
          <AlertTriangle className="w-4 h-4" />
          Hardware & OS Operating System Boundaries
        </div>
        <ul className="list-disc list-inside text-slate-400 space-y-1 text-[11px]">
          <li><strong>No Emulator Support</strong>: Android Virtual Devices cannot broadcast BLE advertising packets. Must be verified on two real handsets.</li>
          <li><strong>RF Range</strong>: Direct BLE line-of-sight range is ~10 to 30 meters. Concrete walls, metallic obstacles, and high 2.4 GHz interference will reduce packet delivery.</li>
          <li><strong>Doze Mode & Battery Throttling</strong>: NearPulse includes <code className="text-cyan-300">NearPulseForegroundService</code> with <code className="text-cyan-300">connectedDevice</code> type to prevent Android OS from putting BLE radio to sleep.</li>
        </ul>
      </div>
    </div>
  );
};
