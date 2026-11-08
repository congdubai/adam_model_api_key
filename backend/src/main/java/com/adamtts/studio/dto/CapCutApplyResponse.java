package com.adamtts.studio.dto;

public class CapCutApplyResponse {
    private boolean success;
    private String message;
    private String projectFolder;
    private int syncedClips;
    private int addedSubtitles;
    private boolean audioAdded;
    private boolean capcutWasRunning;

    public CapCutApplyResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(String projectFolder) {
        this.projectFolder = projectFolder;
    }

    public int getSyncedClips() {
        return syncedClips;
    }

    public void setSyncedClips(int syncedClips) {
        this.syncedClips = syncedClips;
    }

    public int getAddedSubtitles() {
        return addedSubtitles;
    }

    public void setAddedSubtitles(int addedSubtitles) {
        this.addedSubtitles = addedSubtitles;
    }

    public boolean isAudioAdded() {
        return audioAdded;
    }

    public void setAudioAdded(boolean audioAdded) {
        this.audioAdded = audioAdded;
    }

    public boolean isCapcutWasRunning() {
        return capcutWasRunning;
    }

    public void setCapcutWasRunning(boolean capcutWasRunning) {
        this.capcutWasRunning = capcutWasRunning;
    }
}

