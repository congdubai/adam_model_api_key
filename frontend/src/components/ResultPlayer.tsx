import React, { useState, useRef, useMemo, useEffect } from 'react';
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
  Archive,
  Film,
  Zap,
  AlertTriangle,
  RefreshCw,
  FolderOpen,
  Check,
} from 'lucide-react';
import { JobDetail, SegmentDto, CapCutProject, CapCutApplyResponse } from '../types';
import {
  getDownloadUrl,
  getSegmentDownloadUrl,
  getAudioStreamUrl,
  detectCapCut,
  fetchCapCutProjects,
  applyToCapCut,
  openCapCutFolder,
} from '../services/api';

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
  const [audioFormat, setAudioFormat] = useState<'mp3' | 'wav'>('mp3');
  const [regeneratingIndex, setRegeneratingIndex] = useState<number | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  // CapCut Direct Integration State
  const [capcutProjects, setCapcutProjects] = useState<CapCutProject[]>([]);
  const [selectedFolder, setSelectedFolder] = useState<string>('');
  const [capcutRunning, setCapcutRunning] = useState<boolean>(false);
  const [capcutAvailable, setCapcutAvailable] = useState<boolean>(false);
  const [capcutLoading, setCapcutLoading] = useState<boolean>(false);
  const [syncClips, setSyncClips] = useState<boolean>(false);
  const [addAudio, setAddAudio] = useState<boolean>(true);
  const [addSubtitles, setAddSubtitles] = useState<boolean>(true);
  const [subtitleStyle, setSubtitleStyle] = useState<'tiktok_black_bar' | 'classic_shadow'>('tiktok_black_bar');
  const [subtitleAlignment, setSubtitleAlignment] = useState<number>(1);
  const [clearCache, setClearCache] = useState<boolean>(true);
  const [syncing, setSyncing] = useState<boolean>(false);
  const [syncResult, setSyncResult] = useState<CapCutApplyResponse | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);

  useEffect(() => {
    loadCapcut();
  }, []);

  const loadCapcut = async () => {
    setCapcutLoading(true);
    try {
      const detect = await detectCapCut();
      setCapcutAvailable(detect.available);
      setCapcutRunning(detect.isRunning);
      if (detect.available) {
        const projs = await fetchCapCutProjects();
        setCapcutProjects(projs);
        if (projs.length > 0 && !selectedFolder) {
          setSelectedFolder(projs[0].folderPath);
        }
      }
    } catch (e) {
      console.warn('CapCut detect error:', e);
    } finally {
      setCapcutLoading(false);
    }
  };

  const handleApplyCapCut = async () => {
    if (!selectedFolder) return;
    setSyncing(true);
    setSyncResult(null);
    setSyncError(null);
    try {
      const res = await applyToCapCut({
        jobId: job.id,
        projectFolder: selectedFolder,
        syncVideoClips: syncClips,
        addAudioTrack: addAudio,
        addSubtitleTrack: addSubtitles,
        clearCache: clearCache,
        subtitleStyle: subtitleStyle,
        subtitleAlignment: subtitleAlignment,
      });
      if (res.success) {
        setSyncResult(res);
      } else {
        setSyncError(res.message);
      }
    } catch (err: any) {
      setSyncError(err.message || 'Lỗi đồng bộ vào CapCut');
    } finally {
      setSyncing(false);
    }
  };

  const handleOpenFolder = async (path: string) => {
    try {
      await openCapCutFolder(path);
    } catch (e) {
      console.error(e);
    }
  };

  const totalJobDuration = useMemo(() => {
    if (job.segments && job.segments.length > 0) {
      const last = job.segments[job.segments.length - 1];
      return last.endTimeSeconds || 0;
    }
    return 0;
  }, [job.segments]);

  const effectiveDuration = duration > 0 ? duration : totalJobDuration;

  // Stable audio URL that does NOT change on every re-render!
  const audioUrl = useMemo(
    () => getAudioStreamUrl(job.id, audioVersion, audioFormat),
    [job.id, audioVersion, audioFormat]
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
      console.warn('Audio play error, reloading:', err);
      try {
        audioRef.current.load();
        await audioRef.current.play();
      } catch (retryErr) {
        console.error('Audio retry error:', retryErr);
      }
    }
  };

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
    }
  };

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      const d = audioRef.current.duration;
      if (!isNaN(d) && isFinite(d) && d > 0) {
        setDuration(d);
      }
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
          onLoadedData={handleLoadedMetadata}
          onCanPlay={handleLoadedMetadata}
          onDurationChange={handleLoadedMetadata}
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
              max={effectiveDuration || 0}
              step="0.05"
              value={currentTime}
              onChange={handleSeek}
              className="w-full accent-brand-600 cursor-pointer h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none"
            />
            <div className="flex justify-between text-[11px] font-mono text-slate-500 dark:text-slate-400">
              <span>{formatSeconds(currentTime)}</span>
              <span>{formatSeconds(effectiveDuration)}</span>
            </div>
          </div>
        </div>

        {/* Format Selector & Native Browser Player Bar */}
        <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
            <span>Định dạng phát:</span>
            <button
              type="button"
              onClick={() => {
                setAudioFormat('mp3');
                setAudioVersion((v) => v + 1);
              }}
              className={`px-2 py-0.5 rounded font-mono text-[11px] transition ${
                audioFormat === 'mp3'
                  ? 'bg-brand-600 text-white font-bold'
                  : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-300'
              }`}
            >
              MP3
            </button>
            <button
              type="button"
              onClick={() => {
                setAudioFormat('wav');
                setAudioVersion((v) => v + 1);
              }}
              className={`px-2 py-0.5 rounded font-mono text-[11px] transition ${
                audioFormat === 'wav'
                  ? 'bg-brand-600 text-white font-bold'
                  : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-300'
              }`}
            >
              WAV
            </button>
          </div>

          <audio
            key={audioUrl}
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

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
          <a
            href={getDownloadUrl(job.id, 'wav')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-brand-200 dark:border-brand-900 bg-brand-50/50 dark:bg-brand-950/20 hover:bg-brand-100/60 dark:hover:bg-brand-900/40 transition text-center group"
          >
            <FileAudio className="w-5 h-5 text-brand-600 dark:text-brand-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Tải WAV</span>
            <span className="text-[10px] text-slate-500">Khối liền CapCut</span>
          </a>

          <a
            href={getDownloadUrl(job.id, 'zip')}
            download
            className="flex flex-col items-center justify-center p-3 rounded-xl border border-violet-200 dark:border-violet-900 bg-violet-50/50 dark:bg-violet-950/20 hover:bg-violet-100/60 dark:hover:bg-violet-900/40 transition text-center group"
          >
            <Archive className="w-5 h-5 text-violet-600 dark:text-violet-400 mb-1 group-hover:scale-110 transition" />
            <span className="text-xs font-bold text-slate-900 dark:text-white">Tải ZIP Đoạn Rời</span>
            <span className="text-[10px] text-slate-500">Từng file riêng biệt</span>
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
                      <div className="flex items-center justify-end gap-1.5">
                        <a
                          href={getSegmentDownloadUrl(job.id, seg.index)}
                          download
                          title={`Tải riêng âm thanh đoạn ${seg.index}`}
                          className="p-1 rounded text-slate-500 hover:text-brand-600 dark:hover:text-brand-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </a>
                        <button
                          onClick={() => handleRegenerate(seg.index)}
                          disabled={isRegen}
                          title="Đọc lại riêng đoạn này (bỏ cache, dùng key pool)"
                          className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-950/50 transition disabled:opacity-40"
                        >
                          <RotateCcw className={`w-3.5 h-3.5 ${isRegen ? 'animate-spin' : ''}`} />
                          <span>Đọc lại</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* CapCut Direct Sync & Automation Box */}
      <div className="p-5 rounded-2xl border-2 border-indigo-200 dark:border-indigo-800/80 bg-gradient-to-br from-indigo-50/70 via-purple-50/40 to-white dark:from-indigo-950/40 dark:via-purple-950/20 dark:to-slate-900 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-indigo-100 dark:border-indigo-900/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-indigo-600 text-white rounded-xl shadow-md shadow-indigo-500/20">
              <Film className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-sm text-slate-900 dark:text-white">
                  Đồng bộ Trực tiếp vào Project CapCut PC
                </h3>
                <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300">
                  {capcutAvailable ? 'Đã kết nối CapCut PC' : 'Tự Động 100%'}
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Tự động ghi âm thanh, phụ đề chữ chạy và co dãn clip video khớp từng câu nói vào project CapCut.
              </p>
            </div>
          </div>

          <button
            onClick={onOpenGuide}
            className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1 shrink-0"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>Cách kéo thủ công</span>
          </button>
        </div>

        {/* Project Selector Bar */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
          <div className="md:col-span-8 flex items-center gap-2">
            <div className="flex-1 space-y-1">
              <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                <span>Chọn Project CapCut trên máy:</span>
                {capcutRunning && (
                  <span className="text-[10px] text-amber-600 dark:text-amber-400 flex items-center gap-1 font-normal">
                    <AlertTriangle className="w-3 h-3" />
                    CapCut đang chạy (khuyên nên đóng CapCut trước khi ghi)
                  </span>
                )}
              </label>
              <div className="flex items-center gap-2">
                <select
                  value={selectedFolder}
                  onChange={(e) => setSelectedFolder(e.target.value)}
                  disabled={syncing || capcutLoading || capcutProjects.length === 0}
                  className="flex-1 text-xs p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 outline-none"
                >
                  {capcutProjects.length === 0 ? (
                    <option value="">
                      {capcutLoading ? 'Đang quét project CapCut...' : 'Không tìm thấy project CapCut nào'}
                    </option>
                  ) : (
                    capcutProjects.map((p) => (
                      <option key={p.folderPath} value={p.folderPath}>
                        {p.name} ({p.clipCount} clip{p.durationSeconds > 0 ? ` • ${(p.durationSeconds).toFixed(1)}s` : ''})
                      </option>
                    ))
                  )}
                </select>
                <button
                  type="button"
                  onClick={loadCapcut}
                  disabled={capcutLoading || syncing}
                  title="Quét lại danh sách project CapCut"
                  className="p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 transition"
                >
                  <RefreshCw className={`w-4 h-4 ${capcutLoading ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>
          </div>

          <div className="md:col-span-4 flex items-end">
            <button
              type="button"
              onClick={handleApplyCapCut}
              disabled={syncing || !selectedFolder}
              className="w-full py-2.5 px-4 rounded-xl font-bold text-xs text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 shadow-md shadow-indigo-500/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-1.5 transition"
            >
              {syncing ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Đang ghi vào CapCut...</span>
                </>
              ) : (
                <>
                  <Zap className="w-4 h-4 text-amber-300" />
                  <span>⚡ Áp dụng vào CapCut</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Options Row */}
        <div className="flex flex-wrap items-center gap-4 text-xs text-slate-700 dark:text-slate-300 pt-1">
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={addSubtitles}
              onChange={(e) => setAddSubtitles(e.target.checked)}
              className="rounded accent-indigo-600"
            />
            <span className="font-medium">Ghi track phụ đề chữ chạy (Subtitle)</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={addAudio}
              onChange={(e) => setAddAudio(e.target.checked)}
              className="rounded accent-indigo-600"
            />
            <span>Ghi dải âm thanh lồng tiếng (Voice WAV)</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={syncClips}
              onChange={(e) => setSyncClips(e.target.checked)}
              className="rounded accent-indigo-600"
            />
            <span>Tua tốc độ video/ảnh theo câu nói (0.4x - 2.0x)</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={clearCache}
              onChange={(e) => setClearCache(e.target.checked)}
              className="rounded accent-indigo-600"
            />
            <span>Xóa cache CapCut trước khi ghi</span>
          </label>
        </div>

        {/* Subtitle Customization & Preview Panel */}
        {addSubtitles && (
          <div className="p-3.5 rounded-xl bg-slate-900/5 dark:bg-slate-800/60 border border-indigo-200/80 dark:border-indigo-900/60 space-y-2.5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                  Kiểu phụ đề:
                </span>
                <div className="inline-flex rounded-lg border border-slate-300 dark:border-slate-700 p-0.5 bg-white dark:bg-slate-900 text-[11px]">
                  <button
                    type="button"
                    onClick={() => setSubtitleStyle('tiktok_black_bar')}
                    className={`px-2.5 py-1 rounded-md transition ${
                      subtitleStyle === 'tiktok_black_bar'
                        ? 'bg-indigo-600 text-white font-bold shadow-xs'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                    }`}
                  >
                    Chữ trắng nền đen mờ bo góc (như video mẫu)
                  </button>
                  <button
                    type="button"
                    onClick={() => setSubtitleStyle('classic_shadow')}
                    className={`px-2.5 py-1 rounded-md transition ${
                      subtitleStyle === 'classic_shadow'
                        ? 'bg-indigo-600 text-white font-bold shadow-xs'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                    }`}
                  >
                    Viền bóng mờ cổ điển
                  </button>
                </div>
              </div>

              {/* Alignment */}
              <div className="flex items-center gap-1.5 text-[11px]">
                <span className="text-slate-500 dark:text-slate-400">Căn lề:</span>
                <div className="inline-flex rounded-lg border border-slate-300 dark:border-slate-700 p-0.5 bg-white dark:bg-slate-900">
                  <button
                    type="button"
                    onClick={() => setSubtitleAlignment(1)}
                    className={`px-2 py-0.5 rounded transition ${
                      subtitleAlignment === 1
                        ? 'bg-indigo-600 text-white font-bold'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                    title="Căn giữa dòng chuẩn khung hình"
                  >
                    Căn giữa (chuẩn)
                  </button>
                  <button
                    type="button"
                    onClick={() => setSubtitleAlignment(0)}
                    className={`px-2 py-0.5 rounded transition ${
                      subtitleAlignment === 0
                        ? 'bg-indigo-600 text-white font-bold'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                    title="Căn lề trái"
                  >
                    Căn trái
                  </button>
                </div>
              </div>
            </div>

            {/* Visual Preview Box */}
            <div className="relative overflow-hidden rounded-lg bg-slate-900/90 dark:bg-black/90 p-3 border border-slate-800 text-left">
              <div className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold mb-1.5 flex items-center justify-between">
                <span>Xem trước mẫu phụ đề:</span>
                <span className="text-[10px] lowercase text-emerald-400 font-normal">
                  tự động canh vị trí chuẩn không bị che bởi mô tả & nút tương tác
                </span>
              </div>
              <div className={subtitleAlignment === 0 ? 'text-left' : 'text-center'}>
                {subtitleStyle === 'tiktok_black_bar' ? (
                  <div className="inline-block px-3 py-1.5 rounded-md bg-black/75 text-white font-sans text-xs tracking-wide leading-relaxed shadow-sm">
                    {subtitleAlignment === 0 ? (
                      <>
                        phấn nền son chỉ một lần lướt là đi hết<br />luôn hơ
                      </>
                    ) : (
                      'phấn nền son chỉ một lần lướt là đi hết luôn hơ'
                    )}
                  </div>
                ) : (
                  <div
                    className="inline-block text-white font-sans text-xs tracking-wide font-medium"
                    style={{
                      textShadow: '0 2px 4px rgba(0,0,0,0.9), 0 0 2px #000',
                    }}
                  >
                    phấn nền son chỉ một lần lướt là đi hết luôn hơ
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Success / Error Feedback */}
        {syncResult && (
          <div className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 text-xs text-emerald-900 dark:text-emerald-200 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
              <span>{syncResult.message}</span>
            </div>
            <button
              onClick={() => handleOpenFolder(syncResult.projectFolder)}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-[11px] transition shrink-0"
            >
              <FolderOpen className="w-3.5 h-3.5" />
              <span>Mở thư mục CapCut</span>
            </button>
          </div>
        )}

        {syncError && (
          <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-200 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-500 shrink-0" />
            <span>{syncError}</span>
          </div>
        )}
      </div>
    </div>
  );
};

