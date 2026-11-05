import React, { useState, useRef, useMemo } from 'react';
import {
  Play,
  Pause,
  Download,
  RotateCcw,
  FileAudio,
  FileText,
  FileSpreadsheet,
  CheckCircle,
  ExternalLink,
  Volume2,
} from 'lucide-react';
import { JobDetail, SegmentDto } from '../types';
import { getDownloadUrl, getAudioStreamUrl } from '../services/api';

interface ResultPlayerProps {
  job: JobDetail;
  onRegenerateSegment: (segmentIndex: number) => Promise<void>;
  onOpenGuide: () => void;
}

export const ResultPlayer: React.FC<ResultPlayerProps> = ({
  job,
  onRegenerateSegment,
  onOpenGuide,
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [audioVersion, setAudioVersion] = useState(0);
  const [regeneratingIndex, setRegeneratingIndex] = useState<number | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Stable audio URL that does NOT change on every re-render!
  const audioUrl = useMemo(
    () => getAudioStreamUrl(job.id, audioVersion),
    [job.id, audioVersion]
  );

  const togglePlay = async () => {
    if (!audioRef.current) return;
    try {
      if (audioRef.current.paused) {
        await audioRef.current.play();
      } else {
        audioRef.current.pause();
      }
    } catch (err) {
      console.error('Audio play error:', err);
    }
  };

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
    }
  };

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      setDuration(audioRef.current.duration);
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setCurrentTime(val);
    if (audioRef.current) {
      audioRef.current.currentTime = val;
    }
  };

  const jumpToSegment = async (startTime: number) => {
    if (!audioRef.current) return;
    try {
      audioRef.current.currentTime = startTime;
      await audioRef.current.play();
    } catch (err) {
      console.error('Jump to segment error:', err);
    }
  };

  const handleRegenerate = async (idx: number) => {
    try {
      setRegeneratingIndex(idx);
      await onRegenerateSegment(idx);
      setAudioVersion((v) => v + 1);
    } finally {
      setRegeneratingIndex(null);
    }
  };

  const formatSeconds = (sec: number) => {
    if (isNaN(sec) || sec < 0) return '00:00.0';
    const mins = Math.floor(sec / 60);
    const secs = Math.floor(sec % 60);
    const ms = Math.floor((sec % 1) * 10);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${ms}`;
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-6">
      {/* Header & Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 rounded-lg">
            <CheckCircle className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-semibold text-base text-slate-900 dark:text-white flex items-center gap-2">
              Kết quả Ghép Âm thanh & Timeline CapCut
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-800 dark:text-emerald-300 font-medium">
                Hoàn tất
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Âm thanh xuất chuẩn 44.1kHz mono 16-bit, timeline tính theo sample không lệch giây nào.
            </p>
          </div>
        </div>

        {/* CapCut Guide link button */}
        <button
          onClick={onOpenGuide}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 hover:bg-indigo-100 transition"
        >
          <ExternalLink className="w-3.5 h-3.5" />
          <span>Hướng dẫn nhập CapCut</span>
        </button>
      </div>

      {/* Audio Player Card */}
      <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 space-y-3">
        {/* Hidden/Native Audio Element */}
        <audio
          ref={audioRef}
          src={audioUrl}
          preload="auto"
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          onEnded={() => setIsPlaying(false)}
          onError={(e) => console.error('Audio load error:', e)}
        />

        {/* Custom Scrubber Player */}
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={togglePlay}
            className="w-12 h-12 rounded-full bg-brand-600 hover:bg-brand-700 text-white flex items-center justify-center shadow-md shadow-brand-500/30 transition shrink-0 active:scale-95"
            title={isPlaying ? 'Tạm dừng' : 'Phát âm thanh'}
          >
            {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
          </button>

          <div className="flex-1 space-y-1">
            <input
              type="range"
              min="0"
              max={duration || 0}
              step="0.05"
              value={currentTime}
              onChange={handleSeek}
              className="w-full accent-brand-600 cursor-pointer h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none"
            />
            <div className="flex justify-between text-[11px] font-mono text-slate-500 dark:text-slate-400">
              <span>{formatSeconds(currentTime)}</span>
              <span>{formatSeconds(duration)}</span>
            </div>
          </div>
        </div>

        {/* Native Browser Player Bar for 100% Reliability */}
        <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60 flex items-center justify-between">
          <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">
            Trình phát chuẩn trình duyệt:
          </span>
          <audio
            controls
            preload="metadata"
            src={audioUrl}
            className="h-8 max-w-sm w-full"
          />
        </div>
      </div>

      {/* Download buttons bar */}
      <div className="space-y-2">
        <div className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
          <Download className="w-4 h-4 text-brand-500" />
          <span>Tải file về máy tính:</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-2.5">
          <a
            href={getDownloadUrl(job.id, 'wav')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-brand-200 dark:border-brand-900 bg-brand-50/50 dark:bg-brand-950/20 hover:bg-brand-100/60 dark:hover:bg-brand-900/40 transition text-center group"
          >
            <FileAudio className="w-5 h-5 text-brand-600 dark:text-brand-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Tải WAV</span>
            <span className="text-[10px] text-slate-500">Cho CapCut (Chuẩn)</span>
          </a>

          <a
            href={getDownloadUrl(job.id, 'mp3')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 transition text-center group"
          >
            <Volume2 className="w-5 h-5 text-indigo-600 dark:text-indigo-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Tải MP3</span>
            <span className="text-[10px] text-slate-500">128kbps Nhẹ</span>
          </a>

          <a
            href={getDownloadUrl(job.id, 'srt')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50/50 dark:bg-emerald-950/20 hover:bg-emerald-100/60 dark:hover:bg-emerald-900/40 transition text-center group"
          >
            <FileText className="w-5 h-5 text-emerald-600 dark:text-emerald-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Tải SRT</span>
            <span className="text-[10px] text-slate-500">Phụ đề CapCut</span>
          </a>

          <a
            href={getDownloadUrl(job.id, 'json')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 transition text-center group"
          >
            <FileText className="w-5 h-5 text-cyan-600 dark:text-cyan-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Timeline JSON</span>
            <span className="text-[10px] text-slate-500">Dữ liệu thô</span>
          </a>

          <a
            href={getDownloadUrl(job.id, 'csv')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 transition text-center group"
          >
            <FileSpreadsheet className="w-5 h-5 text-amber-600 dark:text-amber-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Timeline CSV</span>
            <span className="text-[10px] text-slate-500">Mở trong Excel</span>
          </a>
        </div>
      </div>

      {/* Segments Table */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-700 dark:text-slate-300">
          <span>Chi tiết các đoạn ({job.segments.length} đoạn):</span>
          <span className="text-[11px] text-slate-400 font-normal">
            Bấm vào thời gian để nghe đoạn đó • Bấm "Đọc lại" để bỏ cache và tạo lại riêng đoạn đó
          </span>
        </div>

        <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
          <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-800 text-left text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400">
              <tr>
                <th className="py-2.5 px-3 font-medium w-12">STT</th>
                <th className="py-2.5 px-3 font-medium w-40">Mốc thời gian (CapCut)</th>
                <th className="py-2.5 px-3 font-medium w-32">Nguồn / Key</th>
                <th className="py-2.5 px-3 font-medium">Nội dung</th>
                <th className="py-2.5 px-3 font-medium text-right w-24">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800 text-slate-700 dark:text-slate-300">
              {job.segments.map((seg: SegmentDto) => {
                const isRegen = regeneratingIndex === seg.index;
                return (
                  <tr key={seg.index} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition">
                    <td className="py-2.5 px-3 text-slate-400 font-mono">{seg.index}</td>
                    <td className="py-2.5 px-3 font-mono">
                      <div className="space-y-0.5">
                        <button
                          onClick={() => jumpToSegment(seg.startTimeSeconds)}
                          title="Bấm để phát từ thời điểm này"
                          className="text-brand-600 dark:text-brand-400 hover:underline font-medium flex items-center gap-1"
                        >
                          <Play className="w-2.5 h-2.5 fill-current" />
                          <span>
                            {seg.startTimeSeconds.toFixed(2)}s → {seg.endTimeSeconds.toFixed(2)}s
                          </span>
                        </button>
                        <div className="flex items-center gap-1.5 text-[10px]">
                          <span className="text-slate-400 font-sans">Độ dài: {seg.durationSeconds.toFixed(2)}s</span>
                          {seg.targetStartTime !== null && (
                            <span className="px-1.5 py-0.5 rounded bg-indigo-100 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 font-mono text-[9px] font-bold">
                              Mốc: [{seg.targetStartTime}s{seg.targetEndTime ? ' - ' + seg.targetEndTime + 's' : ''}]
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-2.5 px-3">
                      {seg.fromCache ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-cyan-100 text-cyan-800 dark:bg-cyan-950/60 dark:text-cyan-300">
                          Từ cache (0 credit)
                        </span>
                      ) : (
                        <span className="font-mono text-slate-800 dark:text-slate-200">
                          {seg.keyMasked || '—'}
                        </span>
                      )}
                    </td>
                    <td className="py-2.5 px-3">
                      <div className="line-clamp-2" title={seg.text}>
                        <span className="font-medium text-slate-900 dark:text-white">
                          {seg.cleanText || seg.text}
                        </span>
                        {seg.text !== seg.cleanText && (
                          <span className="text-[10px] text-brand-600 dark:text-brand-400 ml-1.5 font-mono">
                            [có thẻ]
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-2.5 px-3 text-right">
                      <button
                        onClick={() => handleRegenerate(seg.index)}
                        disabled={isRegen}
                        title="Đọc lại riêng đoạn này (bỏ cache, dùng key pool)"
                        className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-950/50 transition disabled:opacity-40"
                      >
                        <RotateCcw className={`w-3.5 h-3.5 ${isRegen ? 'animate-spin' : ''}`} />
                        <span>Đọc lại</span>
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* CapCut Quick Summary Box */}
      <div className="p-4 rounded-xl border border-indigo-200 dark:border-indigo-900/60 bg-indigo-50/50 dark:bg-indigo-950/20 text-xs text-indigo-900 dark:text-indigo-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="space-y-1">
          <div className="font-bold flex items-center gap-1.5">
            <span>🎬 Cách dùng trong CapCut:</span>
          </div>
          <p className="text-[11px] text-slate-600 dark:text-slate-400">
            1. Kéo file <strong>.wav</strong> vào đường âm thanh của CapCut. <br />
            2. Vào tab <strong>Văn bản (Text)</strong> → <strong>Phụ đề cục bộ (Local Captions)</strong> → bấm <strong>Nhập</strong> file <strong>.srt</strong>.
          </p>
        </div>
        <button
          onClick={onOpenGuide}
          className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-xs shrink-0 self-start sm:self-auto transition"
        >
          Xem hình ảnh hướng dẫn
        </button>
      </div>
    </div>
  );
};

