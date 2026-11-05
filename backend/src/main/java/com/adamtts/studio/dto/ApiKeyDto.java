package com.adamtts.studio.dto;

import com.adamtts.studio.model.ApiKeyStatus;
import java.time.Instant;

public class ApiKeyDto {
    private String id;
    private String maskedKey;
    private ApiKeyStatus status;
    private Long remainingCredits;
    private Long characterLimit;
    private String tier;
    private long usedInSession;
    private String lastErrorMessage;
    private Instant lastCheckedAt;

    public ApiKeyDto() {
    }

    public ApiKeyDto(String id, String maskedKey, ApiKeyStatus status, Long remainingCredits,
                     Long characterLimit, String tier, long usedInSession,
                     String lastErrorMessage, Instant lastCheckedAt) {
        this.id = id;
        this.maskedKey = maskedKey;
        this.status = status;
        this.remainingCredits = remainingCredits;
        this.characterLimit = characterLimit;
        this.tier = tier;
        this.usedInSession = usedInSession;
        this.lastErrorMessage = lastErrorMessage;
        this.lastCheckedAt = lastCheckedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public void setMaskedKey(String maskedKey) {
        this.maskedKey = maskedKey;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }

    public Long getRemainingCredits() {
        return remainingCredits;
    }

    public void setRemainingCredits(Long remainingCredits) {
        this.remainingCredits = remainingCredits;
    }

    public Long getCharacterLimit() {
        return characterLimit;
    }

    public void setCharacterLimit(Long characterLimit) {
        this.characterLimit = characterLimit;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public long getUsedInSession() {
        return usedInSession;
    }

    public void setUsedInSession(long usedInSession) {
        this.usedInSession = usedInSession;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}

