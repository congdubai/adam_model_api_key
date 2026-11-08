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
    public void getAudioStream(
            @PathVariable String id,
            @RequestParam(value = "format", required = false) String format,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        File file = null;
        if (format != null && !format.isBlank()) {
            file = jobManagerService.getOutputFile(id, format);
        }
        if (file == null || !file.exists()) {
            file = jobManagerService.getOutputFile(id, "mp3");
        }
        if (file == null || !file.exists()) {
            file = jobManagerService.getOutputFile(id, "wav");
        }
        if (file == null || !file.exists()) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = file.getName().endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
        long fileLength = file.length();
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);

        long start = 0;
        long end = fileLength - 1;
        boolean isRange = false;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String cleanRange = rangeHeader.substring(6).trim();
            int dashIndex = cleanRange.indexOf('-');
            if (dashIndex != -1) {
                String startPart = cleanRange.substring(0, dashIndex).trim();
                String endPart = cleanRange.substring(dashIndex + 1).trim();
                try {
                    if (!startPart.isEmpty()) {
                        start = Long.parseLong(startPart);
                    }
                    if (!endPart.isEmpty()) {
                        end = Long.parseLong(endPart);
                    }
                    if (end >= fileLength) {
                        end = fileLength - 1;
                    }
                    if (start <= end && start >= 0) {
                        isRange = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        long rangeLength = end - start + 1;

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

        if (isRange) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeLength));
        } else {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"");
        }

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
             java.io.OutputStream os = response.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[16384];
            long remaining = rangeLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read == -1) break;
                os.write(buffer, 0, read);
                remaining -= read;
            }
            os.flush();
        }
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
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };

        String filename = "adam_tts_" + id.substring(0, 8) + "." + format.toLowerCase();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(file));
    }

    @GetMapping("/{id}/segments/{index}/download")
    public ResponseEntity<Resource> downloadSegment(@PathVariable String id, @PathVariable int index) {
        File file = jobManagerService.getSegmentFile(id, index);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        String ext = file.getName().endsWith(".wav") ? "wav" : "mp3";
        String contentType = ext.equals("wav") ? "audio/wav" : "audio/mpeg";
        String filename = String.format("adam_seg_%02d.%s", index, ext);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(file));
    }
}

