import React, { useState, useEffect, useRef } from 'react';
import { Header } from './components/Header';
import { ApiKeyManager } from './components/ApiKeyManager';
import { ScriptEditor } from './components/ScriptEditor';
import { VoiceSettings } from './components/VoiceSettings';
import { JobProgress } from './components/JobProgress';
import { ResultPlayer } from './components/ResultPlayer';
import { CapCutGuideModal } from './components/CapCutGuideModal';
import { ApiKeyDto, KeyStats, JobDetail, FfmpegStatus } from './types';
import * as api from './services/api';
import { AlertCircle, AlertTriangle } from 'lucide-react';

export const App: React.FC = () => {
  // Theme state
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    const saved = localStorage.getItem('adam_tts_theme');
    if (saved) return saved === 'dark';
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  // System status
  const [ffmpegStatus, setFfmpegStatus] = useState<FfmpegStatus | null>(null);

  // Key pool state
  const [keyStats, setKeyStats] = useState<KeyStats | null>(null);
  const [keys, setKeys] = useState<ApiKeyDto[]>([]);
  const [keysLoading, setKeysLoading] = useState(false);

  // Form parameters
  const [text, setText] = useState('');
  const [splitMode, setSplitMode] = useState<'LINE' | 'PARAGRAPH'>('PARAGRAPH');
  const [modelId, setModelId] = useState('eleven_v3');
  const [stability, setStability] = useState(0.5);
  const [voiceId, setVoiceId] = useState('pNInz6obpgDQGcFmaJgB'); // Adam
  const [gapSeconds, setGapSeconds] = useState(0.4);
  const [speed, setSpeed] = useState(1.0);

  // Job state
  const [activeJob, setActiveJob] = useState<JobDetail | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  // Modal
  const [showGuideModal, setShowGuideModal] = useState(false);

  // Polling ref
  const pollingTimerRef = useRef<number | null>(null);

  // Theme effect
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('adam_tts_theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('adam_tts_theme', 'light');
    }
  }, [darkMode]);

  // Initial load
  useEffect(() => {
    loadKeyStats();
    checkFfmpeg();
  }, []);

  const checkFfmpeg = async () => {
    try {
      const status = await api.checkFfmpegStatus();
      setFfmpegStatus(status);
    } catch (e) {
      setFfmpegStatus({
        available: false,
        message: 'Không thể kết nối tới backend để kiểm tra FFmpeg.',
      });
    }
  };

  const loadKeyStats = async () => {
    try {
      setKeysLoading(true);
      const stats = await api.fetchKeyStats();
      setKeyStats(stats);
      setKeys(stats.keys);
    } catch (err: any) {
      console.error(err);
    } finally {
      setKeysLoading(false);
    }
  };

  const handleAddKeys = async (rawKeys: string) => {
    try {
      setGlobalError(null);
      await api.addKeys(rawKeys);
      await loadKeyStats();
    } catch (err: any) {
      setGlobalError(err.message || 'Không thể thêm key');
    }
  };

  const handleDeleteKey = async (id: string) => {
    try {
      setGlobalError(null);
      await api.deleteKey(id);
      await loadKeyStats();
    } catch (err: any) {
      setGlobalError(err.message || 'Không thể xóa key');
    }
  };

  const handleCheckAllKeys = async () => {
    try {
      setKeysLoading(true);
      setGlobalError(null);
      await api.checkAllKeys();
      await loadKeyStats();
    } catch (err: any) {
      setGlobalError(err.message || 'Lỗi kiểm tra credit');
    } finally {
      setKeysLoading(false);
    }
  };

  const handleCheckSingleKey = async (id: string) => {
    try {
      setGlobalError(null);
      await api.checkSingleKey(id);
      await loadKeyStats();
    } catch (err: any) {
      setGlobalError(err.message || 'Lỗi kiểm tra key');
    }
  };

  // Job Polling
  useEffect(() => {
    if (!activeJob) return;

    const isWorking = activeJob.status === 'RUNNING' || activeJob.status === 'PENDING';
    if (!isWorking) {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
        pollingTimerRef.current = null;
      }
      setIsGenerating(false);
      loadKeyStats(); // Refresh usage and remaining credits
      return;
    }

    setIsGenerating(true);
    const interval = window.setInterval(async () => {
      try {
        const updated = await api.getJob(activeJob.id);
        setActiveJob(updated);
        if (updated.status !== 'RUNNING' && updated.status !== 'PENDING') {
          clearInterval(interval);
          setIsGenerating(false);
          loadKeyStats();
        }
      } catch (err) {
        console.error('Lỗi cập nhật tiến trình:', err);
      }
    }, 1000);

    pollingTimerRef.current = interval;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [activeJob?.id, activeJob?.status]);

  const handleStartGenerate = async () => {
    if (!text.trim()) {
      setGlobalError('Vui lòng nhập nội dung kịch bản trước khi tạo giọng.');
      return;
    }

    if (keys.length === 0) {
      setGlobalError('Chưa có API key nào trong Pool. Vui lòng thêm ít nhất một API key ElevenLabs.');
      return;
    }

    try {
      setGlobalError(null);
      setIsGenerating(true);
      const newJob = await api.createJob({
        text,
        splitMode,
        modelId,
        stability,
        voiceId,
        gapSeconds,
        speed,
      });
      setActiveJob(newJob);
    } catch (err: any) {
      setIsGenerating(false);
      setGlobalError(err.message || 'Không thể bắt đầu tạo giọng.');
    }
  };

  const handleStopJob = async () => {
    if (!activeJob) return;
    try {
      await api.stopJob(activeJob.id);
      const updated = await api.getJob(activeJob.id);
      setActiveJob(updated);
    } catch (err: any) {
      setGlobalError(err.message || 'Lỗi khi dừng job');
    }
  };

  const handleResumeJob = async () => {
    if (!activeJob) return;
    try {
      setGlobalError(null);
      setIsGenerating(true);
      const updated = await api.resumeJob(activeJob.id);
      setActiveJob(updated);
    } catch (err: any) {
      setIsGenerating(false);
      setGlobalError(err.message || 'Không thể tiếp tục job');
    }
  };

  const handleRegenerateSegment = async (segmentIndex: number) => {
    if (!activeJob) return;
    try {
      setGlobalError(null);
      const updated = await api.regenerateSegment(activeJob.id, segmentIndex);
      setActiveJob(updated);
      await loadKeyStats();
    } catch (err: any) {
      setGlobalError(err.message || 'Không thể tạo lại đoạn');
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950">
      {/* Top Navigation */}
      <Header
        ffmpegStatus={ffmpegStatus}
        darkMode={darkMode}
        onToggleDarkMode={() => setDarkMode(!darkMode)}
        onOpenGuide={() => setShowGuideModal(true)}
      />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Global Error Banner */}
        {globalError && (
          <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-800 dark:text-rose-200 text-xs flex items-center justify-between shadow-xs">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-500" />
              <span>{globalError}</span>
            </div>
            <button
              onClick={() => setGlobalError(null)}
              className="text-xs font-semibold hover:underline px-2 py-0.5"
            >
              Đóng
            </button>
          </div>
        )}

        {/* FFmpeg Warning Banner if missing */}
        {ffmpegStatus && !ffmpegStatus.available && (
          <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200 text-xs flex items-start gap-2.5 shadow-xs">
            <AlertTriangle className="w-4 h-4 shrink-0 text-amber-500 mt-0.5" />
            <div className="space-y-1">
              <div className="font-bold">Cảnh báo: Không tìm thấy FFmpeg trên máy tính</div>
              <p className="opacity-90 leading-relaxed whitespace-pre-line">{ffmpegStatus.message}</p>
            </div>
          </div>
        )}

        {/* Section 1: API Key Pool Manager */}
        <ApiKeyManager
          stats={keyStats}
          keys={keys}
          loading={keysLoading}
          onAddKeys={handleAddKeys}
          onDeleteKey={handleDeleteKey}
          onCheckAllKeys={handleCheckAllKeys}
          onCheckSingleKey={handleCheckSingleKey}
        />

        {/* Section 2: Script Editor & Voice Settings */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-7">
            <ScriptEditor
              text={text}
              onChangeText={setText}
              splitMode={splitMode}
              onChangeSplitMode={setSplitMode}
              modelId={modelId}
            />
          </div>

          <div className="lg:col-span-5">
            <VoiceSettings
              modelId={modelId}
              onChangeModelId={setModelId}
              stability={stability}
              onChangeStability={setStability}
              voiceId={voiceId}
              onChangeVoiceId={setVoiceId}
              gapSeconds={gapSeconds}
              onChangeGapSeconds={setGapSeconds}
              speed={speed}
              onChangeSpeed={setSpeed}
              isGenerating={isGenerating}
              onStartGenerate={handleStartGenerate}
              disabled={!text.trim() || keys.length === 0}
            />
          </div>
        </div>

        {/* Section 3: Job Progress (when working or paused) */}
        {activeJob && (
          <JobProgress
            job={activeJob}
            onStopJob={handleStopJob}
            onResumeJob={handleResumeJob}
          />
        )}

        {/* Section 4: Result Player & CapCut Exporter */}
        {activeJob && activeJob.audioReady && (
          <ResultPlayer
            job={activeJob}
            onRegenerateSegment={handleRegenerateSegment}
            onOpenGuide={() => setShowGuideModal(true)}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 dark:border-slate-800 py-6 text-center text-xs text-slate-500 dark:text-slate-400">
        <p>Adam TTS Studio • Hoàn toàn chạy local tại 127.0.0.1:8080 • Bảo mật API Key tuyệt đối</p>
      </footer>

      {/* CapCut Guide Modal */}
      <CapCutGuideModal
        isOpen={showGuideModal}
        onClose={() => setShowGuideModal(false)}
      />
    </div>
  );
};

