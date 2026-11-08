import React from 'react';
import { Sliders, Mic, Info, Sparkles, Zap } from 'lucide-react';

interface VoiceSettingsProps {
  modelId: string;
  onChangeModelId: (modelId: string) => void;
  stability: number;
  onChangeStability: (val: number) => void;
  voiceId: string;
  onChangeVoiceId: (voiceId: string) => void;
  gapSeconds: number;
  onChangeGapSeconds: (gap: number) => void;
  speed: number;
  onChangeSpeed: (val: number) => void;
  isGenerating: boolean;
  onStartGenerate: () => void;
  disabled: boolean;
}

const MODELS = [
  { id: 'eleven_v3', name: 'Eleven v3 (Mặc định)', desc: 'Cảm xúc đỉnh cao, hỗ trợ thẻ cảm xúc [excited], [whispers]...' },
  { id: 'eleven_multilingual_v2', name: 'Eleven Multilingual v2', desc: 'Đa ngôn ngữ, ổn định và tự nhiên cao' },
  { id: 'eleven_flash_v2_5', name: 'Eleven Flash v2.5', desc: 'Tốc độ phản hồi cực nhanh, tiết kiệm thời gian' },
];

const STABILITY_OPTIONS = [
  { value: 0.0, label: 'Creative (0.0)', desc: 'Biểu cảm mạnh mẽ, sống động nhưng có thể biến âm' },
  { value: 0.5, label: 'Natural (0.5 - Mặc định)', desc: 'Cân bằng lý tưởng giữa cảm xúc và phát âm chuẩn' },
  { value: 1.0, label: 'Robust (1.0)', desc: 'Phát âm cực kỳ ổn định, ít biến điệu nhất' },
];

