package com.adamtts.studio.service;

import com.adamtts.studio.model.SegmentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimelineService {

    private final ObjectMapper objectMapper;
    private final TextSplittingService textSplittingService;

    public TimelineService(ObjectMapper objectMapper, TextSplittingService textSplittingService) {
        this.objectMapper = objectMapper;
        this.textSplittingService = textSplittingService;
    }

    /**
     * Calculates sample-accurate start and end times for all segments.
     * Prevents accumulated floating-point drift.
     */
    public void calculateTimestamps(List<SegmentEntity> segments, double gapSeconds) {
        if (segments == null || segments.isEmpty()) {
            return;
        }

        final double sampleRate = 44100.0;
        long totalSamples = 0;
        long gapSamples = Math.round(gapSeconds * sampleRate);

        for (int i = 0; i < segments.size(); i++) {
            SegmentEntity seg = segments.get(i);
            long segSamples = Math.round(seg.getDurationSeconds() * sampleRate);

            // If segment specifies a target start time (e.g. [45, 60] -> 45.0s)
            if (seg.getTargetStartTime() != null) {
                long targetStartSamples = Math.round(seg.getTargetStartTime() * sampleRate);
                if (targetStartSamples > totalSamples) {
                    totalSamples = targetStartSamples;
                }
            }

            double startTime = totalSamples / sampleRate;
            double endTime = (totalSamples + segSamples) / sampleRate;

            seg.setStartTimeSeconds(startTime);
            seg.setEndTimeSeconds(endTime);

            // Advance sample count by segment duration
            totalSamples += segSamples;

            // If there is a next segment:
            if (i < segments.size() - 1) {
                SegmentEntity nextSeg = segments.get(i + 1);
                if (nextSeg.getTargetStartTime() != null) {
                    long nextTargetStartSamples = Math.round(nextSeg.getTargetStartTime() * sampleRate);
                    if (nextTargetStartSamples <= totalSamples) {
                        totalSamples += gapSamples;
                    }
                } else {
                    totalSamples += gapSamples;
                }
            }
        }
    }

    /**
     * Formats seconds into SRT timestamp format: HH:mm:ss,SSS
     */
    public String formatSrtTimestamp(double seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long totalMillis = Math.round(seconds * 1000.0);
        long hours = totalMillis / 3600000;
        long minutes = (totalMillis % 3600000) / 60000;
        long secs = (totalMillis % 60000) / 1000;
        long millis = totalMillis % 1000;

        return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }

    /**
     * Generates SRT subtitle content.
     * Subtitles must strip emotion tags [..].
     */
    public String generateSrtContent(List<SegmentEntity> segments) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < segments.size(); i++) {
            SegmentEntity seg = segments.get(i);
            String cleanText = seg.getCleanText();
            if (cleanText == null || cleanText.isBlank()) {
                cleanText = textSplittingService.stripEmotionTags(seg.getText());
            }

            sb.append(i + 1).append("\r\n");
            sb.append(formatSrtTimestamp(seg.getStartTimeSeconds()))
                    .append(" --> ")
                    .append(formatSrtTimestamp(seg.getEndTimeSeconds()))
                    .append("\r\n");
            sb.append(cleanText).append("\r\n\r\n");
        }

        return sb.toString();
    }

    /**
     * Writes SRT file to disk.
     */
    public void writeSrtFile(List<SegmentEntity> segments, File srtFile) throws IOException {
        String content = generateSrtContent(segments);
        try (PrintWriter out = new PrintWriter(new FileWriter(srtFile, StandardCharsets.UTF_8))) {
            out.print(content);
        }
    }

    /**
     * Generates timeline.json and writes to disk.
     */
    public void writeTimelineJsonFile(List<SegmentEntity> segments, File jsonFile) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            SegmentEntity seg = segments.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("index", i + 1);
            map.put("start", round3(seg.getStartTimeSeconds()));
            map.put("end", round3(seg.getEndTimeSeconds()));
            map.put("duration", round3(seg.getDurationSeconds()));
            map.put("text", seg.getText());
            map.put("cleanText", seg.getCleanText());
            map.put("keyMasked", seg.getKeyMasked());
            list.add(map);
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, list);
    }

    /**
     * Generates timeline.csv and writes to disk.
     */
    public void writeTimelineCsvFile(List<SegmentEntity> segments, File csvFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile, StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM so Excel opens Vietnamese characters correctly
            writer.print('\ufeff');
            writer.println("index,start,end,duration,clean_text,key_masked");

            for (int i = 0; i < segments.size(); i++) {
                SegmentEntity seg = segments.get(i);
                writer.println(String.format(java.util.Locale.US,
                        "%d,%.3f,%.3f,%.3f,\"%s\",\"%s\"",
                        i + 1,
                        seg.getStartTimeSeconds(),
                        seg.getEndTimeSeconds(),
                        seg.getDurationSeconds(),
                        escapeCsv(seg.getCleanText()),
                        escapeCsv(seg.getKeyMasked() != null ? seg.getKeyMasked() : "")
                ));
            }
        }
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String escapeCsv(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\"\"");
    }
}

