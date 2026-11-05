package com.adamtts.studio.controller;

import com.adamtts.studio.dto.AddKeysRequest;
import com.adamtts.studio.dto.ApiKeyDto;
import com.adamtts.studio.dto.KeyStatsDto;
import com.adamtts.studio.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> getKeys() {
        return ResponseEntity.ok(apiKeyService.getAllKeyDtos());
    }

    @GetMapping("/stats")
    public ResponseEntity<KeyStatsDto> getKeyStats() {
        return ResponseEntity.ok(apiKeyService.getKeyStats());
    }

    @PostMapping
    public ResponseEntity<List<ApiKeyDto>> addKeys(@RequestBody AddKeysRequest request) {
        List<ApiKeyDto> updated = apiKeyService.addKeys(request.getKeys());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable String id) {
        boolean deleted = apiKeyService.deleteKey(id);
        return ResponseEntity.ok(Map.of("success", deleted, "id", id));
    }

    @PostMapping("/check")
    public ResponseEntity<List<ApiKeyDto>> checkAllKeys() {
        return ResponseEntity.ok(apiKeyService.checkAllKeys());
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<ApiKeyDto> checkSingleKey(@PathVariable String id) {
        ApiKeyDto result = apiKeyService.checkKeyById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}

