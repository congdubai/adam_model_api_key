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

export function getDownloadUrl(jobId: string, format: 'wav' | 'mp3' | 'srt' | 'json' | 'csv'): string {
  return `${API_BASE}/jobs/${jobId}/download/${format}`;
}

export function getAudioStreamUrl(jobId: string): string {
  return `${API_BASE}/jobs/${jobId}/audio?t=${Date.now()}`;
}

