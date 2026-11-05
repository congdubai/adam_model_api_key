import React from 'react';
import { Loader2, Square, Play, AlertCircle, AlertTriangle } from 'lucide-react';
import { JobDetail } from '../types';

interface JobProgressProps {
  job: JobDetail;
  onStopJob: () => void;
  onResumeJob: () => void;
}

export const JobProgress: React.FC<JobProgressProps> = ({ job, onStopJob, onResumeJob }) => {
  const percent = job.totalSegments > 0 ? Math.round((job.completedSegments / job.totalSegments) * 100) : 0;
  const isRunning = job.status === 'RUNNING' || job.status === 'PENDING';
  const isPausedNoKeys = job.status === 'PAUSED_NO_KEYS';
  const isStopped = job.status === 'STOPPED';
  const isFailed = job.status === 'FAILED';

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div
            className={`p-2 rounded-lg ${
              isRunning
                ? 'bg-brand-50 dark:bg-brand-950/50 text-brand-600'
                : isPausedNoKeys
                ? 'bg-amber-50 dark:bg-amber-950/50 text-amber-600'
                : isFailed
                ? 'bg-rose-50 dark:bg-rose-950/50 text-rose-600'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-600'
            }`}
          >
            {isRunning ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : isPausedNoKeys ? (
              <AlertTriangle className="w-5 h-5" />
            ) : isFailed ? (
              <AlertCircle className="w-5 h-5" />
            ) : (
              <Square className="w-5 h-5" />
            )}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-base text-slate-900 dark:text-white">
                {isRunning
                  ? 'Đang tiến hành lồng tiếng...'
                  : isPausedNoKeys
                  ? 'Tạm dừng: Hết API Key khả dụng'
                  : isStopped
                  ? 'Tiến trình đã dừng'
                  : 'Tiến trình thất bại'}
              </h3>
              <span className="text-xs px-2 py-0.5 rounded-full font-mono bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                {job.completedSegments} / {job.totalSegments} đoạn
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isRunning && job.currentKeyMasked
                ? `Đang dùng key: ${job.currentKeyMasked}`
                : isPausedNoKeys
                ? 'Các đoạn đã hoàn thành được bảo lưu toàn vẹn. Hãy thêm key mới để tiếp tục.'
                : isStopped
                ? 'Đã bảo lưu các đoạn đã xong.'
                : 'Đã có lỗi xảy ra trong quá trình xử lý.'}
            </p>
          </div>
        </div>

        {/* Action button */}
        <div className="flex items-center gap-2">
          {isRunning && (
            <button
              onClick={onStopJob}
              className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-medium rounded-lg border border-rose-300 dark:border-rose-800 text-rose-700 dark:text-rose-300 hover:bg-rose-50 dark:hover:bg-rose-950/50 transition"
            >
              <Square className="w-3.5 h-3.5 fill-current" />
              <span>Dừng lại</span>
            </button>
          )}

          {(isPausedNoKeys || isStopped) && (
            <button
              onClick={onResumeJob}
              className="flex items-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition shadow-sm"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Thêm Key & Tiếp tục</span>
            </button>
          )}
        </div>
      </div>

      {/* Progress Bar */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-500 dark:text-slate-400 font-medium">Tiến độ tổng thể</span>
          <span className="font-bold text-brand-600 dark:text-brand-400">{percent}%</span>
        </div>
        <div className="w-full bg-slate-100 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-300 ${
              isPausedNoKeys
                ? 'bg-amber-500'
                : isFailed
                ? 'bg-rose-500'
                : 'bg-gradient-to-r from-brand-500 to-indigo-600'
            }`}
            style={{ width: `${percent}%` }}
          />
        </div>
      </div>

      {/* Error or Alert Message */}
      {job.errorMessage && (
        <div
          className={`p-3 rounded-xl border text-xs flex items-start gap-2 ${
            isPausedNoKeys
              ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-200 dark:border-amber-900/40 text-amber-800 dark:text-amber-300'
              : 'bg-rose-50 dark:bg-rose-950/30 border-rose-200 dark:border-rose-900/40 text-rose-800 dark:text-rose-300'
          }`}
        >
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <div className="font-semibold">{job.errorMessage}</div>
            {isPausedNoKeys && (
              <p className="text-[11px] opacity-90">
                👉 <strong>Cách xử lý:</strong> Hãy kéo lên phần <strong>Quản lý Pool API Key</strong> ở trên, dán thêm key ElevenLabs còn credit, sau đó bấm nút <strong>"Thêm Key & Tiếp tục"</strong>. Các đoạn trước đó sẽ không cần đọc lại!
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
