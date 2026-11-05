import React, { useRef } from 'react';
import { FileText, Sparkles } from 'lucide-react';

interface ScriptEditorProps {
  text: string;
  onChangeText: (text: string) => void;
  splitMode: 'LINE' | 'PARAGRAPH';
  onChangeSplitMode: (mode: 'LINE' | 'PARAGRAPH') => void;
  modelId: string;
}

const EMOTION_TAGS = [
  { tag: '[excited]', label: 'Hào hứng', color: 'hover:bg-amber-50 hover:text-amber-600 dark:hover:bg-amber-950/40' },
  { tag: '[whispers]', label: 'Thì thầm', color: 'hover:bg-purple-50 hover:text-purple-600 dark:hover:bg-purple-950/40' },
  { tag: '[sad]', label: 'Buồn bã', color: 'hover:bg-blue-50 hover:text-blue-600 dark:hover:bg-blue-950/40' },
  { tag: '[angry]', label: 'Tức giận', color: 'hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-950/40' },
  { tag: '[laughs]', label: 'Cười', color: 'hover:bg-emerald-50 hover:text-emerald-600 dark:hover:bg-emerald-950/40' },
  { tag: '[sighs]', label: 'Thở dài', color: 'hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800' },
  { tag: '[curious]', label: 'Tò mò', color: 'hover:bg-cyan-50 hover:text-cyan-600 dark:hover:bg-cyan-950/40' },
  { tag: '[short pause]', label: 'Nghỉ ngắn', color: 'hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-950/40' },
  { tag: '[long pause]', label: 'Nghỉ dài', color: 'hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-950/40' },
];

const SAMPLE_TEXT = `[excited] Chào mừng các bạn đã đến với Adam TTS Studio! 
Đây là công cụ chuyển văn bản thành giọng đọc chuyên nghiệp dành cho CapCut.

[whispers] Bạn có biết bí mật này không? Chúng tôi có thể tự động chuyển key khi hết credit.
[laughs] Quá tuyệt vời phải không nào? [short pause] 

Hãy dán kịch bản của bạn vào đây và cùng trải nghiệm chất lượng giọng đọc Eleven v3 đỉnh cao nhé!`;

const TIMELINE_SAMPLE_TEXT = `[0, 20] [excited] Đoạn mở đầu từ giây 0: Chào mừng các bạn đến với video! Hãy cùng theo dõi tình tiết gay cấn ngay sau đây.

[45, 60] [whispers] Đoạn tiếp theo bắt đầu chính xác ở giây 45: Toàn bộ khoảng từ giây 20 đến 45 sẽ là khoảng lặng cho cảnh hành động và nhạc nền trong CapCut.

[01:15, 01:30] Đoạn kết thúc: Cảm ơn các bạn đã theo dõi, hẹn gặp lại ở video tiếp theo nhé!`;

