export type ApiKeyStatus = 'OK' | 'EXHAUSTED' | 'ERROR' | 'UNCHECKED';

export interface ApiKeyDto {
  id: string;
  maskedKey: string;
  status: ApiKeyStatus;
  remainingCredits: number | null;
  characterLimit: number | null;
  tier: string | null;
  usedInSession: number;
  lastErrorMessage: string | null;
  lastCheckedAt: string | null;
}

export interface KeyStats {
  totalKeys: number;
  validKeys: number;
  totalRemainingCredits: number;
  totalUsedInSession: number;
  keys: ApiKeyDto[];
}

export type JobStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'STOPPED'
  | 'PAUSED_NO_KEYS'
  | 'FAILED';

export type SegmentStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'FAILED';

export interface SegmentDto {
  index: number;
  text: string;
  cleanText: string;
  status: SegmentStatus;
  durationSeconds: number;
  startTimeSeconds: number;
  endTimeSeconds: number;
  targetStartTime: number | null;
  targetEndTime: number | null;
  keyMasked: string | null;
  fromCache: boolean;
  errorMessage: string | null;
}

export interface JobDetail {
  id: string;
  status: JobStatus;
  modelId: string;
  voiceId: string;
  stability: number;
  gapSeconds: number;
  speed?: number;
  splitMode: 'LINE' | 'PARAGRAPH';
  totalSegments: number;
  completedSegments: number;
  currentSegmentIndex: number;
  currentKeyMasked: string | null;
  errorMessage: string | null;
  createdAt: string;
  finishedAt: string | null;
  audioReady: boolean;
  segments: SegmentDto[];
}

export interface JobCreateRequest {
  text: string;
  splitMode: 'LINE' | 'PARAGRAPH';
  modelId: string;
  stability: number;
  voiceId: string;
  gapSeconds: number;
  speed?: number;
}

export interface FfmpegStatus {
  available: boolean;
  message: string;
}

export interface CapCutDetectResult {
  available: boolean;
  capcutDir: string;
  isRunning: boolean;
}

export interface CapCutProject {
  id: string;
  name: string;
  folderPath: string;
  clipCount: number;
  readable: boolean;
  modifiedTime: number;
  durationSeconds: number;
}

export interface CapCutApplyRequest {
  jobId: string;
  projectFolder: string;
  syncVideoClips: boolean;
  addAudioTrack: boolean;
  addSubtitleTrack: boolean;
  clearCache: boolean;
  subtitleStyle?: 'tiktok_black_bar' | 'classic_shadow';
  subtitleAlignment?: number;
  subtitleSize?: number;
}

export interface CapCutApplyResponse {
  success: boolean;
  message: string;
  projectFolder: string;
  syncedClips: number;
  addedSubtitles: number;
  audioAdded: boolean;
  capcutWasRunning: boolean;
}

