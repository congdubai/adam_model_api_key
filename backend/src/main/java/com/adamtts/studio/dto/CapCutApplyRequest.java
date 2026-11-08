package com.adamtts.studio.dto;

public class CapCutApplyRequest {
    private String jobId;
    private String projectFolder;
    private boolean syncVideoClips = true;
    private boolean addAudioTrack = true;
    private boolean addSubtitleTrack = true;
    private boolean clearCache = true;

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
}