export const ScriptEditor: React.FC<ScriptEditorProps> = ({
  text,
  onChangeText,
  splitMode,
  onChangeSplitMode,
  modelId,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Insert tag at current cursor position
  const handleInsertTag = (tag: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const current = text;
    const newText = current.substring(0, start) + tag + ' ' + current.substring(end);
    onChangeText(newText);

    // Reposition cursor right after inserted tag + space
    setTimeout(() => {
      textarea.focus();
      const newPos = start + tag.length + 1;
      textarea.setSelectionRange(newPos, newPos);
    }, 10);
  };

  // Estimate number of segments
  const calculateSegmentsCount = () => {
    if (!text.trim()) return 0;
    if (splitMode === 'LINE') {
      return text.split(/\r?\n/).filter((l) => l.trim().length > 0).length;
    } else {
      return text.split(/(\r?\n\s*){2,}/).filter((p) => p.trim().length > 0).length;
    }
  };

  const charCount = text.length;
  const wordCount = text.trim() ? text.trim().split(/\s+/).length : 0;
  const segmentCount = calculateSegmentsCount();
  const maxLimit = modelId === 'eleven_v3' ? 4500 : 9000;

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-4">
      {/* Header & Modes */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400 rounded-lg">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-semibold text-base text-slate-900 dark:text-white">Soạn thảo Kịch bản & Tách đoạn</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Nhập nội dung lồng tiếng, chèn thẻ cảm xúc và chọn quy tắc tách đoạn.
            </p>
          </div>
        </div>

        {/* Split mode selector */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-xl text-xs">
          <button
            type="button"
            onClick={() => onChangeSplitMode('PARAGRAPH')}
            className={`px-3 py-1.5 rounded-lg font-medium transition ${
              splitMode === 'PARAGRAPH'
                ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Đoạn cách nhau dòng trống
          </button>
          <button
            type="button"
            onClick={() => onChangeSplitMode('LINE')}
            className={`px-3 py-1.5 rounded-lg font-medium transition ${
              splitMode === 'LINE'
                ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Mỗi dòng là 1 đoạn
          </button>
        </div>
      </div>

      {/* Emotion Tag Bar */}
      <div className="space-y-1.5">
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500 dark:text-slate-400">
          <span className="flex items-center gap-1 font-medium">
            <Sparkles className="w-3.5 h-3.5 text-brand-500" />
            Thẻ cảm xúc & Mốc thời gian (bấm để chèn tại con trỏ):
          </span>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => onChangeText(SAMPLE_TEXT)}
              className="text-[11px] text-brand-600 dark:text-brand-400 hover:underline font-medium"
            >
              Mẫu cảm xúc
            </button>
            <span className="text-slate-300 dark:text-slate-700">•</span>
            <button
              type="button"
              onClick={() => onChangeText(TIMELINE_SAMPLE_TEXT)}
              className="text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline font-semibold"
              title="Kịch bản có mốc thời gian [0, 20], [45, 60] để khớp cảnh CapCut"
            >
              🎬 Mẫu mốc thời gian CapCut [0, 20]
            </button>
          </div>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {/* Custom Timeline Tag Button */}
          <button
            type="button"
            onClick={() => handleInsertTag('[0, 20]')}
            className="px-2.5 py-1 text-xs rounded-lg border border-indigo-300 dark:border-indigo-800 bg-indigo-50/70 dark:bg-indigo-950/40 text-indigo-800 dark:text-indigo-300 font-mono transition hover:bg-indigo-100"
            title="Chèn mốc thời gian bắt đầu và kết thúc phân cảnh"
          >
            <span className="font-bold text-indigo-600 dark:text-indigo-400 mr-1">[0, 20]</span>
            <span className="text-[10px] text-slate-500">(Mốc giây)</span>
          </button>

          {EMOTION_TAGS.map((t) => (
            <button
              key={t.tag}
              type="button"
              onClick={() => handleInsertTag(t.tag)}
              className={`px-2.5 py-1 text-xs rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-800/40 text-slate-700 dark:text-slate-300 font-mono transition ${t.color}`}
            >
              <span className="font-semibold text-brand-600 dark:text-brand-400 mr-1">{t.tag}</span>
              <span className="text-[10px] text-slate-400">({t.label})</span>
            </button>
          ))}
        </div>
      </div>

      {/* Timeline Hint Box */}
      <div className="text-[11px] bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800/80 p-2.5 rounded-xl text-slate-600 dark:text-slate-400 flex items-start gap-2">
        <span className="font-bold text-indigo-600 dark:text-indigo-400 shrink-0">🎬 Mốc CapCut:</span>
        <span>
          Ghi <code className="px-1 py-0.5 rounded bg-slate-200 dark:bg-slate-700 font-mono text-[10px]">[0, 20]</code>, <code className="px-1 py-0.5 rounded bg-slate-200 dark:bg-slate-700 font-mono text-[10px]">[45, 60]</code> hoặc <code className="px-1 py-0.5 rounded bg-slate-200 dark:bg-slate-700 font-mono text-[10px]">[01:15]</code> ở đầu đoạn. Tool sẽ <strong>tự động chèn khoảng lặng</strong> giữa các đoạn để audio khớp chính xác từng phân cảnh video CapCut!
        </span>
      </div>

      {/* Textarea */}
      <div className="relative">
        <textarea
          ref={textareaRef}
          value={text}
          onChange={(e) => onChangeText(e.target.value)}
          placeholder="Nhập nội dung kịch bản cần lồng tiếng vào đây..."
          rows={9}
          className="w-full text-sm leading-relaxed p-4 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500 outline-none resize-y transition shadow-xs"
        />
      </div>

      {/* Realtime stats footer */}
      <div className="flex flex-wrap items-center justify-between text-xs text-slate-500 dark:text-slate-400 pt-1 border-t border-slate-100 dark:border-slate-800/60">
        <div className="flex items-center gap-4">
          <div>
            Số ký tự:{' '}
            <span className="font-semibold text-slate-900 dark:text-white">
              {charCount.toLocaleString('vi-VN')}
            </span>
          </div>
          <div>
            Số từ:{' '}
            <span className="font-semibold text-slate-900 dark:text-white">
              {wordCount.toLocaleString('vi-VN')}
            </span>
          </div>
          <div>
            Số đoạn ước tính:{' '}
            <span className="font-semibold text-brand-600 dark:text-brand-400">
              {segmentCount} đoạn
            </span>
          </div>
        </div>

        <div className="text-[11px] text-slate-400">
          Đoạn &gt; {maxLimit.toLocaleString('vi-VN')} ký tự sẽ tự động cắt nhỏ theo dấu câu (. ! ? …)
        </div>
      </div>
    </div>
  );
};
