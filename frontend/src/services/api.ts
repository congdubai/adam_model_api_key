import { ApiKeyDto, KeyStats, JobDetail, JobCreateRequest, FfmpegStatus } from '../types';

const API_BASE = '/api';

export async function fetchKeys(): Promise<ApiKeyDto[]> {
  const res = await fetch(`${API_BASE}/keys`);
  if (!res.ok) throw new Error('Không thể tải danh sách API key');
  return res.json();
}

export async function fetchKeyStats(): Promise<KeyStats> {
  const res = await fetch(`${API_BASE}/keys/stats`);
  if (!res.ok) throw new Error('Không thể tải thống kê API key');
  return res.json();
}

export async function addKeys(keys: string): Promise<ApiKeyDto[]> {
  const res = await fetch(`${API_BASE}/keys`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ keys }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể thêm API key');
  }
  return res.json();
}

export async function deleteKey(id: string): Promise<boolean> {
  const res = await fetch(`${API_BASE}/keys/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error('Không thể xóa API key');
  const data = await res.json();
  return data.success;
}

export async function checkAllKeys(): Promise<ApiKeyDto[]> {
  const res = await fetch(`${API_BASE}/keys/check`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Không thể kiểm tra credit các API key');
  return res.json();
}

export async function checkSingleKey(id: string): Promise<ApiKeyDto> {
  const res = await fetch(`${API_BASE}/keys/${id}/check`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Không thể kiểm tra API key này');
  return res.json();
}

export async function createJob(req: JobCreateRequest): Promise<JobDetail> {
  const res = await fetch(`${API_BASE}/jobs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể khởi tạo tiến trình TTS');
  }
  return res.json();
}

export async function getJob(id: string): Promise<JobDetail> {
  const res = await fetch(`${API_BASE}/jobs/${id}`);
  if (!res.ok) throw new Error('Không thể lấy thông tin tiến trình ' + id);
  return res.json();
}

export async function stopJob(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/jobs/${id}/stop`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Không thể dừng tiến trình');
}

export async function resumeJob(id: string): Promise<JobDetail> {
  const res = await fetch(`${API_BASE}/jobs/${id}/resume`, {
    method: 'POST',
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể tiếp tục tiến trình');
  }
  return res.json();
}

export async function regenerateSegment(jobId: string, segmentIndex: number): Promise<JobDetail> {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/segments/${segmentIndex}/regenerate`, {
    method: 'POST',
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể đọc lại đoạn này');
  }
  return res.json();
}

export async function checkFfmpegStatus(): Promise<FfmpegStatus> {
  const res = await fetch(`${API_BASE}/system/ffmpeg-status`);
  if (!res.ok) throw new Error('Không thể kiểm tra FFmpeg');
  return res.json();
}

export function getDownloadUrl(jobId: string, format: 'wav' | 'mp3' | 'srt' | 'json' | 'csv' | 'zip'): string {
  return `${API_BASE}/jobs/${jobId}/download/${format}`;
}

export function getSegmentDownloadUrl(jobId: string, segmentIndex: number): string {
  return `${API_BASE}/jobs/${jobId}/segments/${segmentIndex}/download`;
}

export function getAudioStreamUrl(jobId: string, version?: string | number, format?: 'mp3' | 'wav'): string {
  const params = new URLSearchParams();
  if (version !== undefined && version !== null) {
    params.set('v', String(version));
  }
  if (format) {
    params.set('format', format);
  }
  const qs = params.toString();
  return `${API_BASE}/jobs/${jobId}/audio${qs ? `?${qs}` : ''}`;
}

// CapCut Direct Integration APIs
export async function detectCapCut(): Promise<import('../types').CapCutDetectResult> {
  const res = await fetch(`${API_BASE}/capcut/detect`);
  if (!res.ok) throw new Error('Không thể kiểm tra CapCut');
  return res.json();
}

export async function fetchCapCutProjects(): Promise<import('../types').CapCutProject[]> {
  const res = await fetch(`${API_BASE}/capcut/projects`);
  if (!res.ok) throw new Error('Không thể lấy danh sách project CapCut');
  return res.json();
}

export async function applyToCapCut(req: import('../types').CapCutApplyRequest): Promise<import('../types').CapCutApplyResponse> {
  const res = await fetch(`${API_BASE}/capcut/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Lỗi đồng bộ vào CapCut');
  }
  return res.json();
}

export async function openCapCutFolder(path: string): Promise<boolean> {
  const res = await fetch(`${API_BASE}/capcut/open-folder`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path }),
  });
  return res.ok;
}

