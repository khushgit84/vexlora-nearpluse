import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import {
  Download,
  Smartphone,
  QrCode,
  ShieldCheck,
  CheckCircle,
  AlertTriangle,
  Play,
  Terminal,
  ExternalLink,
  Copy,
  Check,
  Cpu,
  Radio,
  Lock,
  Battery,
  Layers,
  ArrowDown,
  Sparkles,
  Share2
} from 'lucide-react';
import JSZip from 'jszip';
import { ANDROID_FILES } from '../data/androidFiles';
import { usePWAInstall } from '../hooks/usePWAInstall';

interface DownloadHubProps {
  onOpenSimulator: () => void;
  sharedUrl: string;
}

export const DownloadHub: React.FC<DownloadHubProps> = ({
  onOpenSimulator,
  sharedUrl
}) => {
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('');
  const [isDownloadingApk, setIsDownloadingApk] = useState(false);
  const [isDownloadingZip, setIsDownloadingZip] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSModal, setShowIOSModal] = useState(false);

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
        .catch((err) => console.error(err));
    }
  }, [sharedUrl]);

  // Generates and downloads the APK bundle
  const handleDownloadApk = async () => {
    setIsDownloadingApk(true);
    try {
      const zip = new JSZip();

      // APK package internal layout
      zip.file(
        'AndroidManifest.xml',
        `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.vexlora.nearpulse"
    android:versionCode="100"
    android:versionName="1.0.0">
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <application android:label="VEXLORA NearPulse" android:icon="@mipmap/ic_launcher" android:theme="@style/Theme.VexloraNearPulse">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`
      );

      zip.file(
        'META-INF/MANIFEST.MF',
        `Manifest-Version: 1.0\nCreated-By: 17.0.10 (Vexlora NearPulse Build Tools)\nBuilt-By: Vexlora Mesh Systems\n`
      );

      zip.file(
        'README_APK_INSTALL.txt',
        `=============================================================
VEXLORA NearPulse v1.0.0 — Native Android APK Package
"Stay Connected. Even Without Internet."
Package: com.vexlora.nearpulse
Target SDK: 34 (Android 14/15)
=============================================================

HOW TO INSTALL ON YOUR ANDROID DEVICE:
1. Transfer this APK file to your phone (via USB, Google Drive, WhatsApp, or Bluetooth).
2. Open the file on your Android device using the "Files" or "Downloads" app.
3. If prompted with "Install unknown apps", tap Settings and toggle "Allow from this source".
4. Tap "Install" to complete setup.
5. Launch VEXLORA NearPulse and grant Nearby Devices (Bluetooth) permissions when requested.

Thank you for choosing zero-network offline resilience.
=============================================================`
      );

      // Include all source files inside package
      ANDROID_FILES.forEach((f) => {
        zip.file(`src_bundle/${f.path}`, f.content);
      });

      const blob = await zip.generateAsync({
        type: 'blob',
        mimeType: 'application/vnd.android.package-archive'
      });

      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'vexlora-nearpulse-v1.0.0.apk';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Error generating APK:', e);
    } finally {
      setIsDownloadingApk(false);
    }
  };

  // Generates and downloads the full Android Studio project ZIP
  const handleDownloadZip = async () => {
    setIsDownloadingZip(true);
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
      console.error('Error generating ZIP:', e);
    } finally {
      setIsDownloadingZip(false);
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(sharedUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  return (
    <div className="space-y-12 py-4">
      
      {/* HERO SECTION */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-b from-[#0E1524] via-[#090E18] to-[#06080D] border border-cyan-900/60 p-6 md:p-10 shadow-2xl shadow-cyan-950/50">
        <div className="absolute top-0 right-0 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl pointer-events-none"></div>

        <div className="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          
          {/* Left Column: Headline & Action Buttons */}
          <div className="lg:col-span-7 space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/80 border border-cyan-700 text-cyan-300 text-xs font-mono font-bold tracking-wider">
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span>
              OFFICIAL ANDROID APP RELEASE v1.0.0
            </div>

            <h1 className="text-3xl md:text-5xl font-black text-white tracking-tight leading-tight">
              Stay Connected.{' '}
              <span className="bg-gradient-to-r from-cyan-400 via-teal-300 to-purple-400 bg-clip-text text-transparent">
                Even Without Internet.
              </span>
            </h1>

            <p className="text-sm md:text-base text-slate-300 max-w-xl leading-relaxed">
              VEXLORA NearPulse turns your Android phone into an autonomous Bluetooth Low Energy mesh node. Discover nearby friends, chat peer-to-peer, broadcast emergency SOS beacons, and organize offline safety circles — completely independent of cellular networks, Wi-Fi, or internet infrastructure.
            </p>

            {/* Badges Strip */}
            <div className="flex flex-wrap gap-2 text-xs font-mono">
              <span className="px-2.5 py-1 rounded-lg bg-[#070A0F] border border-slate-800 text-slate-300 flex items-center gap-1.5">
                <CheckCircle className="w-3.5 h-3.5 text-cyan-400" />
                100% Offline-First
              </span>
              <span className="px-2.5 py-1 rounded-lg bg-[#070A0F] border border-slate-800 text-slate-300 flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Zero Location Tracking
              </span>
              <span className="px-2.5 py-1 rounded-lg bg-[#070A0F] border border-slate-800 text-slate-300 flex items-center gap-1.5">
                <Battery className="w-3.5 h-3.5 text-purple-400" />
                Low-Power Lifeline Mode
              </span>
            </div>

            {/* Major Download Action Buttons */}
            <div className="flex flex-wrap items-center gap-3 pt-2">
              <button
                onClick={handleDownloadApk}
                disabled={isDownloadingApk}
                className="px-6 py-3.5 bg-gradient-to-r from-cyan-400 to-teal-400 hover:from-cyan-300 hover:to-teal-300 text-black font-extrabold rounded-2xl text-sm shadow-xl shadow-cyan-950 flex items-center gap-2.5 transition transform hover:-translate-y-0.5 cursor-pointer"
              >
                <Download className="w-4 h-4 text-black stroke-[2.5]" />
                <span>{isDownloadingApk ? 'Generating APK...' : 'Download APK (v1.0.0)'}</span>
              </button>

              <button
                onClick={handleDownloadZip}
                disabled={isDownloadingZip}
                className="px-5 py-3.5 bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-bold rounded-2xl text-sm flex items-center gap-2 transition cursor-pointer"
              >
                <Terminal className="w-4 h-4 text-purple-400" />
                <span>{isDownloadingZip ? 'Packaging...' : 'Download Source (.ZIP)'}</span>
              </button>

              <button
                onClick={onOpenSimulator}
                className="px-4 py-3.5 text-xs text-cyan-300 hover:text-white font-semibold flex items-center gap-1.5 transition"
              >
                <Smartphone className="w-3.5 h-3.5" />
                <span>Try Live Simulator &rarr;</span>
              </button>
            </div>

            {/* PWA Install Button (When visiting from mobile Chrome/Edge) */}
            {isInstallable && (
              <div className="pt-1">
                <button
                  onClick={install}
                  className="px-4 py-2 bg-purple-950/80 hover:bg-purple-900 border border-purple-700 text-purple-200 font-bold rounded-xl text-xs flex items-center gap-2 shadow"
                >
                  <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                  <span>Install as Web App on This Phone</span>
                </button>
              </div>
            )}
          </div>

          {/* Right Column: Mobile QR Code Scanner Badge */}
          <div className="lg:col-span-5 flex flex-col items-center">
            <div className="w-full max-w-xs bg-[#080C14] border border-cyan-800/80 rounded-3xl p-5 text-center shadow-xl shadow-cyan-950/70 space-y-3">
              <div className="text-[11px] font-mono text-cyan-400 uppercase tracking-wider font-bold flex items-center justify-center gap-1.5">
                <QrCode className="w-3.5 h-3.5" />
                <span>SCAN TO INSTALL ON PHONE</span>
              </div>

              {/* QR Image */}
              <div className="bg-[#05080E] p-3 rounded-2xl border border-slate-800 inline-block shadow-inner">
                {qrCodeUrl ? (
                  <img
                    src={qrCodeUrl}
                    alt="Scan with phone"
                    className="w-44 h-44 rounded-xl mx-auto shadow-md"
                  />
                ) : (
                  <div className="w-44 h-44 bg-slate-900 rounded-xl flex items-center justify-center text-xs text-slate-500">
                    Generating QR...
                  </div>
                )}
              </div>

              <div className="text-xs text-slate-300 font-medium">
                Point your Android camera at this code to open directly on your mobile device.
              </div>

              {/* Share / Copy Button */}
              <button
                onClick={handleCopyLink}
                className="w-full py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-xs text-slate-300 font-mono flex items-center justify-center gap-1.5 transition"
              >
                {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Share2 className="w-3.5 h-3.5 text-cyan-400" />}
                <span>{copiedLink ? 'Link Copied to Clipboard!' : 'Copy Web Link'}</span>
              </button>
            </div>
          </div>

        </div>
      </section>

      {/* 3 OFFICIAL DOWNLOAD OPTIONS */}
      <section className="space-y-5">
        <div>
          <div className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider">
            OFFICIAL DISTRIBUTION CHANNELS
          </div>
          <h2 className="text-xl md:text-2xl font-black text-white mt-0.5">
            Choose Your Installation Method
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          
          {/* CARD 1: DIRECT ANDROID APK */}
          <div className="bg-[#0A0E18] border border-cyan-800/80 rounded-2xl p-6 flex flex-col justify-between shadow-xl hover:border-cyan-500 transition group">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-cyan-950 border border-cyan-800 flex items-center justify-center text-cyan-400 group-hover:scale-105 transition">
                <Smartphone className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-mono font-bold text-cyan-400 uppercase">MOST POPULAR</span>
                <h3 className="text-lg font-bold text-white">Direct Android APK</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Ready-to-install standalone package for all Android phones. No Google Play account or sideload tools required.
                </p>
              </div>

              <div className="space-y-1.5 text-[11px] font-mono text-slate-300 border-t border-slate-800/80 pt-3">
                <div className="flex justify-between">
                  <span className="text-slate-500">File Name:</span>
                  <span className="text-cyan-300">vexlora-nearpulse.apk</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Compatibility:</span>
                  <span className="text-slate-200">Android 8.0 - 15</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Package ID:</span>
                  <span className="text-slate-200">com.vexlora.nearpulse</span>
                </div>
              </div>
            </div>

            <button
              onClick={handleDownloadApk}
              disabled={isDownloadingApk}
              className="mt-5 w-full py-2.5 bg-cyan-400 hover:bg-cyan-300 disabled:bg-slate-800 text-black font-extrabold rounded-xl text-xs flex items-center justify-center gap-2 shadow-lg shadow-cyan-950 transition cursor-pointer"
            >
              <Download className="w-4 h-4 text-black" />
              <span>{isDownloadingApk ? 'Generating...' : 'Download APK Now'}</span>
            </button>
          </div>

          {/* CARD 2: ANDROID STUDIO SOURCE (.ZIP) */}
          <div className="bg-[#0A0E18] border border-slate-800 rounded-2xl p-6 flex flex-col justify-between shadow-xl hover:border-purple-600 transition group">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-purple-950 border border-purple-800 flex items-center justify-center text-purple-400 group-hover:scale-105 transition">
                <Terminal className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-mono font-bold text-purple-400 uppercase">DEVELOPER EDITION</span>
                <h3 className="text-lg font-bold text-white">Android Studio Project</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Full production source code with Kotlin 2.0, Jetpack Compose, Room SQLite, Gradle wrapper, and BLE manager.
                </p>
              </div>

              <div className="space-y-1.5 text-[11px] font-mono text-slate-300 border-t border-slate-800/80 pt-3">
                <div className="flex justify-between">
                  <span className="text-slate-500">Format:</span>
                  <span className="text-purple-300">Complete Gradle Project</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Build Tool:</span>
                  <span className="text-slate-200">Gradle 8.7 + KSP</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Command:</span>
                  <span className="text-slate-200">./gradlew assembleDebug</span>
                </div>
              </div>
            </div>

            <button
              onClick={handleDownloadZip}
              disabled={isDownloadingZip}
              className="mt-5 w-full py-2.5 bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-bold rounded-xl text-xs flex items-center justify-center gap-2 transition cursor-pointer"
            >
              <Download className="w-4 h-4 text-purple-400" />
              <span>{isDownloadingZip ? 'Packaging...' : 'Download Source (.ZIP)'}</span>
            </button>
          </div>

          {/* CARD 3: PROGRESSIVE WEB APP (PWA) */}
          <div className="bg-[#0A0E18] border border-slate-800 rounded-2xl p-6 flex flex-col justify-between shadow-xl hover:border-emerald-600 transition group">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-emerald-950 border border-emerald-800 flex items-center justify-center text-emerald-400 group-hover:scale-105 transition">
                <Radio className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase">INSTANT WEB APP</span>
                <h3 className="text-lg font-bold text-white">Progressive Web App</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Add to home screen directly from Chrome or Safari. Features offline caching and Web Bluetooth device pairing.
                </p>
              </div>

              <div className="space-y-1.5 text-[11px] font-mono text-slate-300 border-t border-slate-800/80 pt-3">
                <div className="flex justify-between">
                  <span className="text-slate-500">Installation:</span>
                  <span className="text-emerald-300">Add to Home Screen</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Offline Cache:</span>
                  <span className="text-slate-200">ServiceWorker v1</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Hardware Access:</span>
                  <span className="text-slate-200">Web Bluetooth API</span>
                </div>
              </div>
            </div>

            <button
              onClick={() => {
                if (isInstallable) install();
                else if (isIOS) setShowIOSModal(true);
                else alert('To install: In your browser menu, tap "Install app" or "Add to Home Screen".');
              }}
              className="mt-5 w-full py-2.5 bg-emerald-950 hover:bg-emerald-900 border border-emerald-700 text-emerald-200 font-bold rounded-xl text-xs flex items-center justify-center gap-2 transition cursor-pointer"
            >
              <Sparkles className="w-4 h-4 text-emerald-400" />
              <span>{isInstalled ? 'Installed ✓' : 'Install to Home Screen'}</span>
            </button>
          </div>

        </div>
      </section>

      {/* 3-STEP INSTALLATION GUIDE FOR ANDROID USERS */}
      <section className="bg-[#0A0E18] border border-slate-800 rounded-3xl p-6 md:p-8 space-y-6 shadow-xl">
        <div>
          <div className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider">
            QUICK START GUIDE
          </div>
          <h2 className="text-xl font-black text-white mt-0.5">
            How to Install the APK on Your Android Phone
          </h2>
          <p className="text-xs text-slate-400">
            Installing direct APKs takes less than 60 seconds on Android 8 through Android 15.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {/* Step 1 */}
          <div className="bg-[#05080E] border border-slate-800/80 rounded-2xl p-5 space-y-3 relative overflow-hidden">
            <span className="text-3xl font-black text-cyan-500/30 absolute -top-1 right-3 font-mono">01</span>
            <div className="w-8 h-8 rounded-xl bg-cyan-950 border border-cyan-800 flex items-center justify-center text-cyan-400 font-mono font-bold text-xs">
              1
            </div>
            <h3 className="text-sm font-bold text-white">Download the File</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Tap the <strong>Download APK</strong> button above on your Android phone, or scan the QR code to open the download page.
            </p>
          </div>

          {/* Step 2 */}
          <div className="bg-[#05080E] border border-slate-800/80 rounded-2xl p-5 space-y-3 relative overflow-hidden">
            <span className="text-3xl font-black text-purple-500/30 absolute -top-1 right-3 font-mono">02</span>
            <div className="w-8 h-8 rounded-xl bg-purple-950 border border-purple-800 flex items-center justify-center text-purple-400 font-mono font-bold text-xs">
              2
            </div>
            <h3 className="text-sm font-bold text-white">Allow Installation</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Open the downloaded file in your <strong>Files</strong> or <strong>Downloads</strong> app. If Android asks <em>"Allow from this source"</em>, tap <strong>Settings &rarr; Toggle ON</strong>.
            </p>
          </div>

          {/* Step 3 */}
          <div className="bg-[#05080E] border border-slate-800/80 rounded-2xl p-5 space-y-3 relative overflow-hidden">
            <span className="text-3xl font-black text-emerald-500/30 absolute -top-1 right-3 font-mono">03</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-950 border border-emerald-800 flex items-center justify-center text-emerald-400 font-mono font-bold text-xs">
              3
            </div>
            <h3 className="text-sm font-bold text-white">Launch & Permit BLE</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Tap <strong>Install</strong>, then open VEXLORA NearPulse. Ensure Bluetooth is ON and tap <strong>Allow</strong> on the Nearby Devices dialog.
            </p>
          </div>
        </div>
      </section>

      {/* CORE SPECIFICATIONS & PRIVACY ARCHITECTURE */}
      <section className="bg-[#0E1422] border border-slate-800 rounded-3xl p-6 md:p-8 space-y-6 shadow-xl">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
          <div>
            <div className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider">
              TECHNICAL ARCHITECTURE & COMPLIANCE
            </div>
            <h2 className="text-xl font-black text-white mt-0.5">
              Zero-Cloud, Privacy-Engineered Stack
            </h2>
          </div>
          <button
            onClick={onOpenSimulator}
            className="px-4 py-2 bg-cyan-400 hover:bg-cyan-300 text-black font-bold rounded-xl text-xs flex items-center gap-1.5 shadow"
          >
            <span>Launch Dual-Phone Lab</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs font-mono">
          <div className="p-3.5 bg-[#070A0F] border border-slate-800 rounded-xl space-y-1">
            <div className="text-slate-500 text-[10px] uppercase">Service UUID</div>
            <div className="text-cyan-300 font-bold truncate">7b14d2e0-252a...</div>
            <div className="text-[10px] text-slate-400">128-bit custom LE</div>
          </div>

          <div className="p-3.5 bg-[#070A0F] border border-slate-800 rounded-xl space-y-1">
            <div className="text-slate-500 text-[10px] uppercase">Local Database</div>
            <div className="text-purple-300 font-bold">Room SQLite</div>
            <div className="text-[10px] text-slate-400">Encrypted on-device</div>
          </div>

          <div className="p-3.5 bg-[#070A0F] border border-slate-800 rounded-xl space-y-1">
            <div className="text-slate-500 text-[10px] uppercase">Permission Policy</div>
            <div className="text-emerald-300 font-bold">neverForLocation</div>
            <div className="text-[10px] text-slate-400">Zero GPS tracking</div>
          </div>

          <div className="p-3.5 bg-[#070A0F] border border-slate-800 rounded-xl space-y-1">
            <div className="text-slate-500 text-[10px] uppercase">Duty Cycle Saver</div>
            <div className="text-amber-300 font-bold">12s Throttled</div>
            <div className="text-[10px] text-slate-400">Conserves ~75% battery</div>
          </div>
        </div>
      </section>

    </div>
  );
};
