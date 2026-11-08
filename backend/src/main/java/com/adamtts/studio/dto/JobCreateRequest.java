package com.adamtts.studio.dto;

import jakarta.validation.constraints.NotBlank;

public class JobCreateRequest {
    @NotBlank(message = "Văn bản không được để trống")
    private String text;

    private String splitMode = "PARAGRAPH"; // "LINE" or "PARAGRAPH"
    private String modelId = "eleven_v3";   // "eleven_v3", "eleven_multilingual_v2", "eleven_flash_v2_5"
    private Double stability = 0.5;         // Creative=0.0, Natural=0.5, Robust=1.0
    private String voiceId = "pNInz6obpgDQGcFmaJgB"; // Default: Adam
    private double gapSeconds = 0.4;        // Default 0.4s
    private Double speed = 1.0;             // Speaking speed: 1.0=normal, 1.1=fast, 1.25=very fast

    public JobCreateRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSplitMode() {
        return splitMode;
    }

    public void setSplitMode(String splitMode) {
        this.splitMode = splitMode;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Double getStability() {
        return stability;
    }

    public void setStability(Double stability) {
        this.stability = stability;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
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
}

