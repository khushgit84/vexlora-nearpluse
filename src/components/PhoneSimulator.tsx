import React, { useState, useEffect, useRef } from 'react';
import {
  Radio,
  Bluetooth,
  ShieldAlert,
  Users,
  Settings,
  Send,
  AlertTriangle,
  ArrowLeft,
  CheckCheck,
  Power,
  RotateCcw,
  QrCode,
  Info,
  CheckCircle,
  HelpCircle,
  ChevronRight,
  WifiOff,
  Signal,
  Battery,
  BatteryCharging,
  BatteryWarning,
  Activity,
  Zap,
  Sliders,
  TrendingDown,
  Gauge
} from 'lucide-react';
import { ScreenTab, SimulatedPeer, ChatMessage, SosAlertState, BlePowerMode } from '../types';

interface PhoneSimulatorProps {
  deviceName: string;
  deviceModel: string;
  pseudonym: string;
  onRotatePseudonym: () => void;
  isDiscovering: boolean;
  onToggleDiscovering: () => void;
  peers: SimulatedPeer[];
  onConnectPeer: (peer: SimulatedPeer) => void;
  activePeer: SimulatedPeer | null;
  onDisconnectPeer: () => void;
  messages: ChatMessage[];
  onSendMessage: (text: string) => void;
  isSosActive: boolean;
  onStartSos: () => void;
  onStopSos: () => void;
  incomingSos: SosAlertState | null;
  onAcknowledgeSos: () => void;
  onDismissSos: () => void;
  safetyCircle: { code: string; name: string } | null;
  onCreateCircle: (name: string) => void;
  onJoinCircle: (code: string) => void;
  onLeaveCircle: () => void;
  onClearData: () => void;
  batteryLevel?: number;
  onSetBatteryLevel?: (level: number) => void;
  powerMode?: BlePowerMode;
  onSetPowerMode?: (mode: BlePowerMode) => void;
}

