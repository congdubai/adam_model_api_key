package com.adamtts.studio.controller;

import com.adamtts.studio.dto.JobCreateRequest;
import com.adamtts.studio.dto.JobDetailDto;
import com.adamtts.studio.dto.JobSummaryDto;
import com.adamtts.studio.service.JobManagerService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobManagerService jobManagerService;

    public JobController(JobManagerService jobManagerService) {
        this.jobManagerService = jobManagerService;
    }

    @PostMapping
    public ResponseEntity<JobDetailDto> createJob(@Valid @RequestBody JobCreateRequest request) {
        JobDetailDto job = jobManagerService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDetailDto> getJob(@PathVariable String id) {
        JobDetailDto job = jobManagerService.getJobDetail(id);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<JobSummaryDto> getJobSummary(@PathVariable String id) {
        JobSummaryDto summary = jobManagerService.getJobSummary(id);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopJob(@PathVariable String id) {
        jobManagerService.stopJob(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã yêu cầu dừng tiến trình"));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<JobDetailDto> resumeJob(@PathVariable String id) {
        JobDetailDto job = jobManagerService.resumeJob(id);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/segments/{index}/regenerate")
    public ResponseEntity<JobDetailDto> regenerateSegment(@PathVariable String id, @PathVariable int index) {
        JobDetailDto updated = jobManagerService.regenerateSegment(id, index);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> getAudioStream(@PathVariable String id) {
        File file = jobManagerService.getOutputFile(id, "mp3");
        if (file == null || !file.exists()) {
            file = jobManagerService.getOutputFile(id, "wav");
        }
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String mediaType = file.getName().endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .body(new FileSystemResource(file));
    }

    @GetMapping("/{id}/download/{format}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id, @PathVariable String format) {
        File file = jobManagerService.getOutputFile(id, format);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = switch (format.toLowerCase()) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "srt" -> "text/plain; charset=utf-8";
            case "json" -> "application/json; charset=utf-8";
            case "csv" -> "text/csv; charset=utf-8";
            default -> "application/octet-stream";
        };

        String filename = "adam_tts_" + id.substring(0, 8) + "." + format.toLowerCase();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(file));
    }
}

