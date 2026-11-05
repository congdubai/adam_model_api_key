package com.adamtts.studio.service;

import com.adamtts.studio.dto.*;
import com.adamtts.studio.exception.NoAvailableKeyException;
import com.adamtts.studio.model.JobEntity;
import com.adamtts.studio.model.JobStatus;
import com.adamtts.studio.model.SegmentEntity;
import com.adamtts.studio.model.SegmentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class JobManagerService {

    private static final Logger log = LoggerFactory.getLogger(JobManagerService.class);

    private final ObjectMapper objectMapper;
    private final TextSplittingService textSplittingService;
    private final ApiKeyService apiKeyService;
    private final AudioProcessingService audioProcessingService;
    private final TimelineService timelineService;
    private final File jobsDir;

    private final Map<String, JobEntity> activeJobs = new ConcurrentHashMap<>();
    private final Set<String> stopRequestedJobs = ConcurrentHashMap.newKeySet();
    private final ExecutorService jobExecutor = Executors.newFixedThreadPool(4);

    public JobManagerService(ObjectMapper objectMapper,
                             TextSplittingService textSplittingService,
                             ApiKeyService apiKeyService,
                             AudioProcessingService audioProcessingService,
                             TimelineService timelineService,
                             @Value("${app.data-dir:./data}") String dataDir) {
        this.objectMapper = objectMapper;
        this.textSplittingService = textSplittingService;
        this.apiKeyService = apiKeyService;
        this.audioProcessingService = audioProcessingService;
        this.timelineService = timelineService;
        this.jobsDir = new File(dataDir, "jobs");
        if (!jobsDir.exists()) {
            jobsDir.mkdirs();
        }
    }

    @PostConstruct
    public void init() {
        // Load recent jobs from disk
        File[] jobFolders = jobsDir.listFiles(File::isDirectory);
        if (jobFolders != null) {
            for (File folder : jobFolders) {
                File jobFile = new File(folder, "job.json");
                if (jobFile.exists()) {
                    try {
                        JobEntity entity = objectMapper.readValue(jobFile, JobEntity.class);
                        activeJobs.put(entity.getId(), entity);
                    } catch (Exception e) {
                        log.warn("Could not load existing job {}: {}", folder.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    public synchronized JobDetailDto createJob(JobCreateRequest req) {
        audioProcessingService.requireFfmpeg();

        String jobId = UUID.randomUUID().toString();
        List<String> rawSegments = textSplittingService.splitText(req.getText(), req.getSplitMode(), req.getModelId());

        if (rawSegments.isEmpty()) {
            throw new IllegalArgumentException("Văn bản không có nội dung để tạo giọng");
        }

        JobEntity job = new JobEntity();
        job.setId(jobId);
        job.setStatus(JobStatus.PENDING);
        job.setModelId(req.getModelId());
        job.setVoiceId(req.getVoiceId());
        job.setStability(req.getStability());
        job.setGapSeconds(req.getGapSeconds());
        job.setSplitMode(req.getSplitMode());
        job.setTotalSegments(rawSegments.size());
        job.setCompletedSegments(0);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        List<SegmentEntity> segmentEntities = new ArrayList<>();
        for (int i = 0; i < rawSegments.size(); i++) {
            String rawText = rawSegments.get(i);
            TextSplittingService.ParsedSegment parsed = textSplittingService.parseSegmentTiming(rawText);
            String spokenText = parsed.spokenText();
            String cleanText = textSplittingService.stripEmotionTags(spokenText);
            String hash = audioProcessingService.calculateHash(job.getModelId(), job.getVoiceId(), job.getStability(), spokenText);

            SegmentEntity seg = new SegmentEntity(i + 1, spokenText, cleanText, hash);
            seg.setTargetStartTime(parsed.targetStartTime());
            seg.setTargetEndTime(parsed.targetEndTime());
            segmentEntities.add(seg);
        }
        job.setSegments(segmentEntities);

        activeJobs.put(jobId, job);
        saveJob(job);

        // Run asynchronously
        jobExecutor.submit(() -> runJob(jobId));

        return toDetailDto(job);
    }

    public void stopJob(String jobId) {
        JobEntity job = activeJobs.get(jobId);
        if (job != null && (job.getStatus() == JobStatus.RUNNING || job.getStatus() == JobStatus.PENDING)) {
            stopRequestedJobs.add(jobId);
            job.setStatus(JobStatus.STOPPED);
            job.setUpdatedAt(Instant.now());
            job.setErrorMessage("Người dùng đã dừng tiến trình.");
            saveJob(job);
        }
    }

    public synchronized JobDetailDto resumeJob(String jobId) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Không tìm thấy job: " + jobId);
        }

        stopRequestedJobs.remove(jobId);
        job.setStatus(JobStatus.PENDING);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());
        saveJob(job);

        jobExecutor.submit(() -> runJob(jobId));
        return toDetailDto(job);
    }

    private void runJob(String jobId) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) return;

        job.setStatus(JobStatus.RUNNING);
        job.setUpdatedAt(Instant.now());
        saveJob(job);

        File jobDir = new File(jobsDir, jobId);
        if (!jobDir.exists()) {
            jobDir.mkdirs();
        }

        try {
            List<SegmentEntity> segments = job.getSegments();

            for (int i = 0; i < segments.size(); i++) {
                if (stopRequestedJobs.contains(jobId)) {
                    job.setStatus(JobStatus.STOPPED);
                    job.setUpdatedAt(Instant.now());
                    saveJob(job);
                    return;
                }

                SegmentEntity seg = segments.get(i);
                if (seg.getStatus() == SegmentStatus.COMPLETED) {
                    continue; // Skip already completed
                }

                job.setCurrentSegmentIndex(i + 1);
                seg.setStatus(SegmentStatus.IN_PROGRESS);
                saveJob(job);

                File cachedFile = audioProcessingService.getCachedAudio(seg.getAudioHash());
                if (cachedFile != null) {
                    // Use from cache
                    log.info("Segment {}/{} found in cache ({})", i + 1, segments.size(), seg.getAudioHash());
                    seg.setFromCache(true);
                    seg.setKeyMasked("Từ cache");
                    seg.setAudioFilePath(cachedFile.getAbsolutePath());
                    double duration = audioProcessingService.getAudioDuration(cachedFile);
                    seg.setDurationSeconds(duration);
                    seg.setStatus(SegmentStatus.COMPLETED);
                    seg.setErrorMessage(null);

                } else {
                    // Synthesize using key pool with rotation
                    log.info("Synthesizing segment {}/{} (length: {})", i + 1, segments.size(), seg.getText().length());
                    ApiKeyService.TtsResult ttsResult = apiKeyService.executeTtsWithKeySwitching(
                            job.getVoiceId(), seg.getText(), job.getModelId(), job.getStability()
                    );

                    File savedFile = audioProcessingService.saveToCache(seg.getAudioHash(), ttsResult.audioData());
                    seg.setFromCache(false);
                    seg.setKeyIdUsed(ttsResult.keyId());
                    seg.setKeyMasked(ttsResult.keyMasked());
                    seg.setAudioFilePath(savedFile.getAbsolutePath());
                    job.setCurrentKeyMasked(ttsResult.keyMasked());

                    double duration = audioProcessingService.getAudioDuration(savedFile);
                    seg.setDurationSeconds(duration);
                    seg.setStatus(SegmentStatus.COMPLETED);
                    seg.setErrorMessage(null);
                }

                int completed = (int) segments.stream().filter(s -> s.getStatus() == SegmentStatus.COMPLETED).count();
                job.setCompletedSegments(completed);
                saveJob(job);
            }

            // Check if all segments are completed
            boolean allDone = segments.stream().allMatch(s -> s.getStatus() == SegmentStatus.COMPLETED);
            if (allDone) {
                // Calculate sample-accurate timeline
                timelineService.calculateTimestamps(segments, job.getGapSeconds());

                // Prepare segment files for merging
                List<File> audioFiles = new ArrayList<>();
                for (SegmentEntity seg : segments) {
                    audioFiles.add(new File(seg.getAudioFilePath()));
                }

                File workDir = new File(jobDir, "work");
                File outputWav = new File(jobDir, "output.wav");
                File outputMp3 = new File(jobDir, "output.mp3");
                File outputSrt = new File(jobDir, "output.srt");
                File outputJson = new File(jobDir, "timeline.json");
                File outputCsv = new File(jobDir, "timeline.csv");

                // Merge audio with exact timeline spacing (supports [0, 20], [45, 60] time codes)
                audioProcessingService.mergeSegmentsWithExactTiming(segments, workDir, outputWav, outputMp3);

                // Export SRT, JSON, CSV
                timelineService.writeSrtFile(segments, outputSrt);
                timelineService.writeTimelineJsonFile(segments, outputJson);
                timelineService.writeTimelineCsvFile(segments, outputCsv);

                job.setWavFilePath(outputWav.getAbsolutePath());
                job.setMp3FilePath(outputMp3.getAbsolutePath());
                job.setSrtFilePath(outputSrt.getAbsolutePath());
                job.setJsonFilePath(outputJson.getAbsolutePath());
                job.setCsvFilePath(outputCsv.getAbsolutePath());
                job.setStatus(JobStatus.COMPLETED);
                job.setFinishedAt(Instant.now());
                job.setUpdatedAt(Instant.now());
                saveJob(job);
                log.info("Job {} completed successfully! WAV, MP3, SRT and timeline exported.", jobId);
            }

        } catch (NoAvailableKeyException e) {
            log.warn("Job {} paused: {}", jobId, e.getMessage());
            job.setStatus(JobStatus.PAUSED_NO_KEYS);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(Instant.now());
            saveJob(job);
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Lỗi: " + e.getMessage());
            job.setUpdatedAt(Instant.now());
            saveJob(job);
        }
    }

    public synchronized JobDetailDto regenerateSegment(String jobId, int segmentIndex) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Không tìm thấy job: " + jobId);
        }

        if (segmentIndex < 1 || segmentIndex > job.getSegments().size()) {
            throw new IllegalArgumentException("Chỉ mục đoạn không hợp lệ: " + segmentIndex);
        }

        SegmentEntity seg = job.getSegments().get(segmentIndex - 1);
        // Invalidate cache for this segment
        audioProcessingService.deleteFromCache(seg.getAudioHash());

        // Re-synthesize
        ApiKeyService.TtsResult result = apiKeyService.executeTtsWithKeySwitching(
                job.getVoiceId(), seg.getText(), job.getModelId(), job.getStability()
        );

        try {
            File saved = audioProcessingService.saveToCache(seg.getAudioHash(), result.audioData());
            seg.setFromCache(false);
            seg.setKeyIdUsed(result.keyId());
            seg.setKeyMasked(result.keyMasked());
            seg.setAudioFilePath(saved.getAbsolutePath());
            double duration = audioProcessingService.getAudioDuration(saved);
            seg.setDurationSeconds(duration);
            seg.setStatus(SegmentStatus.COMPLETED);
            seg.setErrorMessage(null);

            // Recompute timeline for all segments
            timelineService.calculateTimestamps(job.getSegments(), job.getGapSeconds());

            // Re-merge entire audio file
            File jobDir = new File(jobsDir, jobId);
            File workDir = new File(jobDir, "work");
            File outputWav = new File(jobDir, "output.wav");
            File outputMp3 = new File(jobDir, "output.mp3");
            File outputSrt = new File(jobDir, "output.srt");
            File outputJson = new File(jobDir, "timeline.json");
            File outputCsv = new File(jobDir, "timeline.csv");

            audioProcessingService.mergeSegmentsWithExactTiming(job.getSegments(), workDir, outputWav, outputMp3);
            timelineService.writeSrtFile(job.getSegments(), outputSrt);
            timelineService.writeTimelineJsonFile(job.getSegments(), outputJson);
            timelineService.writeTimelineCsvFile(job.getSegments(), outputCsv);

            job.setUpdatedAt(Instant.now());
            saveJob(job);

            return toDetailDto(job);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc lại đoạn: " + e.getMessage(), e);
        }
    }

    public JobDetailDto getJobDetail(String jobId) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) {
            return null;
        }
        return toDetailDto(job);
    }

    public JobSummaryDto getJobSummary(String jobId) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) {
            return null;
        }
        return toSummaryDto(job);
    }

    public File getOutputFile(String jobId, String format) {
        JobEntity job = activeJobs.get(jobId);
        if (job == null) {
            return null;
        }

        File jobDir = new File(jobsDir, jobId);
        return switch (format.toLowerCase()) {
            case "wav" -> new File(jobDir, "output.wav");
            case "mp3" -> new File(jobDir, "output.mp3");
            case "srt" -> new File(jobDir, "output.srt");
            case "json" -> new File(jobDir, "timeline.json");
            case "csv" -> new File(jobDir, "timeline.csv");
            default -> null;
        };
    }

    private synchronized void saveJob(JobEntity job) {
        File jobDir = new File(jobsDir, job.getId());
        if (!jobDir.exists()) {
            jobDir.mkdirs();
        }
        File jobFile = new File(jobDir, "job.json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jobFile, job);
        } catch (IOException e) {
            log.error("Failed to save job {}", job.getId(), e);
        }
    }

    private JobSummaryDto toSummaryDto(JobEntity job) {
        JobSummaryDto dto = new JobSummaryDto();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus());
        dto.setTotalSegments(job.getTotalSegments());
        dto.setCompletedSegments(job.getCompletedSegments());
        dto.setCurrentSegmentIndex(job.getCurrentSegmentIndex());
        dto.setCurrentKeyMasked(job.getCurrentKeyMasked());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setFinishedAt(job.getFinishedAt());
        dto.setAudioReady(job.getStatus() == JobStatus.COMPLETED);
        return dto;
    }

    private JobDetailDto toDetailDto(JobEntity job) {
        JobDetailDto dto = new JobDetailDto();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus());
        dto.setModelId(job.getModelId());
        dto.setVoiceId(job.getVoiceId());
        dto.setStability(job.getStability());
        dto.setGapSeconds(job.getGapSeconds());
        dto.setSplitMode(job.getSplitMode());
        dto.setTotalSegments(job.getTotalSegments());
        dto.setCompletedSegments(job.getCompletedSegments());
        dto.setCurrentSegmentIndex(job.getCurrentSegmentIndex());
        dto.setCurrentKeyMasked(job.getCurrentKeyMasked());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setFinishedAt(job.getFinishedAt());
        dto.setAudioReady(job.getStatus() == JobStatus.COMPLETED);

        List<SegmentDto> segmentDtos = new ArrayList<>();
        if (job.getSegments() != null) {
            for (SegmentEntity s : job.getSegments()) {
                SegmentDto sDto = new SegmentDto(
                        s.getIndex(),
                        s.getText(),
                        s.getCleanText(),
                        s.getStatus(),
                        s.getDurationSeconds(),
                        s.getStartTimeSeconds(),
                        s.getEndTimeSeconds(),
                        s.getKeyMasked(),
                        s.isFromCache(),
                        s.getErrorMessage()
                );
                sDto.setTargetStartTime(s.getTargetStartTime());
                sDto.setTargetEndTime(s.getTargetEndTime());
                segmentDtos.add(sDto);
            }
        }
        dto.setSegments(segmentDtos);
        return dto;
    }
}

