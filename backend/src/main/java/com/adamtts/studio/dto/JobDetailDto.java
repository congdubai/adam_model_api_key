package com.adamtts.studio.dto;

import com.adamtts.studio.model.JobStatus;
import java.time.Instant;
import java.util.List;

public class JobDetailDto {
    private String id;
    private JobStatus status;
    private String modelId;
    private String voiceId;
    private Double stability;
    private double gapSeconds;
    private String splitMode;
    private int totalSegments;
    private int completedSegments;
    private int currentSegmentIndex;
    private String currentKeyMasked;
    private String errorMessage;
    private Instant createdAt;
    private Instant finishedAt;
    private boolean audioReady;
    private List<SegmentDto> segments;

    public JobDetailDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public Double getStability() {
        return stability;
    }

    public void setStability(Double stability) {
        this.stability = stability;
    }

    public double getGapSeconds() {
        return gapSeconds;
    }

    public void setGapSeconds(double gapSeconds) {
        this.gapSeconds = gapSeconds;
    }

    public String getSplitMode() {
        return splitMode;
    }

    public void setSplitMode(String splitMode) {
        this.splitMode = splitMode;
    }

    public int getTotalSegments() {
        return totalSegments;
    }

    public void setTotalSegments(int totalSegments) {
        this.totalSegments = totalSegments;
    }

    public int getCompletedSegments() {
        return completedSegments;
    }

    public void setCompletedSegments(int completedSegments) {
        this.completedSegments = completedSegments;
    }

    public int getCurrentSegmentIndex() {
        return currentSegmentIndex;
    }

    public void setCurrentSegmentIndex(int currentSegmentIndex) {
        this.currentSegmentIndex = currentSegmentIndex;
    }

    public String getCurrentKeyMasked() {
        return currentKeyMasked;
    }

    public void setCurrentKeyMasked(String currentKeyMasked) {
        this.currentKeyMasked = currentKeyMasked;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public boolean isAudioReady() {
        return audioReady;
    }

    public void setAudioReady(boolean audioReady) {
        this.audioReady = audioReady;
    }

    public List<SegmentDto> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentDto> segments) {
        this.segments = segments;
    }
}

