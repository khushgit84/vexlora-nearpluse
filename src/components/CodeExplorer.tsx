import React, { useState } from 'react';
import { ANDROID_FILES, AndroidCodeFile } from '../data/androidFiles';
import { Code, Download, Copy, Check, FileText, Smartphone, Shield, Database, Wrench } from 'lucide-react';
import JSZip from 'jszip';

export const CodeExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<AndroidCodeFile>(ANDROID_FILES[1]); // BleManager.kt default
  const [copied, setCopied] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadZip = async () => {
    setIsExporting(true);
    try {
      const zip = new JSZip();

      // Add all project files into zip structure
      ANDROID_FILES.forEach((f) => {
        zip.file(f.path, f.content);
      });

      // Add root settings & build files
      zip.file('android/settings.gradle.kts', `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "VexloraNearPulse"
include(":app")`);

      zip.file('android/build.gradle.kts', `plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}`);

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

  const getCategoryIcon = (category: AndroidCodeFile['category']) => {
    switch (category) {
      case 'kotlin-ble':
        return <Smartphone className="w-3.5 h-3.5 text-cyan-400" />;
      case 'compose-ui':
        return <Code className="w-3.5 h-3.5 text-purple-400" />;
      case 'manifest':
        return <Shield className="w-3.5 h-3.5 text-emerald-400" />;
      case 'room-db':
        return <Database className="w-3.5 h-3.5 text-amber-400" />;
      case 'gradle':
        return <Wrench className="w-3.5 h-3.5 text-rose-400" />;
      default:
        return <FileText className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  return (
    <div className="bg-[#0A0E17] border border-slate-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col h-[740px]">
      {/* Explorer Top Toolbar */}
      <div className="bg-[#0E1422] border-b border-slate-800 p-3.5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-cyan-950/60 border border-cyan-800 text-cyan-400">
            <Code className="w-4 h-4" />
          </div>
          <div>
            <div className="text-sm font-bold text-slate-100 flex items-center gap-2">
              Native Android Project Codebase
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-300 font-mono border border-cyan-800">
                Kotlin 2.0 • Jetpack Compose • Room
              </span>
            </div>
            <div className="text-xs text-slate-400">
              Ready to import into Android Studio Iguana / Jellyfish / Koala
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleCopy}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-medium border border-slate-700 transition"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
            {copied ? 'Copied' : 'Copy File'}
          </button>

          <button
            onClick={handleDownloadZip}
            disabled={isExporting}
            className="flex items-center gap-1.5 px-3.5 py-1.5 bg-cyan-400 hover:bg-cyan-300 disabled:bg-slate-800 text-black font-bold rounded-xl text-xs shadow-lg shadow-cyan-950/50 transition"
          >
            <Download className="w-3.5 h-3.5" />
            {isExporting ? 'Packaging ZIP...' : 'Download Project ZIP'}
          </button>
        </div>
      </div>

      {/* Explorer Body: Sidebar & Code View */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left File Tree Sidebar */}
        <div className="w-72 bg-[#090D15] border-r border-slate-800/80 overflow-y-auto p-2 space-y-1">
          <div className="px-2 py-1 text-[10px] font-mono uppercase tracking-wider text-slate-400">
            PROJECT FILES ({ANDROID_FILES.length})
          </div>

          {ANDROID_FILES.map((file) => {
            const isSelected = selectedFile.path === file.path;
            return (
              <button
                key={file.path}
                onClick={() => setSelectedFile(file)}
                className={`w-full text-left px-2.5 py-2 rounded-xl text-xs flex items-center gap-2 transition ${
                  isSelected
                    ? 'bg-cyan-950/60 text-cyan-200 border border-cyan-800/70 font-semibold'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                {getCategoryIcon(file.category)}
                <div className="flex-1 truncate">
                  <div className="truncate">{file.name}</div>
                  <div className="text-[10px] text-slate-400 truncate">{file.path.split('/').slice(-2, -1)[0]}</div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Right Code Display Area */}
        <div className="flex-1 flex flex-col bg-[#070A0F] overflow-hidden">
          {/* File Header Bar */}
          <div className="bg-[#0C111C] px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs">
            <div className="font-mono text-cyan-300 font-semibold flex items-center gap-2">
              <span className="text-slate-500">/</span>
              {selectedFile.path}
            </div>
            <div className="text-[11px] text-slate-400 font-sans max-w-sm truncate">
              {selectedFile.description}
            </div>
          </div>

          {/* Syntax Code View */}
          <div className="flex-1 overflow-auto p-4 font-mono text-xs text-slate-200 leading-relaxed bg-[#070A0F]">
            <pre className="select-text whitespace-pre">
              <code>{selectedFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
