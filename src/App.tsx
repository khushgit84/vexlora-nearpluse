import React, { useState } from 'react';
import { PhoneSimulator } from './components/PhoneSimulator';
import { CodeExplorer } from './components/CodeExplorer';
import { TwoPhoneGuide } from './components/TwoPhoneGuide';
import { WebBluetoothScanner } from './components/WebBluetoothScanner';
import { InstallPhoneModal } from './components/InstallPhoneModal';
import { DownloadHub } from './components/DownloadHub';
import { SimulatedPeer, ChatMessage, SosAlertState } from './types';
import {
  Smartphone,
  Code,
  FileText,
  Radio,
  Zap,
  ShieldAlert,
  Send,
  Users,
  RotateCcw,
  Download,
  Terminal,
  ExternalLink,
  Layers,
  Sparkles
} from 'lucide-react';
import JSZip from 'jszip';
import { ANDROID_FILES } from './data/androidFiles';

type MainViewMode = 'download' | 'dual-lab' | 'code-explorer' | 'field-guide' | 'web-ble';

export default function App() {
  const [viewMode, setViewMode] = useState<MainViewMode>('download');
  const [showInstallModal, setShowInstallModal] = useState(false);

  // Device A State (Vexlora Alpha)
  const [deviceAPseudonym, setDeviceAPseudonym] = useState('NP-ALPHA');
  const [deviceADiscovering, setDeviceADiscovering] = useState(true);
  const [deviceAActivePeer, setDeviceAActivePeer] = useState<SimulatedPeer | null>(null);
  const [deviceABatteryLevel, setDeviceABatteryLevel] = useState(84);
  const [deviceAPowerMode, setDeviceAPowerMode] = useState<import('./types').BlePowerMode>('BALANCED');
  const [deviceARssiHistory, setDeviceARssiHistory] = useState<number[]>([-56, -60, -58, -62, -55, -59, -58, -57, -61, -58]);

  const [deviceAMessages, setDeviceAMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      senderPseudonym: 'NP-BETA',
      recipientAddress: 'AA:BB:CC:11:22:33',
      content: 'Offline mesh link established.',
      timestamp: '12:44',
      isOutgoing: false,
      status: 'DELIVERED'
    }
  ]);
  const [deviceASosActive, setDeviceASosActive] = useState(false);
  const [deviceAIncomingSos, setDeviceAIncomingSos] = useState<SosAlertState | null>(null);
  const [deviceACircle, setDeviceACircle] = useState<{ code: string; name: string } | null>({
    code: 'NP-7421',
    name: 'Expedition Squad'
  });

  // Device B State (Vexlora Beta)
  const [deviceBPseudonym, setDeviceBPseudonym] = useState('NP-BETA');
  const [deviceBDiscovering, setDeviceBDiscovering] = useState(true);
  const [deviceBActivePeer, setDeviceBActivePeer] = useState<SimulatedPeer | null>(null);
  const [deviceBBatteryLevel, setDeviceBBatteryLevel] = useState(76);
  const [deviceBPowerMode, setDeviceBPowerMode] = useState<import('./types').BlePowerMode>('BALANCED');
  const [deviceBRssiHistory, setDeviceBRssiHistory] = useState<number[]>([-56, -60, -58, -62, -55, -59, -58, -57, -61, -58]);

  const [deviceBMessages, setDeviceBMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      senderPseudonym: 'NP-BETA',
      recipientAddress: 'AA:BB:CC:11:22:33',
      content: 'Offline mesh link established.',
      timestamp: '12:44',
      isOutgoing: true,
      status: 'DELIVERED'
    }
  ]);
  const [deviceBSosActive, setDeviceBSosActive] = useState(false);
  const [deviceBIncomingSos, setDeviceBIncomingSos] = useState<SosAlertState | null>(null);
  const [deviceBCircle, setDeviceBCircle] = useState<{ code: string; name: string } | null>({
    code: 'NP-7421',
    name: 'Expedition Squad'
  });

  // Base RSSI for distance simulation
  const [simulatedDistanceRssi, setSimulatedDistanceRssi] = useState(-58);

  // Periodic natural RF RSSI fluctuation (simulates realistic wireless multipath fading)
  React.useEffect(() => {
    const interval = setInterval(() => {
      const jitter = Math.floor(Math.random() * 7) - 3; // -3 to +3 dBm
      const newRssi = Math.max(-98, Math.min(-42, simulatedDistanceRssi + jitter));

      setDeviceARssiHistory((prev) => [...prev.slice(-14), newRssi]);
      setDeviceBRssiHistory((prev) => [...prev.slice(-14), newRssi]);
    }, 2800);
    return () => clearInterval(interval);
  }, [simulatedDistanceRssi]);

  const currentRssiA = deviceARssiHistory[deviceARssiHistory.length - 1] || -58;
  const currentRssiB = deviceBRssiHistory[deviceBRssiHistory.length - 1] || -58;

  // Discovered peers for Device A (sees Device B)
  const peersForA: SimulatedPeer[] = deviceBDiscovering
    ? [
        {
          id: 'dev-b',
          name: 'Device B',
          pseudonym: deviceBPseudonym,
          address: 'BB:CC:DD:44:55:66',
          rssi: currentRssiA,
          rssiHistory: deviceARssiHistory,
          signalStrength: currentRssiA >= -65 ? 'EXCELLENT' : currentRssiA >= -75 ? 'STRONG' : 'MODERATE',
          lastSeen: 'Just now',
          isSos: deviceBSosActive,
          circleCode: deviceBCircle?.code,
          connected: deviceAActivePeer?.id === 'dev-b',
          batteryLevel: deviceBBatteryLevel,
          powerMode: deviceBBatteryLevel <= 20 ? 'BATTERY_SAVER' : deviceBPowerMode,
          batteryHealth: deviceBBatteryLevel > 60 ? 'OPTIMAL' : deviceBBatteryLevel > 20 ? 'NORMAL' : 'CRITICAL'
        }
      ]
    : [];

  // Discovered peers for Device B (sees Device A)
  const peersForB: SimulatedPeer[] = deviceADiscovering
    ? [
        {
          id: 'dev-a',
          name: 'Device A',
          pseudonym: deviceAPseudonym,
          address: 'AA:BB:CC:11:22:33',
          rssi: currentRssiB,
          rssiHistory: deviceBRssiHistory,
          signalStrength: currentRssiB >= -65 ? 'EXCELLENT' : currentRssiB >= -75 ? 'STRONG' : 'MODERATE',
          lastSeen: 'Just now',
          isSos: deviceASosActive,
          circleCode: deviceACircle?.code,
          connected: deviceBActivePeer?.id === 'dev-a',
          batteryLevel: deviceABatteryLevel,
          powerMode: deviceABatteryLevel <= 20 ? 'BATTERY_SAVER' : deviceAPowerMode,
          batteryHealth: deviceABatteryLevel > 60 ? 'OPTIMAL' : deviceABatteryLevel > 20 ? 'NORMAL' : 'CRITICAL'
        }
      ]
    : [];

  // P2P Message Transfer from A -> B
  const handleSendFromA = (text: string) => {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const msgId = 'msg-' + Date.now();

    const outMsg: ChatMessage = {
      id: msgId,
      senderPseudonym: deviceAPseudonym,
      recipientAddress: 'BB:CC:DD:44:55:66',
      content: text,
      timestamp: time,
      isOutgoing: true,
      status: 'DELIVERED'
    };

    const inMsg: ChatMessage = {
      id: msgId,
      senderPseudonym: deviceAPseudonym,
      recipientAddress: 'BB:CC:DD:44:55:66',
      content: text,
      timestamp: time,
      isOutgoing: false,
      status: 'DELIVERED'
    };

    setDeviceAMessages((prev) => [...prev, outMsg]);
    setDeviceBMessages((prev) => [...prev, inMsg]);
  };

  // P2P Message Transfer from B -> A
  const handleSendFromB = (text: string) => {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const msgId = 'msg-' + Date.now();

    const outMsg: ChatMessage = {
      id: msgId,
      senderPseudonym: deviceBPseudonym,
      recipientAddress: 'AA:BB:CC:11:22:33',
      content: text,
      timestamp: time,
      isOutgoing: true,
      status: 'DELIVERED'
    };

    const inMsg: ChatMessage = {
      id: msgId,
      senderPseudonym: deviceBPseudonym,
      recipientAddress: 'AA:BB:CC:11:22:33',
      content: text,
      timestamp: time,
      isOutgoing: false,
      status: 'DELIVERED'
    };

    setDeviceBMessages((prev) => [...prev, outMsg]);
    setDeviceAMessages((prev) => [...prev, inMsg]);
  };

  // SOS Triggered on A -> Alert B
  const handleStartSosOnA = () => {
    setDeviceASosActive(true);
    setDeviceBIncomingSos({
      alertId: 'sos-' + Date.now(),
      broadcasterName: deviceAPseudonym,
      broadcasterAddress: 'AA:BB:CC:11:22:33',
      details: 'Emergency SOS beacon active nearby',
      timestamp: 'Just now',
      isAcknowledged: false
    });
  };

  const handleStopSosOnA = () => {
    setDeviceASosActive(false);
    setDeviceBIncomingSos(null);
  };

  // SOS Triggered on B -> Alert A
  const handleStartSosOnB = () => {
    setDeviceBSosActive(true);
    setDeviceAIncomingSos({
      alertId: 'sos-' + Date.now(),
      broadcasterName: deviceBPseudonym,
      broadcasterAddress: 'BB:CC:DD:44:55:66',
      details: 'Emergency SOS beacon active nearby',
      timestamp: 'Just now',
      isAcknowledged: false
    });
  };

  const handleStopSosOnB = () => {
    setDeviceBSosActive(false);
    setDeviceAIncomingSos(null);
  };

  // Reset entire simulated mesh
  const handleResetLab = () => {
    setDeviceADiscovering(true);
    setDeviceBDiscovering(true);
    setDeviceASosActive(false);
    setDeviceBSosActive(false);
    setDeviceAIncomingSos(null);
    setDeviceBIncomingSos(null);
    setDeviceAMessages([]);
    setDeviceBMessages([]);
  };

  // Quick download of the Android zip
  const handleQuickDownloadZip = async () => {
    const zip = new JSZip();
    ANDROID_FILES.forEach((f) => zip.file(f.path, f.content));
    zip.file('android/settings.gradle.kts', `rootProject.name = "VexloraNearPulse"\ninclude(":app")`);
    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'vexlora-nearpulse-android-project.zip';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="min-h-screen bg-[#06080D] text-slate-100 flex flex-col font-sans selection:bg-cyan-500 selection:text-black">
      {/* Top Cyber Navigation Bar */}
      <header className="sticky top-0 z-50 bg-[#080B12]/95 backdrop-blur-md border-b border-slate-800/80 px-4 lg:px-8 py-3">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          
          {/* Logo & Tagline */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-400 via-cyan-500 to-purple-600 p-0.5 shadow-lg shadow-cyan-950">
              <div className="w-full h-full bg-[#080B12] rounded-[10px] flex items-center justify-center">
                <Radio className="w-5 h-5 text-cyan-400 animate-pulse" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs tracking-widest text-cyan-400 font-bold">VEXLORA</span>
                <span className="text-lg font-black tracking-tight text-white">NearPulse</span>
                <span className="text-[10px] font-mono px-2 py-0.5 bg-cyan-950 text-cyan-300 border border-cyan-800 rounded-full">
                  Android Native • Kotlin • Jetpack Compose
                </span>
              </div>
              <div className="text-xs text-slate-400 font-medium">
                Stay Connected. Even Without Internet.
              </div>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center gap-1.5 bg-[#0D121F] p-1 border border-slate-800 rounded-2xl">
            <button
              onClick={() => setViewMode('download')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
                viewMode === 'download'
                  ? 'bg-cyan-400 text-black shadow-md shadow-cyan-950 font-bold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download App</span>
            </button>

            <button
              onClick={() => setViewMode('dual-lab')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
                viewMode === 'dual-lab'
                  ? 'bg-cyan-400 text-black shadow-md shadow-cyan-950'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Dual-Phone Live Lab</span>
            </button>

            <button
              onClick={() => setViewMode('code-explorer')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
                viewMode === 'code-explorer'
                  ? 'bg-cyan-400 text-black shadow-md shadow-cyan-950'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Code className="w-3.5 h-3.5" />
              <span>Android Codebase</span>
            </button>

            <button
              onClick={() => setViewMode('field-guide')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
                viewMode === 'field-guide'
                  ? 'bg-cyan-400 text-black shadow-md shadow-cyan-950'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              <span>2-Phone Test Guide</span>
            </button>

            <button
              onClick={() => setViewMode('web-ble')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
                viewMode === 'web-ble'
                  ? 'bg-cyan-400 text-black shadow-md shadow-cyan-950'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Zap className="w-3.5 h-3.5" />
              <span>Web Bluetooth</span>
            </button>
          </div>

          {/* Action Buttons: Install on Phone & Download Android Studio Zip */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowInstallModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-2 bg-cyan-400 hover:bg-cyan-300 text-black font-extrabold rounded-xl text-xs shadow-lg shadow-cyan-950 transition cursor-pointer"
            >
              <Smartphone className="w-4 h-4 text-black" />
              <span>Install on My Phone</span>
            </button>

            <button
              onClick={handleQuickDownloadZip}
              className="hidden sm:flex items-center gap-1.5 px-3.5 py-2 bg-[#121826] hover:bg-[#1A2234] border border-slate-700 text-slate-200 font-bold rounded-xl text-xs transition"
            >
              <Download className="w-3.5 h-3.5 text-cyan-400" />
              <span>Project ZIP</span>
            </button>
          </div>

        </div>
      </header>

      {/* Main Interactive Workspace */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        
        {/* VIEW 0: OFFICIAL PRODUCT LANDING PAGE & DOWNLOAD HUB */}
        {viewMode === 'download' && (
          <DownloadHub
            onOpenSimulator={() => setViewMode('dual-lab')}
            sharedUrl="https://ais-pre-t74mr6wltamwq4iqbmzaoq-972564470254.asia-southeast1.run.app"
          />
        )}

        {/* VIEW 1: DUAL-PHONE LIVE LAB */}
        {viewMode === 'dual-lab' && (
          <div className="space-y-6">
            
            {/* Quick Scenario Control Banner */}
            <div className="bg-[#0E1422] border border-slate-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3 shadow-xl">
              <div>
                <div className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  Simulated 2-Handset BLE Mesh Protocol Bridge
                </div>
                <div className="text-sm font-bold text-slate-100">
                  Interact with Device A & Device B to test genuine BLE behaviors
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => handleSendFromA("Hi Beta, sending offline test via GATT")}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-cyan-300 border border-cyan-900 rounded-xl text-xs font-medium flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Send A &rarr; B Message</span>
                </button>

                <button
                  onClick={handleStartSosOnA}
                  className="px-3 py-1.5 bg-red-950/60 hover:bg-red-900/60 text-red-300 border border-red-800 rounded-xl text-xs font-medium flex items-center gap-1.5"
                >
                  <ShieldAlert className="w-3.5 h-3.5" />
                  <span>Broadcast SOS on Device A</span>
                </button>

                <button
                  onClick={handleResetLab}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-medium flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Reset Mesh</span>
                </button>
              </div>
            </div>

            {/* Battery Health & RF Signal Distance Control Strip */}
            <div className="bg-[#0A0F1A] border border-cyan-950/60 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-4 text-xs">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-cyan-950 border border-cyan-800 text-cyan-400">
                  <Radio className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-[10px] font-mono uppercase text-cyan-400 font-bold">RF SIMULATION CONTROLS</div>
                  <div className="font-bold text-white flex items-center gap-2">
                    <span>Signal Strength Distance:</span>
                    <span className="text-cyan-300 font-mono">{simulatedDistanceRssi} dBm</span>
                    <span className="text-[10px] text-slate-400">
                      ({simulatedDistanceRssi >= -65 ? 'Near ~1m' : simulatedDistanceRssi >= -80 ? 'Mid ~10m' : 'Boundary ~25m'})
                    </span>
                  </div>
                </div>
              </div>

              {/* Distance / RSSI Buttons */}
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setSimulatedDistanceRssi(-48)}
                  className={`px-2.5 py-1 rounded-lg font-mono text-[11px] border transition ${
                    simulatedDistanceRssi === -48
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Close (-48 dBm)
                </button>
                <button
                  onClick={() => setSimulatedDistanceRssi(-68)}
                  className={`px-2.5 py-1 rounded-lg font-mono text-[11px] border transition ${
                    simulatedDistanceRssi === -68
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Medium (-68 dBm)
                </button>
                <button
                  onClick={() => setSimulatedDistanceRssi(-88)}
                  className={`px-2.5 py-1 rounded-lg font-mono text-[11px] border transition ${
                    simulatedDistanceRssi === -88
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Distant (-88 dBm)
                </button>
              </div>

              {/* Low Power Mode Toggles */}
              <div className="flex items-center gap-2 border-l border-slate-800 pl-4">
                <span className="text-[10px] font-mono text-slate-400">Battery Modes:</span>
                <button
                  onClick={() => {
                    const next = deviceABatteryLevel <= 20 ? 84 : 14;
                    setDeviceABatteryLevel(next);
                  }}
                  className={`px-2.5 py-1 rounded-lg font-mono text-[11px] border transition ${
                    deviceABatteryLevel <= 20
                      ? 'bg-red-950 border-red-700 text-red-200 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-300'
                  }`}
                >
                  Device A: {deviceABatteryLevel <= 20 ? '🪫 14% (Throttled)' : '🔋 84% Normal'}
                </button>
                <button
                  onClick={() => {
                    const next = deviceBBatteryLevel <= 20 ? 76 : 12;
                    setDeviceBBatteryLevel(next);
                  }}
                  className={`px-2.5 py-1 rounded-lg font-mono text-[11px] border transition ${
                    deviceBBatteryLevel <= 20
                      ? 'bg-red-950 border-red-700 text-red-200 font-bold'
                      : 'bg-slate-900 border-slate-800 text-slate-300'
                  }`}
                >
                  Device B: {deviceBBatteryLevel <= 20 ? '🪫 12% (Throttled)' : '🔋 76% Normal'}
                </button>
              </div>
            </div>

            {/* Side-by-Side Dual Phone Mockups */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center justify-center py-2">
              
              {/* PHONE A */}
              <div className="flex flex-col items-center">
                <div className="text-center mb-3">
                  <span className="text-xs font-mono font-bold text-cyan-400 bg-cyan-950/80 px-2.5 py-1 rounded-full border border-cyan-800">
                    DEVICE A (HOST / ADVERTISER)
                  </span>
                  <div className="text-xs text-slate-400 mt-1">Google Pixel 8 Pro • Android 14 (API 34)</div>
                </div>

                <PhoneSimulator
                  deviceName="Pixel 8 Pro (Alpha)"
                  deviceModel="Pixel 8 Pro"
                  pseudonym={deviceAPseudonym}
                  onRotatePseudonym={() => setDeviceAPseudonym('NP-' + Math.floor(1000 + Math.random() * 9000))}
                  isDiscovering={deviceADiscovering}
                  onToggleDiscovering={() => setDeviceADiscovering(!deviceADiscovering)}
                  peers={peersForA}
                  onConnectPeer={(peer) => setDeviceAActivePeer(peer)}
                  activePeer={deviceAActivePeer}
                  onDisconnectPeer={() => setDeviceAActivePeer(null)}
                  messages={deviceAMessages}
                  onSendMessage={handleSendFromA}
                  isSosActive={deviceASosActive}
                  onStartSos={handleStartSosOnA}
                  onStopSos={handleStopSosOnA}
                  incomingSos={deviceAIncomingSos}
                  onAcknowledgeSos={() => setDeviceAIncomingSos((prev) => prev ? { ...prev, isAcknowledged: true } : null)}
                  onDismissSos={() => setDeviceAIncomingSos(null)}
                  safetyCircle={deviceACircle}
                  onCreateCircle={(name) => setDeviceACircle({ code: 'NP-4819', name })}
                  onJoinCircle={(code) => setDeviceACircle({ code, name: 'Circle ' + code })}
                  onLeaveCircle={() => setDeviceACircle(null)}
                  onClearData={() => {
                    setDeviceAMessages([]);
                    setDeviceACircle(null);
                  }}
                  batteryLevel={deviceABatteryLevel}
                  onSetBatteryLevel={(lvl) => setDeviceABatteryLevel(lvl)}
                  powerMode={deviceABatteryLevel <= 20 ? 'BATTERY_SAVER' : deviceAPowerMode}
                  onSetPowerMode={(mode) => setDeviceAPowerMode(mode)}
                />
              </div>

              {/* PHONE B */}
              <div className="flex flex-col items-center">
                <div className="text-center mb-3">
                  <span className="text-xs font-mono font-bold text-purple-400 bg-purple-950/80 px-2.5 py-1 rounded-full border border-purple-800">
                    DEVICE B (PEER / CLIENT)
                  </span>
                  <div className="text-xs text-slate-400 mt-1">Samsung Galaxy S24 • Android 14 (API 34)</div>
                </div>

                <PhoneSimulator
                  deviceName="Galaxy S24 (Beta)"
                  deviceModel="Galaxy S24"
                  pseudonym={deviceBPseudonym}
                  onRotatePseudonym={() => setDeviceBPseudonym('NP-' + Math.floor(1000 + Math.random() * 9000))}
                  isDiscovering={deviceBDiscovering}
                  onToggleDiscovering={() => setDeviceBDiscovering(!deviceBDiscovering)}
                  peers={peersForB}
                  onConnectPeer={(peer) => setDeviceBActivePeer(peer)}
                  activePeer={deviceBActivePeer}
                  onDisconnectPeer={() => setDeviceBActivePeer(null)}
                  messages={deviceBMessages}
                  onSendMessage={handleSendFromB}
                  isSosActive={deviceBSosActive}
                  onStartSos={handleStartSosOnB}
                  onStopSos={handleStopSosOnB}
                  incomingSos={deviceBIncomingSos}
                  onAcknowledgeSos={() => setDeviceBIncomingSos((prev) => prev ? { ...prev, isAcknowledged: true } : null)}
                  onDismissSos={() => setDeviceBIncomingSos(null)}
                  safetyCircle={deviceBCircle}
                  onCreateCircle={(name) => setDeviceBCircle({ code: 'NP-4819', name })}
                  onJoinCircle={(code) => setDeviceBCircle({ code, name: 'Circle ' + code })}
                  onLeaveCircle={() => setDeviceBCircle(null)}
                  onClearData={() => {
                    setDeviceBMessages([]);
                    setDeviceBCircle(null);
                  }}
                  batteryLevel={deviceBBatteryLevel}
                  onSetBatteryLevel={(lvl) => setDeviceBBatteryLevel(lvl)}
                  powerMode={deviceBBatteryLevel <= 20 ? 'BATTERY_SAVER' : deviceBPowerMode}
                  onSetPowerMode={(mode) => setDeviceBPowerMode(mode)}
                />
              </div>

            </div>

            {/* Mesh Architecture Quick Info Callout */}
            <div className="bg-[#0A0E17] border border-slate-800 rounded-2xl p-5 grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div className="space-y-1">
                <div className="text-cyan-400 font-mono font-bold uppercase text-[11px]">128-Bit Service Advertising</div>
                <div className="text-slate-300">
                  Both phones advertise <code className="text-cyan-300">7b14d2e0-252a-4df4-8d4e-e17f5412e601</code>. Arbitrary nearby BLE devices (like fitness trackers or headphones) are filtered out so only NearPulse participants appear.
                </div>
              </div>
              <div className="space-y-1">
                <div className="text-purple-400 font-mono font-bold uppercase text-[11px]">GATT Characteristic Exchange</div>
                <div className="text-slate-300">
                  Text messages are written to <code className="text-purple-300">CHAR_MESSAGE_UUID</code> with MTU 512 negotiation. The receiving device responds immediately with a delivery ACK receipt on <code className="text-purple-300">CHAR_ACK_UUID</code>.
                </div>
              </div>
              <div className="space-y-1">
                <div className="text-red-400 font-mono font-bold uppercase text-[11px]">Emergency Distress Protocol</div>
                <div className="text-slate-300">
                  SOS broadcasts a high-priority beacon in BLE Service Data with a 3-second hold to avoid false triggers. Receiving phones trigger an alarm overlay with mutual rescue acknowledgment.
                </div>
              </div>
            </div>

          </div>
        )}

        {/* VIEW 2: CODE EXPLORER */}
        {viewMode === 'code-explorer' && (
          <div className="space-y-4">
            <CodeExplorer />
          </div>
        )}

        {/* VIEW 3: TWO PHONE FIELD TESTING GUIDE */}
        {viewMode === 'field-guide' && (
          <div className="space-y-4">
            <TwoPhoneGuide />
          </div>
        )}

        {/* VIEW 4: PHYSICAL WEB BLUETOOTH SCANNER */}
        {viewMode === 'web-ble' && (
          <div className="space-y-4">
            <WebBluetoothScanner />
          </div>
        )}

      </main>

      {/* Install on My Phone Modal */}
      <InstallPhoneModal
        isOpen={showInstallModal}
        onClose={() => setShowInstallModal(false)}
        sharedUrl="https://ais-pre-t74mr6wltamwq4iqbmzaoq-972564470254.asia-southeast1.run.app"
      />
    </div>
  );
}
