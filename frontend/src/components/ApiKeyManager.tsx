import React, { useState } from 'react';
import { Key, Plus, RefreshCw, Trash2, AlertCircle, Check } from 'lucide-react';
import { ApiKeyDto, KeyStats } from '../types';

interface ApiKeyManagerProps {
  stats: KeyStats | null;
  keys: ApiKeyDto[];
  loading: boolean;
  onAddKeys: (keys: string) => Promise<void>;
  onDeleteKey: (id: string) => Promise<void>;
  onCheckAllKeys: () => Promise<void>;
  onCheckSingleKey: (id: string) => Promise<void>;
}

export const ApiKeyManager: React.FC<ApiKeyManagerProps> = ({
  stats,
  keys,
  loading,
  onAddKeys,
  onDeleteKey,
  onCheckAllKeys,
  onCheckSingleKey,
}) => {
  const [showAddBox, setShowAddBox] = useState(false);
  const [rawInput, setRawInput] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [checkingId, setCheckingId] = useState<string | null>(null);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rawInput.trim()) return;
    try {
      setSubmitting(true);
      await onAddKeys(rawInput);
      setRawInput('');
      setShowAddBox(false);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCheckOne = async (id: string) => {
    try {
      setCheckingId(id);
      await onCheckSingleKey(id);
    } finally {
      setCheckingId(null);
    }
  };

  const formatNumber = (num: number | null | undefined) => {
    if (num === null || num === undefined) return '—';
    return num.toLocaleString('vi-VN');
  };

  const getStatusBadge = (status: ApiKeyDto['status'], errorMsg: string | null) => {
    switch (status) {
      case 'OK':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            <Check className="w-3 h-3" /> Sẵn sàng
          </span>
        );
      case 'EXHAUSTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
            <AlertCircle className="w-3 h-3" /> Hết credit
          </span>
        );
      case 'ERROR':
        return (
          <span
            title={errorMsg || 'Lỗi key'}
            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300 cursor-help"
          >
            <AlertCircle className="w-3 h-3" /> Lỗi key
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
            Chưa kiểm tra
          </span>
        );
    }
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-4">
      {/* Header & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-brand-50 dark:bg-brand-950/50 text-brand-600 dark:text-brand-400 rounded-lg">
            <Key className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-semibold text-base text-slate-900 dark:text-white flex items-center gap-2">
              Quản lý Pool API Key
              <span className="text-xs px-2 py-0.5 font-normal rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                {keys.length} keys
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Tự động luân chuyển khi hết credit. Key được che an toàn (4 đầu + 4 cuối).
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowAddBox(!showAddBox)}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-brand-600 hover:bg-brand-700 text-white transition shadow-sm"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Thêm Key</span>
          </button>
          <button
            onClick={onCheckAllKeys}
            disabled={loading || keys.length === 0}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition disabled:opacity-50"
            title="Kiểm tra hạn mức & credit còn lại từ ElevenLabs"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Kiểm tra credit</span>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800">
          <div className="text-xs text-slate-500 dark:text-slate-400">Tổng credit còn lại</div>
          <div className="text-xl font-bold text-brand-600 dark:text-brand-400 mt-1">
            {formatNumber(stats?.totalRemainingCredits)}
            <span className="text-xs font-normal text-slate-400 ml-1">ký tự</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800">
          <div className="text-xs text-slate-500 dark:text-slate-400">Đã dùng trong phiên này</div>
          <div className="text-xl font-bold text-slate-900 dark:text-white mt-1">
            {formatNumber(stats?.totalUsedInSession)}
            <span className="text-xs font-normal text-slate-400 ml-1">ký tự</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800">
          <div className="text-xs text-slate-500 dark:text-slate-400">Key khả dụng / Tổng số</div>
          <div className="text-xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
            {stats?.validKeys ?? 0}
            <span className="text-sm font-normal text-slate-400"> / {stats?.totalKeys ?? 0} keys</span>
          </div>
        </div>
      </div>

      {/* Add Box Modal / Collapsible */}
      {showAddBox && (
        <form onSubmit={handleAdd} className="p-4 rounded-xl border border-brand-200 dark:border-brand-900/60 bg-brand-50/40 dark:bg-brand-950/20 space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
              Dán danh sách API Key (mỗi key một dòng):
            </label>
            <span className="text-[11px] text-slate-500 dark:text-slate-400">
              Key được mã hoá lưu ở backend, không bao giờ lộ ra ngoài
            </span>
          </div>
          <textarea
            value={rawInput}
            onChange={(e) => setRawInput(e.target.value)}
            placeholder="xi-api-key-1&#10;xi-api-key-2&#10;xi-api-key-3"
            rows={4}
            className="w-full text-xs font-mono p-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500 outline-none"
          />
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowAddBox(false)}
              className="px-3 py-1.5 text-xs text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting || !rawInput.trim()}
              className="px-4 py-1.5 text-xs font-medium rounded-lg bg-brand-600 text-white hover:bg-brand-700 disabled:opacity-50 transition"
            >
              {submitting ? 'Đang thêm...' : 'Lưu vào Pool'}
            </button>
          </div>
        </form>
      )}

      {/* Keys Table */}
      {keys.length === 0 ? (
        <div className="text-center py-8 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-xl">
          <Key className="w-8 h-8 text-slate-300 dark:text-slate-700 mx-auto mb-2" />
          <p className="text-sm text-slate-600 dark:text-slate-400 font-medium">Chưa có API key nào trong Pool</p>
          <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
            Bấm nút <span className="font-semibold text-brand-600">"Thêm Key"</span> để dán một hoặc nhiều ElevenLabs API key.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
          <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-800 text-left text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400">
              <tr>
                <th className="py-2.5 px-3 font-medium">STT</th>
                <th className="py-2.5 px-3 font-medium">API Key (Đã che)</th>
                <th className="py-2.5 px-3 font-medium">Trạng thái</th>
                <th className="py-2.5 px-3 font-medium">Credit còn lại / Hạn mức</th>
                <th className="py-2.5 px-3 font-medium">Gói (Tier)</th>
                <th className="py-2.5 px-3 font-medium">Đã dùng (Phiên này)</th>
                <th className="py-2.5 px-3 font-medium text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800 text-slate-700 dark:text-slate-300">
              {keys.map((k, idx) => {
                const percent =
                  k.characterLimit && k.remainingCredits !== null
                    ? Math.min(100, Math.max(0, (k.remainingCredits / k.characterLimit) * 100))
                    : null;

                return (
                  <tr key={k.id} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition">
                    <td className="py-2.5 px-3 text-slate-400">{idx + 1}</td>
                    <td className="py-2.5 px-3 font-mono font-medium text-slate-900 dark:text-white">
                      {k.maskedKey}
                    </td>
                    <td className="py-2.5 px-3">{getStatusBadge(k.status, k.lastErrorMessage)}</td>
                    <td className="py-2.5 px-3 min-w-[180px]">
                      {k.remainingCredits !== null ? (
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-[11px]">
                            <span className="font-semibold text-slate-900 dark:text-white">
                              {formatNumber(k.remainingCredits)}
                            </span>
                            <span className="text-slate-400">/ {formatNumber(k.characterLimit)}</span>
                          </div>
                          {percent !== null && (
                            <div className="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all ${
                                  percent > 30 ? 'bg-emerald-500' : percent > 10 ? 'bg-amber-500' : 'bg-rose-500'
                                }`}
                                style={{ width: `${percent}%` }}
                              />
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400 italic text-[11px]">
                          {k.lastErrorMessage?.includes('thiếu quyền')
                            ? 'Không đọc được credit (vẫn tạo giọng tốt)'
                            : 'Chưa kiểm tra'}
                        </span>
                      )}
                    </td>
                    <td className="py-2.5 px-3 capitalize text-slate-600 dark:text-slate-400">
                      {k.tier || '—'}
                    </td>
                    <td className="py-2.5 px-3 font-medium text-slate-900 dark:text-white">
                      {formatNumber(k.usedInSession)}
                    </td>
                    <td className="py-2.5 px-3 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          onClick={() => handleCheckOne(k.id)}
                          disabled={checkingId === k.id}
                          title="Kiểm tra lại credit key này"
                          className="p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 hover:text-slate-900 dark:hover:text-white transition disabled:opacity-40"
                        >
                          <RefreshCw className={`w-3.5 h-3.5 ${checkingId === k.id ? 'animate-spin' : ''}`} />
                        </button>
                        <button
                          onClick={() => onDeleteKey(k.id)}
                          title="Xóa key khỏi pool"
                          className="p-1 rounded hover:bg-rose-50 dark:hover:bg-rose-950/50 text-slate-400 hover:text-rose-600 transition"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