export const VoiceSettings: React.FC<VoiceSettingsProps> = ({
  modelId,
  onChangeModelId,
  stability,
  onChangeStability,
  voiceId,
  onChangeVoiceId,
  gapSeconds,
  onChangeGapSeconds,
  speed,
  onChangeSpeed,
  isGenerating,
  onStartGenerate,
  disabled,
}) => {
  const isV3 = modelId === 'eleven_v3';

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-5">
      <div className="flex items-center gap-2.5">
        <div className="p-2 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 rounded-lg">
          <Sliders className="w-5 h-5" />
        </div>
        <div>
          <h2 className="font-semibold text-base text-slate-900 dark:text-white">Cài đặt Giọng đọc & Mô hình</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Tùy chỉnh model ElevenLabs, độ ổn định (stability) và khoảng nghỉ giữa các đoạn.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Model Selector */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
            Mô hình ElevenLabs (Model)
          </label>
          <select
            value={modelId}
            onChange={(e) => onChangeModelId(e.target.value)}
            className="w-full text-xs p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500 outline-none"
          >
            {MODELS.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
          <p className="text-[11px] text-slate-500 dark:text-slate-400">
            {MODELS.find((m) => m.id === modelId)?.desc}
          </p>
        </div>

        {/* Voice ID */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
            <span>Voice ID</span>
            <span className="text-[10px] text-brand-600 dark:text-brand-400 font-normal">
              pNInz6obpgDQGcFmaJgB = Adam
            </span>
          </label>
          <div className="relative">
            <input
              type="text"
              value={voiceId}
              onChange={(e) => onChangeVoiceId(e.target.value)}
              placeholder="pNInz6obpgDQGcFmaJgB"
              className="w-full text-xs font-mono p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500 outline-none"
            />
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400">
            Giọng mặc định là Adam. Bạn có thể thay bằng bất kỳ Voice ID nào từ thư viện ElevenLabs.
          </p>
        </div>
      </div>

      {/* Stability (dành cho Eleven v3) */}
      <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
            <span>Độ ổn định giọng đọc (Stability)</span>
            {isV3 && (
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-mono font-normal">
                v3: {stability}
              </span>
            )}
          </label>
          {!isV3 && (
            <span className="text-[11px] text-slate-400 italic">
              (Chỉ áp dụng sâu cho mô hình eleven_v3)
            </span>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
          {STABILITY_OPTIONS.map((opt) => {
            const isSelected = stability === opt.value;
            return (
              <button
                key={opt.value}
                type="button"
                onClick={() => onChangeStability(opt.value)}
                className={`p-3 rounded-xl border text-left transition ${
                  isSelected
                    ? 'border-brand-500 bg-brand-50/60 dark:bg-brand-950/40 text-brand-900 dark:text-brand-200 ring-2 ring-brand-500/20'
                    : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-700 dark:text-slate-300'
                }`}
              >
                <div className="text-xs font-bold flex items-center justify-between">
                  <span>{opt.label}</span>
                  {isSelected && <Sparkles className="w-3.5 h-3.5 text-brand-500" />}
                </div>
                <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 leading-snug">
                  {opt.desc}
                </div>
              </button>
            );
          })}
        </div>

        <div className="flex items-start gap-1.5 text-[11px] text-amber-700 dark:text-amber-400/90 bg-amber-50/70 dark:bg-amber-950/30 p-2.5 rounded-lg border border-amber-200/60 dark:border-amber-900/40">
          <Info className="w-4 h-4 shrink-0 mt-0.5" />
          <span>
            <strong>Lưu ý:</strong> Chế độ <strong>Creative (0.0)</strong> giúp Eleven v3 biểu cảm mạnh và cuốn hút hơn rất nhiều nhưng đôi khi dễ đọc sai từ ngữ lạ. Nếu cần an toàn tuyệt đối hãy chọn <strong>Natural (0.5)</strong> hoặc <strong>Robust (1.0)</strong>.
          </span>
        </div>
      </div>

      {/* Gap Between Segments */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
            Khoảng nghỉ giữa các đoạn (Gap Silence)
          </label>
          <p className="text-[11px] text-slate-500 dark:text-slate-400">
            Khoảng im lặng chèn giữa 2 đoạn khi ghép file (tính theo giây, mẫu sample chính xác). Mặc định 0.4s.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="number"
            min="0"
            max="3"
            step="0.1"
            value={gapSeconds}
            onChange={(e) => onChangeGapSeconds(parseFloat(e.target.value) || 0)}
            className="w-24 text-xs font-semibold p-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-center focus:ring-2 focus:ring-brand-500 outline-none"
          />
          <span className="text-xs text-slate-500">giây</span>
        </div>
      </div>

      {/* Speaking Speed */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 space-y-2.5">
        <div className="flex items-center justify-between">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-amber-500" />
            <span>Tốc độ nói (Speaking Speed)</span>
            <span className="text-[10px] px-2 py-0.5 rounded bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-mono font-bold">
              {speed}x
            </span>
          </label>
          <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">
            {speed === 1.0 ? 'Tốc độ gốc' : speed > 1.0 ? `Nhanh hơn ${(Math.round((speed - 1.0) * 100))}%` : `Chậm hơn ${(Math.round((1.0 - speed) * 100))}%`}
          </span>
        </div>

        {/* Preset quick buttons */}
        <div className="grid grid-cols-5 gap-1.5">
          {[
            { val: 0.9, label: '0.9x', desc: 'Chậm' },
            { val: 1.0, label: '1.0x', desc: 'Chuẩn' },
            { val: 1.1, label: '1.1x', desc: 'Hơi nhanh' },
            { val: 1.15, label: '1.15x', desc: 'TikTok/Review' },
            { val: 1.25, label: '1.25x', desc: 'Nhanh' },
          ].map((item) => {
            const isSelected = Math.abs(speed - item.val) < 0.01;
            return (
              <button
                key={item.val}
                type="button"
                onClick={() => onChangeSpeed(item.val)}
                className={`py-1.5 px-1 rounded-xl text-center border transition ${
                  isSelected
                    ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/60 text-brand-700 dark:text-brand-300 font-bold ring-2 ring-brand-500/20'
                    : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/40 text-slate-600 dark:text-slate-400'
                }`}
              >
                <div className="text-xs font-bold leading-tight">{item.label}</div>
                <div className="text-[10px] text-slate-400 leading-tight mt-0.5 truncate">{item.desc}</div>
              </button>
            );
          })}
        </div>

        {/* Range slider & precise input */}
        <div className="flex items-center gap-3 pt-1">
          <input
            type="range"
            min="0.75"
            max="1.5"
            step="0.05"
            value={speed}
            onChange={(e) => onChangeSpeed(parseFloat(e.target.value))}
            className="flex-1 accent-brand-600 cursor-pointer"
          />
          <input
            type="number"
            min="0.5"
            max="2.0"
            step="0.05"
            value={speed}
            onChange={(e) => onChangeSpeed(parseFloat(e.target.value) || 1.0)}
            className="w-16 text-xs font-mono font-semibold p-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-center outline-none focus:ring-1 focus:ring-brand-500"
          />
          <span className="text-xs text-slate-500">x</span>
        </div>

        <p className="text-[11px] text-slate-500 dark:text-slate-400">
          Chỉnh tốc độ qua thuật toán FFmpeg (giữ nguyên cao độ và cảm xúc gốc), tự động co/dãn timeline chuẩn xác từng mili-giây cho CapCut.
        </p>
      </div>

      {/* Big Action CTA */}
      <div className="pt-3">
        <button
          type="button"
          onClick={onStartGenerate}
          disabled={disabled || isGenerating}
          className="w-full py-3.5 px-6 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-brand-600 via-indigo-600 to-brand-700 hover:from-brand-700 hover:to-brand-800 shadow-md shadow-brand-500/25 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition transform active:scale-[0.99]"
        >
          {isGenerating ? (
            <>
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span>Đang xử lý âm thanh & timeline...</span>
            </>
          ) : (
            <>
              <Mic className="w-5 h-5" />
              <span>Tạo Giọng Nói & Xuất File CapCut</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
