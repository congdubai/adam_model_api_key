import React from 'react';
import { Volume2, Moon, Sun, AlertTriangle, CheckCircle2, HelpCircle } from 'lucide-react';
import { FfmpegStatus } from '../types';

interface HeaderProps {
  ffmpegStatus: FfmpegStatus | null;
  darkMode: boolean;
  onToggleDarkMode: () => void;
  onOpenGuide: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  ffmpegStatus,
  darkMode,
  onToggleDarkMode,
  onOpenGuide,
}) => {
  return (
    <header className="border-b border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
            <Volume2 className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-lg sm:text-xl text-slate-900 dark:text-white tracking-tight">
                Adam TTS Studio
              </h1>
              <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full bg-brand-100 text-brand-700 dark:bg-brand-950 dark:text-brand-300 border border-brand-200 dark:border-brand-800">
                v3 CapCut Ready
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block">
              ElevenLabs Key Pool • Đồng bộ Timeline • Xuất WAV & SRT
            </p>
          </div>
        </div>

        {/* Right tools */}
        <div className="flex items-center gap-3">
          {/* FFmpeg status badge */}
          {ffmpegStatus && (
            <div
              title={ffmpegStatus.message}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border cursor-help ${
                ffmpegStatus.available
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800'
                  : 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800'
              }`}
            >
              {ffmpegStatus.available ? (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                  <span>FFmpeg OK</span>
                </>
              ) : (
                <>
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                  <span>Thiếu FFmpeg</span>
                </>
              )}
            </div>
          )}

          {/* Guide button */}
          <button
            onClick={onOpenGuide}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <HelpCircle className="w-4 h-4 text-brand-500" />
            <span className="hidden sm:inline">Cách dùng CapCut</span>
          </button>

          {/* Theme toggle */}
          <button
            onClick={onToggleDarkMode}
            className="p-2 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            title={darkMode ? 'Chuyển sang giao diện Sáng' : 'Chuyển sang giao diện Tối'}
          >
            {darkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-600" />}
          </button>
        </div>
      </div>
    </header>
  );
};

