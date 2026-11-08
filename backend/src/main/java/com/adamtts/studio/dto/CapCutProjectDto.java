package com.adamtts.studio.dto;

public class CapCutProjectDto {
    private String id;
    private String name;
    private String folderPath;
    private int clipCount;
    private boolean readable;
    private long modifiedTime;
    private double durationSeconds;

    public CapCutProjectDto() {
    }

    public CapCutProjectDto(String id, String name, String folderPath, int clipCount, boolean readable, long modifiedTime, double durationSeconds) {
        this.id = id;
        this.name = name;
        this.folderPath = folderPath;
        this.clipCount = clipCount;
        this.readable = readable;
        this.modifiedTime = modifiedTime;
        this.durationSeconds = durationSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public int getClipCount() {
        return clipCount;
    }

    public void setClipCount(int clipCount) {
        this.clipCount = clipCount;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}