// Visual Signal Strength (RSSI) Waveform Graph
const SignalStrengthGraph: React.FC<{
  history: number[];
  currentRssi: number;
  peerName: string;
  powerMode: BlePowerMode;
}> = ({ history, currentRssi, peerName, powerMode }) => {
  const points = history.length > 0 ? history : [-65, -62, -58, -60, -56, -58, -62, -59, -58];
  const width = 270;
  const height = 75;
  const minDb = -105;
  const maxDb = -40;

  // Coordinate mapping
  const coords = points.map((val, idx) => {
    const x = (idx / (points.length - 1)) * (width - 20) + 10;
    const clamped = Math.max(minDb, Math.min(maxDb, val));
    const y = height - ((clamped - minDb) / (maxDb - minDb)) * (height - 20) - 10;
    return { x, y, val };
  });

  const pathD = coords.reduce((acc, pt, idx) => {
    return idx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`;
  }, '');

  const areaD = `${pathD} L ${coords[coords.length - 1].x} ${height} L ${coords[0].x} ${height} Z`;

  return (
    <div className="bg-[#0A0F1A] border border-cyan-900/40 rounded-xl p-2.5 space-y-1.5">
      <div className="flex items-center justify-between text-[10px]">
        <div className="flex items-center gap-1.5 font-mono text-cyan-400 font-bold">
          <Activity className="w-3 h-3 text-cyan-400 animate-pulse" />
          <span>SIGNAL STRENGTH (RSSI) GRAPH</span>
        </div>
        <span className="font-mono text-[10px] text-slate-300">
          Target: <strong className="text-cyan-300">{peerName}</strong>
        </span>
      </div>

      {/* SVG Canvas Line Graph */}
      <div className="relative w-full h-[75px] bg-[#060A12] rounded-lg border border-slate-800/80 overflow-hidden">
        {/* Horizontal grid guide lines */}
        <div className="absolute inset-0 flex flex-col justify-between p-1 opacity-20 pointer-events-none">
          <div className="border-b border-cyan-400 w-full"></div>
          <div className="border-b border-cyan-400 w-full"></div>
          <div className="border-b border-cyan-400 w-full"></div>
        </div>

        {/* Labels */}
        <div className="absolute top-1 left-1.5 text-[8px] font-mono text-cyan-500/70">-40 dBm</div>
        <div className="absolute bottom-1 left-1.5 text-[8px] font-mono text-slate-500">-100 dBm</div>
        <div className="absolute top-1 right-2 text-[9px] font-mono font-bold text-cyan-300">
          {currentRssi} dBm
        </div>

        <svg className="w-full h-full overflow-visible" viewBox={`0 0 ${width} ${height}`}>
          <defs>
            <linearGradient id="rssiGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#00F0FF" stopOpacity="0.35" />
              <stop offset="100%" stopColor="#00F0FF" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Area fill */}
          <path d={areaD} fill="url(#rssiGrad)" />

          {/* Line stroke */}
          <path
            d={pathD}
            fill="none"
            stroke="#00F0FF"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Data Points */}
          {coords.map((pt, idx) => (
            <circle
              key={idx}
              cx={pt.x}
              cy={pt.y}
              r={idx === coords.length - 1 ? 3.5 : 2}
              fill={idx === coords.length - 1 ? '#00F0FF' : '#A855F7'}
              className={idx === coords.length - 1 ? 'animate-pulse' : ''}
            />
          ))}
        </svg>
      </div>

      {/* Real-time stats footer */}
      <div className="flex items-center justify-between text-[9px] font-mono text-slate-400">
        <span>RF Noise Floor: -98 dBm</span>
        <span className="text-emerald-400">
          Scan Profile: {powerMode === 'BATTERY_SAVER' ? 'Throttled (12s duty)' : 'Continuous LE'}
        </span>
      </div>
    </div>
  );
};

export const PhoneSimulator: React.FC<PhoneSimulatorProps> = ({
  deviceName,
  deviceModel,
  pseudonym,
  onRotatePseudonym,
  isDiscovering,
  onToggleDiscovering,
  peers,
  onConnectPeer,
  activePeer,
  onDisconnectPeer,
  messages,
  onSendMessage,
  isSosActive,
  onStartSos,
  onStopSos,
  incomingSos,
  onAcknowledgeSos,
  onDismissSos,
  safetyCircle,
  onCreateCircle,
  onJoinCircle,
  onLeaveCircle,
  onClearData,
  batteryLevel = 82,
  onSetBatteryLevel,
  powerMode = 'BALANCED',
  onSetPowerMode
}) => {
  const [currentTab, setCurrentTab] = useState<ScreenTab>('home');
  const [inputText, setInputText] = useState('');
  const [holdTimer, setHoldTimer] = useState<number>(0);
  const [isHoldingSos, setIsHoldingSos] = useState(false);
  const [circleInput, setCircleInput] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [newCircleName, setNewCircleName] = useState('');
  const [showPowerSheet, setShowPowerSheet] = useState(false);
  const chatScrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll chat
  useEffect(() => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages]);

  // Hold-to-activate 3-second SOS timer
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isHoldingSos) {
      interval = setInterval(() => {
        setHoldTimer((prev) => {
          if (prev >= 100) {
            clearInterval(interval);
            setIsHoldingSos(false);
            onStartSos();
            return 0;
          }
          return prev + 4; // ~2.5 to 3 seconds
        });
      }, 100);
    } else {
      setHoldTimer(0);
    }
    return () => clearInterval(interval);
  }, [isHoldingSos, onStartSos]);

  const quickChips = ["I'm safe", "Need help", "Meet me here", "Battery low", "On my way"];

  const handleSend = () => {
    if (inputText.trim()) {
      onSendMessage(inputText.trim());
      setInputText('');
    }
  };

  // Determine battery health status and color
  const getBatteryColor = (level: number) => {
    if (level > 50) return 'text-emerald-400';
    if (level > 20) return 'text-amber-400';
    return 'text-red-500 animate-pulse';
  };

  const isLowPower = batteryLevel <= 20 || powerMode === 'BATTERY_SAVER';

  return (
    <div className="flex flex-col items-center">
      {/* Handset Meta Header */}
      <div className="flex items-center justify-between w-full max-w-[340px] px-2 py-1 mb-2">
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
          <span className="text-xs font-semibold text-slate-200">{deviceName}</span>
          <span className="text-[10px] text-slate-500 font-mono">({deviceModel})</span>
        </div>
        <div className="flex items-center gap-2">
          {/* Quick Battery Mode Trigger */}
          <button
            onClick={() => onSetBatteryLevel && onSetBatteryLevel(batteryLevel > 25 ? 14 : 85)}
            className={`text-[10px] px-2 py-0.5 rounded-full font-mono flex items-center gap-1 cursor-pointer transition ${
              isLowPower
                ? 'bg-red-950 text-red-300 border border-red-800'
                : 'bg-emerald-950 text-emerald-300 border border-emerald-800'
            }`}
            title="Click to toggle low battery simulation (14% vs 85%)"
          >
            <Battery className="w-3 h-3" />
            <span>{batteryLevel}% {isLowPower ? 'Saver' : 'Normal'}</span>
          </button>

          <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono ${
            isDiscovering ? 'bg-cyan-950 text-cyan-300 border border-cyan-800' : 'bg-slate-800 text-slate-400'
          }`}>
            {isDiscovering ? (isLowPower ? 'DUTY-CYCLED' : 'BLE ADV') : 'IDLE'}
          </span>
        </div>
      </div>

      {/* Phone Hardware Mockup Frame */}
      <div className="relative w-[340px] h-[720px] bg-black rounded-[42px] p-3 shadow-2xl shadow-cyan-950/30 border-4 border-slate-800 flex flex-col overflow-hidden select-none">
        {/* Dynamic Island / Speaker Punch */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 w-24 h-5 bg-black rounded-full z-30 flex items-center justify-center border border-slate-900">
          <div className="w-2.5 h-2.5 rounded-full bg-slate-900 border border-slate-800"></div>
        </div>

        {/* Inner Screen Display (Matches Material 3 Cyber Dark Palette) */}
        <div className="relative w-full h-full bg-[#070A0F] rounded-[32px] overflow-hidden flex flex-col text-slate-100 font-sans border border-slate-900/50">
          
          {/* Status Bar */}
          <div className="pt-3 px-6 pb-1 flex justify-between items-center text-[11px] text-slate-400 z-20 font-mono">
            <span>12:45</span>
            <div className="flex items-center gap-2">
              <Bluetooth className={`w-3 h-3 ${isDiscovering ? 'text-cyan-400 animate-pulse' : 'text-slate-500'}`} />
              <WifiOff className="w-3 h-3 text-amber-400/80" />
              {/* Battery Indicator with Health Level */}
              <div
                onClick={() => setShowPowerSheet(!showPowerSheet)}
                className={`flex items-center gap-1 cursor-pointer font-bold ${getBatteryColor(batteryLevel)}`}
                title="Tap to adjust power saving mode"
              >
                <span>{batteryLevel}%</span>
                <Battery className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>

          {/* Incoming SOS Emergency Overlay Banner */}
          {incomingSos && (
            <div className="absolute top-10 left-3 right-3 z-40 bg-red-950/90 border border-red-500 rounded-xl p-2.5 shadow-lg shadow-red-950/80 animate-bounce">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-red-500 shrink-0 animate-pulse" />
                <div className="flex-1 min-w-0">
                  <div className="text-[11px] font-bold text-red-400 uppercase tracking-wider">EMERGENCY SOS DETECTED</div>
                  <div className="text-[11px] text-slate-200 truncate">{incomingSos.broadcasterName} signaling distress nearby!</div>
                </div>
              </div>
              <div className="mt-2 flex gap-1.5 justify-end">
                <button
                  onClick={onAcknowledgeSos}
                  className="px-2.5 py-1 bg-red-600 hover:bg-red-500 text-white rounded text-[11px] font-bold shadow"
                >
                  {incomingSos.isAcknowledged ? 'Acknowledged ✓' : 'Acknowledge Help'}
                </button>
                <button
                  onClick={onDismissSos}
                  className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-[11px]"
                >
                  Dismiss
                </button>
              </div>
            </div>
          )}

          {/* Screen Content Container */}
          <div className="flex-1 overflow-y-auto px-3.5 py-2">
            
            {/* SCREEN 1: HOME */}
            {currentTab === 'home' && (
              <div className="space-y-3">
                {/* Header branding */}
                <div className="flex items-center justify-between pt-1">
                  <div>
                    <div className="text-[10px] font-mono tracking-widest text-cyan-400 font-bold">VEXLORA</div>
                    <div className="text-lg font-black tracking-tight text-white flex items-center gap-1.5">
                      NearPulse
                      <span className="w-1.5 h-1.5 rounded-full bg-cyan-400"></span>
                    </div>
                  </div>
                  <div className="px-2.5 py-1 rounded-full bg-[#121824] border border-slate-800 text-[11px] font-mono flex items-center gap-1.5">
                    <span className={`w-2 h-2 rounded-full ${isDiscovering ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600'}`}></span>
                    <span className="text-slate-200 font-semibold">{pseudonym}</span>
                  </div>
                </div>

                <div className="text-[11px] text-slate-400 leading-tight">
                  Stay Connected. Even Without Internet.
                </div>

                {/* Battery Health & Power Mode Banner */}
                <div className={`p-2.5 rounded-xl border text-xs flex items-center justify-between transition ${
                  isLowPower
                    ? 'bg-amber-950/30 border-amber-800/80 text-amber-200'
                    : 'bg-[#0E1420] border-slate-800 text-slate-300'
                }`}>
                  <div className="flex items-center gap-2">
                    <div className={`p-1.5 rounded-lg ${isLowPower ? 'bg-amber-950 text-amber-400' : 'bg-cyan-950 text-cyan-400'}`}>
                      <BatteryCharging className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-[10px] uppercase font-mono tracking-wider text-slate-400">POWER PROFILE</div>
                      <div className="text-xs font-bold text-white flex items-center gap-1.5">
                        <span>{isLowPower ? 'Battery Saver (Throttled)' : 'Balanced Mode'}</span>
                        <span className={`text-[9px] px-1.5 py-0.2 rounded font-mono ${isLowPower ? 'bg-amber-900/60 text-amber-300' : 'bg-emerald-900/60 text-emerald-300'}`}>
                          {batteryLevel}%
                        </span>
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => onSetBatteryLevel && onSetBatteryLevel(batteryLevel <= 20 ? 85 : 14)}
                    className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-[10px] font-mono rounded-lg text-slate-300 border border-slate-700"
                  >
                    {isLowPower ? 'Restore' : 'Simulate Low'}
                  </button>
                </div>

                {/* Discovery Power Card */}
                <div className="bg-[#0E1420] border border-slate-800/80 rounded-2xl p-3.5 relative overflow-hidden">
                  <div className="flex justify-between items-start mb-2.5">
                    <div>
                      <div className="text-[10px] uppercase font-mono tracking-wider text-slate-400">BLE MESH STATUS</div>
                      <div className={`text-sm font-bold ${isDiscovering ? 'text-cyan-300' : 'text-slate-400'}`}>
                        {isDiscovering ? (isLowPower ? 'DUTY-CYCLED SCAN' : 'DISCOVERY ACTIVE') : 'STANDBY MODE'}
                      </div>
                    </div>
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center border ${
                      isDiscovering
                        ? 'bg-cyan-500/10 border-cyan-400/40 text-cyan-400 animate-pulse'
                        : 'bg-slate-800/40 border-slate-700 text-slate-500'
                    }`}>
                      <Radio className="w-4 h-4" />
                    </div>
                  </div>

                  {/* Metrics grid */}
                  <div className="grid grid-cols-3 gap-2 py-2 border-y border-slate-800/80 text-center font-mono">
                    <div>
                      <div className="text-base font-bold text-cyan-400">{peers.length}</div>
                      <div className="text-[9px] text-slate-400">Nearby Peers</div>
                    </div>
                    <div>
                      <div className="text-base font-bold text-purple-400">
                        {isLowPower ? '12s Slp' : 'Cont.'}
                      </div>
                      <div className="text-[9px] text-slate-400">Scan Duty</div>
                    </div>
                    <div>
                      <div className={`text-base font-bold ${getBatteryColor(batteryLevel)}`}>
                        {batteryLevel}%
                      </div>
                      <div className="text-[9px] text-slate-400">Lifeline</div>
                    </div>
                  </div>

                  {/* Toggle button */}
                  <button
                    onClick={onToggleDiscovering}
                    className={`mt-3 w-full py-2.5 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 ${
                      isDiscovering
                        ? 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700'
                        : 'bg-cyan-400 hover:bg-cyan-300 text-black shadow-lg shadow-cyan-950'
                    }`}
                  >
                    <Power className="w-3.5 h-3.5" />
                    {isDiscovering ? 'Stop Nearby Discovery' : 'Start Nearby Discovery'}
                  </button>
                </div>

                {/* Emergency SOS Quick Button */}
                <button
                  onClick={() => setCurrentTab('sos')}
                  className={`w-full py-2.5 px-3 rounded-2xl flex items-center justify-between border transition ${
                    isSosActive
                      ? 'bg-red-600 border-red-400 text-white animate-pulse'
                      : 'bg-red-950/20 hover:bg-red-950/40 border-red-800/50 text-red-400'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <ShieldAlert className="w-5 h-5 shrink-0" />
                    <div className="text-left">
                      <div className="text-xs font-black tracking-wide">
                        {isSosActive ? 'EMERGENCY SOS IS BROADCASTING' : 'EMERGENCY SOS MODE'}
                      </div>
                      <div className="text-[10px] text-red-300/70">Hold 3s to broadcast distress signal</div>
                    </div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-red-400" />
                </button>

                {/* Quick Navigation 2x2 Grid */}
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => setCurrentTab('radar')}
                    className="p-3 bg-[#0E1420] hover:bg-[#141C2C] border border-slate-800 rounded-xl text-left transition flex flex-col justify-between h-20"
                  >
                    <Radio className="w-4 h-4 text-cyan-400" />
                    <div>
                      <div className="text-xs font-bold text-slate-100">Bluetooth Radar</div>
                      <div className="text-[10px] text-slate-400">Signal Graph & RSSI</div>
                    </div>
                  </button>

                  <button
                    onClick={() => setCurrentTab('chat')}
                    className="p-3 bg-[#0E1420] hover:bg-[#141C2C] border border-slate-800 rounded-xl text-left transition flex flex-col justify-between h-20"
                  >
                    <Send className="w-4 h-4 text-purple-400" />
                    <div>
                      <div className="text-xs font-bold text-slate-100">Nearby Chat</div>
                      <div className="text-[10px] text-slate-400">Offline P2P GATT</div>
                    </div>
                  </button>

                  <button
                    onClick={() => setCurrentTab('circle')}
                    className="p-3 bg-[#0E1420] hover:bg-[#141C2C] border border-slate-800 rounded-xl text-left transition flex flex-col justify-between h-20"
                  >
                    <Users className="w-4 h-4 text-emerald-400" />
                    <div>
                      <div className="text-xs font-bold text-slate-100">Safety Circle</div>
                      <div className="text-[10px] text-slate-400">{safetyCircle ? safetyCircle.code : 'No Circle'}</div>
                    </div>
                  </button>

                  <button
                    onClick={() => setCurrentTab('settings')}
                    className="p-3 bg-[#0E1420] hover:bg-[#141C2C] border border-slate-800 rounded-xl text-left transition flex flex-col justify-between h-20"
                  >
                    <Settings className="w-4 h-4 text-slate-400" />
                    <div>
                      <div className="text-xs font-bold text-slate-100">Hardware & Info</div>
                      <div className="text-[10px] text-slate-400">Power & Diagnostics</div>
                    </div>
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 2: BLUETOOTH RADAR & SIGNAL STRENGTH GRAPH */}
            {currentTab === 'radar' && (
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <button onClick={() => setCurrentTab('home')} className="p-1 text-slate-400 hover:text-white">
                      <ArrowLeft className="w-4 h-4" />
                    </button>
                    <div>
                      <div className="text-sm font-bold text-white">BLUETOOTH RADAR</div>
                      <div className="text-[10px] text-cyan-400 font-mono">128-bit Service Filter</div>
                    </div>
                  </div>
                  <button
                    onClick={onToggleDiscovering}
                    className="text-[10px] font-mono px-2 py-1 bg-slate-800 rounded-lg text-slate-300 hover:bg-slate-700"
                  >
                    {isDiscovering ? 'PAUSE' : 'SCAN'}
                  </button>
                </div>

                {/* Radar Visual Canvas Animation */}
                <div className="relative w-full h-36 bg-[#0E1420] border border-slate-800 rounded-2xl flex items-center justify-center overflow-hidden">
                  {/* Concentric rings */}
                  <div className="absolute w-32 h-32 rounded-full border border-slate-800/80"></div>
                  <div className="absolute w-20 h-20 rounded-full border border-slate-800"></div>
                  <div className="absolute w-10 h-10 rounded-full border border-slate-700/80"></div>
                  
                  {/* Crosshairs */}
                  <div className="absolute w-full h-[1px] bg-slate-800/60"></div>
                  <div className="absolute h-full w-[1px] bg-slate-800/60"></div>

                  {/* Sweep line */}
                  {isDiscovering && (
                    <div className="absolute w-16 h-16 top-1/2 left-1/2 origin-top-left -translate-x-[0.5px] -translate-y-[0.5px]">
                      <div className="w-full h-full border-l-2 border-cyan-400/80 [background:conic-gradient(from_0deg_at_0%_0%,rgba(0,240,255,0.25)_0deg,transparent_60deg)] animate-[spin_3s_linear_infinite]"></div>
                    </div>
                  )}

                  {/* Center Host Dot */}
                  <div className="absolute w-2.5 h-2.5 rounded-full bg-cyan-400 shadow-md shadow-cyan-400"></div>

                  {/* Plotted Discovered Peers */}
                  {peers.map((peer, idx) => {
                    const isLeft = idx % 2 === 0;
                    const offset = 24 + (idx * 14);
                    return (
                      <div
                        key={peer.id}
                        onClick={() => {
                          onConnectPeer(peer);
                          setCurrentTab('chat');
                        }}
                        style={{
                          transform: `translate(${isLeft ? -offset : offset}px, ${isLeft ? -offset / 1.5 : offset / 1.5}px)`
                        }}
                        className={`absolute cursor-pointer group`}
                        title={`Tap to connect to ${peer.pseudonym}`}
                      >
                        <div className={`w-3.5 h-3.5 rounded-full flex items-center justify-center ${
                          peer.isSos ? 'bg-red-500 animate-ping' : 'bg-cyan-400'
                        }`}></div>
                        <div className="absolute -top-4 -left-6 bg-black/80 px-1.5 py-0.5 rounded text-[8px] font-mono text-cyan-300 border border-slate-800 whitespace-nowrap">
                          {peer.pseudonym}
                        </div>
                      </div>
                    );
                  })}

                  <div className="absolute bottom-1.5 text-[8px] font-mono text-slate-500">
                    {isDiscovering ? (isLowPower ? 'DUTY-CYCLE 10s PULSE' : 'LE CONTINUOUS SCAN') : 'SCANNER IDLE'}
                  </div>
                </div>

                {/* REAL-TIME SIGNAL STRENGTH (RSSI) GRAPH */}
                {peers.length > 0 && (
                  <SignalStrengthGraph
                    history={peers[0].rssiHistory}
                    currentRssi={peers[0].rssi}
                    peerName={peers[0].pseudonym}
                    powerMode={powerMode}
                  />
                )}

                {/* RSSI & Low-Power Behavior Notice */}
                <div className="p-2 bg-slate-900/60 border border-slate-800 rounded-lg text-[9px] text-slate-400 flex items-start gap-1.5">
                  <Info className="w-3.5 h-3.5 text-cyan-400 shrink-0 mt-0.5" />
                  <span>
                    {isLowPower
                      ? 'Low-power mode: Scanner sleeps 10s between 1.5s bursts. Reduces radio draw by ~75% to prolong lifeline.'
                      : 'RSSI indicates received RF attenuation (-40 to -100 dBm). Natural multipath causes minor ±3 dBm jitter.'}
                  </span>
                </div>

                {/* Discovered Peers List with Battery Indicator */}
                <div className="text-[10px] uppercase font-mono tracking-wider text-slate-400">
                  DISCOVERED PEERS ({peers.length})
                </div>

                {peers.length === 0 ? (
                  <div className="p-3 text-center text-slate-500 bg-[#0E1420] border border-slate-800 rounded-xl">
                    <Radio className="w-5 h-5 mx-auto mb-1 text-slate-600" />
                    <div className="text-xs text-slate-400">No NearPulse peers detected</div>
                    <div className="text-[9px]">Turn on discovery on other device or press Scan</div>
                  </div>
                ) : (
                  <div className="space-y-1.5 max-h-48 overflow-y-auto">
                    {peers.map((peer) => (
                      <div
                        key={peer.id}
                        className={`p-2.5 rounded-xl border flex items-center justify-between ${
                          peer.isSos
                            ? 'bg-red-950/20 border-red-800/80'
                            : 'bg-[#0E1420] border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <div className={`w-2 h-2 rounded-full ${peer.isSos ? 'bg-red-500 animate-pulse' : 'bg-cyan-400'}`}></div>
                          <div>
                            <div className="text-xs font-bold text-slate-100 flex items-center gap-1.5">
                              {peer.pseudonym}
                              {peer.isSos && (
                                <span className="bg-red-600 text-white text-[8px] font-mono px-1 rounded">SOS</span>
                              )}
                              {/* Peer Battery Health Badge */}
                              <span className={`text-[9px] font-mono px-1.5 py-0.2 rounded-full border ${
                                peer.batteryLevel <= 20
                                  ? 'bg-red-950/80 text-red-300 border-red-800'
                                  : 'bg-emerald-950/80 text-emerald-300 border-emerald-800'
                              }`}>
                                🔋 {peer.batteryLevel}% {peer.powerMode === 'BATTERY_SAVER' ? '(Saver)' : ''}
                              </span>
                            </div>
                            <div className="text-[10px] text-slate-400 font-mono">
                              {peer.signalStrength} ({peer.rssi} dBm) • {peer.lastSeen}
                            </div>
                          </div>
                        </div>

                        <button
                          onClick={() => {
                            onConnectPeer(peer);
                            setCurrentTab('chat');
                          }}
                          className="px-2.5 py-1 bg-cyan-950 border border-cyan-800 text-cyan-300 rounded-lg text-xs font-semibold hover:bg-cyan-900"
                        >
                          Connect
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* SCREEN 3: NEARBY CHAT */}
            {currentTab === 'chat' && (
              <div className="flex flex-col h-full space-y-2">
                {/* Chat Top Header with Battery Info */}
                <div className="flex items-center justify-between pb-1 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <button onClick={() => setCurrentTab('home')} className="p-1 text-slate-400 hover:text-white">
                      <ArrowLeft className="w-4 h-4" />
                    </button>
                    <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></div>
                    <div>
                      <div className="text-xs font-bold text-slate-100 flex items-center gap-1.5">
                        <span>{activePeer ? activePeer.pseudonym : 'Select a Peer'}</span>
                        {activePeer && (
                          <span className="text-[9px] font-mono text-slate-400">
                            (🔋 {activePeer.batteryLevel}%)
                          </span>
                        )}
                      </div>
                      <div className="text-[9px] font-mono text-emerald-400">
                        {activePeer ? 'GATT LE Connected (MTU 512)' : 'Tap Radar to connect'}
                      </div>
                    </div>
                  </div>

                  {activePeer && (
                    <button
                      onClick={onDisconnectPeer}
                      className="text-[10px] text-slate-400 hover:text-red-400 px-2 py-0.5 bg-slate-900 rounded border border-slate-800"
                    >
                      Disconnect
                    </button>
                  )}
                </div>

                {/* Message Stream */}
                <div ref={chatScrollRef} className="flex-1 overflow-y-auto space-y-2 pr-1 min-h-[220px]">
                  {messages.length === 0 ? (
                    <div className="h-40 flex flex-col items-center justify-center text-slate-500 text-center">
                      <Send className="w-6 h-6 mb-1 text-slate-600" />
                      <div className="text-xs text-slate-400">No offline GATT packets exchanged</div>
                      <div className="text-[10px]">Use quick safety chips below to send</div>
                    </div>
                  ) : (
                    messages.map((msg) => (
                      <div
                        key={msg.id}
                        className={`flex flex-col ${msg.isOutgoing ? 'items-end' : 'items-start'}`}
                      >
                        <div
                          className={`max-w-[80%] rounded-2xl px-3 py-1.5 text-xs ${
                            msg.isOutgoing
                              ? 'bg-cyan-950/80 border border-cyan-800/80 text-cyan-100 rounded-br-sm'
                              : 'bg-slate-800/90 border border-slate-700 text-slate-100 rounded-bl-sm'
                          }`}
                        >
                          {!msg.isOutgoing && (
                            <div className="text-[9px] font-mono text-purple-400 mb-0.5 font-bold">
                              {msg.senderPseudonym}
                            </div>
                          )}
                          <div>{msg.content}</div>
                          <div className="flex items-center justify-end gap-1 mt-0.5 text-[8px] font-mono text-slate-400">
                            <span>{msg.timestamp}</span>
                            {msg.isOutgoing && (
                              <span>
                                {msg.status === 'DELIVERED' ? (
                                  <CheckCheck className="w-3 h-3 text-emerald-400 inline" />
                                ) : (
                                  '⏳'
                                )}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                {/* Quick Chips */}
                <div className="flex gap-1.5 overflow-x-auto py-1 no-scrollbar">
                  {quickChips.map((chip) => (
                    <button
                      key={chip}
                      onClick={() => onSendMessage(chip)}
                      className="px-2.5 py-1 bg-[#121824] hover:bg-[#1A2334] border border-slate-800 rounded-full text-[10px] text-cyan-300 whitespace-nowrap"
                    >
                      {chip}
                    </button>
                  ))}
                </div>

                {/* Input bar */}
                <div className="flex items-center gap-1.5 pt-1">
                  <input
                    type="text"
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                    placeholder="Type offline message..."
                    className="flex-1 bg-[#0E1420] border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                  />
                  <button
                    onClick={handleSend}
                    disabled={!inputText.trim()}
                    className="p-2 bg-cyan-400 disabled:bg-slate-800 text-black disabled:text-slate-600 rounded-xl"
                  >
                    <Send className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 4: EMERGENCY SOS MODE */}
            {currentTab === 'sos' && (
              <div className="space-y-3 flex flex-col items-center text-center">
                <div className="w-full flex items-center justify-between">
                  <button onClick={() => setCurrentTab('home')} className="p-1 text-slate-400 hover:text-white">
                    <ArrowLeft className="w-4 h-4" />
                  </button>
                  <div className="text-xs font-black tracking-wider text-red-500 uppercase">EMERGENCY SOS CONSOLE</div>
                  <div className="w-4"></div>
                </div>

                {/* Range limitation disclaimer */}
                <div className="w-full p-2.5 bg-red-950/20 border border-red-900/50 rounded-xl text-left text-[10px] text-slate-300 flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-red-400">BLE RANGE LIMITATION: </span>
                    Broadcasts directly over Bluetooth LE (~10–30m line-of-sight). Not a replacement for official 911 / 112 emergency services.
                  </div>
                </div>

                {/* Big Hold-to-Activate SOS Button */}
                <div className="py-4 flex flex-col items-center">
                  <div className={`relative w-44 h-44 rounded-full flex flex-col items-center justify-center transition-all ${
                    isSosActive
                      ? 'bg-red-600 border-4 border-red-400 shadow-2xl shadow-red-600/50 animate-pulse'
                      : 'bg-[#0E1420] border-2 border-slate-800 hover:border-red-600/50'
                  }`}>
                    {/* Ring progress for hold */}
                    {isHoldingSos && (
                      <div
                        className="absolute inset-0 rounded-full border-4 border-red-500 animate-spin"
                        style={{ borderTopColor: 'transparent' }}
                      ></div>
                    )}

                    <ShieldAlert className={`w-12 h-12 mb-1 ${isSosActive ? 'text-white' : 'text-red-500'}`} />
                    <div className="text-sm font-black tracking-wider text-white">
                      {isSosActive ? 'SOS ACTIVE' : 'EMERGENCY'}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">
                      {isSosActive ? 'BROADCASTING...' : isHoldingSos ? `${holdTimer}%` : 'HOLD 3 SECONDS'}
                    </div>
                  </div>

                  {!isSosActive ? (
                    <button
                      onMouseDown={() => setIsHoldingSos(true)}
                      onMouseUp={() => setIsHoldingSos(false)}
                      onTouchStart={() => setIsHoldingSos(true)}
                      onTouchEnd={() => setIsHoldingSos(false)}
                      className="mt-4 px-6 py-2.5 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-red-950"
                    >
                      {isHoldingSos ? 'Keep Holding...' : 'Press & Hold to Activate'}
                    </button>
                  ) : (
                    <button
                      onClick={onStopSos}
                      className="mt-4 px-6 py-2.5 bg-slate-800 hover:bg-slate-700 text-red-400 border border-red-800 rounded-xl text-xs font-bold"
                    >
                      Cancel Emergency SOS
                    </button>
                  )}
                </div>

                <div className="text-[10px] text-slate-500 max-w-[260px]">
                  Requires nearby participating NearPulse handsets within Bluetooth range to receive and acknowledge.
                </div>
              </div>
            )}

            {/* SCREEN 5: SAFETY CIRCLE */}
            {currentTab === 'circle' && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <button onClick={() => setCurrentTab('home')} className="p-1 text-slate-400 hover:text-white">
                      <ArrowLeft className="w-4 h-4" />
                    </button>
                    <div>
                      <div className="text-sm font-bold text-white">SAFETY CIRCLE</div>
                      <div className="text-[10px] text-emerald-400 font-mono">Offline Group Roster</div>
                    </div>
                  </div>
                </div>

                {/* Safety Circle Card */}
                {!safetyCircle ? (
                  <div className="p-4 bg-[#0E1420] border border-slate-800 rounded-2xl text-center space-y-3">
                    <Users className="w-8 h-8 text-emerald-400 mx-auto" />
                    <div className="text-xs font-bold text-slate-200">No Circle Configured</div>
                    <div className="text-[10px] text-slate-400">
                      Create or join an offline safety group with family or hiking expedition members.
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => setShowCreateModal(true)}
                        className="flex-1 py-2 bg-emerald-500 hover:bg-emerald-400 text-black font-bold text-xs rounded-xl"
                      >
                        Create Circle
                      </button>
                      <button
                        onClick={() => setShowJoinModal(true)}
                        className="flex-1 py-2 bg-slate-800 hover:bg-slate-700 text-emerald-300 font-bold text-xs rounded-xl border border-emerald-900"
                      >
                        Join Code
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <div className="p-3 bg-[#0E1420] border border-emerald-800/60 rounded-2xl flex justify-between items-center">
                      <div>
                        <div className="text-[10px] font-mono text-emerald-400 uppercase">ACTIVE CIRCLE</div>
                        <div className="text-sm font-bold text-white">{safetyCircle.name}</div>
                        <div className="text-xs font-mono font-bold text-cyan-300 mt-0.5">CODE: {safetyCircle.code}</div>
                      </div>
                      <button
                        onClick={onLeaveCircle}
                        className="text-[10px] text-red-400 hover:text-red-300 px-2 py-1 bg-red-950/20 border border-red-900 rounded"
                      >
                        Leave
                      </button>
                    </div>

                    <div className="text-[10px] uppercase font-mono tracking-wider text-slate-400">
                      CIRCLE MEMBERS (DIRECT BLE)
                    </div>

                    <div className="space-y-1.5">
                      <div className="p-2.5 bg-[#0E1420] border border-slate-800 rounded-xl flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                          <span className="text-xs font-semibold text-slate-200">{pseudonym} (You)</span>
                        </div>
                        <span className="text-[9px] font-mono text-cyan-400">HOST</span>
                      </div>

                      {peers.map((peer) => (
                        <div
                          key={peer.id}
                          className="p-2.5 bg-[#0E1420] border border-slate-800 rounded-xl flex items-center justify-between"
                        >
                          <div className="flex items-center gap-2">
                            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                            <div>
                              <div className="text-xs font-semibold text-slate-200">{peer.pseudonym}</div>
                              <div className="text-[9px] text-slate-400 font-mono">
                                {peer.signalStrength} • 🔋 {peer.batteryLevel}%
                              </div>
                            </div>
                          </div>
                          <span className="text-[9px] text-emerald-400 font-mono">NEARBY</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* SCREEN 6: SETTINGS & HARDWARE DIAGNOSTICS */}
            {currentTab === 'settings' && (
              <div className="space-y-3 text-xs">
                <div className="flex items-center gap-2 pb-1 border-b border-slate-800">
                  <button onClick={() => setCurrentTab('home')} className="p-1 text-slate-400 hover:text-white">
                    <ArrowLeft className="w-4 h-4" />
                  </button>
                  <div className="font-bold text-white">SETTINGS & DIAGNOSTICS</div>
                </div>

                {/* Battery Health & Power Saver Configuration */}
                <div className="bg-[#0E1420] border border-slate-800 rounded-xl p-3 space-y-2">
                  <div className="text-[10px] font-mono text-cyan-400 uppercase flex items-center justify-between">
                    <span>BATTERY HEALTH & POWER MODE</span>
                    <span className={getBatteryColor(batteryLevel)}>{batteryLevel}% Remaining</span>
                  </div>
                  <div className="text-[11px] text-slate-300">
                    Current Mode:{' '}
                    <strong className="text-cyan-300">
                      {powerMode === 'BATTERY_SAVER'
                        ? 'Emergency Saver (12s Duty Cycle)'
                        : powerMode === 'LOW_POWER'
                        ? 'Low Power (5s Duty Cycle)'
                        : 'Balanced (Continuous LE)'}
                    </strong>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pt-1">
                    <button
                      onClick={() => onSetBatteryLevel && onSetBatteryLevel(15)}
                      className="py-1.5 px-2 bg-red-950/40 hover:bg-red-900/40 border border-red-800/80 rounded-lg text-[10px] text-red-300 font-mono"
                    >
                      Set Low (15%)
                    </button>
                    <button
                      onClick={() => onSetBatteryLevel && onSetBatteryLevel(85)}
                      className="py-1.5 px-2 bg-emerald-950/40 hover:bg-emerald-900/40 border border-emerald-800/80 rounded-lg text-[10px] text-emerald-300 font-mono"
                    >
                      Set Optimal (85%)
                    </button>
                  </div>
                </div>

                {/* BLE Hardware Diagnostics */}
                <div className="bg-[#0E1420] border border-slate-800 rounded-xl p-3 space-y-2">
                  <div className="text-[10px] font-mono text-cyan-400 uppercase">BLUETOOTH HARDWARE STACK</div>
                  <div className="flex justify-between text-slate-300">
                    <span>BLE Adapter:</span>
                    <span className="text-emerald-400 font-mono font-bold">Detected (LE 5.0)</span>
                  </div>
                  <div className="flex justify-between text-slate-300">
                    <span>Multiple Advertisement:</span>
                    <span className="text-emerald-400 font-mono font-bold">Supported</span>
                  </div>
                  <div className="flex justify-between text-slate-300">
                    <span>Target UUID:</span>
                    <span className="text-slate-400 font-mono text-[10px]">7b14d2e0-252a...</span>
                  </div>
                </div>

                {/* Privacy Pseudonym */}
                <div className="bg-[#0E1420] border border-slate-800 rounded-xl p-3 space-y-2">
                  <div className="text-[10px] font-mono text-cyan-400 uppercase">EPHEMERAL IDENTITY</div>
                  <div className="flex justify-between items-center">
                    <div>
                      <div className="text-slate-400 text-[10px]">Current Pseudonym</div>
                      <div className="text-sm font-mono font-bold text-white">{pseudonym}</div>
                    </div>
                    <button
                      onClick={onRotatePseudonym}
                      className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-cyan-400 rounded-lg text-xs font-mono"
                    >
                      Rotate
                    </button>
                  </div>
                </div>

                {/* Android 12 Permissions */}
                <div className="bg-[#0E1420] border border-slate-800 rounded-xl p-3 space-y-1.5 font-mono text-[11px]">
                  <div className="text-[10px] text-cyan-400 uppercase">ANDROID 12+ PERMISSIONS</div>
                  <div className="text-emerald-400">✓ BLUETOOTH_SCAN (neverForLocation)</div>
                  <div className="text-emerald-400">✓ BLUETOOTH_ADVERTISE</div>
                  <div className="text-emerald-400">✓ BLUETOOTH_CONNECT</div>
                  <div className="text-emerald-400">✓ POST_NOTIFICATIONS</div>
                </div>

                {/* Clear Local Data */}
                <button
                  onClick={onClearData}
                  className="w-full py-2 bg-red-950/20 hover:bg-red-950/40 border border-red-900/60 text-red-400 font-bold rounded-xl text-xs"
                >
                  Clear Local Cache & Messages
                </button>
              </div>
            )}

          </div>

          {/* Android Navigation Bar / Bottom Bar */}
          <div className="h-14 bg-[#070A0F] border-t border-slate-800/80 px-2 flex justify-around items-center z-20">
            <button
              onClick={() => setCurrentTab('home')}
              className={`flex flex-col items-center gap-0.5 ${currentTab === 'home' ? 'text-cyan-400' : 'text-slate-500'}`}
            >
              <Radio className="w-4 h-4" />
              <span className="text-[9px] font-medium">Home</span>
            </button>

            <button
              onClick={() => setCurrentTab('radar')}
              className={`flex flex-col items-center gap-0.5 ${currentTab === 'radar' ? 'text-cyan-400' : 'text-slate-500'}`}
            >
              <Radio className="w-4 h-4" />
              <span className="text-[9px] font-medium">Radar</span>
            </button>

            <button
              onClick={() => setCurrentTab('chat')}
              className={`flex flex-col items-center gap-0.5 ${currentTab === 'chat' ? 'text-cyan-400' : 'text-slate-500'}`}
            >
              <Send className="w-4 h-4" />
              <span className="text-[9px] font-medium">Chat</span>
            </button>

            <button
              onClick={() => setCurrentTab('sos')}
              className={`flex flex-col items-center gap-0.5 ${currentTab === 'sos' ? 'text-red-400' : 'text-slate-500'}`}
            >
              <ShieldAlert className="w-4 h-4" />
              <span className="text-[9px] font-medium">SOS</span>
            </button>

            <button
              onClick={() => setCurrentTab('circle')}
              className={`flex flex-col items-center gap-0.5 ${currentTab === 'circle' ? 'text-emerald-400' : 'text-slate-500'}`}
            >
              <Users className="w-4 h-4" />
              <span className="text-[9px] font-medium">Circle</span>
            </button>
          </div>

        </div>
      </div>

      {/* Modal for Create Circle */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#0E1420] border border-slate-800 rounded-2xl p-5 max-w-xs w-full text-white space-y-3">
            <div className="text-sm font-bold">Create Local Safety Circle</div>
            <div className="text-xs text-slate-400">Generate an offline mesh circle code:</div>
            <input
              type="text"
              value={newCircleName}
              onChange={(e) => setNewCircleName(e.target.value)}
              placeholder="Circle Name (e.g. Squad Alpha)"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white"
            />
            <div className="flex gap-2">
              <button
                onClick={() => {
                  onCreateCircle(newCircleName || 'Squad Alpha');
                  setShowCreateModal(false);
                  setNewCircleName('');
                }}
                className="flex-1 py-2 bg-emerald-500 hover:bg-emerald-400 text-black font-bold text-xs rounded-xl"
              >
                Create
              </button>
              <button
                onClick={() => setShowCreateModal(false)}
                className="flex-1 py-2 bg-slate-800 text-slate-300 text-xs rounded-xl"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal for Join Circle */}
      {showJoinModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#0E1420] border border-slate-800 rounded-2xl p-5 max-w-xs w-full text-white space-y-3">
            <div className="text-sm font-bold">Join Safety Circle</div>
            <div className="text-xs text-slate-400">Enter the 6-character code from the host:</div>
            <input
              type="text"
              value={circleInput}
              onChange={(e) => setCircleInput(e.target.value.toUpperCase())}
              placeholder="e.g. NP-4921"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white font-mono uppercase"
            />
            <div className="flex gap-2">
              <button
                onClick={() => {
                  if (circleInput.trim()) {
                    onJoinCircle(circleInput.trim());
                    setShowJoinModal(false);
                    setCircleInput('');
                  }
                }}
                className="flex-1 py-2 bg-emerald-500 hover:bg-emerald-400 text-black font-bold text-xs rounded-xl"
              >
                Join
              </button>
              <button
                onClick={() => setShowJoinModal(false)}
                className="flex-1 py-2 bg-slate-800 text-slate-300 text-xs rounded-xl"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
