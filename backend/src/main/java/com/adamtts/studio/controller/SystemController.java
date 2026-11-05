package com.adamtts.studio.controller;

import com.adamtts.studio.service.AudioProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final AudioProcessingService audioProcessingService;

    public SystemController(AudioProcessingService audioProcessingService) {
        this.audioProcessingService = audioProcessingService;
    }

    @GetMapping("/ffmpeg-status")
    public ResponseEntity<Map<String, Object>> checkFfmpeg() {
        boolean available = audioProcessingService.checkFfmpegAvailability();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available
                        ? "FFmpeg và FFprobe đã sẵn sàng."
                        : "Không tìm thấy ffmpeg/ffprobe trên hệ thống. Vui lòng cài đặt FFmpeg và thêm C:\\ffmpeg\\bin vào PATH (hoặc chạy: winget install Gyan.FFmpeg) rồi khởi động lại backend."
        ));
    }
}

