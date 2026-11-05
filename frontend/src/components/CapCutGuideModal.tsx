import React from 'react';
import { X, CheckCircle2, Film, FileAudio, FileText, Sparkles } from 'lucide-react';

interface CapCutGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CapCutGuideModal: React.FC<CapCutGuideModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 rounded-lg">
              <Film className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base text-slate-900 dark:text-white">
                Hướng dẫn nhập Âm thanh & Phụ đề vào CapCut
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Chỉ 3 bước đơn giản để giọng đọc Adam và chữ chạy khớp từng khung hình
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Steps List */}
        <div className="space-y-4">
          {/* Step 1 */}
          <div className="flex items-start gap-3.5 p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800">
            <div className="w-7 h-7 rounded-full bg-brand-600 text-white flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
              1
            </div>
            <div className="space-y-1">
              <div className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FileAudio className="w-4 h-4 text-brand-500" />
                <span>Nhập file âm thanh WAV vào CapCut</span>
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                Tải file <strong>WAV</strong> từ Adam TTS Studio. Mở CapCut PC, vào mục <strong>Media (Phương tiện)</strong> → kéo thả file <code>adam_tts_*.wav</code> xuống thanh <strong>Timeline âm thanh (Audio track)</strong> ở vị trí giây <code>00:00</code>.
              </p>
            </div>
          </div>

          {/* Step 2 */}
          <div className="flex items-start gap-3.5 p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800">
            <div className="w-7 h-7 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
              2
            </div>
            <div className="space-y-1">
              <div className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FileText className="w-4 h-4 text-indigo-500" />
                <span>Nhập file phụ đề SRT (Văn bản → Phụ đề cục bộ)</span>
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                Tại thanh menu trên cùng của CapCut, chọn tab <strong>Văn bản (Text)</strong> → chọn <strong>Phụ đề cục bộ (Local captions)</strong> → bấm nút <strong>Nhập (Import)</strong> và chọn file <code>adam_tts_*.srt</code> vừa tải về. Sau đó kéo phụ đề vào Timeline.
              </p>
            </div>
          </div>

          {/* Step 3 */}
          <div className="flex items-start gap-3.5 p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800">
            <div className="w-7 h-7 rounded-full bg-emerald-600 text-white flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
              3
            </div>
            <div className="space-y-1">
              <div className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                <span>Hoàn tất & Tận hưởng thành quả</span>
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                Các đoạn phụ đề đã được loại bỏ hoàn toàn các thẻ cảm xúc như <code>[excited]</code>, <code>[whispers]</code> và thời gian bắt đầu - kết thúc được tính toán chuẩn theo mẫu sample 44.1kHz. Bạn có thể chọn hiệu ứng chữ, font chữ, hoạt ảnh (animation) trong CapCut theo ý muốn!
              </p>
            </div>
          </div>
        </div>

        {/* Tips Box */}
        <div className="p-3.5 rounded-xl bg-brand-50/70 dark:bg-brand-950/30 border border-brand-200/70 dark:border-brand-900/50 text-xs text-brand-900 dark:text-brand-300 space-y-1">
          <div className="font-bold flex items-center gap-1.5">
            <Sparkles className="w-4 h-4 text-brand-500" />
            <span>Mẹo tối ưu:</span>
          </div>
          <p className="leading-relaxed opacity-90">
            Nếu có một đoạn câu đọc chưa ưng ý, bạn không cần phải tạo lại cả bài! Chỉ cần bấm nút <strong>"Đọc lại"</strong> ngay tại đoạn đó ở bảng kết quả. Hệ thống sẽ tự động dùng key pool tạo lại đoạn đó và ghép lại toàn bộ audio + SRT cho bạn trong tích tắc.
          </p>
        </div>

        {/* Footer */}
        <div className="flex justify-end pt-2">
          <button
            onClick={onClose}
            className="px-5 py-2 text-xs font-semibold rounded-xl bg-slate-900 text-white dark:bg-white dark:text-slate-900 hover:opacity-90 transition"
          >
            Đã hiểu, đóng cửa sổ
          </button>
        </div>
      </div>
    </div>
  );
};
