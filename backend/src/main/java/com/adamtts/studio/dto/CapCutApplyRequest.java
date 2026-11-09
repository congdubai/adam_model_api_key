package com.adamtts.studio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CapCutApplyRequest {
    private String jobId;
    private String projectFolder;
    private boolean syncVideoClips = true;
    private boolean addAudioTrack = true;
    private boolean addSubtitleTrack = true;
    private boolean clearCache = true;
    private String subtitleStyle = "tiktok_black_bar"; // "tiktok_black_bar" or "classic_shadow"
    private int subtitleAlignment = 0; // 0 = Left (default for TikTok lower-left), 1 = Center
    private int subtitleSize = 24;
    private Double subtitleY = null;

    public CapCutApplyRequest() {
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(String projectFolder) {
        this.projectFolder = projectFolder;
    }

    public boolean isSyncVideoClips() {
        return syncVideoClips;
    }

    public void setSyncVideoClips(boolean syncVideoClips) {
        this.syncVideoClips = syncVideoClips;
    }

    public boolean isAddAudioTrack() {
        return addAudioTrack;
    }

    public void setAddAudioTrack(boolean addAudioTrack) {
        this.addAudioTrack = addAudioTrack;
    }

    public boolean isAddSubtitleTrack() {
        return addSubtitleTrack;
    }

    public void setAddSubtitleTrack(boolean addSubtitleTrack) {
        this.addSubtitleTrack = addSubtitleTrack;
    }

    public boolean isClearCache() {
        return clearCache;
    }

    public void setClearCache(boolean clearCache) {
        this.clearCache = clearCache;
    }

    public String getSubtitleStyle() {
        return subtitleStyle;
    }

    public void setSubtitleStyle(String subtitleStyle) {
        this.subtitleStyle = subtitleStyle;
    }

    public int getSubtitleAlignment() {
        return subtitleAlignment;
    }

    public void setSubtitleAlignment(int subtitleAlignment) {
        this.subtitleAlignment = subtitleAlignment;
    }

    public int getSubtitleSize() {
        return subtitleSize;
    }

    public void setSubtitleSize(int subtitleSize) {
        this.subtitleSize = subtitleSize;
    }

    public Double getSubtitleY() {
        return subtitleY;
    }

    public void setSubtitleY(Double subtitleY) {
        this.subtitleY = subtitleY;
    }
}


