import React, { useState } from 'react';
import { Bluetooth, Radio, RefreshCw, AlertCircle, CheckCircle, Shield } from 'lucide-react';

interface RealDiscoveredDevice {
  id: string;
  name: string;
  connected: boolean;
}

export const WebBluetoothScanner: React.FC = () => {
  const [isSupported] = useState<boolean>(
    typeof navigator !== 'undefined' && 'bluetooth' in navigator && !!(navigator as any).bluetooth
  );
  const [scanning, setScanning] = useState(false);
  const [realDevices, setRealDevices] = useState<RealDiscoveredDevice[]>([]);
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) => {
    setLog((prev) => [new Date().toLocaleTimeString() + ': ' + msg, ...prev.slice(0, 10)]);
  };

  const handleScanWebBluetooth = async () => {
    const navBluetooth = (navigator as any).bluetooth;
    if (!navBluetooth) {
      addLog('Web Bluetooth API is not supported in this browser environment.');
      return;
    }

    setScanning(true);
    addLog('Requesting Bluetooth device pairing dialog...');

    try {
      // Request device with generic access or accept all devices
      const device = await navBluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: ['generic_access', 'battery_service']
      });

      addLog(`Discovered real physical Bluetooth device: "${device.name || 'Unnamed Peripheral'}" (${device.id})`);

      setRealDevices((prev) => {
        if (!prev.some((d) => d.id === device.id)) {
          return [
            ...prev,
            {
              id: device.id,
              name: device.name || 'Nearby BLE Peripheral',
              connected: false
            }
          ];
        }
        return prev;
      });
    } catch (err: any) {
      if (err.name === 'NotFoundError') {
        addLog('Device selection dialog was cancelled by user.');
      } else {
        addLog(`BLE error: ${err.message || err.toString()}`);
      }
    } finally {
      setScanning(false);
    }
  };

  return (
    <div className="bg-[#0A0E17] border border-slate-800 rounded-2xl p-5 space-y-4 text-slate-200">
      <div className="flex items-center justify-between border-b border-slate-800 pb-3">
        <div className="flex items-center gap-2">
          <div className="p-2 bg-cyan-950 border border-cyan-800 rounded-xl text-cyan-400">
            <Bluetooth className="w-4 h-4" />
          </div>
          <div>
            <div className="text-sm font-bold text-white flex items-center gap-2">
              Physical Web Bluetooth Hardware Scanner
              {isSupported ? (
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-950 text-emerald-300 border border-emerald-800">
                  Web Bluetooth API Ready
                </span>
              ) : (
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-amber-950 text-amber-300 border border-amber-800">
                  Chromium Required
                </span>
              )}
            </div>
            <div className="text-xs text-slate-400">
              Query physical Bluetooth Low Energy peripherals directly from your browser
            </div>
          </div>
        </div>

        <button
          onClick={handleScanWebBluetooth}
          disabled={scanning || !isSupported}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-cyan-400 hover:bg-cyan-300 disabled:bg-slate-800 text-black font-bold rounded-xl text-xs transition"
        >
          {scanning ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Radio className="w-3.5 h-3.5" />}
          {scanning ? 'Scanning...' : 'Pair Real BLE Device'}
        </button>
      </div>

      {/* Discovered Real Devices */}
      {realDevices.length > 0 && (
        <div className="space-y-2">
          <div className="text-xs font-mono uppercase text-slate-400">Detected Physical Hardware:</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {realDevices.map((d) => (
              <div key={d.id} className="p-3 bg-[#0E1422] border border-cyan-900/50 rounded-xl flex items-center justify-between">
                <div>
                  <div className="text-xs font-bold text-cyan-300">{d.name}</div>
                  <div className="text-[10px] text-slate-400 font-mono truncate max-w-[200px]">ID: {d.id}</div>
                </div>
                <span className="text-[10px] text-emerald-400 font-mono">Discovered</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Hardware Log Console */}
      <div className="bg-[#070A0F] border border-slate-800/80 rounded-xl p-3 font-mono text-[11px] text-slate-300 space-y-1">
        <div className="text-[10px] text-slate-500 uppercase flex items-center justify-between">
          <span>HARDWARE EVENT LOG</span>
          <span>Web Bluetooth Status</span>
        </div>
        {log.length === 0 ? (
          <div className="text-slate-600 text-[10px] py-1">
            Click "Pair Real BLE Device" to trigger Chrome's native Bluetooth picker.
          </div>
        ) : (
          log.map((entry, idx) => (
            <div key={idx} className="text-cyan-400/90 text-[10px]">
              {entry}
            </div>
          ))
        )}
      </div>
    </div>
  );
};
