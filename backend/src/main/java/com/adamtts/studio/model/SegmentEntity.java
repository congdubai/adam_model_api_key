package com.adamtts.studio.model;

public class SegmentEntity {
    private int index;
    private String text;
    private String cleanText;
    private String audioHash;
    private SegmentStatus status;
    private double durationSeconds;
    private double startTimeSeconds;
    private double endTimeSeconds;
    private String keyIdUsed;
    private String keyMasked;
    private boolean fromCache;
    private String errorMessage;
    private String audioFilePath;

    public SegmentEntity() {
    }

    public SegmentEntity(int index, String text, String cleanText, String audioHash) {
        this.index = index;
        this.text = text;
        this.cleanText = cleanText;
        this.audioHash = audioHash;
        this.status = SegmentStatus.PENDING;
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

    public String getAudioHash() {
        return audioHash;
    }

    public void setAudioHash(String audioHash) {
        this.audioHash = audioHash;
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

    public String getKeyIdUsed() {
        return keyIdUsed;
    }

    public void setKeyIdUsed(String keyIdUsed) {
        this.keyIdUsed = keyIdUsed;
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

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
}

