package com.adamtts.studio.service;

import com.adamtts.studio.dto.ApiKeyDto;
import com.adamtts.studio.dto.KeyStatsDto;
import com.adamtts.studio.exception.InvalidKeyException;
import com.adamtts.studio.exception.NoAvailableKeyException;
import com.adamtts.studio.exception.QuotaExceededException;
import com.adamtts.studio.exception.RateLimitException;
import com.adamtts.studio.model.ApiKeyEntity;
import com.adamtts.studio.model.ApiKeyStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ObjectMapper objectMapper;
    private final ElevenLabsClient elevenLabsClient;
    private final File keysFile;
    private final List<ApiKeyEntity> keys = new CopyOnWriteArrayList<>();
    private long[] backoffDelaysMs = { 2500, 5000, 7500 };

    public void setBackoffDelaysMs(long[] delays) {
        if (delays != null) {
            this.backoffDelaysMs = delays;
        }
    }

    public ApiKeyService(ObjectMapper objectMapper,
                         ElevenLabsClient elevenLabsClient,
                         @Value("${app.data-dir:./data}") String dataDir) {
        this.objectMapper = objectMapper;
        this.elevenLabsClient = elevenLabsClient;
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.keysFile = new File(dir, "keys.json");
    }

    @PostConstruct
    public synchronized void init() {
        loadKeysFromFile();
    }

    public synchronized void loadKeysFromFile() {
        if (!keysFile.exists()) {
            return;
        }
        try {
            List<ApiKeyEntity> loaded = objectMapper.readValue(keysFile, new TypeReference<>() {});
            keys.clear();
            if (loaded != null) {
                keys.addAll(loaded);
            }
            log.info("Loaded {} API keys from {}", keys.size(), keysFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load keys from {}: {}", keysFile.getAbsolutePath(), e.getMessage());
        }
    }

    public synchronized void saveKeysToFile() {
        try {
            File parent = keysFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(keysFile, keys);
        } catch (IOException e) {
            log.error("Failed to save keys to {}: {}", keysFile.getAbsolutePath(), e.getMessage());
        }
    }

    public synchronized List<ApiKeyDto> addKeys(String rawKeysText) {
        if (rawKeysText == null || rawKeysText.isBlank()) {
            return getAllKeyDtos();
        }

        String[] lines = rawKeysText.split("\\r?\\n");
        int addedCount = 0;

        for (String line : lines) {
            String trimmedKey = line.trim();
            if (trimmedKey.isEmpty()) {
                continue;
            }

            // Check if key already exists (compare raw key)
            boolean exists = keys.stream().anyMatch(k -> trimmedKey.equals(k.getKey()));
            if (!exists) {
                String id = UUID.randomUUID().toString();
                String masked = maskKey(trimmedKey);
                ApiKeyEntity entity = new ApiKeyEntity(id, trimmedKey, masked);
                keys.add(entity);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            saveKeysToFile();
            log.info("Added {} new API keys to pool.", addedCount);
        }

        return getAllKeyDtos();
    }

    public synchronized boolean deleteKey(String id) {
        boolean removed = keys.removeIf(k -> k.getId().equals(id));
        if (removed) {
            saveKeysToFile();
            log.info("Deleted API key with id: {}", id);
        }
        return removed;
    }

    public List<ApiKeyDto> getAllKeyDtos() {
        return keys.stream().map(this::toDto).toList();
    }

    public KeyStatsDto getKeyStats() {
        int total = keys.size();
        int valid = 0;
        long totalRemaining = 0;
        long totalUsed = 0;

        for (ApiKeyEntity k : keys) {
            if (k.getStatus() == ApiKeyStatus.OK || k.getStatus() == ApiKeyStatus.UNCHECKED) {
                valid++;
            }
            if (k.getRemainingCredits() != null && k.getRemainingCredits() > 0) {
                totalRemaining += k.getRemainingCredits();
            }
            totalUsed += k.getUsedInSession();
        }

        return new KeyStatsDto(total, valid, totalRemaining, totalUsed, getAllKeyDtos());
    }

    public synchronized List<ApiKeyDto> checkAllKeys() {
        for (ApiKeyEntity entity : keys) {
            checkSingleKey(entity);
        }
        saveKeysToFile();
        return getAllKeyDtos();
    }

    public synchronized ApiKeyDto checkKeyById(String id) {
        ApiKeyEntity entity = keys.stream()
                .filter(k -> k.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (entity != null) {
            checkSingleKey(entity);
            saveKeysToFile();
            return toDto(entity);
        }
        return null;
    }

    private void checkSingleKey(ApiKeyEntity entity) {
        ElevenLabsClient.SubscriptionInfo sub = elevenLabsClient.checkSubscription(entity.getKey());
        entity.setLastCheckedAt(Instant.now());

        if (sub != null && sub.readable()) {
            entity.setCharacterLimit(sub.characterLimit());
            entity.setRemainingCredits(sub.remainingCredits());
            entity.setTier(sub.tier());
            entity.setStatus(sub.remainingCredits() > 0 ? ApiKeyStatus.OK : ApiKeyStatus.EXHAUSTED);
            entity.setLastErrorMessage(null);
        } else if (sub != null) {
            // Subscription could not be read
            if (sub.tier() != null && sub.tier().contains("Không xác định")) {
                // Key lacks permission to read subscription, but still usable
                entity.setRemainingCredits(null);
                entity.setCharacterLimit(null);
                entity.setTier("Không đọc được");
                entity.setStatus(ApiKeyStatus.OK);
                entity.setLastErrorMessage("Không đọc được credit (thiếu quyền), vẫn có thể dùng tạo giọng");
            } else {
                entity.setStatus(ApiKeyStatus.ERROR);
                entity.setLastErrorMessage(sub.message());
            }
        } else {
            entity.setStatus(ApiKeyStatus.ERROR);
            entity.setLastErrorMessage("Không nhận được phản hồi kiểm tra subscription");
        }
    }

    /**
     * Executes TTS for a given segment with smart key rotation and retry logic:
     * - Order: in order of appearance in keys list.
     * - Candidate key must have remaining == null or remaining >= segmentLength, and status != ERROR.
     * - 429: backoff 2.5s, 5s, 7.5s (max 3 retries), then switch key.
     * - 401/402/403 quota_exceeded: mark EXHAUSTED, remaining = segmentLength - 1, switch key.
     * - 401/402/403 other: mark ERROR, switch key.
     * - 400/422: throw directly.
     */
    public TtsResult executeTtsWithKeySwitching(String voiceId, String text, String modelId, Double stability) {
        int segmentLength = text != null ? text.length() : 0;
        int startIndex = 0;

        while (true) {
            ApiKeyEntity selectedKey = null;

            // Find first available key starting from startIndex
            for (int i = startIndex; i < keys.size(); i++) {
                ApiKeyEntity k = keys.get(i);
                if (k.getStatus() == ApiKeyStatus.ERROR) {
                    continue;
                }
                // Check if remaining credits are sufficient (null means unknown, so try it)
                if (k.getRemainingCredits() == null || k.getRemainingCredits() >= segmentLength) {
                    selectedKey = k;
                    startIndex = i + 1; // For next search if this key fails
                    break;
                }
            }

            if (selectedKey == null) {
                throw new NoAvailableKeyException("Không còn API key nào khả dụng hoặc đủ credit cho đoạn (" + segmentLength + " ký tự). Vui lòng thêm key mới.");
            }

            // Try synthesizing with selectedKey
            try {
                byte[] audio = trySynthesizeWithBackoff(selectedKey, voiceId, text, modelId, stability);

                // Success! Update session usage and decrement remaining credit estimate
                selectedKey.addUsedInSession(segmentLength);
                if (selectedKey.getRemainingCredits() != null) {
                    long newRemaining = Math.max(0, selectedKey.getRemainingCredits() - segmentLength);
                    selectedKey.setRemainingCredits(newRemaining);
                    if (newRemaining == 0) {
                        selectedKey.setStatus(ApiKeyStatus.EXHAUSTED);
                    }
                }
                saveKeysToFile();

                return new TtsResult(audio, selectedKey.getId(), selectedKey.getMaskedKey());

            } catch (QuotaExceededException ex) {
                log.warn("Key {} quota exceeded for segment of length {}", selectedKey.getMaskedKey(), segmentLength);
                selectedKey.setStatus(ApiKeyStatus.EXHAUSTED);
                // Mark remaining so shorter segments can still try
                selectedKey.setRemainingCredits((long) Math.max(0, segmentLength - 1));
                selectedKey.setLastErrorMessage("Hết credit (quota_exceeded)");
                saveKeysToFile();
                // Loop to try next key

            } catch (InvalidKeyException ex) {
                log.warn("Key {} is invalid: {}", selectedKey.getMaskedKey(), ex.getMessage());
                selectedKey.setStatus(ApiKeyStatus.ERROR);
                selectedKey.setLastErrorMessage(ex.getMessage());
                saveKeysToFile();
                // Loop to try next key

            } catch (RateLimitException ex) {
                log.warn("Key {} rate limit exceeded after retries. Switching to next key.", selectedKey.getMaskedKey());
                // Switch key
            }
        }
    }

    private byte[] trySynthesizeWithBackoff(ApiKeyEntity key, String voiceId, String text, String modelId, Double stability) {
        long[] backoffsMs = this.backoffDelaysMs;
        int attempt = 0;

        while (true) {
            try {
                return elevenLabsClient.synthesizeSpeech(key.getId(), key.getKey(), voiceId, text, modelId, stability);
            } catch (RateLimitException ex) {
                if (attempt < backoffsMs.length) {
                    long delay = backoffsMs[attempt];
                    attempt++;
                    log.info("Rate limit (429) hit for key {}. Retrying attempt {}/3 after {}ms...",
                            key.getMaskedKey(), attempt, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
        }
    }

    public static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        int len = key.length();
        if (len <= 8) {
            return key.charAt(0) + "..." + key.charAt(len - 1);
        }
        return key.substring(0, 4) + "..." + key.substring(len - 4);
    }

    private ApiKeyDto toDto(ApiKeyEntity e) {
        return new ApiKeyDto(
                e.getId(),
                e.getMaskedKey(),
                e.getStatus(),
                e.getRemainingCredits(),
                e.getCharacterLimit(),
                e.getTier(),
                e.getUsedInSession(),
                e.getLastErrorMessage(),
                e.getLastCheckedAt()
        );
    }

    public record TtsResult(byte[] audioData, String keyId, String keyMasked) {}
}
