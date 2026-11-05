package com.adamtts.studio.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextSplittingService {

    private static final int MAX_CHARS_V3 = 4500;
    private static final int MAX_CHARS_DEFAULT = 9000;
    private static final Pattern EMOTION_TAG_PATTERN = Pattern.compile("\\[[^\\]]*\\]");
    // Sentence boundary regex: punctuation followed by space or end of text
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?…])\\s+");
    // Timeline tag regex at beginning of segment: [0, 30], [0 , 30], [45, 60], [45], [00:45, 01:10], [1:30]
    private static final Pattern TIMELINE_TAG_PATTERN = Pattern.compile(
            "^\\[\\s*(\\d+(?::\\d+)?(?:\\.\\d+)?)\\s*(?:,\\s*(\\d+(?::\\d+)?(?:\\.\\d+)?))?\\s*\\]\\s*"
    );

    public record ParsedSegment(
            String rawText,
            String spokenText,
            Double targetStartTime,
            Double targetEndTime
    ) {}

    /**
     * Parses optional timeline anchor [start, end] from beginning of a segment.
     * Strips [start, end] from spoken text, but keeps emotion tags like [excited].
     */
    public ParsedSegment parseSegmentTiming(String segmentText) {
        if (segmentText == null || segmentText.isBlank()) {
            return new ParsedSegment("", "", null, null);
        }

        String trimmed = segmentText.trim();
        Matcher matcher = TIMELINE_TAG_PATTERN.matcher(trimmed);

        if (matcher.find()) {
            String startStr = matcher.group(1);
            String endStr = matcher.group(2);

            Double start = parseTimestampToSeconds(startStr);
            Double end = endStr != null ? parseTimestampToSeconds(endStr) : null;

            String spokenText = trimmed.substring(matcher.end()).trim();
            return new ParsedSegment(trimmed, spokenText, start, end);
        }

        return new ParsedSegment(trimmed, trimmed, null, null);
    }

    public static Double parseTimestampToSeconds(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            String trimmed = input.trim();
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":");
                if (parts.length == 2) {
                    double minutes = Double.parseDouble(parts[0]);
                    double seconds = Double.parseDouble(parts[1]);
                    return minutes * 60.0 + seconds;
                } else if (parts.length == 3) {
                    double hours = Double.parseDouble(parts[0]);
                    double minutes = Double.parseDouble(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    return hours * 3600.0 + minutes * 60.0 + seconds;
                }
            }
            return Double.parseDouble(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    public int getMaxCharsForModel(String modelId) {
        if ("eleven_v3".equalsIgnoreCase(modelId)) {
            return MAX_CHARS_V3;
        }
        return MAX_CHARS_DEFAULT;
    }

    /**
     * Splits raw input text into list of segments based on mode and model character limits.
     *
     * @param rawText   The raw input text
     * @param splitMode "LINE" or "PARAGRAPH"
     * @param modelId   The ElevenLabs model ID
     * @return List of clean segment strings
     */
    public List<String> splitText(String rawText, String splitMode, String modelId) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        int maxChars = getMaxCharsForModel(modelId);
        List<String> rawChunks = new ArrayList<>();

        if ("LINE".equalsIgnoreCase(splitMode)) {
            String[] lines = rawText.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    rawChunks.add(trimmed);
                }
            }
        } else {
            // Default: PARAGRAPH (blank line separated)
            String[] paragraphs = rawText.split("(\\r?\\n\\s*){2,}");
            for (String p : paragraphs) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    rawChunks.add(trimmed);
                }
            }
        }

        List<String> finalSegments = new ArrayList<>();
        for (String chunk : rawChunks) {
            if (chunk.length() <= maxChars) {
                finalSegments.add(chunk);
            } else {
                finalSegments.addAll(splitBySentences(chunk, maxChars));
            }
        }

        return finalSegments;
    }

    /**
     * Splits a long chunk into smaller pieces respecting sentence boundaries.
     */
    public List<String> splitBySentences(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        String[] sentences = SENTENCE_SPLIT_PATTERN.split(text);

        StringBuilder currentSegment = new StringBuilder();

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            // If a single sentence exceeds maxChars, hard break it by words or chars
            if (trimmedSentence.length() > maxChars) {
                if (!currentSegment.isEmpty()) {
                    result.add(currentSegment.toString().trim());
                    currentSegment.setLength(0);
                }
                result.addAll(hardSplit(trimmedSentence, maxChars));
                continue;
            }

            if (currentSegment.length() == 0) {
                currentSegment.append(trimmedSentence);
            } else if (currentSegment.length() + 1 + trimmedSentence.length() <= maxChars) {
                currentSegment.append(" ").append(trimmedSentence);
            } else {
                result.add(currentSegment.toString().trim());
                currentSegment.setLength(0);
                currentSegment.append(trimmedSentence);
            }
        }

        if (!currentSegment.isEmpty()) {
            result.add(currentSegment.toString().trim());
        }

        return result;
    }

    private List<String> hardSplit(String text, int maxChars) {
        List<String> pieces = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxChars) {
                current.append(" ").append(word);
            } else {
                pieces.add(current.toString().trim());
                current.setLength(0);
                current.append(word);
            }
        }

        if (!current.isEmpty()) {
            pieces.add(current.toString().trim());
        }

        return pieces;
    }

    /**
     * Strips emotion and sound tags like [excited], [whispers], [sad], [short pause], etc.
     * For SRT subtitle generation.
     */
    public String stripEmotionTags(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = EMOTION_TAG_PATTERN.matcher(text);
        String cleaned = matcher.replaceAll("");
        // Remove multiple consecutive spaces and trim
        return cleaned.replaceAll("\\s{2,}", " ").trim();
    }
}

