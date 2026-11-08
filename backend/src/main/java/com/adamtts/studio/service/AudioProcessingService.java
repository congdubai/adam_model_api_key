package com.adamtts.studio.service;

import com.adamtts.studio.exception.FfmpegNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import com.adamtts.studio.model.SegmentEntity;

@Service
public class AudioProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AudioProcessingService.class);

    private final String ffmpegCmd;
    private final String ffprobeCmd;
    private final File cacheDir;

    public AudioProcessingService(@Value("${app.ffmpeg-path:ffmpeg}") String ffmpegCmd,
                                  @Value("${app.ffprobe-path:ffprobe}") String ffprobeCmd,
                                  @Value("${app.data-dir:./data}") String dataDir) {
        this.ffmpegCmd = ffmpegCmd;
        this.ffprobeCmd = ffprobeCmd;
        this.cacheDir = new File(dataDir, "cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    @PostConstruct
    public void init() {
        checkFfmpegAvailability();
    }

    /**
     * Checks whether ffmpeg and ffprobe are available.
     * Throws FfmpegNotFoundException if missing.
     */
    public boolean checkFfmpegAvailability() {
        boolean ffmpegOk = testCommand(ffmpegCmd, "-version");
        boolean ffprobeOk = testCommand(ffprobeCmd, "-version");

        if (!ffmpegOk || !ffprobeOk) {
            String msg = "Không tìm thấy ffmpeg hoặc ffprobe trên hệ thống.\n" +
                    "Vui lòng cài đặt FFmpeg và thêm vào biến môi trường PATH.\n" +
                    "- Cách 1: Chạy PowerShell (Admin): winget install Gyan.FFmpeg\n" +
                    "- Cách 2: Tải file zip từ https://www.gyan.dev/ffmpeg/builds/, giải nén vào C:\\ffmpeg và thêm C:\\ffmpeg\\bin vào PATH hệ thống.\n" +
                    "Sau khi cài đặt xong, hãy khởi động lại ứng dụng.";
            log.warn(msg);
            return false;
        }
        return true;
    }

    public void requireFfmpeg() {
        if (!checkFfmpegAvailability()) {
            throw new FfmpegNotFoundException("Không tìm thấy ffmpeg hoặc ffprobe trên hệ thống. Vui lòng cài đặt FFmpeg và cấu hình PATH.");
        }
    }

    private boolean testCommand(String... cmd) {
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a deterministic SHA-256 hash for caching.
     */
    public String calculateHash(String modelId, String voiceId, Double stability, String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String key = (modelId != null ? modelId : "") + "|" +
                    (voiceId != null ? voiceId : "") + "|" +
                    (stability != null ? stability : 0.5) + "|" +
                    (text != null ? text.trim() : "");
            byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Retrieves cached audio file if it exists and has content.
     */
    public File getCachedAudio(String hash) {
        File file = new File(cacheDir, hash + ".mp3");
        if (file.exists() && file.length() > 0) {
            return file;
        }
        return null;
    }

    /**
     * Saves audio data into cache.
     */
    public File saveToCache(String hash, byte[] audioData) throws IOException {
        File file = new File(cacheDir, hash + ".mp3");
        Files.write(file.toPath(), audioData);
        return file;
    }

    /**
     * Generates a silence MP3 file directly in cache without calling ElevenLabs API.
     */
    public File generateSilenceMp3(String hash, double durationSeconds) {
        requireFfmpeg();
        File file = new File(cacheDir, hash + ".mp3");
        List<String> silenceCmd = List.of(
                ffmpegCmd,
                "-y",
                "-f", "lavfi",
                "-i", "anullsrc=r=44100:cl=mono",
                "-t", String.format(java.util.Locale.US, "%.4f", durationSeconds),
                "-c:a", "libmp3lame",
                "-b:a", "128k",
                file.getAbsolutePath()
        );
        executeProcess(silenceCmd, "Tạo khoảng lặng MP3 " + durationSeconds + "s");
        return file;
    }

    /**
     * Deletes a cached audio file (e.g. for segment regenerate).
     */
    public boolean deleteFromCache(String hash) {
        File file = new File(cacheDir, hash + ".mp3");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Uses ffprobe to accurately measure duration of an audio file in seconds.
     */
    public double getAudioDuration(File audioFile) {
        requireFfmpeg();
        List<String> cmd = List.of(
                ffprobeCmd,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audioFile.getAbsolutePath()
        );

        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && output != null && !output.isBlank()) {
                return Double.parseDouble(output.trim());
            } else {
                throw new RuntimeException("ffprobe failed with exit code " + exitCode + ": " + output);
            }
        } catch (Exception e) {
            log.error("Failed to get duration with ffprobe for {}: {}", audioFile.getAbsolutePath(), e.getMessage());
            throw new RuntimeException("Không thể đọc độ dài file âm thanh: " + e.getMessage(), e);
        }
    }

    /**
     * Converts raw MP3 segments to normalized 44.1kHz mono WAV files, optionally applying speed adjustment via FFmpeg atempo.
     * Updates each segment's durationSeconds to reflect the exact speed-adjusted audio duration.
     */
    public void prepareSegmentWavs(List<SegmentEntity> segments, File workDir, Double speed) {
        requireFfmpeg();

        if (segments == null || segments.isEmpty()) {
            return;
        }

        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        for (int i = 0; i < segments.size(); i++) {
            SegmentEntity seg = segments.get(i);
            File mp3 = new File(seg.getAudioFilePath());
            File segmentWav = new File(workDir, "seg_" + i + ".wav");

            List<String> convertCmd = new ArrayList<>();
            convertCmd.add(ffmpegCmd);
            convertCmd.add("-y");
            convertCmd.add("-i");
            convertCmd.add(mp3.getAbsolutePath());

            if (speed != null && Math.abs(speed - 1.0) > 0.01) {
                convertCmd.add("-filter:a");
                convertCmd.add(String.format(java.util.Locale.US, "atempo=%.2f", speed));
            }

            convertCmd.add("-ar");
            convertCmd.add("44100");
            convertCmd.add("-ac");
            convertCmd.add("1");
            convertCmd.add("-c:a");
            convertCmd.add("pcm_s16le");
            convertCmd.add(segmentWav.getAbsolutePath());

            String actionDesc = "Chuẩn hoá đoạn " + (i + 1) + (speed != null && Math.abs(speed - 1.0) > 0.01 ? " (tốc độ " + speed + "x)" : "");
            executeProcess(convertCmd, actionDesc);

            double actualDuration = getAudioDuration(segmentWav);
            seg.setDurationSeconds(actualDuration);
        }
    }

    /**
     * Concatenates pre-converted audio segments with exact timeline spacing (supporting custom timeline anchors like [0, 20] and [45, 60]).
     */
    public void mergePreparedSegmentsWithExactTiming(List<SegmentEntity> segments, File workDir, File outputWav, File outputMp3) throws IOException {
        requireFfmpeg();

        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Không có file âm thanh nào để ghép");
        }

        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        File concatListFile = new File(workDir, "concat_list.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(concatListFile), StandardCharsets.UTF_8))) {
            // 1. Initial silence if first segment starts after 0.0s (e.g. [5, 20])
            SegmentEntity firstSeg = segments.get(0);
            if (firstSeg.getStartTimeSeconds() > 0.005) {
                File leadSilence = new File(workDir, "lead_silence.wav");
                generateSilenceFile(firstSeg.getStartTimeSeconds(), leadSilence);
                String escapedLead = leadSilence.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                writer.println("file '" + escapedLead + "'");
            }

            // 2. Add each pre-converted segment and calculate silence gap to next segment
            for (int i = 0; i < segments.size(); i++) {
                SegmentEntity seg = segments.get(i);
                File segmentWav = new File(workDir, "seg_" + i + ".wav");

                String escapedPath = segmentWav.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                writer.println("file '" + escapedPath + "'");

                // If not last segment, check gap to next segment
                if (i < segments.size() - 1) {
                    SegmentEntity nextSeg = segments.get(i + 1);
                    double gap = nextSeg.getStartTimeSeconds() - seg.getEndTimeSeconds();
                    if (gap > 0.005) {
                        File gapSilence = new File(workDir, "gap_" + i + ".wav");
                        generateSilenceFile(gap, gapSilence);
                        String escapedGap = gapSilence.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                        writer.println("file '" + escapedGap + "'");
                    }
                }
            }

            // 3. Tail silence if last segment specifies targetEndTime (e.g. [00:24, 00:30] -> end exactly at 30s)
            SegmentEntity lastSeg = segments.get(segments.size() - 1);
            if (lastSeg.getTargetEndTime() != null && lastSeg.getTargetEndTime() > lastSeg.getEndTimeSeconds() + 0.005) {
                double tailGap = lastSeg.getTargetEndTime() - lastSeg.getEndTimeSeconds();
                File tailSilence = new File(workDir, "tail_silence.wav");
                generateSilenceFile(tailGap, tailSilence);
                String escapedTail = tailSilence.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                writer.println("file '" + escapedTail + "'");
            }
        }

        // 3. Concatenate using concat demuxer to output WAV (44.1kHz mono 16-bit)
        List<String> concatWavCmd = List.of(
                ffmpegCmd,
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatListFile.getAbsolutePath(),
                "-c", "copy",
                outputWav.getAbsolutePath()
        );
        executeProcess(concatWavCmd, "Ghép file WAV chính xác");

        // 4. Export MP3 from output WAV
        if (outputMp3 != null) {
            List<String> mp3Cmd = List.of(
                    ffmpegCmd,
                    "-y",
                    "-i", outputWav.getAbsolutePath(),
                    "-ar", "44100",
                    "-ac", "1",
                    "-c:a", "libmp3lame",
                    "-b:a", "128k",
                    outputMp3.getAbsolutePath()
            );
            executeProcess(mp3Cmd, "Xuất file MP3");
        }
    }

    /**
     * Concatenates audio segments with exact timeline spacing (supporting custom timeline anchors like [0, 20] and [45, 60]).
     */
    public void mergeSegmentsWithExactTiming(List<SegmentEntity> segments, File workDir, File outputWav, File outputMp3) throws IOException {
        prepareSegmentWavs(segments, workDir, 1.0);
        mergePreparedSegmentsWithExactTiming(segments, workDir, outputWav, outputMp3);
    }

    private void generateSilenceFile(double durationSeconds, File outputFile) {
        List<String> silenceCmd = List.of(
                ffmpegCmd,
                "-y",
                "-f", "lavfi",
                "-i", "anullsrc=r=44100:cl=mono",
                "-t", String.format(java.util.Locale.US, "%.4f", durationSeconds),
                "-ar", "44100",
                "-ac", "1",
                "-c:a", "pcm_s16le",
                outputFile.getAbsolutePath()
        );
        executeProcess(silenceCmd, "Tạo khoảng lặng (" + String.format(java.util.Locale.US, "%.2fs", durationSeconds) + ")");
    }

    /**
     * Concatenates audio segments with gap silence into WAV (44.1kHz mono 16-bit) and MP3.
     */
    public void mergeSegments(List<File> segmentMp3Files, double gapSeconds, File workDir, File outputWav, File outputMp3) throws IOException {
        requireFfmpeg();

        if (segmentMp3Files == null || segmentMp3Files.isEmpty()) {
            throw new IllegalArgumentException("Không có file âm thanh nào để ghép");
        }

        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        // 1. Generate silence WAV of duration gapSeconds (if gap > 0)
        File silenceWav = new File(workDir, "silence.wav");
        if (gapSeconds > 0) {
            List<String> silenceCmd = List.of(
                    ffmpegCmd,
                    "-y",
                    "-f", "lavfi",
                    "-i", "anullsrc=r=44100:cl=mono",
                    "-t", String.format(java.util.Locale.US, "%.4f", gapSeconds),
                    "-ar", "44100",
                    "-ac", "1",
                    "-c:a", "pcm_s16le",
                    silenceWav.getAbsolutePath()
            );
            executeProcess(silenceCmd, "Tạo khoảng lặng (silence)");
        }

        // 2. Convert each segment to standard 44.1kHz mono 16-bit WAV
        File concatListFile = new File(workDir, "concat_list.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(concatListFile), StandardCharsets.UTF_8))) {
            for (int i = 0; i < segmentMp3Files.size(); i++) {
                File mp3 = segmentMp3Files.get(i);
                File segmentWav = new File(workDir, "seg_" + i + ".wav");

                List<String> convertCmd = List.of(
                        ffmpegCmd,
                        "-y",
                        "-i", mp3.getAbsolutePath(),
                        "-ar", "44100",
                        "-ac", "1",
                        "-c:a", "pcm_s16le",
                        segmentWav.getAbsolutePath()
                );
                executeProcess(convertCmd, "Chuẩn hoá đoạn " + (i + 1));

                // Add to concat list
                // Format: file 'path' (escape single quotes for ffmpeg)
                String escapedPath = segmentWav.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                writer.println("file '" + escapedPath + "'");

                // Add silence gap between segments (not after the last segment)
                if (gapSeconds > 0 && i < segmentMp3Files.size() - 1) {
                    String escapedSilencePath = silenceWav.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                    writer.println("file '" + escapedSilencePath + "'");
                }
            }
        }

        // 3. Concatenate using concat demuxer to output WAV (44.1kHz mono 16-bit)
        List<String> concatWavCmd = List.of(
                ffmpegCmd,
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatListFile.getAbsolutePath(),
                "-c", "copy",
                outputWav.getAbsolutePath()
        );
        executeProcess(concatWavCmd, "Ghép file WAV chính xác");

        // 4. Export MP3 from output WAV
        if (outputMp3 != null) {
            List<String> mp3Cmd = List.of(
                    ffmpegCmd,
                    "-y",
                    "-i", outputWav.getAbsolutePath(),
                    "-ar", "44100",
                    "-ac", "1",
                    "-c:a", "libmp3lame",
                    "-b:a", "128k",
                    outputMp3.getAbsolutePath()
            );
            executeProcess(mp3Cmd, "Xuất file MP3");
        }
    }

    private void executeProcess(List<String> cmd, String taskName) {
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg command failed [{}]: {}\nOutput: {}", taskName, String.join(" ", cmd), output);
                throw new RuntimeException("Lỗi xử lý âm thanh (" + taskName + "): exit code " + exitCode);
            }
        } catch (Exception e) {
            log.error("Error executing {}: {}", taskName, e.getMessage());
            throw new RuntimeException("Không thể thực hiện " + taskName + ": " + e.getMessage(), e);
        }
    }
}

