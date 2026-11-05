package com.adamtts.studio.service;

import com.adamtts.studio.dto.ApiKeyDto;
import com.adamtts.studio.exception.InvalidKeyException;
import com.adamtts.studio.exception.NoAvailableKeyException;
import com.adamtts.studio.exception.QuotaExceededException;
import com.adamtts.studio.exception.RateLimitException;
import com.adamtts.studio.model.ApiKeyStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

    private ElevenLabsClient elevenLabsClient;
    private ApiKeyService apiKeyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        elevenLabsClient = Mockito.mock(ElevenLabsClient.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        apiKeyService = new ApiKeyService(objectMapper, elevenLabsClient, tempDir.toString());
        apiKeyService.setBackoffDelaysMs(new long[]{ 10, 10, 10 });
    }

    @Test
    @DisplayName("Che API key an toàn (4 đầu + 4 cuối)")
    void testMaskKey() {
        assertEquals("sk_1...cdef", ApiKeyService.maskKey("sk_1234567890abcdef"));
        assertEquals("1...8", ApiKeyService.maskKey("12345678"));
        assertEquals("", ApiKeyService.maskKey(""));
        assertEquals("", ApiKeyService.maskKey(null));
    }

    @Test
    @DisplayName("Thêm nhiều key cùng lúc từ chuỗi nhiều dòng")
    void testAddMultipleKeys() {
        String rawKeys = "key_one_12345678\r\nkey_two_87654321\nkey_three_11223344\n\n";
        List<ApiKeyDto> dtos = apiKeyService.addKeys(rawKeys);

        assertEquals(3, dtos.size());
        assertEquals("key_...5678", dtos.get(0).getMaskedKey());
        assertEquals("key_...4321", dtos.get(1).getMaskedKey());
        assertEquals("key_...3344", dtos.get(2).getMaskedKey());
    }

    @Test
    @DisplayName("Chuyển key khi key 1 hết credit (quota_exceeded) sang key 2 thành công")
    void testKeySwitchingOnQuotaExceeded() {
        apiKeyService.addKeys("key_alpha_11111111\nkey_beta_22222222");

        byte[] fakeAudio = new byte[]{1, 2, 3, 4};

        // Key 1 throws QuotaExceededException
        when(elevenLabsClient.synthesizeSpeech(anyString(), eq("key_alpha_11111111"), anyString(), anyString(), anyString(), any()))
                .thenThrow(new QuotaExceededException("key1", "Quota exceeded"));

        // Key 2 succeeds
        when(elevenLabsClient.synthesizeSpeech(anyString(), eq("key_beta_22222222"), anyString(), anyString(), anyString(), any()))
                .thenReturn(fakeAudio);

        String text = "Xin chào tôi là Adam.";
        ApiKeyService.TtsResult result = apiKeyService.executeTtsWithKeySwitching("pNInz6obpgDQGcFmaJgB", text, "eleven_v3", 0.5);

        assertNotNull(result);
        assertArrayEquals(fakeAudio, result.audioData());
        assertEquals("key_...2222", result.keyMasked());

        // Verify Key 1 is marked EXHAUSTED and remaining is text.length() - 1
        List<ApiKeyDto> keys = apiKeyService.getAllKeyDtos();
        assertEquals(ApiKeyStatus.EXHAUSTED, keys.get(0).getStatus());
        assertEquals(text.length() - 1, keys.get(0).getRemainingCredits());
    }

    @Test
    @DisplayName("Bỏ qua key có remaining credit nhỏ hơn độ dài đoạn")
    void testSkipKeyWithInsufficientCredit() {
        // Mock subscription responses before adding/checking keys
        when(elevenLabsClient.checkSubscription("key_small_credit11"))
                .thenReturn(new ElevenLabsClient.SubscriptionInfo(990L, 1000L, 10L, "free", true, "OK"));
        when(elevenLabsClient.checkSubscription("key_large_credit22"))
                .thenReturn(new ElevenLabsClient.SubscriptionInfo(0L, 10000L, 10000L, "starter", true, "OK"));

        apiKeyService.addKeys("key_small_credit11\nkey_large_credit22");
        apiKeyService.checkAllKeys();

        // Key 1 has 10 credits remaining. Segment length is 50.
        byte[] fakeAudio = new byte[]{9, 8, 7};
        when(elevenLabsClient.synthesizeSpeech(anyString(), eq("key_large_credit22"), anyString(), anyString(), anyString(), any()))
                .thenReturn(fakeAudio);

        String text = "Một đoạn văn bản dài hơn 10 ký tự chắc chắn cần nhiều hơn 10 credits.";
        ApiKeyService.TtsResult result = apiKeyService.executeTtsWithKeySwitching("pNInz6obpgDQGcFmaJgB", text, "eleven_v3", 0.5);

        assertNotNull(result);
        assertEquals("key_...it22", result.keyMasked());

        // Verify key_small was never called for TTS
        verify(elevenLabsClient, never()).synthesizeSpeech(anyString(), eq("key_small_credit11"), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Thử lại khi gặp 429 và chuyển sang key tiếp theo nếu hết số lần thử")
    void testRateLimit429RetryAndSwitch() {
        apiKeyService.addKeys("key_rate_limited1\nkey_backup2222222");

        // Key 1 throws RateLimitException every time
        when(elevenLabsClient.synthesizeSpeech(anyString(), eq("key_rate_limited1"), anyString(), anyString(), anyString(), any()))
                .thenThrow(new RateLimitException("429 Too Many Requests"));

        byte[] fakeAudio = new byte[]{5, 5, 5};
        when(elevenLabsClient.synthesizeSpeech(anyString(), eq("key_backup2222222"), anyString(), anyString(), anyString(), any()))
                .thenReturn(fakeAudio);

        ApiKeyService.TtsResult result = apiKeyService.executeTtsWithKeySwitching("voiceId", "Test retry", "eleven_v3", 0.5);

        assertNotNull(result);
        assertEquals("key_...2222", result.keyMasked());

        // Key 1 should be attempted 4 times total: initial + 3 retries
        verify(elevenLabsClient, times(4)).synthesizeSpeech(anyString(), eq("key_rate_limited1"), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Khi không còn key nào khả dụng thì ném ngoại lệ NoAvailableKeyException")
    void testNoAvailableKeyThrowsException() {
        apiKeyService.addKeys("invalid_key_999999");

        when(elevenLabsClient.synthesizeSpeech(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new InvalidKeyException("k1", "Key không hợp lệ"));

        assertThrows(NoAvailableKeyException.class, () ->
                apiKeyService.executeTtsWithKeySwitching("voiceId", "Test text", "eleven_v3", 0.5)
        );
    }
}
