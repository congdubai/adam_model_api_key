package com.adamtts.studio.service;

import com.adamtts.studio.exception.ElevenLabsApiException;
import com.adamtts.studio.exception.InvalidKeyException;
import com.adamtts.studio.exception.QuotaExceededException;
import com.adamtts.studio.exception.RateLimitException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ElevenLabsClient(RestClient restClient,
                            ObjectMapper objectMapper,
                            @Value("${app.elevenlabs.base-url:https://api.elevenlabs.io}") String baseUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public record SubscriptionInfo(
            Long characterCount,
            Long characterLimit,
            Long remainingCredits,
            String tier,
            boolean readable,
            String message
    ) {}

    /**
     * Checks subscription for an API key.
     * Note: API key is never logged.
     */
    public SubscriptionInfo checkSubscription(String apiKey) {
        String url = baseUrl + "/v1/user/subscription";
        try {
            String jsonResponse = restClient.get()
                    .uri(url)
                    .header("xi-api-key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (jsonResponse != null) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                long charCount = root.path("character_count").asLong(0);
                long charLimit = root.path("character_limit").asLong(0);
                String tier = root.path("tier").asText("N/A");
                long remaining = Math.max(0, charLimit - charCount);

                return new SubscriptionInfo(charCount, charLimit, remaining, tier, true, "OK");
            }
        } catch (HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            int code = ex.getStatusCode().value();
            log.warn("Subscription check returned HTTP {}", code);

            // If error is about permissions, still return readable=false so key can be used
            if (code == 401 || code == 403) {
                String detail = parseErrorDetail(body);
                if (detail.toLowerCase().contains("permission") || detail.toLowerCase().contains("scope")) {
                    return new SubscriptionInfo(null, null, null, "Không xác định", false, "Không đọc được credit (thiếu quyền), vẫn có thể dùng tạo giọng");
                }
                // Invalid key
                return new SubscriptionInfo(null, null, null, null, false, "Key không hợp lệ hoặc bị từ chối: " + detail);
            }
            return new SubscriptionInfo(null, null, null, null, false, "Lỗi HTTP " + code + ": " + parseErrorDetail(body));
        } catch (Exception ex) {
            log.warn("Subscription check failed: {}", ex.getMessage());
            return new SubscriptionInfo(null, null, null, "Lỗi kết nối", false, "Lỗi kết nối: " + ex.getMessage());
        }

        return new SubscriptionInfo(null, null, null, "N/A", false, "Không có phản hồi từ máy chủ");
    }

    /**
     * Synthesize speech for a given text segment.
     */
    public byte[] synthesizeSpeech(String keyId, String apiKey, String voiceId, String text, String modelId, Double stability) {
        String url = baseUrl + "/v1/text-to-speech/" + voiceId + "?output_format=mp3_44100_128";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("model_id", modelId != null && !modelId.isBlank() ? modelId : "eleven_v3");

        if (stability != null) {
            Map<String, Object> voiceSettings = new HashMap<>();
            voiceSettings.put("stability", stability);
            requestBody.put("voice_settings", voiceSettings);
        }

        try {
            return restClient.post()
                    .uri(url)
                    .header("xi-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            log.warn("ElevenLabs TTS error HTTP {}", status);

            if (status == 429) {
                throw new RateLimitException("ElevenLabs bị giới hạn tần suất (429 Too Many Requests).");
            }

            if (status == 401 || status == 402 || status == 403) {
                String detailStatus = parseStatusField(responseBody);
                String detailMessage = parseErrorDetail(responseBody);

                if ("quota_exceeded".equalsIgnoreCase(detailStatus)
                        || detailMessage.toLowerCase().contains("quota_exceeded")
                        || detailMessage.toLowerCase().contains("quota exceeded")
                        || detailMessage.toLowerCase().contains("credit limit")
                        || status == 402) {
                    throw new QuotaExceededException(keyId, "Tài khoản ElevenLabs đã hết credit hoặc đạt hạn mức (quota_exceeded).");
                }

                throw new InvalidKeyException(keyId, "API key không hợp lệ hoặc không có quyền (HTTP " + status + "): " + detailMessage);
            }

            // Other 400, 422...
            throw new ElevenLabsApiException(status, parseErrorDetail(responseBody));
        } catch (HttpServerErrorException ex) {
            throw new ElevenLabsApiException(ex.getStatusCode().value(), "ElevenLabs server error: " + ex.getMessage());
        } catch (RateLimitException | QuotaExceededException | InvalidKeyException | ElevenLabsApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ElevenLabsApiException(500, "Lỗi kết nối tới ElevenLabs: " + ex.getMessage());
        }
    }

    private String parseStatusField(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode detail = root.path("detail");
            if (detail.isObject()) {
                return detail.path("status").asText("");
            } else if (detail.isTextual()) {
                return detail.asText("");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String parseErrorDetail(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) {
            return "Không có chi tiết lỗi";
        }
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode detail = root.path("detail");
            if (detail.isObject()) {
                if (detail.has("message")) {
                    return detail.path("message").asText();
                }
                return detail.toString();
            } else if (detail.isTextual()) {
                return detail.asText();
            }
            if (root.has("message")) {
                return root.path("message").asText();
            }
        } catch (Exception ignored) {
        }
        return jsonBody;
    }
}

