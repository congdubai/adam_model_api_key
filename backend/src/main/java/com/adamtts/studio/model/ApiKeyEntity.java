package com.adamtts.studio.model;

import java.time.Instant;

public class ApiKeyEntity {
    private String id;
    private String key;
    private String maskedKey;
    private ApiKeyStatus status;
    private Long remainingCredits;
    private Long characterLimit;
    private String tier;
    private long usedInSession;
    private String lastErrorMessage;
    private Instant lastCheckedAt;

    public ApiKeyEntity() {
    }

    public ApiKeyEntity(String id, String key, String maskedKey) {
        this.id = id;
        this.key = key;
        this.maskedKey = maskedKey;
        this.status = ApiKeyStatus.UNCHECKED;
        this.usedInSession = 0L;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public void addUsedInSession(long count) {
        this.usedInSession += count;
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

