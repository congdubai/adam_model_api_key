package com.adamtts.studio.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobEntity {
    private String id;
    private JobStatus status;
    private String modelId;
    private String voiceId;
    private Double stability;
    private double gapSeconds;
    private Double speed = 1.0;
    private String splitMode;
    private int totalSegments;
    private int completedSegments;
    private int currentSegmentIndex;
    private String currentKeyMasked;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant finishedAt;
    private List<SegmentEntity> segments = new ArrayList<>();
    private String wavFilePath;
    private String mp3FilePath;
    private String srtFilePath;
    private String jsonFilePath;
    private String csvFilePath;

    public JobEntity() {
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

    public Double getSpeed() {
        return speed != null && speed > 0 ? speed : 1.0;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<SegmentEntity> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentEntity> segments) {
        this.segments = segments;
    }

    public String getWavFilePath() {
        return wavFilePath;
    }

    public void setWavFilePath(String wavFilePath) {
        this.wavFilePath = wavFilePath;
    }

    public String getMp3FilePath() {
        return mp3FilePath;
    }

    public void setMp3FilePath(String mp3FilePath) {
        this.mp3FilePath = mp3FilePath;
    }

    public String getSrtFilePath() {
        return srtFilePath;
    }

    public void setSrtFilePath(String srtFilePath) {
        this.srtFilePath = srtFilePath;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }
}

