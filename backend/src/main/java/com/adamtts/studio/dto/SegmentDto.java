package com.adamtts.studio.dto;

import com.adamtts.studio.model.SegmentStatus;

public class SegmentDto {
    private int index;
    private String text;
    private String cleanText;
    private SegmentStatus status;
    private double durationSeconds;
    private double startTimeSeconds;
    private double endTimeSeconds;
    private String keyMasked;
    private boolean fromCache;
    private String errorMessage;

    public SegmentDto() {
    }

    public SegmentDto(int index, String text, String cleanText, SegmentStatus status,
                      double durationSeconds, double startTimeSeconds, double endTimeSeconds,
                      String keyMasked, boolean fromCache, String errorMessage) {
        this.index = index;
        this.text = text;
        this.cleanText = cleanText;
        this.status = status;
        this.durationSeconds = durationSeconds;
        this.startTimeSeconds = startTimeSeconds;
        this.endTimeSeconds = endTimeSeconds;
        this.keyMasked = keyMasked;
        this.fromCache = fromCache;
        this.errorMessage = errorMessage;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCleanText() {
        return cleanText;
    }

    public void setCleanText(String cleanText) {
        this.cleanText = cleanText;
    }

    public SegmentStatus getStatus() {
        return status;
    }

    public void setStatus(SegmentStatus status) {
        this.status = status;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public double getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public void setStartTimeSeconds(double startTimeSeconds) {
        this.startTimeSeconds = startTimeSeconds;
    }

    public double getEndTimeSeconds() {
        return endTimeSeconds;
    }

    public void setEndTimeSeconds(double endTimeSeconds) {
        this.endTimeSeconds = endTimeSeconds;
    }

    public String getKeyMasked() {
        return keyMasked;
    }

    public void setKeyMasked(String keyMasked) {
        this.keyMasked = keyMasked;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

