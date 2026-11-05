package com.adamtts.studio.dto;

import com.adamtts.studio.model.JobStatus;
import java.time.Instant;

public class JobSummaryDto {
    private String id;
    private JobStatus status;
    private int totalSegments;
    private int completedSegments;
    private int currentSegmentIndex;
    private String currentKeyMasked;
    private String errorMessage;
    private Instant createdAt;
    private Instant finishedAt;
    private boolean audioReady;

    public JobSummaryDto() {
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
}

