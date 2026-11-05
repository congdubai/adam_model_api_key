package com.adamtts.studio.dto;

import java.util.List;

public class KeyStatsDto {
    private int totalKeys;
    private int validKeys;
    private long totalRemainingCredits;
    private long totalUsedInSession;
    private List<ApiKeyDto> keys;

    public KeyStatsDto() {
    }

    public KeyStatsDto(int totalKeys, int validKeys, long totalRemainingCredits, long totalUsedInSession, List<ApiKeyDto> keys) {
        this.totalKeys = totalKeys;
        this.validKeys = validKeys;
        this.totalRemainingCredits = totalRemainingCredits;
        this.totalUsedInSession = totalUsedInSession;
        this.keys = keys;
    }

    public int getTotalKeys() {
        return totalKeys;
    }

    public void setTotalKeys(int totalKeys) {
        this.totalKeys = totalKeys;
    }

    public int getValidKeys() {
        return validKeys;
    }

    public void setValidKeys(int validKeys) {
        this.validKeys = validKeys;
    }

    public long getTotalRemainingCredits() {
        return totalRemainingCredits;
    }

    public void setTotalRemainingCredits(long totalRemainingCredits) {
        this.totalRemainingCredits = totalRemainingCredits;
    }

    public long getTotalUsedInSession() {
        return totalUsedInSession;
    }

    public void setTotalUsedInSession(long totalUsedInSession) {
        this.totalUsedInSession = totalUsedInSession;
    }

    public List<ApiKeyDto> getKeys() {
        return keys;
    }

    public void setKeys(List<ApiKeyDto> keys) {
        this.keys = keys;
    }
}

