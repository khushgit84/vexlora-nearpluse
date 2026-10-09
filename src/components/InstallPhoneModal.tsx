import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import {
  Smartphone,
  QrCode,
  Download,
  Terminal,
  CheckCircle,
  Copy,
  ExternalLink,
  X,
  Play,
  Usb,
  ShieldCheck,
  Check,
  AlertTriangle
} from 'lucide-react';
import JSZip from 'jszip';
import { ANDROID_FILES } from '../data/androidFiles';

interface InstallPhoneModalProps {
  isOpen: boolean;
  onClose: () => void;
  sharedUrl: string;
}

export const InstallPhoneModal: React.FC<InstallPhoneModalProps> = ({
  isOpen,
  onClose,
  sharedUrl
}) => {
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('');
  const [copiedUrl, setCopiedUrl] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [activeTab, setActiveTab] = useState<'scan-phone' | 'build-apk' | 'adb'>('scan-phone');

  useEffect(() => {
    if (sharedUrl) {
      QRCode.toDataURL(sharedUrl, {
        width: 240,
        margin: 2,
        color: {
          dark: '#00F0FF',
          light: '#070A0F'
        }
      })
        .then((url) => setQrCodeUrl(url))
        .catch((err) => console.error('QR code generation failed:', err));
    }
  }, [sharedUrl]);

  if (!isOpen) return null;

  const handleCopyUrl = () => {
    navigator.clipboard.writeText(sharedUrl);
    setCopiedUrl(true);
    setTimeout(() => setCopiedUrl(false), 2000);
  };

  const handleDownloadZip = async () => {
    setIsExporting(true);
    try {
      const zip = new JSZip();
      ANDROID_FILES.forEach((f) => zip.file(f.path, f.content));
      zip.file(
        'android/settings.gradle.kts',
        `rootProject.name = "VexloraNearPulse"\ninclude(":app")`
      );
      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'vexlora-nearpulse-android-project.zip';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Error generating zip:', e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-[#0C101A] border border-cyan-800/80 rounded-3xl max-w-2xl w-full text-slate-100 shadow-2xl shadow-cyan-950/80 overflow-hidden relative my-8">
        
        {/* Top Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-[#0E1422]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-950 border border-cyan-700 flex items-center justify-center text-cyan-400">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-white flex items-center gap-2">
                Install VEXLORA NearPulse on Your Phone
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-300 border border-cyan-800">
                  Android Native
                </span>
              </h3>
              <p className="text-xs text-slate-400">
                Choose how you want to run and test NearPulse on your physical handset
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-slate-800/80 bg-[#080B12] px-5 pt-2 gap-2">
          <button
            onClick={() => setActiveTab('scan-phone')}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'scan-phone'
                ? 'border-cyan-400 text-cyan-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>1. Scan QR (Instant on Phone)</span>
          </button>

          <button
            onClick={() => setActiveTab('build-apk')}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'build-apk'
                ? 'border-cyan-400 text-cyan-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Play className="w-3.5 h-3.5" />
            <span>2. Android Studio (1-Click Run)</span>
          </button>

          <button
            onClick={() => setActiveTab('adb')}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 flex items-center gap-1.5 transition ${
              activeTab === 'adb'
                ? 'border-cyan-400 text-cyan-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Terminal className="w-3.5 h-3.5" />
            <span>3. Command Line & APK</span>
          </button>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          
          {/* TAB 1: SCAN QR ON PHONE */}
          {activeTab === 'scan-phone' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                {/* QR Code Container */}
                <div className="flex flex-col items-center justify-center p-4 bg-[#070A0F] border border-cyan-900/60 rounded-2xl">
                  {qrCodeUrl ? (
                    <img
                      src={qrCodeUrl}
                      alt="Scan to open on phone"
                      className="w-48 h-48 rounded-xl shadow-lg shadow-cyan-950"
                    />
                  ) : (
                    <div className="w-48 h-48 bg-slate-900 animate-pulse rounded-xl flex items-center justify-center text-xs text-slate-500">
                      Generating QR...
                    </div>
                  )}
                  <div className="mt-2 text-center">
                    <span className="text-[11px] font-mono text-cyan-400 font-bold">
                      Point Phone Camera Here
                    </span>
                    <div className="text-[10px] text-slate-400">Works on Chrome & Samsung Internet</div>
                  </div>
                </div>

                {/* Instructions */}
                <div className="space-y-3 text-xs">
                  <div className="font-bold text-white text-sm">
                    Open Directly in Your Phone's Browser:
                  </div>

                  <div className="space-y-2 text-slate-300">
                    <div className="flex items-start gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 flex items-center justify-center text-cyan-400 text-[10px] font-bold shrink-0">1</span>
                      <span>Open your Android phone camera or QR scanner.</span>
                    </div>

                    <div className="flex items-start gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 flex items-center justify-center text-cyan-400 text-[10px] font-bold shrink-0">2</span>
                      <span>Tap the link that appears on your phone screen.</span>
                    </div>

                    <div className="flex items-start gap-2">
                      <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 flex items-center justify-center text-cyan-400 text-[10px] font-bold shrink-0">3</span>
                      <span>
                        In Chrome on your phone, you can test the full dual-mesh interface and use <strong>Web Bluetooth</strong> to scan physical BLE devices!
                      </span>
                    </div>
                  </div>

                  {/* Copy Link Input */}
                  <div className="pt-2">
                    <div className="text-[10px] font-mono text-slate-400 mb-1">Direct URL Link:</div>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        readOnly
                        value={sharedUrl}
                        className="flex-1 bg-[#070A0F] border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-cyan-300 font-mono truncate"
                      />
                      <button
                        onClick={handleCopyUrl}
                        className="px-3 py-1.5 bg-cyan-500 hover:bg-cyan-400 text-black font-bold rounded-xl text-xs flex items-center gap-1 transition"
                      >
                        {copiedUrl ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                        <span>{copiedUrl ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: ANDROID STUDIO 1-CLICK RUN */}
          {activeTab === 'build-apk' && (
            <div className="space-y-4 text-xs">
              <div className="p-4 bg-[#070A0F] border border-slate-800 rounded-2xl space-y-3">
                <div className="flex items-center justify-between">
                  <div className="font-bold text-sm text-white flex items-center gap-2">
                    <Play className="w-4 h-4 text-emerald-400" />
                    <span>Run Directly from Android Studio</span>
                  </div>
                  <button
                    onClick={handleDownloadZip}
                    disabled={isExporting}
                    className="px-3.5 py-1.5 bg-cyan-400 hover:bg-cyan-300 disabled:bg-slate-800 text-black font-bold rounded-xl text-xs flex items-center gap-1.5 transition shadow"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>{isExporting ? 'Packaging...' : 'Download Project ZIP'}</span>
                  </button>
                </div>

                <div className="space-y-2.5 text-slate-300">
                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-cyan-400 font-bold shrink-0">1</span>
                    <div>
                      <strong>Download & Unzip:</strong> Click the button above to download <code className="text-cyan-300 bg-slate-900 px-1 py-0.5 rounded">vexlora-nearpulse-android-project.zip</code> and extract it on your computer.
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-cyan-400 font-bold shrink-0">2</span>
                    <div>
                      <strong>Open in Android Studio:</strong> Select <strong>File &rarr; Open</strong> &rarr; choose the extracted <code className="text-cyan-300 bg-slate-900 px-1 py-0.5 rounded">android/</code> folder. Android Studio will automatically sync Gradle.
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-cyan-400 font-bold shrink-0">3</span>
                    <div>
                      <strong>Connect Your Phone:</strong> Connect your physical phone via USB cable (with USB debugging enabled) or over Wi-Fi pairing.
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-cyan-400 font-bold shrink-0">4</span>
                    <div>
                      <strong>Click Run (▶):</strong> Click the green <strong>Run 'app'</strong> button. Android Studio will compile the APK, install it onto your phone, and launch VEXLORA NearPulse instantly!
                    </div>
                  </div>
                </div>
              </div>

              {/* Permission note */}
              <div className="p-3 bg-emerald-950/30 border border-emerald-900/60 rounded-xl text-slate-300 flex items-start gap-2 text-[11px]">
                <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <strong>Safe & Privacy First:</strong> NearPulse uses <code className="text-emerald-300">neverForLocation</code> in the Android manifest. The app does not track GPS location or collect personal data.
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: COMMAND LINE & DIRECT APK */}
          {activeTab === 'adb' && (
            <div className="space-y-4 text-xs">
              <div className="p-4 bg-[#070A0F] border border-slate-800 rounded-2xl space-y-3">
                <div className="font-bold text-sm text-white flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-cyan-400" />
                  <span>Build APK via Terminal & Install via ADB</span>
                </div>

                <p className="text-slate-400 text-xs">
                  If you have Gradle or Android SDK command line tools installed:
                </p>

                {/* Code block */}
                <div className="bg-[#05080E] border border-slate-800/80 rounded-xl p-3 font-mono text-[11px] text-slate-200 space-y-2 overflow-x-auto">
                  <div className="text-slate-500"># 1. Unzip and enter directory</div>
                  <div className="text-cyan-300">cd android</div>
                  <div className="text-slate-500 mt-2"># 2. Compile Debug APK</div>
                  <div className="text-cyan-300">./gradlew assembleDebug</div>
                  <div className="text-slate-500 mt-2"># 3. Install directly to your connected Android phone</div>
                  <div className="text-cyan-300">adb install -r app/build/outputs/apk/debug/app-debug.apk</div>
                </div>

                <div className="text-slate-400 text-[11px]">
                  💡 <em>Tip: You can also copy the generated <code className="text-cyan-300">app-debug.apk</code> file to Google Drive or send it via WhatsApp to your phone and tap it to install directly.</em>
                </div>
              </div>

              <div className="flex justify-end">
                <button
                  onClick={handleDownloadZip}
                  disabled={isExporting}
                  className="px-4 py-2 bg-cyan-400 hover:bg-cyan-300 text-black font-bold rounded-xl text-xs flex items-center gap-1.5 transition shadow"
                >
                  <Download className="w-4 h-4" />
                  <span>{isExporting ? 'Packaging ZIP...' : 'Download Full Android Project ZIP'}</span>
                </button>
              </div>
            </div>
          )}

        </div>

      </div>
    </div>
  );
};
